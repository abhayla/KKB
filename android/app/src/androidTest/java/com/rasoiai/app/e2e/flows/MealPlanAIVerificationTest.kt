package com.rasoiai.app.e2e.flows

import android.util.Log
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.util.BackendTestHelper
import dagger.hilt.android.testing.HiltAndroidTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Requirement: Meal Plan AI Verification — captures AI input/output and validates
 * that Gemini-generated meal plans respect all user constraints.
 *
 * Unlike MealPlanGenerationFlowTest (UI-driven), this test operates entirely at
 * the API boundary: it sets up data via REST, triggers generation, and validates
 * the response JSON. No UI interaction needed.
 *
 * Test Profile: Sharma Family (Vegetarian + SATTVIC)
 * - Cuisines: North, South
 * - Dislikes: Karela, Baingan, Mushroom
 * - Weekday cooking: 30 min, Weekend: 60 min
 * - INCLUDE: Chai (daily B+S), Dal (4x/week L+D), Paneer (2x/week L+D)
 * - EXCLUDE: Mushroom (never), Onion (Tuesday)
 *
 * Validation Checks:
 * - STRUCT-01: 7 days in plan
 * - STRUCT-02: All 28 slots have items
 * - STRUCT-03: 80%+ slots have 2+ items
 * - SEM-01: All meals vegetarian (hard)
 * - SEM-02: No disliked items in names (hard)
 * - SEM-03: No Mushroom anywhere (hard)
 * - SEM-04: No Onion on Tuesday (hard)
 * - SEM-05: Weekday prep <= 30 min (hard)
 * - SEM-06: Weekend prep <= 60 min (hard)
 * - SEM-07..09: INCLUDE frequency checks (soft — logged, not asserted)
 *
 * Artifacts written to app external files:
 *   /storage/emulated/0/Android/data/com.rasoiai.app/files/ai_verification/
 */
@HiltAndroidTest
class MealPlanAIVerificationTest : BaseE2ETest() {

    private var authToken: String? = null
    private val validationResults = JSONArray()
    private var artifactDir: File? = null

    // Non-vegetarian keywords to detect in recipe names and dietary tags
    private val nonVegKeywords = listOf(
        "chicken", "mutton", "lamb", "fish", "prawn", "shrimp", "egg",
        "meat", "pork", "beef", "keema", "tikka chicken", "butter chicken",
        "tandoori chicken", "fish fry", "egg curry", "omelette", "scrambled egg",
        "non_vegetarian", "non-vegetarian", "NON_VEGETARIAN"
    )

    private val dislikedItems = listOf("karela", "baingan", "mushroom")

    // Weekend day names from the API response
    private val weekendDays = setOf("Saturday", "Sunday")

    @Before
    override fun setUp() {
        super.setUp()

        // Authenticate without meal plan (we'll generate via API)
        setUpAuthenticatedStateWithoutMealPlan()

        // Get auth token for direct API calls
        authToken = BackendTestHelper.authenticateWithRetry(
            baseUrl = BACKEND_BASE_URL,
            firebaseToken = "fake-firebase-token",
            maxRetries = 3
        )?.accessToken

        // Clear prior test data
        if (authToken != null) {
            clearRecipeRulesAndGoals()
            clearFamilyMembers()
        }

        // Set up artifact directory
        artifactDir = File(context.getExternalFilesDir(null), "ai_verification")
        artifactDir?.mkdirs()
    }

    @After
    override fun tearDown() {
        // Always write results artifact, even on failure
        try {
            writeArtifact("ai_verification_results.json", validationResults)
            Log.i(TAG, "Validation results written (${validationResults.length()} checks)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write results artifact: ${e.message}")
        }
        super.tearDown()
    }

    // ===================== Main Test =====================

    @Test
    fun aiGeneratedMealPlan_respectsAllInputConstraints() {
        val token = authToken ?: fail("Authentication failed — cannot proceed")
        token as String

        // Phase 1: Setup test data via API
        val inputContext = phase1_setupTestData(token)

        // Phase 2: Capture AI input context
        phase2_captureInputContext(token, inputContext)

        // Phase 3: Generate meal plan + capture output
        val mealPlanOutput = phase3_generateAndCapture(token)

        // Phase 4: Structural validation
        phase4_structuralValidation(mealPlanOutput)

        // Phase 5: Semantic validation
        phase5_semanticValidation(mealPlanOutput)

        // Summary
        logFinalSummary()
    }

    // ===================== Phase 1: Setup Test Data =====================

    private fun phase1_setupTestData(token: String): JSONObject {
        Log.i(TAG, "=== Phase 1: Setup Test Data via API ===")

        // 1a. Update user preferences (vegetarian, sattvic, north+south, dislikes, cooking times)
        val prefs = JSONObject().apply {
            put("household_size", 3)
            put("primary_diet", "vegetarian")
            put("dietary_restrictions", JSONArray(listOf("sattvic")))
            put("cuisine_preferences", JSONArray(listOf("north", "south")))
            put("disliked_ingredients", JSONArray(listOf("Karela", "Baingan", "Mushroom")))
            put("spice_level", "medium")
            put("weekday_cooking_time", 30)
            put("weekend_cooking_time", 60)
            put("busy_days", JSONArray(listOf("MONDAY", "WEDNESDAY", "FRIDAY")))
            put("items_per_meal", 2)
            put("strict_dietary_mode", true)
            put("strict_allergen_mode", true)
        }
        val prefsUpdated = BackendTestHelper.updateUserPreferences(BACKEND_BASE_URL, token, prefs)
        Log.i(TAG, "Preferences updated: $prefsUpdated")

        // 1b. Create family members
        val members = listOf(
            JSONObject().apply {
                put("name", "Ramesh")
                put("age_group", "adult")
                put("dietary_restrictions", JSONArray(listOf("diabetic-friendly")))
                put("health_conditions", JSONArray(listOf("diabetes", "low_oil")))
            },
            JSONObject().apply {
                put("name", "Sunita")
                put("age_group", "adult")
                put("dietary_restrictions", JSONArray())
                put("health_conditions", JSONArray(listOf("low_salt")))
            },
            JSONObject().apply {
                put("name", "Aarav")
                put("age_group", "child")
                put("dietary_restrictions", JSONArray())
                put("health_conditions", JSONArray(listOf("no_spicy")))
            }
        )

        val createdMembers = JSONArray()
        for (member in members) {
            val created = BackendTestHelper.createFamilyMember(BACKEND_BASE_URL, token, member)
            if (created != null) {
                createdMembers.put(created)
                Log.i(TAG, "Created family member: ${member.getString("name")}")
            } else {
                Log.w(TAG, "Failed to create family member: ${member.getString("name")}")
            }
        }

        // 1c. Create recipe rules
        val rules = listOf(
            // INCLUDE: Chai daily for breakfast
            JSONObject().apply {
                put("target_type", "INGREDIENT")
                put("action", "INCLUDE")
                put("target_name", "Chai")
                put("frequency_type", "DAILY")
                put("enforcement", "REQUIRED")
                put("meal_slot", "BREAKFAST")
                put("is_active", true)
            },
            // INCLUDE: Chai daily for snacks
            JSONObject().apply {
                put("target_type", "INGREDIENT")
                put("action", "INCLUDE")
                put("target_name", "Chai")
                put("frequency_type", "DAILY")
                put("enforcement", "REQUIRED")
                put("meal_slot", "SNACKS")
                put("is_active", true)
            },
            // INCLUDE: Dal 4x/week for lunch+dinner
            JSONObject().apply {
                put("target_type", "INGREDIENT")
                put("action", "INCLUDE")
                put("target_name", "Dal")
                put("frequency_type", "TIMES_PER_WEEK")
                put("frequency_count", 4)
                put("enforcement", "PREFERRED")
                put("meal_slot", "LUNCH")
                put("is_active", true)
            },
            // INCLUDE: Paneer 2x/week for lunch+dinner
            JSONObject().apply {
                put("target_type", "INGREDIENT")
                put("action", "INCLUDE")
                put("target_name", "Paneer")
                put("frequency_type", "TIMES_PER_WEEK")
                put("frequency_count", 2)
                put("enforcement", "PREFERRED")
                put("meal_slot", "LUNCH")
                put("is_active", true)
            },
            // EXCLUDE: Mushroom never
            JSONObject().apply {
                put("target_type", "INGREDIENT")
                put("action", "EXCLUDE")
                put("target_name", "Mushroom")
                put("frequency_type", "NEVER")
                put("enforcement", "REQUIRED")
                put("is_active", true)
            },
            // EXCLUDE: Onion on Tuesday
            JSONObject().apply {
                put("target_type", "INGREDIENT")
                put("action", "EXCLUDE")
                put("target_name", "Onion")
                put("frequency_type", "SPECIFIC_DAYS")
                put("frequency_days", "TUESDAY")
                put("enforcement", "REQUIRED")
                put("is_active", true)
            }
        )

        val createdRules = JSONArray()
        for (rule in rules) {
            val created = BackendTestHelper.createRecipeRule(BACKEND_BASE_URL, token, rule)
            if (created != null) {
                createdRules.put(created)
                Log.i(TAG, "Created rule: ${rule.getString("action")} ${rule.getString("target_name")}")
            } else {
                Log.w(TAG, "Failed to create rule: ${rule.getString("action")} ${rule.getString("target_name")}")
            }
        }

        Log.i(TAG, "Phase 1 complete: prefs=$prefsUpdated, members=${createdMembers.length()}, rules=${createdRules.length()}")

        return JSONObject().apply {
            put("preferences", prefs)
            put("family_members", createdMembers)
            put("recipe_rules", createdRules)
        }
    }

    // ===================== Phase 2: Capture AI Input =====================

    private fun phase2_captureInputContext(token: String, setupData: JSONObject) {
        Log.i(TAG, "=== Phase 2: Capture AI Input Context ===")

        // Fetch current state from API (what the AI will actually see)
        val userJson = BackendTestHelper.getCurrentUser(BACKEND_BASE_URL, token)
        val rulesJson = BackendTestHelper.getRecipeRules(BACKEND_BASE_URL, token)
        val membersJson = BackendTestHelper.getFamilyMembers(BACKEND_BASE_URL, token)

        val inputContext = JSONObject().apply {
            put("user_profile", userJson ?: JSONObject())
            put("recipe_rules", rulesJson ?: JSONObject())
            put("family_members", membersJson ?: JSONObject())
            put("setup_data_sent", setupData)
        }

        writeArtifact("ai_verification_input.json", inputContext)

        // Log summary
        val ruleCount = rulesJson?.optJSONArray("rules")?.length() ?: 0
        val memberCount = membersJson?.optJSONArray("members")?.length() ?: 0
        Log.i(TAG, "Input captured: user=${userJson != null}, rules=$ruleCount, members=$memberCount")
    }

    // ===================== Phase 3: Generate + Capture =====================

    private fun phase3_generateAndCapture(token: String): JSONObject {
        Log.i(TAG, "=== Phase 3: Generate Meal Plan + Capture Output ===")

        val maxAttempts = 5
        for (attempt in 1..maxAttempts) {
            Log.i(TAG, "Generation attempt $attempt/$maxAttempts")
            val startTime = System.currentTimeMillis()
            val mealPlanResponse = BackendTestHelper.generateMealPlanWithResponse(
                BACKEND_BASE_URL, token
            )
            val elapsed = System.currentTimeMillis() - startTime

            if (mealPlanResponse != null) {
                Log.i(TAG, "Meal plan generated in ${elapsed}ms (attempt $attempt)")
                writeArtifact("ai_verification_output.json", mealPlanResponse)
                logAllRecipeNames(mealPlanResponse)
                return mealPlanResponse
            }

            Log.w(TAG, "Attempt $attempt failed after ${elapsed}ms")
            if (attempt < maxAttempts) {
                val backoffSec = attempt * 5L
                Log.i(TAG, "Waiting ${backoffSec}s before retry...")
                Thread.sleep(backoffSec * 1000)
            }
        }

        fail("Meal plan generation returned null after $maxAttempts attempts")
        @Suppress("UNREACHABLE_CODE")
        throw AssertionError("unreachable")
    }

    // ===================== Phase 4: Structural Validation =====================

    private fun phase4_structuralValidation(mealPlan: JSONObject) {
        Log.i(TAG, "=== Phase 4: Structural Validation ===")

        val days = mealPlan.getJSONArray("days")

        // STRUCT-01: 7 days in plan
        val dayCount = days.length()
        logValidation("STRUCT-01", "7 days in plan", dayCount == 7, "found $dayCount days")
        assertTrue("STRUCT-01: Expected 7 days, found $dayCount", dayCount == 7)

        // STRUCT-02: All 28 slots have items
        var filledSlots = 0
        val totalSlots = 7 * 4
        for (d in 0 until days.length()) {
            val day = days.getJSONObject(d)
            val meals = day.getJSONObject("meals")
            for (slot in listOf("breakfast", "lunch", "dinner", "snacks")) {
                val items = meals.optJSONArray(slot)
                if (items != null && items.length() > 0) {
                    filledSlots++
                }
            }
        }
        logValidation("STRUCT-02", "All 28 slots have items", filledSlots == totalSlots,
            "$filledSlots/$totalSlots slots filled")
        assertTrue("STRUCT-02: Expected $totalSlots filled slots, found $filledSlots", filledSlots == totalSlots)

        // STRUCT-03: 80%+ slots have 2+ items
        var multiItemSlots = 0
        for (d in 0 until days.length()) {
            val day = days.getJSONObject(d)
            val meals = day.getJSONObject("meals")
            for (slot in listOf("breakfast", "lunch", "dinner", "snacks")) {
                val items = meals.optJSONArray(slot)
                if (items != null && items.length() >= 2) {
                    multiItemSlots++
                }
            }
        }
        val multiItemPct = (multiItemSlots.toDouble() / totalSlots * 100).toInt()
        val struct03Pass = multiItemPct >= 80
        logValidation("STRUCT-03", "80%+ slots have 2+ items", struct03Pass,
            "$multiItemSlots/$totalSlots ($multiItemPct%) have 2+ items")
        assertTrue("STRUCT-03: Only $multiItemPct% slots have 2+ items (need 80%)", struct03Pass)
    }

    // ===================== Phase 5: Semantic Validation =====================

    private fun phase5_semanticValidation(mealPlan: JSONObject) {
        Log.i(TAG, "=== Phase 5: Semantic Validation ===")

        val days = mealPlan.getJSONArray("days")
        val hardFailures = mutableListOf<String>()

        // SEM-01: All meals VEGETARIAN
        val nonVegFound = mutableListOf<String>()
        forEachMealItem(days) { dayName, slot, item ->
            val recipeName = item.optString("recipe_name", "").lowercase()
            val tags = getStringList(item.optJSONArray("dietary_tags"))

            // Check tags for non-veg
            for (tag in tags) {
                if (tag.lowercase() in listOf("non_vegetarian", "non-vegetarian")) {
                    nonVegFound.add("$dayName/$slot: ${item.optString("recipe_name")} (tag: $tag)")
                }
            }
            // Check name for non-veg keywords
            for (keyword in nonVegKeywords) {
                if (recipeName.contains(keyword)) {
                    nonVegFound.add("$dayName/$slot: ${item.optString("recipe_name")} (keyword: $keyword)")
                    break
                }
            }
        }
        val sem01Pass = nonVegFound.isEmpty()
        logValidation("SEM-01", "All meals VEGETARIAN", sem01Pass,
            if (sem01Pass) "no non-veg items found" else "non-veg found: ${nonVegFound.joinToString("; ")}")
        if (!sem01Pass) hardFailures.add("SEM-01: Non-veg items found: ${nonVegFound.joinToString("; ")}")

        // SEM-02: No disliked items in recipe names
        val dislikedFound = mutableListOf<String>()
        forEachMealItem(days) { dayName, slot, item ->
            val recipeName = item.optString("recipe_name", "").lowercase()
            for (disliked in dislikedItems) {
                if (recipeName.contains(disliked)) {
                    dislikedFound.add("$dayName/$slot: ${item.optString("recipe_name")} (disliked: $disliked)")
                }
            }
        }
        val sem02Pass = dislikedFound.isEmpty()
        logValidation("SEM-02", "No disliked items in names", sem02Pass,
            if (sem02Pass) "no disliked items found" else "disliked found: ${dislikedFound.joinToString("; ")}")
        if (!sem02Pass) hardFailures.add("SEM-02: Disliked items found: ${dislikedFound.joinToString("; ")}")

        // SEM-03: No Mushroom anywhere (EXCLUDE NEVER)
        val mushroomFound = mutableListOf<String>()
        forEachMealItem(days) { dayName, slot, item ->
            val recipeName = item.optString("recipe_name", "").lowercase()
            if (recipeName.contains("mushroom")) {
                mushroomFound.add("$dayName/$slot: ${item.optString("recipe_name")}")
            }
        }
        val sem03Pass = mushroomFound.isEmpty()
        logValidation("SEM-03", "No Mushroom anywhere", sem03Pass,
            if (sem03Pass) "no mushroom found" else "mushroom found: ${mushroomFound.joinToString("; ")}")
        if (!sem03Pass) hardFailures.add("SEM-03: Mushroom found: ${mushroomFound.joinToString("; ")}")

        // SEM-04: No Onion on Tuesday (EXCLUDE SPECIFIC_DAYS)
        val onionOnTuesday = mutableListOf<String>()
        for (d in 0 until days.length()) {
            val day = days.getJSONObject(d)
            val dayName = day.getString("day_name")
            if (dayName.equals("Tuesday", ignoreCase = true)) {
                forEachMealItemInDay(day) { slot, item ->
                    val recipeName = item.optString("recipe_name", "").lowercase()
                    if (recipeName.contains("onion")) {
                        onionOnTuesday.add("$slot: ${item.optString("recipe_name")}")
                    }
                }
            }
        }
        val sem04Pass = onionOnTuesday.isEmpty()
        logValidation("SEM-04", "No Onion on Tuesday", sem04Pass,
            if (sem04Pass) "no onion on Tuesday" else "onion on Tuesday: ${onionOnTuesday.joinToString("; ")}")
        if (!sem04Pass) hardFailures.add("SEM-04: Onion on Tuesday: ${onionOnTuesday.joinToString("; ")}")

        // SEM-05: Weekday prep times <= 30 min
        val weekdayOvertime = mutableListOf<String>()
        for (d in 0 until days.length()) {
            val day = days.getJSONObject(d)
            val dayName = day.getString("day_name")
            if (dayName !in weekendDays) {
                forEachMealItemInDay(day) { slot, item ->
                    val prepTime = item.optInt("prep_time_minutes", 0)
                    if (prepTime > 30) {
                        weekdayOvertime.add("$dayName/$slot: ${item.optString("recipe_name")} (${prepTime}min)")
                    }
                }
            }
        }
        val sem05Pass = weekdayOvertime.isEmpty()
        logValidation("SEM-05", "Weekday prep <= 30 min", sem05Pass,
            if (sem05Pass) "all weekday items within limit" else "overtime: ${weekdayOvertime.joinToString("; ")}")
        if (!sem05Pass) hardFailures.add("SEM-05: Weekday overtime: ${weekdayOvertime.joinToString("; ")}")

        // SEM-06: Weekend prep times <= 60 min
        val weekendOvertime = mutableListOf<String>()
        for (d in 0 until days.length()) {
            val day = days.getJSONObject(d)
            val dayName = day.getString("day_name")
            if (dayName in weekendDays) {
                forEachMealItemInDay(day) { slot, item ->
                    val prepTime = item.optInt("prep_time_minutes", 0)
                    if (prepTime > 60) {
                        weekendOvertime.add("$dayName/$slot: ${item.optString("recipe_name")} (${prepTime}min)")
                    }
                }
            }
        }
        val sem06Pass = weekendOvertime.isEmpty()
        logValidation("SEM-06", "Weekend prep <= 60 min", sem06Pass,
            if (sem06Pass) "all weekend items within limit" else "overtime: ${weekendOvertime.joinToString("; ")}")
        if (!sem06Pass) hardFailures.add("SEM-06: Weekend overtime: ${weekendOvertime.joinToString("; ")}")

        // SEM-07 (SOFT): Chai in breakfast/snacks >= 5/7 days
        var chaiDays = 0
        for (d in 0 until days.length()) {
            val day = days.getJSONObject(d)
            val meals = day.getJSONObject("meals")
            var chaiFound = false
            for (slot in listOf("breakfast", "snacks")) {
                val items = meals.optJSONArray(slot) ?: continue
                for (i in 0 until items.length()) {
                    val name = items.getJSONObject(i).optString("recipe_name", "").lowercase()
                    if (name.contains("chai") || name.contains("tea")) {
                        chaiFound = true
                        break
                    }
                }
                if (chaiFound) break
            }
            if (chaiFound) chaiDays++
        }
        val sem07Pass = chaiDays >= 5
        logValidation("SEM-07", "Chai in B/S >= 5/7 days (SOFT)", sem07Pass,
            "chai found in $chaiDays/7 days")
        if (!sem07Pass) Log.w(TAG, "SOFT FAIL SEM-07: Chai only in $chaiDays/7 days (expected >= 5)")

        // SEM-08 (SOFT): Dal in lunch/dinner 3-5 times
        var dalCount = 0
        forEachMealItem(days) { _, slot, item ->
            if (slot in listOf("lunch", "dinner")) {
                val name = item.optString("recipe_name", "").lowercase()
                if (name.contains("dal") || name.contains("daal") || name.contains("lentil")) {
                    dalCount++
                }
            }
        }
        val sem08Pass = dalCount in 3..5
        logValidation("SEM-08", "Dal in L/D 3-5 times (SOFT)", sem08Pass,
            "dal found $dalCount times in lunch/dinner")
        if (!sem08Pass) Log.w(TAG, "SOFT FAIL SEM-08: Dal found $dalCount times (expected 3-5)")

        // SEM-09 (SOFT): Paneer in lunch/dinner 1-3 times
        var paneerCount = 0
        forEachMealItem(days) { _, slot, item ->
            if (slot in listOf("lunch", "dinner")) {
                val name = item.optString("recipe_name", "").lowercase()
                if (name.contains("paneer")) {
                    paneerCount++
                }
            }
        }
        val sem09Pass = paneerCount in 1..3
        logValidation("SEM-09", "Paneer in L/D 1-3 times (SOFT)", sem09Pass,
            "paneer found $paneerCount times in lunch/dinner")
        if (!sem09Pass) Log.w(TAG, "SOFT FAIL SEM-09: Paneer found $paneerCount times (expected 1-3)")

        // Assert hard failures
        if (hardFailures.isNotEmpty()) {
            fail("Hard validation failures:\n${hardFailures.joinToString("\n")}")
        }
    }

    // ===================== Utility Methods =====================

    /**
     * Iterates all meal items across all days and slots.
     */
    private fun forEachMealItem(
        days: JSONArray,
        callback: (dayName: String, slot: String, item: JSONObject) -> Unit
    ) {
        for (d in 0 until days.length()) {
            val day = days.getJSONObject(d)
            val dayName = day.getString("day_name")
            val meals = day.getJSONObject("meals")
            for (slot in listOf("breakfast", "lunch", "dinner", "snacks")) {
                val items = meals.optJSONArray(slot) ?: continue
                for (i in 0 until items.length()) {
                    callback(dayName, slot, items.getJSONObject(i))
                }
            }
        }
    }

    /**
     * Iterates all meal items within a single day.
     */
    private fun forEachMealItemInDay(
        day: JSONObject,
        callback: (slot: String, item: JSONObject) -> Unit
    ) {
        val meals = day.getJSONObject("meals")
        for (slot in listOf("breakfast", "lunch", "dinner", "snacks")) {
            val items = meals.optJSONArray(slot) ?: continue
            for (i in 0 until items.length()) {
                callback(slot, items.getJSONObject(i))
            }
        }
    }

    /**
     * Extracts a list of strings from a JSONArray.
     */
    private fun getStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.getString(it) }
    }

    /**
     * Logs a validation check result to both Logcat and the results JSON array.
     */
    private fun logValidation(checkId: String, description: String, passed: Boolean, detail: String) {
        val status = if (passed) "PASS" else "FAIL"
        Log.i(TAG, "[$status] $checkId: $description — $detail")

        validationResults.put(JSONObject().apply {
            put("check_id", checkId)
            put("description", description)
            put("passed", passed)
            put("detail", detail)
        })
    }

    /**
     * Writes a JSON artifact to the external files directory.
     */
    private fun writeArtifact(filename: String, json: Any) {
        try {
            val file = File(artifactDir, filename)
            val content = when (json) {
                is JSONObject -> json.toString(2)
                is JSONArray -> json.toString(2)
                else -> json.toString()
            }
            file.writeText(content)
            Log.i(TAG, "Artifact written: ${file.absolutePath} (${content.length} chars)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write artifact $filename: ${e.message}")
        }
    }

    /**
     * Logs all recipe names in a formatted table.
     */
    private fun logAllRecipeNames(mealPlan: JSONObject) {
        val days = mealPlan.getJSONArray("days")
        val sb = StringBuilder()
        sb.appendLine("╔════════════════════════════════════════════════════════════╗")
        sb.appendLine("║              AI GENERATED MEAL PLAN RECIPES                ║")
        sb.appendLine("╠════════════════════════════════════════════════════════════╣")

        for (d in 0 until days.length()) {
            val day = days.getJSONObject(d)
            val dayName = day.getString("day_name")
            sb.appendLine("║ $dayName")
            val meals = day.getJSONObject("meals")
            for (slot in listOf("breakfast", "lunch", "dinner", "snacks")) {
                val items = meals.optJSONArray(slot) ?: continue
                val names = (0 until items.length()).map {
                    items.getJSONObject(it).optString("recipe_name", "?")
                }
                sb.appendLine("║   ${slot.uppercase()}: ${names.joinToString(", ")}")
            }
        }
        sb.appendLine("╚════════════════════════════════════════════════════════════╝")
        Log.i(TAG, sb.toString())
    }

    /**
     * Logs the final summary of all validation checks.
     */
    private fun logFinalSummary() {
        var passCount = 0
        var failCount = 0
        for (i in 0 until validationResults.length()) {
            if (validationResults.getJSONObject(i).getBoolean("passed")) passCount++ else failCount++
        }
        Log.i(TAG, "=== VALIDATION SUMMARY: $passCount passed, $failCount failed out of ${validationResults.length()} checks ===")
        Log.i(TAG, "Artifacts at: ${artifactDir?.absolutePath}")
        Log.i(TAG, "Pull with: adb pull ${artifactDir?.absolutePath}/ .")
    }

    companion object {
        private const val TAG = "MealPlanAIVerify"
    }
}
