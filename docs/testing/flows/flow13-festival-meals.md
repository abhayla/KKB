# Flow 13: Festival-Aware Meal Planning

## Metadata

| Attribute | Value |
|-----------|-------|
| **Flow Name** | festival-meals |
| **Flow ID** | FLOW-13 |
| **Goal** | Verify AI generates appropriate meals for festival/fasting periods |
| **Priority** | P1 |
| **Complexity** | Medium |
| **Estimated Duration** | 5-7 minutes |
| **Last Updated** | 2026-02-14 |

## Prerequisites

- Backend API running (`uvicorn app.main:app --reload`)
- PostgreSQL database with schema applied (`alembic upgrade head`)
- Festival data seeded in DB (`PYTHONPATH=. python scripts/seed_festivals.py`)
- Android emulator running (API 34)
- AI meal generation working (Gemini API key configured)
- User authenticated via fake-firebase-token

## Depends On

- **Flow 01** (auth-onboarding-mealgen-home) — Must complete authentication and initial onboarding

## Test User Persona

**Sharma Family (Navratri Observers)**

| Member | Type | Age | Dietary Preference | Festival Observance |
|--------|------|-----|--------------------|---------------------|
| Priya Sharma | ADULT | 35 | Vegetarian | Observes Navratri fasting (9 days) |

**Festival Setup:**
- Festival: Navratri (9 days)
- Fasting Rules: No onion, no garlic, no non-veg, Sattvic only
- Special Dishes: Sabudana Khichdi, Kuttu ki Puri, Singhare ka Halwa, Fruit Salad

## Test Phases

### Phase A: Seed Festival Data

| Step | Action | Verification |
|------|--------|--------------|
| A1 | Run `PYTHONPATH=. python scripts/seed_festivals.py` | Script completes without errors |
| A2 | Verify backend logs show "Festival data seeded successfully" | Logs confirm |
| A3 | Call `GET /api/v1/festivals` via Swagger | Returns festivals including Navratri |
| A4 | Check Navratri entry: `is_fasting_day: true`, `date` range covers 9 days | Correct |
| A5 | Verify `festival_type: "NAVRATRI"` (or similar enum) | Correct |

### Phase B: Generate Meal Plan for Navratri Week

| Step | Action | Verification |
|------|--------|--------------|
| B1 | Set device/emulator date to Navratri start date (or modify user preferences to target week) | Date set |
| B2 | Navigate to Home screen | Home screen displays |
| B3 | Tap "Generate New Plan" | Loading indicator appears |
| B4 | Wait for meal plan generation (4-7 seconds) | Meal plan cards appear for 7 days |
| B5 | Check backend logs for AI prompt | Prompt includes: "Navratri fasting days: [dates]" |
| B6 | Verify prompt mentions: "No onion, no garlic, Sattvic, vrat-friendly ingredients" | Constraints included |

### Phase C: Validate Fasting Day Meals

| Step | Action | Verification |
|------|--------|--------------|
| C1 | Scan meals for Navratri days (9 days or subset in 7-day plan) | All meals comply with fasting rules |
| C2 | Check for NO onion/garlic in any meal | None present |
| C3 | Check for NO non-veg items | Only veg/Sattvic items |
| C4 | Check for Sattvic ingredients only | No tamasic foods (meat, onion, garlic, alcohol) |
| C5 | Check for special Navratri dishes | Includes Sabudana Khichdi, Kuttu Puri, Singhare Halwa, etc. |
| C6 | Tap on a Navratri meal recipe card → Recipe Detail | Recipe shows vrat-friendly ingredients (sabudana, kuttu, singhare, sendha namak) |
| C7 | Verify recipe instructions mention "vrat" or "fasting" | Special notes included |
| C8 | Check Home screen for festival badge/label on relevant days | "Navratri" badge visible on meal cards |

### Phase D: Contradictions C34-C35

| Contradiction | Setup | Action | Expected Behavior |
|---------------|-------|--------|-------------------|
| **C34**: Non-veg user on fasting day | User pref: non-vegetarian, Navratri fasting day | Generate plan for fasting day | Fasting rules override user diet (veg Sattvic meals) OR AI explains conflict |
| **C35**: INCLUDE non-veg on fasting day | Navratri fasting day | Add recipe rule: INCLUDE "Chicken Curry" LUNCH on fasting date | System shows warning: "Cannot include non-veg on fasting day" OR AI skips rule |

### Phase E: Verify Home Screen Festival Display

| Step | Action | Verification |
|------|--------|--------------|
| E1 | Navigate to Home screen | Meal plan displayed |
| E2 | Check meal cards for Navratri days | Badge/chip shows "Navratri" or "🪔" icon |
| E3 | Verify non-fasting days have no badge | Regular meals, no festival badge |
| E4 | Tap on festival badge (if clickable) | Shows festival info dialog (optional feature) |
| E5 | Scroll through all 7 days | Festival badges appear only on correct dates |

## Contradictions Summary

| ID | Contradiction | Fix Strategy |
|----|---------------|--------------|
| C34 | Non-veg user preference vs fasting day rules | Fasting rules override user diet (festival takes precedence) |
| C35 | INCLUDE non-veg rule on fasting day | System blocks rule creation OR AI skips rule with explanation |

## Fix Strategy

**For C34 (Non-veg user on fasting day):**
- Backend: `ai_meal_service.py` checks if meal date matches `festivals.is_fasting_day = true`
- AI prompt: "Date 2026-04-10 is Navratri (fasting). Override user non-veg preference → Sattvic veg only"
- Gemini generates veg Sattvic meals regardless of user dietary_type
- Optional: Show snackbar on Home screen: "Fasting rules applied for Navratri"

**For C35 (INCLUDE non-veg on fasting day):**
- Backend: `recipe_rules_service.py` validates rule before insert
- Check: If `enforcement.specific_date` matches festival fasting day + rule is INCLUDE + recipe has non-veg tag → return 400
- Error message: "Cannot include non-vegetarian items on fasting day (Navratri: 2026-04-10)"
- OR: AI prompt includes rule → Gemini response: "Skipped 'Chicken Curry' INCLUDE due to Navratri fasting rules"

## Test Data Cleanup

After test completion:
```bash
# Remove test user and meal plan
PYTHONPATH=. python scripts/cleanup_user.py

# Festival data persists (shared across users)
# To re-seed festivals:
PYTHONPATH=. python scripts/seed_festivals.py
```

## Related Files

- Android: `app/presentation/home/HomeScreen.kt` (festival badge display)
- Android: `domain/model/MealPlanDay.kt` (festival field)
- Backend: `app/api/v1/endpoints/festivals.py`
- Backend: `app/models/festival.py`
- Backend: `app/ai/meal_generation_service.py` (festival-aware prompt)
- Backend: `scripts/seed_festivals.py`
- Tests: `backend/tests/test_festivals_api.py` (9 tests)

## Notes

- Navratri dates vary yearly (lunar calendar) — seed script should use relative dates or current year
- Fasting rules vary by region (North vs South India) — initial implementation uses North Indian rules
- Some users observe partial fasting (1 meal/day) vs full fasting (fruits only) — MVP uses standard vrat rules
- Festival data is shared across all users (not user-specific)
