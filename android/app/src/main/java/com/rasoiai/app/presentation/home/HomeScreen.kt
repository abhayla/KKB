package com.rasoiai.app.presentation.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.home.components.RasoiBottomNavigation
import com.rasoiai.app.presentation.navigation.Screen
import com.rasoiai.app.presentation.theme.DietaryColors
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.MealItem
import com.rasoiai.domain.model.MealPlanDay
import com.rasoiai.domain.model.MealType
import java.time.LocalDate

@Composable
fun HomeScreen(
    onNavigateToRecipeDetail: (recipeId: String, isLocked: Boolean, fromMealPlan: Boolean) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToGrocery: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToStats: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is HomeNavigationEvent.NavigateToRecipeDetail -> {
                    onNavigateToRecipeDetail(event.recipeId, event.isLocked, event.fromMealPlan)
                }
                HomeNavigationEvent.NavigateToSettings -> onNavigateToSettings()
                HomeNavigationEvent.NavigateToGrocery -> onNavigateToGrocery()
                HomeNavigationEvent.NavigateToChat -> onNavigateToChat()
                HomeNavigationEvent.NavigateToFavorites -> onNavigateToFavorites()
                HomeNavigationEvent.NavigateToStats -> onNavigateToStats()
                HomeNavigationEvent.NavigateToNotifications -> { /* TODO: Navigate to notifications */ }
            }
        }
    }

    // Show error in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    HomeScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onMenuClick = { /* TODO: Open drawer */ },
        onNotificationsClick = viewModel::navigateToNotifications,
        onProfileClick = viewModel::navigateToSettings,
        onDateSelect = viewModel::selectDate,
        onRefreshClick = viewModel::showRefreshOptions,
        onDayLockClick = viewModel::toggleDayLock,
        onMealLockClick = viewModel::toggleMealLock,
        onRecipeClick = viewModel::onRecipeClick,
        onDismissRecipeSheet = viewModel::dismissRecipeActionSheet,
        onViewRecipe = viewModel::viewRecipe,
        onSwapRecipe = viewModel::showSwapOptions,
        onToggleLock = viewModel::toggleLockRecipe,
        onRemoveRecipe = viewModel::removeRecipeFromMeal,
        onDismissRefreshSheet = viewModel::dismissRefreshSheet,
        onRegenerateDay = viewModel::regenerateDay,
        onRegenerateWeek = viewModel::regenerateWeek,
        onDismissSwapSheet = viewModel::dismissSwapSheet,
        onConfirmSwap = { viewModel.swapRecipe() },
        onToggleRecipeLockDirect = viewModel::toggleRecipeLockDirect,
        onRemoveRecipeDirect = viewModel::removeRecipeFromMealDirect,
        onBottomNavItemClick = { screen ->
            when (screen) {
                Screen.Home -> { /* Already on Home */ }
                Screen.Grocery -> viewModel.navigateToGrocery()
                Screen.Chat -> viewModel.navigateToChat()
                Screen.Favorites -> viewModel.navigateToFavorites()
                Screen.Stats -> viewModel.navigateToStats()
                else -> { }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onMenuClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onRefreshClick: () -> Unit,
    onDayLockClick: () -> Unit,
    onMealLockClick: (MealType) -> Unit,
    onRecipeClick: (MealItem, MealType) -> Unit,
    onDismissRecipeSheet: () -> Unit,
    onViewRecipe: () -> Unit,
    onSwapRecipe: () -> Unit,
    onToggleLock: () -> Unit,
    onRemoveRecipe: () -> Unit,
    onDismissRefreshSheet: () -> Unit,
    onRegenerateDay: () -> Unit,
    onRegenerateWeek: () -> Unit,
    onDismissSwapSheet: () -> Unit,
    onConfirmSwap: () -> Unit,
    onToggleRecipeLockDirect: (MealItem, MealType) -> Unit,
    onRemoveRecipeDirect: (MealItem, MealType) -> Unit,
    onBottomNavItemClick: (Screen) -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.HOME_SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RasoiAI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier.testTag(TestTags.HOME_PROFILE_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            RasoiBottomNavigation(
                currentScreen = Screen.Home,
                onItemClick = onBottomNavItemClick
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = spacing.md)
                ) {
                    // Festival Banner
                    uiState.upcomingFestival?.let { festival ->
                        item {
                            FestivalBanner(
                                festival = festival,
                                onClick = { /* TODO: Navigate to festival recipes */ }
                            )
                        }
                    }

                    // Week Header
                    item {
                        WeekHeader(
                            dateRange = uiState.formattedDateRange
                        )
                    }

                    // Week Date Selector
                    item {
                        WeekDateSelector(
                            weekDates = uiState.weekDates,
                            onDateSelect = onDateSelect,
                            modifier = Modifier.testTag(TestTags.HOME_WEEK_SELECTOR)
                        )
                    }

                    // Selected Day Header
                    item {
                        SelectedDayHeader(
                            dayText = uiState.formattedSelectedDay,
                            isToday = uiState.isToday,
                            isDayLocked = uiState.isSelectedDayLocked,
                            onLockClick = onDayLockClick,
                            onRefreshClick = onRefreshClick
                        )
                    }

                    // Meal Sections
                    uiState.selectedDayMeals?.let { dayMeals ->
                        item {
                            MealSection(
                                title = "Breakfast",
                                icon = "\uD83C\uDF05", // Sunrise emoji
                                meals = dayMeals.breakfast,
                                mealType = MealType.BREAKFAST,
                                isMealLocked = uiState.isMealLocked(MealType.BREAKFAST),
                                isDayLocked = uiState.isSelectedDayLocked,
                                onRecipeClick = onRecipeClick,
                                onLockClick = { onMealLockClick(MealType.BREAKFAST) },
                                onAddClick = { /* TODO: Add recipe */ },
                                onToggleRecipeLockDirect = onToggleRecipeLockDirect,
                                onRemoveRecipeDirect = onRemoveRecipeDirect
                            )
                        }

                        item {
                            MealSection(
                                title = "Lunch",
                                icon = "\u2600\uFE0F", // Sun emoji
                                meals = dayMeals.lunch,
                                mealType = MealType.LUNCH,
                                isMealLocked = uiState.isMealLocked(MealType.LUNCH),
                                isDayLocked = uiState.isSelectedDayLocked,
                                onRecipeClick = onRecipeClick,
                                onLockClick = { onMealLockClick(MealType.LUNCH) },
                                onAddClick = { /* TODO: Add recipe */ },
                                onToggleRecipeLockDirect = onToggleRecipeLockDirect,
                                onRemoveRecipeDirect = onRemoveRecipeDirect
                            )
                        }

                        item {
                            MealSection(
                                title = "Snacks",
                                icon = "\uD83C\uDF6A", // Cookie emoji
                                meals = dayMeals.snacks,
                                mealType = MealType.SNACKS,
                                isMealLocked = uiState.isMealLocked(MealType.SNACKS),
                                isDayLocked = uiState.isSelectedDayLocked,
                                onRecipeClick = onRecipeClick,
                                onLockClick = { onMealLockClick(MealType.SNACKS) },
                                onAddClick = { /* TODO: Add recipe */ },
                                onToggleRecipeLockDirect = onToggleRecipeLockDirect,
                                onRemoveRecipeDirect = onRemoveRecipeDirect
                            )
                        }

                        item {
                            MealSection(
                                title = "Dinner",
                                icon = "\uD83C\uDF19", // Moon emoji
                                meals = dayMeals.dinner,
                                mealType = MealType.DINNER,
                                isMealLocked = uiState.isMealLocked(MealType.DINNER),
                                isDayLocked = uiState.isSelectedDayLocked,
                                onRecipeClick = onRecipeClick,
                                onLockClick = { onMealLockClick(MealType.DINNER) },
                                onAddClick = { /* TODO: Add recipe */ },
                                onToggleRecipeLockDirect = onToggleRecipeLockDirect,
                                onRemoveRecipeDirect = onRemoveRecipeDirect
                            )
                        }
                    }
                }
            }

            // Loading overlay when refreshing
            AnimatedVisibility(
                visible = uiState.isRefreshing,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularProgressIndicator()
            }
        }
    }

    // Recipe Action Sheet
    if (uiState.showRecipeActionSheet && uiState.selectedMealItem != null && uiState.selectedMealType != null) {
        val isEffectivelyLocked = uiState.isRecipeEffectivelyLocked(
            uiState.selectedMealItem,
            uiState.selectedMealType
        )
        RecipeActionSheet(
            mealItem = uiState.selectedMealItem,
            isEffectivelyLocked = isEffectivelyLocked,
            isDayLocked = uiState.isSelectedDayLocked,
            isMealLocked = uiState.isMealLocked(uiState.selectedMealType),
            onDismiss = onDismissRecipeSheet,
            onViewRecipe = onViewRecipe,
            onSwapRecipe = onSwapRecipe,
            onToggleLock = onToggleLock,
            onRemove = onRemoveRecipe
        )
    }

    // Refresh Options Sheet
    if (uiState.showRefreshSheet) {
        RefreshOptionsSheet(
            selectedDay = uiState.formattedSelectedDay,
            dateRange = uiState.formattedDateRange,
            onDismiss = onDismissRefreshSheet,
            onRegenerateDay = onRegenerateDay,
            onRegenerateWeek = onRegenerateWeek
        )
    }

    // Swap Recipe Sheet
    if (uiState.showSwapSheet) {
        SwapRecipeSheet(
            recipeName = uiState.selectedMealItem?.recipeName ?: "",
            swapSuggestions = uiState.swapSuggestions,
            onDismiss = onDismissSwapSheet,
            onSelectRecipe = { selectedRecipe -> onConfirmSwap() }
        )
    }
}

@Composable
private fun FestivalBanner(
    festival: FestivalInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(spacing.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\uD83C\uDF89", // Party emoji
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${festival.name} in ${festival.daysUntil} ${if (festival.daysUntil == 1) "day" else "days"}!",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "View festive recipes \u2192",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun WeekHeader(dateRange: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm)
    ) {
        Text(
            text = "This Week's Menu",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = dateRange,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeekDateSelector(
    weekDates: List<WeekDay>,
    onDateSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        contentPadding = PaddingValues(horizontal = spacing.md)
    ) {
        items(
            items = weekDates,
            key = { it.date.toEpochDay() }
        ) { weekDay ->
            DateItem(
                weekDay = weekDay,
                onClick = { onDateSelect(weekDay.date) }
            )
        }
    }
}

@Composable
private fun DateItem(
    weekDay: WeekDay,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        weekDay.isSelected -> MaterialTheme.colorScheme.primary
        weekDay.isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        weekDay.isSelected -> MaterialTheme.colorScheme.onPrimary
        weekDay.isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Get day name for testTag (e.g., "monday", "tuesday")
    val dayTag = weekDay.date.dayOfWeek.name.lowercase()

    Column(
        modifier = Modifier
            .testTag("${TestTags.HOME_DAY_TAB_PREFIX}$dayTag")
            .clip(RoundedCornerShape(spacing.sm))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = weekDay.dayName,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.7f)
        )
        Text(
            text = weekDay.dayNumber.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        if (weekDay.isToday && !weekDay.isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        } else if (weekDay.isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun SelectedDayHeader(
    dayText: String,
    isToday: Boolean,
    isDayLocked: Boolean,
    onLockClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = dayText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (isToday) {
                    Text(
                        text = "TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(spacing.sm))
            // Day Lock Button - icon shows current state (🔒 locked, 🔓 unlocked)
            IconButton(
                onClick = onLockClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isDayLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isDayLocked) "Day locked - tap to unlock" else "Day unlocked - tap to lock",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        TextButton(
            onClick = onRefreshClick,
            contentPadding = PaddingValues(horizontal = spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(spacing.xs))
            Text("Refresh")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealSection(
    title: String,
    icon: String,
    meals: List<MealItem>,
    mealType: MealType,
    isMealLocked: Boolean,
    isDayLocked: Boolean,
    onRecipeClick: (MealItem, MealType) -> Unit,
    onLockClick: () -> Unit,
    onAddClick: () -> Unit,
    onToggleRecipeLockDirect: (MealItem, MealType) -> Unit,
    onRemoveRecipeDirect: (MealItem, MealType) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalTime = meals.sumOf { it.prepTimeMinutes }
    val totalCalories = meals.sumOf { it.calories }
    // Meal is effectively locked if day is locked OR meal itself is locked
    val isEffectivelyLocked = isDayLocked || isMealLocked

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .testTag("${TestTags.MEAL_CARD_PREFIX}${mealType.name.lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(spacing.md)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header - long-press to toggle meal lock (only when day is NOT locked)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { /* no-op */ },
                        onLongClick = { if (!isDayLocked) onLockClick() }
                    )
                    .padding(spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Meal Lock Button - icon shows current state (🔒 locked, 🔓 unlocked)
                    // Disabled if day is already locked (inherits day lock)
                    IconButton(
                        onClick = onLockClick,
                        modifier = Modifier.size(32.dp),
                        enabled = !isDayLocked
                    ) {
                        Icon(
                            imageVector = if (isEffectivelyLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = when {
                                isDayLocked -> "Meal locked by day lock"
                                isMealLocked -> "Meal locked - tap to unlock"
                                else -> "Meal unlocked - tap to lock"
                            },
                            modifier = Modifier.size(16.dp),
                            tint = if (isDayLocked) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    TextButton(
                        onClick = onAddClick,
                        contentPadding = PaddingValues(horizontal = spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.xxs))
                        Text("Add", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Meal items with swipe actions
            meals.forEach { meal ->
                val itemIsLocked = isEffectivelyLocked || meal.isLocked
                SwipeableMealItemRow(
                    mealItem = meal,
                    isEffectivelyLocked = itemIsLocked,
                    onRecipeClick = { onRecipeClick(meal, mealType) },
                    onLockToggle = { onToggleRecipeLockDirect(meal, mealType) },
                    onDelete = if (!itemIsLocked) {
                        { onRemoveRecipeDirect(meal, mealType) }
                    } else null
                )
            }

            // Total
            if (meals.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Total: ${if (totalTime > 0) "$totalTime min" else "--"} · ${if (totalCalories > 0) "$totalCalories cal" else "--"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableMealItemRow(
    mealItem: MealItem,
    isEffectivelyLocked: Boolean,
    onRecipeClick: () -> Unit,
    onLockToggle: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            // Don't auto-dismiss, just reveal actions
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            SwipeActionBackground(
                isLocked = isEffectivelyLocked,
                onLockToggle = onLockToggle,
                onDelete = onDelete
            )
        },
        content = {
            MealItemRow(
                mealItem = mealItem,
                isEffectivelyLocked = isEffectivelyLocked,
                onClick = onRecipeClick
            )
        }
    )
}

@Composable
private fun SwipeActionBackground(
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = spacing.md),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lock/Unlock button
        IconButton(onClick = onLockToggle) {
            Icon(
                imageVector = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = if (isLocked) "Unlock" else "Lock",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        // Delete button (only when unlocked)
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MealItemRow(
    mealItem: MealItem,
    isEffectivelyLocked: Boolean,
    onClick: () -> Unit
) {
    val dietaryColor = when {
        mealItem.dietaryTags.contains(DietaryTag.VEGETARIAN) -> DietaryColors.Vegetarian
        mealItem.dietaryTags.contains(DietaryTag.VEGAN) -> DietaryColors.Vegan
        mealItem.dietaryTags.contains(DietaryTag.NON_VEGETARIAN) -> DietaryColors.NonVegetarian
        else -> DietaryColors.Vegetarian
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dietary indicator
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dietaryColor)
        )

        Spacer(modifier = Modifier.width(spacing.sm))

        // Recipe name
        Text(
            text = mealItem.recipeName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Time and Calories
        Text(
            text = buildString {
                if (mealItem.prepTimeMinutes > 0) {
                    append("${mealItem.prepTimeMinutes} min")
                } else {
                    append("--")
                }
                append(" · ")
                if (mealItem.calories > 0) {
                    append("${mealItem.calories} cal")
                } else {
                    append("--")
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(spacing.sm))

        // Show lock icon when locked (replaces swap icon per wireframe)
        // When locked: show lock icon
        // When unlocked: show swap icon
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(32.dp)
        ) {
            if (isEffectivelyLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeActionSheet(
    mealItem: MealItem,
    isEffectivelyLocked: Boolean,
    isDayLocked: Boolean,
    isMealLocked: Boolean,
    onDismiss: () -> Unit,
    onViewRecipe: () -> Unit,
    onSwapRecipe: () -> Unit,
    onToggleLock: () -> Unit,
    onRemove: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    // Determine lock context for display
    val lockContextText = when {
        isDayLocked -> "Day is locked"
        isMealLocked -> "Meal is locked"
        mealItem.isLocked -> "Recipe is locked"
        else -> null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            Row(
                modifier = Modifier.padding(bottom = spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mealItem.recipeName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (isEffectivelyLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Show lock context if applicable
            if (lockContextText != null && isEffectivelyLocked) {
                Text(
                    text = lockContextText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = spacing.sm)
                )
            }

            // View Recipe - always enabled
            ActionSheetItem(
                icon = Icons.Default.Visibility,
                text = "View Recipe",
                onClick = onViewRecipe
            )

            // Swap Recipe - disabled when locked
            ActionSheetItem(
                icon = Icons.Default.SwapHoriz,
                text = "Swap Recipe",
                subtitle = if (isEffectivelyLocked) "Unlock to swap" else "Replace with similar",
                enabled = !isEffectivelyLocked,
                onClick = onSwapRecipe
            )

            // Lock/Unlock Recipe - shows different options based on lock level
            ActionSheetItem(
                icon = if (mealItem.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                text = if (mealItem.isLocked) "Unlock Recipe" else "Lock Recipe",
                subtitle = when {
                    isDayLocked -> "Unlock day first"
                    isMealLocked -> "Unlock meal first"
                    mealItem.isLocked -> "Allow regenerate"
                    else -> "Protect from regenerate"
                },
                enabled = !isDayLocked && !isMealLocked,
                onClick = onToggleLock
            )

            // Remove from Meal - disabled when locked
            ActionSheetItem(
                icon = Icons.Default.Remove,
                text = "Remove from Meal",
                subtitle = if (isEffectivelyLocked) "Unlock to remove" else null,
                textColor = if (isEffectivelyLocked)
                    MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                else
                    MaterialTheme.colorScheme.error,
                enabled = !isEffectivelyLocked,
                onClick = onRemove
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(spacing.sm)
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(spacing.xl))
        }
    }
}

@Composable
private fun ActionSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    subtitle: String? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.38f
    val effectiveTextColor = textColor.copy(alpha = if (enabled) textColor.alpha else 0.38f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = effectiveTextColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = effectiveTextColor
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshOptionsSheet(
    selectedDay: String,
    dateRange: String,
    onDismiss: () -> Unit,
    onRegenerateDay: () -> Unit,
    onRegenerateWeek: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            Text(
                text = "Regenerate Meals",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = spacing.md)
            )

            ActionSheetItem(
                icon = Icons.Outlined.CalendarToday,
                text = "This Day Only",
                subtitle = "Regenerate $selectedDay",
                onClick = onRegenerateDay
            )

            ActionSheetItem(
                icon = Icons.Outlined.CalendarMonth,
                text = "Entire Week",
                subtitle = "Regenerate $dateRange",
                onClick = onRegenerateWeek
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = "Note: Locked recipes (\uD83D\uDD12) will not be changed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = spacing.sm)
            )

            Spacer(modifier = Modifier.height(spacing.md))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(spacing.sm)
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(spacing.xl))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwapRecipeSheet(
    recipeName: String,
    swapSuggestions: List<MealItem>,
    onDismiss: () -> Unit,
    onSelectRecipe: (MealItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Filter suggestions based on search query
    val filteredSuggestions = remember(searchQuery, swapSuggestions) {
        if (searchQuery.isBlank()) {
            swapSuggestions
        } else {
            swapSuggestions.filter {
                it.recipeName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md)
        ) {
            Text(
                text = "Swap $recipeName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = spacing.sm)
            )

            Text(
                text = "Select a similar recipe to replace",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = spacing.md)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search recipes...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(spacing.sm)
            )

            Spacer(modifier = Modifier.height(spacing.md))

            // Section title
            Text(
                text = "Similar Recipes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = spacing.sm)
            )

            // Recipe Grid (2 columns)
            if (filteredSuggestions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No recipes match your search" else "No similar recipes available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(vertical = spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp) // Fixed height for the grid
                ) {
                    items(
                        items = filteredSuggestions,
                        key = { it.recipeId }
                    ) { suggestion ->
                        SwapRecipeGridItem(
                            mealItem = suggestion,
                            onClick = { onSelectRecipe(suggestion) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Cancel Button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(spacing.sm)
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(spacing.xl))
        }
    }
}

/**
 * Grid item for swap recipe selection using MealItem data
 */
@Composable
private fun SwapRecipeGridItem(
    mealItem: MealItem,
    onClick: () -> Unit
) {
    val isVegetarian = mealItem.dietaryTags.contains(DietaryTag.VEGETARIAN) ||
                      mealItem.dietaryTags.contains(DietaryTag.VEGAN)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(spacing.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(topStart = spacing.md, topEnd = spacing.md))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mealItem.recipeName.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Content
            Column(
                modifier = Modifier.padding(spacing.sm)
            ) {
                // Name with dietary indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Veg/Non-veg indicator
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isVegetarian) DietaryColors.Vegetarian else DietaryColors.NonVegetarian
                            )
                    )

                    Spacer(modifier = Modifier.width(spacing.xs))

                    Text(
                        text = mealItem.recipeName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(spacing.xs))

                // Time and calories
                Text(
                    text = "${mealItem.prepTimeMinutes}m${if (mealItem.calories > 0) " \u00B7 ${mealItem.calories}cal" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDFAF4)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1C1B1F
)
@Composable
private fun HomeScreenPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            // Preview would need mock data
        }
    }
}
