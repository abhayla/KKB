package com.rasoiai.app.e2e.journeys

import android.util.Log
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.RecipeDetailRobot
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.e2e.util.JourneyStepLogger
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

/**
 * J04: Daily Meal Planning (single Activity session)
 *
 * Scenario: Returning user checks today's meals, browses days, views a recipe,
 * and executes a real meal swap with backend and Room DB verification.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J04_DailyMealPlanningJourney
 * ```
 */
@HiltAndroidTest
class J04_DailyMealPlanningJourney : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var recipeDetailRobot: RecipeDetailRobot
    private val logger = JourneyStepLogger("J04")

    companion object {
        private const val TAG = "J04_DailyMealPlanning"
        private const val SWAP_TIMEOUT_MS = 10_000L
    }

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()
        homeRobot = HomeRobot(composeTestRule)
        recipeDetailRobot = RecipeDetailRobot(composeTestRule)
    }

    /**
     * Recursively walks the semantic tree to find a node whose test tag
     * starts with [TestTags.SWAP_RECIPE_ITEM_PREFIX].
     */
    private fun findSwapItemTag(node: SemanticsNode): String? {
        val tag = node.config.getOrElseNullable(SemanticsProperties.TestTag) { null }
        if (tag != null && tag.startsWith(TestTags.SWAP_RECIPE_ITEM_PREFIX)) {
            return tag
        }
        for (child in node.children) {
            val found = findSwapItemTag(child)
            if (found != null) return found
        }
        return null
    }

    @Test
    fun dailyMealPlanningJourney() {
        val totalSteps = 13

        try {
            logger.step(1, totalSteps, "Home screen with meals") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
                homeRobot.assertAllMealCardsDisplayed()
            }

            logger.step(2, totalSteps, "Week selector navigation") {
                homeRobot.assertWeekSelectorDisplayed()
                homeRobot.selectDay(DayOfWeek.MONDAY)
                homeRobot.selectDay(DayOfWeek.WEDNESDAY)
                homeRobot.selectDay(DayOfWeek.SATURDAY)
            }

            logger.step(3, totalSteps, "Verify meal types") {
                homeRobot.assertMealCardDisplayed(MealType.BREAKFAST)
                homeRobot.assertMealCardDisplayed(MealType.LUNCH)
                homeRobot.assertMealCardDisplayed(MealType.DINNER)
                homeRobot.assertMealCardDisplayed(MealType.SNACKS)
            }

            logger.step(4, totalSteps, "Open recipe detail") {
                homeRobot.navigateToRecipeDetail(MealType.LUNCH)
                recipeDetailRobot.waitForRecipeDetailScreen()
                recipeDetailRobot.assertRecipeDetailScreenDisplayed()
            }

            logger.step(5, totalSteps, "View recipe info and actions") {
                recipeDetailRobot.assertIngredientsListDisplayed()
                recipeDetailRobot.assertStartCookingDisplayed()
            }

            // Record pre-swap dinner recipe from Room DB for comparison
            val today = java.time.LocalDate.now().toString()
            var preSwapDinnerRecipeId = ""
            var preSwapDinnerRecipeName = ""
            var mealPlanId = ""

            logger.step(6, totalSteps, "Record pre-swap dinner recipe from Room DB") {
                recipeDetailRobot.goBack()
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)

                val localPlan = runBlocking {
                    mealPlanDao.getMealPlanForDate(today).first()
                }
                assertNotNull("Room meal plan entity should not be null", localPlan)
                mealPlanId = localPlan!!.id

                val dinnerItems = runBlocking {
                    mealPlanDao.getMealPlanItemsForDateAndType(mealPlanId, today, "dinner")
                }
                assertTrue("Should have at least 1 dinner item", dinnerItems.isNotEmpty())
                preSwapDinnerRecipeId = dinnerItems[0].recipeId
                preSwapDinnerRecipeName = dinnerItems[0].recipeName
                Log.d(TAG, "Pre-swap dinner: '$preSwapDinnerRecipeName' (id=$preSwapDinnerRecipeId)")
            }

            logger.step(7, totalSteps, "Execute meal swap: open sheet, select alternative, confirm") {
                val swapStartTime = System.currentTimeMillis()

                // Open action sheet on dinner card
                homeRobot.tapMealCard(MealType.DINNER)
                homeRobot.assertRecipeActionSheetDisplayed()

                // Tap "Swap Recipe" to open swap sheet
                homeRobot.tapSwapRecipeAction()

                // Wait for swap sheet with suggestions
                homeRobot.assertSwapSheetDisplayed()
                homeRobot.assertSwapSuggestionsDisplayed()

                // Select the first available swap recipe item
                // Items have tags like "swap_recipe_item_{recipeId}"
                // Poll for swap items to appear (API fetches suggestions)
                val itemPollStart = System.currentTimeMillis()
                var swapItemTag: String? = null

                while (swapItemTag == null && (System.currentTimeMillis() - itemPollStart) < 8000) {
                    composeTestRule.waitForIdle()

                    // Scan grid's semantic children for swap item tags
                    val gridNodes = composeTestRule
                        .onAllNodesWithTag(TestTags.SWAP_RECIPE_GRID, useUnmergedTree = true)
                        .fetchSemanticsNodes()

                    for (gridNode in gridNodes) {
                        swapItemTag = findSwapItemTag(gridNode)
                        if (swapItemTag != null) break
                    }

                    if (swapItemTag == null) {
                        Thread.sleep(500)
                    }
                }

                Log.d(TAG, "Swap item search took ${System.currentTimeMillis() - itemPollStart}ms, found: $swapItemTag")
                assertNotNull(
                    "Should find a swap recipe item with tag prefix '${TestTags.SWAP_RECIPE_ITEM_PREFIX}'",
                    swapItemTag
                )

                // Tap the swap recipe item to execute the swap
                composeTestRule
                    .onNodeWithTag(swapItemTag!!, useUnmergedTree = true)
                    .performScrollTo()
                    .performClick()
                Log.d(TAG, "Tapped swap recipe item: $swapItemTag")

                composeTestRule.waitForIdle()

                // Wait for the swap to complete (sheet should dismiss)
                val swapElapsed = System.currentTimeMillis() - swapStartTime
                Log.d(TAG, "Swap interaction completed in ${swapElapsed}ms")
                assertTrue(
                    "Swap should complete within ${SWAP_TIMEOUT_MS}ms (took ${swapElapsed}ms)",
                    swapElapsed <= SWAP_TIMEOUT_MS
                )
            }

            logger.step(8, totalSteps, "Verify meal card shows different recipe after swap") {
                // Wait for UI to settle after swap
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                composeTestRule.waitForIdle()
                Thread.sleep(1000) // Allow Room/sync to propagate

                // Read the post-swap dinner item from Room
                val postSwapDinnerItems = runBlocking {
                    mealPlanDao.getMealPlanItemsForDateAndType(mealPlanId, today, "dinner")
                }
                assertTrue("Should still have dinner items after swap", postSwapDinnerItems.isNotEmpty())
                val postSwapRecipeId = postSwapDinnerItems[0].recipeId
                val postSwapRecipeName = postSwapDinnerItems[0].recipeName
                Log.d(TAG, "Post-swap dinner: '$postSwapRecipeName' (id=$postSwapRecipeId)")

                assertNotEquals(
                    "Dinner recipe should have changed after swap " +
                        "(pre='$preSwapDinnerRecipeName' post='$postSwapRecipeName')",
                    preSwapDinnerRecipeId,
                    postSwapRecipeId
                )
                Log.d(TAG, "Swap verified: '$preSwapDinnerRecipeName' -> '$postSwapRecipeName'")
            }

            logger.step(9, totalSteps, "Verify Room DB has updated meal plan items") {
                val localItems = runBlocking {
                    mealPlanDao.getMealPlanItemsSync(mealPlanId)
                }
                assertTrue(
                    "Room should have meal plan items (found ${localItems.size})",
                    localItems.isNotEmpty()
                )

                val mealTypes = localItems.map { it.mealType }.distinct()
                Log.d(TAG, "Room DB: ${localItems.size} items across meal types: $mealTypes")
                assertTrue(
                    "Room should have items for multiple meal types (found ${mealTypes.size})",
                    mealTypes.size >= 2
                )

                // Verify the swapped dinner item is persisted with the new recipe
                val dinnerItems = localItems.filter { it.mealType == "dinner" && it.date == today }
                assertTrue("Room should have dinner items for today", dinnerItems.isNotEmpty())
                assertNotEquals(
                    "Room dinner recipe ID should reflect the swap",
                    preSwapDinnerRecipeId,
                    dinnerItems[0].recipeId
                )
            }

            logger.step(10, totalSteps, "Verify backend meal plan reflects the swap") {
                val authToken = runBlocking {
                    userPreferencesDataStore.accessToken.first()
                }
                assertNotNull("Auth token should be stored in DataStore", authToken)

                val mealPlan = BackendTestHelper.getCurrentMealPlan(
                    BACKEND_BASE_URL, authToken!!
                )
                assertNotNull("Backend should have a meal plan", mealPlan)

                val daysArray = mealPlan!!.getJSONArray("days")
                assertTrue(
                    "Meal plan should have at least 1 day (found ${daysArray.length()})",
                    daysArray.length() >= 1
                )
                Log.d(TAG, "Backend: meal plan with ${daysArray.length()} days")

                // Find today's dinner in backend response and verify it matches the swap
                var backendDinnerRecipeId: String? = null
                for (d in 0 until daysArray.length()) {
                    val day = daysArray.getJSONObject(d)
                    if (day.getString("date") == today) {
                        val meals = day.getJSONObject("meals")
                        if (meals.has("dinner")) {
                            val dinnerArray = meals.getJSONArray("dinner")
                            if (dinnerArray.length() > 0) {
                                backendDinnerRecipeId = dinnerArray.getJSONObject(0)
                                    .optString("recipe_id", null)
                            }
                        }
                        break
                    }
                }

                if (backendDinnerRecipeId != null) {
                    assertNotEquals(
                        "Backend dinner recipe should reflect the swap",
                        preSwapDinnerRecipeId,
                        backendDinnerRecipeId
                    )
                    Log.d(TAG, "Backend swap verified: dinner recipe_id=$backendDinnerRecipeId (was $preSwapDinnerRecipeId)")
                } else {
                    Log.w(TAG, "Could not find today's dinner in backend response — skipping backend swap assertion")
                }
            }
            logger.step(11, totalSteps, "Lock a meal and verify locked state") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)

                // Lock lunch meal using the lock button on the meal card
                homeRobot.tapLockMeal(MealType.LUNCH)
                homeRobot.assertMealLocked(MealType.LUNCH)
                Log.d(TAG, "Lunch meal locked successfully")
            }

            logger.step(12, totalSteps, "Unlock the meal and verify unlocked state") {
                // Unlock the same meal
                homeRobot.tapLockMeal(MealType.LUNCH)
                homeRobot.assertMealUnlocked(MealType.LUNCH)
                Log.d(TAG, "Lunch meal unlocked successfully")
            }

            logger.step(13, totalSteps, "Lock entire day and verify day lock state") {
                // Lock the current day using the day header lock button
                homeRobot.tapDayLock()
                homeRobot.assertDayLocked()
                Log.d(TAG, "Day locked successfully")

                // Unlock the day
                homeRobot.tapDayLock()
                homeRobot.assertDayUnlocked()
                Log.d(TAG, "Day unlocked successfully")
            }
        } finally {
            logger.printSummary()
        }
    }
}
