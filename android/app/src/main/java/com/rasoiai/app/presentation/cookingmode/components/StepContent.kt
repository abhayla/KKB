package com.rasoiai.app.presentation.cookingmode.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.Instruction

/**
 * Displays the current cooking step instruction with optional tips
 */
@Composable
fun StepContent(
    instruction: Instruction,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main instruction text
        Text(
            text = instruction.instruction,
            style = MaterialTheme.typography.headlineSmall.copy(
                lineHeight = 32.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = spacing.md)
        )

        // Duration indicator if available
        instruction.durationMinutes?.let { duration ->
            if (duration > 0) {
                Spacer(modifier = Modifier.height(spacing.md))
                Text(
                    text = "\u23F1\uFE0F $duration min",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Tips section if available
        instruction.tips?.let { tips ->
            if (tips.isNotBlank()) {
                Spacer(modifier = Modifier.height(spacing.lg))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(spacing.sm)
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.md)
                    ) {
                        Text(
                            text = "\uD83D\uDCA1 Tip",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = tips,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Timer required indicator
        if (instruction.timerRequired && instruction.durationMinutes != null) {
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = "Timer recommended for this step",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
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
private fun StepContentPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            StepContent(
                instruction = Instruction(
                    stepNumber = 1,
                    instruction = "Wash and soak toor dal for 30 minutes. Pressure cook with turmeric and salt for 3 whistles.",
                    durationMinutes = 30,
                    timerRequired = true,
                    tips = "Soaking helps the dal cook faster and more evenly."
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDFAF4)
@Composable
private fun StepContentNoTimerPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            StepContent(
                instruction = Instruction(
                    stepNumber = 2,
                    instruction = "Heat ghee in a pan. Add cumin seeds and let them splutter.",
                    durationMinutes = null,
                    timerRequired = false,
                    tips = null
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
