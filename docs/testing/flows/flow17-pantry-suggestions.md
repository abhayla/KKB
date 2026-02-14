# Flow 17: Pantry Recipe Suggestions

## Metadata

| Attribute | Value |
|-----------|-------|
| **Flow Name** | pantry-suggestions |
| **Flow ID** | FLOW-17 |
| **Goal** | Test pantry-based recipe suggestions — add ingredients, get recipe matches |
| **Priority** | P1 |
| **Complexity** | Medium |
| **Estimated Duration** | 8-12 minutes |
| **Last Updated** | 2026-02-14 |

## Prerequisites

- Backend API running (`uvicorn app.main:app --reload`)
- PostgreSQL database with schema applied (`alembic upgrade head`)
- Recipes seeded in database (`PYTHONPATH=. python scripts/import_recipes_postgres.py`)
- Android emulator running (API 34)
- Backend pantry suggestion endpoint implemented (Phase 4)
- User authenticated via fake-firebase-token

## Depends On

- **Flow 01** (new-user-journey) — Must complete authentication and onboarding
- **Phase 4** (pantry-suggestions) — Backend logic to match pantry items with recipes

## Test User Persona

**Sharma Family — Pantry Management**

| Field | Value |
|-------|-------|
| Email | `e2e-test@rasoiai.test` |
| Auth Token | `fake-firebase-token` |
| Display Name | Abhay Sharma |
| Dietary Preference | Vegetarian |
| Cuisines | North Indian, South Indian |

**Pantry Items to Test:**

| Pantry State | Ingredients | Expected Recipe Matches |
|--------------|-------------|-------------------------|
| **State 1 (Basic)** | Rice, Dal, Tomatoes, Onions, Turmeric | Dal Tadka, Tomato Rice, Vegetable Pulao |
| **State 2 (South Indian)** | Rice, Urad Dal, Curry Leaves, Mustard Seeds, Coconut | Idli, Sambar, Coconut Chutney |
| **State 3 (Minimal)** | Potatoes, Salt | Boiled Potatoes, Aloo Paratha (partial match) |
| **State 4 (Empty)** | (none) | No suggestions OR empty state message |

## Test Phases

### Phase A: Navigate to Pantry Screen

| Step | Action | Verification |
|------|--------|--------------|
| A1 | Launch app, ensure user authenticated | Home screen displays |
| A2 | Navigate: Settings → Pantry (or Home → overflow menu → Pantry) | Pantry screen displays |
| A3 | Verify screen title: "Pantry" OR "My Pantry" | Title visible |
| A4 | Verify initial state: empty pantry OR existing items from prior session | List visible (may be empty) |
| A5 | Verify "Add Item" button/FAB visible | Button present |
| A6 | Verify "Find Recipes" button visible (may be disabled if pantry empty) | Button present |
| A7 | Screenshot: `flow17_pantry_initial.png` | Screen captured |

### Phase B: Add Pantry Items (State 1 — Basic)

| Step | Action | Verification |
|------|--------|--------------|
| B1 | Tap "Add Item" button | Add pantry item dialog appears |
| B2 | Enter ingredient: "Rice" | Text field populated |
| B3 | Tap "Add" OR "Save" | Dialog closes, "Rice" appears in pantry list |
| B4 | Verify "Rice" card shows: Name, quantity (optional), delete button | Card visible |
| B5 | Repeat B1-B3 for: Dal, Tomatoes, Onions, Turmeric | 5 items total in pantry list |
| B6 | Verify pantry item count: 5 | List has 5 cards |
| B7 | Verify "Find Recipes" button enabled (no longer grayed out) | Button enabled |
| B8 | Screenshot: `flow17_pantry_5_items.png` | 5 items visible |

**Alternative: Backend API Shortcut (for speed):**

Instead of manually adding items via UI (steps B1-B5), use backend API:

```bash
# Get JWT
JWT=$(curl -s -X POST http://localhost:8000/api/v1/auth/firebase \
  -H 'Content-Type: application/json' \
  -d '{"firebase_token":"fake-firebase-token"}' | \
  python -c 'import sys,json;print(json.load(sys.stdin).get("access_token",""))')

# Add pantry items via API
for ITEM in "Rice" "Dal" "Tomatoes" "Onions" "Turmeric"; do
  curl -s -X POST -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    http://localhost:8000/api/v1/pantry \
    -d "{\"ingredient_name\": \"$ITEM\", \"quantity\": 1, \"unit\": \"kg\"}"
  echo "Added: $ITEM"
done
```

Then refresh pantry screen in app to see items.

### Phase C: Find Recipe Suggestions

| Step | Action | Verification |
|------|--------|--------------|
| C1 | Tap "Find Recipes" button | Loading indicator appears |
| C2 | Wait 2-3 seconds for backend to process | Loading completes |
| C3 | Verify recipe suggestion screen/bottom sheet appears | Recipe suggestions list displays |
| C4 | Verify title: "Recipes You Can Make" OR "Suggested Recipes" | Title visible |
| C5 | Verify at least 1 recipe suggestion visible | List has >= 1 recipe card |
| C6 | Verify recipe card shows: Recipe name, Match percentage, Missing ingredients count | All fields present |
| C7 | Verify recipes sorted by match percentage (highest first) | First recipe has highest % |
| C8 | Screenshot: `flow17_suggestions_basic.png` | Suggestions list |

### Phase D: Validate Recipe Match Details

| Step | Action | Verification |
|------|--------|--------------|
| D1 | Tap on first recipe suggestion (highest match) | Recipe Detail screen OR detail sheet appears |
| D2 | Verify recipe name visible (e.g., "Dal Tadka") | Recipe name displayed |
| D3 | Verify ingredients section shows: Available ingredients (green/checkmark), Missing ingredients (red/cross) | Ingredients categorized |
| D4 | Verify match percentage displayed (e.g., "80% match — 4/5 ingredients available") | Percentage + fraction shown |
| D5 | Verify missing ingredients listed (e.g., "Missing: Green Chilies") | Missing list visible |
| D6 | Press BACK to return to suggestions list | Suggestions list displays |
| D7 | Screenshot: `flow17_recipe_match_detail.png` | Match details visible |

### Phase E: Tap Suggested Recipe → Opens Recipe Detail

| Step | Action | Verification |
|------|--------|--------------|
| E1 | From suggestions list, tap second recipe card | Navigation to Recipe Detail screen |
| E2 | Verify Recipe Detail screen loads with full recipe data | Recipe name, image, ingredients, instructions visible |
| E3 | Verify "Start Cooking" button present (recipe is actionable) | Button visible |
| E4 | Verify recipe tags include cuisine and dietary tags | Tags displayed |
| E5 | Press BACK twice to return to Pantry screen | Pantry screen displays |
| E6 | Screenshot: `flow17_recipe_detail_from_suggestion.png` | Recipe detail screen |

### Phase F: Contradiction C38 — Empty Pantry

| Step | Action | Verification |
|------|--------|--------------|
| F1 | Delete all pantry items (tap delete on each card OR use "Clear All" button) | Pantry list empty |
| F2 | Verify pantry empty state: "Your pantry is empty" OR "Add ingredients to get started" | Empty state message visible |
| F3 | Verify "Find Recipes" button disabled OR shows error on tap | Button disabled or shows error dialog |
| F4 | Tap "Find Recipes" (if enabled) | Error dialog: "Add at least 1 ingredient to find recipes" OR no action |
| F5 | Screenshot: `flow17_pantry_empty_error.png` | Empty state/error |

**Backend API Shortcut to Clear Pantry:**

```bash
# Get all pantry items
PANTRY_ITEMS=$(curl -s -H "Authorization: Bearer $JWT" \
  http://localhost:8000/api/v1/pantry | \
  python -c "import sys,json; print(' '.join([str(i['id']) for i in json.load(sys.stdin)]))")

# Delete each item
for ITEM_ID in $PANTRY_ITEMS; do
  curl -s -X DELETE -H "Authorization: Bearer $JWT" \
    http://localhost:8000/api/v1/pantry/$ITEM_ID
done
```

### Phase G: Contradiction C39 — No Recipe Matches

| Step | Action | Verification |
|------|--------|--------------|
| G1 | Add unusual ingredients that won't match any recipe: "Dragon Fruit", "Quinoa", "Kale" | 3 items in pantry |
| G2 | Tap "Find Recipes" | Loading indicator appears |
| G3 | Wait for backend to process | Loading completes |
| G4 | Verify graceful empty state: "No recipes found with these ingredients" OR "Try adding more common ingredients" | Empty state message visible |
| G5 | Verify suggestion to add more items OR browse all recipes | Helpful message/action present |
| G6 | Screenshot: `flow17_no_matches.png` | Empty state with helpful message |

**Backend API to Test C39:**

```bash
# Clear pantry first (see Phase F), then add unusual items
for ITEM in "Dragon Fruit" "Quinoa" "Kale"; do
  curl -s -X POST -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    http://localhost:8000/api/v1/pantry \
    -d "{\"ingredient_name\": \"$ITEM\", \"quantity\": 1, \"unit\": \"kg\"}"
done

# Request suggestions
curl -s -H "Authorization: Bearer $JWT" \
  http://localhost:8000/api/v1/pantry/suggestions | jq '.'
```

Expected response: `[]` (empty array) OR `{"recipes": [], "message": "No matching recipes found"}`

### Phase H: Test Different Pantry States

| Step | Action | Verification |
|------|--------|--------------|
| H1 | Clear pantry, add State 2 ingredients: Rice, Urad Dal, Curry Leaves, Mustard Seeds, Coconut | 5 items added |
| H2 | Tap "Find Recipes" | Suggestions appear: Idli, Sambar, Coconut Chutney |
| H3 | Verify South Indian recipes dominate suggestions (match cuisine) | Recipes match regional cuisine |
| H4 | Screenshot: `flow17_suggestions_south_indian.png` | South Indian recipes |
| H5 | Clear pantry, add State 3 ingredients: Potatoes, Salt (minimal) | 2 items added |
| H6 | Tap "Find Recipes" | Suggestions appear with low match % (e.g., 20-30%) |
| H7 | Verify suggestions include: Aloo Paratha (partial match), Boiled Potatoes | Partial match recipes shown |
| H8 | Verify missing ingredients clearly listed for each suggestion | Missing ingredients visible |
| H9 | Screenshot: `flow17_suggestions_minimal.png` | Partial match recipes |

### Phase I: Backend API Verification

After Phase H, verify backend suggestion algorithm:

```bash
# Get JWT (if not already set)
JWT=$(curl -s -X POST http://localhost:8000/api/v1/auth/firebase \
  -H 'Content-Type: application/json' \
  -d '{"firebase_token":"fake-firebase-token"}' | \
  python -c 'import sys,json;print(json.load(sys.stdin).get("access_token",""))')

# Get pantry items
curl -s -H "Authorization: Bearer $JWT" \
  http://localhost:8000/api/v1/pantry | jq '.'

# Get recipe suggestions
curl -s -H "Authorization: Bearer $JWT" \
  http://localhost:8000/api/v1/pantry/suggestions | jq '.'

# Verify suggestion response format
curl -s -H "Authorization: Bearer $JWT" \
  http://localhost:8000/api/v1/pantry/suggestions | \
  python -c "
import sys, json
suggestions = json.load(sys.stdin)
print(f'Total suggestions: {len(suggestions)}')
if suggestions:
    top = suggestions[0]
    print(f'Top recipe: {top[\"recipe_name\"]}')
    print(f'Match %: {top[\"match_percentage\"]}')
    print(f'Missing: {top.get(\"missing_ingredients\", [])}')
"
```

## Contradictions

This flow tests pantry-specific contradictions:

| ID | Contradiction | Setup | Expected Behavior |
|----|---------------|-------|-------------------|
| **C38** | Empty pantry | User deletes all pantry items, taps "Find Recipes" | Error dialog: "Add at least 1 ingredient to find recipes" OR button disabled |
| **C39** | No recipe matches | User adds unusual ingredients (Dragon Fruit, Quinoa, Kale) | Graceful empty state: "No recipes found. Try adding more common ingredients like rice, dal, or vegetables." |
| **C46** | Duplicate pantry item | User adds "Rice" twice | Second add shows error: "Rice is already in your pantry" OR increments quantity |

## Fix Strategy

**Relevant files for this flow:**

- **Android:**
  - `app/presentation/pantry/PantryScreen.kt` — UI with item list, Add button, Find Recipes button
  - `app/presentation/pantry/PantryViewModel.kt` — State management
  - `app/presentation/pantry/SuggestionsSheet.kt` — Recipe suggestions bottom sheet/screen
  - `domain/model/PantryItem.kt` — Domain model
  - `domain/model/RecipeSuggestion.kt` — Suggestion model with match percentage
  - `data/local/entity/PantryItemEntity.kt` — Room entity
  - `data/local/dao/PantryDao.kt` — Database queries
  - `data/repository/PantryRepositoryImpl.kt` — Repository

- **Backend:**
  - `app/api/v1/endpoints/pantry.py` — CRUD + suggestions endpoint
  - `app/models/pantry_item.py` — SQLAlchemy model
  - `app/services/pantry_service.py` — Business logic (CRUD, suggestions algorithm)
  - `app/services/recipe_service.py` — Recipe search/matching logic

**Common issues:**

- **No suggestions returned:** Verify `pantry_service.get_recipe_suggestions()` correctly matches pantry ingredients with recipe ingredients
- **Match percentage incorrect:** Ensure calculation: `(pantry_ingredients ∩ recipe_ingredients) / total_recipe_ingredients * 100`
- **Missing ingredients not listed:** Verify response includes `missing_ingredients` field: `recipe_ingredients - pantry_ingredients`
- **Empty pantry not handled:** Add validation in `PantryViewModel.findRecipes()` to check pantry.isEmpty() before API call
- **C39 empty state not shown:** Verify SuggestionsSheet handles empty response array with custom empty state UI

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Pantry | A1-A7, B1-B8, F1-F5, G1-G6, H1-H9 | Item list, Add items, Delete items, Find Recipes button |
| Recipe Suggestions (Sheet/Screen) | C1-C8, D1-D7, F4, G4-G6, H2-H9 | Suggestions list, match %, missing ingredients |
| Recipe Detail | E1-E6 | Full recipe from suggestion |

## Test Data Cleanup

After test completion:

```bash
# Remove test user and all pantry items
PYTHONPATH=. python scripts/cleanup_user.py
```

## Backend Suggestion Algorithm

The pantry suggestion algorithm (Phase 4) works as follows:

1. **Input:** User's pantry items (list of ingredient names)
2. **Process:**
   - Query all recipes in database
   - For each recipe, compare recipe ingredients with pantry items
   - Calculate match percentage: `matched_ingredients / total_recipe_ingredients * 100`
   - Identify missing ingredients: `recipe_ingredients - pantry_items`
   - Filter recipes with match >= 20% (configurable threshold)
   - Sort by match percentage (descending)
3. **Output:** List of recipe suggestions with:
   - `recipe_id`, `recipe_name`, `match_percentage`, `missing_ingredients[]`, `recipe_image_url`

**Example Response:**

```json
[
  {
    "recipe_id": "rec-123",
    "recipe_name": "Dal Tadka",
    "match_percentage": 80,
    "missing_ingredients": ["Green Chilies"],
    "recipe_image_url": "https://..."
  },
  {
    "recipe_id": "rec-456",
    "recipe_name": "Tomato Rice",
    "match_percentage": 75,
    "missing_ingredients": ["Curry Leaves", "Cashews"],
    "recipe_image_url": "https://..."
  }
]
```

## Notes

- Pantry items stored in `pantry_items` table (PostgreSQL) and `pantry_item_entities` (Room)
- Ingredient name matching is case-insensitive and normalized (singular form)
- Ingredient aliases handled via `config/reference_data/ingredients.yaml` (e.g., "Curd" = "Yogurt")
- Match threshold: 20% minimum (at least 1/5 ingredients match)
- Future enhancement: Track pantry item quantities and expiration dates, suggest recipes before ingredients expire
- Future enhancement: Smart shopping — add missing ingredients directly to grocery list from suggestions screen
