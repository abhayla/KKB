package com.rasoiai.app.presentation.recipedetail

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.rasoiai.app.presentation.common.TestTags
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.recipedetail.components.IngredientsTab
import com.rasoiai.app.presentation.recipedetail.components.InstructionsTab
import com.rasoiai.app.presentation.recipedetail.components.NutritionCard
import com.rasoiai.app.presentation.recipedetail.components.RecipeHeader
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing

@Composable
fun RecipeDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCookingMode: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is RecipeDetailNavigationEvent.NavigateToCookingMode -> {
                    onNavigateToCookingMode(event.recipeId)
                }
                is RecipeDetailNavigationEvent.NavigateToChat -> {
                    onNavigateToChat(event.recipeContext)
                }
                RecipeDetailNavigationEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    // Show error/success messages in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    RecipeDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = viewModel::navigateBack,
        onFavoriteClick = viewModel::toggleFavorite,
        onTabSelect = viewModel::selectTab,
        onServingsChange = viewModel::updateServings,
        onIngredientChecked = viewModel::toggleIngredientChecked,
        onAddAllToGrocery = viewModel::addAllToGroceryList,
        onStartCookingMode = viewModel::startCookingMode,
        onModifyWithAI = viewModel::modifyWithAI
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecipeDetailContent(
    uiState: RecipeDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onTabSelect: (Int) -> Unit,
    onServingsChange: (Int) -> Unit,
    onIngredientChecked: (String) -> Unit,
    onAddAllToGrocery: () -> Unit,
    onStartCookingMode: () -> Unit,
    onModifyWithAI: () -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.RECIPE_DETAIL_SCREEN),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.testTag(TestTags.RECIPE_FAVORITE_BUTTON)
                    ) {
                        Icon(
                            imageVector = if (uiState.recipe?.isFavorite == true) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = if (uiState.recipe?.isFavorite == true) {
                                "Remove from favorites"
                            } else {
                                "Add to favorites"
                            },
                            tint = if (uiState.recipe?.isFavorite == true) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        val context = LocalContext.current
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    uiState.recipe?.let { recipe ->
                                        val shareText = buildString {
                                            append("Check out ${recipe.name} on RasoiAI!")
                                            append("\n${recipe.description}")
                                            append("\nCuisine: ${recipe.cuisineType}")
                                            append("\nTime: ${recipe.prepTimeMinutes + recipe.cookTimeMinutes} min")
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share recipe"))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Collection") },
                                onClick = { showMenu = false }
                                // TODO(#25 follow-up): launch collection picker and
                                // call favoritesRepository.addToCollection(...).
                            )
                            DropdownMenuItem(
                                text = { Text("Report Issue") },
                                onClick = {
                                    showMenu = false
                                    uiState.recipe?.let { recipe ->
                                        val subject = "RasoiAI: Issue with recipe \"${recipe.name}\""
                                        val body = buildString {
                                            append("Recipe ID: ${recipe.id}\n")
                                            append("Recipe Name: ${recipe.name}\n")
                                            append("Cuisine: ${recipe.cuisineType}\n\n")
                                            append("Describe the issue below:\n\n")
                                        }
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:support@rasoiai.com")
                                            putExtra(Intent.EXTRA_SUBJECT, subject)
                                            putExtra(Intent.EXTRA_TEXT, body)
                                        }
                                        runCatching {
                                            context.startActivity(
                                                Intent.createChooser(intent, "Report issue via")
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.recipe == null -> {
                    Text(
                        text = uiState.errorMessage ?: "Recipe not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Recipe Header
                        RecipeHeader(
                            name = uiState.recipe.name,
                            imageUrl = uiState.recipe.imageUrl,
                            cuisineText = uiState.cuisineDisplayText,
                            totalTimeMinutes = uiState.totalTimeMinutes,
                            servings = uiState.selectedServings,
                            calories = uiState.scaledNutrition?.calories,
                            isVegetarian = uiState.isVegetarian,
                            tags = uiState.displayTags,
                            lockState = uiState.lockState,
                            averageRating = uiState.recipe.averageRating,
                            ratingCount = uiState.recipe.ratingCount,
                            userRating = uiState.recipe.userRating
                        )

                        Spacer(modifier = Modifier.height(spacing.md))

                        // Nutrition Card
                        NutritionCard(
                            nutrition = uiState.scaledNutrition,
                            servings = uiState.selectedServings,
                            modifier = Modifier.padding(horizontal = spacing.md)
                        )

                        Spacer(modifier = Modifier.height(spacing.md))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Stable tab click handlers to avoid recomposition
                        val currentOnTabSelect by rememberUpdatedState(onTabSelect)
                        val onSelectIngredientsTab = remember { { currentOnTabSelect(0) } }
                        val onSelectInstructionsTab = remember { { currentOnTabSelect(1) } }

                        // Tabs
                        TabRow(
                            selectedTabIndex = uiState.selectedTabIndex,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(
                                selected = uiState.selectedTabIndex == 0,
                                onClick = onSelectIngredientsTab,
                                text = {
                                    Text(
                                        text = "INGREDIENTS",
                                        fontWeight = if (uiState.selectedTabIndex == 0) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                }
                            )
                            Tab(
                                selected = uiState.selectedTabIndex == 1,
                                onClick = onSelectInstructionsTab,
                                text = {
                                    Text(
                                        text = "INSTRUCTIONS",
                                        fontWeight = if (uiState.selectedTabIndex == 1) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                }
                            )
                        }

                        // Tab Content
                        when (uiState.selectedTabIndex) {
                            0 -> IngredientsTab(
                                ingredients = uiState.scaledIngredients,
                                selectedServings = uiState.selectedServings,
                                checkedIngredients = uiState.checkedIngredients,
                                onServingsChange = onServingsChange,
                                onIngredientChecked = onIngredientChecked,
                                onAddAllToGrocery = onAddAllToGrocery,
                                modifier = Modifier
                                    .padding(top = spacing.sm)
                                    .testTag(TestTags.RECIPE_INGREDIENTS_LIST)
                            )
                            1 -> InstructionsTab(
                                instructions = uiState.recipe.instructions,
                                modifier = Modifier
                                    .padding(top = spacing.sm)
                                    .testTag(TestTags.RECIPE_INSTRUCTIONS_LIST)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Spacer(modifier = Modifier.height(spacing.md))

                        // Bottom Buttons
                        Button(
                            onClick = onStartCookingMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.md)
                                .testTag(TestTags.RECIPE_START_COOKING_BUTTON),
                            shape = RoundedCornerShape(spacing.sm)
                        ) {
                            Text(
                                text = "\uD83C\uDF73 START COOKING MODE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.sm))

                        OutlinedButton(
                            onClick = onModifyWithAI,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.md),
                            shape = RoundedCornerShape(spacing.sm)
                        ) {
                            Text(
                                text = "\uD83D\uDCAC Modify with AI",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.xl))
                    }
                }
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
private fun RecipeDetailScreenPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            // Preview would need mock data
        }
    }
}
