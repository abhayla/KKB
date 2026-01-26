package com.rasoiai.app.presentation.pantry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.PantryCategory

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, category: PantryCategory, quantity: Int, unit: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PantryCategory.VEGETABLES) }
    var quantity by remember { mutableIntStateOf(1) }
    var unit by remember { mutableStateOf("piece") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Pantry Item",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(spacing.md))

                // Category selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    PantryCategory.entries.take(8).forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = "${category.emoji} ${category.displayName.split(" ").first()}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.md))

                // Quantity and unit
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = quantity.toString(),
                        onValueChange = { quantity = it.toIntOrNull() ?: 1 },
                        label = { Text("Qty") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(spacing.md))

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name, selectedCategory, quantity, unit)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RemoveExpiredDialog(
    expiredItems: List<com.rasoiai.domain.model.PantryItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Remove Expired Items?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "These items may have expired:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                expiredItems.take(5).forEach { item ->
                    Text(
                        text = "• ${item.name} (${item.category.emoji})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = spacing.xxs)
                    )
                }

                if (expiredItems.size > 5) {
                    Text(
                        text = "...and ${expiredItems.size - 5} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Remove All",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep All")
            }
        }
    )
}
