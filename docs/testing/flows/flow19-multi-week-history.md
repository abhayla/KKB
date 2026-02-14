# Flow 19: Multi-Week History

## Metadata
- **Flow Name:** `multi-week-history`
- **Goal:** Navigate between different weeks on Home screen, verify meal plan loading and data consistency
- **Preconditions:** Backend running, at least 2 meal plans for different weeks, multi-week navigation implemented (Phase 6 feature)
- **Estimated Duration:** 8-12 minutes
- **Screens Covered:** Home, Grocery
- **Depends On:** Flow 01 (authenticated user with meal plan), Phase 6 (multi-week history)
- **State Produced:** User navigates through multiple weeks, verifies historical meal plans

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] User authenticated
- [ ] At least 1 current week meal plan generated
- [ ] Backend running with multi-week support
- [ ] Ability to seed/generate additional meal plans for past/future weeks

## Test User Persona

Uses existing Sharma family data. Focus is on multi-week navigation and data consistency.

## Steps

### Phase A: Verify Current Week (Steps 1-5)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| A1 | Navigate to Home screen | Home screen with current week meal plan | `flow19_home_current.png` | HARD |
| A2 | Verify week header shows "This Week's Menu" or current date range | Header text contains week range (e.g., "Feb 10 - Feb 16") | — | HARD |
| A3 | Verify day selector shows current week days | 7 day tabs visible, current day highlighted | — | HARD |
| A4 | Verify meal plan data exists | BREAKFAST, LUNCH, DINNER, SNACKS all have items | — | HARD |
| A5 | Note first meal name | Record name of Monday BREAKFAST item for later comparison | — | — |

### Phase B: Generate/Seed Previous Week Meal Plan (Steps 6-8)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| B1 | Via backend API, generate meal plan for previous week | See Backend API section below | — | HARD |
| B2 | Verify API response 200 | Previous week plan created in database | — | HARD |
| B3 | Note previous week date range | e.g., "Feb 3 - Feb 9" | — | — |

### Backend API: Generate Previous Week Meal Plan

```bash
# Calculate previous week date range (7 days before current week start)
# For testing, use hardcoded dates or calculate via Python

# Example: Generate meal plan for week of Feb 3-9, 2026
curl -s -X POST http://localhost:8000/api/v1/meal-plans/generate \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "week_start_date": "2026-02-03",
    "week_end_date": "2026-02-09"
  }' | python -c "
import sys, json
try:
    d = json.load(sys.stdin)
    if d.get('id'):
        print(f'Previous week plan created: {d.get(\"id\")}')
        print(f'Week: {d.get(\"week_start_date\")} to {d.get(\"week_end_date\")}')
    else:
        print('ERROR: No meal plan ID in response')
except Exception as e:
    print(f'ERROR: {e}')
"
```

### Phase C: Navigate to Previous Week (Steps 9-14)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| C1 | On Home screen, verify Previous Week arrow button exists | content-desc "Previous Week" or left arrow visible | — | HARD |
| C2 | Tap Previous Week arrow | Loading indicator, then previous week meal plan loads | `flow19_previous_week.png` | HARD |
| C3 | Verify week header updates | Header shows previous week range (e.g., "Feb 3 - Feb 9") | — | HARD |
| C4 | Verify day selector updates | Days show dates from previous week | — | HARD |
| C5 | Verify meal plan data exists | All meal slots have items (different from current week) | — | HARD |
| C6 | Compare Monday BREAKFAST item | Different recipe name than current week (recorded in A5) | — | HARD |

### Phase D: Navigate Back to Current Week (Steps 15-19)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| D1 | Tap Next Week arrow | Current week meal plan loads again | — | HARD |
| D2 | Verify week header shows current week range | Back to "This Week's Menu" or current dates | — | HARD |
| D3 | Verify Monday BREAKFAST matches original | Same recipe name as noted in A5 | — | HARD |
| D4 | Verify "Next Week" arrow exists (for future weeks) | Right arrow visible | — | — |
| D5 | Verify "Back to This Week" button exists (if on non-current week) | Button visible/hidden based on week selected | — | — |

### Phase E: Navigate to Week with No Plan (Steps 20-24)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| E1 | Tap Next Week arrow (from current week) | Navigate to future week | — | — |
| E2 | Verify no meal plan data | Empty state or message "No meal plan for this week" | `flow19_empty_week.png` | HARD |
| E3 | Verify week header shows correct future week range | Header displays future dates | — | HARD |
| E4 | Verify day selector shows future week days | Days are correct for future week | — | — |
| E5 | Verify "Generate Meal Plan" button exists | Option to generate plan for this week | — | — |

### Phase F: Return to Current Week (Steps 25-27)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| F1 | Tap "Back to This Week" button | Instantly returns to current week (no loading) | — | HARD |
| F2 | Verify current week meal plan displayed | Same data as Phase A | — | HARD |
| F3 | Verify week header shows current week | "This Week's Menu" visible | — | HARD |

### Phase G: Grocery List Multi-Week Integration (Steps 28-33)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| G1 | While on current week, navigate to Grocery | Grocery screen loads | `flow19_grocery_current.png` | — |
| G2 | Verify grocery items from current week meal plan | Items match current week recipes | — | HARD |
| G3 | Navigate back to Home | Home screen | — | — |
| G4 | Switch to previous week | Previous week meal plan loads | — | — |
| G5 | Navigate to Grocery | Grocery screen loads | `flow19_grocery_previous.png` | — |
| G6 | Verify grocery items updated (optional feature) | Items match previous week recipes OR remain from current week (document behavior) | — | — |

### Backend API Cross-Validation: Meal Plan Retrieval

```bash
# Verify both meal plans exist in backend

# Get all meal plans for user
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/meal-plans | \
  python -c "
import sys, json
d = json.load(sys.stdin)
plans = d if isinstance(d, list) else d.get('meal_plans', [])
print(f'Total meal plans: {len(plans)} (expected: at least 2)')
for i, plan in enumerate(plans, 1):
    start = plan.get('week_start_date', 'unknown')
    end = plan.get('week_end_date', 'unknown')
    days = len(plan.get('days', []))
    print(f'  Plan {i}: {start} to {end}, {days} days')
if len(plans) >= 2:
    print('Multi-week data -> PASS')
else:
    print('WARNING: Need at least 2 meal plans for multi-week test')
"
```

### Phase H: Contradictions C43-C45 (Steps 34-42)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| H1 | **C43:** Rapidly tap Previous/Next Week arrows 10 times | UI handles rapid navigation without crash | — | HARD |
| H2 | Verify Home screen still functional | No crashes, correct week displayed | `flow19_c43_rapid.png` | HARD |
| H3 | **C44:** Navigate to week 4 weeks in the past | Tap Previous Week 4 times | — | — |
| H4 | Verify very old week loads (if plan exists) | Historical plan loads OR empty state | `flow19_c44_old_week.png` | — |
| H5 | Tap "Back to This Week" | Returns to current week | — | HARD |
| H6 | **C45:** Navigate to week 4 weeks in the future | Tap Next Week 4 times | — | — |
| H7 | Verify very future week shows empty state | "No meal plan" or generate option | `flow19_c45_future_week.png` | HARD |
| H8 | Tap "Back to This Week" | Returns to current week | — | HARD |
| H9 | Run crash/ANR detection (Pattern 9) | No crashes during rapid navigation | — | HARD |

### Phase I: Cleanup (Steps 43-44)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| I1 | Ensure Home screen on current week | Current week displayed | `flow19_final.png` | — |
| I2 | Verify all data intact | Current week meal plan unchanged | — | — |

## Validation Checkpoints

No `validate_meal_plan.py` checkpoints — validation is multi-week navigation-focused:
- **Week Navigation:** Previous/Next Week arrows work correctly
- **Data Loading:** Meal plans load for correct week
- **Empty State:** Graceful handling of weeks with no plan
- **Quick Return:** "Back to This Week" button works
- **Grocery Integration:** (Optional) Grocery list updates with selected week

## Fix Strategy

**Relevant files for this flow:**
- Home UI: `app/presentation/home/HomeViewModel.kt`, `HomeScreen.kt`
- Week navigation: `app/presentation/home/components/WeekSelector.kt` (if implemented)
- Meal plan loading: `domain/repository/MealPlanRepository.kt`, `data/repository/MealPlanRepositoryImpl.kt`
- Backend multi-week API: `backend/app/api/v1/endpoints/meal_plans.py`
- Meal plan service: `backend/app/services/meal_plan_service.py`

**Common issues:**
- Week arrows don't update meal plan → ViewModel not fetching new week data
- Grocery doesn't update when week changes → Grocery screen not observing week state
- Empty state not showing → UI not handling null/empty meal plan
- "Back to This Week" doesn't work → Week state not resetting to current
- Crash on rapid navigation → Race condition in ViewModel, missing debounce/throttle
- Previous weeks don't load → Backend not returning historical plans (check query)

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Home | A1-A5, C1-C6, D1-D5, E1-E5, F1-F3, H1-H9 | Multi-week navigation, empty state |
| Grocery | G1-G6 | Multi-week grocery integration (optional) |

## Contradictions

| ID | Description | Steps | Expected Outcome |
|----|-------------|-------|------------------|
| C43 | Rapid week navigation | H1-H2 | No crash, UI stable |
| C44 | Navigate to very old week (4 weeks past) | H3-H5 | Historical plan loads or empty state |
| C45 | Navigate to very future week (4 weeks ahead) | H6-H8 | Empty state with generate option |
