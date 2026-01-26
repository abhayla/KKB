package com.rasoiai.app.presentation.pantry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.PantryCategory

/**
 * Data class representing a scanned item before being added to the pantry
 */
data class ScannedItem(
    val name: String,
    val category: PantryCategory,
    var quantity: Int = 1,
    var unit: String = "piece"
)

/**
 * Bottom sheet showing scan results with edit capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultsSheet(
    scannedItems: List<ScannedItem>,
    onDismiss: () -> Unit,
    onConfirm: (List<ScannedItem>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Create mutable copies for editing
    val editableItems = remember(scannedItems) {
        mutableStateListOf<ScannedItem>().apply {
            addAll(scannedItems.map { it.copy() })
        }
    }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scan Results",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${editableItems.size} items detected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = "Review and edit quantities before adding to your pantry",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(spacing.md))

            // Scanned items list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                itemsIndexed(
                    items = editableItems,
                    key = { index, _ -> index }
                ) { index, item ->
                    ScannedItemCard(
                        item = item,
                        onQuantityChange = { newQty ->
                            editableItems[index] = item.copy(quantity = newQty)
                        },
                        onUnitChange = { newUnit ->
                            editableItems[index] = item.copy(unit = newUnit)
                        },
                        onRemove = {
                            editableItems.removeAt(index)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { onConfirm(editableItems.toList()) },
                    enabled = editableItems.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Add ${editableItems.size} Items")
                }
            }
        }
    }
}

@Composable
private fun ScannedItemCard(
    item: ScannedItem,
    onQuantityChange: (Int) -> Unit,
    onUnitChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(spacing.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category emoji
            Text(
                text = item.category.emoji,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.width(spacing.sm))

            // Item name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.category.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quantity controls
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) },
                    modifier = Modifier.height(32.dp).width(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        modifier = Modifier.height(16.dp).width(16.dp)
                    )
                }

                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = spacing.sm)
                )

                FilledIconButton(
                    onClick = { onQuantityChange(item.quantity + 1) },
                    modifier = Modifier.height(32.dp).width(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.height(16.dp).width(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(spacing.sm))

            // Remove button
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove item",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
