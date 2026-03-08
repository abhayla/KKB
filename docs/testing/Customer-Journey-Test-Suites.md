# Customer Journey Test Suites

17 journey-based test suites that group the 28 E2E test files into realistic user scenarios. Each journey has a runnable JUnit `@Suite` class.

**Location:** `android/app/src/androidTest/java/com/rasoiai/app/e2e/journeys/`

---

## Quick Reference

```bash
cd android

# Run a specific journey
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J01_FirstTimeUserSuite

# Run all journeys
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.e2e.journeys
```

| Suite | Journey | Test Classes |
|-------|---------|:------------:|
| `J01` | First-Time User Gets Started | 3 |
| `J02` | New User First Meal Plan | 2 |
| `J03` | Complete End-to-End Journey | 1 |
| `J04` | Daily Meal Planning | 2 |
| `J05` | Weekly Grocery Shopping | 2 |
| `J06` | Cooking a Meal | 3 |
| `J07` | Managing Dietary Preferences | 3 |
| `J08` | AI Meal Plan Quality Assurance | 2 |
| `J09` | Family Profile Management | 2 |
| `J10` | Exploring App Features | 4 |
| `J11` | Customizing App Settings | 3 |
| `J12` | Offline and Error Resilience | 2 |
| `J13` | Returning User Quick Check | 3 |
| `J14` | AI Chat and Recipe Discovery | 3 |
| `J15` | Household Setup & Member Management | 2 |
| `J16` | Household Meal Collaboration | 2 |
| `J17` | Household Notifications & Awareness | 2 |

---

## Journey Descriptions

### J1: First-Time User Gets Started

Brand new user downloads app, signs up, completes onboarding.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `AuthFlowTest` | Phone auth sign-up, OTP verification, JWT receipt |
| 2 | `OnboardingFlowTest` | 5-step onboarding UI (household, dietary, cuisine, ingredients, cooking time) |
| 3 | `OnboardingNavigationTest` | First-time vs returning user routing logic |

### J2: New User First Meal Plan

Fresh user goes through auth + onboarding + generates first meal plan.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `CoreDataFlowTest` | Full auth->onboarding->generation->home (clears state) |
| 2 | `MealPlanGenerationFlowTest` | Generation progress steps, API connectivity, meal plan creation |

### J3: Complete End-to-End Journey

Full app walkthrough -- auth through every major screen.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `FullJourneyFlowTest` | 13-phase deep journey: Auth -> Onboarding -> MealGen1 -> Home1 -> RecipeRules -> MealGen2 -> Home2 -> RecipeDetail -> Grocery -> Favorites -> Chat -> Stats -> Settings |

### J4: Daily Meal Planning

Returning user checks today's meals, swaps/locks items, adds a recipe.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `HomeScreenComprehensiveTest` | 50 tests: navigation, week selector, lock/swap, action sheets, add recipe, refresh, content validation |
| 2 | `RecipeInteractionFlowTest` | Recipe search, auto-favorite on add, meal type filtering |

### J5: Weekly Grocery Shopping

User reviews meal plan then generates and uses grocery list.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `HomeScreenComprehensiveTest` | Review meal cards, day navigation |
| 2 | `GroceryFlowTest` | Grocery list generation, category grouping, item checking |

### J6: Cooking a Meal

User picks a recipe from home, views details, enters cooking mode.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `HomeScreenComprehensiveTest` | Tap recipe -> recipe detail navigation |
| 2 | `CookingModeFlowTest` | Recipe scaling, step-by-step cooking, start cooking button |
| 3 | `RecipeInteractionFlowTest` | Recipe search, adding recipes from catalog |

### J7: Managing Dietary Preferences

User configures include/exclude rules and nutrition goals.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `RecipeRulesFlowTest` | Add/delete/toggle/edit include & exclude rules |
| 2 | `NutritionGoalsFlowTest` | Nutrition goal CRUD (green leafy, protein targets) |
| 3 | `SharmaRecipeRulesVerificationTest` | Deep persistence verification: Room DB + Backend sync |

### J8: AI Meal Plan Quality Assurance

Verify AI respects dietary constraints, allergies, and rules in generated plans.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `MealPlanAIVerificationTest` | Vegetarian enforcement, allergen exclusion, INCLUDE rule compliance |
| 2 | `MealPlanGenerationFlowTest` | Generation API connectivity, progress steps |

### J9: Family Profile Management

User manages family members, verifies data persists across onboarding and settings.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `SharmaOnboardingVerificationTest` | DataStore + Backend persistence of 3 Sharma family members |
| 2 | `FamilyProfileFlowTest` | Family member CRUD, preferences sync |

### J10: Exploring App Features

User browses favorites, chats with AI, checks cooking stats, manages pantry.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `FavoritesFlowTest` | Add/remove favorites, persistence in Room DB |
| 2 | `ChatFlowTest` | AI chat, context awareness, recipe suggestions |
| 3 | `StatsScreenTest` | Cooking streak, calendar, achievements |
| 4 | `PantryFlowTest` | Pantry items, expiry tracking, smart suggestions |

### J11: Customizing App Settings

User adjusts preferences, recipe rules, and nutrition goals via Settings.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `SettingsFlowTest` | Settings sub-screens, preference display, navigation |
| 2 | `RecipeRulesFlowTest` | Include/exclude rule management |
| 3 | `NutritionGoalsFlowTest` | Nutrition goal management |

### J12: Offline and Error Resilience

User encounters network issues, app handles errors gracefully.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `OfflineFlowTest` | Cached data access, local mutations, sync on reconnect |
| 2 | `EdgeCasesTest` | Error handling, validation, session management |

### J13: Returning User Quick Check

Existing user opens app, checks meals, reviews grocery list.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `OnboardingNavigationTest` | Returning user bypasses onboarding -> Home |
| 2 | `HomeScreenComprehensiveTest` | Meal card display, day navigation |
| 3 | `GroceryFlowTest` | Grocery list review |

### J14: AI Chat and Recipe Discovery

User explores recipes via AI chat and search, saves favorites.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `ChatFlowTest` | AI chat, recipe suggestions |
| 2 | `RecipeInteractionFlowTest` | Recipe search, add from catalog |
| 3 | `FavoritesFlowTest` | Save and manage favorites |

### J15: Household Setup & Member Management

User creates a household, manages members, generates and shares invite codes.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `HouseholdSetupFlowTest` | Create/view/update/deactivate household, validation |
| 2 | `HouseholdMemberFlowTest` | Add by phone, invite codes, join, leave, transfer ownership, roles |

### J16: Household Meal Collaboration

Household members manage shared recipe rules and collaborate on meal plans.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `HouseholdRecipeRulesFlowTest` | Household INCLUDE/EXCLUDE rules, merged constraints |
| 2 | `HouseholdMealPlanFlowTest` | Shared meal plan, item status (cooked/skipped/ordered out), monthly stats |

### J17: Household Notifications & Awareness

User checks household notifications and monitors shared meal plan activity.

| Order | Test File | What It Covers |
|:-----:|-----------|----------------|
| 1 | `HouseholdNotificationFlowTest` | Notification list, mark read, badge count, access control |
| 2 | `HouseholdMealPlanFlowTest` | Shared meal plan view, activity monitoring |

---

## Coverage Matrix

Every test file appears in at least one journey:

| Test File | Journeys |
|-----------|----------|
| `AuthFlowTest` | J1 |
| `OnboardingFlowTest` | J1 |
| `OnboardingNavigationTest` | J1, J13 |
| `CoreDataFlowTest` | J2 |
| `MealPlanGenerationFlowTest` | J2, J8 |
| `FullJourneyFlowTest` | J3 |
| `HomeScreenComprehensiveTest` | J4, J5, J6, J13 |
| `RecipeInteractionFlowTest` | J4, J6, J14 |
| `GroceryFlowTest` | J5, J13 |
| `CookingModeFlowTest` | J6 |
| `RecipeRulesFlowTest` | J7, J11 |
| `NutritionGoalsFlowTest` | J7, J11 |
| `SharmaRecipeRulesVerificationTest` | J7 |
| `MealPlanAIVerificationTest` | J8 |
| `SharmaOnboardingVerificationTest` | J9 |
| `FamilyProfileFlowTest` | J9 |
| `FavoritesFlowTest` | J10, J14 |
| `ChatFlowTest` | J10, J14 |
| `StatsScreenTest` | J10 |
| `PantryFlowTest` | J10 |
| `SettingsFlowTest` | J11 |
| `OfflineFlowTest` | J12 |
| `EdgeCasesTest` | J12 |
| `HouseholdSetupFlowTest` | J15 |
| `HouseholdMemberFlowTest` | J15 |
| `HouseholdMealPlanFlowTest` | J16, J17 |
| `HouseholdRecipeRulesFlowTest` | J16 |
| `HouseholdNotificationFlowTest` | J17 |

**28/28 test files covered (100%)**

---

## setUp() Compatibility

Each test class is self-contained -- it re-establishes its required state in `@Before`:

| setUp Pattern | Tests | Behavior |
|---------------|-------|----------|
| `clearAllState()` | CoreDataFlowTest, SharmaOnboardingVerificationTest, FullJourneyFlowTest | Resets everything -- must be first if present |
| `setUpNewUserState()` | AuthFlowTest, OnboardingFlowTest, MealPlanGenerationFlowTest | Clears data, sets up for fresh user |
| `setUpAuthenticatedState()` | HomeScreen, Grocery, Chat, Favorites, Stats, Settings, Pantry, CookingMode, RecipeInteraction, Offline | Skips to Home with meal plan |
| `setUpAuthenticatedStateWithoutMealPlan()` | RecipeRules, NutritionGoals, SharmaRecipeRulesVerification, MealPlanAIVerification | Skips to Home without triggering Gemini |

The `backendMealPlanGenerated` companion object flag ensures the first test in a JVM run generates a meal plan; subsequent tests reuse it.

---

## Run Commands

```bash
cd android

# Individual journeys
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J01_FirstTimeUserSuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J02_NewUserFirstMealPlanSuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J03_CompleteE2EJourneySuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J04_DailyMealPlanningSuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J05_WeeklyGroceryShoppingSuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J06_CookingAMealSuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J07_ManagingDietaryPrefsSuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J08_AIMealPlanQualitySuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J09_FamilyProfileMgmtSuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J10_ExploringAppFeaturesSuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J11_CustomizingSettingsSuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J12_OfflineErrorResilienceSuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J13_ReturningUserQuickCheckSuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J14_AIChatRecipeDiscoverySuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J15_HouseholdSetupSuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J16_HouseholdMealCollaborationSuite

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J17_HouseholdNotificationsSuite

# All journeys at once
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.e2e.journeys
```

---

*Last Updated: March 8, 2026*
