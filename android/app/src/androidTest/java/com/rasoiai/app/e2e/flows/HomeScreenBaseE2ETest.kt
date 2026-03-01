package com.rasoiai.app.e2e.flows

import android.util.Log
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.domain.model.MealType
import org.junit.Before

/**
 * Base class for HomeScreen E2E tests that need authenticated state with meal data.
 *
 * Provides:
 * - [homeRobot] initialized and ready
 * - Authenticated state with meal plan
 * - Meal data loaded (waits for breakfast card)
 *
 * Subclasses can override [initializeAdditionalRobots] to set up extra robots,
 * or override [setUp] entirely for custom setup logic.
 */
abstract class HomeScreenBaseE2ETest : BaseE2ETest() {

    protected lateinit var homeRobot: HomeRobot

    /**
     * Override to initialize additional robots after homeRobot is ready.
     * Called during setUp() after homeRobot initialization.
     */
    protected open fun initializeAdditionalRobots() {
        // Default: no additional robots
    }

    @Before
    override fun setUp() {
        super.setUp()

        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)
        initializeAdditionalRobots()

        homeRobot.waitForHomeScreen(30000)
        waitForMealDataToLoad()
    }

    /**
     * Wait for meal data to load (up to 60s for API-generated meal plans).
     * Waits for a breakfast meal card to appear, indicating data is ready.
     */
    protected fun waitForMealDataToLoad(timeoutMillis: Long = 60000) {
        try {
            homeRobot.assertMealCardDisplayed(MealType.BREAKFAST, timeoutMillis = timeoutMillis)
            Log.i(TAG, "Meal data loaded successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Meal data failed to load: ${e.message}")
            throw AssertionError("Meal data did not load within ${timeoutMillis}ms", e)
        }
    }

    companion object {
        private const val TAG = "HomeScreenBaseE2ETest"
    }
}
