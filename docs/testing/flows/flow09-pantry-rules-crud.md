# Flow 09: Pantry & Rules CRUD

## Metadata
- **Flow Name:** `pantry-rules-crud`
- **Goal:** Test CRUD operations on Pantry items and Recipe Rules, plus contradictions C22-C27
- **Preconditions:** User authenticated with access to Pantry and Recipe Rules screens
- **Estimated Duration:** 12-18 minutes
- **Screens Covered:** Settings, Pantry, Recipe Rules, offline-mode
- **Depends On:** none (needs authenticated user)
- **State Produced:** New pantry items and recipe rules created/modified

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] User authenticated
- [ ] Backend running (Recipe Rules require backend sync)

## Test User Persona

Uses existing Sharma family data. CRUD operations on rules and pantry.

## Steps

### Phase A: Pantry CRUD (Steps 1-9)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| A1 | Navigate: Profile icon → Settings → scroll to "Pantry" | Pantry link visible | UI | — | — |
| A2 | Tap "Pantry" | Pantry screen loads | UI | `flow09_pantry.png` | — |
| A3 | Verify initial state | Empty or existing pantry items | UI | — | — |
| A3a | Verify Camera/Scan button exists | content-desc "Camera" or "Scan" visible in XML | UI | — | — |
| A3b | Verify Gallery button exists | content-desc "Gallery" visible in XML | UI | — | — |
| A3c | Verify "Find Recipes" button exists | text "Find Recipes" visible (may need scroll) | UI | — | — |
| A4 | Tap Add button ("+" or "Add Item") | Add item dialog/sheet appears | UI | `flow09_pantry_add.png` | — |
| A5 | Type "Turmeric" and confirm | Turmeric added to pantry | UI | — | — |
| A6 | Add second item: "Cumin Seeds" | Cumin Seeds added | UI | — | — |
| A7 | Add third item: "Basmati Rice" | Three items in pantry | UI | `flow09_pantry_items.png` | — |
| A8 | Delete one item (swipe or tap delete) | Item removed from list | UI | — | — |
| A9 | Verify deletion | Only 2 items remain | UI | — | — |

### Phase B: Recipe Rules CRUD (Steps 10-20)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| B1 | Press BACK to Settings, scroll to "Recipe Rules" | Recipe Rules link visible | UI | — | — |
| B2 | Tap "Recipe Rules" | Recipe Rules screen loads | UI | `flow09_rules.png` | — |
| B3 | Verify "Rules" tab selected | Rules tab active | UI | — | — |
| B4 | Tap Add rule button ("+" or "Add Rule") | Add rule bottom sheet appears | UI | `flow09_add_rule.png` | — |
| B5 | Create INCLUDE rule: "Dal Tadka" for DINNER | Fill action=INCLUDE, search for Dal Tadka, select DINNER | UI | — | — |
| B6 | Confirm/Save rule | Rule appears in list | UI | `flow09_rule_added.png` | — |
| B6a | Verify toggle switch on new rule | Switch/toggle visible on Dal Tadka rule card | UI | — | — |
| B6b | Tap toggle to disable rule | Rule becomes inactive (toggle off) | UI | — | — |
| B6c | Tap toggle again to re-enable | Rule becomes active (toggle on) | UI | — | — |
| B7 | Create EXCLUDE rule: "Karela Sabzi" for ANY meal | Fill action=EXCLUDE, target=Karela Sabzi | UI | — | — |
| B8 | Confirm/Save rule | Second rule in list | UI | — | — |
| B9 | Tap Nutrition tab | Nutrition Goals tab shows | UI | `flow09_nutrition_tab.png` | — |
| B10 | Verify Nutrition tab loads | Goals list or empty state | UI | — | — |
| B11 | Tap back to Rules tab | Rules tab active | UI | — | — |

### Backend API Cross-Validation: Rule CRUD

```bash
# Verify rules created on backend
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/recipe-rules | \
  python -c "
import sys, json
d = json.load(sys.stdin)
rules = d if isinstance(d, list) else d.get('rules', [])
print(f'Total rules: {len(rules)}')
include_rules = [r for r in rules if r.get('action') == 'INCLUDE']
exclude_rules = [r for r in rules if r.get('action') == 'EXCLUDE']
print(f'  INCLUDE: {len(include_rules)} (expected: at least 1 - Dal Tadka)')
print(f'  EXCLUDE: {len(exclude_rules)} (expected: at least 1 - Karela Sabzi)')
for r in rules:
    print(f'  {r.get(\"action\")}: {r.get(\"target_name\")} for {r.get(\"meal_slot\", \"ANY\")}')
"
```

### Phase C: Contradictions C22-C27 (Steps 21-35)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| C1 | **C22:** Try to add INCLUDE "Dal Tadka" + EXCLUDE "Dal Tadka" | Tap Add, create EXCLUDE for "Dal Tadka" | UI | — | — |
| C2 | Attempt to save | 409 CONFLICT or warning — same item can't be both INCLUDE and EXCLUDE | UI | `flow09_c22_conflict.png` | — |
| C3 | Dismiss error/dialog | Return to rules list | UI | — | — |
| C4 | **C23:** Try to add same rule twice (INCLUDE "Dal Tadka" for DINNER again) | Tap Add, recreate identical rule | UI | — | — |
| C5 | Attempt to save | 409 CONFLICT, snackbar "already exists" | UI | `flow09_c23_duplicate.png` | — |
| C5a | **Backend verification:** Confirm only 1 Dal Tadka INCLUDE rule exists | curl check below | API | — | — |
| C6 | Dismiss error | Return to rules list | UI | — | — |

### Backend API Cross-Validation: Duplicate Prevention

```bash
# Verify no duplicate rules on backend
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/recipe-rules | \
  python -c "
import sys, json
d = json.load(sys.stdin)
rules = d if isinstance(d, list) else d.get('rules', [])
dal_rules = [r for r in rules if 'dal' in r.get('target_name', '').lower() and 'tadka' in r.get('target_name', '').lower()]
print(f'Dal Tadka rules count: {len(dal_rules)} (expected: 1, not 2)')
if len(dal_rules) == 1:
    print('Duplicate prevention -> PASS')
else:
    print('WARNING: Duplicate rules exist!')
"
```

| C7 | **C24:** Create INCLUDE rule for "Karela" (which is in disliked list) | Tap Add, INCLUDE "Karela" | UI | — | — |
| C8 | Attempt to save | Warning about dislike conflict, or allows (INCLUDE overrides dislike) | UI | `flow09_c24_dislike.png` | — |
| C9 | Note behavior | Document how app handles rule-vs-dislike conflict | UI | — | — |
| C10 | **C25:** Try to add rule with very long name (100+ chars) | Type: "Super Extra Special Homemade Traditional Grandmother Style North Indian Dal Tadka With Extra Ghee Tempering..." | UI | — | — |
| C11 | Attempt to save | Accepts (truncated if needed) or shows validation error | UI | `flow09_c25_long.png` | — |
| C12 | **C26:** Add 5+ INCLUDE rules rapidly | Add rules for: Poha, Paratha, Idli, Dosa, Chole | UI | — | — |
| C13 | Verify all stored | All 5+ new rules in list | UI | `flow09_c26_many_rules.png` | — |
| C14 | Scroll through rules list | All rules visible | UI | — | — |
| C15 | **C27:** Delete ALL rules | Delete each rule one by one (or bulk delete if available) | UI | — | — |
| C16 | Verify empty state | "No rules" or empty list shown | UI | `flow09_c27_empty.png` | — |

### Phase D: Cleanup (Steps 36-37)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| D1 | Optionally re-add a basic rule for later flows | INCLUDE "Chai" for BREAKFAST | UI | — | — |
| D2 | Press BACK to Settings, then BACK to Home | Home screen | UI | — | — |

### Phase E: Offline Rule Creation (Steps E1-E8)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| E1 | Enable airplane mode (ADB: `adb shell settings put global airplane_mode_on 1; adb shell am broadcast -a android.intent.action.AIRPLANE_MODE`) | Device offline | API | — | — |
| E2 | Tap Add Rule | Add sheet opens (works offline) | UI | — | — |
| E3 | Create INCLUDE "Offline Chai" DAILY REQUIRED | Rule form filled | UI | — | — |
| E4 | Save rule | Rule saves to Room DB (local) | UI | `flow09_offline_rule.png` | — |
| E5 | Verify rule card displayed | "Offline Chai" visible in list | UI | — | — |
| E6 | **C59:** Disable airplane mode (`adb shell settings put global airplane_mode_on 0; adb shell am broadcast -a android.intent.action.AIRPLANE_MODE`) | Device online | API | — | — |
| E7 | Wait 5 seconds for sync | Background sync triggers | API | — | — |
| E8 | Verify backend has rule | curl check shows "Offline Chai" | API | — | Offline create syncs on reconnect |

### Backend API Cross-Validation: Offline Sync

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/recipe-rules | \
  python -c "
import sys, json
d = json.load(sys.stdin)
rules = d if isinstance(d, list) else d.get('rules', [])
offline = [r for r in rules if 'offline' in r.get('target_name', '').lower()]
if offline:
    print(f'Offline rule synced -> PASS: {offline[0][\"target_name\"]}')
else:
    print('WARNING: Offline rule not found on backend')
"
```

### Phase F: Offline Rule Update (Steps F1-F6)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| F1 | Enable airplane mode | Device offline | API | — | — |
| F2 | Tap existing rule to edit | Edit sheet opens | UI | — | — |
| F3 | Change enforcement: REQUIRED → PREFERRED | Enforcement updated locally | UI | — | — |
| F4 | Save rule | Rule updates in Room DB | UI | — | — |
| F5 | **C60:** Disable airplane mode | Device online, sync triggers | API | — | Client timestamp newer = client wins |
| F6 | Verify backend reflects updated enforcement | curl check shows PREFERRED | API | — | — |

### Phase G: Offline Rule Deletion (Steps G1-G4)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| G1 | Enable airplane mode | Device offline | API | — | — |
| G2 | Delete a rule via three-dot menu | Rule removed from local list | UI | — | — |
| G3 | **C61:** Disable airplane mode | Device online, sync triggers | API | — | Offline delete syncs to backend |
| G4 | Verify backend no longer has deleted rule | curl check confirms deletion | API | — | — |

## Validation Checkpoints

No `validate_meal_plan.py` checkpoints — validation is CRUD-focused:
- Create: items/rules appear in list after creation
- Read: existing items/rules display correctly
- Update: rule modifications persist (if supported)
- Delete: items/rules removed from list
- Contradictions: proper error handling (409, warnings, not crashes)

## Fix Strategy

**Relevant files for this flow:**
- Pantry: `app/presentation/pantry/PantryViewModel.kt`, `PantryScreen.kt`
- Pantry DAO: `data/local/dao/PantryDao.kt`
- Recipe Rules: `app/presentation/reciperules/RecipeRulesViewModel.kt`, `RecipeRulesScreen.kt`
- Rules API: `backend/app/api/v1/endpoints/recipe_rules.py`
- Duplicate detection: `backend/app/api/v1/endpoints/recipe_rules.py` (dup_query)
- Android duplicate: `data/local/dao/RecipeRulesDao.kt` (findDuplicate)

**Common issues:**
- 409 not shown as snackbar → ViewModel not catching HTTP 409
- Add dialog doesn't dismiss → check sheet state management
- Rules not syncing → check sync endpoint / OfflineQueue
- Pantry items vanish on restart → Room Dao issue
- Long names crash layout → text overflow not handled

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Settings | A1, B1, D2 | Navigation to Pantry/Rules |
| Pantry | A2-A9 | CRUD operations |
| Recipe Rules | B2-B11, C1-C16, D1 | CRUD + contradictions C22-C27 |
