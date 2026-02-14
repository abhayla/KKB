# Flow 14: Nutrition Goals & Impact

## Metadata

| Attribute | Value |
|-----------|-------|
| **Flow Name** | nutrition-goals |
| **Flow ID** | FLOW-14 |
| **Goal** | Test nutrition goal CRUD and impact on meal generation |
| **Priority** | P1 |
| **Complexity** | Medium |
| **Estimated Duration** | 6-8 minutes |
| **Last Updated** | 2026-02-14 |

## Prerequisites

- Backend API running (`uvicorn app.main:app --reload`)
- PostgreSQL database with schema applied (`alembic upgrade head`)
- Android emulator running (API 34)
- AI meal generation working (Gemini API key configured)
- User authenticated via fake-firebase-token
- Recipe Rules screen with Nutrition tab implemented

## Depends On

- **Flow 01** (auth-onboarding-mealgen-home) — Must complete authentication and initial onboarding

## Test User Persona

**Health-Conscious Sharma Family**

| Member | Type | Age | Dietary Preference | Nutrition Goals |
|--------|------|-----|--------------------|-----------------|
| Rajesh Sharma | ADULT | 40 | Vegetarian | High protein (muscle building), more vegetables |

**Nutrition Goal Targets:**
- Protein: 10 servings/week (initial) → 12 servings/week (edited)
- Vegetables: 14 servings/week (initial) → deleted

## Test Phases

### Phase A: Navigate to Recipe Rules → Nutrition Tab

| Step | Action | Verification |
|------|--------|--------------|
| A1 | Tap Settings from bottom nav | Settings screen displays |
| A2 | Tap "Recipe Rules" (or navigate via Home → overflow menu) | Recipe Rules screen displays |
| A3 | Verify 2 tabs visible: "Rules" and "Nutrition" | Both tabs present |
| A4 | Tap "Nutrition" tab | Nutrition tab content displays |
| A5 | Verify initial state: empty state or existing goals | "No nutrition goals yet" OR list of goals |
| A6 | Verify FAB "Add Goal" visible | FAB present |

### Phase B: CRUD Nutrition Goals

| Step | Action | Verification |
|------|--------|--------------|
| **B1 - Add Protein Goal** | | |
| B1.1 | Tap FAB "Add Goal" | Add Nutrition Goal dialog appears |
| B1.2 | Select category: "Protein" (dropdown or chips) | Protein selected |
| B1.3 | Enter target_servings: 10 | Field populated |
| B1.4 | Select timeframe: "WEEKLY" (default) | Weekly selected |
| B1.5 | Verify is_active: true (default toggle ON) | Toggle ON |
| B1.6 | Tap "Save" | Dialog closes |
| B1.7 | Verify Protein goal appears in list: "Protein: 10 servings/week" | Goal card displays |
| **B2 - Add Vegetables Goal** | | |
| B2.1 | Tap FAB "Add Goal" again | Add dialog appears |
| B2.2 | Select category: "Vegetables" | Vegetables selected |
| B2.3 | Enter target_servings: 14 | Field populated |
| B2.4 | Timeframe: "WEEKLY" | Weekly selected |
| B2.5 | Tap "Save" | Dialog closes |
| B2.6 | Verify Vegetables goal in list: "Vegetables: 14 servings/week" | Goal card displays |
| B2.7 | Verify total goals count: 2 | Two cards visible |
| **B3 - Edit Protein Goal** | | |
| B3.1 | Tap on Protein goal card | Edit dialog appears with current values |
| B3.2 | Change target_servings from 10 to 12 | Field updated |
| B3.3 | Tap "Save" | Dialog closes |
| B3.4 | Verify Protein goal now shows: "Protein: 12 servings/week" | Updated value displays |
| **B4 - Delete Vegetables Goal** | | |
| B4.1 | Swipe left on Vegetables goal card (or tap delete icon) | Delete confirmation dialog appears |
| B4.2 | Confirm deletion | Dialog closes |
| B4.3 | Verify Vegetables goal removed from list | Only Protein goal remains (count = 1) |

### Phase C: Verify Backend Persistence

| Step | Action | Verification |
|------|--------|--------------|
| C1 | Call `GET /api/v1/nutrition-goals` via Swagger or curl | Returns array with 1 goal (Protein) |
| C2 | Verify Protein goal JSON: `category: "PROTEIN"`, `target_servings: 12`, `timeframe: "WEEKLY"`, `is_active: true` | Correct |
| C3 | Verify Vegetables goal NOT present (deleted) | Only 1 goal in response |
| C4 | Call `POST /api/v1/nutrition-goals` to add Vegetables again (14 servings) | 201 Created |
| C5 | Call `GET /api/v1/nutrition-goals` again | Returns 2 goals |
| C6 | Call `DELETE /api/v1/nutrition-goals/{vegetables_goal_id}` | 204 No Content |
| C7 | Verify backend logs show CRUD operations | Logs confirm all operations |

### Phase D: Generate Meal Plan with Nutrition Goals

| Step | Action | Verification |
|------|--------|--------------|
| D1 | Navigate to Home screen | Home screen displays |
| D2 | Tap "Generate New Plan" (or wait for auto-generation) | Loading indicator appears |
| D3 | Wait for meal plan generation (4-7 seconds) | Meal plan cards appear for 7 days |
| D4 | Check backend logs for AI prompt | Prompt includes: "Nutrition goals: Protein 12 servings/week" |
| D5 | Scan all 21 meals (7 days × 3 meals) | Verify high-protein items present (paneer, dal, legumes, tofu, curd) |
| D6 | Count protein servings across week | Should total ~12 or close (AI best effort) |
| D7 | Tap on a high-protein recipe → Recipe Detail | Recipe shows protein-rich ingredients |
| D8 | Verify recipe tags include "High Protein" (if tagging exists) | Tag present (optional) |

### Phase E: Contradictions C36-C37

| Contradiction | Setup | Action | Expected Behavior |
|---------------|-------|--------|-------------------|
| **C36**: Impossible target | User has 1 nutrition goal | Add goal: Protein 50 servings/week (7 days × max 3 meals = 21 servings) | System shows warning: "Target exceeds weekly meal count" OR AI does best effort + explains in chat |
| **C37**: Duplicate goal category | Protein goal already exists (12/week) | Add another goal: Protein 8/week | System returns 409 Conflict: "Nutrition goal for PROTEIN already exists" |

## Contradictions Summary

| ID | Contradiction | Fix Strategy |
|----|---------------|--------------|
| C36 | Impossible target (50 protein servings in 21 meals) | Frontend validation: warn if target > 21 (7 days × 3 meals). Backend: AI does best effort, no error. |
| C37 | Duplicate goal category | Backend unique constraint on (user_id, category). Return 409 with message. |

## Fix Strategy

**For C36 (Impossible target):**
- Android: `NutritionGoalsViewModel` validates target before save
- Check: `if (targetServings > 21 && timeframe == WEEKLY)` → show warning dialog
- Warning: "Weekly meal plan has max 21 meals (7 days × 3). Target 50 servings may not be achievable."
- User can proceed or edit target
- Backend: AI prompt includes goal → Gemini does best effort → response includes note: "Targeted 50 protein servings, achieved 21 (maximum possible)"
- No 400 error, graceful handling

**For C37 (Duplicate goal category):**
- Backend: `nutrition_goals` table has unique constraint: `UNIQUE(user_id, category)`
- `nutrition_goals_service.py` catches `IntegrityError` on insert
- Return 409 Conflict: `{"detail": "Nutrition goal for category 'PROTEIN' already exists. Please edit the existing goal."}`
- Android: Catch 409 → show snackbar: "You already have a Protein goal. Edit it instead."

## Test Data Cleanup

After test completion:
```bash
# Remove test user and all nutrition goals
PYTHONPATH=. python scripts/cleanup_user.py
```

## Related Files

- Android: `app/presentation/reciperules/RecipeRulesScreen.kt` (tabs)
- Android: `app/presentation/reciperules/NutritionGoalsTab.kt` (nutrition tab content)
- Android: `app/presentation/reciperules/NutritionGoalsViewModel.kt`
- Android: `domain/model/NutritionGoal.kt`
- Backend: `app/api/v1/endpoints/recipe_rules.py` (nutrition goals endpoints are here)
- Backend: `app/models/nutrition_goal.py`
- Backend: `app/services/nutrition_goals_service.py`
- Backend: `app/ai/meal_generation_service.py` (nutrition goals in prompt)
- Tests: `backend/tests/test_nutrition_goals_api.py` (part of FR-010)

## Notes

- Nutrition goal categories are enum: PROTEIN, VEGETABLES, FRUITS, GRAINS, DAIRY (see `FoodCategory` enum in domain model)
- Timeframe enum: DAILY, WEEKLY, MONTHLY (MVP uses WEEKLY only)
- AI impact is "best effort" — Gemini tries to meet targets but doesn't guarantee exact servings
- Future enhancement: Track actual servings consumed (requires cooked_recipes + nutrition data)
- Recipe Rule INCLUDE/EXCLUDE can conflict with nutrition goals (e.g., EXCLUDE all protein sources) — AI handles via prompt reasoning
