# Flow 10: Stats Tracking

## Metadata
- **Flow Name:** `stats-tracking`
- **Goal:** Verify stats screen displays cooking streak, cuisine charts, time tabs, and achievements
- **Preconditions:** User has cooked at least one recipe (from Flow 3 or manual)
- **Estimated Duration:** 3-5 minutes
- **Screens Covered:** Stats
- **Depends On:** none (stats may be empty for fresh users — that's OK)
- **State Produced:** None (read-only flow)

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] User authenticated
- [ ] Ideally, user has completed at least one cooking session (Flow 3 marks a recipe as cooked)
- [ ] Stats may show zeros for fresh accounts — this is expected

## Test User Persona

Uses existing Sharma family data. Read-only verification.

## Steps

### Phase A: Stats Screen Exploration (Steps 1-7)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| A1 | Tap bottom nav "Stats" | Stats screen loads | `flow10_stats.png` | — |
| A2 | Verify screen title | "Stats" or "Cooking Stats" visible | — | — |
| A3 | Verify cooking streak section | Streak counter visible (may be 0) | — | — |
| A4 | Verify time period tabs | "Week", "Month", "All Time" or similar tabs visible | — | — |
| A5 | Tap "Month" tab | Stats update for monthly view | `flow10_stats_month.png` | — |
| A6 | Tap "All Time" tab | Stats update for all-time view | — | — |
| A7 | Tap "Week" tab (back to default) | Weekly view restored | — | — |

### Phase B: Chart & Achievements (Steps 8-12)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| B1 | Look for cuisine distribution chart | Chart or "No data" placeholder | — | — |
| B2 | Scroll down for more stats | Additional sections visible below fold | `flow10_stats_scrolled.png` | — |
| B3 | Look for achievements section | Achievements/badges or placeholder | — | — |
| B4 | Verify data consistency | If streak > 0, cuisine chart should have data | — | — |
| B5 | Run crash/ANR detection (Pattern 9) | No crashes | — | — |

### Phase C: Final State (Steps 13-14)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| C1 | Take final screenshot | Stats in current state | `flow10_stats_final.png` | — |
| C2 | Tap bottom nav "Home" | Return to Home | — | — |

## Validation Checkpoints

No `validate_meal_plan.py` checkpoints — validation is UI-based:
- Stats screen loads without crash
- Time period tabs are functional
- Data is consistent (streak, chart, achievements)
- Empty state handled gracefully for fresh accounts

## Fix Strategy

**Relevant files for this flow:**
- Stats screen: `app/presentation/stats/StatsViewModel.kt`, `StatsScreen.kt`
- Stats DAO: `data/local/dao/StatsDao.kt`
- Cooked recipes: `data/local/entity/CookedRecipeEntity.kt`
- Charts: Look for chart library usage in `StatsScreen.kt`

**Common issues:**
- Chart not rendering → Canvas-based charts may not appear in uiautomator XML
- Streak incorrect → check CookedRecipeEntity dates and streak calculation
- Tabs not switching → check tab state management in ViewModel
- Empty state not shown → missing conditional for zero data

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Stats | A1-A7, B1-B5, C1 | Time tabs, streak, chart, achievements |
