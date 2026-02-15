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

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| A1 | Tap bottom nav "Stats" | Stats screen loads | UI | `flow10_stats.png` | — |
| A2 | Verify screen title | "Stats" or "Cooking Stats" visible | UI | — | — |
| A3 | Verify cooking streak section | Streak counter visible (may be 0) | UI | — | — |
| A4 | Verify time period tabs | "Week", "Month", "All Time" or similar tabs visible | UI | — | — |
| A5 | Tap "Month" tab | Stats update for monthly view | UI | `flow10_stats_month.png` | — |
| A6 | Tap "All Time" tab | Stats update for all-time view | UI | — | — |
| A7 | Tap "Week" tab (back to default) | Weekly view restored | UI | — | — |
| A7a | Look for Share button | content-desc "Share" visible | UI | — | — |
| A7b | Tap Share button (if found) | Share intent launches | UI | — | — |
| A7c | Press BACK to dismiss share | Return to Stats | UI | — | — |

### Phase B: Chart & Achievements (Steps 8-12)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| B1 | Look for cuisine distribution chart | Chart or "No data" placeholder | UI | — | — |
| B2 | Scroll down for more stats | Additional sections visible below fold | UI | `flow10_stats_scrolled.png` | — |
| B3 | Look for achievements section | Achievements/badges or placeholder | UI | — | — |
| B3a | Look for "View All" achievements link | text "View All" near achievements | UI | — | — |
| B3b | Tap "View All" if found | Achievements screen loads | UI | `flow10_achievements.png` | — |
| B3c | Press BACK | Return to Stats | UI | — | — |
| B3d | Look for leaderboard/challenge sections | text "Leaderboard" or "Challenge" | UI | — | — |
| B4 | Verify data consistency | If streak > 0, cuisine chart should have data | UI | — | — |
| B5 | Run crash/ANR detection (Pattern 9) | No crashes | API | — | — |

### Backend API Cross-Validation: Stats Data

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/stats | \
  python -c "
import sys, json
d = json.load(sys.stdin)
print(f'cooking_streak: {d.get(\"cooking_streak\", 0)}')
print(f'total_cooked: {d.get(\"total_cooked\", 0)}')
cuisine_breakdown = d.get('cuisine_breakdown', {})
print(f'cuisine_breakdown: {len(cuisine_breakdown)} cuisines')
for cuisine, count in cuisine_breakdown.items():
    print(f'  {cuisine}: {count}')
"
```

### Phase C: Final State (Steps 13-14)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| C1 | Take final screenshot | Stats in current state | UI | `flow10_stats_final.png` | — |
| C2 | Tap bottom nav "Home" | Return to Home | UI | — | — |

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
