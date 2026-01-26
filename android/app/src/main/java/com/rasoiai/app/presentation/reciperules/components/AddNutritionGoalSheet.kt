package com.rasoiai.app.presentation.reciperules.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rasoiai.app.presentation.reciperules.RecipeRulesUiState
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.FoodCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNutritionGoalSheet(
    uiState: RecipeRulesUiState,
    onDismiss: () -> Unit,
    onFoodCategoryChange: (FoodCategory) -> Unit,
    onWeeklyTargetChange: (Int) -> Unit,
    onSave: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var categoryExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }

    val title = if (uiState.editingNutritionGoal != null) {
        "Edit Nutrition Goal"
    } else {
        "Add Nutrition Goal"
    }

    // Available categories include the current one if editing
    val availableCategories = if (uiState.editingNutritionGoal != null) {
        uiState.availableFoodCategories + uiState.editingNutritionGoal.foodCategory
    } else {
        uiState.availableFoodCategories
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            // Food Category
            Text(
                text = "Food Category:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.selectedFoodCategory?.let {
                        "${it.emoji} ${it.displayName}"
                    } ?: "Select category...",
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    shape = RoundedCornerShape(8.dp)
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    if (availableCategories.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "All categories have goals",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { categoryExpanded = false }
                        )
                    } else {
                        availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text("${category.emoji} ${category.displayName}")
                                },
                                onClick = {
                                    onFoodCategoryChange(category)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Show available categories list
            if (availableCategories.isNotEmpty() && !categoryExpanded) {
                Spacer(modifier = Modifier.height(spacing.sm))
                Text(
                    text = "Available categories:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                availableCategories.take(5).forEach { category ->
                    Text(
                        text = "• ${category.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (availableCategories.size > 5) {
                    Text(
                        text = "• ...and ${availableCategories.size - 5} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(spacing.md))

            // Weekly Target
            Text(
                text = "Weekly Target:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "At least",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.width(spacing.sm))

                ExposedDropdownMenuBox(
                    expanded = targetExpanded,
                    onExpandedChange = { targetExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.weeklyTarget.toString(),
                        onValueChange = {},
                        modifier = Modifier
                            .width(80.dp)
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = targetExpanded,
                        onDismissRequest = { targetExpanded = false }
                    ) {
                        (1..14).forEach { count ->
                            DropdownMenuItem(
                                text = { Text(count.toString()) },
                                onClick = {
                                    onWeeklyTargetChange(count)
                                    targetExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(spacing.sm))

                Text(
                    text = "times per week",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = "Recommended: 3-7 servings per week for most categories",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(spacing.xl))

            // Save Button
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSaveNutritionGoal
            ) {
                Text(
                    text = "SAVE GOAL",
                    modifier = Modifier.padding(vertical = spacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(spacing.lg))
        }
    }
}
