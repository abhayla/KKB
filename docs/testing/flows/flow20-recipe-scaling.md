# Flow 20: Recipe Scaling

## Metadata
- **Flow Name:** `recipe-scaling`
- **Goal:** Test recipe scaling — adjust servings, verify ingredient quantity changes, test edge cases
- **Preconditions:** Backend running with scale endpoint, at least one meal plan with recipes
- **Estimated Duration:** 5-8 minutes
- **Screens Covered:** Recipe Detail, Grocery
- **Depends On:** Flow 01 (authenticated user with meal plan)
- **State Produced:** Scaled recipe with modified ingredient quantities

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] User authenticated
- [ ] At least 1 meal plan with recipes exists
- [ ] Recipe scaling endpoint `/api/v1/recipes/{id}/scale` implemented
- [ ] Grocery "Add to List" uses scaled quantities

## Test User Persona

Uses existing Sharma family data. Focus is on recipe scaling accuracy, not user preferences.

## Steps

### Phase A: Navigate to Recipe Detail (Steps 1-4)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| A1 | Navigate to Home screen | Current week meal plan displayed | — | — |
| A2 | Tap a BREAKFAST meal card (e.g., Poha) | Action sheet appears | — | — |
| A3 | Tap "View Recipe" | Recipe Detail screen loads | `flow20_recipe_detail.png` | HARD |
| A4 | Verify servings selector exists | Servings dropdown/stepper visible (e.g., "Servings: 4") | — | HARD |

### Phase B: Note Default Servings & Ingredients (Steps 5-8)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| B1 | Read default servings | e.g., "4 servings" | — | — |
| B2 | Scroll to Ingredients section | Ingredients list visible | — | — |
| B3 | Note first 3 ingredient quantities | e.g., "2 cups Rice", "1 tbsp Oil", "1/2 tsp Salt" | `flow20_default_servings.png` | — |
| B4 | Record baseline for comparison | Write down: servings=4, Rice=2 cups, Oil=1 tbsp, Salt=0.5 tsp | — | — |

### Phase C: Increase Servings (Steps 9-13)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| C1 | Tap servings selector/stepper | Servings picker opens OR stepper +/- visible | — | — |
| C2 | Change servings from 4 to 8 | Servings value updates to "8 servings" | — | HARD |
| C3 | Wait for ingredient recalculation (may be instant or require API call) | Loading indicator OR instant update | — | — |
| C4 | Verify ingredients doubled | Rice=4 cups (was 2), Oil=2 tbsp (was 1), Salt=1 tsp (was 0.5) | `flow20_servings_8.png` | HARD |
| C5 | Scroll through all ingredients | All quantities scaled correctly (multiplied by 2) | — | HARD |

### Backend API Cross-Validation: Scaled Recipe

```bash
# Get recipe ID from UI (visible in logs or URL if using web)
RECIPE_ID="test-recipe-id"

# Scale recipe to 8 servings via API
curl -s -H "Authorization: Bearer $JWT" \
  "http://localhost:8000/api/v1/recipes/${RECIPE_ID}/scale?servings=8" | \
  python -c "
import sys, json
d = json.load(sys.stdin)
servings = d.get('servings', 0)
ingredients = d.get('ingredients', [])
print(f'Servings: {servings} (expected: 8)')
print('Ingredients:')
for ing in ingredients[:3]:  # First 3 ingredients
    name = ing.get('name', 'unknown')
    qty = ing.get('quantity', 0)
    unit = ing.get('unit', '')
    print(f'  - {name}: {qty} {unit}')
# Verify servings = 8
if servings == 8:
    print('Scaling -> PASS')
else:
    print('WARNING: Servings not scaled correctly')
"
```

### Phase D: Decrease Servings (Steps 14-18)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| D1 | Tap servings selector again | Picker opens | — | — |
| D2 | Change servings from 8 to 2 | Servings value updates to "2 servings" | — | HARD |
| D3 | Wait for ingredient recalculation | Loading or instant update | — | — |
| D4 | Verify ingredients halved from original | Rice=1 cup (was 2), Oil=0.5 tbsp (was 1), Salt=0.25 tsp (was 0.5) | `flow20_servings_2.png` | HARD |
| D5 | Verify fractional quantities display correctly | "1/2 tbsp" or "0.5 tbsp", "1/4 tsp" or "0.25 tsp" | — | HARD |

### Phase E: Grocery Integration with Scaled Quantities (Steps 19-23)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| E1 | With servings still at 2, tap "Add to Grocery" button | Add to grocery confirmation or instant add | — | HARD |
| E2 | Navigate to Grocery screen (bottom nav) | Grocery screen loads | `flow20_grocery.png` | — |
| E3 | Find Rice in grocery list | Rice item visible | — | HARD |
| E4 | Verify Rice quantity is 1 cup (scaled value, not original 2 cups) | Grocery item shows "1 cup" | — | HARD |
| E5 | Navigate back to Home | Home screen | — | — |

### Phase F: Contradictions C40-C42 (Steps 24-35)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| F1 | Open same recipe again | Recipe Detail loads | — | — |
| F2 | **C40:** Set servings to 1 (minimum) | Servings selector allows minimum value | — | HARD |
| F3 | Verify ingredients show fractional values | Rice=0.5 cups ("1/2 cup"), Oil=0.25 tbsp ("1/4 tbsp"), Salt=0.125 tsp ("1/8 tsp") | `flow20_c40_servings_1.png` | HARD |
| F4 | Verify no "0" quantities or negative values | All ingredients have positive quantities | — | HARD |
| F5 | **C41:** Set servings to 12 (maximum or very large) | Servings selector allows large value | — | HARD |
| F6 | Verify ingredients show large quantities | Rice=6 cups (12/4 * 2), Oil=3 tbsp, Salt=1.5 tsp | `flow20_c41_servings_12.png` | HARD |
| F7 | Verify no integer overflow or display issues | All quantities displayed correctly (not "NaN" or overflow) | — | HARD |
| F8 | Scroll through entire ingredient list | All ingredients scaled, no missing items | — | HARD |
| F9 | **C42:** Reset servings to original (4) | Servings value changes to 4 | — | HARD |
| F10 | Verify ingredients return to baseline | Rice=2 cups, Oil=1 tbsp, Salt=0.5 tsp (matches Phase B) | `flow20_c42_reset.png` | HARD |

### Backend API Cross-Validation: Edge Case Scaling

```bash
# Test minimum servings (1)
curl -s -H "Authorization: Bearer $JWT" \
  "http://localhost:8000/api/v1/recipes/${RECIPE_ID}/scale?servings=1" | \
  python -c "
import sys, json
d = json.load(sys.stdin)
servings = d.get('servings', 0)
ingredients = d.get('ingredients', [])
print(f'Minimum Servings: {servings} (expected: 1)')
# Check for fractional quantities
for ing in ingredients[:3]:
    qty = ing.get('quantity', 0)
    if qty > 0:
        print(f'  {ing.get(\"name\")}: {qty} {ing.get(\"unit\")} -> PASS (positive)')
    else:
        print(f'  WARNING: Zero or negative quantity for {ing.get(\"name\")}')
"

# Test maximum servings (12)
curl -s -H "Authorization: Bearer $JWT" \
  "http://localhost:8000/api/v1/recipes/${RECIPE_ID}/scale?servings=12" | \
  python -c "
import sys, json
d = json.load(sys.stdin)
servings = d.get('servings', 0)
ingredients = d.get('ingredients', [])
print(f'Maximum Servings: {servings} (expected: 12)')
# Check for reasonable quantities (no overflow)
for ing in ingredients[:3]:
    qty = ing.get('quantity', 0)
    if qty > 0 and qty < 10000:  # Arbitrary large threshold
        print(f'  {ing.get(\"name\")}: {qty} {ing.get(\"unit\")} -> PASS (reasonable)')
    else:
        print(f'  WARNING: Unreasonable quantity for {ing.get(\"name\")}: {qty}')
"
```

### Phase G: Additional Edge Cases (Steps 36-40)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| G1 | Set servings to 5 (odd number, not 2^n) | Servings changes to 5 | — | — |
| G2 | Verify fractional scaling accurate | Rice=2.5 cups (5/4 * 2), Salt=0.625 tsp (5/4 * 0.5) | `flow20_odd_servings.png` | HARD |
| G3 | Verify decimal display readable | "2.5 cups" or "2 1/2 cups" (not "2.5000000") | — | HARD |
| G4 | Change servings multiple times rapidly | Tap stepper +/- 5 times quickly | — | — |
| G5 | Verify UI stable, no crash | Final servings value correct, no ANR | — | HARD |

### Phase H: Cleanup (Steps 41-42)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| H1 | Press BACK to Home | Home screen | `flow20_final.png` | — |
| H2 | Run crash/ANR detection (Pattern 9) | No crashes during scaling operations | — | HARD |

## Validation Checkpoints

No `validate_meal_plan.py` checkpoints — validation is recipe scaling-focused:
- **Scaling Math:** Ingredient quantities scale proportionally (servings * original_qty / original_servings)
- **Fractional Display:** Fractions rendered as "1/2" or "0.5" (not "0.5000000")
- **Edge Cases:** Minimum (1), maximum (12), odd numbers (5) all work
- **Grocery Integration:** Scaled quantities propagate to grocery list
- **UI Stability:** No crashes on rapid changes

## Fix Strategy

**Relevant files for this flow:**
- Recipe Detail UI: `app/presentation/recipedetail/RecipeDetailViewModel.kt`, `RecipeDetailScreen.kt`
- Servings selector: `app/presentation/recipedetail/components/ServingsSelector.kt` (if implemented)
- Backend scaling: `backend/app/api/v1/endpoints/recipes.py` (GET `/recipes/{id}/scale`)
- Recipe service: `backend/app/services/recipe_service.py` (`scale_recipe()`)
- Grocery integration: `app/presentation/grocery/GroceryViewModel.kt`

**Common issues:**
- Ingredients don't update after servings change → ViewModel not re-fetching scaled recipe
- Fractional quantities display as long decimals → Number formatting missing (use `DecimalFormat` or `%.2f`)
- Grocery gets original quantities, not scaled → "Add to Grocery" not using current servings state
- Servings selector doesn't allow minimum/maximum → Missing validation or hardcoded limits
- Crash on rapid changes → ViewModel not debouncing/throttling API calls
- API 500 on scale endpoint → Backend not handling edge cases (servings=0, negative, etc.)
- Ingredient quantities become "0.0" → Scaling math error (division by zero or incorrect ratio)

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Home | A1, E5, H1 | Navigation |
| Recipe Detail | A3-A4, B1-B4, C1-C5, D1-D5, F1-F10, G1-G5 | Servings selector, ingredient scaling |
| Grocery | E2-E4 | Scaled quantities in grocery list |

## Contradictions

| ID | Description | Steps | Expected Outcome |
|----|-------------|-------|------------------|
| C40 | Servings = 1 (minimum) | F2-F4 | Fractional quantities handled (e.g., "1/2 cup", "1/4 tsp") |
| C41 | Servings = 12 (maximum) | F5-F8 | Large quantities display correctly, no overflow |
| C42 | Reset servings to original | F9-F10 | Ingredients return to baseline values |
