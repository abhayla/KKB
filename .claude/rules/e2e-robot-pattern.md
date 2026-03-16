---
description: >
  E2E tests MUST use the Robot pattern for screen interactions. Each screen has a Robot class
  that encapsulates all UI interactions. Tests compose Robots — never call composeTestRule directly.
globs: ["android/app/src/androidTest/**/*.kt"]
synthesized: true
private: false
---

# E2E Robot Pattern

E2E tests use the Robot pattern to separate test intent from UI mechanics. Each screen has a Robot class in `e2e/robots/`. Tests compose Robots to build user journeys.

## Robot structure

```kotlin
class HomeRobot(private val composeTestRule: ComposeContentTestRule) {

    fun waitForHomeScreen() {
        composeTestRule.waitUntilWithBackoff { isNodeWithTagDisplayed(TestTags.HOME_SCREEN) }
    }

    fun assertMealPlanDisplayed() {
        composeTestRule.onNodeWithTag(TestTags.MEAL_PLAN_CARD).assertIsDisplayed()
    }

    fun tapMealCard(mealType: MealType) {
        composeTestRule.onNodeWithTag("${TestTags.MEAL_CARD_PREFIX}${mealType.name}")
            .performScrollTo()
            .performClick()
    }

    fun swipeToNextDay() {
        composeTestRule.onNodeWithTag(TestTags.WEEK_VIEW).performTouchInput { swipeLeft() }
    }
}
```

## Available robots (10)

| Robot | Screen | Key interactions |
|-------|--------|-----------------|
| `AuthRobot` | Auth/Login | Phone number entry, OTP verification |
| `OnboardingRobot` | Onboarding | Step navigation, preference selection |
| `HomeRobot` | Home | Week view, meal cards, refresh, swap |
| `GroceryRobot` | Grocery | List management, item checking |
| `ChatRobot` | Chat | Message input, AI response wait |
| `FavoritesRobot` | Favorites | Collection management |
| `CookingModeRobot` | Cooking | Step navigation, timer |
| `HouseholdRobot` | Household settings | Member management |
| `HouseholdMembersRobot` | Household members | Add/edit/remove members |
| `HouseholdNotificationsRobot` | Household notifications | Notification actions |

## Test composition with Robots

```kotlin
@HiltAndroidTest
class J01_FirstTimeUserJourney : BaseE2ETest() {
    private lateinit var authRobot: AuthRobot
    private lateinit var onboardingRobot: OnboardingRobot
    private val logger = JourneyStepLogger("J01")

    @Before
    override fun setUp() {
        super.setUp()
        setUpNewUserState()
        authRobot = AuthRobot(composeTestRule)
        onboardingRobot = OnboardingRobot(composeTestRule)
    }

    @Test
    fun firstTimeUserJourney() {
        logger.step(1, 5, "Auth screen displayed") {
            authRobot.waitForAuthScreen()
            authRobot.assertAuthScreenDisplayed()
        }
        logger.step(2, 5, "Phone auth sign-up") {
            authRobot.enterPhoneNumber()
            // ...
        }
    }
}
```

## TestTags convention

All UI elements use `Modifier.testTag()` with constants from `presentation/common/TestTags.kt`. Robots reference these tags — never hardcoded strings:

```kotlin
// In composable
Modifier.testTag(TestTags.MEAL_PLAN_CARD)

// In robot
composeTestRule.onNodeWithTag(TestTags.MEAL_PLAN_CARD)
```

## Wait utilities

Robots use backoff-based waiting from `e2e/base/ComposeTestExtensions.kt`:

| Utility | Purpose |
|---------|---------|
| `waitUntilWithBackoff { }` | Wait with exponential backoff for a condition |
| `waitForNetworkContent()` | Wait for network-loaded content to appear |
| `waitForSheetText(text)` | Wait for bottom sheet text |
| `clickWithRetry(tag)` | Click with automatic retry on stale nodes |
| `isNodeWithTagDisplayed(tag)` | Check visibility without assertion failure |

## Journey test suites (J01-J17)

17 journey suites group E2E tests by user scenario. Each suite is in `e2e/journeys/` with a `*Journey.kt` (test class) and `*Suite.kt` (JUnit suite runner).

## Adding a new Robot

1. Create `e2e/robots/NewFeatureRobot.kt` with `ComposeContentTestRule` constructor
2. Add wait, assert, and interaction methods using `TestTags` constants
3. Add any new test tags to `TestTags.kt`
4. Use the robot in journey tests via composition

## MUST NOT

- MUST NOT call `composeTestRule` directly in test methods — always go through a Robot
- MUST NOT hardcode UI element identifiers in tests or robots — use `TestTags` constants
- MUST NOT use `Thread.sleep()` for waits — use `waitUntilWithBackoff` or framework waiters
- MUST NOT create journey tests without using `JourneyStepLogger` — structured logging is required for debugging flaky E2E tests
