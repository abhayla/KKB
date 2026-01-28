package com.rasoiai.app.presentation.favorites

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.rasoiai.app.presentation.common.TestTags
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.favorites.components.AddCollectionCard
import com.rasoiai.app.presentation.favorites.components.AddToCollectionDialog
import com.rasoiai.app.presentation.favorites.components.CollectionCard
import com.rasoiai.app.presentation.favorites.components.CreateCollectionDialog
import com.rasoiai.app.presentation.favorites.components.FavoritesFilterChips
import com.rasoiai.app.presentation.favorites.components.RecipeGridItem
import com.rasoiai.app.presentation.home.components.RasoiBottomNavigation
import com.rasoiai.app.presentation.navigation.Screen
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.FavoriteCollection

@Composable
fun FavoritesScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToGrocery: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToRecipeDetail: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                FavoritesNavigationEvent.NavigateBack -> onNavigateToHome()
                FavoritesNavigationEvent.NavigateToHome -> onNavigateToHome()
                FavoritesNavigationEvent.NavigateToGrocery -> onNavigateToGrocery()
                FavoritesNavigationEvent.NavigateToChat -> onNavigateToChat()
                FavoritesNavigationEvent.NavigateToStats -> onNavigateToStats()
                is FavoritesNavigationEvent.NavigateToRecipeDetail -> {
                    onNavigateToRecipeDetail(event.recipeId)
                }
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

    FavoritesScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onSearchClick = viewModel::toggleSearchBar,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onCollectionClick = viewModel::selectCollection,
        onCreateCollectionClick = viewModel::showCreateCollectionDialog,
        onRecipeClick = viewModel::onRecipeClick,
        onRemoveRecipeClick = viewModel::removeFromCollection,
        onAddToCollectionClick = viewModel::addToCollection,
        onCuisineFilterChange = viewModel::setCuisineFilter,
        onTimeFilterChange = viewModel::setTimeFilter,
        onReorderClick = {
            if (uiState.isReorderMode) {
                viewModel.saveReorderState()
            } else {
                viewModel.enterReorderMode()
            }
        },
        onMoveRecipe = viewModel::onReorderRecipes,
        onBottomNavItemClick = { screen ->
            when (screen) {
                Screen.Home -> viewModel.navigateToHome()
                Screen.Grocery -> viewModel.navigateToGrocery()
                Screen.Chat -> viewModel.navigateToChat()
                Screen.Favorites -> { /* Already on Favorites */ }
                Screen.Stats -> viewModel.navigateToStats()
                else -> { }
            }
        }
    )

    // Dialogs
    if (uiState.showCreateCollectionDialog) {
        CreateCollectionDialog(
            onDismiss = viewModel::dismissCreateCollectionDialog,
            onCreate = viewModel::createCollection
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavoritesScreenContent(
    uiState: FavoritesUiState,
    snackbarHostState: SnackbarHostState,
    onSearchClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCollectionClick: (String) -> Unit,
    onCreateCollectionClick: () -> Unit,
    onRecipeClick: (String) -> Unit,
    onRemoveRecipeClick: (String) -> Unit,
    onAddToCollectionClick: (String, String) -> Unit,
    onCuisineFilterChange: (com.rasoiai.domain.model.CuisineType?) -> Unit,
    onTimeFilterChange: (TimeFilter?) -> Unit,
    onReorderClick: () -> Unit,
    onMoveRecipe: (fromIndex: Int, toIndex: Int) -> Unit,
    onBottomNavItemClick: (Screen) -> Unit
) {
    var showAddToCollectionDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.testTag(TestTags.FAVORITES_SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.showSearchBar) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search favorites...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    } else {
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (uiState.isReorderMode) {
                        TextButton(onClick = onReorderClick) {
                            Text(
                                text = "Done",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = if (uiState.showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (uiState.showSearchBar) "Close search" else "Search"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            RasoiBottomNavigation(
                currentScreen = Screen.Favorites,
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // Collections Row
                    if (!uiState.isReorderMode) {
                        CollectionsRow(
                            collections = uiState.collections,
                            selectedCollectionId = uiState.selectedCollectionId,
                            onCollectionClick = onCollectionClick,
                            onCreateCollectionClick = onCreateCollectionClick
                        )

                        Spacer(modifier = Modifier.height(spacing.md))

                        // Filter Chips
                        FavoritesFilterChips(
                            selectedCuisine = uiState.selectedCuisineFilter,
                            selectedTimeFilter = uiState.selectedTimeFilter,
                            onCuisineSelected = onCuisineFilterChange,
                            onTimeFilterSelected = onTimeFilterChange,
                            modifier = Modifier.padding(horizontal = spacing.md)
                        )

                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // Recipe count and Reorder button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.isReorderMode) "Drag to reorder recipes"
                                   else "${uiState.selectedCollection?.name ?: "All"} (${uiState.recipeCount})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (!uiState.isReorderMode && uiState.filteredRecipes.isNotEmpty()) {
                            TextButton(onClick = onReorderClick) {
                                Text("Reorder")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.sm))

                    // Recipe Grid
                    if (uiState.filteredRecipes.isEmpty()) {
                        EmptyState(
                            message = if (uiState.searchQuery.isNotBlank())
                                "No recipes found for \"${uiState.searchQuery}\""
                            else
                                "No favorite recipes yet. Add recipes from the Home screen!"
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(
                                start = spacing.md,
                                end = spacing.md,
                                bottom = spacing.md
                            ),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(spacing.sm),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = uiState.filteredRecipes,
                                key = { _, recipe -> recipe.id }
                            ) { index, recipe ->
                                RecipeGridItem(
                                    recipe = recipe,
                                    onClick = { onRecipeClick(recipe.id) },
                                    onRemoveClick = { onRemoveRecipeClick(recipe.id) },
                                    onAddToCollectionClick = { showAddToCollectionDialog = recipe.id },
                                    isReorderMode = uiState.isReorderMode,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < uiState.filteredRecipes.size - 1,
                                    onMoveUp = {
                                        if (index > 0) {
                                            onMoveRecipe(index, index - 1)
                                        }
                                    },
                                    onMoveDown = {
                                        if (index < uiState.filteredRecipes.size - 1) {
                                            onMoveRecipe(index, index + 1)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add to collection dialog
    showAddToCollectionDialog?.let { recipeId ->
        val customCollections = uiState.collections
            .filter { !it.isDefault }
            .map { it.id to it.name }

        AddToCollectionDialog(
            collections = customCollections,
            onDismiss = { showAddToCollectionDialog = null },
            onCollectionSelected = { collectionId ->
                onAddToCollectionClick(recipeId, collectionId)
                showAddToCollectionDialog = null
            }
        )
    }
}

@Composable
private fun CollectionsRow(
    collections: List<FavoriteCollection>,
    selectedCollectionId: String,
    onCollectionClick: (String) -> Unit,
    onCreateCollectionClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Collections:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            items(
                items = collections,
                key = { it.id }
            ) { collection ->
                CollectionCard(
                    collection = collection,
                    isSelected = collection.id == selectedCollectionId,
                    onClick = { onCollectionClick(collection.id) }
                )
            }

            item {
                AddCollectionCard(onClick = onCreateCollectionClick)
            }
        }
    }
}

@Composable
private fun EmptyState(
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDFAF4)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1C1B1F
)
@Composable
private fun FavoritesScreenPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            // Preview would need mock data
        }
    }
}
