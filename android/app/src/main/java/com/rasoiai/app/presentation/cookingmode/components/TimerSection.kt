package com.rasoiai.app.presentation.cookingmode.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rasoiai.app.presentation.cookingmode.TimerState
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing

/**
 * Timer section for cooking steps with start, pause, resume, and stop controls
 */
@Composable
fun TimerSection(
    timerState: TimerState,
    displayText: String,
    progress: Float,
    durationMinutes: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDismissComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "timer_progress"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(spacing.md)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (timerState) {
                TimerState.IDLE -> {
                    // Start timer button
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(spacing.sm)
                    ) {
                        Text(
                            text = "\u23F1\uFE0F SET TIMER",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(spacing.sm))
                        Text(
                            text = "${durationMinutes}:00",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                TimerState.RUNNING, TimerState.PAUSED -> {
                    // Timer display with circular progress
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(120.dp),
                            strokeWidth = 8.dp,
                            color = if (timerState == TimerState.PAUSED) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "\u23F1\uFE0F",
                                fontSize = 20.sp
                            )
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.alpha(
                                    if (timerState == TimerState.PAUSED) 0.6f else 1f
                                )
                            )
                            if (timerState == TimerState.PAUSED) {
                                Text(
                                    text = "PAUSED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.md))

                    // Timer controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (timerState == TimerState.RUNNING) {
                            OutlinedButton(
                                onClick = onPause,
                                shape = RoundedCornerShape(spacing.sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause"
                                )
                                Spacer(modifier = Modifier.width(spacing.xs))
                                Text("PAUSE")
                            }
                        } else {
                            Button(
                                onClick = onResume,
                                shape = RoundedCornerShape(spacing.sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume"
                                )
                                Spacer(modifier = Modifier.width(spacing.xs))
                                Text("RESUME")
                            }
                        }

                        OutlinedButton(
                            onClick = onStop,
                            shape = RoundedCornerShape(spacing.sm),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop"
                            )
                            Spacer(modifier = Modifier.width(spacing.xs))
                            Text("STOP")
                        }
                    }
                }

                TimerState.COMPLETED -> {
                    // Timer complete notification
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "\u23F1\uFE0F TIME'S UP!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(spacing.sm))

                        Text(
                            text = "Step timer completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(spacing.md))

                        Button(
                            onClick = onDismissComplete,
                            shape = RoundedCornerShape(spacing.sm)
                        ) {
                            Text(
                                text = "DISMISS",
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

@Preview(showBackground = true, backgroundColor = 0xFFFDFAF4)
@Composable
private fun TimerSectionIdlePreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimerSection(
                timerState = TimerState.IDLE,
                displayText = "30:00",
                progress = 1f,
                durationMinutes = 30,
                onStart = {},
                onPause = {},
                onResume = {},
                onStop = {},
                onDismissComplete = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDFAF4)
@Composable
private fun TimerSectionRunningPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimerSection(
                timerState = TimerState.RUNNING,
                displayText = "24:35",
                progress = 0.82f,
                durationMinutes = 30,
                onStart = {},
                onPause = {},
                onResume = {},
                onStop = {},
                onDismissComplete = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDFAF4)
@Composable
private fun TimerSectionPausedPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimerSection(
                timerState = TimerState.PAUSED,
                displayText = "24:35",
                progress = 0.82f,
                durationMinutes = 30,
                onStart = {},
                onPause = {},
                onResume = {},
                onStop = {},
                onDismissComplete = {},
                modifier = Modifier.padding(16.dp)
            )
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
private fun TimerSectionCompletedPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimerSection(
                timerState = TimerState.COMPLETED,
                displayText = "00:00",
                progress = 0f,
                durationMinutes = 30,
                onStart = {},
                onPause = {},
                onResume = {},
                onStop = {},
                onDismissComplete = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
