# Meal Generation Algorithm

This document describes the implementation of RasoiAI's AI-powered meal plan generation using Google Gemini.

**Related:** [Meal-Generation-Config-Architecture.md](./Meal-Generation-Config-Architecture.md) - Config structure and chat integration

---

## Implementation Status

| Feature | Status | Notes |
|---------|--------|-------|
| Gemini AI generation | ✅ Implemented | Uses Gemini 2.5 Flash (`gemini-2.5-flash`) |
| 2-item pairing logic | ✅ Implemented | AI generates complementary pairs |
| INCLUDE rules (DAILY/TIMES_PER_WEEK) | ✅ Implemented | Passed to AI in prompt |
| EXCLUDE rules (NEVER/SPECIFIC_DAYS) | ✅ Implemented | Post-processing enforcement |
| Allergy exclusion | ✅ Implemented | Post-processing enforcement |
| Dislike filtering | ✅ Implemented | Included in AI prompt |
| Cooking time limits | ✅ Implemented | AI estimates prep times |
| Festival/fasting integration | ✅ Implemented | Context passed to AI |
| Retry with exponential backoff | ✅ Implemented | 3 attempts max |
| JSON response validation | ✅ Implemented | Structure validation |

**Test Coverage:** 25 unit tests + 6 schema tests + 14 generation tracker tests

---

## Overview

The algorithm generates a **7-day personalized meal plan** using Google Gemini AI. The AI freely generates recipe names (no database lookup required) while respecting user preferences, INCLUDE/EXCLUDE rules, allergies, and festivals.

```
┌─────────────────────────────────────────────────────────────────────┐
│                       AIMealService                                  │
├─────────────────────────────────────────────────────────────────────┤
│  Inputs:                                                            │
│  ├── User Preferences (PostgreSQL)                                   │
│  ├── Festivals (PostgreSQL)                                         │
│  └── Pairing Config (YAML → PostgreSQL)                              │
│                                                                     │
│  Process:                                                           │
│  ├── Build comprehensive prompt                                      │
│  ├── Call Gemini 2.5 Flash (JSON output)                            │
│  ├── Validate response structure                                     │
│  └── Post-process: enforce allergens, EXCLUDE rules                  │
│                                                                     │
│  Outputs:                                                           │
│  └── GeneratedMealPlan (7 days × 4 slots × 2 items = 56 items)     │
└─────────────────────────────────────────────────────────────────────┘
```

**Source Code:** `backend/app/services/ai_meal_service.py`

---

## Data Structures

### MealItem
```python
@dataclass
class MealItem:
    id: str                              # UUID for each item
    recipe_name: str                     # AI-generated recipe name
    prep_time_minutes: int = 30          # AI-estimated time
    dietary_tags: list[str] = field(default_factory=list)
    category: str = ""                   # dal, sabzi, chai, etc.
    is_locked: bool = False
    recipe_id: str = "AI_GENERATED"      # No database lookup
    recipe_image_url: Optional[str] = None
    calories: int = 0
```

### UserPreferences
```python
@dataclass
class UserPreferences:
    dietary_tags: list[str]              # ["vegetarian"]
    cuisine_type: Optional[str]          # "north"
    allergies: list[dict]                # [{"ingredient": "peanuts", "severity": "SEVERE"}]
    dislikes: list[str]                  # ["karela", "lauki"]
    weekday_cooking_time: int            # 30 min
    weekend_cooking_time: int            # 60 min
    busy_days: list[str]                 # ["MONDAY", "WEDNESDAY"]
    include_rules: list[dict]            # INCLUDE rules from recipe_rules
    exclude_rules: list[dict]            # EXCLUDE rules from recipe_rules
```

### GeneratedMealPlan
```python
@dataclass
class GeneratedMealPlan:
    week_start_date: str                 # "2026-01-27"
    week_end_date: str                   # "2026-02-02"
    days: list[DayMeals]                 # 7 days
    rules_applied: dict                  # Summary of rules applied
```

---

## Algorithm Phases

### Phase 1: Load Data

```python
# 1. Load user preferences from PostgreSQL
prefs = await self._load_user_preferences(user_id)

# 2. Load festivals for the week
festivals = await self._load_festivals(week_start_date)

# 3. Get pairing config (optional, for prompt guidance)
config = await self.config_service.get_config()
```

---

### Phase 2: Build AI Prompt

The prompt is carefully structured to provide the AI with all necessary context:

```python
def _build_prompt(self, prefs, festivals, config, week_start_date) -> str:
    """Build comprehensive prompt for Gemini."""
```

**Prompt Structure:**

```
You are RasoiAI, an Indian meal planning assistant. Generate a 7-day meal plan.

## USER PREFERENCES (STRICT - MUST FOLLOW)

### Dietary Tags: [vegetarian, eggetarian, etc.]

### Allergies (NEVER INCLUDE - SAFETY CRITICAL):
- peanuts (SEVERE)
- shellfish (MODERATE)

### Dislikes (AVOID WHEN POSSIBLE):
- karela, lauki, turai

### Cuisine Preference: north, west

## INCLUDE RULES (MUST APPEAR)

- Chai: DAILY at breakfast, snacks
- Dal: 4x/week at lunch, dinner
- Egg: 4x/week at breakfast, lunch, dinner
- Chicken: 2x/week at lunch, dinner

## EXCLUDE RULES (NEVER INCLUDE)

- Mushroom: NEVER
- Onion: On TUESDAY
- Non-Veg: On TUESDAY

## COOKING TIME LIMITS

- Weekdays: Max 30 minutes
- Weekends: Max 60 minutes
- Busy days (MONDAY, WEDNESDAY): Max 30 minutes

## FESTIVALS THIS WEEK

- Tuesday 2026-02-10: Ekadashi (fasting day)
  - Special foods: Sabudana Khichdi, fruits
  - Avoid: grains, onion, garlic

## PAIRING GUIDANCE

- Dal pairs with: rice, roti, paratha, naan
- Sabzi pairs with: roti, paratha, rice
- Paratha pairs with: chai, raita, pickle
- Dosa/Idli pairs with: sambar, chutney

## OUTPUT FORMAT

Return valid JSON:
{
  "days": [
    {
      "date": "2026-02-09",
      "day_name": "Monday",
      "breakfast": [
        {"recipe_name": "Aloo Paratha", "prep_time_minutes": 25,
         "dietary_tags": ["vegetarian"], "category": "paratha"},
        {"recipe_name": "Masala Chai", "prep_time_minutes": 10,
         "dietary_tags": ["vegetarian"], "category": "chai"}
      ],
      "lunch": [...],
      "dinner": [...],
      "snacks": [...]
    }
  ]
}
```

---

### Phase 3: Generate with Retry

```python
async def _generate_with_retry(self, prompt: str, max_retries: int = 3) -> str:
    """Call Gemini with exponential backoff retry."""

    for attempt in range(max_retries):
        try:
            response = await generate_text(prompt)
            self._validate_response_structure(response)
            return response
        except Exception as e:
            logger.warning(f"Attempt {attempt + 1} failed: {e}")
            if attempt < max_retries - 1:
                await asyncio.sleep(2 ** attempt)  # 1s, 2s, 4s

    raise ServiceUnavailableError(f"Failed after {max_retries} attempts")
```

**Gemini Configuration:**
```python
config = types.GenerateContentConfig(
    temperature=0.8,                    # Creative but consistent
    max_output_tokens=65536,            # Enough for full week with detailed recipes
    response_mime_type="application/json",  # Structured output
    response_json_schema=MEAL_PLAN_SCHEMA,  # Enforced JSON structure (short keys)
    thinking_config=types.ThinkingConfig(thinking_budget=0),  # Disabled — adds latency, schema already constrains
)
```

**Structured Output (Short Keys):** Gemini 2.5 Flash rejects complex `response_schema` with "too many states for serving" error. The workaround uses `response_json_schema` (plain dict) with abbreviated property names: `d`=date, `dn`=day_name, `b`=breakfast, `l`=lunch, `di`=dinner, `s`=snacks, `n`=recipe_name, `t`=prep_time, `tags`=dietary_tags, `c`=category, `cal`=calories. The prompt explains the abbreviations to the AI, and the parser handles both short and long key formats.

---

### Phase 4: Validate Response

```python
def _validate_response_structure(self, response_text: str) -> dict:
    """Validate the JSON structure from Gemini."""

    data = json.loads(response_text)

    # Must have 'days' array
    if "days" not in data:
        raise ValueError("Response missing 'days' array")

    # Must have 7 days
    if len(data["days"]) != 7:
        raise ValueError(f"Expected 7 days, got {len(data['days'])}")

    # Each day must have required meal slots
    for day in data["days"]:
        for slot in ["breakfast", "lunch", "dinner", "snacks"]:
            if slot not in day:
                raise ValueError(f"Day missing '{slot}' slot")

    return data
```

---

### Phase 5: Parse Response

```python
def _parse_response(self, response_text: str, week_start_date: date) -> GeneratedMealPlan:
    """Convert JSON response to GeneratedMealPlan dataclass."""

    data = json.loads(response_text)
    days = []

    for i, day_data in enumerate(data["days"]):
        current_date = week_start_date + timedelta(days=i)

        day_meals = DayMeals(
            date=current_date.isoformat(),
            day_name=current_date.strftime("%A"),
            breakfast=self._parse_meal_items(day_data.get("breakfast", [])),
            lunch=self._parse_meal_items(day_data.get("lunch", [])),
            dinner=self._parse_meal_items(day_data.get("dinner", [])),
            snacks=self._parse_meal_items(day_data.get("snacks", [])),
            festival=day_data.get("festival")
        )
        days.append(day_meals)

    return GeneratedMealPlan(
        week_start_date=week_start_date.isoformat(),
        week_end_date=(week_start_date + timedelta(days=6)).isoformat(),
        days=days,
        rules_applied={}
    )
```

---

### Phase 6: Post-Processing Enforcement

Even though the AI is instructed to follow rules, we enforce critical safety rules after generation:

```python
def _enforce_rules(self, plan: GeneratedMealPlan, prefs: UserPreferences) -> GeneratedMealPlan:
    """Post-process to enforce allergens and EXCLUDE rules."""

    # Build set of items to check against
    allergens = {a["ingredient"].lower() for a in prefs.allergies}

    # EXCLUDE NEVER rules
    exclude_never = {
        r["target"].lower()
        for r in prefs.exclude_rules
        if r.get("frequency") == "NEVER"
    }

    for day in plan.days:
        day_name = day.day_name.upper()

        # EXCLUDE SPECIFIC_DAYS rules for this day
        exclude_today = {
            r["target"].lower()
            for r in prefs.exclude_rules
            if r.get("frequency") == "SPECIFIC_DAYS"
            and day_name in [d.upper() for d in r.get("specific_days", [])]
        }

        all_excluded = allergens | exclude_never | exclude_today

        # Check each meal slot
        for slot in ["breakfast", "lunch", "dinner", "snacks"]:
            items = getattr(day, slot)
            filtered_items = []

            for item in items:
                recipe_lower = item.recipe_name.lower()
                # Check if any excluded ingredient appears in recipe name
                should_exclude = any(
                    excluded in recipe_lower
                    for excluded in all_excluded
                )

                if should_exclude:
                    logger.warning(
                        f"Removing {item.recipe_name} on {day.date} "
                        f"- contains excluded ingredient"
                    )
                else:
                    filtered_items.append(item)

            setattr(day, slot, filtered_items)

    return plan
```

**What Gets Enforced:**

| Rule Type | Enforcement |
|-----------|-------------|
| Allergies | Always removed (safety critical) |
| EXCLUDE NEVER | Always removed |
| EXCLUDE SPECIFIC_DAYS | Removed on specified days only |
| Dislikes | Included in prompt (AI avoids) |
| INCLUDE rules | Included in prompt (AI generates) |

---

## Rule Hierarchy

The prompt instructs the AI to follow this priority order:

1. **Allergies** → NEVER include (strict, safety)
2. **EXCLUDE rules** → NEVER include on specified days
3. **INCLUDE rules** → MUST appear at specified frequency
4. **Dislikes** → Avoid when possible
5. **Festivals** → Incorporate special_foods, respect fasting
6. **Pairing** → AI decides within constraints

---

## Output Structure

```python
GeneratedMealPlan(
    week_start_date="2026-01-27",
    week_end_date="2026-02-02",
    days=[
        DayMeals(
            date="2026-01-27",
            day_name="Monday",
            breakfast=[
                MealItem(
                    id="uuid-1",
                    recipe_name="Masala Chai",
                    prep_time_minutes=10,
                    dietary_tags=["vegetarian"],
                    category="chai",
                    recipe_id="AI_GENERATED"
                ),
                MealItem(
                    id="uuid-2",
                    recipe_name="Aloo Paratha",
                    prep_time_minutes=25,
                    dietary_tags=["vegetarian"],
                    category="paratha",
                    recipe_id="AI_GENERATED"
                )
            ],
            lunch=[
                MealItem(recipe_name="Dal Fry", category="dal", ...),
                MealItem(recipe_name="Jeera Rice", category="rice", ...)
            ],
            dinner=[
                MealItem(recipe_name="Paneer Butter Masala", category="curry", ...),
                MealItem(recipe_name="Butter Naan", category="bread", ...)
            ],
            snacks=[
                MealItem(recipe_name="Samosa", category="snack", ...),
                MealItem(recipe_name="Masala Chai", category="chai", ...)
            ],
            festival=None
        ),
        # ... 6 more days
    ],
    rules_applied={
        "include_rules": 3,
        "exclude_rules": 2,
        "allergies_excluded": 1,
        "dislikes_noted": 3
    }
)
```

---

## Key Design Decisions

### Decision #1: AI-Generated Recipes (No Database Lookup)

**Rationale:**
- AI can generate any Indian recipe name without being limited to database entries
- Eliminates complex fallback logic when database lacks matching recipes
- More creative and varied meal suggestions
- Works even with empty/sparse recipe databases

**Trade-offs:**
- No recipe instructions available (recipe_id = "AI_GENERATED")
- Prep times are AI estimates, not database values
- No calorie/nutrition data from database

### Decision #2: Prompt-Based Rule Communication

**Approach:** All user preferences are communicated to the AI in the prompt, not enforced through code filtering.

**Advantages:**
- AI can make intelligent trade-offs
- Natural language understanding of rules
- Handles edge cases gracefully

**Disadvantages:**
- AI may occasionally miss rules (hence post-processing enforcement)

### Decision #3: Post-Processing Enforcement

**Approach:** Critical safety rules (allergies, EXCLUDE) are enforced after AI generation.

**Why:**
- AI is generally reliable but not 100%
- Allergies are safety-critical - must guarantee exclusion
- Simple string matching catches obvious violations
- Better to remove items than serve allergens

### Decision #4: Retry with Exponential Backoff

**Implementation:**
- 3 retry attempts maximum
- Delays: 1s, 2s, 4s (exponential)
- Validates JSON structure before accepting
- Fails with ServiceUnavailableError after all retries

### Decision #5: JSON Response Mode with Schema Enforcement

**Approach:** Use Gemini's `response_json_schema` (plain dict) for structured output with enforced schema.

**Benefits:**
- Guaranteed valid JSON format with correct structure
- Schema enforcement prevents missing fields
- Short property keys avoid Gemini's "too many states" limitation
- No need to extract JSON from text

**Note:** `response_schema` (typed `genai.types.Schema`) was rejected by Gemini 2.5 Flash for complex meal plan schemas. Using `response_json_schema` (plain dict) with abbreviated keys is the working solution.

### Decision #6: Generation Tracker (Structured Logging)

**Approach:** Per-call JSON logging via `generation_tracker.py` for debugging meal generation.

**Output:** `logs/MEAL_PLAN-{timestamp}-{microseconds}.json` with 4 sections: prompt, Gemini response, post-processing, and client response. Includes timing breakdown (AI duration, save duration, total) and token usage.

---

## Comparison: Old vs New Approach

| Aspect | Old (Database Lookup) | New (AI Generation) |
|--------|----------------------|---------------------|
| Recipe source | PostgreSQL (3,580 recipes) | AI generates freely |
| Fallback strategy | 5-level progressive relaxation | Retry with backoff |
| Recipe deduplication | Track used_recipe_ids | AI manages variety |
| Allergen handling | Pre-filter database queries | Post-process removal |
| Cooking time | Filter by prep_time_minutes | AI estimates, prompt guidance |
| Pairing logic | Code-driven category lookup | AI decides pairs |
| Festival handling | Not implemented | Context in prompt |
| Complexity | ~1100 lines | ~1057 lines |

---

## Testing

### Test Coverage

| Test File | Test Class | Tests | Purpose |
|-----------|------------|-------|---------|
| `test_ai_meal_service.py` | TestPromptBuilding | 7 | Verify prompt includes all preferences |
| `test_ai_meal_service.py` | TestResponseParsing | 3 | Verify JSON parsing to dataclasses |
| `test_ai_meal_service.py` | TestValidation | 5 | Verify structure validation |
| `test_ai_meal_service.py` | TestEnforcement | 4 | Verify post-processing rules |
| `test_ai_meal_service.py` | TestIntegration | 6 | Full flow with mocked Gemini |
| `test_gemini_schema.py` | — | 6 | Schema parameter forwarding to Gemini |
| `test_generation_tracker.py` | TestMealGenerationContext | 4 | Dataclass init, timing properties |
| `test_generation_tracker.py` | TestEmitStructuredLog | 7 | JSON file emission, 4-section structure |
| `test_generation_tracker.py` | TestFullPipelineTracking | 3 | End-to-end context population |

### Test Script

```bash
cd backend
source venv/Scripts/activate  # Windows
PYTHONPATH=. pytest tests/services/test_ai_meal_service.py -v
PYTHONPATH=. pytest tests/test_gemini_schema.py -v
PYTHONPATH=. pytest tests/test_generation_tracker.py -v
```

### Validation Test (Tabular)

```bash
cd backend
PYTHONPATH=. python test_meal_api_tabular.py
```

Tests the Sharma Family profile with constraints:
- Chai DAILY in breakfast ✓
- Dal 4x/week ✓
- Mushroom NEVER ✓
- Peanut allergy ✓
- Cooking time limits ✓

---

## Performance

| Metric | Value |
|--------|-------|
| Gemini API call | ~30-40 seconds (with `response_json_schema` + `thinking_budget=0`) |
| Post-AI DB writes | ~1.1 seconds (optimized from ~35-65s) |
| Total generation | ~35-50 seconds |
| Endpoint timeout | 180 seconds (allows 1 retry + backoff) |
| Retry overhead | +1-4 seconds per retry |
| Post-processing | <100ms |

---

## Future Improvements

1. **Recipe linking** - Match AI-generated names to database recipes for instructions
2. **Nutrition estimation** - AI estimates calories based on recipe type
3. **Feedback loop** - Learn from user swaps to improve future suggestions
4. **Multi-language** - Support Hindi recipe names in output
5. **Image generation** - Generate placeholder images for AI recipes
6. **Caching** - Cache meal plans for offline regeneration

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Gemini 404 error | Check model name (`gemini-2.5-flash`), verify API key |
| "Too many states for serving" | Use `response_json_schema` (plain dict) with SHORT property keys, not `response_schema` |
| Empty response | Retry logic handles this; check prompt size |
| Invalid JSON | Validate response structure; retry automatically |
| Missing items after enforcement | AI included allergen; post-processing removed it |
| INCLUDE rule not satisfied | Check prompt format; AI may have misunderstood |
| Generation timeout | Endpoint timeout is 180s; check `logs/MEAL_PLAN-*.json` for timing breakdown |

---

*Last updated: March 2026 - Added structured output (response_json_schema with short keys), generation tracker, thinking disabled*
