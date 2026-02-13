# Flow 02: Existing User

## Metadata
- **Flow Name:** `existing-user`
- **Goal:** Verify data persistence after app restart and generate a third meal plan with new settings
- **Preconditions:** User exists with meal plan #2 from Flow 1
- **Estimated Duration:** 8-12 minutes
- **Screens Covered:** Splash, Home, Settings, Recipe Detail
- **Depends On:** `new-user-journey` (needs existing user state with plan #2)
- **State Produced:** User with updated settings and meal plan #3

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] Flow 1 (`new-user-journey`) completed — user has Vegetarian diet, 3 items/meal, Mild spice
- [ ] Do NOT run `cleanup_user.py` between Flow 1 and Flow 2
- [ ] Backend running

## Test User Persona

**Sharma Family — Existing State (from Flow 1):**

| Field | Current Value |
|-------|--------------|
| Primary Diet | Vegetarian (changed in Flow 1) |
| Cuisines | North Indian, South Indian, East Indian |
| Spice Level | Mild |
| Disliked | Karela, Lauki, Capsicum |
| Items Per Meal | 3 |
| Weekday Cooking Time | 30 minutes |
| Weekend Cooking Time | 60 minutes |
| Busy Days | Monday, Wednesday |

**Settings Changes (for meal plan #3):**

| Setting | Changed To |
|---------|-----------|
| Weekday Cooking Time | 45 minutes |
| Busy Days | Remove Monday, Add Friday → Wednesday, Friday |
| Allow Recipe Repeat | ON |

## Steps

### Phase A: App Restart & Persistence Check (Steps 1-7)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| A1 | Force-stop app: `$ADB shell am force-stop $APP_PACKAGE` | App killed | — | — |
| A2 | Relaunch: `$ADB shell am start -n $APP_PACKAGE/$APP_ACTIVITY` | App starts | — | — |
| A3 | Wait 5s, dump UI | Home screen (NOT auth/onboarding) — user is remembered | `flow02_restart_home.png` | — |
| A4 | Verify meal plan still loaded | text="This Week's Menu", BREAKFAST, recipe names present | — | — |
| A5 | Verify meal data matches plan #2 | Recipe names in cards (compare with Flow 1 plan #2 if possible) | — | — |
| A6 | Tap a day tab (e.g., WED) | Wednesday meals displayed | — | — |
| A7 | Verify no onboarding prompt | No "Tell us about your household" text | — | — |

### Phase B: Verify Plan #2 Still Valid (Steps 8-10)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| B1 | Run V4a-V4k on current plan | Matches Vegetarian, 3 items, Mild expectations | — | V4a-V4k |
| B2 | Spot-check: scroll to DINNER | DINNER section has recipe names | — | — |
| B3 | Scroll to SNACKS | SNACKS section has recipe names | — | — |

### Checkpoint 1: Verify Plan #2 Persistence
```bash
python scripts/validate_meal_plan.py \
  --jwt "$JWT" \
  --checks V4a,V4b,V4c,V4d,V4e \
  --expected-diet "Vegetarian" \
  --expected-items-per-meal 3 \
  --disliked "Karela,Lauki,Capsicum" \
  --output "$LOG_DIR/flow02_validation1.json"
```

### Backend API Cross-Validation: Plan #2 Persistence

```bash
# Direct API check: verify plan #2 still exists on backend
PLAN_DATA=$(curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/meal-plans/current)
echo "$PLAN_DATA" | python -c "
import sys, json
d = json.load(sys.stdin)
days = d.get('days', [])
print(f'Plan has {len(days)} days')
for day in days[:2]:
    meals = day.get('meals', {})
    for slot in ['breakfast', 'lunch', 'dinner', 'snacks']:
        items = meals.get(slot, [])
        print(f'  {day.get(\"date\", \"?\")} {slot}: {len(items)} items')
"
```

### Phase C: Settings Changes (Steps 11-17)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| C1 | Navigate: tap Profile icon | Settings screen | `flow02_settings.png` | — |
| C2 | Verify current settings match Flow 1 changes | Vegetarian, Mild, 3 items/meal visible | — | — |
| C3 | Find weekday cooking time setting | Current value: 30 minutes | — | — |
| C4 | Change weekday cooking time to 45 minutes | Updated to 45 | — | — |
| C5 | Find busy days setting | Currently: Monday, Wednesday | — | — |
| C6 | Change busy days: remove Monday, add Friday | Now: Wednesday, Friday | — | — |
| C7 | Find "Allow Recipe Repeat" toggle | Currently OFF | — | — |
| C8 | Toggle to ON | Allow repeats = ON | `flow02_settings_updated.png` | — |
| C9 | Navigate back to Home | Home screen | — | — |

### Backend API Cross-Validation: Settings Update

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/users/me | \
  python -c "
import sys, json
d = json.load(sys.stdin)
print(f'weekday_cooking_time: {d.get(\"weekday_cooking_time\")} (expected: 45)')
print(f'busy_days: {d.get(\"busy_days\")} (expected: [Wednesday, Friday])')
print(f'allow_repeats: {d.get(\"allow_repeats\")} (expected: True)')
"
```

### Phase D: Third Meal Plan Generation (Steps 18-23)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| D1 | Tap Refresh/Regenerate | Refresh options appear | — | — |
| D2 | Select "Entire Week" | Generation starts | — | — |
| D3 | Wait for generation (up to 90s) | New meal plan loads | `flow02_home_plan3.png` | — |
| D4 | Verify meal cards updated | Different recipes from Plan #2 | — | — |
| D5 | Run V4a-V4k validation | Reflects 45min weekday, new busy days, repeats allowed | — | V4a-V4k |
| D6 | Spot-check a Recipe Detail | Tap a meal card → View Recipe → verify ingredients | `flow02_recipe_spot.png` | — |

### Checkpoint 2: After Plan #3 Generation
```bash
python scripts/validate_meal_plan.py \
  --jwt "$JWT" \
  --checks V4a,V4b,V4c,V4d,V4e,V4f,V4g,V4h,V4i,V4j,V4k \
  --expected-diet "Vegetarian" \
  --expected-items-per-meal 3 \
  --disliked "Karela,Lauki,Capsicum" \
  --cuisines "North Indian,South Indian,East Indian" \
  --max-spice "Mild" \
  --weekday-cooking-time 45 \
  --weekend-cooking-time 60 \
  --busy-days "Wednesday,Friday" \
  --allow-repeats \
  --output "$LOG_DIR/flow02_validation2.json"
```

## Validation Checkpoints

1. **Checkpoint 1 (Phase B):** Verify plan #2 persisted correctly after app restart
2. **Checkpoint 2 (Phase D):** New plan #3 reflects updated settings (45min weekday, new busy days)

## Fix Strategy

**Relevant files for this flow:**
- Persistence issues: `data/local/datastore/AppSettingsDataStore.kt`, `data/local/dao/MealPlanDao.kt`
- Room cache: `data/local/RasoiDatabase.kt`
- Settings sync: `data/repository/SettingsRepositoryImpl.kt`
- Auth token persistence: `data/local/datastore/AuthDataStore.kt`

**Common issues:**
- App shows auth screen after restart → auth token not persisted in DataStore
- Meal plan blank after restart → Room cache empty, check MealPlanDao
- Settings reset to defaults → DataStore not saving, check SettingsRepository

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Splash | A2 | Auto-transitions (no auth required) |
| Home | A3-A7, D3-D4 | Persistence, day tabs, plan #3 |
| Settings | C1-C8 | Persistence, setting changes |
| Recipe Detail | D6 | Spot-check after regeneration |
