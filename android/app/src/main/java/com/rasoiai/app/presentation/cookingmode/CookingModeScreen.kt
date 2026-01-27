package com.rasoiai.app.presentation.cookingmode

import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.cookingmode.components.CookingCompleteDialog
import com.rasoiai.app.presentation.cookingmode.components.StepContent
import com.rasoiai.app.presentation.cookingmode.components.TimerSection
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing

@Composable
fun CookingModeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: CookingModeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Keep screen on
    KeepScreenOn()

    // Handle back button
    BackHandler(enabled = true) {
        viewModel.requestExit()
    }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                CookingModeNavigationEvent.NavigateBack -> onNavigateBack()
                CookingModeNavigationEvent.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    // Set up timer completion callback with vibration/sound
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.onTimerComplete = {
            // Vibrate on timer complete
            try {
                val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                vibrator?.let {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        it.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(longArrayOf(0, 500, 200, 500), -1)
                    }
                }
            } catch (e: Exception) {
                // Ignore vibration errors
            }
        }
    }

    CookingModeContent(
        uiState = uiState,
        onCloseClick = viewModel::requestExit,
        onVoiceGuidanceToggle = viewModel::toggleVoiceGuidance,
        onPreviousStep = viewModel::previousStep,
        onNextStep = viewModel::nextStep,
        onStartTimer = viewModel::startTimer,
        onPauseTimer = viewModel::pauseTimer,
        onResumeTimer = viewModel::resumeTimer,
        onStopTimer = viewModel::stopTimer,
        onDismissTimerComplete = viewModel::dismissTimerComplete,
        onDismissExitConfirmation = viewModel::dismissExitConfirmation,
        onConfirmExit = viewModel::confirmExit,
        onRatingChange = viewModel::updateRating,
        onFeedbackChange = viewModel::updateFeedback,
        onSubmitRating = viewModel::submitRating,
        onSkipRating = viewModel::skipRating
    )
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CookingModeContent(
    uiState: CookingModeUiState,
    onCloseClick: () -> Unit,
    onVoiceGuidanceToggle: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onDismissTimerComplete: () -> Unit,
    onDismissExitConfirmation: () -> Unit,
    onConfirmExit: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onFeedbackChange: (String) -> Unit,
    onSubmitRating: () -> Unit,
    onSkipRating: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.recipe?.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Step ${uiState.stepNumber} / ${uiState.totalSteps}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCloseClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close cooking mode"
                        )
                    }
                },
                actions = {
                    // Voice guidance toggle
                    IconButton(onClick = onVoiceGuidanceToggle) {
                        Icon(
                            imageVector = if (uiState.voiceGuidanceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = if (uiState.voiceGuidanceEnabled) "Disable voice guidance" else "Enable voice guidance",
                            tint = if (uiState.voiceGuidanceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Screen stays ON indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(spacing.sm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = "Screen stays ON",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
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
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Progress indicator
                        LinearProgressIndicator(
                            progress = { uiState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Main content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(spacing.md),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(spacing.lg))

                            // Step number badge
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "STEP ${uiState.stepNumber}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(spacing.xl))

                            // Step content
                            uiState.currentStep?.let { step ->
                                StepContent(
                                    instruction = step,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(spacing.xl))

                            // Timer section (if step has duration)
                            if (uiState.hasTimer) {
                                TimerSection(
                                    timerState = uiState.timerState,
                                    displayText = uiState.timerDisplayText,
                                    progress = uiState.timerProgress,
                                    durationMinutes = uiState.currentStep?.durationMinutes ?: 0,
                                    onStart = onStartTimer,
                                    onPause = onPauseTimer,
                                    onResume = onResumeTimer,
                                    onStop = onStopTimer,
                                    onDismissComplete = onDismissTimerComplete,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(spacing.xl))
                        }

                        // Navigation buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(spacing.md)
                        ) {
                            OutlinedButton(
                                onClick = onPreviousStep,
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isFirstStep,
                                shape = RoundedCornerShape(spacing.sm)
                            ) {
                                Text(
                                    text = "\u2190 PREV",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = onNextStep,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(spacing.sm)
                            ) {
                                Text(
                                    text = if (uiState.isLastStep) "FINISH \u2713" else "NEXT \u2192",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Exit confirmation dialog
    if (uiState.showExitConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissExitConfirmation,
            title = { Text("Exit Cooking Mode?") },
            text = { Text("Your progress will not be saved. Are you sure you want to exit?") },
            confirmButton = {
                TextButton(onClick = onConfirmExit) {
                    Text("Exit", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExitConfirmation) {
                    Text("Continue Cooking")
                }
            }
        )
    }

    // Completion dialog
    if (uiState.showCompletionDialog) {
        CookingCompleteDialog(
            recipeName = uiState.recipe?.name ?: "",
            rating = uiState.rating,
            feedback = uiState.feedback,
            onRatingChange = onRatingChange,
            onFeedbackChange = onFeedbackChange,
            onSubmit = onSubmitRating,
            onSkip = onSkipRating
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
private fun CookingModeScreenPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            // Preview would need mock data
        }
    }
}
