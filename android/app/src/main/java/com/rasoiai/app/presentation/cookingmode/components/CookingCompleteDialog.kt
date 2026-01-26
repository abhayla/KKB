package com.rasoiai.app.presentation.cookingmode.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing

/**
 * Dialog shown when cooking is complete, allowing user to rate the dish
 */
@Composable
fun CookingCompleteDialog(
    recipeName: String,
    rating: Int,
    feedback: String,
    onRatingChange: (Int) -> Unit,
    onFeedbackChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit
) {
    Dialog(onDismissRequest = onSkip) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(spacing.lg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Celebration emoji
                Text(
                    text = "\uD83C\uDF89",
                    fontSize = 48.sp
                )

                Spacer(modifier = Modifier.height(spacing.md))

                // Title
                Text(
                    text = "Cooking Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                // Recipe name
                Text(
                    text = "$recipeName is ready",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(spacing.xl))

                // Rating section
                Text(
                    text = "Rate this dish",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                // Star rating
                StarRating(
                    rating = rating,
                    onRatingChange = onRatingChange
                )

                Spacer(modifier = Modifier.height(spacing.lg))

                // Feedback text field
                Text(
                    text = "How did it turn out?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                OutlinedTextField(
                    value = feedback,
                    onValueChange = onFeedbackChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Add feedback (optional)...")
                    },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(spacing.sm)
                )

                Spacer(modifier = Modifier.height(spacing.lg))

                // Submit button
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(spacing.sm),
                    enabled = rating > 0
                ) {
                    Text(
                        text = "DONE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(spacing.sm))

                // Skip button
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "SKIP RATING",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun StarRating(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) {
                    Icons.Filled.Star
                } else {
                    Icons.Outlined.StarOutline
                },
                contentDescription = "Star $i",
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onRatingChange(i) }
                    .padding(spacing.xs),
                tint = if (i <= rating) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
            if (i < 5) {
                Spacer(modifier = Modifier.width(spacing.xs))
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CookingCompleteDialogPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CookingCompleteDialog(
                recipeName = "Dal Tadka",
                rating = 4,
                feedback = "",
                onRatingChange = {},
                onFeedbackChange = {},
                onSubmit = {},
                onSkip = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StarRatingPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            StarRating(
                rating = 3,
                onRatingChange = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
