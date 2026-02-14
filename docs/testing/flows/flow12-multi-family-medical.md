# Flow 12: Multi-Family Medical Edge Cases

## Metadata

| Attribute | Value |
|-----------|-------|
| **Flow Name** | multi-family-medical |
| **Flow ID** | FLOW-12 |
| **Goal** | Test extended joint family with diverse medical conditions affecting meal generation |
| **Priority** | P1 |
| **Complexity** | High |
| **Estimated Duration** | 8-10 minutes |
| **Last Updated** | 2026-02-14 |

## Prerequisites

- Backend API running (`uvicorn app.main:app --reload`)
- PostgreSQL database with schema applied (`alembic upgrade head`)
- Android emulator running (API 34)
- Family members CRUD endpoint working (`/api/v1/family-members`)
- AI meal generation working (Gemini API key configured)
- User authenticated via fake-firebase-token

## Depends On

- **Flow 01** (auth-onboarding-mealgen-home) — Must complete authentication and initial onboarding

## Test User Persona

**Extended Sharma Joint Family** (6 members)

| Member | Type | Age | Health Conditions | Per-Member Diet |
|--------|------|-----|-------------------|-----------------|
| Dadaji (Grandfather) | SENIOR | 75 | diabetic, soft_food | vegetarian, low_salt |
| Dadiji (Grandmother) | SENIOR | 70 | low_salt | Jain (no root vegetables) |
| Papa (Father) | ADULT | 45 | high_protein | non-vegetarian |
| Mummy (Mother) | ADULT | 40 | low_oil | vegetarian |
| Rahul (Teenager) | ADULT | 16 | (none) | (follows household) |
| Chhoti (Child) | CHILD | 6 | no_spicy, soft_food | vegetarian |

## Test Phases

### Phase A: CRUD All 6 Family Members

| Step | Action | Verification |
|------|--------|--------------|
| A1 | Tap Settings from bottom nav | Settings screen displays |
| A2 | Tap "Family Members" | Family Members screen shows empty state or existing members |
| A3 | Tap FAB "Add Member" | Add Member dialog appears |
| A4 | Enter Dadaji: name, type=SENIOR, age=75 | Fields populated |
| A5 | Add health_conditions: diabetic, soft_food | Chips display |
| A6 | Add dietary_preferences: vegetarian, low_salt | Chips display |
| A7 | Tap "Save" | Dialog closes, Dadaji appears in list |
| A8 | Repeat A3-A7 for Dadiji (SENIOR, 70, low_salt, Jain) | Dadiji added |
| A9 | Repeat A3-A7 for Papa (ADULT, 45, high_protein, non-veg) | Papa added |
| A10 | Repeat A3-A7 for Mummy (ADULT, 40, low_oil, veg) | Mummy added |
| A11 | Repeat A3-A7 for Rahul (ADULT, 16, none, household) | Rahul added |
| A12 | Repeat A3-A7 for Chhoti (CHILD, 6, no_spicy+soft_food, veg) | Chhoti added |
| A13 | Verify all 6 members visible in list | 6 family member cards displayed |

### Phase B: Verify Backend API Persistence

| Step | Action | Verification |
|------|--------|--------------|
| B1 | Call `GET /api/v1/family-members` via Swagger or curl | Returns array of 6 members |
| B2 | Verify Dadaji JSON: `health_conditions: ["diabetic", "soft_food"]` | Correct |
| B3 | Verify Dadiji JSON: `dietary_preferences: ["Jain"]` | Correct (no root veg) |
| B4 | Verify Papa JSON: `dietary_preferences: ["non-vegetarian"]` | Correct |
| B5 | Verify Chhoti JSON: `health_conditions: ["no_spicy", "soft_food"]`, `member_type: "CHILD"` | Correct |
| B6 | Verify all IDs are UUID format | Valid UUIDs |

### Phase C: Generate Meal Plan with All Members

| Step | Action | Verification |
|------|--------|--------------|
| C1 | Navigate back to Home screen | Home screen displays |
| C2 | Tap "Generate New Plan" (if exists) or wait for auto-generation | Loading indicator appears |
| C3 | Wait for meal plan generation (4-7 seconds) | Meal plan cards appear for 7 days |
| C4 | Check backend logs for AI prompt | Prompt includes all 6 family members with health conditions |
| C5 | Verify prompt mentions: "diabetic", "soft_food", "Jain", "high_protein", "no_spicy", "low_salt", "low_oil" | All constraints included |

### Phase D: Validate Meal Plan Constraints

| Step | Action | Verification |
|------|--------|--------------|
| D1 | Scan all 21 meals (7 days × 3 meals) | No high-sugar items for diabetic (no sweets, limited rice) |
| D2 | Check for Jain compliance | No onions, garlic, potatoes, carrots (root vegetables) |
| D3 | Check for soft food options | Includes dal, khichdi, soft roti, mashed items |
| D4 | Check for child-friendly meals | No spicy curries, mild flavors for Chhoti |
| D5 | Check for low-salt options | Meals suitable for Dadaji and Dadiji |
| D6 | Check for high-protein options | Includes paneer, legumes, or non-veg for Papa |
| D7 | Tap on a recipe card → Recipe Detail | Recipe shows ingredients compatible with constraints |

### Phase E: Contradictions C28-C33

| Contradiction | Setup | Action | Expected Behavior |
|---------------|-------|--------|-------------------|
| **C28**: Diabetic + Sweet INCLUDE | Dadaji is diabetic | Add recipe rule: INCLUDE "Gulab Jamun" DINNER | System shows warning or AI skips the rule with explanation |
| **C29**: Per-member diet override | Papa non-veg, household veg | Generate plan | Papa gets non-veg options OR separate meal suggestions |
| **C30**: Delete member mid-week | 6 members, meal plan exists | Delete Chhoti via Settings → regenerate plan | New plan adjusts (no more child-friendly constraint) |
| **C31**: Impossible constraints | All 6 members with conflicting diets | Add rule: INCLUDE "Onion Curry" (conflicts Jain) | AI explains conflict in chat or skips rule |
| **C32**: Age change triggers update | Chhoti age 6 (CHILD) | Edit Chhoti → age 18 → type ADULT | Health needs update prompt or auto-adjust constraints |
| **C33**: Prompt token limit | 6 members × 7 health/diet tags | Generate plan | AI successfully processes (no 400/413 error) or graceful degradation |

### Phase F: Verify Family Member Display

| Step | Action | Verification |
|------|--------|--------------|
| F1 | Go to Settings → Family Members | All 6 members listed with names |
| F2 | Tap on Dadaji card | Shows age 75, SENIOR, diabetic, soft_food, veg, low_salt |
| F3 | Tap Edit on Dadiji | Edit dialog shows current values (70, Jain, low_salt) |
| F4 | Tap Delete on Rahul → Confirm | Rahul removed, count = 5 |
| F5 | Regenerate meal plan | New plan reflects 5 members (no Rahul constraints) |

## Contradictions Summary

| ID | Contradiction | Fix Strategy |
|----|---------------|--------------|
| C28 | Diabetic member + Sweet INCLUDE rule | AI skips rule with explanation OR system shows warning before adding |
| C29 | Per-member non-veg vs household veg | Per-member diet overrides household preference (Papa gets non-veg) |
| C30 | Delete member mid-week | Regenerate plan adjusts constraints (remove deleted member's needs) |
| C31 | All members conflicting diets | AI explains conflict in chat, suggests relaxing constraints |
| C32 | Age change CHILD → ADULT | Prompt user to update health needs or auto-remove child constraints |
| C33 | 6 members × many tags = token limit | AI prompt stays within limits (use summary format) or return 413 with retry |

## Fix Strategy

**For C28 (Diabetic + Sweet):**
- Backend: `ai_meal_service.py` checks for conflicts between health_conditions and INCLUDE rules
- Return 400 with message: "Cannot include high-sugar items for diabetic family member"
- OR: AI prompt includes conflict → Gemini skips rule + explains in response

**For C29 (Per-member diet override):**
- Backend: `family_members` table has `dietary_preferences` column
- AI prompt format: "Household: vegetarian, BUT Papa (45, ADULT): non-vegetarian, high_protein"
- Gemini generates separate non-veg option for Papa OR notes "Papa can substitute with non-veg"

**For C30 (Delete member mid-week):**
- Android: After delete → show snackbar "Regenerate plan to reflect changes?"
- Backend: Fresh meal generation fetches current family_members only

**For C31 (Impossible constraints):**
- Backend: AI prompt includes all constraints
- Gemini response: "Cannot satisfy all constraints simultaneously. Suggest: relax Jain rule OR exclude onion INCLUDE"
- Chat shows explanation

**For C32 (Age change triggers update):**
- Android: On age edit → if crosses 18 threshold → dialog "Update health needs?"
- Auto-remove `no_spicy` if CHILD → ADULT (configurable)

**For C33 (Token limit):**
- Backend: Limit prompt to 8000 tokens
- If exceeds → use summary format: "6 members: 2 SENIOR (diabetic, Jain), 2 ADULT (veg, non-veg), 1 CHILD (no spicy)"
- If still exceeds → return 413 "Too many family members, please reduce constraints"

## Test Data Cleanup

After test completion:
```bash
# Remove test user and all family members
PYTHONPATH=. python scripts/cleanup_user.py
```

## Related Files

- Android: `app/presentation/settings/screens/FamilyMembersSettings.kt`
- Android: `domain/model/FamilyMember.kt`
- Backend: `app/api/v1/endpoints/family_members.py`
- Backend: `app/models/user.py` (FamilyMember model)
- Backend: `app/ai/meal_generation_service.py` (prompt includes family members)
- Tests: `backend/tests/test_family_members_api.py`
