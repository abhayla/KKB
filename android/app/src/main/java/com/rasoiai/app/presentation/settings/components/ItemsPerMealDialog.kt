package com.rasoiai.app.presentation.settings.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing

/**
 * Dialog for selecting the number of items per meal slot.
 * Options range from 1 to 4 items.
 */
@Composable
fun ItemsPerMealDialog(
    currentValue: Int,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Items per Meal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Choose how many items to include in each meal slot",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = spacing.md)
                )
                (1..4).forEach { count ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(count) }
                            .padding(vertical = spacing.sm)
                            .testTag("items_per_meal_option_$count"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = count == currentValue,
                            onClick = { onSelected(count) }
                        )
                        Spacer(modifier = Modifier.width(spacing.sm))
                        Column {
                            Text(
                                text = "$count ${if (count == 1) "item" else "items"}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = getItemCountDescription(count),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getItemCountDescription(count: Int): String {
    return when (count) {
        1 -> "Single dish meals"
        2 -> "Recommended for balanced meals"
        3 -> "Full thali experience"
        4 -> "Feast mode"
        else -> ""
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ItemsPerMealDialogPreview() {
    RasoiAITheme {
        ItemsPerMealDialog(
            currentValue = 2,
            onSelected = {},
            onDismiss = {}
        )
    }
}
