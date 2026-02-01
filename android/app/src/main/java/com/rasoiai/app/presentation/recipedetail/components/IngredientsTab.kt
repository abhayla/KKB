package com.rasoiai.app.presentation.recipedetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.Ingredient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

@Composable
fun IngredientsTab(
    ingredients: ImmutableList<Ingredient>,
    selectedServings: Int,
    checkedIngredients: ImmutableSet<String>,
    onServingsChange: (Int) -> Unit,
    onIngredientChecked: (String) -> Unit,
    onAddAllToGrocery: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep callback reference stable across recompositions
    val currentOnIngredientChecked by rememberUpdatedState(onIngredientChecked)

    Column(modifier = modifier.fillMaxWidth()) {
        // Servings Selector
        ServingsSelector(
            selectedServings = selectedServings,
            onServingsChange = onServingsChange,
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        // Ingredients List with stable key for each item
        ingredients.forEach { ingredient ->
            val onCheckedChange = remember(ingredient.id) {
                { currentOnIngredientChecked(ingredient.id) }
            }
            IngredientItem(
                ingredient = ingredient,
                isChecked = ingredient.id in checkedIngredients,
                onCheckedChange = onCheckedChange
            )
        }

        Spacer(modifier = Modifier.height(spacing.md))

        // Add All to Grocery Button
        OutlinedButton(
            onClick = onAddAllToGrocery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md),
            shape = RoundedCornerShape(spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = spacing.sm)
            )
            Text("Add All to Grocery List")
        }

        Spacer(modifier = Modifier.height(spacing.md))
    }
}

/** Constant list of serving options to avoid recreation */
private val SERVINGS_OPTIONS = (1..12).toList()

@Composable
private fun ServingsSelector(
    selectedServings: Int,
    onServingsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Servings:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = spacing.xs)
        )

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.RECIPE_SERVINGS_SELECTOR),
            shape = RoundedCornerShape(spacing.sm),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedServings servings",
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select servings"
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            SERVINGS_OPTIONS.forEach { servings ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "$servings serving${if (servings > 1) "s" else ""}",
                            fontWeight = if (servings == selectedServings) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onServingsChange(servings)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun IngredientItem(
    ingredient: Ingredient,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCheckedChange)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onCheckedChange() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.width(spacing.sm))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ingredient.displayText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isChecked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
            )

            if (ingredient.isOptional) {
                Text(
                    text = "(optional)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
