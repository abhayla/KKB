# Flow 05: Grocery Management

## Metadata
- **Flow Name:** `grocery-management`
- **Goal:** Test grocery list display, categories, checkboxes, sharing, and stale data handling (C13)
- **Preconditions:** User has a meal plan (grocery list auto-generated from plan)
- **Estimated Duration:** 4-6 minutes
- **Screens Covered:** Grocery, Home
- **Depends On:** none (needs any meal plan)
- **State Produced:** Some grocery items checked off

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] User has at least one meal plan (grocery list auto-derived)
- [ ] App on Home screen

## Test User Persona

Uses existing Sharma family data. No settings changes in this flow.

## Steps

### Phase A: Grocery List Exploration (Steps 1-4)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| A1 | Tap bottom nav "Grocery" | Grocery screen loads | `flow05_grocery.png` | — |
| A2 | Verify screen title | "Grocery List" or "Grocery" visible | — | — |
| A3 | Verify categories exist | At least 3 category headers (Vegetables, Spices, Dairy, Grains, etc.) | — | — |
| A4 | Verify grocery items | Ingredient names with quantities visible | — | — |

### Phase B: Category Interaction (Steps 5-7)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| B1 | Tap a category header (e.g., "Vegetables") | Category collapses or expands | — | — |
| B2 | Tap the same category again | Category toggles back | — | — |
| B3 | Scroll down to see more categories | Additional categories visible below fold | `flow05_grocery_scrolled.png` | — |

### Phase C: Checkbox Interaction (Steps 8-11)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| C1 | Find a grocery item checkbox | Checkbox visible next to ingredient | — | — |
| C2 | Tap checkbox to mark purchased | Item marked (strikethrough or checked) | `flow05_item_checked.png` | — |
| C3 | Tap a second item checkbox | Second item marked | — | — |
| C4 | Tap first checkbox again to uncheck | Item unchecked (strikethrough removed) | — | — |

### Phase D: WhatsApp Share (Steps 12-13)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| D1 | Find Share/WhatsApp button | Button visible (content-desc "Share" or "WhatsApp") | — | — |
| D2 | Tap Share button | Share intent opens (WhatsApp or system share sheet) | `flow05_share.png` | — |
| D3 | Press BACK to dismiss share sheet | Return to Grocery screen | — | — |

### Phase E: Contradiction C13 — Stale Grocery (Steps 14-17)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| E1 | **C13 Setup:** Check ALL visible items as purchased | All items show checked/strikethrough | — | — |
| E2 | Navigate to Home (bottom nav "Home") | Home screen | — | — |
| E3 | Regenerate meal plan (Refresh → "Entire Week") | New plan generates | — | — |
| E4 | Wait for generation (up to 90s) | New plan loaded | — | — |
| E5 | Navigate to Grocery | Grocery screen | `flow05_grocery_after_regen.png` | — |
| E6 | **C13 Verify:** Check if grocery list reset for new plan | Items should be unchecked for new recipes, list reflects new plan | — | — |

## Validation Checkpoints

No `validate_meal_plan.py` checkpoints — validation is UI-based:
- Grocery items correspond to meal plan ingredients
- Checkbox state toggles correctly
- Share intent launches
- Grocery list resets after plan regeneration

## Fix Strategy

**Relevant files for this flow:**
- Grocery screen: `app/presentation/grocery/GroceryViewModel.kt`, `GroceryScreen.kt`
- Grocery DAO: `data/local/dao/GroceryDao.kt`
- Grocery generation: `data/repository/GroceryRepositoryImpl.kt`
- Share functionality: `app/presentation/grocery/GroceryScreen.kt` (share intent)

**Common issues:**
- Grocery list empty → meal plan might not have generated grocery items, check GroceryDao
- Checkbox state not persisting → Room not saving checked state
- WhatsApp not installed → share sheet shows other options, not a failure
- Grocery not updating after regen → check if grocery generation triggers on new plan

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Grocery | A1-A4, B1-B3, C1-C4, D1-D3, E5-E6 | Categories, checkboxes, share, regen |
| Home | E2-E4 | Plan regeneration trigger |
