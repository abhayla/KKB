---
description: >
  Enforce centralized TestTags object for all Compose UI test identifiers.
  Prevents scattered inline strings and ensures reliable E2E test selectors.
globs: ["android/app/src/main/java/**/presentation/**/*.kt", "android/app/src/androidTest/**/*.kt"]
synthesized: true
private: false
---

# Compose TestTags Convention

## The Rule

All Compose `Modifier.testTag()` values MUST be defined as constants in `presentation/common/TestTags.kt`. NEVER use inline string literals for test tags.

## TestTags Object Structure

```kotlin
// presentation/common/TestTags.kt
object TestTags {
    // Group by feature/screen
    // Use SCREAMING_SNAKE_CASE

    // Onboarding
    const val ONBOARDING_NEXT_BUTTON = "onboarding_next_button"
    const val ONBOARDING_BACK_BUTTON = "onboarding_back_button"

    // Home
    const val HOME_MEAL_CARD_PREFIX = "home_meal_card_"

    // Use _PREFIX suffix for tags that will be appended with dynamic IDs
    const val RECIPE_CARD_PREFIX = "recipe_card_"
}
```

## Usage in Composables

```kotlin
// CORRECT: reference TestTags constant
Text(
    text = recipe.name,
    modifier = Modifier.testTag(TestTags.RECIPE_CARD_PREFIX + recipe.id)
)

// WRONG: inline string
Text(
    text = recipe.name,
    modifier = Modifier.testTag("recipe_card_${recipe.id}")
)
```

## Usage in Tests

```kotlin
// CORRECT: same constant in test
composeTestRule
    .onNodeWithTag(TestTags.RECIPE_CARD_PREFIX + testRecipeId)
    .assertIsDisplayed()

// WRONG: duplicated string
composeTestRule
    .onNodeWithTag("recipe_card_$testRecipeId")  // breaks if tag changes
    .assertIsDisplayed()
```

## Naming Conventions

| Pattern | Format | Example |
|---------|--------|---------|
| Static element | `SCREEN_ELEMENT` | `HOME_GENERATE_BUTTON` |
| Dynamic element | `SCREEN_ELEMENT_PREFIX` + id | `RECIPE_CARD_PREFIX + recipeId` |
| Grouped elements | `SCREEN_GROUP_ELEMENT` | `ONBOARDING_STEP_INDICATOR` |

## MUST DO

- Add new test tags to `TestTags.kt` BEFORE using them in Composables or tests
- Group tags by screen/feature with a comment header
- Use `_PREFIX` suffix for tags that get dynamic IDs appended

## MUST NOT

- NEVER use inline string test tags in `Modifier.testTag()` — always reference `TestTags`
- NEVER duplicate tag strings between production code and test code — both must use the same constant
- NEVER use `contentDescription` as a test selector when `testTag` is available — `contentDescription` is for accessibility, `testTag` is for testing
