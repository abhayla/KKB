# Flow 01: New User Journey

## Metadata
- **Flow Name:** `new-user-journey`
- **Goal:** Complete first-time user experience from auth through meal plan generation and full app tour
- **Preconditions:** Fresh app install / clean test data
- **Estimated Duration:** 15-25 minutes
- **Screens Covered:** Auth, Onboarding (5 steps), Home, Grocery, Chat, Favorites, Stats, Settings, Notifications, Recipe Detail, Cooking Mode, Pantry, Recipe Rules
- **Depends On:** none
- **State Produced:** Authenticated user with preferences, 2 meal plans, visited all screens
- **Skip Policy:** NO SKIP ALLOWED — all phases (A through H) must run in every execution, in order
- **Soft Pass Policy:** NO SOFT PASSES — every step is HARD pass/fail. A SOFT result = flow FAILURE
- **Session Policy:** Flow must complete in a single session. "Done in prior session" is NOT acceptable

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] Test user data cleaned (`scripts/cleanup_user.py`)
- [ ] Backend running with Gemini API key configured
- [ ] App freshly installed (or data cleared)
- [ ] Allow 90 seconds per meal plan generation (Gemini AI)

## Test User Persona

**Sharma Family — Initial Setup (Non-Vegetarian):**

| Field | Value |
|-------|-------|
| Email | `e2e-test@rasoiai.test` |
| Auth Token | `fake-firebase-token` |
| Display Name | Abhay Sharma |
| Household Size | 4 |
| Primary Diet | Non-Vegetarian |
| Allergies | Peanuts, Shellfish |
| Cuisines | North Indian, South Indian |
| Spice Level | Medium |
| Disliked Ingredients | Karela, Lauki |
| Weekday Cooking Time | 30 minutes |
| Weekend Cooking Time | 60 minutes |
| Busy Days | Monday, Wednesday |
| Items Per Meal | 2 |

**Settings Changes (before meal plan #2):**

| Setting | Changed To |
|---------|-----------|
| Primary Diet | Vegetarian |
| Cuisines | Add East Indian |
| Spice Level | Mild |
| Disliked Ingredients | Add Capsicum |
| Items Per Meal | 3 |

## Steps

### Phase A: Authentication (Steps 1-3)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| A1 | Launch app, verify auth screen | text="Sign in with Google" visible | `flow01_auth_screen.png` | — |
| A2 | Tap "Sign in with Google" | Fake auth completes, transitions to Onboarding | — | — |
| A3 | Wait 5s, dump UI | Onboarding Step 1 visible (household size) | `flow01_onboarding_start.png` | — |

### Phase B: Onboarding (Steps 4-15)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| B1 | Set household size to 4 | Size selector shows "4" | — | — |
| B2 | Tap "Next" | Step 2: Dietary preferences | — | — |
| B3 | Select "Non-Vegetarian" | Diet option highlighted | — | — |
| B4 | Enter allergies: Peanuts, Shellfish | Allergy chips visible | — | — |
| B5 | Tap "Next" | Step 3: Cuisine preferences | — | — |
| B6 | Select "North Indian" and "South Indian" | Both highlighted | — | — |
| B7 | Tap "Next" | Step 4: Dislikes & cooking time | — | — |
| B8 | Enter disliked: Karela, Lauki | Dislike chips visible | — | — |
| B9 | Set spice level to Medium (use Pattern 14 for dropdown) | Spice selector shows Medium | — | — |
| B10 | Accept default weekday cooking time (do NOT change dropdown — ADB cannot reach popup items, see Pattern 14) | Default value accepted | — | — |
| B11 | Accept default weekend cooking time (do NOT change dropdown — ADB cannot reach popup items, see Pattern 14) | Default value accepted | — | — |
| B12 | Tap "Create My Meal Plan" or "Generate" | Generation begins | `flow01_onboarding_complete.png` | — |

### Phase C: First Meal Plan Generation (Steps 16-20)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| C1 | Wait for meal plan generation (up to 90s) | Loading indicator, then Home screen | — | — |
| C2 | Verify Home screen loaded | text="This Week's Menu", BREAKFAST visible | `flow01_home_plan1.png` | — |
| C3 | Verify meal cards have real food names | Recipe names (not placeholders) in XML | — | — |
| C4 | Run V4a-V4k validation | All HARD checks pass | — | V4a-V4k |

### Pre-Checkpoint 1: Correct Dropdown Values via Backend API

Onboarding dropdowns (cooking times) could not be set via ADB (see Pattern 14 — Compose popup window limitation). Correct them via backend API before running validation:

```bash
# Get JWT
JWT=$(curl -s -X POST http://localhost:8000/api/v1/auth/firebase \
  -H 'Content-Type: application/json' \
  -d '{"firebase_token":"fake-firebase-token"}' | \
  python -c 'import sys,json;print(json.load(sys.stdin).get("access_token",""))')

# Correct cooking times to match test persona (30 weekday, 60 weekend)
curl -s -X PUT http://localhost:8000/api/v1/users/preferences \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"weekday_cooking_time": 30, "weekend_cooking_time": 60}'
```

### Checkpoint 1: After Plan #1 Generation
```bash
python scripts/validate_meal_plan.py \
  --jwt "$JWT" \
  --checks V4a,V4b,V4c,V4d,V4e,V4f,V4g,V4h,V4i,V4j,V4k \
  --expected-diet "Non-Vegetarian" \
  --expected-items-per-meal 2 \
  --disliked "Karela,Lauki" \
  --cuisines "North Indian,South Indian" \
  --max-spice "Medium" \
  --weekday-cooking-time 30 \
  --weekend-cooking-time 60 \
  --busy-days "Monday,Wednesday" \
  --output "$LOG_DIR/flow01_validation1.json"
```

### Phase D: Settings Changes (Steps 21-30) — MANDATORY, NO SKIP

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| D1 | Navigate: tap Profile icon | Settings screen | `flow01_settings.png` | — |
| D2 | Find and tap dietary setting | Dietary preference screen/dialog | — | — |
| D3 | Change diet to Vegetarian | Vegetarian selected | — | — |
| D4 | Navigate back to Settings | Settings screen | — | — |
| D5 | Find and tap cuisine setting | Cuisine preference screen | — | — |
| D6 | Add "East Indian" cuisine | 3 cuisines selected | — | — |
| D7 | Navigate back to Settings | Settings screen | — | — |
| D8 | Change spice level to Mild | Mild selected | — | — |
| D9 | Scroll down, find disliked setting | Disliked ingredients screen | — | — |
| D10 | Add "Capsicum" to dislikes | Capsicum added | — | — |
| D11 | Change items per meal to 3 | Items per meal = 3 | — | — |
| D12 | Navigate back to Home | Home screen | `flow01_settings_changed.png` | — |

### Backend Cross-Validation: Settings Persistence

After changing settings in Phase D, verify backend received the updates:

```bash
# Verify all settings changes persisted to backend
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/users/me | \
  python -c "
import sys, json
d = json.load(sys.stdin)
checks = {
    'dietary_type': ('Vegetarian', d.get('dietary_type')),
    'spice_level': ('Mild', d.get('spice_level')),
    'items_per_meal': (3, d.get('items_per_meal')),
}
for k, (exp, act) in checks.items():
    status = 'PASS' if str(exp).lower() == str(act).lower() else 'FAIL'
    print(f'{k}: expected={exp}, actual={act} -> {status}')
# Also verify disliked ingredients include Capsicum
dislikes = d.get('disliked_ingredients', [])
has_capsicum = any('capsicum' in x.lower() for x in dislikes)
print(f'disliked_has_capsicum: {has_capsicum} -> {\"PASS\" if has_capsicum else \"FAIL\"}')
# Verify East Indian in cuisines
cuisines = d.get('cuisines', [])
has_east = any('east' in x.lower() for x in cuisines)
print(f'cuisines_has_east_indian: {has_east} -> {\"PASS\" if has_east else \"FAIL\"}')
"
```

### Phase E: Contradictions C1-C5 (Inline) — MANDATORY, NO SKIP

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| E1 | **C1:** Diet is now Vegetarian, but check if Eggs/non-veg INCLUDE rules exist from onboarding | If diet changed, INCLUDE non-veg should still be stored | — | HARD |
| E2 | **C2:** Verify Peanuts in both allergies AND potential disliked (if overlap created) | Both stored, no duplicate error on settings | — | HARD |
| E3 | **C3:** Note: Dadaji=Jain would conflict with North Indian cuisine if family members added | Deferred to Flow 2 (family member CRUD) | — | HARD |
| E4 | **C4/C5:** Household size vs member count — attempt add member if settings allows | Verify household size mismatch handling | — | HARD |

### Phase F: Second Meal Plan Generation (Steps 31-35) — MANDATORY, NO SKIP

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| F1 | Tap Refresh/Regenerate on Home | Refresh options appear | — | — |
| F2 | Select "Entire Week" | Generation starts | — | — |
| F3 | Wait for generation (up to 90s) | New meal plan loads | `flow01_home_plan2.png` | — |
| F4 | Verify meal cards updated | Different recipes from Plan #1 | — | — |
| F5 | Run V4a-V4k validation | Reflects Vegetarian, 3 items/meal, Mild, no Capsicum | — | V4a-V4k |

### Checkpoint 2: After Plan #2 Generation
```bash
python scripts/validate_meal_plan.py \
  --jwt "$JWT" \
  --checks V4a,V4b,V4c,V4d,V4e,V4f,V4g,V4h,V4i,V4j,V4k \
  --expected-diet "Vegetarian" \
  --expected-items-per-meal 3 \
  --disliked "Karela,Lauki,Capsicum" \
  --cuisines "North Indian,South Indian,East Indian" \
  --max-spice "Mild" \
  --weekday-cooking-time 30 \
  --weekend-cooking-time 60 \
  --busy-days "Monday,Wednesday" \
  --output "$LOG_DIR/flow01_validation2.json"
```

### Phase G: 26-Screen Tour (Steps 36-80)

Tour all major screens and verify they load properly after plan generation.

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| G1 | Tap day tab (e.g., TUE) | Different day's meals shown | — | — |
| G2 | Scroll down to DINNER section | DINNER visible | — | — |
| G3 | Scroll down to SNACKS section | SNACKS visible | `flow01_home_snacks.png` | — |
| G4 | Scroll back to top | BREAKFAST visible | — | — |
| G5 | Tap bottom nav "Grocery" | Grocery screen with categories | `flow01_grocery.png` | HARD |
| G6 | Verify grocery categories exist | At least 3 categories visible (auto-generated from meal plan) | — | HARD |
| G6a | Tap a grocery item checkbox | Item marked as purchased (strikethrough) | — | — |
| G6b | Tap "Add custom item" if visible | Add item dialog appears | — | — |
| G6c | Press BACK to dismiss dialog | Return to Grocery | — | — |
| G7 | Tap bottom nav "Chat" | Chat screen with input field | `flow01_chat.png` | — |
| G8 | Verify chat welcome message | "Hi" or "Hello" or assistant greeting | — | — |
| G8a | Verify attachment button exists | content-desc "Attach" or "Attachment" visible in XML | — | — |
| G8b | Verify voice input button exists | content-desc "Voice" visible in XML | — | — |
| G8c | Verify quick action chips exist | Suggestion chips visible below welcome message | — | — |
| G9 | Tap bottom nav "Favs" | Favorites screen (empty state expected) | `flow01_favorites.png` | — |
| G10 | Verify empty state message | "No favorites" or similar | — | — |
| G11 | Tap bottom nav "Stats" | Stats screen | `flow01_stats.png` | — |
| G12 | Verify stats sections exist | Streak, time tabs visible | — | — |
| G13 | Tap bottom nav "Home" | Return to Home | — | — |
| G14 | Tap Profile icon | Settings screen | — | — |
| G15 | Verify settings loaded | Email, dietary, cuisine visible | — | — |
| G16 | Tap Notifications icon (back to Home first) | Notifications screen | `flow01_notifications.png` | — |
| G17 | Verify notifications (empty state OK) | Screen loads without crash | — | — |
| G18 | Press BACK to Home | Home screen | — | — |
| G19 | Tap a BREAKFAST meal card | Action sheet appears | — | — |
| G19a | Verify action sheet has 4 items | "View Recipe", "Swap Recipe", "Lock Recipe", "Remove from Meal" all in XML | — | HARD |
| G20 | Tap "View Recipe" | Recipe Detail screen (real recipe data, not "Recipe not found") | `flow01_recipe_detail.png` | HARD |
| G21 | Verify ingredients section | "Ingredients" heading + items (real ingredient list) | — | HARD |
| G22 | Scroll to "Start Cooking" button | Button visible | — | HARD |
| G23 | Tap "Start Cooking" | Cooking Mode screen | `flow01_cooking_mode.png` | HARD |
| G23a | Verify voice guidance toggle exists | content-desc "Voice" or toggle visible | — | — |
| G23b | Verify progress indicator exists | Progress bar or step indicator visible | — | — |
| G24 | Verify step counter | "Step 1 of N" visible (real cooking steps) | — | HARD |
| G25 | Tap Next step | Step 2 shown | — | HARD |
| G26 | Press BACK twice | Return to Home | — | — |
| G27 | Navigate: Profile → Settings → scroll to Pantry | Pantry link visible in MEAL PREFERENCES | — | HARD |
| G28 | Tap "Pantry" | Pantry screen | `flow01_pantry.png` | HARD |
| G29 | Verify Pantry loads | Title + Add button visible | — | HARD |
| G30 | Press BACK | Settings screen | — | HARD |
| G31 | Scroll to "Recipe Rules" | Recipe Rules link visible | — | — |
| G32 | Tap "Recipe Rules" | Recipe Rules screen | `flow01_recipe_rules.png` | — |
| G33 | Verify Rules tab exists | "Rules" tab visible | — | — |
| G34 | Verify Nutrition tab exists | "Nutrition" tab visible | — | — |
| G35 | Press BACK | Settings screen | — | — |
| G36 | Press BACK | Home screen | — | — |

### Phase H: Final Verification (Steps 81-83)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| H1 | Take final Home screenshot | All sections loaded | `flow01_final.png` | — |
| H2 | Run crash/ANR detection (Pattern 9) | No crashes detected | — | — |
| H3 | Capture app logs (Pattern 13) | Logs saved to session dir | — | — |

## Validation Checkpoints

Two validation checkpoints using `validate_meal_plan.py`:

1. **Checkpoint 1 (Phase C):** Non-Vegetarian, 2 items/meal, Medium spice
2. **Checkpoint 2 (Phase F):** Vegetarian, 3 items/meal, Mild spice, +Capsicum disliked

## Fix Strategy

**Relevant files for this flow:**
- Auth issues: `app/presentation/auth/AuthViewModel.kt`, `backend/app/services/auth_service.py`
- Onboarding issues: `app/presentation/onboarding/OnboardingViewModel.kt`, `app/presentation/onboarding/OnboardingScreen.kt`
- Home screen issues: `app/presentation/home/HomeViewModel.kt`, `app/presentation/home/HomeScreen.kt`
- Meal generation: `backend/app/services/ai_meal_service.py`, `backend/app/api/v1/endpoints/meal_plans.py`
- Settings: `app/presentation/settings/SettingsViewModel.kt`
- Navigation: `app/presentation/navigation/RasoiNavGraph.kt`

**Common issues:**
- Gemini timeout → retry once, then generate via backend API directly
- Onboarding stuck on dropdown → ADB `input tap` cannot reach Compose popup windows (see Pattern 14). Accept default dropdown values in UI, then correct via backend API after onboarding completes.
- Settings changes not persisting → verify DataStore + backend sync

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Auth | A1-A2 | Sign in button, fake auth flow |
| Onboarding | B1-B12 | All 5 steps completed |
| Home | C2-C3, F3-F4, G1-G4 | Meal plan data, day tabs, scroll |
| Grocery | G5-G6 | Categories, items from plan |
| Chat | G7-G8 | Input field, welcome message |
| Favorites | G9-G10 | Empty state |
| Stats | G11-G12 | Sections load |
| Settings | D1-D12, G14-G15 | All preference changes |
| Notifications | G16-G17 | Screen loads |
| Recipe Detail | G20-G22 | Ingredients, instructions |
| Cooking Mode | G23-G25 | Step navigation |
| Pantry | G28-G29 | Screen loads |
| Recipe Rules | G32-G34 | Tabs visible |
