package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.SettingsFlowTest
import com.rasoiai.app.e2e.flows.RecipeRulesFlowTest
import com.rasoiai.app.e2e.flows.NutritionGoalsFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J11: Customizing App Settings
 *
 * Scenario: User adjusts preferences, recipe rules, and nutrition goals via Settings.
 *
 * Flow: Settings sub-screens and preference display -> include/exclude rule management ->
 *       nutrition goal management
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J11_CustomizingSettingsSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    SettingsFlowTest::class,
    RecipeRulesFlowTest::class,
    NutritionGoalsFlowTest::class
)
class J11_CustomizingSettingsSuite
