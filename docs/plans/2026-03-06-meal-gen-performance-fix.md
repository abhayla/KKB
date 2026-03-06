# Meal Generation Performance Fix — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix test infrastructure (rate limit, multi-user, rule seeding), improve Gemini first-attempt success rate via response_schema, fix false-positive family constraint filtering.

**Architecture:** Three sequential phases. Phase 1 fixes Locust test infrastructure with no production impact. Phase 2 adds Gemini response_schema to enforce JSON structure at model level and simplifies the prompt. Phase 3 fixes a substring matching bug and updates docs.

**Tech Stack:** Python FastAPI, slowapi, google-genai SDK, Locust, PostgreSQL

**Design doc:** `docs/plans/2026-03-06-meal-gen-performance-fix-design.md`

---

## Task 1: Conditional Rate Limit in DEBUG Mode

**Files:**
- Modify: `backend/app/api/v1/endpoints/meal_plans.py:116`
- Modify: `backend/app/api/v1/endpoints/photos.py:22`

**Step 1: Add settings import to meal_plans.py**

At the top of `meal_plans.py`, add the import (if not already present):

```python
from app.config import settings
```

**Step 2: Change rate limit decorator on generate endpoint**

In `meal_plans.py`, change line 116 from:

```python
@limiter.limit("5/hour")
```

To:

```python
@limiter.limit("500/hour" if settings.debug else "5/hour")
```

**Step 3: Change rate limit decorator on photos endpoint**

In `photos.py`, change line 22 from:

```python
@limiter.limit("10/hour")
```

To:

```python
@limiter.limit("100/hour" if settings.debug else "10/hour")
```

Add `from app.config import settings` at top if not present.

**Step 4: Run existing backend tests to verify no regression**

Run: `cd backend && PYTHONPATH=. pytest tests/api/test_meal_plans.py -v -x`
Expected: All existing tests pass (rate limit doesn't affect tests since they use test client).

**Step 5: Commit**

```bash
git add backend/app/api/v1/endpoints/meal_plans.py backend/app/api/v1/endpoints/photos.py
git commit -m "perf: raise rate limits in DEBUG mode for load testing"
```

---

## Task 2: Unique Backend Users Per Locust Profile

**Files:**
- Modify: `backend/app/core/firebase.py:58-66`
- Modify: `backend/tests/performance/test_profiles.json`

**Step 1: Write a test for the new token pattern matching**

Create `backend/tests/test_firebase_debug_tokens.py`:

```python
"""Tests for debug firebase token pattern matching."""

import pytest
from unittest.mock import patch

from app.core.firebase import verify_firebase_token


@pytest.fixture(autouse=True)
def enable_debug():
    """Enable debug mode for all tests in this file."""
    with patch("app.core.firebase.settings") as mock_settings:
        mock_settings.debug = True
        mock_settings.firebase_credentials_path = None
        yield mock_settings


def test_plain_fake_token_returns_default_uid(enable_debug):
    result = verify_firebase_token("fake-firebase-token")
    assert result["uid"] == "fake-user-id"
    assert result["name"] == "E2E Test User"


def test_suffixed_fake_token_returns_unique_uid(enable_debug):
    result = verify_firebase_token("fake-firebase-token-sharma")
    assert result["uid"] == "fake-user-sharma"
    assert "sharma" in result["name"].lower()


def test_different_suffixes_return_different_uids(enable_debug):
    r1 = verify_firebase_token("fake-firebase-token-sharma")
    r2 = verify_firebase_token("fake-firebase-token-gupta")
    assert r1["uid"] != r2["uid"]
    assert r1["uid"] == "fake-user-sharma"
    assert r2["uid"] == "fake-user-gupta"
```

**Step 2: Run test to verify it fails**

Run: `cd backend && PYTHONPATH=. pytest tests/test_firebase_debug_tokens.py -v`
Expected: `test_suffixed_fake_token_returns_unique_uid` FAILS (current code only accepts exact match).

**Step 3: Update firebase.py to accept token pattern**

In `backend/app/core/firebase.py`, replace lines 58-66:

```python
    # E2E Test mode: accept fake-firebase-token from Android tests
    if settings.debug and id_token == "fake-firebase-token":
        logger.info("E2E Test: Using fake Firebase token for testing")
        return {
            "uid": "fake-user-id",
            "email": "e2e-test@rasoiai.test",
            "name": "E2E Test User",
            "picture": None,
            "phone_number": "+911111111111",
        }
```

With:

```python
    # E2E Test mode: accept fake-firebase-token and fake-firebase-token-{suffix}
    if settings.debug and id_token.startswith("fake-firebase-token"):
        suffix = id_token.removeprefix("fake-firebase-token").lstrip("-")
        uid = f"fake-user-{suffix}" if suffix else "fake-user-id"
        name = suffix.replace("-", " ").title() if suffix else "E2E Test User"
        phone_suffix = hash(suffix) % 9000000000 + 1000000000 if suffix else 1111111111
        logger.info(f"E2E Test: Using fake Firebase token for testing (uid={uid})")
        return {
            "uid": uid,
            "email": f"{suffix or 'e2e-test'}@rasoiai.test",
            "name": name or "E2E Test User",
            "picture": None,
            "phone_number": f"+91{phone_suffix}",
        }
```

**Step 4: Run test to verify it passes**

Run: `cd backend && PYTHONPATH=. pytest tests/test_firebase_debug_tokens.py -v`
Expected: All 3 tests PASS.

**Step 5: Run full auth test suite to verify backward compatibility**

Run: `cd backend && PYTHONPATH=. pytest tests/api/test_auth.py -v -x`
Expected: All existing auth tests pass (plain `fake-firebase-token` still works).

**Step 6: Update test_profiles.json with unique tokens**

Replace `backend/tests/performance/test_profiles.json` — change each profile's `firebase_token`:

- Sharma: `"fake-firebase-token-sharma"`
- Gupta: `"fake-firebase-token-gupta"`
- Reddy: `"fake-firebase-token-reddy"`
- Khan: `"fake-firebase-token-khan"`
- Jain: `"fake-firebase-token-jain"`

Also add `recipe_rules` array to each profile for Task 3 to use:

**Sharma:**
```json
"recipe_rules": [
    {"target_name": "Chai", "action": "INCLUDE", "target_type": "INGREDIENT", "frequency_type": "DAILY", "meal_slot": "BREAKFAST"},
    {"target_name": "Dal", "action": "INCLUDE", "target_type": "INGREDIENT", "frequency_type": "TIMES_PER_WEEK", "frequency_count": 4, "meal_slot": "LUNCH"},
    {"target_name": "Mushroom", "action": "EXCLUDE", "target_type": "INGREDIENT", "frequency_type": "NEVER"}
]
```

**Gupta:**
```json
"recipe_rules": [
    {"target_name": "Egg", "action": "INCLUDE", "target_type": "INGREDIENT", "frequency_type": "TIMES_PER_WEEK", "frequency_count": 4, "meal_slot": "BREAKFAST"},
    {"target_name": "Pork", "action": "EXCLUDE", "target_type": "INGREDIENT", "frequency_type": "NEVER"}
]
```

**Reddy:**
```json
"recipe_rules": [
    {"target_name": "Dosa", "action": "INCLUDE", "target_type": "INGREDIENT", "frequency_type": "TIMES_PER_WEEK", "frequency_count": 3, "meal_slot": "BREAKFAST"},
    {"target_name": "Idli", "action": "INCLUDE", "target_type": "INGREDIENT", "frequency_type": "TIMES_PER_WEEK", "frequency_count": 2, "meal_slot": "BREAKFAST"}
]
```

**Khan:**
```json
"recipe_rules": [
    {"target_name": "Chicken", "action": "INCLUDE", "target_type": "INGREDIENT", "frequency_type": "TIMES_PER_WEEK", "frequency_count": 3, "meal_slot": "DINNER"},
    {"target_name": "Beef", "action": "EXCLUDE", "target_type": "INGREDIENT", "frequency_type": "NEVER"}
]
```

**Jain:**
```json
"recipe_rules": [
    {"target_name": "Onion", "action": "EXCLUDE", "target_type": "INGREDIENT", "frequency_type": "NEVER"},
    {"target_name": "Garlic", "action": "EXCLUDE", "target_type": "INGREDIENT", "frequency_type": "NEVER"},
    {"target_name": "Potato", "action": "EXCLUDE", "target_type": "INGREDIENT", "frequency_type": "NEVER"}
]
```

**Step 7: Commit**

```bash
git add backend/app/core/firebase.py backend/tests/test_firebase_debug_tokens.py backend/tests/performance/test_profiles.json
git commit -m "perf: support unique fake-firebase-token-{suffix} for multi-user load testing"
```

---

## Task 3: Seed Preferences and Rules in Locust on_start

**Files:**
- Modify: `backend/tests/performance/locustfile.py` — `MealGenHeavyUser.on_start()`

**Step 1: Add preference/rule seeding to MealGenHeavyUser.on_start()**

After the existing auth block in `MealGenHeavyUser.on_start()`, add API calls to seed the test user's preferences, family members, and recipe rules. This uses the endpoints:
- `PUT /api/v1/users/preferences` (schema: `UserPreferencesUpdate`)
- `POST /api/v1/users/family-members` (schema: `FamilyMemberCreate`)
- `POST /api/v1/recipe-rules` (schema: `RecipeRuleCreate`)

Add a `_seed_profile()` method to `MealGenHeavyUser`:

```python
def _seed_profile(self):
    """Seed preferences, family members, and recipe rules from test profile."""
    if not self._token or not self._profile:
        return

    prefs = self._profile.get("preferences", {})

    # 1. Update preferences
    self.client.put(
        "/api/v1/users/preferences",
        json={
            "household_size": prefs.get("household_size", 4),
            "primary_diet": prefs.get("primary_diet"),
            "dietary_restrictions": prefs.get("dietary_restrictions", []),
            "cuisine_preferences": prefs.get("cuisine_preferences", []),
            "disliked_ingredients": prefs.get("dislikes", []),
            "spice_level": prefs.get("spice_level", "medium"),
            "busy_days": prefs.get("busy_days", []),
            "weekday_cooking_time": prefs.get("weekday_cooking_time", 30),
            "weekend_cooking_time": prefs.get("weekend_cooking_time", 60),
        },
        headers=self.auth_headers,
        name="/api/v1/users/preferences [SETUP]",
    )

    # 2. Add family members
    for member in prefs.get("family_members", []):
        self.client.post(
            "/api/v1/users/family-members",
            json={
                "name": member["name"],
                "age_group": member.get("type", "adult").lower(),
                "health_conditions": member.get("special_needs", []),
                "dietary_restrictions": [],
            },
            headers=self.auth_headers,
            name="/api/v1/users/family-members [SETUP]",
        )

    # 3. Add recipe rules
    for rule in self._profile.get("recipe_rules", []):
        self.client.post(
            "/api/v1/recipe-rules",
            json={
                "target_type": rule.get("target_type", "INGREDIENT"),
                "action": rule["action"],
                "target_name": rule["target_name"],
                "frequency_type": rule["frequency_type"],
                "frequency_count": rule.get("frequency_count"),
                "frequency_days": rule.get("frequency_days"),
                "meal_slot": rule.get("meal_slot"),
                "enforcement": rule.get("enforcement", "REQUIRED"),
            },
            headers=self.auth_headers,
            name="/api/v1/recipe-rules [SETUP]",
        )
```

Call `self._seed_profile()` at the end of `on_start()` after authentication succeeds.

**Step 2: Run the smoke Locust test to verify seeding works**

Run: `cd backend && locust -f tests/performance/locustfile.py MealGenHeavyUser --headless -u 1 -r 1 -t 30s --host http://localhost:8000`
Expected: Auth succeeds, preferences/rules seeded (check backend logs for "Loaded N INCLUDE rules").

**Step 3: Commit**

```bash
git add backend/tests/performance/locustfile.py
git commit -m "perf: seed user preferences and recipe rules in Locust on_start"
```

---

## Task 4: Add response_schema to Gemini Client

**Files:**
- Modify: `backend/app/ai/gemini_client.py:111-163` (generate_text) and `165-216` (generate_text_with_metadata)

**Step 1: Write a test for the response_schema parameter**

Create `backend/tests/test_gemini_schema.py`:

```python
"""Tests that generate_text accepts and passes response_schema."""

import pytest
from unittest.mock import AsyncMock, MagicMock, patch


@pytest.mark.asyncio
async def test_generate_text_passes_schema_to_config():
    """Verify response_schema is included in GenerateContentConfig."""
    from google.genai import types

    mock_schema = types.Schema(
        type="OBJECT",
        properties={"days": types.Schema(type="ARRAY")},
    )

    mock_client = MagicMock()
    mock_response = MagicMock()
    mock_response.text = '{"days": []}'
    mock_client.aio.models.generate_content = AsyncMock(return_value=mock_response)

    with patch("app.ai.gemini_client.get_gemini_client", return_value=mock_client):
        from app.ai.gemini_client import generate_text

        result = await generate_text("test prompt", response_schema=mock_schema)
        assert result == '{"days": []}'

        # Verify schema was passed in config
        call_kwargs = mock_client.aio.models.generate_content.call_args
        config = call_kwargs.kwargs.get("config") or call_kwargs[1].get("config")
        assert config.response_schema == mock_schema


@pytest.mark.asyncio
async def test_generate_text_without_schema_works():
    """Verify generate_text still works without response_schema."""
    mock_client = MagicMock()
    mock_response = MagicMock()
    mock_response.text = '{"days": []}'
    mock_client.aio.models.generate_content = AsyncMock(return_value=mock_response)

    with patch("app.ai.gemini_client.get_gemini_client", return_value=mock_client):
        from app.ai.gemini_client import generate_text

        result = await generate_text("test prompt")
        assert result == '{"days": []}'

        call_kwargs = mock_client.aio.models.generate_content.call_args
        config = call_kwargs.kwargs.get("config") or call_kwargs[1].get("config")
        assert config.response_schema is None
```

**Step 2: Run test to verify it fails**

Run: `cd backend && PYTHONPATH=. pytest tests/test_gemini_schema.py -v`
Expected: FAIL — `generate_text()` doesn't accept `response_schema` parameter yet.

**Step 3: Add response_schema parameter to both functions**

In `backend/app/ai/gemini_client.py`, update `generate_text()` signature and config:

```python
async def generate_text(
    prompt: str,
    temperature: float = 0.8,
    max_output_tokens: int = 65536,
    response_schema=None,
) -> str:
```

And in the config construction:

```python
        config = types.GenerateContentConfig(
            temperature=temperature,
            max_output_tokens=max_output_tokens,
            response_mime_type="application/json",
            response_schema=response_schema,
        )
```

Same change for `generate_text_with_metadata()`:

```python
async def generate_text_with_metadata(
    prompt: str,
    temperature: float = 0.8,
    max_output_tokens: int = 65536,
    response_schema=None,
) -> tuple[str, dict]:
```

And its config:

```python
        config = types.GenerateContentConfig(
            temperature=temperature,
            max_output_tokens=max_output_tokens,
            response_mime_type="application/json",
            response_schema=response_schema,
        )
```

**Step 4: Run test to verify it passes**

Run: `cd backend && PYTHONPATH=. pytest tests/test_gemini_schema.py -v`
Expected: Both tests PASS.

**Step 5: Commit**

```bash
git add backend/app/ai/gemini_client.py backend/tests/test_gemini_schema.py
git commit -m "feat: add optional response_schema parameter to Gemini client"
```

---

## Task 5: Define Meal Plan Schema and Pass to Gemini

**Files:**
- Modify: `backend/app/services/ai_meal_service.py`

**Step 1: Write a test for the schema being used**

Add to `backend/tests/test_ai_meal_service.py` (or create if needed):

```python
"""Test that MEAL_PLAN_SCHEMA is valid and used in generation."""

def test_meal_plan_schema_has_required_structure():
    from app.services.ai_meal_service import MEAL_PLAN_SCHEMA

    # Schema must exist and be a google.genai Schema
    assert MEAL_PLAN_SCHEMA is not None
    assert MEAL_PLAN_SCHEMA.type == "OBJECT"
    assert "days" in MEAL_PLAN_SCHEMA.properties

    day_schema = MEAL_PLAN_SCHEMA.properties["days"].items
    assert day_schema is not None
    for slot in ["breakfast", "lunch", "dinner", "snacks"]:
        assert slot in day_schema.properties, f"Missing required slot: {slot}"
```

**Step 2: Run test to verify it fails**

Run: `cd backend && PYTHONPATH=. pytest tests/test_ai_meal_service.py::test_meal_plan_schema_has_required_structure -v`
Expected: FAIL — `MEAL_PLAN_SCHEMA` doesn't exist yet.

**Step 3: Define MEAL_PLAN_SCHEMA in ai_meal_service.py**

Add after the imports section (around line 37), before the data classes:

```python
from google.genai import types as genai_types

# Schema for Gemini structured output — enforces all 4 meal slots per day
_MEAL_ITEM_SCHEMA = genai_types.Schema(
    type="OBJECT",
    required=["recipe_name", "prep_time_minutes", "dietary_tags", "category"],
    properties={
        "recipe_name": genai_types.Schema(type="STRING"),
        "prep_time_minutes": genai_types.Schema(type="INTEGER"),
        "dietary_tags": genai_types.Schema(
            type="ARRAY", items=genai_types.Schema(type="STRING")
        ),
        "category": genai_types.Schema(type="STRING"),
        "calories": genai_types.Schema(type="INTEGER"),
        "ingredients": genai_types.Schema(
            type="ARRAY",
            items=genai_types.Schema(
                type="OBJECT",
                properties={
                    "name": genai_types.Schema(type="STRING"),
                    "quantity": genai_types.Schema(type="NUMBER"),
                    "unit": genai_types.Schema(type="STRING"),
                    "category": genai_types.Schema(type="STRING"),
                },
            ),
        ),
        "nutrition": genai_types.Schema(
            type="OBJECT",
            properties={
                "protein_g": genai_types.Schema(type="NUMBER"),
                "carbs_g": genai_types.Schema(type="NUMBER"),
                "fat_g": genai_types.Schema(type="NUMBER"),
                "fiber_g": genai_types.Schema(type="NUMBER"),
            },
        ),
    },
)

_MEAL_SLOT_SCHEMA = genai_types.Schema(
    type="ARRAY", items=_MEAL_ITEM_SCHEMA, min_items=2
)

MEAL_PLAN_SCHEMA = genai_types.Schema(
    type="OBJECT",
    required=["days"],
    properties={
        "days": genai_types.Schema(
            type="ARRAY",
            min_items=7,
            max_items=7,
            items=genai_types.Schema(
                type="OBJECT",
                required=["date", "day_name", "breakfast", "lunch", "dinner", "snacks"],
                properties={
                    "date": genai_types.Schema(type="STRING"),
                    "day_name": genai_types.Schema(type="STRING"),
                    "breakfast": _MEAL_SLOT_SCHEMA,
                    "lunch": _MEAL_SLOT_SCHEMA,
                    "dinner": _MEAL_SLOT_SCHEMA,
                    "snacks": _MEAL_SLOT_SCHEMA,
                },
            ),
        ),
    },
)
```

**Step 4: Pass the schema to Gemini in _generate_with_retry()**

In `_generate_with_retry()`, change the call from:

```python
response, metadata = await generate_text_with_metadata(prompt)
```

To:

```python
response, metadata = await generate_text_with_metadata(
    prompt, response_schema=MEAL_PLAN_SCHEMA
)
```

And the non-metadata path:

```python
response = await generate_text(prompt, response_schema=MEAL_PLAN_SCHEMA)
```

**Step 5: Run test to verify it passes**

Run: `cd backend && PYTHONPATH=. pytest tests/test_ai_meal_service.py::test_meal_plan_schema_has_required_structure -v`
Expected: PASS.

**Step 6: Run existing ai_meal_service tests**

Run: `cd backend && PYTHONPATH=. pytest tests/test_ai_meal_service.py -v -x`
Expected: All existing tests pass.

**Step 7: Commit**

```bash
git add backend/app/services/ai_meal_service.py
git commit -m "feat: add MEAL_PLAN_SCHEMA for Gemini structured output enforcement"
```

---

## Task 6: Simplify Prompt (Remove JSON Examples)

**Files:**
- Modify: `backend/app/services/ai_meal_service.py:678-770` — `_build_prompt()`

**Step 1: Replace the OUTPUT FORMAT section in _build_prompt()**

In `_build_prompt()`, replace everything from `## OUTPUT FORMAT` (around line 742) to the end of the prompt string with:

```python
## OUTPUT FORMAT
Return valid JSON with exactly 7 days. Each day has 4 slots (breakfast, lunch, dinner, snacks).
Each slot has exactly {prefs.items_per_meal} items. Each item needs: recipe_name, prep_time_minutes, dietary_tags (array), category, calories (int), ingredients (array of {{name, quantity, unit, category}}), nutrition ({{protein_g, carbs_g, fat_g, fiber_g}}).
Use authentic Indian dish names (e.g., "Aloo Paratha", "Masala Chai", "Dal Tadka").

Generate the complete 7-day meal plan now:"""
```

This removes ~2,500 characters of JSON examples that the `response_schema` now handles.

**Step 2: Run existing tests**

Run: `cd backend && PYTHONPATH=. pytest tests/test_ai_meal_service.py -v -x`
Expected: All tests pass. (Prompt content tests may need updating if they check for specific strings in the old format.)

**Step 3: Commit**

```bash
git add backend/app/services/ai_meal_service.py
git commit -m "perf: simplify Gemini prompt — response_schema handles JSON structure"
```

---

## Task 7: Fix False Positive Substring Matching

**Files:**
- Modify: `backend/app/services/ai_meal_service.py:1028,1045,1083,1114`

**Step 1: Write a test for the false positive fix**

Add to `backend/tests/test_ai_meal_service.py`:

```python
def test_enforce_rules_does_not_remove_unsweetened_items():
    """Regression test: 'unsweetened' should NOT match 'sweet' keyword."""
    from app.services.ai_meal_service import AIMealService, MealItem, DayMeals, GeneratedMealPlan, UserPreferences

    service = AIMealService()
    plan = GeneratedMealPlan(
        week_start_date="2026-03-02",
        week_end_date="2026-03-08",
        days=[
            DayMeals(
                date="2026-03-02",
                day_name="Monday",
                breakfast=[
                    MealItem(id="1", recipe_name="Masala Chai"),
                    MealItem(id="2", recipe_name="Aloo Paratha"),
                ],
                lunch=[
                    MealItem(id="3", recipe_name="Dal Tadka"),
                    MealItem(id="4", recipe_name="Jeera Rice"),
                ],
                dinner=[
                    MealItem(id="5", recipe_name="Paneer Curry"),
                    MealItem(id="6", recipe_name="Roti"),
                ],
                snacks=[
                    MealItem(id="7", recipe_name="Herbal Tea (unsweetened)"),
                    MealItem(id="8", recipe_name="Roasted Makhana"),
                ],
            ),
        ] + [
            DayMeals(
                date=f"2026-03-0{i}",
                day_name=["Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"][i-3],
                breakfast=[MealItem(id=f"b{i}", recipe_name="Poha")],
                lunch=[MealItem(id=f"l{i}", recipe_name="Rice")],
                dinner=[MealItem(id=f"d{i}", recipe_name="Roti")],
                snacks=[MealItem(id=f"s{i}", recipe_name="Chai")],
            )
            for i in range(3, 9)
        ],
    )

    prefs = UserPreferences(
        family_members=[
            {"name": "Ramesh", "health_conditions": ["DIABETIC"], "dietary_restrictions": []},
        ],
    )

    result = service._enforce_rules(plan, prefs)

    # "Herbal Tea (unsweetened)" should NOT be removed
    snack_names = [item.recipe_name for item in result.days[0].snacks]
    assert "Herbal Tea (unsweetened)" in snack_names, (
        f"'unsweetened' was incorrectly removed. Remaining snacks: {snack_names}"
    )


def test_enforce_rules_still_removes_actual_sweets():
    """Verify that actual sweet items ARE still removed for diabetic members."""
    from app.services.ai_meal_service import AIMealService, MealItem, DayMeals, GeneratedMealPlan, UserPreferences

    service = AIMealService()
    plan = GeneratedMealPlan(
        week_start_date="2026-03-02",
        week_end_date="2026-03-08",
        days=[
            DayMeals(
                date="2026-03-02",
                day_name="Monday",
                breakfast=[MealItem(id="1", recipe_name="Chai")],
                lunch=[MealItem(id="2", recipe_name="Rice")],
                dinner=[MealItem(id="3", recipe_name="Roti")],
                snacks=[
                    MealItem(id="4", recipe_name="Gulab Jamun"),
                    MealItem(id="5", recipe_name="Sweet Lassi"),
                ],
            ),
        ] + [
            DayMeals(
                date=f"2026-03-0{i}", day_name="Day",
                breakfast=[MealItem(id=f"b{i}", recipe_name="Poha")],
                lunch=[MealItem(id=f"l{i}", recipe_name="Rice")],
                dinner=[MealItem(id=f"d{i}", recipe_name="Roti")],
                snacks=[MealItem(id=f"s{i}", recipe_name="Chai")],
            )
            for i in range(3, 9)
        ],
    )

    prefs = UserPreferences(
        family_members=[
            {"name": "Ramesh", "health_conditions": ["DIABETIC"], "dietary_restrictions": []},
        ],
    )

    result = service._enforce_rules(plan, prefs)

    snack_names = [item.recipe_name for item in result.days[0].snacks]
    assert "Gulab Jamun" not in snack_names, "Gulab Jamun should be removed (contains 'gulab jamun')"
    assert "Sweet Lassi" not in snack_names, "Sweet Lassi should be removed (contains 'sweet')"
```

**Step 2: Run test to verify the false positive is confirmed**

Run: `cd backend && PYTHONPATH=. pytest tests/test_ai_meal_service.py::test_enforce_rules_does_not_remove_unsweetened_items -v`
Expected: FAIL — "unsweetened" currently matches "sweet".

**Step 3: Add word-boundary matching utility**

At the top of `ai_meal_service.py`, add `import re` (if not present).

Then add a helper function near the top of the file:

```python
def _keyword_match(keyword: str, text: str) -> bool:
    """Check if keyword appears as a whole word in text.

    Uses word-boundary matching to avoid false positives like
    'unsweetened' matching 'sweet'.
    """
    return bool(re.search(r'\b' + re.escape(keyword) + r'\b', text))
```

**Step 4: Replace all substring checks with _keyword_match**

In `_enforce_rules()`, replace these 4 lines:

Line ~1028 (allergen check):
```python
# Before:
has_allergen = any(allergen in name_lower for allergen in allergens)
# After:
has_allergen = any(_keyword_match(allergen, name_lower) for allergen in allergens)
```

Line ~1045 (exclude check):
```python
# Before:
is_excluded = any(excl in name_lower for excl in day_excludes)
# After:
is_excluded = any(_keyword_match(excl, name_lower) for excl in day_excludes)
```

Line ~1083 (include count):
```python
# Before:
if target in item.recipe_name.lower():
# After:
if _keyword_match(target, item.recipe_name.lower()):
```

Line ~1114 (family constraint):
```python
# Before:
if keyword in name_lower or keyword in ingredient_text:
# After:
if _keyword_match(keyword, name_lower) or _keyword_match(keyword, ingredient_text):
```

**Step 5: Run both tests**

Run: `cd backend && PYTHONPATH=. pytest tests/test_ai_meal_service.py::test_enforce_rules_does_not_remove_unsweetened_items tests/test_ai_meal_service.py::test_enforce_rules_still_removes_actual_sweets -v`
Expected: Both PASS.

**Step 6: Run full ai_meal_service test suite**

Run: `cd backend && PYTHONPATH=. pytest tests/test_ai_meal_service.py -v -x`
Expected: All tests pass.

**Step 7: Commit**

```bash
git add backend/app/services/ai_meal_service.py backend/tests/test_ai_meal_service.py
git commit -m "fix: use word-boundary matching in family constraint filter to prevent false positives"
```

---

## Task 8: Update Performance Targets and Run Final Test

**Files:**
- Modify: `backend/tests/performance/README.md`

**Step 1: Update performance targets in README**

In `backend/tests/performance/README.md`, change the Performance Targets table from:

```markdown
| Meal generation | <30s | <60s | <120s |
```

To:

```markdown
| Meal generation | <60s | <90s | <120s |
```

**Step 2: Run the full Locust mealgen test**

Run: `cd backend && bash tests/performance/run_perf_tests.sh mealgen`

Verify in the output:
- All 3 users authenticate with different UIDs
- Backend logs show "Loaded N INCLUDE rules" (not 0)
- No 429 rate limit errors
- First-attempt success rate improved

**Step 3: Commit**

```bash
git add backend/tests/performance/README.md
git commit -m "docs: update meal generation performance targets for AI-powered generation"
```

---

## Task Summary

| Task | Phase | Description | Files |
|------|-------|-------------|-------|
| 1 | 1A | Conditional rate limit in DEBUG | `meal_plans.py`, `photos.py` |
| 2 | 1B | Unique fake-firebase-token-{suffix} | `firebase.py`, `test_profiles.json`, new test |
| 3 | 1C | Seed rules in Locust on_start | `locustfile.py` |
| 4 | 2A | response_schema in gemini_client | `gemini_client.py`, new test |
| 5 | 2A | Define MEAL_PLAN_SCHEMA | `ai_meal_service.py` |
| 6 | 2B | Simplify prompt | `ai_meal_service.py` |
| 7 | 3 | Word-boundary matching | `ai_meal_service.py`, new test |
| 8 | 4 | Update perf targets + final test | `README.md` |
