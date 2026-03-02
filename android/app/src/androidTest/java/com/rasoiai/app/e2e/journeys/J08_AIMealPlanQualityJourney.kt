package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

/**
 * J08: AI Meal Plan Quality Assurance (single Activity session)
 *
 * Scenario: User reviews the generated meal plan across multiple days for quality.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J08_AIMealPlanQualityJourney
 * ```
 */
@HiltAndroidTest
class J08_AIMealPlanQualityJourney : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private val logger = JourneyStepLogger("J08")

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()
        homeRobot = HomeRobot(composeTestRule)
    }

    @Test
    fun aiMealPlanQualityJourney() {
        val totalSteps = 5

        try {
            logger.step(1, totalSteps, "Home with meal data") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
                homeRobot.assertAllMealCardsDisplayed()
            }

            logger.step(2, totalSteps, "Review Monday meals") {
                homeRobot.selectDay(DayOfWeek.MONDAY)
                homeRobot.assertMealCardDisplayed(MealType.BREAKFAST)
                homeRobot.assertMealCardDisplayed(MealType.DINNER)
            }

            logger.step(3, totalSteps, "Review Wednesday meals") {
                homeRobot.selectDay(DayOfWeek.WEDNESDAY)
                homeRobot.assertMealCardDisplayed(MealType.LUNCH)
                homeRobot.assertMealCardDisplayed(MealType.SNACKS)
            }

            logger.step(4, totalSteps, "Review Saturday meals") {
                homeRobot.selectDay(DayOfWeek.SATURDAY)
                homeRobot.assertMealCardDisplayed(MealType.BREAKFAST)
                homeRobot.assertMealCardDisplayed(MealType.DINNER)
            }

            logger.step(5, totalSteps, "Week selector is functional") {
                homeRobot.assertWeekSelectorDisplayed()
                homeRobot.selectDay(DayOfWeek.SUNDAY)
            }
        } finally {
            logger.printSummary()
        }
    }
}
