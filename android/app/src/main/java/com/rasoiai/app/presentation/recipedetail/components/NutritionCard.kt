package com.rasoiai.app.presentation.recipedetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.Nutrition

@Composable
fun NutritionCard(
    nutrition: Nutrition?,
    servings: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(spacing.md)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            Text(
                text = "NUTRITION PER SERVING",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing * 1.2f
            )

            Spacer(modifier = Modifier.height(spacing.md))

            if (nutrition != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NutritionItem(
                        label = "Calories",
                        value = "${nutrition.calories}",
                        unit = ""
                    )
                    NutritionItem(
                        label = "Protein",
                        value = "${nutrition.proteinGrams}",
                        unit = "g"
                    )
                    NutritionItem(
                        label = "Carbs",
                        value = "${nutrition.carbohydratesGrams}",
                        unit = "g"
                    )
                    NutritionItem(
                        label = "Fat",
                        value = "${nutrition.fatGrams}",
                        unit = "g"
                    )
                }

                Spacer(modifier = Modifier.height(spacing.sm))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(spacing.sm))

                // Additional nutrition info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NutritionItemSmall(
                        label = "Fiber",
                        value = "${nutrition.fiberGrams}g"
                    )
                    NutritionItemSmall(
                        label = "Sugar",
                        value = "${nutrition.sugarGrams}g"
                    )
                    NutritionItemSmall(
                        label = "Sodium",
                        value = "${nutrition.sodiumMg}mg"
                    )
                }
            } else {
                Text(
                    text = "Nutrition information not available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NutritionItem(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = spacing.xxs)
                )
            }
        }
        Spacer(modifier = Modifier.height(spacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NutritionItemSmall(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
