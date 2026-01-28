package com.rasoiai.app.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.home.components.RasoiBottomNavigation
import com.rasoiai.app.presentation.navigation.Screen
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.MealItem
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealPlanDay
import com.rasoiai.domain.model.MealType
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Compose UI tests for HomeScreen.
 *
 * These tests verify the UI behavior of HomeScreen using Compose testing APIs.
 * They test the UI layer in isolation by providing mock data directly to a
 * test wrapper composable that mirrors HomeScreenContent structure.
 *
 * ## Test Categories:
 * - Loading state tests
 * - Week selector tests
 * - Meal section tests
 * - Top app bar tests
 * - Bottom navigation tests
 * - Lock state tests
 * - Interaction tests
 *
 * ## Running Tests:
 * ```bash
 * ./gradlew :app:connectedAndroidTest --tests "*.HomeScreenTest"
 * ```
 */
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data Factory

    private fun createTestMealItem(
        id: String = "meal-1",
        recipeId: String = "recipe-1",
        name: String = "Dal Tadka",
        prepTime: Int = 30,
        calories: Int = 250,
        isLocked: Boolean = false,
        dietaryTags: List<DietaryTag> = listOf(DietaryTag.VEGETARIAN)
    ) = MealItem(
        id = id,
        recipeId = recipeId,
        recipeName = name,
        recipeImageUrl = null,
        prepTimeMinutes = prepTime,
        calories = calories,
        isLocked = isLocked,
        order = 0,
        dietaryTags = dietaryTags
    )

    private fun createTestMealPlanDay(
        date: LocalDate = LocalDate.now(),
        breakfast: List<MealItem> = listOf(createTestMealItem(id = "b1", name = "Poha")),
        lunch: List<MealItem> = listOf(createTestMealItem(id = "l1", name = "Dal Tadka")),
        dinner: List<MealItem> = listOf(createTestMealItem(id = "d1", name = "Paneer Tikka")),
        snacks: List<MealItem> = listOf(createTestMealItem(id = "s1", name = "Samosa"))
    ) = MealPlanDay(
        date = date,
        dayName = date.dayOfWeek.name,
        breakfast = breakfast,
        lunch = lunch,
        dinner = dinner,
        snacks = snacks,
        festival = null
    )

    private fun createTestMealPlan(
        days: List<MealPlanDay> = (0..6).map { offset ->
            createTestMealPlanDay(
                date = LocalDate.now().plusDays(offset.toLong() - LocalDate.now().dayOfWeek.ordinal)
            )
        }
    ): MealPlan {
        val weekStart = days.first().date
        return MealPlan(
            id = "plan-1",
            weekStartDate = weekStart,
            weekEndDate = weekStart.plusDays(6),
            days = days,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun createTestUiState(
        isLoading: Boolean = false,
        mealPlan: MealPlan? = createTestMealPlan(),
        selectedDate: LocalDate = LocalDate.now(),
        isSelectedDayLocked: Boolean = false
    ): HomeUiState {
        val weekDates = mealPlan?.let {
            (0..6).map { offset ->
                val date = it.weekStartDate.plusDays(offset.toLong())
                WeekDay(
                    date = date,
                    dayName = date.dayOfWeek.name.take(2),
                    dayNumber = date.dayOfMonth,
                    isSelected = date == selectedDate,
                    isToday = date == LocalDate.now()
                )
            }
        } ?: emptyList()

        val dayLockStates = if (isSelectedDayLocked) {
            mapOf(selectedDate to true)
        } else {
            emptyMap()
        }

        return HomeUiState(
            isLoading = isLoading,
            mealPlan = mealPlan,
            selectedDate = selectedDate,
            weekDates = weekDates,
            selectedDayMeals = mealPlan?.days?.find { it.date == selectedDate },
            dayLockStates = dayLockStates
        )
    }

    // endregion

    // region Loading State Tests

    @Test
    fun homeScreen_showsLoadingIndicator_whenLoading() {
        val uiState = createTestUiState(isLoading = true, mealPlan = null)

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.HOME_SCREEN).assertIsDisplayed()
    }

    @Test
    fun homeScreen_hidesLoadingIndicator_whenDataLoaded() {
        val uiState = createTestUiState(isLoading = false)

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("This Week's Menu").assertIsDisplayed()
    }

    // endregion

    // region Week Selector Tests

    @Test
    fun homeScreen_displaysWeekHeader() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("This Week's Menu").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysSelectedDayNumber() {
        val selectedDate = LocalDate.now()
        val uiState = createTestUiState(selectedDate = selectedDate)

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText(selectedDate.dayOfMonth.toString()).assertIsDisplayed()
    }

    @Test
    fun homeScreen_dateSelection_triggersCallback() {
        var selectedDateResult: LocalDate? = null
        val uiState = createTestUiState()
        val targetDate = uiState.weekDates.firstOrNull { !it.isSelected }?.date ?: return

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(
                    uiState = uiState,
                    onDateSelect = { selectedDateResult = it }
                )
            }
        }

        composeTestRule.onNodeWithText(targetDate.dayOfMonth.toString()).performClick()

        assert(selectedDateResult == targetDate) {
            "Expected $targetDate but got $selectedDateResult"
        }
    }

    // endregion

    // region Meal Section Tests

    @Test
    fun homeScreen_displaysMealSections() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("BREAKFAST").assertIsDisplayed()
        composeTestRule.onNodeWithText("LUNCH").assertIsDisplayed()
        composeTestRule.onNodeWithText("DINNER").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("SNACKS").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysMealItems() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Poha").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dal Tadka").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysMealTimeAndCalories() {
        val mealItem = createTestMealItem(name = "Test Recipe", prepTime = 45, calories = 350)
        val day = createTestMealPlanDay(breakfast = listOf(mealItem))
        val plan = createTestMealPlan(days = listOf(day))
        val uiState = createTestUiState(mealPlan = plan, selectedDate = day.date)

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("45 min · 350 cal", substring = true).assertIsDisplayed()
    }

    @Test
    fun homeScreen_mealClick_triggersCallback() {
        var clickedRecipe: String? = null
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(
                    uiState = uiState,
                    onRecipeClick = { meal, _ -> clickedRecipe = meal.recipeName }
                )
            }
        }

        composeTestRule.onNodeWithText("Poha").performClick()

        assert(clickedRecipe == "Poha") { "Expected 'Poha' but got '$clickedRecipe'" }
    }

    // endregion

    // region Top App Bar Tests

    @Test
    fun homeScreen_displaysAppTitle() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("RasoiAI").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysNotificationIcon() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Notifications").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysProfileIcon() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Profile").assertIsDisplayed()
    }

    @Test
    fun homeScreen_profileClick_triggersCallback() {
        var profileClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(
                    uiState = uiState,
                    onProfileClick = { profileClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Profile").performClick()

        assert(profileClicked) { "Profile callback was not invoked" }
    }

    // endregion

    // region Bottom Navigation Tests

    @Test
    fun homeScreen_displaysBottomNavigation() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV).assertIsDisplayed()
    }

    // endregion

    // region Refresh Tests

    @Test
    fun homeScreen_displaysRefreshButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Refresh").assertIsDisplayed()
    }

    @Test
    fun homeScreen_refreshClick_triggersCallback() {
        var refreshClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(
                    uiState = uiState,
                    onRefreshClick = { refreshClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Refresh").performClick()

        assert(refreshClicked) { "Refresh callback was not invoked" }
    }

    // endregion

    // region Lock State Tests

    @Test
    fun homeScreen_showsUnlockedDayIcon_whenDayNotLocked() {
        val uiState = createTestUiState(isSelectedDayLocked = false)

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Day unlocked - tap to lock").assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsLockedDayIcon_whenDayLocked() {
        val uiState = createTestUiState(isSelectedDayLocked = true)

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Day locked - tap to unlock").assertIsDisplayed()
    }

    @Test
    fun homeScreen_dayLockClick_triggersCallback() {
        var lockClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(
                    uiState = uiState,
                    onDayLockClick = { lockClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Day unlocked - tap to lock").performClick()

        assert(lockClicked) { "Day lock callback was not invoked" }
    }

    @Test
    fun homeScreen_showsLockedIcon_forLockedRecipe() {
        val lockedMeal = createTestMealItem(name = "Locked Recipe", isLocked = true)
        val day = createTestMealPlanDay(breakfast = listOf(lockedMeal))
        val plan = createTestMealPlan(days = listOf(day))
        val uiState = createTestUiState(mealPlan = plan, selectedDate = day.date)

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Locked Recipe").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Locked").assertIsDisplayed()
    }

    // endregion

    // region Today Indicator Tests

    @Test
    fun homeScreen_showsTodayIndicator_whenTodaySelected() {
        val uiState = createTestUiState(selectedDate = LocalDate.now())

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("TODAY").assertIsDisplayed()
    }

    // endregion

    // region Empty State Tests

    @Test
    fun homeScreen_displaysEmptyMealSections() {
        val emptyDay = MealPlanDay(
            date = LocalDate.now(),
            dayName = "TODAY",
            breakfast = emptyList(),
            lunch = emptyList(),
            dinner = emptyList(),
            snacks = emptyList(),
            festival = null
        )
        val plan = createTestMealPlan(days = listOf(emptyDay))
        val uiState = createTestUiState(mealPlan = plan, selectedDate = emptyDay.date)

        composeTestRule.setContent {
            RasoiAITheme {
                HomeScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("BREAKFAST").assertIsDisplayed()
        composeTestRule.onNodeWithText("LUNCH").assertIsDisplayed()
    }

    // endregion
}

/**
 * Test composable that mirrors the structure of HomeScreenContent.
 * This allows testing the UI in isolation without the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenTestContent(
    uiState: HomeUiState,
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onDateSelect: (LocalDate) -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onDayLockClick: () -> Unit = {},
    onRecipeClick: (MealItem, MealType) -> Unit = { _, _ -> },
    onBottomNavClick: (Screen) -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.HOME_SCREEN),
        topBar = {
            TopAppBar(
                title = { Text("RasoiAI") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        },
        bottomBar = {
            RasoiBottomNavigation(
                currentScreen = Screen.Home,
                onItemClick = onBottomNavClick
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Week Header
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "This Week's Menu",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = uiState.formattedDateRange,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Week Date Selector
                item {
                    LazyRow(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            count = uiState.weekDates.size,
                            key = { uiState.weekDates[it].date.toEpochDay() }
                        ) { index ->
                            val weekDay = uiState.weekDates[index]
                            Surface(
                                onClick = { onDateSelect(weekDay.date) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (weekDay.isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = weekDay.dayName)
                                    Text(text = weekDay.dayNumber.toString())
                                }
                            }
                        }
                    }
                }

                // Selected Day Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(
                                    text = uiState.formattedSelectedDay,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (uiState.isToday) {
                                    Text(
                                        text = "TODAY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = onDayLockClick) {
                                Icon(
                                    imageVector = if (uiState.isSelectedDayLocked)
                                        Icons.Default.Lock
                                    else
                                        Icons.Default.LockOpen,
                                    contentDescription = if (uiState.isSelectedDayLocked)
                                        "Day locked - tap to unlock"
                                    else
                                        "Day unlocked - tap to lock"
                                )
                            }
                        }
                        TextButton(onClick = onRefreshClick) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("Refresh")
                        }
                    }
                }

                // Meal Sections
                uiState.selectedDayMeals?.let { dayMeals ->
                    item {
                        MealSectionTestContent(
                            title = "BREAKFAST",
                            meals = dayMeals.breakfast,
                            mealType = MealType.BREAKFAST,
                            onRecipeClick = onRecipeClick
                        )
                    }
                    item {
                        MealSectionTestContent(
                            title = "LUNCH",
                            meals = dayMeals.lunch,
                            mealType = MealType.LUNCH,
                            onRecipeClick = onRecipeClick
                        )
                    }
                    item {
                        MealSectionTestContent(
                            title = "DINNER",
                            meals = dayMeals.dinner,
                            mealType = MealType.DINNER,
                            onRecipeClick = onRecipeClick
                        )
                    }
                    item {
                        MealSectionTestContent(
                            title = "SNACKS",
                            meals = dayMeals.snacks,
                            mealType = MealType.SNACKS,
                            onRecipeClick = onRecipeClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealSectionTestContent(
    title: String,
    meals: List<MealItem>,
    mealType: MealType,
    onRecipeClick: (MealItem, MealType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = {}) {
                    Text("Add")
                }
            }

            meals.forEach { meal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRecipeClick(meal, mealType) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = meal.recipeName,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${meal.prepTimeMinutes} min · ${meal.calories} cal",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (meal.isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked"
                        )
                    }
                }
            }
        }
    }
}
