package com.rasoiai.data.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.rasoiai.data.local.dao.OfflineQueueDao
import com.rasoiai.data.local.entity.OfflineQueueEntity
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.FcmTokenRequest
import com.rasoiai.data.remote.dto.MealPlanResponse
import com.rasoiai.data.remote.dto.NutritionGoalUpdateRequest
import com.rasoiai.data.remote.dto.RecipeRuleCreateRequest
import com.rasoiai.data.remote.dto.RecipeRuleDto
import com.rasoiai.data.remote.dto.RecipeRuleUpdateRequest
import com.rasoiai.data.remote.dto.SuccessResponse
import com.rasoiai.data.remote.dto.SwapMealRequest
import com.rasoiai.data.remote.dto.UserResponse
import com.rasoiai.domain.model.OfflineActionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SyncWorkerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockOfflineQueueDao: OfflineQueueDao
    private lateinit var mockApiService: RasoiApiService
    private lateinit var mockContext: Context
    private lateinit var mockWorkerParams: WorkerParameters

    private lateinit var syncWorker: SyncWorker

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockOfflineQueueDao = mockk(relaxed = true)
        mockApiService = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)
        mockWorkerParams = mockk(relaxed = true)

        every { mockWorkerParams.runAttemptCount } returns 0

        syncWorker = SyncWorker(
            appContext = mockContext,
            workerParams = mockWorkerParams,
            offlineQueueDao = mockOfflineQueueDao,
            apiService = mockApiService
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Helper Methods

    private fun createQueueEntity(
        actionType: OfflineActionType,
        payload: String,
        retryCount: Int = 0,
        status: String = "pending"
    ): OfflineQueueEntity {
        return OfflineQueueEntity(
            id = UUID.randomUUID().toString(),
            actionType = actionType.value,
            payload = payload,
            status = status,
            retryCount = retryCount,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun swapMealPayload(planId: String = "plan-1", itemId: String = "item-1"): String {
        return """{"plan_id":"$planId","item_id":"$itemId"}"""
    }

    private fun toggleFavoritePayload(recipeId: String = "recipe-1", isFavorite: Boolean = true): String {
        return """{"recipe_id":"$recipeId","is_favorite":$isFavorite}"""
    }

    private fun notificationPayload(notificationId: String = "notif-1"): String {
        return """{"notification_id":"$notificationId"}"""
    }

    private fun fcmTokenPayload(token: String = "test-fcm-token-abc123"): String {
        return """{"fcm_token":"$token"}"""
    }

    private fun recipeRuleTogglePayload(ruleId: String = "rule-1", isActive: Boolean = true): String {
        return """{"rule_id":"$ruleId","is_active":$isActive}"""
    }

    private fun nutritionGoalTogglePayload(goalId: String = "goal-1", isActive: Boolean = false): String {
        return """{"goal_id":"$goalId","is_active":$isActive}"""
    }

    private fun mealIdsPayload(planId: String = "plan-1", itemId: String = "item-1"): String {
        return """{"plan_id":"$planId","item_id":"$itemId"}"""
    }

    private fun createRecipeRulePayload(): String {
        return """{"target_type":"ingredient","action":"INCLUDE","target_name":"Paneer","frequency_type":"weekly","frequency_count":3,"enforcement":"preferred","is_active":true,"force_override":false}"""
    }

    private fun deleteRecipeRulePayload(ruleId: String = "rule-to-delete"): String {
        return """{"rule_id":"$ruleId"}"""
    }

    private fun syncPreferencesPayload(): String {
        return """{"household_size":4,"primary_diet":"vegetarian","spice_level":"medium","weekday_cooking_time":30,"weekend_cooking_time":60,"items_per_meal":2,"strict_allergen_mode":true,"strict_dietary_mode":false,"allow_recipe_repeat":true,"dietary_restrictions":["gluten-free","nut-free"],"cuisine_preferences":["North Indian","South Indian"],"disliked_ingredients":["bitter gourd","raw onion"],"busy_days":["Monday","Wednesday"]}"""
    }

    private fun createHttpException(code: Int): HttpException {
        val response = Response.error<Any>(code, "".toResponseBody(null))
        return HttpException(response)
    }

    // endregion

    @Nested
    @DisplayName("Action Processing — API Calls")
    inner class ActionProcessing {

        @Test
        @DisplayName("SWAP_MEAL calls apiService.swapMealItem with correct IDs")
        fun test_processSwapMeal_callsApi() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.SWAP_MEAL,
                swapMealPayload("plan-abc", "item-xyz")
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)

            syncWorker.doWork()

            coVerify {
                mockApiService.swapMealItem("plan-abc", "item-xyz", any<SwapMealRequest>())
            }
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }

        @Test
        @DisplayName("TOGGLE_FAVORITE with is_favorite=true calls addFavorite")
        fun test_processToggleFavorite_callsAdd() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.TOGGLE_FAVORITE,
                toggleFavoritePayload("recipe-42", isFavorite = true)
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)

            syncWorker.doWork()

            coVerify { mockApiService.addFavorite(match { it["recipe_id"] == "recipe-42" }) }
            coVerify(exactly = 0) { mockApiService.removeFavorite(any()) }
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }

        @Test
        @DisplayName("TOGGLE_FAVORITE with is_favorite=false calls removeFavorite")
        fun test_processToggleFavorite_callsRemove() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.TOGGLE_FAVORITE,
                toggleFavoritePayload("recipe-42", isFavorite = false)
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)

            syncWorker.doWork()

            coVerify { mockApiService.removeFavorite("recipe-42") }
            coVerify(exactly = 0) { mockApiService.addFavorite(any()) }
        }

        @Test
        @DisplayName("MARK_NOTIFICATION_READ calls markNotificationAsRead")
        fun test_markNotificationRead_callsApi() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.MARK_NOTIFICATION_READ,
                notificationPayload("notif-abc")
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)

            syncWorker.doWork()

            coVerify { mockApiService.markNotificationAsRead("notif-abc") }
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }

        @Test
        @DisplayName("DELETE_NOTIFICATION calls deleteNotification")
        fun test_deleteNotification_callsApi() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.DELETE_NOTIFICATION,
                notificationPayload("notif-del")
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)

            syncWorker.doWork()

            coVerify { mockApiService.deleteNotification("notif-del") }
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }

        @Test
        @DisplayName("REGISTER_FCM_TOKEN calls registerFcmToken with correct token")
        fun test_registerFcmToken_callsApi() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.REGISTER_FCM_TOKEN,
                fcmTokenPayload("my-device-token")
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)
            val tokenSlot = slot<FcmTokenRequest>()
            coEvery { mockApiService.registerFcmToken(capture(tokenSlot)) } returns SuccessResponse(true)

            syncWorker.doWork()

            assertEquals("my-device-token", tokenSlot.captured.fcmToken)
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }

        @Test
        @DisplayName("UNREGISTER_FCM_TOKEN calls unregisterFcmToken")
        fun test_unregisterFcmToken_callsApi() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.UNREGISTER_FCM_TOKEN,
                fcmTokenPayload("old-token")
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)

            syncWorker.doWork()

            coVerify { mockApiService.unregisterFcmToken("old-token") }
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }

        @Test
        @DisplayName("LOCK_MEAL calls lockMealItem with correct IDs")
        fun test_lockMeal_callsApi() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.LOCK_MEAL,
                mealIdsPayload("plan-lock", "item-lock")
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)

            syncWorker.doWork()

            coVerify { mockApiService.lockMealItem("plan-lock", "item-lock") }
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }

        @Test
        @DisplayName("REMOVE_MEAL calls removeMealItem with correct IDs")
        fun test_removeMeal_callsApi() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.REMOVE_MEAL,
                mealIdsPayload("plan-rem", "item-rem")
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)

            syncWorker.doWork()

            coVerify { mockApiService.removeMealItem("plan-rem", "item-rem") }
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }

        @Test
        @DisplayName("TOGGLE_RECIPE_RULE calls updateRecipeRule with correct params")
        fun test_toggleRecipeRule_callsApi() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.TOGGLE_RECIPE_RULE,
                recipeRuleTogglePayload("rule-99", isActive = false)
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)
            val updateSlot = slot<RecipeRuleUpdateRequest>()
            coEvery {
                mockApiService.updateRecipeRule("rule-99", capture(updateSlot))
            } returns mockk(relaxed = true)

            syncWorker.doWork()

            assertEquals(false, updateSlot.captured.isActive)
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }

        @Test
        @DisplayName("TOGGLE_NUTRITION_GOAL calls updateNutritionGoal with correct params")
        fun test_toggleNutritionGoal_callsApi() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.TOGGLE_NUTRITION_GOAL,
                nutritionGoalTogglePayload("goal-42", isActive = true)
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)
            val updateSlot = slot<NutritionGoalUpdateRequest>()
            coEvery {
                mockApiService.updateNutritionGoal("goal-42", capture(updateSlot))
            } returns mockk(relaxed = true)

            syncWorker.doWork()

            assertEquals(true, updateSlot.captured.isActive)
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }

        @Test
        @DisplayName("UPDATE_GROCERY completes immediately as local-only no-op")
        fun test_updateGrocery_isNoOp() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.UPDATE_GROCERY,
                """{"item_id":"grocery-1","checked":true}"""
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)

            syncWorker.doWork()

            // No API calls should be made for grocery updates
            coVerify(exactly = 0) { mockApiService.getGroceryList(any()) }
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }
    }

    @Nested
    @DisplayName("SYNC_PREFERENCES — Payload Fields")
    inner class SyncPreferences {

        @Test
        @DisplayName("SYNC_PREFERENCES sends all expected fields to API")
        fun test_processSyncPreferences_sendsAllFields() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.SYNC_PREFERENCES,
                syncPreferencesPayload()
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)
            val prefsSlot = slot<Map<String, Any>>()
            coEvery { mockApiService.updateUserPreferences(capture(prefsSlot)) } returns mockk(relaxed = true)

            syncWorker.doWork()

            val prefs = prefsSlot.captured
            assertEquals(4, prefs["household_size"])
            assertEquals("vegetarian", prefs["primary_diet"])
            assertEquals("medium", prefs["spice_level"])
            assertEquals(30, prefs["weekday_cooking_time"])
            assertEquals(60, prefs["weekend_cooking_time"])
            assertEquals(2, prefs["items_per_meal"])
            assertEquals(true, prefs["strict_allergen_mode"])
            assertEquals(false, prefs["strict_dietary_mode"])
            assertEquals(true, prefs["allow_recipe_repeat"])
            @Suppress("UNCHECKED_CAST")
            val dietaryRestrictions = prefs["dietary_restrictions"] as List<String>
            assertEquals(listOf("gluten-free", "nut-free"), dietaryRestrictions)
            @Suppress("UNCHECKED_CAST")
            val cuisinePreferences = prefs["cuisine_preferences"] as List<String>
            assertEquals(listOf("North Indian", "South Indian"), cuisinePreferences)
            @Suppress("UNCHECKED_CAST")
            val dislikedIngredients = prefs["disliked_ingredients"] as List<String>
            assertEquals(listOf("bitter gourd", "raw onion"), dislikedIngredients)
            @Suppress("UNCHECKED_CAST")
            val busyDays = prefs["busy_days"] as List<String>
            assertEquals(listOf("Monday", "Wednesday"), busyDays)

            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }
    }

    @Nested
    @DisplayName("HTTP Error Tolerance")
    inner class HttpErrorTolerance {

        @Test
        @DisplayName("CREATE_RECIPE_RULE treats 409 Conflict as success (idempotent)")
        fun test_processCreateRecipeRule_handles409() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.CREATE_RECIPE_RULE,
                createRecipeRulePayload()
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)
            coEvery { mockApiService.createRecipeRule(any()) } throws createHttpException(409)

            syncWorker.doWork()

            // 409 should be treated as success — rule already exists
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
            coVerify(exactly = 0) { mockOfflineQueueDao.markFailed(entity.id, any(), any()) }
        }

        @Test
        @DisplayName("CREATE_RECIPE_RULE re-throws non-409 HTTP errors")
        fun test_processCreateRecipeRule_rethrowsOtherErrors() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.CREATE_RECIPE_RULE,
                createRecipeRulePayload()
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)
            coEvery { mockApiService.createRecipeRule(any()) } throws createHttpException(500)

            syncWorker.doWork()

            // 500 should cause the action to be marked as failed
            coVerify { mockOfflineQueueDao.markFailed(entity.id, any(), any()) }
            coVerify(exactly = 0) { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }

        @Test
        @DisplayName("DELETE_RECIPE_RULE treats 404 Not Found as success (already deleted)")
        fun test_processDeleteRecipeRule_handles404() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.DELETE_RECIPE_RULE,
                deleteRecipeRulePayload("rule-gone")
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)
            coEvery { mockApiService.deleteRecipeRule("rule-gone") } throws createHttpException(404)

            syncWorker.doWork()

            // 404 should be treated as success — rule already deleted
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
            coVerify(exactly = 0) { mockOfflineQueueDao.markFailed(entity.id, any(), any()) }
        }

        @Test
        @DisplayName("DELETE_RECIPE_RULE re-throws non-404 HTTP errors")
        fun test_processDeleteRecipeRule_rethrowsOtherErrors() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.DELETE_RECIPE_RULE,
                deleteRecipeRulePayload("rule-err")
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)
            coEvery { mockApiService.deleteRecipeRule("rule-err") } throws createHttpException(500)

            syncWorker.doWork()

            coVerify { mockOfflineQueueDao.markFailed(entity.id, any(), any()) }
            coVerify(exactly = 0) { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }
    }

    @Nested
    @DisplayName("Retry Logic")
    inner class RetryLogic {

        @Test
        @DisplayName("Failed action increments retry count via markFailed")
        fun test_failedAction_incrementsRetryCount() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.SWAP_MEAL,
                swapMealPayload(),
                retryCount = 0
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)
            coEvery {
                mockApiService.swapMealItem(any(), any(), any())
            } throws RuntimeException("Network timeout")

            syncWorker.doWork()

            // markFailed increments retryCount via SQL (retryCount = retryCount + 1)
            coVerify { mockOfflineQueueDao.markFailed(entity.id, match { it.contains("Network timeout") }, any()) }
        }

        @Test
        @DisplayName("Worker returns retry when there are failed actions and under max attempts")
        fun test_retryLogic_retriesWhenUnderMax() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.SWAP_MEAL,
                swapMealPayload()
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)
            coEvery {
                mockApiService.swapMealItem(any(), any(), any())
            } throws RuntimeException("Server error")
            every { mockWorkerParams.runAttemptCount } returns 0

            val result = syncWorker.doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
        }

        @Test
        @DisplayName("Worker returns success when all actions succeed")
        fun test_retryLogic_successWhenAllPass() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.UPDATE_GROCERY,
                """{"item_id":"g1"}"""
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)

            val result = syncWorker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
        }

        @Test
        @DisplayName("Worker returns success after max retry attempts even with failures")
        fun test_retryLogic_maxRetries() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.SWAP_MEAL,
                swapMealPayload()
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)
            coEvery {
                mockApiService.swapMealItem(any(), any(), any())
            } throws RuntimeException("Persistent error")
            // Simulate being at max retries (MAX_RETRY_ATTEMPTS = 3)
            every { mockWorkerParams.runAttemptCount } returns 3

            val result = syncWorker.doWork()

            // At max retries, worker returns success to stop WorkManager retries
            assertEquals(ListenableWorker.Result.success(), result)
        }

        @Test
        @DisplayName("Worker returns failure on unhandled exception at max retries")
        fun test_retryLogic_failureOnUnhandledException() = runTest {
            // Simulate a crash in getPendingActions itself
            coEvery { mockOfflineQueueDao.getPendingActions() } throws RuntimeException("DB corrupted")
            every { mockWorkerParams.runAttemptCount } returns 3

            val result = syncWorker.doWork()

            assertEquals(ListenableWorker.Result.failure(), result)
        }

        @Test
        @DisplayName("Worker retries on unhandled exception under max retries")
        fun test_retryLogic_retryOnUnhandledException() = runTest {
            coEvery { mockOfflineQueueDao.getPendingActions() } throws RuntimeException("Transient DB error")
            every { mockWorkerParams.runAttemptCount } returns 1

            val result = syncWorker.doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
        }
    }

    @Nested
    @DisplayName("Cleanup")
    inner class Cleanup {

        @Test
        @DisplayName("Completed actions are cleaned up after processing")
        fun test_completedActions_cleaned() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.UPDATE_GROCERY,
                """{"item_id":"g1"}"""
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)

            syncWorker.doWork()

            coVerify { mockOfflineQueueDao.deleteCompletedActions() }
            coVerify { mockOfflineQueueDao.deleteOldCompletedActions(any()) }
        }

        @Test
        @DisplayName("Old completed actions older than 7 days are purged")
        fun test_oldCompletedActions_purged() = runTest {
            coEvery { mockOfflineQueueDao.getPendingActions() } returns emptyList()

            syncWorker.doWork()

            // Verify deleteOldCompletedActions is called with a timestamp ~7 days ago
            val timestampSlot = slot<Long>()
            coVerify { mockOfflineQueueDao.deleteOldCompletedActions(capture(timestampSlot)) }
            val sevenDaysMillis = 7 * 24 * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            // Allow 5 second tolerance for test execution time
            val expectedMin = now - sevenDaysMillis - 5000
            val expectedMax = now - sevenDaysMillis + 5000
            assert(timestampSlot.captured in expectedMin..expectedMax) {
                "Expected timestamp ~7 days ago, got ${timestampSlot.captured}"
            }
        }

        @Test
        @DisplayName("Cleanup runs even when there are no pending actions")
        fun test_cleanup_runsOnEmptyQueue() = runTest {
            coEvery { mockOfflineQueueDao.getPendingActions() } returns emptyList()

            syncWorker.doWork()

            coVerify { mockOfflineQueueDao.deleteCompletedActions() }
            coVerify { mockOfflineQueueDao.deleteOldCompletedActions(any()) }
        }
    }

    @Nested
    @DisplayName("Multiple Actions")
    inner class MultipleActions {

        @Test
        @DisplayName("Processes multiple actions in sequence, marks each appropriately")
        fun test_multipleActions_processedInOrder() = runTest {
            val swapEntity = createQueueEntity(
                OfflineActionType.SWAP_MEAL,
                swapMealPayload()
            )
            val groceryEntity = createQueueEntity(
                OfflineActionType.UPDATE_GROCERY,
                """{"item_id":"g1"}"""
            )
            val failEntity = createQueueEntity(
                OfflineActionType.LOCK_MEAL,
                mealIdsPayload("p-fail", "i-fail")
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(
                swapEntity, groceryEntity, failEntity
            )
            coEvery {
                mockApiService.lockMealItem("p-fail", "i-fail")
            } throws RuntimeException("Server down")

            syncWorker.doWork()

            // First two succeed
            coVerify { mockOfflineQueueDao.markCompleted(swapEntity.id, any()) }
            coVerify { mockOfflineQueueDao.markCompleted(groceryEntity.id, any()) }
            // Third fails
            coVerify { mockOfflineQueueDao.markFailed(failEntity.id, any(), any()) }
        }

        @Test
        @DisplayName("Each action is marked in_progress before processing")
        fun test_actionsMarkedInProgress() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.MARK_NOTIFICATION_READ,
                notificationPayload()
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)

            syncWorker.doWork()

            coVerify { mockOfflineQueueDao.markInProgress(entity.id, any()) }
            coVerify { mockOfflineQueueDao.markCompleted(entity.id, any()) }
        }
    }

    @Nested
    @DisplayName("CREATE_RECIPE_RULE — Payload Parsing")
    inner class CreateRecipeRulePayloadParsing {

        @Test
        @DisplayName("CREATE_RECIPE_RULE sends correctly parsed RecipeRuleCreateRequest")
        fun test_createRecipeRule_parsesPayload() = runTest {
            val entity = createQueueEntity(
                OfflineActionType.CREATE_RECIPE_RULE,
                createRecipeRulePayload()
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(entity)
            val ruleSlot = slot<RecipeRuleCreateRequest>()
            coEvery {
                mockApiService.createRecipeRule(capture(ruleSlot))
            } returns Response.success(mockk(relaxed = true))

            syncWorker.doWork()

            val captured = ruleSlot.captured
            assertEquals("ingredient", captured.targetType)
            assertEquals("INCLUDE", captured.action)
            assertEquals("Paneer", captured.targetName)
            assertEquals("weekly", captured.frequencyType)
            assertEquals(3, captured.frequencyCount)
            assertEquals("preferred", captured.enforcement)
            assertEquals(true, captured.isActive)
            assertEquals(false, captured.forceOverride)
        }
    }
}
