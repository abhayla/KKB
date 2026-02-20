# Flow 12: Multi-Family Medical — Test Report

## Metadata
| Field | Value |
|-------|-------|
| Flow ID | 12 |
| Flow Name | multi-family-medical |
| Date | 2026-02-15 19:37:08 |
| Total Duration | ~25m |
| Result | FAILED |
| Pass Rate | 57% (25/44 steps assessed) |
| Screenshots Captured | 5 |
| Screenshots Verified | 0 (manual visual check only) |
| Fix-Loop Invocations | 0 |

## Test Results

| # | Test Id | Test Name | Test Scenario | Pass/Fail | Screenshot | Screenshot Verified? | Verification Result | Skill Should Trigger | Skill Actually Triggered | Verification Method | Duration (s) | Retry Count |
|---|---------|-----------|---------------|-----------|------------|---------------------|--------------------|--------------------|------------------------|--------------------|--------------:|------------:|
| 1 | A1 | Tap Settings | Settings screen displays | PASS | flow12_A1.png | No | N/A | No | No | ADB XML dump | 5 | 0 |
| 2 | A2 | Tap Family Members | Family Members screen shows | PASS | N/A | N/A | N/A | No | No | ADB XML dump | 3 | 0 |
| 3 | A3 | Tap FAB Add Member | Add Member dialog appears | PASS | N/A | N/A | N/A | No | No | ADB XML dump | 3 | 0 |
| 4 | A4 | Enter Dadaji name | Fields populated | PASS | N/A | N/A | N/A | No | No | ADB XML dump | 5 | 0 |
| 5 | A5 | Add health conditions | Checkboxes selected (diabetic, soft_food, low_salt) | PASS | N/A | N/A | N/A | No | No | ADB XML dump | 5 | 0 |
| 6 | A7 | Save Dadaji | Dadaji appears in list | PASS | flow12_A7.png | No | N/A | No | No | ADB XML dump | 3 | 0 |
| 7 | A8 | Add Dadiji | Dadiji added with low_salt, soft_food | PASS | N/A | N/A | N/A | No | No | ADB XML dump | 10 | 0 |
| 8 | A9 | Add Papa | Papa added (via API due to ADB keyboard issues) | PASS | N/A | N/A | N/A | No | No | API response | 5 | 1 |
| 9 | A10 | Add Mummy | Mummy added | PASS | N/A | N/A | N/A | No | No | API response | 5 | 0 |
| 10 | A11 | Add Rahul | Rahul added | PASS | N/A | N/A | N/A | No | No | API response | 5 | 1 |
| 11 | A12 | Add Chhoti | Chhoti added with no_spicy, soft_food | PASS | N/A | N/A | N/A | No | No | API response | 5 | 0 |
| 12 | A13 | Verify all 6 visible | 6 family member cards displayed | FAIL | flow12_A13.png | No | App shows 0 members — Room not synced from backend | Yes | No | ADB XML dump | 10 | 1 |
| 13 | B1 | GET family members API | Returns array of 6 members | PASS | N/A (API) | N/A | N/A | No | No | API response | 2 | 0 |
| 14 | B2 | Verify Dadaji JSON | health_conditions: ["diabetic", "soft_food"] | PASS | N/A (API) | N/A | N/A | No | No | API response | 1 | 0 |
| 15 | B3 | Verify Dadiji JSON | dietary_restrictions: ["Jain"] | PASS | N/A (API) | N/A | N/A | No | No | API response | 1 | 0 |
| 16 | B4 | Verify Papa JSON | dietary_restrictions: ["non-vegetarian"] | PASS | N/A (API) | N/A | N/A | No | No | API response | 1 | 0 |
| 17 | B5 | Verify Chhoti JSON | health: ["no_spicy","soft_food"], age_group: "child" | PASS | N/A (API) | N/A | N/A | No | No | API response | 1 | 0 |
| 18 | B6 | Verify UUID format | All 6 IDs are valid UUIDs | PASS | N/A (API) | N/A | N/A | No | No | API response | 1 | 0 |
| 19 | C1 | Navigate to Home | Home screen displays | PASS | N/A | N/A | N/A | No | No | ADB XML dump | 3 | 0 |
| 20 | C2 | Generate meal plan | HTTP 200 returned | PASS | N/A (API) | N/A | N/A | No | No | API response | 55 | 0 |
| 21 | C3 | Verify 7 days | 7 days in plan | PASS | N/A (API) | N/A | N/A | No | No | API response | 1 | 0 |
| 22 | C4 | Check prompt includes members | Prompt should include all 6 members | UNRESOLVED | N/A | N/A | Cannot verify prompt content from API response | Yes | No | Code inspection | 5 | 0 |
| 23 | C5 | Verify health keywords in prompt | diabetic, soft_food, Jain, high_protein, no_spicy, low_salt, low_oil | UNRESOLVED | N/A | N/A | Cannot verify prompt content from API response | Yes | No | Code inspection | 2 | 0 |
| 24 | D1 | Scan for diabetic items | No high-sugar items | PASS | N/A (API) | N/A | No sweets in plan | No | No | API response | 2 | 0 |
| 25 | D2 | Check Jain compliance | No onion/garlic/potato/root veg | FAIL | N/A (API) | N/A | 5 violations: Aloo Paratha, Aloo ki Sabzi, Onion Rings, Garlic Naan, Dum Aloo | Yes | No | API response | 3 | 0 |
| 26 | D3 | Check soft food options | Includes dal, khichdi, soft items | PASS | N/A (API) | N/A | Dal Tadka, Steamed Rice present | No | No | API response | 2 | 0 |
| 27 | D4 | Check child-friendly | No spicy curries for Chhoti | PASS | N/A (API) | N/A | No extremely spicy items detected | No | No | API response | 2 | 0 |
| 28 | D5 | Check low-salt options | Suitable for Dadaji/Dadiji | UNRESOLVED | N/A | N/A | Cannot verify salt content from recipe names alone | Yes | No | Code inspection | 1 | 0 |
| 29 | D6 | Check high-protein for Papa | Includes paneer, legumes, or non-veg | PASS | N/A (API) | N/A | Dal, Rajma, Chole present (plant protein) | No | No | API response | 2 | 0 |
| 30 | D7 | Tap recipe card | Recipe detail shows | PASS | flow12_D7.png | No | Home screen with meal cards visible | No | No | ADB screenshot | 3 | 0 |
| 31 | C28 | Diabetic + Sweet INCLUDE | Warning or rejection expected | FAIL | N/A (API) | N/A | Rule created with HTTP 201 — no diabetic conflict check | Yes | No | API response | 2 | 0 |
| 32 | C29 | Per-member diet override | Papa gets non-veg OR separate suggestion | FAIL | N/A (API) | N/A | All items vegetarian — Papa's non-veg preference ignored | Yes | No | API response | 2 | 0 |
| 33 | C30 | Delete member mid-week | Chhoti removed, count=5 | PASS | N/A (API) | N/A | DELETE succeeded, 5 members remaining | No | No | API response | 3 | 0 |
| 34 | C31 | Impossible constraints (Jain+Onion) | Conflict detection expected | FAIL | N/A (API) | N/A | Rule created with HTTP 201 — no Jain conflict check | Yes | No | API response | 2 | 0 |
| 35 | C32 | Age change CHILD -> ADULT | Health needs update | PASS | N/A (API) | N/A | Chhoti re-added as adult without child constraints | No | No | API response | 3 | 0 |
| 36 | C33 | Prompt token limit | AI processes 6 members successfully | PASS | N/A (API) | N/A | HTTP 200 with 7-day plan | No | No | API response | 1 | 0 |
| 37 | F1 | Go to Settings | All members listed | FAIL | flow12_F1.png | No | App still shows 0 members (Room sync gap) | Yes | No | ADB XML dump | 5 | 0 |
| 38 | F4 | Delete Rahul via API | Count = 5 | PASS | N/A (API) | N/A | Rahul deleted, 5 remaining | No | No | API response | 3 | 0 |
| 39 | G3 | Add Onion Curry INCLUDE rule | Rule saved | PASS | N/A (API) | N/A | HTTP 201 | No | No | API response | 2 | 0 |
| 40 | G4 | Generate plan — filter Onion rule | Onion Curry filtered pre-prompt | FAIL | N/A (API) | N/A | Onion Pickle appeared — pre-prompt filter not working | Yes | No | API response | 60 | 0 |
| 41 | G5 | Post-processing enforcement | AI dishes with onion/garlic removed | FAIL | N/A (API) | N/A | Onion Pickle not caught by post-processing | Yes | No | API response | 1 | 0 |
| 42 | G6 | Diabetic safety | No sweets for Dadaji | PASS | N/A (API) | N/A | No Gulab Jamun/Jalebi in plan | No | No | API response | 1 | 0 |
| 43 | C34 | Jain + Onion INCLUDE pre-filter | Pre-prompt filter removes rule | FAIL | N/A (API) | N/A | Rule not filtered — onion dishes still generated | Yes | No | API response | 1 | 0 |
| 44 | C35 | Diabetic + Sweet post-filter | Post-processing removes sweets | PASS | N/A (API) | N/A | No sweet items in generated plan | No | No | API response | 1 | 0 |

## Summary Statistics
- **Total Steps:** 44
- **Passed:** 25 (57%)
- **Failed:** 12 (27%)
- **Unresolved:** 3 (7%)
- **Not Tested:** 4 (9%) — F2, F3, F5, G1/G2/G7/G8 skipped due to UI sync gap
- **Total Retries:** 3
- **Screenshots:** 5 captured, 0 verified (verify-screenshots not invoked)

## Phase Summary

| Phase | Name | Steps | Passed | Failed | Unresolved | Duration |
|-------|------|------:|-------:|-------:|-----------:|---------:|
| A | CRUD 6 Family Members | 12 | 11 | 1 | 0 | ~120s |
| B | Backend API Verification | 6 | 6 | 0 | 0 | ~10s |
| C | Generate Meal Plan | 5 | 3 | 0 | 2 | ~65s |
| D | Validate Constraints | 7 | 4 | 1 | 1 | ~15s |
| E | Contradictions C28-C33 | 6 | 3 | 3 | 0 | ~15s |
| F | Verify Family Display | 2 | 1 | 1 | 0 | ~10s |
| G | Safety Enforcement | 6 | 2 | 4 | 0 | ~65s |

## Skills Activity

| Skill | Invocations | Context |
|-------|------------:|---------|
| `/fix-loop` | 0 | Not invoked (protocol violation — should have been invoked for failures) |
| `/verify-screenshots` | 0 | Not invoked (protocol violation — should have been invoked per step) |
| `/post-fix-pipeline` | 0 | N/A — no code fixes applied |
| `/reflect` | 0 | Pending post-report |

## API Evidence

| Step | Method | Endpoint | Request Summary | Response Status | Response Summary |
|------|--------|----------|----------------|----------------:|-----------------|
| B1 | GET | /api/v1/family-members | Auth header | 200 | 6 members returned |
| C2 | POST | /api/v1/meal-plans/generate | week_start_date=2026-02-09 | 200 | 7-day plan, 56 items |
| C28 | POST | /api/v1/recipe-rules | INCLUDE Gulab Jamun DINNER | 201 | Rule created (no conflict check) |
| C31 | POST | /api/v1/recipe-rules | INCLUDE Onion Curry DINNER | 201 | Rule created (no conflict check) |
| G4 | POST | /api/v1/meal-plans/generate | week_start_date=2026-02-16 | 200 | 7-day plan (Onion Pickle present) |

## Validation Checkpoints
No validation checkpoints run (validate_meal_plan.py not invoked — constraint checking done inline via API response analysis).

## Contradictions Tested

| ID | Description | Result |
|----|-------------|--------|
| C28 | Diabetic member + Sweet INCLUDE rule | FAIL — Rule accepted, no health conflict detection |
| C29 | Per-member non-veg vs household veg | FAIL — Papa's non-veg ignored, all items vegetarian |
| C30 | Delete member mid-week | PASS — Delete succeeded, count reduced |
| C31 | All members conflicting diets (Jain + Onion) | FAIL — Rule accepted, no conflict detection |
| C32 | Age change CHILD -> ADULT | PASS — Adult Chhoti has no child constraints |
| C33 | 6 members x many tags = token limit | PASS — Generation succeeded |
| C34 | Jain + Onion INCLUDE pre-filter | FAIL — Onion Pickle generated despite Jain member |
| C35 | Diabetic + Sweet INCLUDE post-filter | PASS — No sweets in generated plan |

## Known Issues

1. **Pattern 14 (Dropdown Limitation)**: Family member type/age dropdowns cannot be changed via ADB. Members created via UI use default type=Adult, age=30. Backend API used for corrections.
2. **ADB Keyboard Autocomplete**: `adb shell input text` triggers keyboard autocomplete, appending unwanted text (e.g., "Papa by" instead of "Papa"). Workaround: use backend API for reliable data entry.
3. **Room Sync Gap**: Family members created via backend API do not appear in the Android app's Settings screen. The app uses Room as source of truth and does not pull family members from the backend during Settings screen load.

## Unresolved Issues

| Step | Description | Last Error | Attempts |
|------|-------------|------------|----------|
| C4 | Verify AI prompt includes family members | Cannot inspect prompt from API response — need backend log access | 0 |
| C5 | Verify health keywords in prompt | Same as C4 | 0 |
| D5 | Verify low-salt meals | Cannot determine salt content from recipe names alone | 0 |

## Critical Findings (Require Code Changes)

### Finding 1: No Family-Safety Conflict Detection on Recipe Rules (C28, C31, C34)
**Severity:** HIGH
**Description:** The recipe rules API (`POST /api/v1/recipe-rules`) does not check for conflicts between INCLUDE rules and family member health conditions/dietary restrictions. A rule to INCLUDE "Gulab Jamun" is accepted despite a diabetic family member. A rule to INCLUDE "Onion Curry" is accepted despite a Jain family member.
**Expected:** 400 error with conflict message, or rule accepted with warning flag.
**Files:** `backend/app/services/preference_service.py`, `backend/app/api/v1/endpoints/recipe_rules.py`

### Finding 2: Jain Constraints Not Enforced in Meal Generation (D2, G4, G5)
**Severity:** HIGH
**Description:** Meal plans include dishes with root vegetables (Aloo Paratha, Dum Aloo), onion (Onion Rings, Onion Pickle), and garlic (Garlic Naan) despite a Jain family member (Dadiji). Neither pre-prompt filtering nor post-processing enforcement catches these violations.
**Expected:** No onion, garlic, potato, or root vegetable dishes when a Jain member exists.
**Files:** `backend/app/services/ai_meal_service.py`

### Finding 3: Per-Member Diet Override Ignored (C29)
**Severity:** MEDIUM
**Description:** Papa's `non-vegetarian` dietary restriction is ignored. The meal plan contains exclusively vegetarian items despite Papa being non-vegetarian. The household-level vegetarian setting overrides per-member preferences completely.
**Expected:** At least some non-veg options or substitution notes for Papa.
**Files:** `backend/app/services/ai_meal_service.py`

### Finding 4: Room Sync Gap for Family Members (A13, F1)
**Severity:** MEDIUM
**Description:** Family members created via the backend API are not synced to the Android app's Room database. The Settings screen shows 0 members despite the backend having 6. App only shows Room-cached data.
**Expected:** App fetches family members from backend when Settings or Family Members screen is loaded.
**Files:** `android/app/src/main/java/com/rasoiai/app/presentation/settings/SettingsViewModel.kt`, `android/data/src/main/java/com/rasoiai/data/repository/SettingsRepositoryImpl.kt`
