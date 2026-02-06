# Sharma Family Recipe Rules Test Plan

**GitHub Issue:** [#48](https://github.com/abhayla/KKB/issues/48)
**Requirement:** FR-011 / RULE-031
**Date:** 2026-02-06

## Overview

Comprehensive test suite for the Sharma family Recipe Rules profile, verifying all 5 rules across backend API, Swagger UI (Playwright), and Android E2E.

## Sharma Family Profile

| Attribute | Value |
|-----------|-------|
| Email | abhayinfosys@gmail.com |
| Diet | Vegetarian + Sattvic |
| Family Size | 3 |
| Cuisines | North, South Indian |

## 5 Sharma Family Rules

| # | Rule | Type | Action | Frequency | Enforcement | Meal Slot |
|---|------|------|--------|-----------|-------------|-----------|
| 1 | Chai → Breakfast | INGREDIENT | INCLUDE | DAILY | REQUIRED | BREAKFAST |
| 2 | Chai → Snacks | INGREDIENT | INCLUDE | DAILY | REQUIRED | SNACKS |
| 3 | Moringa | INGREDIENT | INCLUDE | 1x/week | PREFERRED | — |
| 4 | Paneer | INGREDIENT | EXCLUDE | NEVER | REQUIRED | — |
| 5 | Green Leafy goal | NUTRITION | — | 5/week | PREFERRED | — |

## Test Files

| File | Tests | Type |
|------|-------|------|
| `backend/tests/test_sharma_recipe_rules.py` | 10 | pytest (backend) |
| `android/.../e2e/base/TestDataFactory.kt` | 3 new rules | Test data |
| `android/.../e2e/flows/RecipeRulesFlowTest.kt` | 4 new methods | Android E2E |

## Backend Tests (10 total)

| Test | Description | Expected |
|------|-------------|----------|
| `test_sharma_create_chai_breakfast_rule` | POST Chai INCLUDE/DAILY/BREAKFAST/REQUIRED | 201 |
| `test_sharma_create_chai_snacks_rule` | POST Chai INCLUDE/DAILY/SNACKS/REQUIRED | 201 |
| `test_sharma_create_moringa_rule` | POST Moringa INCLUDE/TIMES_PER_WEEK(1)/PREFERRED | 201 |
| `test_sharma_create_paneer_exclude_rule` | POST Paneer EXCLUDE/NEVER/REQUIRED | 201 |
| `test_sharma_create_green_leafy_goal` | POST LEAFY_GREENS/5/PREFERRED | 201 |
| `test_sharma_all_rules_listed` | Create all 5, GET returns correct counts | 4 rules + 1 goal |
| `test_sharma_delete_and_recreate_rule` | DELETE then POST, verify new ID | 204 then 201, different IDs |
| `test_sharma_rule_fields_match` | Create rule, verify every field via POST and GET | All fields match |
| `test_sharma_nutrition_goal_fields_match` | Create goal, verify every field | All fields match |
| `test_sharma_duplicate_nutrition_goal_rejected` | Create same LEAFY_GREENS twice | 409 conflict |

## Android E2E Tests (4 new methods)

| Test | Description |
|------|-------------|
| `sharma_chaiBreakfastRule` | Add Chai INCLUDE/DAILY/REQUIRED, verify card |
| `sharma_chaiSnacksRule` | Add Chai INCLUDE/DAILY/REQUIRED, verify card |
| `sharma_moringaIncludeRule` | Add Moringa INCLUDE/1x-week/PREFERRED, verify card |
| `sharma_allFiveRules` | Create all 5 rules, verify all cards on correct tabs |

## Playwright Verification

Verified all 5 rules via Swagger UI (`http://localhost:8000/docs`):
1. Authenticated via POST `/api/v1/auth/firebase`
2. Created all 4 recipe rules via POST `/api/v1/recipe-rules` — all returned 201
3. Created nutrition goal via POST `/api/v1/nutrition-goals` — 409 (already exists, confirming duplicate rejection)
4. GET `/api/v1/recipe-rules` — returned all rules with correct fields
5. GET `/api/v1/nutrition-goals` — returned 1 goal (LEAFY_GREENS)

## Screenshots

| Screenshot | Description |
|------------|-------------|
| `sharma_rules_swagger_before.png` | Swagger UI before rule creation |
| `sharma_rules_swagger_recipe_rules.png` | Recipe-rules API section on Swagger |
| `sharma_rules_swagger_after.png` | API verification showing all 5 Sharma rules |

## Test Results

### Backend: 10/10 passed
```
tests/test_sharma_recipe_rules.py::test_sharma_create_chai_breakfast_rule PASSED
tests/test_sharma_recipe_rules.py::test_sharma_create_chai_snacks_rule PASSED
tests/test_sharma_recipe_rules.py::test_sharma_create_moringa_rule PASSED
tests/test_sharma_recipe_rules.py::test_sharma_create_paneer_exclude_rule PASSED
tests/test_sharma_recipe_rules.py::test_sharma_create_green_leafy_goal PASSED
tests/test_sharma_recipe_rules.py::test_sharma_all_rules_listed PASSED
tests/test_sharma_recipe_rules.py::test_sharma_delete_and_recreate_rule PASSED
tests/test_sharma_recipe_rules.py::test_sharma_rule_fields_match PASSED
tests/test_sharma_recipe_rules.py::test_sharma_nutrition_goal_fields_match PASSED
tests/test_sharma_recipe_rules.py::test_sharma_duplicate_nutrition_goal_rejected PASSED
```

### Existing tests: No regression (18/20 pass, 2 pre-existing auth failures)

### Playwright: All 5 rules verified via live API

### Android E2E: Requires emulator (not available in current session)

## Run Commands

```bash
# Backend tests
cd backend && PYTHONPATH=. pytest tests/test_sharma_recipe_rules.py -v

# Regression check
cd backend && PYTHONPATH=. pytest tests/test_recipe_rules_api.py -v

# Android E2E (requires emulator)
cd android && ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.RecipeRulesFlowTest
```
