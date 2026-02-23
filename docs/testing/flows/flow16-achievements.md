# Flow 16: Achievement Earning

## Metadata

| Attribute | Value |
|-----------|-------|
| **Flow Name** | achievement-earning |
| **Flow ID** | FLOW-16 |
| **Goal** | Test achievement unlock flow — cook meals, earn achievements, see notifications |
| **Priority** | P1 |
| **Complexity** | Medium-High |
| **Estimated Duration** | 10-15 minutes |
| **Last Updated** | 2026-02-14 |

## Prerequisites

- Backend API running (`uvicorn app.main:app --reload`)
- PostgreSQL database with schema applied (`alembic upgrade head`)
- Achievements seeded in database (`PYTHONPATH=. python scripts/seed_achievements.py`)
- Android emulator running (API 34)
- Backend achievement earning logic implemented (Phase 3)
- User authenticated via fake-firebase-token
- At least one meal plan generated (for cooking recipes)

## Depends On

- **Flow 01** (new-user-journey) — Must complete authentication, onboarding, and meal plan generation
- **Phase 3** (achievement-earning) — Backend logic to award achievements based on cooking activity

## Test User Persona

**Sharma Family — Achievement Hunter**

| Field | Value |
|-------|-------|
| Email | `abhayfaircent@gmail.com` (primary) or `zmphzc@gmail.com` (secondary) |
| Auth | Real Google OAuth (account must be signed into emulator) |
| Display Name | Abhay Sharma |
| State | Has completed onboarding, has 1 active meal plan, 0 cooked recipes initially |

**Achievement Milestones to Test:**

| Achievement ID | Name | Description | Unlock Criteria |
|----------------|------|-------------|-----------------|
| ACH-001 | First Meal | Cook your first recipe | 1 cooked recipe |
| ACH-002 | 5 Meals | Cook 5 different recipes | 5 cooked recipes |
| ACH-003 | 10 Meals | Cook 10 different recipes | 10 cooked recipes |
| ACH-004 | 3-Day Streak | Maintain cooking streak for 3 days | 3 consecutive days with >= 1 cooked recipe |
| ACH-005 | Healthy Eater | Cook 5 recipes with vegetables | 5 vegetable-based recipes |

## Test Phases

### Phase A: Verify Initial Achievements State (0 Unlocked)

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| A1 | Navigate to Stats screen from bottom nav | Stats screen displays | UI |
| A2 | Tap "Achievements" tab (if tab-based UI) | Achievements tab content displays | UI |
| A3 | Verify section title: "Achievements" | Title visible | UI |
| A4 | Verify all achievements shown in locked state (grayed out icons, locked badge) | All achievements locked | UI |
| A5 | Verify achievement count: "0/5 Unlocked" OR similar progress indicator | Shows 0 unlocked | UI |
| A6 | Screenshot: `flow16_achievements_initial.png` | All locked state | UI |

### Phase B: Cook First Recipe → Unlock "First Meal"

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| B1 | Navigate to Home screen | Home displays with meal plan | UI |
| B2 | Tap a breakfast meal card → "View Recipe" | Recipe Detail screen displays | UI |
| B3 | Note recipe name (e.g., "Masala Dosa") | Recipe name captured | UI |
| B4 | Tap "Start Cooking" | Cooking Mode screen displays | UI |
| B5 | Complete all cooking steps (tap "Next" until last step) | All steps traversed | UI |
| B6 | Tap "Finish Cooking" OR "Mark as Cooked" | Cooking completion dialog appears | UI |
| B7 | Confirm completion | Dialog closes, return to Home OR Recipe Detail | UI |
| B8 | Wait 2-3 seconds for backend to process achievement | Backend checks cooking count, awards "First Meal" | UI |
| B9 | Verify notification appears: "Achievement Unlocked: First Meal!" | Notification badge on Home increases | UI |
| B10 | Screenshot: `flow16_first_meal_notification.png` | Notification visible | UI |

### Phase C: Verify "First Meal" Achievement Unlocked

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| C1 | Navigate to Stats → Achievements | Achievements screen displays | UI |
| C2 | Verify "First Meal" achievement now unlocked (colored icon, no lock badge) | Achievement unlocked state visible | UI |
| C3 | Verify achievement card shows: Name, Icon, Description, Unlock Date/Time | All fields present | UI |
| C4 | Verify unlock date is today's date | Date matches current date | UI |
| C5 | Verify achievement count: "1/5 Unlocked" OR similar | Progress updated | UI |
| C6 | Tap on "First Meal" achievement card | Achievement detail sheet appears with full description | UI |
| C7 | Screenshot: `flow16_first_meal_unlocked.png` | Unlocked state visible | UI |

### Phase D: Cook 4 More Recipes → Unlock "5 Meals"

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| D1 | Return to Home screen | Home displays | UI |
| D2 | **Recipe 2:** Tap lunch meal card → View Recipe → Start Cooking → Finish | Cooking logged | UI |
| D3 | Wait 2s | Backend processes, no new achievement yet (count = 2) | UI |
| D4 | **Recipe 3:** Tap dinner meal card → View Recipe → Start Cooking → Finish | Cooking logged | UI |
| D5 | Wait 2s | Backend processes, no new achievement yet (count = 3) | UI |
| D6 | **Recipe 4:** Tap snacks meal card → View Recipe → Start Cooking → Finish | Cooking logged | UI |
| D7 | Wait 2s | Backend processes, no new achievement yet (count = 4) | UI |
| D8 | **Recipe 5:** Navigate to next day, tap breakfast → View Recipe → Start Cooking → Finish | Cooking logged | UI |
| D9 | Wait 3s for backend to process 5th cooking | Backend checks, awards "5 Meals" achievement | UI |
| D10 | Verify notification: "Achievement Unlocked: 5 Meals!" | Notification appears | UI |
| D11 | Screenshot: `flow16_five_meals_notification.png` | Notification visible | UI |

**Alternative: Backend API Shortcut (for speed):**

Instead of manually cooking 4 more recipes via UI (steps D2-D8), use backend API to log cooked recipes directly:

```bash
# Get JWT
JWT=$(curl -s -X POST http://localhost:8000/api/v1/auth/firebase \
  -H 'Content-Type: application/json' \
  -d '{"firebase_token":"fake-firebase-token"}' | \
  python -c 'import sys,json;print(json.load(sys.stdin).get("access_token",""))')

# Get recipe IDs from current meal plan
RECIPE_IDS=$(curl -s -H "Authorization: Bearer $JWT" \
  http://localhost:8000/api/v1/meal-plans/current | \
  python -c "
import sys, json
mp = json.load(sys.stdin)
recipes = []
for day in mp.get('days', []):
    for slot in ['breakfast', 'lunch', 'dinner', 'snacks']:
        for item in day.get(slot, []):
            recipes.append(item.get('recipe_id'))
# Print first 5 unique recipe IDs
print(' '.join(list(set(recipes))[:5]))
")

# Log 4 more cooked recipes (2, 3, 4, 5)
for RECIPE_ID in $(echo $RECIPE_IDS | cut -d' ' -f2-5); do
  curl -s -X POST -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    http://localhost:8000/api/v1/stats/cooked-recipes \
    -d "{\"recipe_id\": \"$RECIPE_ID\", \"cooked_date\": \"$(date -I)\"}"
  echo "Logged recipe: $RECIPE_ID"
  sleep 1
done
```

### Phase E: Verify "5 Meals" Achievement Unlocked

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| E1 | Navigate to Stats → Achievements | Achievements screen displays | UI |
| E2 | Verify "5 Meals" achievement unlocked (colored icon) | Unlocked state visible | UI |
| E3 | Verify achievement count: "2/5 Unlocked" OR similar | Progress shows 2 | UI |
| E4 | Verify "10 Meals" and "3-Day Streak" still locked (grayed out) | Remaining achievements locked | UI |
| E5 | Screenshot: `flow16_five_meals_unlocked.png` | Unlocked achievements visible | UI |

### Phase F: Verify Notifications for Each Achievement

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| F1 | Navigate to Notifications screen | Notifications screen displays | UI |
| F2 | Verify 2 achievement unlock notifications present: "First Meal" and "5 Meals" | Both notifications visible | UI |
| F3 | Verify notification title format: "Achievement Unlocked: {name}!" | Title matches pattern | UI |
| F4 | Verify notification body includes achievement description | Description present | UI |
| F5 | Tap on "First Meal" notification | Navigates to Stats → Achievements OR shows achievement detail | UI |
| F6 | Screenshot: `flow16_achievement_notifications.png` | Notifications list | UI |

### Phase G: View All Achievements Screen (Locked vs Unlocked)

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| G1 | Navigate to Stats → Achievements | Achievements screen displays | UI |
| G2 | Verify locked achievements have: Grayed-out icon, Lock badge, "???" OR hidden description | Locked state styling | UI |
| G3 | Verify unlocked achievements have: Colored icon, No lock badge, Full description, Unlock date | Unlocked state styling | UI |
| G4 | Verify achievement cards ordered: Unlocked first, then locked | Order correct | UI |
| G5 | Tap "View All" OR scroll down to see all achievements | All 5+ achievements visible | UI |
| G6 | Screenshot: `flow16_all_achievements.png` | Locked vs unlocked display | UI |

### Phase H: Backend API Verification

After Phase G, verify backend achievement data:

```bash
# Get JWT (if not already set)
JWT=$(curl -s -X POST http://localhost:8000/api/v1/auth/firebase \
  -H 'Content-Type: application/json' \
  -d '{"firebase_token":"fake-firebase-token"}' | \
  python -c 'import sys,json;print(json.load(sys.stdin).get("access_token",""))')

# Get all achievements (user-specific, includes unlock status)
curl -s -H "Authorization: Bearer $JWT" \
  http://localhost:8000/api/v1/stats/achievements | jq '.'

# Verify unlocked achievements
curl -s -H "Authorization: Bearer $JWT" \
  http://localhost:8000/api/v1/stats/achievements | \
  python -c "
import sys, json
achievements = json.load(sys.stdin)
unlocked = [a for a in achievements if a.get('unlocked', False)]
locked = [a for a in achievements if not a.get('unlocked', False)]
print(f'Unlocked: {len(unlocked)}, Locked: {len(locked)}')
print('Unlocked achievements:')
for a in unlocked:
    print(f'  - {a[\"name\"]}: {a.get(\"unlock_date\", \"N/A\")}')
"

# Verify cooked recipes count
curl -s -H "Authorization: Bearer $JWT" \
  http://localhost:8000/api/v1/stats/summary | \
  python -c "
import sys, json
stats = json.load(sys.stdin)
cooked_count = stats.get('total_cooked_recipes', 0)
print(f'Total cooked recipes: {cooked_count}')
print('Expected: >= 5 for \"5 Meals\" achievement')
"
```

## Contradictions

This flow tests achievement-specific contradictions:

| ID | Contradiction | Setup | Expected Behavior | Type |
|----|---------------|-------|-------------------|------|
| **C43** | Achievement already unlocked | User cooks 1st recipe (unlocks "First Meal"), then cooks 1st recipe again | No duplicate notification, achievement stays unlocked, unlock_date unchanged | UI |
| **C44** | Achievement requirements not met | User has cooked 4 recipes (not 5) | "5 Meals" achievement remains locked, no notification, no premature unlock | UI |
| **C45** | Cooking same recipe multiple times | User cooks "Masala Dosa" 5 times | Only counts as 1 unique recipe (achievements track unique recipes, not total cooking instances) | UI |

## Fix Strategy

**Relevant files for this flow:**

- **Android:**
  - `app/presentation/stats/StatsScreen.kt` — Tabs including Achievements
  - `app/presentation/stats/AchievementsTab.kt` — Achievement list UI
  - `app/presentation/stats/StatsViewModel.kt` — State management
  - `domain/model/Achievement.kt` — Domain model
  - `domain/model/AchievementItem.kt` — Enriched model with progress
  - `data/local/entity/AchievementEntity.kt` — Room entity
  - `data/local/dao/StatsDao.kt` — Database queries (includes achievements)
  - `data/repository/StatsRepositoryImpl.kt` — Repository

- **Backend:**
  - `app/api/v1/endpoints/stats.py` — Achievement endpoints (GET /stats/achievements)
  - `app/models/achievement.py` — SQLAlchemy model for achievements catalog
  - `app/models/user_achievement.py` — SQLAlchemy model for user unlocks
  - `app/services/achievement_service.py` — Business logic (check criteria, award achievements)
  - `scripts/seed_achievements.py` — Seed default achievements

**Common issues:**

- **Achievement not unlocking:** Verify `achievement_service.check_and_award()` is called after logging cooked recipe
- **Duplicate notifications:** Ensure `user_achievement` table has unique constraint on `(user_id, achievement_id)`
- **Progress not updating:** Verify `AchievementItem.enrichWithProgress()` correctly calculates current progress vs target
- **Same recipe counted multiple times:** Ensure `cooked_recipes` table has unique constraint on `(user_id, recipe_id, cooked_date)` OR service deduplicates

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Stats (Achievements) | A1-A6, C1-C7, E1-E5, G1-G6 | Achievement list, locked/unlocked state, progress |
| Home | B1, D1 | Access recipes for cooking |
| Recipe Detail | B2 | Access Cooking Mode |
| Cooking Mode | B4-B7, D2-D8 | Complete cooking to trigger achievements |
| Notifications | F1-F6 | Achievement unlock notifications |

## Test Data Cleanup

After test completion:

```bash
# Remove test user, cooked recipes, and achievement unlocks
PYTHONPATH=. python scripts/cleanup_user.py
```

## Notes

- Achievement catalog is seeded via `scripts/seed_achievements.py` — run once per database setup
- User achievements are tracked in `user_achievements` table (PostgreSQL) and `achievement_entities` (Room)
- Achievement unlock logic runs server-side in `achievement_service.py` after each cooking event
- Android app fetches updated achievements via `GET /api/v1/stats/achievements` on Stats screen load
- Achievement types: COOKING_MILESTONE, STREAK_MILESTONE, DIET_MILESTONE, SOCIAL_MILESTONE
- Progress tracking: `AchievementItem` has `currentProgress` and `targetProgress` fields (e.g., "2/5 meals cooked")
- Future enhancement: Share achievement unlocks to social feed (requires social feature Phase)
