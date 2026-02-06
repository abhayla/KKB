package com.rasoiai.app.presentation.reciperules.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rasoiai.app.presentation.reciperules.FrequencyType
import com.rasoiai.app.presentation.reciperules.RecipeRulesUiState
import com.rasoiai.app.presentation.reciperules.RulesTab
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.model.RuleAction
import com.rasoiai.domain.model.RuleEnforcement
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddRuleBottomSheet(
    uiState: RecipeRulesUiState,
    onDismiss: () -> Unit,
    onActionChange: (RuleAction) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectRecipe: (Recipe) -> Unit,
    onSelectIngredient: (String) -> Unit,
    onFrequencyTypeChange: (FrequencyType) -> Unit,
    onFrequencyCountChange: (Int) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onMealSlotChange: (MealType?) -> Unit,
    onEnforcementChange: (RuleEnforcement) -> Unit,
    onSave: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    var frequencyExpanded by remember { mutableStateOf(false) }
    var mealSlotExpanded by remember { mutableStateOf(false) }

    val title = when {
        uiState.isEditing -> "Edit ${uiState.selectedTab.title} Rule"
        else -> "Add ${uiState.selectedTab.title} Rule"
    }

    val isRecipeOrMealSlot = uiState.selectedTab == RulesTab.RECIPE || uiState.selectedTab == RulesTab.MEAL_SLOT
    val showMealSlot = uiState.selectedTab == RulesTab.MEAL_SLOT

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

            // Rule Type (Include/Exclude) - not for meal slot
            if (uiState.selectedTab != RulesTab.MEAL_SLOT) {
                Text(
                    text = "Rule Type:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = uiState.selectedAction == RuleAction.INCLUDE,
                        onClick = { onActionChange(RuleAction.INCLUDE) }
                    )
                    Text(
                        text = "Include this ${uiState.selectedTab.title.lowercase()}",
                        modifier = Modifier.clickable { onActionChange(RuleAction.INCLUDE) }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = uiState.selectedAction == RuleAction.EXCLUDE,
                        onClick = { onActionChange(RuleAction.EXCLUDE) }
                    )
                    Text(
                        text = "Exclude this ${uiState.selectedTab.title.lowercase()}",
                        modifier = Modifier.clickable { onActionChange(RuleAction.EXCLUDE) }
                    )
                }

                Spacer(modifier = Modifier.height(spacing.md))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(spacing.md))
            }

            // Recipe/Ingredient Search
            Text(
                text = if (isRecipeOrMealSlot) "Recipe:" else "Ingredient:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = if (isRecipeOrMealSlot) "Search recipes..." else "Search ingredients..."
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            // Search Results or Suggestions
            if (isRecipeOrMealSlot) {
                val recipes = if (uiState.recipeSearchResults.isNotEmpty()) {
                    uiState.recipeSearchResults
                } else {
                    uiState.popularRecipes
                }

                if (recipes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(spacing.sm))
                    Text(
                        text = if (uiState.recipeSearchResults.isNotEmpty()) "Results:" else "Suggestions:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        recipes.take(6).forEach { recipe ->
                            FilterChip(
                                selected = uiState.selectedTargetId == recipe.id,
                                onClick = { onSelectRecipe(recipe) },
                                label = { Text(recipe.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            } else {
                val ingredients = if (uiState.ingredientSearchResults.isNotEmpty()) {
                    uiState.ingredientSearchResults
                } else {
                    uiState.popularIngredients
                }

                if (ingredients.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(spacing.sm))
                    Text(
                        text = if (uiState.ingredientSearchResults.isNotEmpty()) "Results:" else "Suggestions:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        ingredients.take(8).forEach { ingredient ->
                            FilterChip(
                                selected = uiState.selectedTargetName == ingredient,
                                onClick = { onSelectIngredient(ingredient) },
                                label = { Text(ingredient) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Diet Conflict Warning (Issue #42)
            if (uiState.conflictWarning != null) {
                Spacer(modifier = Modifier.height(spacing.md))

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(spacing.md),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(spacing.sm))
                        Column {
                            Text(
                                text = "Diet Conflict",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = uiState.conflictWarning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(spacing.md))

            // Meal Slot (for meal-slot rules)
            if (showMealSlot) {
                Text(
                    text = "Meal Slot:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                ExposedDropdownMenuBox(
                    expanded = mealSlotExpanded,
                    onExpandedChange = { mealSlotExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedMealSlot?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Select meal",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealSlotExpanded) },
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = mealSlotExpanded,
                        onDismissRequest = { mealSlotExpanded = false }
                    ) {
                        MealType.entries.forEach { mealType ->
                            DropdownMenuItem(
                                text = {
                                    Text(mealType.name.lowercase().replaceFirstChar { it.uppercase() })
                                },
                                onClick = {
                                    onMealSlotChange(mealType)
                                    mealSlotExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.md))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(spacing.md))
            }

            // Frequency
            Text(
                text = "Frequency:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            // Frequency Type Selection
            Column {
                FrequencyType.entries.filter {
                    // Hide "NEVER" for include rules
                    !(uiState.selectedAction == RuleAction.INCLUDE && it == FrequencyType.NEVER)
                }.forEach { type ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = uiState.selectedFrequencyType == type,
                            onClick = { onFrequencyTypeChange(type) }
                        )
                        Text(
                            text = type.displayName,
                            modifier = Modifier.clickable { onFrequencyTypeChange(type) }
                        )

                        // Count selector for "X times per week"
                        if (type == FrequencyType.TIMES_PER_WEEK && uiState.selectedFrequencyType == type) {
                            Spacer(modifier = Modifier.width(spacing.md))
                            ExposedDropdownMenuBox(
                                expanded = frequencyExpanded,
                                onExpandedChange = { frequencyExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = uiState.selectedFrequencyCount.toString(),
                                    onValueChange = {},
                                    modifier = Modifier
                                        .width(80.dp)
                                        .menuAnchor(),
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                                    shape = RoundedCornerShape(8.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = frequencyExpanded,
                                    onDismissRequest = { frequencyExpanded = false }
                                ) {
                                    (1..7).forEach { count ->
                                        DropdownMenuItem(
                                            text = { Text(count.toString()) },
                                            onClick = {
                                                onFrequencyCountChange(count)
                                                frequencyExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Specific Days Selection
            if (uiState.selectedFrequencyType == FrequencyType.SPECIFIC_DAYS) {
                Spacer(modifier = Modifier.height(spacing.sm))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in uiState.selectedDays,
                            onClick = { onToggleDay(day) },
                            label = {
                                Text(day.name.take(3).lowercase().replaceFirstChar { it.uppercase() })
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(spacing.md))

            // Enforcement
            Text(
                text = "Enforcement:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = uiState.selectedEnforcement == RuleEnforcement.REQUIRED,
                    onClick = { onEnforcementChange(RuleEnforcement.REQUIRED) }
                )
                Text(
                    text = "Required (must include)",
                    modifier = Modifier.clickable { onEnforcementChange(RuleEnforcement.REQUIRED) }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = uiState.selectedEnforcement == RuleEnforcement.PREFERRED,
                    onClick = { onEnforcementChange(RuleEnforcement.PREFERRED) }
                )
                Text(
                    text = "Preferred (try to include)",
                    modifier = Modifier.clickable { onEnforcementChange(RuleEnforcement.PREFERRED) }
                )
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            // Save Button - shows warning state if conflict exists (Issue #42)
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSaveRule,
                colors = if (uiState.hasConflict) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(
                    text = if (uiState.hasConflict) "SAVE ANYWAY" else "SAVE RULE",
                    modifier = Modifier.padding(vertical = spacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(spacing.lg))
        }
    }
}
