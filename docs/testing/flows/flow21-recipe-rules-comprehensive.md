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

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| A1 | Navigate: Profile icon → Settings → Recipe Rules | Recipe Rules screen loads | UI | — | — |
| A2 | Verify 2 tabs: Rules, Nutrition | Both tabs visible | UI | `flow21_rules_screen.png` | — |
| A3 | Tap Add Rule, create INCLUDE "Chai" DAILY REQUIRED for BREAKFAST | Chai rule saved | UI | — | — |
| A4 | Verify Chai rule card displayed | Card shows "Chai", "Every day", "Breakfast" | UI | — | — |
| A5 | Create EXCLUDE "Karela" NEVER REQUIRED | Karela rule saved | UI | — | — |
| A6 | Verify both rules displayed | 2 rule cards visible | UI | `flow21_base_rules.png` | — |

### Phase B: Edit Lifecycle (Steps B1-B10)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| B1 | Tap Chai rule card | Edit sheet opens with pre-populated values | UI | `flow21_edit_sheet.png` | — |
| B2 | Verify pre-populated: INCLUDE, Chai, DAILY, REQUIRED, BREAKFAST | All fields match original | UI | — | — |
| B3 | Change enforcement: REQUIRED → PREFERRED | PREFERRED radio selected | UI | — | — |
| B4 | Save rule | Sheet closes, card updated | UI | — | — |
| B5 | Verify Chai card shows "Preferred" | Updated enforcement displayed | UI | — | — |
| B6 | Tap Chai to edit again | Edit sheet reopens | UI | — | — |
| B7 | Change frequency: DAILY → TIMES_PER_WEEK, count=3 (**Room DB fallback**: select TIMES_PER_WEEK via UI, save with default count, then update count via Room DB) | Frequency fields updated | UI | — | — |
| B8 | Save rule, then apply Room DB fallback for count if needed | Sheet closes | UI | — | — |
| B9 | Force-restart app, verify Chai card shows "3x per week" | Updated frequency displayed | UI | — | — |
| B10 | Tap Chai, change meal slot: BREAKFAST → ANY | Meal slot updated | UI | `flow21_edit_complete.png` | — |

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

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| C1 | Tap Chai to edit | Edit sheet opens | UI | — | — |
| C2 | Switch frequency: TIMES_PER_WEEK → DAILY | Daily selected | UI | — | — |
| C3 | Save, verify "Every day" displayed | Card updated | UI | — | — |
| C4 | Edit again, switch: DAILY → SPECIFIC_DAYS (Mon, Thu) | Specific days selected | UI | — | — |
| C5 | Save, verify "Mon, Thu" or day indicators | Card updated | UI | — | — |
| C6 | **C47:** Edit, switch: SPECIFIC_DAYS → DAILY | Day selection should clear | UI | — | Verify no leftover day data |
| C7 | Save, verify "Every day" (clean, no day remnants) | Card updated | UI | — | — |
| C8 | **C48:** Edit, set TIMES_PER_WEEK count=2, then switch to NEVER | Count should clear | UI | `flow21_freq_switch.png` | — |

### Phase D: Meal Slot Modes (Steps D1-D8)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| D1 | Create new rule: INCLUDE "Paneer" TIMES_PER_WEEK 2x | Rule created | UI | — | — |
| D2 | Edit Paneer, select SPECIFIC meal slots: BREAKFAST + LUNCH | Two slots selected | UI | `flow21_meal_slots.png` | — |
| D3 | Save, verify "Breakfast, Lunch" displayed | Card shows specific slots | UI | — | — |
| D4 | Edit Paneer, switch to ANY meal slot | ANY mode selected | UI | — | — |
| D5 | Save, verify "Any slot" displayed | Card updated | UI | — | — |
| D6 | Edit Paneer, select SPECIFIC: DINNER + SNACKS | Two different slots selected | UI | — | — |
| D7 | Save, verify "Dinner, Snacks" displayed | Card updated | UI | — | — |
| D8 | **C49:** Edit Paneer, select ALL 4 slots in SPECIFIC mode | All chips selected (semantically = ANY) | UI | — | Verify behavior: treats as ANY or keeps all 4 |

### Phase E: Diet Conflict (Steps E1-E8)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| E1 | Tap Add Rule | Add rule sheet opens | UI | — | — |
| E2 | Select INCLUDE action | Include selected | UI | — | — |
| E3 | Search for "Chicken" | Search results appear | UI | — | — |
| E4 | Select Chicken, set 2x/week PREFERRED | Rule configured | UI | — | — |
| E5 | **C50:** Tap Save | Diet conflict warning appears (vegetarian user + chicken) | UI | `flow21_diet_conflict.png` | — |
| E6 | Note warning text | Document exact warning message | UI | — | — |
| E7 | Tap "SAVE ANYWAY" (if available) | Rule saves despite conflict | UI | — | — |
| E8 | **C51:** Repeat for "Eggs" INCLUDE | Second conflict rule (multiple conflicts) | UI | — | — |

### Phase F: Search Edge Cases (Steps F1-F8)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| F1 | Tap Add Rule, select INCLUDE | Add sheet open | UI | — | — |
| F2 | Search "Xyzabc123" | No results / empty state | UI | `flow21_search_empty.png` | — |
| F3 | **C52:** Search "Dal & Rice" (special characters) | Handles & gracefully (results or empty, no crash) | UI | — | — |
| F4 | Clear search, search "a" (single char) | **C53:** Results or "type more" prompt | UI | — | — |
| F5 | Clear search | Search field empty | UI | — | — |
| F6 | Verify popular suggestions appear | Default suggestions visible | UI | — | — |
| F7 | Search "Chai" | Chai appears in results | UI | — | — |
| F8 | Cancel/dismiss add sheet | Return to rules list | UI | — | — |

### Phase G: Rule Sorting — Active Before Paused (Steps G1-G6)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| G1 | Verify current rules listed | Multiple rules visible | UI | — | — |
| G2 | Open menu on one rule, tap "Pause" | Rule becomes paused/inactive | UI | — | — |
| G3 | Verify paused rule shows visual indicator (dimmed, badge, etc.) | Visual difference | UI | `flow21_paused_rule.png` | — |
| G4 | Verify active rules appear before paused rules | Ordering correct | UI | — | — |
| G5 | Open menu on paused rule, tap "Enable" | Rule becomes active again | UI | — | — |
| G6 | Verify rule returns to active section | Ordering updated | UI | — | — |

### Phase H: Pause Effect on Meal Generation (Steps H1-H6)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| H1 | Pause a REQUIRED rule (e.g., Chai) | Rule paused | UI | — | — |
| H2 | **C54:** Pause ALL rules | All rules paused (zero active) | UI | — | — |
| H3 | Navigate to Home | Home screen displayed | UI | — | — |
| H4 | Generate meal plan (if possible) | Meal plan generates without active rules | UI | — | — |
| H5 | Verify meal plan generated successfully | Meals display (no rules applied) | UI | `flow21_no_rules_mealplan.png` | SOFT assertion: AI should work with zero rules |
| H6 | Return to Recipe Rules, re-enable rules | Rules active again | UI | — | — |

### Phase I: Nutrition — All 8 Categories (Steps I1-I16)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| I1 | Switch to Nutrition tab | Nutrition tab active | UI | — | — |
| I2 | Add goal: GREEN_LEAFY, target=5, PREFERRED (**Room DB fallback** for target: create with default, update via `UPDATE nutrition_goals SET weeklyTarget=5`) | Goal created | UI | — | — |
| I3 | Force-restart app, verify "Green Leafy" card with "0/5" | Progress displayed | UI | — | — |
| I4 | Add goal: CITRUS_VITAMIN_C, target=4, PREFERRED (Room DB fallback for target) | Goal created | UI | — | — |
| I5 | Add goal: IRON_RICH, target=6, PREFERRED (Room DB fallback for target) | Goal created | UI | — | — |
| I6 | Add goal: HIGH_PROTEIN, target=7, REQUIRED (Room DB fallback for target) | Goal created | UI | — | — |
| I7 | Add goal: CALCIUM_RICH, target=3, PREFERRED | Goal created | UI | — | — |
| I8 | Add goal: FIBER_RICH, target=5, PREFERRED | Goal created | UI | — | — |
| I9 | Add goal: OMEGA_3, target=3, PREFERRED | Goal created | UI | — | — |
| I10 | Add goal: ANTIOXIDANT, target=4, PREFERRED | Goal created | UI | `flow21_all_8_goals.png` | — |
| I11 | Verify all 8 goals displayed | 8 goal cards visible | UI | — | — |
| I12 | **C55:** Create goal with weekly_target=1 (minimum) — Room DB fallback: `UPDATE nutrition_goals SET weeklyTarget=1` | Accepts target=1 | UI | — | — |
| I13 | **C56:** Edit goal to weekly_target=14 (maximum) — Room DB fallback: `UPDATE nutrition_goals SET weeklyTarget=14` | Accepts target=14 | UI | — | — |
| I14 | Scroll through all goals | All accessible | UI | — | — |
| I15 | Verify each category shows correct display name | Names match enum | UI | — | — |
| I16 | Verify progress bars at 0% for new goals | All show 0/X | UI | — | — |

### Phase J: Nutrition Editing (Steps J1-J6)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| J1 | Tap GREEN_LEAFY goal card | Edit sheet opens | UI | — | — |
| J2 | Change weekly target: 5 → 8 — Room DB fallback: `UPDATE nutrition_goals SET weeklyTarget=8 WHERE foodCategory='GREEN_LEAFY'` | Target updated | UI | — | — |
| J3 | Force-restart app, verify "0/8" displayed | Card updated | UI | — | — |
| J4 | Tap HIGH_PROTEIN goal | Edit sheet opens | UI | — | — |
| J5 | Toggle enforcement: REQUIRED → PREFERRED | Enforcement changed | UI | — | — |
| J6 | **C57:** Update progress to exceed target (e.g., 8/5 = 160%) | Accepts or caps at target | UI | `flow21_exceed_target.png` | — |

### Phase K: All Categories Exhausted (Steps K1-K4)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| K1 | With all 8 goals created, tap Add button | Add sheet opens | UI | — | — |
| K2 | **C58:** Verify behavior when all categories used | Add button disabled, or "All categories in use" message, or empty category dropdown | UI | `flow21_all_categories_used.png` | — |
| K3 | Note actual behavior | Document what happens | UI | — | — |
| K4 | Dismiss add sheet | Return to goals list | UI | — | — |

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

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| L1 | Run rules curl command above | Rules match UI state | API | — | — |
| L2 | Verify rule count matches UI | Counts equal | API | — | — |
| L3 | Verify edited rule fields persist | Enforcement, frequency, meal slot correct | API | — | — |
| L4 | Verify paused rules show is_active=false | Backend reflects pause state | API | — | — |
| L5 | Run goals curl command above | Goals match UI state | API | — | — |
| L6 | Verify 8 goals exist | Count = 8 | API | — | — |
| L7 | Verify edited goal targets persist | Weekly targets correct | API | — | — |
| L8 | Verify sync_status = SYNCED for all | No PENDING items | API | — | — |

## Contradictions Summary

| ID | Contradiction | Expected Behavior | Type |
|----|---------------|-------------------|------|
| C47 | SPECIFIC_DAYS → DAILY clears day selection | Day selection data cleared, "Every day" displayed | UI |
| C48 | TIMES_PER_WEEK → NEVER clears count | Count cleared, "Never" displayed | UI |
| C49 | Select all 4 slots in SPECIFIC mode | Either treated as ANY or keeps all 4 selected | UI |
| C50 | Diet conflict warning + SAVE ANYWAY | Warning displayed, user can override with SAVE ANYWAY | UI |
| C51 | Multiple conflicting rules (Eggs+Chicken for vegetarian) | Each conflict shows individual warning | UI |
| C52 | Special characters in search ("Dal & Rice") | Graceful handling, no crash | UI |
| C53 | Single-char query ("a") | Results or "type more" prompt | UI |
| C54 | Pause all rules then generate | Meal plan works with zero active rules | UI |
| C55 | weekly_target=1 (minimum) | Accepted by validation | UI |
| C56 | weekly_target=14 (maximum) | Accepted by validation | UI |
| C57 | Progress exceeds target (8/5) | Accepts or caps at target | UI |
| C58 | Attempt 9th category | Add disabled, message, or empty dropdown | UI |

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
