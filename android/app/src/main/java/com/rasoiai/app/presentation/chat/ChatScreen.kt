package com.rasoiai.app.presentation.chat

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.chat.components.ChatInputBar
import com.rasoiai.app.presentation.chat.components.ChatMessageItem
import com.rasoiai.app.presentation.home.components.RasoiBottomNavigation
import com.rasoiai.app.presentation.navigation.Screen
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing

@Composable
fun ChatScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToGrocery: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToRecipeDetail: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigationEvent by viewModel.navigationEvent.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            ChatNavigationEvent.NavigateBack -> {
                onNavigateToHome()
                viewModel.onNavigationHandled()
            }
            ChatNavigationEvent.NavigateToHome -> {
                onNavigateToHome()
                viewModel.onNavigationHandled()
            }
            ChatNavigationEvent.NavigateToGrocery -> {
                onNavigateToGrocery()
                viewModel.onNavigationHandled()
            }
            ChatNavigationEvent.NavigateToFavorites -> {
                onNavigateToFavorites()
                viewModel.onNavigationHandled()
            }
            ChatNavigationEvent.NavigateToStats -> {
                onNavigateToStats()
                viewModel.onNavigationHandled()
            }
            is ChatNavigationEvent.NavigateToRecipeDetail -> {
                onNavigateToRecipeDetail(event.recipeId)
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

    ChatScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = viewModel::navigateBack,
        onMenuClick = viewModel::toggleMenu,
        onDismissMenu = viewModel::dismissMenu,
        onClearChatClick = viewModel::showClearChatDialog,
        onInputChange = viewModel::updateInputText,
        onSendClick = viewModel::sendMessage,
        onQuickActionClick = viewModel::onQuickActionClick,
        onRecipeClick = viewModel::navigateToRecipeDetail,
        onAddToMealPlan = viewModel::addRecipeToMealPlan,
        onAttachmentClick = viewModel::onAttachmentButtonClick,
        onVoiceClick = viewModel::onVoiceButtonClick,
        onBottomNavItemClick = { screen ->
            when (screen) {
                Screen.Home -> viewModel.navigateToHome()
                Screen.Grocery -> viewModel.navigateToGrocery()
                Screen.Chat -> { /* Already on Chat */ }
                Screen.Favorites -> viewModel.navigateToFavorites()
                Screen.Stats -> viewModel.navigateToStats()
                else -> { }
            }
        }
    )

    // Clear chat confirmation dialog
    if (uiState.showClearChatDialog) {
        ClearChatDialog(
            onConfirm = viewModel::clearChatHistory,
            onDismiss = viewModel::dismissClearChatDialog
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreenContent(
    uiState: ChatUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onClearChatClick: () -> Unit,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onQuickActionClick: (String) -> Unit,
    onRecipeClick: (String) -> Unit,
    onAddToMealPlan: (String) -> Unit,
    onAttachmentClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onBottomNavItemClick: (Screen) -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll to bottom when new message is added
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RasoiAI Assistant",
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
                    Box {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = uiState.showMenu,
                            onDismissRequest = onDismissMenu
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Chat History") },
                                onClick = onClearChatClick,
                                leadingIcon = {
                                    Text("🗑️")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Chat Settings") },
                                onClick = {
                                    onDismissMenu()
                                    // TODO: Navigate to settings
                                },
                                leadingIcon = {
                                    Text("⚙️")
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
            Column {
                ChatInputBar(
                    inputText = uiState.inputText,
                    isSending = uiState.isSending,
                    onInputChange = onInputChange,
                    onSendClick = onSendClick,
                    onAttachmentClick = onAttachmentClick,
                    onVoiceClick = onVoiceClick
                )
                RasoiBottomNavigation(
                    currentScreen = Screen.Chat,
                    onItemClick = onBottomNavItemClick
                )
            }
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
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = spacing.sm)
                ) {
                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { message ->
                        ChatMessageItem(
                            message = message,
                            onQuickActionClick = onQuickActionClick,
                            onRecipeClick = onRecipeClick,
                            onAddToMealPlan = onAddToMealPlan
                        )
                    }

                    // Show typing indicator when AI is responding
                    if (uiState.isSending) {
                        item {
                            TypingIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = spacing.md, vertical = spacing.xs)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "RasoiAI is typing...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
            )
        }
    }
}

@Composable
private fun ClearChatDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Clear Chat History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to clear all chat history? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Clear",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFDFAF4)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1C1B1F
)
@Composable
private fun ChatScreenPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            // Preview would need mock data
        }
    }
}
