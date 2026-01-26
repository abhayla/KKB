package com.rasoiai.app.presentation.pantry

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.home.components.RasoiBottomNavigation
import com.rasoiai.app.presentation.navigation.Screen
import com.rasoiai.app.presentation.pantry.components.AddItemDialog
import com.rasoiai.app.presentation.pantry.components.CameraScanSection
import com.rasoiai.app.presentation.pantry.components.PantryItemCard
import com.rasoiai.app.presentation.pantry.components.PantryItemCardLarge
import com.rasoiai.app.presentation.pantry.components.RemoveExpiredDialog
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing

@Composable
fun PantryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToGrocery: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToStats: () -> Unit,
    viewModel: PantryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigationEvent by viewModel.navigationEvent.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            PantryNavigationEvent.NavigateBack -> {
                onNavigateBack()
                viewModel.onNavigationHandled()
            }
            PantryNavigationEvent.NavigateToHome -> {
                onNavigateToHome()
                viewModel.onNavigationHandled()
            }
            PantryNavigationEvent.NavigateToGrocery -> {
                onNavigateToGrocery()
                viewModel.onNavigationHandled()
            }
            PantryNavigationEvent.NavigateToChat -> {
                onNavigateToChat()
                viewModel.onNavigationHandled()
            }
            PantryNavigationEvent.NavigateToFavorites -> {
                onNavigateToFavorites()
                viewModel.onNavigationHandled()
            }
            PantryNavigationEvent.NavigateToStats -> {
                onNavigateToStats()
                viewModel.onNavigationHandled()
            }
            is PantryNavigationEvent.NavigateToRecipeSearch -> {
                // Navigate to chat with ingredients context
                onNavigateToChat()
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

    PantryScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = viewModel::navigateBack,
        onCaptureClick = viewModel::simulateScan, // Using simulate for demo
        onGalleryClick = viewModel::onGalleryClick,
        onViewAllClick = viewModel::showAllItemsSheet,
        onItemClick = { /* Could show item details */ },
        onAddItemClick = viewModel::showAddItemDialog,
        onFindRecipesClick = viewModel::onFindRecipesClick,
        onBottomNavItemClick = { screen ->
            when (screen) {
                Screen.Home -> viewModel.navigateToHome()
                Screen.Grocery -> viewModel.navigateToGrocery()
                Screen.Chat -> viewModel.navigateToChat()
                Screen.Favorites -> viewModel.navigateToFavorites()
                Screen.Stats -> viewModel.navigateToStats()
                else -> { }
            }
        }
    )

    // Dialogs
    if (uiState.showAddItemDialog) {
        AddItemDialog(
            onDismiss = viewModel::dismissAddItemDialog,
            onAdd = viewModel::addItem
        )
    }

    if (uiState.showRemoveExpiredDialog) {
        RemoveExpiredDialog(
            expiredItems = uiState.expiredItems,
            onDismiss = viewModel::dismissRemoveExpiredDialog,
            onConfirm = viewModel::removeExpiredItems
        )
    }

    // Bottom sheet for all items
    if (uiState.showAllItemsSheet) {
        AllItemsBottomSheet(
            items = uiState.pantryItems,
            onDismiss = viewModel::dismissAllItemsSheet,
            onRemoveItem = viewModel::removeItem
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantryScreenContent(
    uiState: PantryUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onFindRecipesClick: () -> Unit,
    onBottomNavItemClick: (Screen) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pantry Scan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddItemClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add item manually"
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
                currentScreen = Screen.Pantry,
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
                    contentPadding = PaddingValues(vertical = spacing.md)
                ) {
                    // Camera scan section
                    item {
                        CameraScanSection(
                            isScanning = uiState.isScanning,
                            onCaptureClick = onCaptureClick,
                            onGalleryClick = onGalleryClick,
                            modifier = Modifier.padding(horizontal = spacing.md)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // My Pantry section header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "My Pantry (${uiState.itemCount} items)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            TextButton(onClick = onViewAllClick) {
                                Text("View All")
                            }
                        }
                    }

                    // Pantry items horizontal scroll
                    item {
                        if (uiState.pantryItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .padding(horizontal = spacing.md),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No items in pantry. Scan or add items to get started!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = spacing.md),
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                            ) {
                                items(
                                    items = uiState.pantryItems.take(10),
                                    key = { it.id }
                                ) { item ->
                                    PantryItemCard(
                                        item = item,
                                        onClick = { onItemClick(item.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Expiry warning legend
                    item {
                        Spacer(modifier = Modifier.height(spacing.sm))
                        Row(
                            modifier = Modifier.padding(horizontal = spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ = Expiring soon",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                    }

                    // Find Recipes button
                    item {
                        Button(
                            onClick = onFindRecipesClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.md),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(spacing.sm))
                            Text(
                                text = "Find Recipes (${uiState.matchingRecipeCount} matches)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllItemsBottomSheet(
    items: List<com.rasoiai.domain.model.PantryItem>,
    onDismiss: () -> Unit,
    onRemoveItem: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md)
                .padding(bottom = spacing.xl)
        ) {
            Text(
                text = "All Pantry Items (${items.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = spacing.md)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                items(
                    items = items,
                    key = { it.id }
                ) { item ->
                    PantryItemCardLarge(
                        item = item,
                        onRemoveClick = { onRemoveItem(item.id) }
                    )
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
private fun PantryScreenPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            // Preview would need mock data
        }
    }
}
