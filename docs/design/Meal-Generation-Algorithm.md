# Meal Generation Algorithm

This document describes the detailed implementation of RasoiAI's meal plan generation algorithm.

**Related:** [Meal-Generation-Config-Architecture.md](./Meal-Generation-Config-Architecture.md) - Config structure and chat integration

---

## Overview

The algorithm generates a **7-day personalized meal plan** with 4 meal slots per day (breakfast, lunch, dinner, snacks), each containing **2 complementary items** (e.g., Dal + Rice).

```
┌─────────────────────────────────────────────────────────────────────┐
│                    MealGenerationService                             │
├─────────────────────────────────────────────────────────────────────┤
│  Inputs:                                                            │
│  ├── User Preferences (Firestore)                                   │
│  ├── Recipe Database (3,590 recipes)                                │
│  └── Config Rules (YAML → Firestore)                                │
│                                                                     │
│  Outputs:                                                           │
│  └── GeneratedMealPlan (7 days × 4 slots × 2 items = 56 items)     │
└─────────────────────────────────────────────────────────────────────┘
```

**Source Code:** `backend/app/services/meal_generation_service.py`

---

## Data Structures

### MealItem
```python
@dataclass
class MealItem:
    id: str
    recipe_id: str
    recipe_name: str
    recipe_image_url: Optional[str]
    prep_time_minutes: int = 30
    calories: int = 0
    is_locked: bool = False
    dietary_tags: list[str]
    category: str
```

### UserPreferences
```python
@dataclass
class UserPreferences:
    dietary_tags: list[str]           # ["vegetarian"]
    cuisine_type: Optional[str]       # "north"
    allergies: list[dict]             # [{"ingredient": "peanuts", "severity": "SEVERE"}]
    dislikes: list[str]               # ["karela", "lauki"]
    weekday_cooking_time: int         # 30 min
    weekend_cooking_time: int         # 60 min
    busy_days: list[str]              # ["MONDAY", "WEDNESDAY"]
    include_rules: list[dict]         # INCLUDE rules from recipe_rules
    exclude_rules: list[dict]         # EXCLUDE rules from recipe_rules
    nutrition_goals: list[dict]       # NUTRITION_GOAL rules
```

### GeneratedMealPlan
```python
@dataclass
class GeneratedMealPlan:
    week_start_date: str              # "2026-01-27"
    week_end_date: str                # "2026-02-02"
    days: list[DayMeals]              # 7 days
    rules_applied: dict               # Summary of rules applied
```

---

## Algorithm Phases

### Phase 1: Initialization

```python
# 1. Load config (pairing rules from YAML/Firestore)
config = await self.config_service.get_config()

# 2. Load user preferences
prefs = await self._load_user_preferences(user_id)

# 3. Build tracking structures
used_recipe_ids: set[str] = set()           # Avoid duplicate recipes across week
include_tracker = self._build_include_tracker()  # Track INCLUDE rule fulfillment
```

**Include Tracker Structure:**
```python
include_tracker = {
    "0": {
        "rule": {...},
        "target": "Chai",
        "meal_slots": ["breakfast"],
        "times_needed": 7,          # DAILY = 7
        "times_assigned": 0,
        "frequency": "DAILY"
    },
    "1": {
        "rule": {...},
        "target": "Dal",
        "meal_slots": ["lunch", "dinner"],
        "times_needed": 4,          # 4x per week
        "times_assigned": 0,
        "frequency": "TIMES_PER_WEEK"
    }
}
```

---

### Phase 2: Build Exclusion List

Creates a comprehensive set of ingredients that must **never appear** in any recipe.

```python
def _build_exclude_list(self, prefs: UserPreferences) -> set[str]:
    exclude = set()

    # 1. EXCLUDE rules with NEVER frequency
    for rule in prefs.exclude_rules:
        if rule.get("frequency") == "NEVER":
            target = rule.get("target", "").lower()
            exclude.add(target)
            # Add singular/plural variants
            if target.endswith("s"):
                exclude.add(target[:-1])    # peanuts -> peanut
            else:
                exclude.add(target + "s")   # mushroom -> mushrooms

    # 2. Allergies (CRITICAL - expanded to variants)
    for allergy in prefs.allergies:
        ingredient = allergy.get("ingredient", "").lower()
        exclude.add(ingredient)
        # Expand to known variants
        if ingredient in allergen_variants:
            exclude.update(allergen_variants[ingredient])

    # 3. Dislikes
    for dislike in prefs.dislikes:
        exclude.add(dislike.lower())

    return exclude
```

**Allergen Variant Expansion:**

| Allergen | Variants Added |
|----------|----------------|
| `peanut` | peanuts, groundnut, groundnuts, moongphali |
| `dairy` | milk, cheese, paneer, curd, yogurt, cream, butter, ghee |
| `gluten` | wheat, maida, atta, bread, roti, naan |
| `shellfish` | shrimp, prawn, crab, lobster |
| `tree nuts` | almond, cashew, walnut, pistachio, kaju, badam |

---

### Phase 3: Daily Generation Loop

```
for day_index in range(7):  # Monday → Sunday
    │
    ├── Determine cooking time limit
    │   ├── Weekday: weekday_cooking_time (default 30 min)
    │   ├── Weekend: weekend_cooking_time (default 60 min)
    │   └── Busy day: weekday_cooking_time (overrides weekend)
    │
    ├── Build per-day tracking
    │   └── used_ingredients_today: set[str]  # Avoid same ingredient in lunch AND dinner
    │
    └── for slot in [breakfast, lunch, dinner, snacks]:
            │
            ├── Adjust slot cooking time
            │   └── Dinner gets minimum 45 min (ensures good options)
            │
            ├── Process INCLUDE rules
            │   └── If matched → add item + find complementary pair
            │
            └── If no INCLUDE → Generate default paired meal
```

**Cooking Time Logic:**
```python
max_time = prefs.weekday_cooking_time
if is_busy_day:
    max_time = prefs.weekday_cooking_time
elif is_weekend:
    max_time = prefs.weekend_cooking_time

# Special case: Dinner gets minimum 45 min
if slot == "dinner" and max_time < 45:
    slot_max_time = 45
```

---

### Phase 4: INCLUDE Rule Processing

The algorithm tracks INCLUDE rules across the week to ensure fulfillment.

**Assignment Logic by Frequency:**

| Frequency | Assignment Strategy |
|-----------|---------------------|
| `DAILY` | Always assign, allow recipe reuse |
| `TIMES_PER_WEEK` | Spread evenly across week |
| Fallback | Force assignment if running out of days |

**TIMES_PER_WEEK Distribution:**
```python
# For Dal 4x/week:
times_needed = 4
times_assigned = tracker["times_assigned"]
days_remaining = 7 - day_index
times_remaining = times_needed - times_assigned

# Must assign if running out of days
if days_remaining <= times_remaining:
    should_assign = True

# Otherwise assign on evenly spaced days (0, 2, 4, 6 for 4x/week)
elif (day_index % max(1, 7 // times_needed)) == 0:
    should_assign = True
```

**Search Strategy for INCLUDE Rules:**

```
DAILY Rules (e.g., Chai):
├── Search with cuisine filter, no meal_type filter, allow reuse
├── If empty → Retry without cuisine filter
└── Select random from results

NON-DAILY Rules (e.g., Dal 4x/week):
├── Search with cuisine + meal_type + exclude used recipes
├── If empty → Retry without meal_type filter
└── Select random from results
```

**Why DAILY rules allow reuse:**
- Morning Chai is expected to be the same recipe (it's just chai)
- Prevents running out of recipes for items needed every day
- Makes the meal plan more realistic

---

### Phase 5: 2-Item Pairing Logic

Each meal slot gets **2 complementary items** based on Indian cuisine traditions.

**Config-Driven Pairs:**
```python
meal_type_pairs = {
    "breakfast": ["paratha:chai", "poha:chai", "idli:sambar", "dosa:chutney"],
    "lunch": ["dal:rice", "sabzi:roti", "curry:rice"],
    "dinner": ["dal:roti", "sabzi:paratha", "curry:rice", "dal:rice"],
    "snacks": ["snack:chai", "snack:chutney"]
}
```

**Pairing Process:**

```
Scenario 1: INCLUDE rule matched (e.g., "Dal")
├── Add Dal recipe to slot
└── Find complementary item using config lookup
    └── Dal → pairs with: [rice, roti]
    └── Search for Rice or Roti recipe

Scenario 2: No INCLUDE rule
├── Pick random pair from config (e.g., "dal:rice")
├── Search for Dal recipe as primary
└── Search for Rice recipe as accompaniment
```

**Complementary Item Categories:**
```python
default_pairs = {
    "dal": ["rice", "roti"],
    "sabzi": ["roti", "paratha"],
    "curry": ["rice", "naan"],
    "dosa": ["sambar", "chutney"],
    "idli": ["sambar", "chutney"],
    "paratha": ["chai", "curd"],
    "poha": ["chai"],
    "rice": ["dal", "sambar"],
}
```

---

### Phase 6: Recipe Filtering Pipeline

Every recipe candidate passes through multiple filter layers:

```
Recipe Candidate
     │
     ├── 1. Dietary filter
     │   └── Must match: vegetarian, vegan, jain, etc.
     │
     ├── 2. Cuisine filter
     │   └── Prefer: north, south, east, west
     │
     ├── 3. Cooking time filter
     │   └── Must be ≤ max_cooking_time for the day/slot
     │
     ├── 4. Exclusion filter
     │   ├── Check recipe name for excluded ingredients
     │   └── Check ingredients list for excluded items
     │
     ├── 5. Used recipe filter
     │   └── Exclude recipes already used this week
     │
     └── 6. Daily ingredient filter
         └── Exclude if main ingredient already used today
```

**Exclusion Check Implementation:**
```python
def _recipe_matches_excludes(self, recipe: dict, exclude_ingredients: set[str]) -> bool:
    # Check recipe name
    name = recipe.get("name", "").lower()
    for excluded in exclude_ingredients:
        if excluded in name:
            return True  # Recipe contains excluded ingredient

    # Check ingredients list
    for ing in recipe.get("ingredients", []):
        ing_name = ing.get("name", "").lower() if isinstance(ing, dict) else str(ing).lower()
        for excluded in exclude_ingredients:
            if excluded in ing_name:
                return True

    return False
```

**Daily Ingredient Tracking:**
```python
main_ingredients = [
    "rajma", "chole", "dal", "paneer", "aloo", "gobi", "palak",
    "bhindi", "baingan", "matar", "chana", "moong", "toor",
    "idli", "dosa", "paratha", "roti", "rice", "biryani", "pulao",
    "sambar", "rasam", "curry", "sabzi", "khichdi", "poha", "upma"
]

# If "Rajma" is in lunch, don't allow "Rajma Curry" in dinner
```

---

### Phase 7: Fallback Strategies

When recipe search fails, the algorithm progressively relaxes constraints:

```
Level 1: Full filters
├── cuisine_type + meal_type + cooking_time + dietary + excludes
│
Level 2: Remove meal_type filter
├── Some recipes don't have proper meal_type tags
│
Level 3: Remove cuisine_type filter
├── Expand to all cuisines if preferred cuisine lacks options
│
Level 4: Remove cooking time limit
├── For slots that are still empty
│
Level 5: For DAILY rules - Allow recipe reuse
└── Same Chai recipe can appear every day
```

**Fallback Code Pattern:**
```python
# Primary search
recipes = await self.recipe_repo.search_by_ingredient(
    ingredient=target,
    cuisine_type=prefs.cuisine_type,
    meal_type=slot,
    max_time_minutes=max_cooking_time,
    exclude_ids=used_recipe_ids,
)

# Fallback 1: Remove meal_type
if not recipes:
    recipes = await self.recipe_repo.search_by_ingredient(
        ingredient=target,
        cuisine_type=prefs.cuisine_type,
        meal_type=None,  # Removed
        max_time_minutes=max_cooking_time,
        exclude_ids=used_recipe_ids,
    )

# Fallback 2: Remove cuisine_type
if not recipes:
    recipes = await self.recipe_repo.search_by_ingredient(
        ingredient=target,
        cuisine_type=None,  # Removed
        meal_type=None,
        max_time_minutes=max_cooking_time,
        exclude_ids=used_recipe_ids,
    )

# Fallback 3: For DAILY rules, allow reuse
if not recipes and frequency == "DAILY":
    recipes = await self.recipe_repo.search_by_ingredient(
        ingredient=target,
        cuisine_type=None,
        meal_type=None,
        max_time_minutes=max_cooking_time,
        exclude_ids=None,  # Allow reuse
    )
```

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
                MealItem(recipe_name="Masala Chai", category="beverage", ...),
                MealItem(recipe_name="Aloo Paratha", category="paratha", ...)
            ],
            lunch=[
                MealItem(recipe_name="Dal Fry", category="dal", ...),
                MealItem(recipe_name="Jeera Rice", category="rice", ...)
            ],
            dinner=[
                MealItem(recipe_name="Paneer Butter Masala", category="curry", ...),
                MealItem(recipe_name="Butter Naan", category="roti", ...)
            ],
            snacks=[
                MealItem(recipe_name="Samosa", category="snack", ...),
                MealItem(recipe_name="Green Chutney", category="chutney", ...)
            ]
        ),
        # ... 6 more days
    ],
    rules_applied={
        "include_rules": 3,
        "exclude_rules": 2,
        "nutrition_goals": 0,
        "allergies_excluded": 1,
        "dislikes_excluded": 3
    }
)
```

---

## Key Design Decisions

### Decision #1: Meal Structure (Main + Complementary Items)

**Structure:** Each meal slot contains Main item(s) + Complementary item(s) for each main.

| Aspect | Decision |
|--------|----------|
| **Structure** | Main item(s) + Complementary item(s) for each main |
| **Complementary** | Always added (never skip), even for complete dishes like Biryani |
| **Generic items** | Both main and complementary can be suggested without database recipe (marked as "No recipe - make your own") |

**Number of Main Items (Based on Cooking Time):**

| Cooking Time | Default Meal |
|--------------|--------------|
| ≤ 30 min | 1 main + complementary (simple) |
| 30-45 min | 2 mains + complementary (full) |
| > 45 min | 2+ mains + complementary (elaborate) |

**User Preference Options (all supported):**

| Setting | Description |
|---------|-------------|
| **Items per meal (B)** | Range like "2-3 items", "3-4 items", "4+ items" |
| **Per meal-type (C)** | Different config for breakfast/lunch/dinner/snacks |
| **Override time (D)** | "Always full meals" or "Always simple" |

**Priority:** Per meal-type (C) > Items per meal (B) > Override (D) > Cooking Time Logic

**Complementary Selection Logic:**
1. Check what's already in the meal (avoid duplicates - variety first)
2. From remaining options, pick based on cuisine preference (North = Roti, South = Rice)
3. If variety not possible, allow duplicate (better to repeat than skip)
4. Database recipes and generic suggestions treated equally

**Pairing Configuration (config-driven):**
```python
pairings = {
    "dal": ["rice", "roti", "paratha"],
    "sabzi": ["roti", "paratha", "rice"],
    "curry": ["rice", "naan", "roti"],
    "biryani": ["raita", "salad"],
    "dosa": ["sambar", "chutney"],
    "khichdi": ["papad", "pickle", "curd"],
}
```

---

### Decision #2: Weekly Deduplication

**Approach:** User configurable preference for recipe repetition.

**Configuration Options:**

| Setting | Options | Default |
|---------|---------|---------|
| Global toggle (A) | "Allow recipe repeats" ON/OFF | OFF |
| Per item type - Main (D) | "Main items can repeat" ON/OFF | OFF |
| Per item type - Complementary (D) | "Complementary can repeat" ON/OFF | ON |
| Per INCLUDE rule | "Allow repeat for this rule" checkbox | OFF |

**Default behavior:** Main items no repeat, Complementary can repeat.

**Priority:** Per rule > Per item type (D) > Global toggle (A)

**Logic Flow:**
```
1. Check per-rule "Allow repeat" setting
   └── If ON → Allow repeat for this rule
2. Check per item type setting (D)
   └── Main item → Check "Main items can repeat"
   └── Complementary → Check "Complementary can repeat"
3. Global toggle (A) only applies if D not configured
```

---

### Decision #3: Daily Ingredient Tracking

**Approach:** Same main ingredient cannot appear twice on same day (e.g., no Rajma at lunch AND dinner).

| Aspect | Decision |
|--------|----------|
| **Tracking scope** | Only main items tracked, accompaniments can repeat |
| **Classification method** | Based on recipe category |

**Category Classification:**

| Main Item Categories | Accompaniment Categories |
|---------------------|-------------------------|
| dal | rice |
| sabzi | roti |
| curry | paratha |
| biryani | naan |
| pulao | chutney |
| khichdi | raita |
| dosa | sambar |
| idli | pickle |
| poha | papad |
| upma | salad |
| paneer_dish | beverage |
| egg_dish | chai |

**Logic:**
```
1. When adding recipe to meal slot:
   └── Get recipe category
   └── If category is "main item":
       └── Check if ingredient already used today
       └── If yes → skip, find alternative
       └── If no → add to used_ingredients_today
   └── If category is "accompaniment":
       └── Allow (no tracking)
```

---

### Decision #4: DAILY Rules Allow Reuse

**Status:** Merged into Decision #2.

DAILY rules (like "Chai every morning") follow the same per-rule "Allow repeat" checkbox from Decision #2. No special treatment - user enables repeat if needed.

---

### Decision #5: Allergen Variant Expansion

**Approach:** User can optionally enable regional variant expansion for allergens.

| Aspect | Decision |
|--------|----------|
| **Default** | Auto-expand OFF - only exact term excluded |
| **UI** | Simple toggle to enable/disable variants |
| **Extendable** | Variants list can be updated over time |

**Allergen Variants Map:**

```python
allergen_variants = {
    "peanut": ["peanuts", "groundnut", "groundnuts", "moongphali"],
    "dairy": ["milk", "cheese", "paneer", "curd", "yogurt", "cream", "butter", "ghee"],
    "gluten": ["wheat", "maida", "atta", "bread", "roti", "naan"],
    "shellfish": ["shrimp", "prawn", "crab", "lobster"],
    "tree nuts": ["almond", "cashew", "walnut", "pistachio", "kaju", "badam"],
    "soy": ["soya", "soybean", "soybeans", "tofu", "soy sauce", "soya chunks"],
    "egg": ["eggs", "anda", "omelette", "egg white", "egg yolk"],
    "sesame": ["til", "sesame seeds", "tahini", "gingelly"],
}
```

**User Flow:**
```
1. User adds allergy: "Peanuts"
2. System saves: peanuts (exact term only)
3. User sees toggle: "Include regional variants (groundnut, moongphali)"
4. If enabled → expands to all variants
5. Can disable anytime in settings
```

---

### Decision #6: Cooking Time Minimums

**Approach:** User configurable - Global minimum + meal-type override.

| Setting | Default |
|---------|---------|
| Global minimum | 15 min |
| Dinner override | Enabled, 45 min |
| Other meal overrides | Disabled |

**Configuration:**

```
Cooking Time Settings:
├── Global minimum: [15] min (applies to all meals)
└── Meal-type overrides:
    ├── ☑ Dinner minimum: [45] min
    ├── ☐ Lunch minimum: [__] min
    ├── ☐ Breakfast minimum: [__] min
    └── ☐ Snacks minimum: [__] min
```

**Logic:**
```
1. Get user's cooking time for the day (weekday/weekend/busy)
2. Apply global minimum (max of user time vs global min)
3. Apply meal-type override if enabled (max of step 2 vs meal min)
4. Final cooking time = result of step 3
```

---

### Decision #7: Progressive Fallbacks

**Approach:** Progressive fallbacks for database search, generic suggestion as final option.

| Aspect | Decision |
|--------|----------|
| **Dietary tags** | User configurable "Strict dietary" toggle (default ON - never relax) |
| **Allergies** | Never relax - safety critical |
| **Dislikes** | Can relax as last resort, with warning to user |
| **Final fallback** | Suggest generic item (no database recipe) |

**Fallback Sequence:**

```
Level 1: Full filters (cuisine + meal_type + time + dietary + excludes)
    ↓ (if empty)
Level 2: Remove meal_type filter
    ↓ (if empty)
Level 3: Remove cuisine_type filter
    ↓ (if empty)
Level 4: Remove cooking time limit
    ↓ (if empty)
Level 5: Relax dislikes (with warning: "Could not avoid [item]")
    ↓ (if empty)
Level 6: Suggest generic item (no database recipe)

NEVER RELAX:
- Allergies (safety critical)
- Dietary tags (if "Strict dietary" ON)
```

**User Settings:**

| Setting | Default | Effect |
|---------|---------|--------|
| Strict dietary | ON | Never relax vegetarian/vegan/jain |
| Strict dietary | OFF | Can relax dietary as last resort with warning |

---

## Performance Considerations

| Aspect | Approach |
|--------|----------|
| **Recipe search** | Uses Firestore queries with filters, limited to needed results |
| **Caching** | Config is loaded once per generation request |
| **Deduplication** | `used_recipe_ids` set provides O(1) lookup |
| **Ingredient tracking** | Simple string matching on recipe names |

---

## Testing

### Test Data: Sharma Family Profile

```python
SHARMA_FAMILY = {
    "household_size": 4,
    "family_members": [
        {"name": "Ramesh", "type": "ADULT", "age": 45},
        {"name": "Sunita", "type": "ADULT", "age": 42, "special_needs": ["LOW_OIL"]},
        {"name": "Arjun", "type": "CHILD", "age": 16, "special_needs": ["HIGH_PROTEIN"]},
        {"name": "Priya", "type": "CHILD", "age": 12},
    ],
    "dietary_tags": ["vegetarian"],
    "cuisine_preferences": ["north", "west"],
    "allergies": [{"ingredient": "peanuts", "severity": "SEVERE"}],
    "disliked_ingredients": ["karela", "lauki", "turai"],
    "weekday_cooking_time_minutes": 30,
    "weekend_cooking_time_minutes": 60,
    "busy_days": ["MONDAY", "WEDNESDAY"],
    "recipe_rules": [
        {"type": "INCLUDE", "target": "Chai", "frequency": "DAILY", "meal_slot": ["breakfast"]},
        {"type": "INCLUDE", "target": "Dal", "frequency": "TIMES_PER_WEEK", "times_per_week": 4, "meal_slot": ["lunch", "dinner"]},
        {"type": "INCLUDE", "target": "Paneer", "frequency": "TIMES_PER_WEEK", "times_per_week": 2, "meal_slot": ["lunch", "dinner"]},
        {"type": "EXCLUDE", "target": "Mushroom", "frequency": "NEVER"},
        {"type": "EXCLUDE", "target": "Onion", "frequency": "SPECIFIC_DAYS", "specific_days": ["TUESDAY"]},
    ],
}
```

### Validation Checklist

| Rule | Expected | Pass Criteria |
|------|----------|---------------|
| Chai (INCLUDE DAILY) | 7 in breakfast | Count >= 7 |
| Dal (INCLUDE 4x/week) | 4 in lunch/dinner | Count >= 4 |
| Paneer (INCLUDE 2x/week) | 2 in lunch/dinner | Count >= 2 |
| Mushroom (EXCLUDE) | 0 anywhere | Count = 0 |
| Peanuts (ALLERGY) | 0 anywhere | Count = 0 |
| Dislikes (karela/lauki/turai) | 0 anywhere | Count = 0 |
| 2-item pairing | All 28 slots have 2 items | No single-item slots |
| Dietary compliance | All vegetarian | No non-veg recipes |
| Cooking time | Weekday ≤30m, Weekend ≤60m | All within limits |

### Test Script

```bash
cd backend
source venv/Scripts/activate  # Windows
python test_meal_api_tabular.py
```

---

## Future Improvements

1. **Nutrition tracking** - Track weekly nutrition goals (protein, iron, etc.)
2. **Festival menus** - Auto-suggest festival-appropriate meals
3. **Leftovers handling** - Suggest using yesterday's dal for lunch
4. **Seasonal ingredients** - Prefer in-season vegetables
5. **Cost optimization** - Balance expensive and budget recipes

---

*Last updated: January 29, 2026 - Key Design Decisions reviewed and approved*
