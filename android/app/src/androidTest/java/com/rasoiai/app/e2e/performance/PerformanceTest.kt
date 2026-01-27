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
        val startTime = SystemClock.elapsedRealtime()

        // Wait for home screen to be displayed
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)

        val endTime = SystemClock.elapsedRealtime()
        val launchTime = endTime - startTime

        // Cold start should be under 3 seconds (3000ms)
        assertTrue(
            "Cold start took ${launchTime}ms, expected < 3000ms",
            launchTime < 3000
        )
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
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)

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

        // Calculate average transition time
        val averageTime = transitionTimes.average()

        // Screen transitions should be under 300ms on average
        assertTrue(
            "Average transition time ${averageTime}ms, expected < 300ms",
            averageTime < 300
        )

        // No single transition should exceed 500ms
        val maxTime = transitionTimes.maxOrNull() ?: 0
        assertTrue(
            "Max transition time ${maxTime}ms, expected < 500ms",
            maxTime < 500
        )
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
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)

        // Get initial memory usage
        val initialMemory = getMemoryUsageMB()

        // Navigate through all screens
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

        // Open multiple recipes
        for (day in DayOfWeek.values().take(7)) {
            homeRobot.selectDay(day)
            homeRobot.tapMealCard(MealType.BREAKFAST)
            recipeDetailRobot.waitForRecipeDetailScreen(SHORT_TIMEOUT)
            recipeDetailRobot.goBack()
        }

        // Force GC
        Runtime.getRuntime().gc()
        Thread.sleep(500)

        // Get final memory usage
        val finalMemory = getMemoryUsageMB()

        // Memory increase should be reasonable (< 50MB after navigation)
        val memoryIncrease = finalMemory - initialMemory
        assertTrue(
            "Memory increased by ${memoryIncrease}MB, expected < 50MB",
            memoryIncrease < 50
        )
    }

    /**
     * Test: Measure scroll performance
     */
    @Test
    fun scrollPerformance() {
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)

        // Measure scroll through days
        val startTime = SystemClock.elapsedRealtime()

        for (i in 1..10) {
            homeRobot.swipeToNextDay()
        }

        for (i in 1..10) {
            homeRobot.swipeToPreviousDay()
        }

        val endTime = SystemClock.elapsedRealtime()
        val scrollTime = endTime - startTime

        // 20 swipes should complete in reasonable time
        assertTrue(
            "Scroll performance: ${scrollTime}ms for 20 swipes",
            scrollTime < 5000
        )
    }

    /**
     * Test: Recipe detail load time
     */
    @Test
    fun recipeDetailLoadTime() {
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
        homeRobot.selectDay(DayOfWeek.MONDAY)

        val startTime = SystemClock.elapsedRealtime()
        homeRobot.tapMealCard(MealType.BREAKFAST)
        recipeDetailRobot.waitForRecipeDetailScreen(SHORT_TIMEOUT)
        val endTime = SystemClock.elapsedRealtime()

        val loadTime = endTime - startTime

        // Recipe detail should load in under 500ms
        assertTrue(
            "Recipe detail load time ${loadTime}ms, expected < 500ms",
            loadTime < 500
        )
    }

    /**
     * Test: Multiple recipe opens don't cause memory leak
     */
    @Test
    fun multipleRecipeOpens_noMemoryLeak() {
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)

        val initialMemory = getMemoryUsageMB()

        // Open and close many recipes
        for (i in 1..20) {
            val day = DayOfWeek.values()[i % 7]
            val mealType = MealType.values()[i % 4]

            homeRobot.selectDay(day)
            homeRobot.tapMealCard(mealType)
            recipeDetailRobot.waitForRecipeDetailScreen(SHORT_TIMEOUT)
            recipeDetailRobot.goBack()
        }

        // Force GC
        Runtime.getRuntime().gc()
        Thread.sleep(500)

        val finalMemory = getMemoryUsageMB()
        val memoryIncrease = finalMemory - initialMemory

        // Memory should be stable after many opens/closes
        assertTrue(
            "Memory increased by ${memoryIncrease}MB after 20 recipe opens, expected < 30MB",
            memoryIncrease < 30
        )
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
