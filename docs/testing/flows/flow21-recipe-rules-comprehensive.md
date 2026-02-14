# Flow 21: Recipe Rules Comprehensive

## Metadata
- **Flow Name:** `recipe-rules-comprehensive`
- **Goal:** Test rule editing, frequency switching, meal slot modes, diet conflicts, search edge cases, rule sorting, all 8 nutrition categories, progress tracking
- **Preconditions:** User authenticated with existing onboarding data
- **Estimated Duration:** 15-20 minutes
- **Screens Covered:** Recipe Rules (both tabs)
- **Depends On:** Flow 01 (authenticated user)
- **State Produced:** Multiple rules and nutrition goals created/modified/paused

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] User authenticated
- [ ] Backend running (Recipe Rules require backend sync)
- [ ] User has vegetarian diet preference set (for diet conflict tests)

## Test User Persona

Uses existing Sharma family vegetarian profile. Tests rule editing lifecycle, all 8 nutrition categories, and edge cases.

## Known ADB Limitations & Workarounds

### Room DB Fallback Pattern

Compose number fields with `readOnly=true` (frequency count, weekly target) and `ExposedDropdownMenu` selectors cannot be edited via ADB `input tap` or `input text`. Use Room DB direct updates after creating items with default values.

```bash
# After creating a rule with default frequency count via UI:
RULE_ID=$($ADB shell "run-as com.rasoiai.app sqlite3 databases/rasoi_database \
  'SELECT id FROM recipe_rules ORDER BY createdAt DESC LIMIT 1;'" | tr -d '\r')

# Update frequency count directly in Room DB
$ADB shell "run-as com.rasoiai.app sqlite3 databases/rasoi_database \
  'UPDATE recipe_rules SET frequencyCount=3 WHERE id=\"$RULE_ID\";'"

# For nutrition goals weekly target:
GOAL_ID=$($ADB shell "run-as com.rasoiai.app sqlite3 databases/rasoi_database \
  'SELECT id FROM nutrition_goals ORDER BY createdAt DESC LIMIT 1;'" | tr -d '\r')

$ADB shell "run-as com.rasoiai.app sqlite3 databases/rasoi_database \
  'UPDATE nutrition_goals SET weeklyTarget=5 WHERE id=\"$GOAL_ID\";'"
```

After Room DB update, force-stop and restart app to reload data from Room.

### ANR Prevention

Rapid toggle operations (pausing multiple rules) now use offline queue pattern (`Dispatchers.IO` + `OfflineQueueDao`). If ANR is still detected during rapid toggles, it indicates a regression — auto-classify as `ISSUE_FOUND` and invoke `/fix-loop`.

## Steps

### Phase A: Setup — Navigate & Create Base Rules (Steps A1-A6)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| A1 | Navigate: Profile icon → Settings → Recipe Rules | Recipe Rules screen loads | — | — |
| A2 | Verify 2 tabs: Rules, Nutrition | Both tabs visible | `flow21_rules_screen.png` | — |
| A3 | Tap Add Rule, create INCLUDE "Chai" DAILY REQUIRED for BREAKFAST | Chai rule saved | — | — |
| A4 | Verify Chai rule card displayed | Card shows "Chai", "Every day", "Breakfast" | — | — |
| A5 | Create EXCLUDE "Karela" NEVER REQUIRED | Karela rule saved | — | — |
| A6 | Verify both rules displayed | 2 rule cards visible | `flow21_base_rules.png` | — |

### Phase B: Edit Lifecycle (Steps B1-B10)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| B1 | Tap Chai rule card | Edit sheet opens with pre-populated values | `flow21_edit_sheet.png` | — |
| B2 | Verify pre-populated: INCLUDE, Chai, DAILY, REQUIRED, BREAKFAST | All fields match original | — | — |
| B3 | Change enforcement: REQUIRED → PREFERRED | PREFERRED radio selected | — | — |
| B4 | Save rule | Sheet closes, card updated | — | — |
| B5 | Verify Chai card shows "Preferred" | Updated enforcement displayed | — | — |
| B6 | Tap Chai to edit again | Edit sheet reopens | — | — |
| B7 | Change frequency: DAILY → TIMES_PER_WEEK, count=3 (**Room DB fallback**: select TIMES_PER_WEEK via UI, save with default count, then update count via Room DB) | Frequency fields updated | — | — |
| B8 | Save rule, then apply Room DB fallback for count if needed | Sheet closes | — | — |
| B9 | Force-restart app, verify Chai card shows "3x per week" | Updated frequency displayed | — | — |
| B10 | Tap Chai, change meal slot: BREAKFAST → ANY | Meal slot updated | `flow21_edit_complete.png` | — |

### Backend API Cross-Validation: Edit Lifecycle

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/recipe-rules | \
  python -c "
import sys, json
d = json.load(sys.stdin)
rules = d if isinstance(d, list) else d.get('rules', [])
chai = [r for r in rules if 'chai' in r.get('target_name', '').lower()]
if chai:
    r = chai[0]
    print(f'Chai rule: enforcement={r[\"enforcement\"]}, freq={r[\"frequency_type\"]}, count={r.get(\"frequency_count\")}, slot={r.get(\"meal_slot\")}')
    assert r['enforcement'] == 'PREFERRED', 'Enforcement should be PREFERRED'
    assert r['frequency_type'] == 'TIMES_PER_WEEK', 'Frequency should be TIMES_PER_WEEK'
    print('Edit lifecycle verification -> PASS')
else:
    print('ERROR: Chai rule not found')
"
```

### Phase C: Frequency Switching (Steps C1-C8)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| C1 | Tap Chai to edit | Edit sheet opens | — | — |
| C2 | Switch frequency: TIMES_PER_WEEK → DAILY | Daily selected | — | — |
| C3 | Save, verify "Every day" displayed | Card updated | — | — |
| C4 | Edit again, switch: DAILY → SPECIFIC_DAYS (Mon, Thu) | Specific days selected | — | — |
| C5 | Save, verify "Mon, Thu" or day indicators | Card updated | — | — |
| C6 | **C47:** Edit, switch: SPECIFIC_DAYS → DAILY | Day selection should clear | — | Verify no leftover day data |
| C7 | Save, verify "Every day" (clean, no day remnants) | Card updated | — | — |
| C8 | **C48:** Edit, set TIMES_PER_WEEK count=2, then switch to NEVER | Count should clear | `flow21_freq_switch.png` | — |

### Phase D: Meal Slot Modes (Steps D1-D8)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| D1 | Create new rule: INCLUDE "Paneer" TIMES_PER_WEEK 2x | Rule created | — | — |
| D2 | Edit Paneer, select SPECIFIC meal slots: BREAKFAST + LUNCH | Two slots selected | `flow21_meal_slots.png` | — |
| D3 | Save, verify "Breakfast, Lunch" displayed | Card shows specific slots | — | — |
| D4 | Edit Paneer, switch to ANY meal slot | ANY mode selected | — | — |
| D5 | Save, verify "Any slot" displayed | Card updated | — | — |
| D6 | Edit Paneer, select SPECIFIC: DINNER + SNACKS | Two different slots selected | — | — |
| D7 | Save, verify "Dinner, Snacks" displayed | Card updated | — | — |
| D8 | **C49:** Edit Paneer, select ALL 4 slots in SPECIFIC mode | All chips selected (semantically = ANY) | — | Verify behavior: treats as ANY or keeps all 4 |

### Phase E: Diet Conflict (Steps E1-E8)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| E1 | Tap Add Rule | Add rule sheet opens | — | — |
| E2 | Select INCLUDE action | Include selected | — | — |
| E3 | Search for "Chicken" | Search results appear | — | — |
| E4 | Select Chicken, set 2x/week PREFERRED | Rule configured | — | — |
| E5 | **C50:** Tap Save | Diet conflict warning appears (vegetarian user + chicken) | `flow21_diet_conflict.png` | — |
| E6 | Note warning text | Document exact warning message | — | — |
| E7 | Tap "SAVE ANYWAY" (if available) | Rule saves despite conflict | — | — |
| E8 | **C51:** Repeat for "Eggs" INCLUDE | Second conflict rule (multiple conflicts) | — | — |

### Phase F: Search Edge Cases (Steps F1-F8)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| F1 | Tap Add Rule, select INCLUDE | Add sheet open | — | — |
| F2 | Search "Xyzabc123" | No results / empty state | `flow21_search_empty.png` | — |
| F3 | **C52:** Search "Dal & Rice" (special characters) | Handles & gracefully (results or empty, no crash) | — | — |
| F4 | Clear search, search "a" (single char) | **C53:** Results or "type more" prompt | — | — |
| F5 | Clear search | Search field empty | — | — |
| F6 | Verify popular suggestions appear | Default suggestions visible | — | — |
| F7 | Search "Chai" | Chai appears in results | — | — |
| F8 | Cancel/dismiss add sheet | Return to rules list | — | — |

### Phase G: Rule Sorting — Active Before Paused (Steps G1-G6)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| G1 | Verify current rules listed | Multiple rules visible | — | — |
| G2 | Open menu on one rule, tap "Pause" | Rule becomes paused/inactive | — | — |
| G3 | Verify paused rule shows visual indicator (dimmed, badge, etc.) | Visual difference | `flow21_paused_rule.png` | — |
| G4 | Verify active rules appear before paused rules | Ordering correct | — | — |
| G5 | Open menu on paused rule, tap "Enable" | Rule becomes active again | — | — |
| G6 | Verify rule returns to active section | Ordering updated | — | — |

### Phase H: Pause Effect on Meal Generation (Steps H1-H6)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| H1 | Pause a REQUIRED rule (e.g., Chai) | Rule paused | — | — |
| H2 | **C54:** Pause ALL rules | All rules paused (zero active) | — | — |
| H3 | Navigate to Home | Home screen displayed | — | — |
| H4 | Generate meal plan (if possible) | Meal plan generates without active rules | — | — |
| H5 | Verify meal plan generated successfully | Meals display (no rules applied) | `flow21_no_rules_mealplan.png` | SOFT assertion: AI should work with zero rules |
| H6 | Return to Recipe Rules, re-enable rules | Rules active again | — | — |

### Phase I: Nutrition — All 8 Categories (Steps I1-I16)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| I1 | Switch to Nutrition tab | Nutrition tab active | — | — |
| I2 | Add goal: GREEN_LEAFY, target=5, PREFERRED (**Room DB fallback** for target: create with default, update via `UPDATE nutrition_goals SET weeklyTarget=5`) | Goal created | — | — |
| I3 | Force-restart app, verify "Green Leafy" card with "0/5" | Progress displayed | — | — |
| I4 | Add goal: CITRUS_VITAMIN_C, target=4, PREFERRED (Room DB fallback for target) | Goal created | — | — |
| I5 | Add goal: IRON_RICH, target=6, PREFERRED (Room DB fallback for target) | Goal created | — | — |
| I6 | Add goal: HIGH_PROTEIN, target=7, REQUIRED (Room DB fallback for target) | Goal created | — | — |
| I7 | Add goal: CALCIUM_RICH, target=3, PREFERRED | Goal created | — | — |
| I8 | Add goal: FIBER_RICH, target=5, PREFERRED | Goal created | — | — |
| I9 | Add goal: OMEGA_3, target=3, PREFERRED | Goal created | — | — |
| I10 | Add goal: ANTIOXIDANT, target=4, PREFERRED | Goal created | `flow21_all_8_goals.png` | — |
| I11 | Verify all 8 goals displayed | 8 goal cards visible | — | — |
| I12 | **C55:** Create goal with weekly_target=1 (minimum) — Room DB fallback: `UPDATE nutrition_goals SET weeklyTarget=1` | Accepts target=1 | — | — |
| I13 | **C56:** Edit goal to weekly_target=14 (maximum) — Room DB fallback: `UPDATE nutrition_goals SET weeklyTarget=14` | Accepts target=14 | — | — |
| I14 | Scroll through all goals | All accessible | — | — |
| I15 | Verify each category shows correct display name | Names match enum | — | — |
| I16 | Verify progress bars at 0% for new goals | All show 0/X | — | — |

### Phase J: Nutrition Editing (Steps J1-J6)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| J1 | Tap GREEN_LEAFY goal card | Edit sheet opens | — | — |
| J2 | Change weekly target: 5 → 8 — Room DB fallback: `UPDATE nutrition_goals SET weeklyTarget=8 WHERE foodCategory='GREEN_LEAFY'` | Target updated | — | — |
| J3 | Force-restart app, verify "0/8" displayed | Card updated | — | — |
| J4 | Tap HIGH_PROTEIN goal | Edit sheet opens | — | — |
| J5 | Toggle enforcement: REQUIRED → PREFERRED | Enforcement changed | — | — |
| J6 | **C57:** Update progress to exceed target (e.g., 8/5 = 160%) | Accepts or caps at target | `flow21_exceed_target.png` | — |

### Phase K: All Categories Exhausted (Steps K1-K4)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| K1 | With all 8 goals created, tap Add button | Add sheet opens | — | — |
| K2 | **C58:** Verify behavior when all categories used | Add button disabled, or "All categories in use" message, or empty category dropdown | `flow21_all_categories_used.png` | — |
| K3 | Note actual behavior | Document what happens | — | — |
| K4 | Dismiss add sheet | Return to goals list | — | — |

### Phase L: Backend Cross-Validation (Steps L1-L8)

```bash
# Verify all rules on backend
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/recipe-rules | \
  python -c "
import sys, json
d = json.load(sys.stdin)
rules = d if isinstance(d, list) else d.get('rules', [])
print(f'Total rules: {len(rules)}')
for r in rules:
    status = 'ACTIVE' if r.get('is_active') else 'PAUSED'
    print(f'  [{status}] {r[\"action\"]}: {r[\"target_name\"]} ({r[\"frequency_type\"]})')
"

# Verify all nutrition goals on backend
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/nutrition-goals | \
  python -c "
import sys, json
d = json.load(sys.stdin)
goals = d if isinstance(d, list) else d.get('goals', [])
print(f'Total nutrition goals: {len(goals)}')
for g in goals:
    print(f'  {g[\"food_category\"]}: {g[\"current_progress\"]}/{g[\"weekly_target\"]} ({g[\"enforcement\"]})')
if len(goals) == 8:
    print('All 8 categories present -> PASS')
else:
    print(f'WARNING: Expected 8 categories, got {len(goals)}')
"
```

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| L1 | Run rules curl command above | Rules match UI state | — | — |
| L2 | Verify rule count matches UI | Counts equal | — | — |
| L3 | Verify edited rule fields persist | Enforcement, frequency, meal slot correct | — | — |
| L4 | Verify paused rules show is_active=false | Backend reflects pause state | — | — |
| L5 | Run goals curl command above | Goals match UI state | — | — |
| L6 | Verify 8 goals exist | Count = 8 | — | — |
| L7 | Verify edited goal targets persist | Weekly targets correct | — | — |
| L8 | Verify sync_status = SYNCED for all | No PENDING items | — | — |

## Contradictions Summary

| ID | Contradiction | Expected Behavior |
|----|---------------|-------------------|
| C47 | SPECIFIC_DAYS → DAILY clears day selection | Day selection data cleared, "Every day" displayed |
| C48 | TIMES_PER_WEEK → NEVER clears count | Count cleared, "Never" displayed |
| C49 | Select all 4 slots in SPECIFIC mode | Either treated as ANY or keeps all 4 selected |
| C50 | Diet conflict warning + SAVE ANYWAY | Warning displayed, user can override with SAVE ANYWAY |
| C51 | Multiple conflicting rules (Eggs+Chicken for vegetarian) | Each conflict shows individual warning |
| C52 | Special characters in search ("Dal & Rice") | Graceful handling, no crash |
| C53 | Single-char query ("a") | Results or "type more" prompt |
| C54 | Pause all rules then generate | Meal plan works with zero active rules |
| C55 | weekly_target=1 (minimum) | Accepted by validation |
| C56 | weekly_target=14 (maximum) | Accepted by validation |
| C57 | Progress exceeds target (8/5) | Accepted or capped at target |
| C58 | Attempt 9th category | Add disabled, message, or empty dropdown |

## Fix Strategy

**For C50/C51 (Diet conflict):**
- RecipeRulesViewModel checks user's dietary_type against rule target
- Non-veg items (Chicken, Eggs, Fish, Mutton) conflict with VEGETARIAN/VEGAN/JAIN
- Warning dialog with "SAVE ANYWAY" button
- Backend has no diet conflict check (client-side only)

**For C54 (Zero active rules):**
- AI prompt should work without any rules
- meal_generation_service filters only is_active=True rules
- Empty rules list = unconstrained generation

**For C58 (All categories used):**
- Nutrition tab add button should disable or show info message
- Backend returns 409 for duplicate category regardless

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Settings | A1 | Navigation to Recipe Rules |
| Recipe Rules - Rules Tab | A2-A6, B1-B10, C1-C8, D1-D8, E1-E8, F1-F8, G1-G6, H1-H6 | Full rules lifecycle |
| Recipe Rules - Nutrition Tab | I1-I16, J1-J6, K1-K4 | Full nutrition goals lifecycle |
| Home (optional) | H3-H5 | Meal generation without rules |
