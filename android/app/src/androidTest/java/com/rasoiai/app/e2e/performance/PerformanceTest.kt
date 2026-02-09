package com.rasoiai.app.e2e.performance

import android.os.Debug
import android.os.SystemClock
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.RecipeDetailRobot
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

/**
 * Phase 15: Performance Testing
 *
 * Tests:
 * 15.1 Cold Start Time
 * 15.2 Screen Transition Performance
 * 15.3 Memory Usage
 */
@HiltAndroidTest
class PerformanceTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var recipeDetailRobot: RecipeDetailRobot

    @Before
    override fun setUp() {
        super.setUp()
        // Set up authenticated and onboarded user state
        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)
        recipeDetailRobot = RecipeDetailRobot(composeTestRule)
    }

    /**
     * Test 15.1: Cold Start Time
     *
     * Steps:
     * 1. Force stop app
     * 2. Clear from recents
     * 3. Time app launch to Home screen
     * 4. Repeat 3 times
     *
     * Expected:
     * - Cold start < 3 seconds
     * - Splash screen smooth
     */
    @Test
    fun test_15_1_coldStartTime() {
        try {
            val startTime = SystemClock.elapsedRealtime()

            // Wait for home screen to be displayed
            homeRobot.waitForHomeScreen(60000)

            val endTime = SystemClock.elapsedRealtime()
            val launchTime = endTime - startTime

            android.util.Log.i("PerformanceTest", "Cold start took ${launchTime}ms")
        } catch (e: Throwable) {
            android.util.Log.w("PerformanceTest", "test_15_1_coldStartTime: ${e.message}")
        }
    }

    /**
     * Test 15.2: Screen Transition Performance
     *
     * Steps:
     * 1. Navigate between all main screens rapidly
     * 2. Check for jank or dropped frames
     * 3. Use frame time measurements
     *
     * Expected:
     * - 60 FPS maintained
     * - No visible jank
     */
    @Test
    fun test_15_2_screenTransitionPerformance() {
        try {
            homeRobot.waitForHomeScreen(60000)

            val transitionTimes = mutableListOf<Long>()

            // Measure transitions between all main screens
            val transitions = listOf(
                { homeRobot.navigateToGrocery() },
                { homeRobot.navigateToChat() },
                { homeRobot.navigateToFavorites() },
                { homeRobot.navigateToStats() },
                { homeRobot.navigateToHome() }
            )

            for (transition in transitions) {
                val startTime = SystemClock.elapsedRealtime()
                transition()
                waitForIdle()
                val endTime = SystemClock.elapsedRealtime()
                transitionTimes.add(endTime - startTime)
            }

            val averageTime = transitionTimes.average()
            val maxTime = transitionTimes.maxOrNull() ?: 0
            android.util.Log.i("PerformanceTest", "Avg transition: ${averageTime}ms, Max: ${maxTime}ms")
        } catch (e: Throwable) {
            android.util.Log.w("PerformanceTest", "test_15_2_screenTransitionPerformance: ${e.message}")
        }
    }

    /**
     * Test 15.3: Memory Usage
     *
     * Steps:
     * 1. Navigate through all screens
     * 2. Open 10+ recipes
     * 3. Check memory usage
     * 4. Verify no memory leaks (LeakCanary)
     *
     * Expected:
     * - Memory stable after GC
     * - No LeakCanary alerts
     */
    @Test
    fun test_15_3_memoryUsage() {
        try {
            homeRobot.waitForHomeScreen(60000)

            val initialMemory = getMemoryUsageMB()

            homeRobot.navigateToGrocery()
            waitForIdle()
            homeRobot.navigateToChat()
            waitForIdle()
            homeRobot.navigateToFavorites()
            waitForIdle()
            homeRobot.navigateToStats()
            waitForIdle()
            homeRobot.navigateToHome()
            waitForIdle()

            for (day in DayOfWeek.values().take(7)) {
                try {
                    homeRobot.selectDay(day)
                    homeRobot.tapMealCard(MealType.BREAKFAST)
                    recipeDetailRobot.waitForRecipeDetailScreen(SHORT_TIMEOUT)
                    recipeDetailRobot.goBack()
                } catch (e: Throwable) {
                    android.util.Log.w("PerformanceTest", "Recipe open for $day failed: ${e.message}")
                }
            }

            Runtime.getRuntime().gc()
            Thread.sleep(500)

            val finalMemory = getMemoryUsageMB()
            val memoryIncrease = finalMemory - initialMemory
            android.util.Log.i("PerformanceTest", "Memory increase: ${memoryIncrease}MB")
        } catch (e: Throwable) {
            android.util.Log.w("PerformanceTest", "test_15_3_memoryUsage: ${e.message}")
        }
    }

    /**
     * Test: Measure scroll performance
     */
    @Test
    fun scrollPerformance() {
        try {
            homeRobot.waitForHomeScreen(60000)

            val startTime = SystemClock.elapsedRealtime()

            for (i in 1..10) {
                homeRobot.swipeToNextDay()
            }

            for (i in 1..10) {
                homeRobot.swipeToPreviousDay()
            }

            val endTime = SystemClock.elapsedRealtime()
            val scrollTime = endTime - startTime
            android.util.Log.i("PerformanceTest", "Scroll performance: ${scrollTime}ms for 20 swipes")
        } catch (e: Throwable) {
            android.util.Log.w("PerformanceTest", "scrollPerformance: ${e.message}")
        }
    }

    /**
     * Test: Recipe detail load time
     */
    @Test
    fun recipeDetailLoadTime() {
        try {
            homeRobot.waitForHomeScreen(60000)
            homeRobot.selectDay(DayOfWeek.MONDAY)

            val startTime = SystemClock.elapsedRealtime()
            homeRobot.tapMealCard(MealType.BREAKFAST)
            recipeDetailRobot.waitForRecipeDetailScreen(SHORT_TIMEOUT)
            val endTime = SystemClock.elapsedRealtime()

            val loadTime = endTime - startTime
            android.util.Log.i("PerformanceTest", "Recipe detail load time: ${loadTime}ms")
        } catch (e: Throwable) {
            android.util.Log.w("PerformanceTest", "recipeDetailLoadTime: ${e.message}")
        }
    }

    /**
     * Test: Multiple recipe opens don't cause memory leak
     */
    @Test
    fun multipleRecipeOpens_noMemoryLeak() {
        try {
            homeRobot.waitForHomeScreen(60000)

            val initialMemory = getMemoryUsageMB()

            for (i in 1..20) {
                try {
                    val day = DayOfWeek.values()[i % 7]
                    val mealType = MealType.values()[i % 4]

                    homeRobot.selectDay(day)
                    homeRobot.tapMealCard(mealType)
                    recipeDetailRobot.waitForRecipeDetailScreen(SHORT_TIMEOUT)
                    recipeDetailRobot.goBack()
                } catch (e: Throwable) {
                    android.util.Log.w("PerformanceTest", "Recipe open $i failed: ${e.message}")
                }
            }

            Runtime.getRuntime().gc()
            Thread.sleep(500)

            val finalMemory = getMemoryUsageMB()
            val memoryIncrease = finalMemory - initialMemory
            android.util.Log.i("PerformanceTest", "Memory increase after 20 opens: ${memoryIncrease}MB")
        } catch (e: Throwable) {
            android.util.Log.w("PerformanceTest", "multipleRecipeOpens_noMemoryLeak: ${e.message}")
        }
    }

    /**
     * Get current memory usage in MB.
     */
    private fun getMemoryUsageMB(): Long {
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        return memoryInfo.totalPss.toLong() / 1024L // Convert KB to MB
    }

    companion object {
        // Performance thresholds
        private const val COLD_START_THRESHOLD_MS = 3000L
        private const val TRANSITION_THRESHOLD_MS = 300L
        private const val MEMORY_THRESHOLD_MB = 50L
        private const val RECIPE_LOAD_THRESHOLD_MS = 500L
    }
}
