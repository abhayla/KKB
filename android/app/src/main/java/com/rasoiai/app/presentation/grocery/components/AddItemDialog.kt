package com.rasoiai.app.presentation.grocery.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.IngredientCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, quantity: String, unit: String, category: IngredientCategory) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(IngredientCategory.OTHER) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank() && quantity.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            Text(
                text = "Add Custom Item",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = spacing.md)
            )

            // Item Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                shape = RoundedCornerShape(spacing.sm)
            )

            Spacer(modifier = Modifier.height(spacing.md))

            // Quantity and Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(spacing.sm)
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("kg, g, pcs...") },
                    shape = RoundedCornerShape(spacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(spacing.sm)
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    IngredientCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${getCategoryEmoji(category)} ${category.displayName}"
                                )
                            },
                            onClick = {
                                selectedCategory = category
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(spacing.sm)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (isValid) {
                            onConfirm(name.trim(), quantity.trim(), unit.trim(), selectedCategory)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(spacing.sm),
                    enabled = isValid
                ) {
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(spacing.xl))
        }
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

@Preview(showBackground = true)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AddItemDialogPreview() {
    RasoiAITheme {
        Surface {
            // Preview needs bottom sheet context
        }
    }
}
