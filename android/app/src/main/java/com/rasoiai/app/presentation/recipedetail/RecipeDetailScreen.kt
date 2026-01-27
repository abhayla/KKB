package com.rasoiai.app.presentation.recipedetail

import android.content.res.Configuration
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

    // Stable no-op callback for unimplemented feature
    val onMoreClick = remember { { /* TODO: Show more options */ } }

    RecipeDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = viewModel::navigateBack,
        onFavoriteClick = viewModel::toggleFavorite,
        onMoreClick = onMoreClick,
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
private fun RecipeDetailContent(
    uiState: RecipeDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    onTabSelect: (Int) -> Unit,
    onServingsChange: (Int) -> Unit,
    onIngredientChecked: (String) -> Unit,
    onAddAllToGrocery: () -> Unit,
    onStartCookingMode: () -> Unit,
    onModifyWithAI: () -> Unit
) {
    Scaffold(
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
                    IconButton(onClick = onFavoriteClick) {
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
                    IconButton(onClick = onMoreClick) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
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
                            lockState = uiState.lockState
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
                                modifier = Modifier.padding(top = spacing.sm)
                            )
                            1 -> InstructionsTab(
                                instructions = uiState.recipe.instructions,
                                modifier = Modifier.padding(top = spacing.sm)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Spacer(modifier = Modifier.height(spacing.md))

                        // Bottom Buttons
                        Button(
                            onClick = onStartCookingMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.md),
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
