package com.rasoiai.app.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.Recipe

/**
 * Bottom sheet for adding a recipe to a meal slot.
 * Displays recipes in a 2-column grid layout with search and tabs for Suggestions/Favorites.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeSheet(
    mealType: MealType,
    suggestedRecipes: List<Recipe>,
    favoriteRecipes: List<Recipe>,
    onDismiss: () -> Unit,
    onRecipeSelected: (Recipe) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val mealName = when (mealType) {
        MealType.BREAKFAST -> "Breakfast"
        MealType.LUNCH -> "Lunch"
        MealType.DINNER -> "Dinner"
        MealType.SNACKS -> "Snacks"
    }

    // Filter recipes based on search query
    val filteredSuggestions = remember(searchQuery, suggestedRecipes) {
        if (searchQuery.isBlank()) {
            suggestedRecipes
        } else {
            suggestedRecipes.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.cuisineType.displayName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredFavorites = remember(searchQuery, favoriteRecipes) {
        if (searchQuery.isBlank()) {
            favoriteRecipes
        } else {
            favoriteRecipes.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.cuisineType.displayName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val displayedRecipes = if (selectedTabIndex == 0) filteredSuggestions else filteredFavorites

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag(TestTags.ADD_RECIPE_SHEET)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md)
        ) {
            // Title
            Text(
                text = "Add Recipe to $mealName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
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

            // Tabs: Suggestions / Favorites
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Suggestions",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Favorites",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Recipe Grid (2 columns)
            if (displayedRecipes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.xl),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No recipes match your search" else "No recipes available",
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
                        items = displayedRecipes,
                        key = { it.id }
                    ) { recipe ->
                        RecipeSelectionGridItem(
                            recipe = recipe,
                            onClick = { onRecipeSelected(recipe) }
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
 * Alternative version with simplified recipe data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeSheet(
    mealType: MealType,
    suggestedRecipes: List<RecipeSelectionItem>,
    favoriteRecipes: List<RecipeSelectionItem>,
    onDismiss: () -> Unit,
    onRecipeSelected: (RecipeSelectionItem) -> Unit,
    useSimplifiedData: Boolean = true // Marker to differentiate overloads
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val mealName = when (mealType) {
        MealType.BREAKFAST -> "Breakfast"
        MealType.LUNCH -> "Lunch"
        MealType.DINNER -> "Dinner"
        MealType.SNACKS -> "Snacks"
    }

    // Filter recipes based on search query
    val filteredSuggestions = remember(searchQuery, suggestedRecipes) {
        if (searchQuery.isBlank()) {
            suggestedRecipes
        } else {
            suggestedRecipes.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.cuisineType.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredFavorites = remember(searchQuery, favoriteRecipes) {
        if (searchQuery.isBlank()) {
            favoriteRecipes
        } else {
            favoriteRecipes.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.cuisineType.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val displayedRecipes = if (selectedTabIndex == 0) filteredSuggestions else filteredFavorites

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag(TestTags.ADD_RECIPE_SHEET)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md)
        ) {
            // Title
            Text(
                text = "Add Recipe to $mealName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
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

            // Tabs: Suggestions / Favorites
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Suggestions",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Favorites",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Recipe Grid (2 columns)
            if (displayedRecipes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.xl),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No recipes match your search" else "No recipes available",
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
                        items = displayedRecipes,
                        key = { it.id }
                    ) { recipe ->
                        RecipeSelectionGridItem(
                            item = recipe,
                            onClick = { onRecipeSelected(recipe) }
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
