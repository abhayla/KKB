# Flow 07: Edge Cases

## Metadata
- **Flow Name:** `edge-cases`
- **Goal:** Test rapid navigation, back stack, crash recovery, and boundary conditions (C16-C21)
- **Preconditions:** User authenticated with meal plan
- **Estimated Duration:** 5-8 minutes
- **Screens Covered:** Home, Grocery, Chat, Favorites, Stats, Settings, Recipe Detail
- **Depends On:** none (needs authenticated user)
- **State Produced:** None (stress test, no persistent state changes)

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] User authenticated with meal plan
- [ ] App on Home screen

## Test User Persona

Uses existing Sharma family data. This flow tests stability, not functionality.

## Steps

### Phase A: Rapid Navigation (Steps 1-5)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| A1 | Rapid-tap bottom nav: Home → Grocery → Chat → Favs → Stats (1s each) | Each screen appears briefly | — | — |
| A2 | Rapid-tap back: Stats → Home via bottom nav | Returns to Home | — | — |
| A3 | Run crash/ANR detection (Pattern 9) | No crashes from rapid nav | — | — |
| A4 | Tap Grocery → immediately tap Home | No navigation crash | — | — |
| A5 | Tap Chat → type text → immediately tap Home | No crash, keyboard dismissed | `flow07_rapid_nav.png` | — |

### Phase B: Back Stack Testing (Steps 6-9)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| B1 | Navigate: Home → Settings → Pantry | Pantry screen | — | — |
| B2 | Press BACK | Returns to Settings | — | — |
| B3 | Press BACK | Returns to Home | — | — |
| B4 | Press BACK again | App minimizes or shows "Press back again to exit" | — | — |
| B5 | Relaunch app if minimized | Home screen restored | — | — |
| B6 | Navigate: Home → meal card → View Recipe → Start Cooking | Cooking Mode | — | — |
| B7 | Press BACK | Returns to Recipe Detail | — | — |
| B8 | Press BACK | Returns to Home | — | — |
| B9 | Run crash/ANR detection | No crashes | `flow07_back_stack.png` | — |

### Phase C: Contradictions C16-C21 (Steps 10-21)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| C1 | **C16:** Navigate to Settings, set ALL 7 days as busy | All days selected (Mon-Sun) | — | — |
| C2 | Navigate to Home, verify no crash | App handles extreme busy days | `flow07_c16_all_busy.png` | — |
| C3 | Revert: remove all busy days (or set back to Mon, Wed) | Reverted | — | — |
| C4 | **C17:** Set weekday cooking time to minimum (15 min if possible) | Minimum time set | — | — |
| C5 | Verify app doesn't crash with tight constraint | Settings saves | — | — |
| C6 | Revert cooking time to 30 min | Reverted | — | — |
| C7 | **C18:** Navigate to Settings → Cuisine preferences | Cuisine screen | — | — |
| C8 | Deselect ALL cuisines | Empty selection | — | — |
| C9 | Verify behavior: error message or default fallback | No crash | `flow07_c18_no_cuisine.png` | — |
| C10 | Reselect North Indian | Restored | — | — |
| C11 | **C19:** Navigate to Home, tap Refresh/Regenerate | Refresh options | — | — |
| C12 | Tap "Entire Week" then IMMEDIATELY tap again | Second tap should be ignored/queued | `flow07_c19_double_tap.png` | — |
| C13 | Wait for generation or verify double-tap blocked | Single generation, not duplicate | — | — |
| C14 | **C20:** Tap a meal card → "View Recipe" | Recipe Detail loads | — | — |
| C15 | Press BACK within 0.5 seconds | Clean return to Home, no crash | — | — |
| C16 | **C21:** Navigate to Settings, scroll to "Sign Out" | Sign Out button visible | — | — |
| C17 | Tap "Sign Out" | Confirmation dialog | — | — |
| C18 | Tap "Sign Out" to confirm | App navigates to Auth screen | `flow07_c21_signout.png` | — |
| C19 | Sign back in (tap "Sign in with Google") | Fake auth completes | — | — |
| C20 | Wait for Home screen | Home loads with preserved data | `flow07_c21_resignin.png` | — |
| C21 | Verify data preserved | Meal plan, preferences still intact | — | — |
| C21a | **Backend verification:** meal plan preserved | curl check below | — | — |
| C21b | **Backend verification:** preferences intact | curl check below | — | — |

### Backend API Cross-Validation: Sign-Out/In Data Preservation

```bash
# Verify meal plan still exists after sign-out/sign-in cycle
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/meal-plans/current | \
  python -c "
import sys, json
d = json.load(sys.stdin)
days = d.get('days', [])
print(f'Meal plan days: {len(days)} (expected: 7)')
if len(days) == 7:
    print('Meal plan preserved after sign-out/in -> PASS')
else:
    print('WARNING: Meal plan data may be lost')
"

# Verify preferences intact
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/users/me | \
  python -c "
import sys, json
d = json.load(sys.stdin)
print(f'dietary_type: {d.get(\"dietary_type\")}')
print(f'spice_level: {d.get(\"spice_level\")}')
print(f'cuisines: {d.get(\"cuisines\")}')
print('Preferences intact -> PASS' if d.get('dietary_type') else 'FAIL: preferences missing')
"
```

### Phase D: Final Stability Check (Steps 22-23)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| D1 | Run crash/ANR detection (Pattern 9) | No crashes detected | — | — |
| D2 | Capture logcat errors (Pattern 13) | Log saved for review | — | — |

## Validation Checkpoints

No `validate_meal_plan.py` checkpoints — validation is stability-focused:
- No crashes during rapid navigation
- Back stack works correctly (proper screen ordering)
- Boundary conditions don't crash the app
- Sign out → sign in preserves data

## Fix Strategy

**Relevant files for this flow:**
- Navigation: `app/presentation/navigation/RasoiNavGraph.kt`, `Screen.kt`
- Back handling: Individual screen composables
- Sign out: `app/presentation/settings/SettingsViewModel.kt`
- Generation debounce: `app/presentation/home/HomeViewModel.kt`
- Busy days: `app/presentation/settings/screens/CookingTimeScreen.kt`

**Common issues:**
- Rapid nav crash → missing DisposableEffect cleanup in ViewModels
- Back stack corruption → NavController popBackStack issues
- Double-tap generation → missing debounce/loading guard
- Sign out not clearing data → DataStore/Room not cleared properly

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Home | A1-A5, C2, C11-C13, C20-C21 | Rapid nav, regenerate |
| Grocery | A1 | Rapid nav target |
| Chat | A1, A5 | Rapid nav + keyboard |
| Favorites | A1 | Rapid nav target |
| Stats | A1-A2 | Rapid nav target |
| Settings | B1, C1-C10, C16-C17 | Back stack, edge settings |
| Pantry | B1-B2 | Back stack |
| Recipe Detail | B6-B7, C14-C15 | Back stack, premature back |
| Cooking Mode | B6-B7 | Back stack |
| Auth | C18-C19 | Sign out/in cycle |
