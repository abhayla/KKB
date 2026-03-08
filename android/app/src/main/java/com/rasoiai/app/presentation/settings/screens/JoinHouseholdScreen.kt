package com.rasoiai.app.presentation.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.settings.viewmodels.JoinHouseholdNavigationEvent
import com.rasoiai.app.presentation.settings.viewmodels.JoinHouseholdUiState
import com.rasoiai.app.presentation.settings.viewmodels.JoinHouseholdViewModel
import com.rasoiai.app.presentation.theme.spacing

// ---------------------------------------------------------------------------
// Route (hiltViewModel injection point)
// ---------------------------------------------------------------------------

@Composable
fun JoinHouseholdRoute(
    onNavigateBack: () -> Unit,
    onNavigateToHousehold: () -> Unit,
    viewModel: JoinHouseholdViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is JoinHouseholdNavigationEvent.NavigateToHousehold -> onNavigateToHousehold()
            }
        }
    }

    JoinHouseholdScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onCodeChanged = viewModel::onCodeChanged,
        onJoin = viewModel::joinHousehold
    )
}

// ---------------------------------------------------------------------------
// Screen (pure, no ViewModel reference)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinHouseholdScreen(
    uiState: JoinHouseholdUiState,
    onNavigateBack: () -> Unit,
    onCodeChanged: (String) -> Unit,
    onJoin: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join Household", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(spacing.xl))

            Icon(
                imageVector = Icons.Filled.GroupAdd,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(spacing.md))

            Text(
                text = "Enter Invite Code",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = "Enter the invite code shared by your household owner to join their meal plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(spacing.md)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    OutlinedTextField(
                        value = uiState.inviteCode,
                        onValueChange = onCodeChanged,
                        label = { Text("Invite code") },
                        placeholder = { Text("e.g. ABC12345") },
                        singleLine = true,
                        isError = uiState.error != null,
                        supportingText = uiState.error?.let { error ->
                            {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        shape = RoundedCornerShape(spacing.sm),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.HOUSEHOLD_JOIN_CODE_FIELD)
                            .semantics { contentDescription = "Invite code input" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xl))

            Button(
                onClick = onJoin,
                enabled = !uiState.isLoading && uiState.inviteCode.isNotBlank(),
                shape = RoundedCornerShape(spacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag(TestTags.HOUSEHOLD_JOIN_BUTTON)
                    .semantics { contentDescription = "Join household" }
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(24.dp)
                            .width(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Join Household", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
