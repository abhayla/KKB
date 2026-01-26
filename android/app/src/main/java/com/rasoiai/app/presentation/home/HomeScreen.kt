package com.rasoiai.app.presentation.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    onNavigateToRecipeDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToGrocery: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToStats: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigationEvent by viewModel.navigationEvent.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            is HomeNavigationEvent.NavigateToRecipeDetail -> {
                onNavigateToRecipeDetail(event.recipeId)
                viewModel.onNavigationHandled()
            }
            HomeNavigationEvent.NavigateToSettings -> {
                onNavigateToSettings()
                viewModel.onNavigationHandled()
            }
            HomeNavigationEvent.NavigateToGrocery -> {
                onNavigateToGrocery()
                viewModel.onNavigationHandled()
            }
            HomeNavigationEvent.NavigateToChat -> {
                onNavigateToChat()
                viewModel.onNavigationHandled()
            }
            HomeNavigationEvent.NavigateToFavorites -> {
                onNavigateToFavorites()
                viewModel.onNavigationHandled()
            }
            HomeNavigationEvent.NavigateToStats -> {
                onNavigateToStats()
                viewModel.onNavigationHandled()
            }
            HomeNavigationEvent.NavigateToNotifications -> {
                // TODO: Navigate to notifications
                viewModel.onNavigationHandled()
            }
            null -> { /* No navigation */ }
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
        onRecipeClick = viewModel::onRecipeClick,
        onDismissRecipeSheet = viewModel::dismissRecipeActionSheet,
        onViewRecipe = viewModel::viewRecipe,
        onSwapRecipe = viewModel::showSwapOptions,
        onToggleLock = viewModel::toggleLockRecipe,
        onDismissRefreshSheet = viewModel::dismissRefreshSheet,
        onRegenerateDay = viewModel::regenerateDay,
        onRegenerateWeek = viewModel::regenerateWeek,
        onDismissSwapSheet = viewModel::dismissSwapSheet,
        onConfirmSwap = { viewModel.swapRecipe() },
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
    onRecipeClick: (MealItem, MealType) -> Unit,
    onDismissRecipeSheet: () -> Unit,
    onViewRecipe: () -> Unit,
    onSwapRecipe: () -> Unit,
    onToggleLock: () -> Unit,
    onDismissRefreshSheet: () -> Unit,
    onRegenerateDay: () -> Unit,
    onRegenerateWeek: () -> Unit,
    onDismissSwapSheet: () -> Unit,
    onConfirmSwap: () -> Unit,
    onBottomNavItemClick: (Screen) -> Unit
) {
    Scaffold(
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
                    IconButton(onClick = onProfileClick) {
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
                            onDateSelect = onDateSelect
                        )
                    }

                    // Selected Day Header
                    item {
                        SelectedDayHeader(
                            dayText = uiState.formattedSelectedDay,
                            isToday = uiState.isToday,
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
                                onRecipeClick = onRecipeClick,
                                onAddClick = { /* TODO: Add recipe */ }
                            )
                        }

                        item {
                            MealSection(
                                title = "Lunch",
                                icon = "\u2600\uFE0F", // Sun emoji
                                meals = dayMeals.lunch,
                                mealType = MealType.LUNCH,
                                onRecipeClick = onRecipeClick,
                                onAddClick = { /* TODO: Add recipe */ }
                            )
                        }

                        item {
                            MealSection(
                                title = "Dinner",
                                icon = "\uD83C\uDF19", // Moon emoji
                                meals = dayMeals.dinner,
                                mealType = MealType.DINNER,
                                onRecipeClick = onRecipeClick,
                                onAddClick = { /* TODO: Add recipe */ }
                            )
                        }

                        item {
                            MealSection(
                                title = "Snacks",
                                icon = "\uD83C\uDF6A", // Cookie emoji
                                meals = dayMeals.snacks,
                                mealType = MealType.SNACKS,
                                onRecipeClick = onRecipeClick,
                                onAddClick = { /* TODO: Add recipe */ }
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
    if (uiState.showRecipeActionSheet && uiState.selectedMealItem != null) {
        RecipeActionSheet(
            mealItem = uiState.selectedMealItem,
            onDismiss = onDismissRecipeSheet,
            onViewRecipe = onViewRecipe,
            onSwapRecipe = onSwapRecipe,
            onToggleLock = onToggleLock
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
            onDismiss = onDismissSwapSheet,
            onConfirmSwap = onConfirmSwap
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
    onDateSelect: (LocalDate) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        contentPadding = PaddingValues(horizontal = spacing.md)
    ) {
        items(weekDates) { weekDay ->
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

    Column(
        modifier = Modifier
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
    onRefreshClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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

@Composable
private fun MealSection(
    title: String,
    icon: String,
    meals: List<MealItem>,
    mealType: MealType,
    onRecipeClick: (MealItem, MealType) -> Unit,
    onAddClick: () -> Unit
) {
    val totalTime = meals.sumOf { it.prepTimeMinutes }
    val totalCalories = meals.sumOf { it.calories }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(spacing.md)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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

            // Meal items
            meals.forEach { meal ->
                MealItemRow(
                    mealItem = meal,
                    onClick = { onRecipeClick(meal, mealType) }
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

@Composable
private fun MealItemRow(
    mealItem: MealItem,
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

        Spacer(modifier = Modifier.width(spacing.md))

        // Lock icon
        if (mealItem.isLocked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Swap icon
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Swap",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeActionSheet(
    mealItem: MealItem,
    onDismiss: () -> Unit,
    onViewRecipe: () -> Unit,
    onSwapRecipe: () -> Unit,
    onToggleLock: () -> Unit
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
                text = mealItem.recipeName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = spacing.md)
            )

            ActionSheetItem(
                icon = Icons.Default.Visibility,
                text = "View Recipe",
                onClick = onViewRecipe
            )

            ActionSheetItem(
                icon = Icons.Default.SwapHoriz,
                text = "Swap Recipe",
                subtitle = "Replace with similar",
                onClick = onSwapRecipe
            )

            ActionSheetItem(
                icon = if (mealItem.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                text = if (mealItem.isLocked) "Unlock Recipe" else "Lock Recipe",
                subtitle = if (mealItem.isLocked) "Allow regenerate" else "Protect from regenerate",
                onClick = onToggleLock
            )

            ActionSheetItem(
                icon = Icons.Default.Remove,
                text = "Remove from Meal",
                textColor = MaterialTheme.colorScheme.error,
                onClick = onDismiss // TODO: Implement remove
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    onDismiss: () -> Unit,
    onConfirmSwap: () -> Unit
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
                text = "Swap $recipeName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = spacing.md)
            )

            Text(
                text = "Replace with a similar recipe?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(spacing.sm)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onConfirmSwap,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(spacing.sm)
                ) {
                    Text("Swap")
                }
            }

            Spacer(modifier = Modifier.height(spacing.xl))
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
