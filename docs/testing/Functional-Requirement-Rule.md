# Functional Requirements

This document serves as the single source of truth for tracking functional requirements and their test coverage.

## Requirements Traceability Matrix

| ID | Requirement | GitHub Issue | Android E2E Test | Backend Test | Status |
|----|-------------|--------------|------------------|--------------|--------|
| FR-001 | Recipe search returns results from database | TBD | [`RecipeInteractionFlowTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/RecipeInteractionFlowTest.kt) | [`test_recipe_search.py`](../../backend/tests/test_recipe_search.py) | ✅ |
| FR-002 | Auto-add recipe to favorites from Suggestions tab | [#40](https://github.com/abhayla/KKB/issues/40) | [`RecipeInteractionFlowTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/RecipeInteractionFlowTest.kt) | N/A (Android-only) | ✅ |
| FR-003 | First-time vs returning user navigation (ONB-036) | [#41](https://github.com/abhayla/KKB/issues/41) | [`OnboardingNavigationTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/OnboardingNavigationTest.kt) | N/A (Android-only) | ✅ |
| FR-004 | MealPlanGenerationFlowTest fixes + diet conflict warnings | [#42](https://github.com/abhayla/KKB/issues/42) | [`MealPlanGenerationFlowTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/MealPlanGenerationFlowTest.kt) | N/A (Android-only) | ✅ |
| FR-005 | Family Profile Data Persistence (Onboarding & Settings) | [#44](https://github.com/abhayla/KKB/issues/44) | [`FamilyProfileFlowTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/FamilyProfileFlowTest.kt) | N/A (Android-only) | ✅ |
| FR-006 | Recipe Rules Backend Sync (Offline-First) | [#45](https://github.com/abhayla/KKB/issues/45) | [`RecipeRulesFlowTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/RecipeRulesFlowTest.kt) | [`test_recipe_rules_api.py`](../../backend/tests/test_recipe_rules_api.py), [`test_migrate_legacy_rules.py`](../../backend/tests/test_migrate_legacy_rules.py) | ✅ |
| FR-007 | Expanded E2E Tests for Grocery, Favorites, Settings | [#39](https://github.com/abhayla/KKB/issues/39) | `GroceryFlowTest.kt` (18), `FavoritesFlowTest.kt` (16), `SettingsFlowTest.kt` (22) | N/A (Android-only) | ✅ |
| FR-008 | E2E Test Infrastructure - Meal generation API connectivity | [#43](https://github.com/abhayla/KKB/issues/43) | [`MealPlanGenerationFlowTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/MealPlanGenerationFlowTest.kt) | N/A (Android E2E infrastructure) | ✅ |
| FR-009 | NotificationsScreen UI Tests | [#36](https://github.com/abhayla/KKB/issues/36) | [`NotificationsScreenTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/presentation/notifications/NotificationsScreenTest.kt) | N/A (Android UI tests) | 🚧 |
| FR-010 | AI Recipe Catalog — shared recipe search for Recipe Rules | [#47](https://github.com/abhayla/KKB/issues/47) | `RecipeRulesFlowTest.kt` | [`test_ai_recipe_catalog.py`](../../backend/tests/test_ai_recipe_catalog.py) | ✅ |
| FR-011 | Sharma family Recipe Rules E2E test suite | [#48](https://github.com/abhayla/KKB/issues/48) | [`RecipeRulesFlowTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/RecipeRulesFlowTest.kt) | [`test_sharma_recipe_rules.py`](../../backend/tests/test_sharma_recipe_rules.py) | ✅ |
| FR-012 | Recipe Rules duplicate prevention & case normalization | [#49](https://github.com/abhayla/KKB/issues/49) | N/A (DAO + ViewModel) | [`test_recipe_rules_dedup.py`](../../backend/tests/test_recipe_rules_dedup.py) | ✅ |
| FR-013 | Sync missing preferences + Family Members CRUD endpoint | [#50](https://github.com/abhayla/KKB/issues/50) | N/A (backend-only) | [`test_family_members_api.py`](../../backend/tests/test_family_members_api.py) | ✅ |
| FR-014 | Recipe Rules force override for family safety conflicts | TBD | `RecipeRulesViewModelTest.kt` | [`test_recipe_rule_family_conflict.py`](../../backend/tests/services/test_recipe_rule_family_conflict.py) | ✅ |
| FR-015 | Household CRUD (create, view, update, deactivate) | TBD | [`HouseholdSetupFlowTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/HouseholdSetupFlowTest.kt) | [`test_household_crud.py`](../../backend/tests/api/test_household_crud.py), [`test_household_service.py`](../../backend/tests/services/test_household_service.py) | ✅ |
| FR-016 | Household member management (add, remove, permissions) | TBD | [`HouseholdMemberFlowTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/HouseholdMemberFlowTest.kt) | [`test_household_membership.py`](../../backend/tests/api/test_household_membership.py), [`test_household_validation.py`](../../backend/tests/api/test_household_validation.py) | ✅ |
| FR-017 | Household invite code (generate, share, join) | TBD | [`HouseholdMemberFlowTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/HouseholdMemberFlowTest.kt) | [`test_household_membership.py`](../../backend/tests/api/test_household_membership.py) | ✅ |
| FR-018 | Family/Personal scope toggle on 5 screens | TBD | [`HouseholdMealPlanFlowTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/HouseholdMealPlanFlowTest.kt) | [`test_household_scoped.py`](../../backend/tests/api/test_household_scoped.py) | ✅ |
| FR-019 | Household meal plan view with status tracking | TBD | [`HouseholdMealPlanFlowTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/HouseholdMealPlanFlowTest.kt) | [`test_household_layer4.py`](../../backend/tests/api/test_household_layer4.py) | ✅ |
| FR-020 | Household notifications | TBD | [`HouseholdNotificationFlowTest.kt`](../../android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/HouseholdNotificationFlowTest.kt) | [`test_household_layer4.py`](../../backend/tests/api/test_household_layer4.py) | ✅ |

**Status Legend:**
- ✅ Implemented and tested
- 🚧 In Progress
- ❌ Not Started
- ⚠️ Tests Failing

---

## How to Add a New Requirement

### 1. Create GitHub Issue
Use the "Functional Requirement" template when creating a new issue:
- Go to GitHub Issues → New Issue → "Functional Requirement"
- Fill in all required fields (Feature Area, Description, Acceptance Criteria, Test Scenarios)
- Assign the next available FR-XXX number in the title

### 2. Implement the Feature
- Reference the GitHub Issue in your commits: `Implements #45`
- Follow existing patterns in the codebase

### 3. Create Tests
Follow patterns documented in [E2E-Testing-Prompt.md](./E2E-Testing-Prompt.md):

**Android E2E Test:**
```kotlin
/**
 * Requirement: #45 - FR-001: Recipe search returns results from database
 *
 * Tests the recipe search functionality in the Add Recipe sheet.
 */
@HiltAndroidTest
class YourFeatureTest : BaseE2ETest() {
    // Test implementation
}
```

**Backend Test:**
```python
"""
Requirement: #45 - FR-001: Recipe search returns results from database

Tests the recipe search API endpoint.
"""
@pytest.mark.asyncio
async def test_your_feature():
    # Test implementation
```

### 4. Update This Document
Add a new row to the Requirements Traceability Matrix above with:
- FR-XXX ID
- Brief requirement description
- GitHub Issue link
- Test file names
- Status

### 5. Verify Tests Pass

**Android E2E:**
```bash
cd android
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.YourFeatureTest
```

**Backend:**
```bash
cd backend
source venv/Scripts/activate  # or venv/bin/activate on Linux/Mac
PYTHONPATH=. pytest tests/test_your_feature.py -v
```

---

## Traceability Flow

```
GitHub Issue (#XX)           →  Created BEFORE implementation
    ↓
Implementation               →  Commits reference Issue #XX
    ↓
Test Files                   →  KDoc/Docstring references Issue #XX
    ↓
This Document                →  Links everything together
    ↓
CLAUDE.md Rules              →  Ensures process is followed
```

---

## Test File Locations

| Test Type | Location | Naming Convention |
|-----------|----------|-------------------|
| Android E2E | `android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/` | `*Test.kt` |
| Android UI | `android/app/src/androidTest/java/com/rasoiai/app/presentation/` | `*ScreenTest.kt` |
| Android Unit | `android/app/src/test/java/com/rasoiai/app/` | `*Test.kt` |
| Backend | `backend/tests/` | `test_*.py` |

---

## Quick Reference: Running Tests

```bash
# All Android E2E tests
cd android && ./gradlew :app:connectedDebugAndroidTest

# Specific Android E2E test
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.TestClassName

# All backend tests
cd backend && PYTHONPATH=. pytest

# Specific backend test file
PYTHONPATH=. pytest tests/test_file.py -v

# Specific backend test function
PYTHONPATH=. pytest tests/test_file.py::test_function_name -v
```
