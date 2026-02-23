package com.rasoiai.app.presentation.grocery

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.rasoiai.app.presentation.common.TestTags
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.grocery.components.AddItemDialog
import com.rasoiai.app.presentation.grocery.components.EditItemDialog
import com.rasoiai.app.presentation.grocery.components.WhatsAppShareDialog
import com.rasoiai.app.presentation.home.components.RasoiBottomNavigation
import com.rasoiai.app.presentation.navigation.Screen
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.GroceryCategory
import com.rasoiai.domain.model.GroceryItem
import com.rasoiai.domain.model.IngredientCategory

@Composable
fun GroceryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToStats: () -> Unit,
    viewModel: GroceryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                GroceryNavigationEvent.NavigateBack -> onNavigateBack()
                GroceryNavigationEvent.NavigateToHome -> onNavigateToHome()
                GroceryNavigationEvent.NavigateToChat -> onNavigateToChat()
                GroceryNavigationEvent.NavigateToFavorites -> onNavigateToFavorites()
                GroceryNavigationEvent.NavigateToStats -> onNavigateToStats()
                is GroceryNavigationEvent.ShareViaWhatsApp -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // WhatsApp not installed, use regular share
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, event.text)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Grocery List"))
                    }
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

    GroceryScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = viewModel::navigateBack,
        onWhatsAppShareClick = viewModel::showWhatsAppDialog,
        onToggleCategory = viewModel::toggleCategoryExpanded,
        onToggleItemPurchased = viewModel::toggleItemPurchased,
        onEditItem = viewModel::showEditDialog,
        onDeleteItem = viewModel::deleteItem,
        onAddCustomItem = viewModel::showAddItemDialog,
        onMoreOptionsClick = viewModel::showMoreOptionsMenu,
        onDismissMoreOptions = viewModel::dismissMoreOptionsMenu,
        onClearPurchasedClick = viewModel::clearPurchasedItems,
        onShareAsTextClick = viewModel::shareAsText,
        onBottomNavItemClick = { screen ->
            when (screen) {
                Screen.Home -> viewModel.navigateToHome()
                Screen.Grocery -> { /* Already on Grocery */ }
                Screen.Chat -> viewModel.navigateToChat()
                Screen.Favorites -> viewModel.navigateToFavorites()
                Screen.Stats -> viewModel.navigateToStats()
                else -> { }
            }
        }
    )

    // Dialogs
    if (uiState.showWhatsAppDialog) {
        WhatsAppShareDialog(
            shareText = uiState.whatsAppShareText,
            totalItems = uiState.totalItems,
            unpurchasedItems = uiState.unpurchasedItems,
            selectedOption = uiState.shareOption,
            onOptionSelected = viewModel::setShareOption,
            onDismiss = viewModel::dismissWhatsAppDialog,
            onShare = viewModel::shareViaWhatsApp
        )
    }

    if (uiState.showEditDialog) {
        uiState.selectedItem?.let { item ->
            EditItemDialog(
                item = item,
                onDismiss = viewModel::dismissEditDialog,
                onConfirm = { quantity, unit ->
                    viewModel.updateItemQuantity(quantity, unit)
                }
            )
        }
    }

    if (uiState.showAddItemDialog) {
        AddItemDialog(
            onDismiss = viewModel::dismissAddItemDialog,
            onConfirm = { name, quantity, unit, category ->
                viewModel.addCustomItem(name, quantity, unit, category)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroceryScreenContent(
    uiState: GroceryUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onWhatsAppShareClick: () -> Unit,
    onToggleCategory: (IngredientCategory) -> Unit,
    onToggleItemPurchased: (GroceryItem) -> Unit,
    onEditItem: (GroceryItem) -> Unit,
    onDeleteItem: (GroceryItem) -> Unit,
    onAddCustomItem: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    onDismissMoreOptions: () -> Unit,
    onClearPurchasedClick: () -> Unit,
    onShareAsTextClick: () -> Unit,
    onBottomNavItemClick: (Screen) -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.GROCERY_SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Grocery List",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = onMoreOptionsClick) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = uiState.showMoreOptionsMenu,
                            onDismissRequest = onDismissMoreOptions
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear purchased items") },
                                onClick = onClearPurchasedClick,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.RemoveCircleOutline,
                                        contentDescription = "Clear purchased items"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share as text") },
                                onClick = onShareAsTextClick,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.TextSnippet,
                                        contentDescription = "Share as text"
                                    )
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
        bottomBar = {
            RasoiBottomNavigation(
                currentScreen = Screen.Grocery,
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
                    // Week Header
                    item {
                        WeekHeader(
                            dateRange = uiState.formattedDateRange,
                            totalItems = uiState.totalItems
                        )
                    }

                    // WhatsApp Share Button
                    item {
                        WhatsAppShareButton(onClick = onWhatsAppShareClick)
                    }

                    // Categories
                    items(
                        items = uiState.categories,
                        key = { it.category.name }
                    ) { category ->
                        GroceryCategorySection(
                            category = category,
                            isExpanded = category.category in uiState.expandedCategories,
                            onToggleExpanded = { onToggleCategory(category.category) },
                            onToggleItemPurchased = onToggleItemPurchased,
                            onEditItem = onEditItem,
                            onDeleteItem = onDeleteItem
                        )
                    }

                    // Add Custom Item Button
                    item {
                        AddCustomItemButton(onClick = onAddCustomItem)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(
    dateRange: String,
    totalItems: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .testTag(TestTags.GROCERY_WEEK_HEADER)
    ) {
        Text(
            text = "Week of $dateRange",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "$totalItems items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TestTags.GROCERY_TOTAL_ITEMS)
        )
    }
}

@Composable
private fun WhatsAppShareButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .testTag(TestTags.GROCERY_WHATSAPP_BUTTON),
        shape = RoundedCornerShape(spacing.sm),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF25D366) // WhatsApp green
        )
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share via WhatsApp",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(spacing.sm))
        Text(
            text = "Share via WhatsApp",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun GroceryCategorySection(
    category: GroceryCategory,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleItemPurchased: (GroceryItem) -> Unit,
    onEditItem: (GroceryItem) -> Unit,
    onDeleteItem: (GroceryItem) -> Unit
) {
    val categoryTag = category.category.name.lowercase()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .testTag("${TestTags.GROCERY_CATEGORY_PREFIX}$categoryTag"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(spacing.md)
    ) {
        Column {
            // Category Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getCategoryEmoji(category.category),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Text(
                        text = category.category.displayName.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text(
                        text = "(${category.itemCount})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Items
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    category.items.forEach { item ->
                        GroceryItemRow(
                            item = item,
                            onTogglePurchased = { onToggleItemPurchased(item) },
                            onEdit = { onEditItem(item) },
                            onDelete = { onDeleteItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroceryItemRow(
    item: GroceryItem,
    onTogglePurchased: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false // Don't dismiss, just trigger action
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                            else -> Color.Transparent
                        }
                    )
                    .padding(horizontal = spacing.md),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                }
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    else -> {}
                }
            }
        },
        content = {
            Surface(
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onTogglePurchased)
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isPurchased,
                        onCheckedChange = { onTogglePurchased() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.width(spacing.sm))

                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.isPurchased) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Text(
                        text = item.displayQuantity,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }
        }
    )
}

@Composable
private fun AddCustomItemButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .testTag(TestTags.GROCERY_ADD_ITEM_BUTTON),
        shape = RoundedCornerShape(spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add custom item",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(spacing.sm))
        Text(
            text = "Add custom item",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private fun getCategoryEmoji(category: IngredientCategory): String {
    return when (category) {
        IngredientCategory.VEGETABLES -> "\uD83E\uDD6C"
        IngredientCategory.FRUITS -> "\uD83C\uDF4E"
        IngredientCategory.DAIRY -> "\uD83E\uDD5B"
        IngredientCategory.GRAINS -> "\uD83C\uDF3E"
        IngredientCategory.PULSES -> "\uD83E\uDED8"
        IngredientCategory.SPICES -> "\uD83C\uDF36\uFE0F"
        IngredientCategory.OILS -> "\uD83E\uDED2"
        IngredientCategory.MEAT -> "\uD83E\uDD69"
        IngredientCategory.SEAFOOD -> "\uD83E\uDD90"
        IngredientCategory.NUTS -> "\uD83E\uDD5C"
        IngredientCategory.SWEETENERS -> "\uD83C\uDF6F"
        IngredientCategory.OTHER -> "\uD83E\uDDFA"
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDFAF4)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1C1B1F
)
@Composable
private fun GroceryScreenPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            // Preview would need mock data
        }
    }
}
