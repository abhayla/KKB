package com.rasoiai.app.presentation.recipedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rasoiai.app.presentation.theme.DietaryColors
import com.rasoiai.app.presentation.theme.spacing

@Composable
fun RecipeHeader(
    name: String,
    imageUrl: String?,
    cuisineText: String,
    totalTimeMinutes: Int,
    servings: Int,
    calories: Int?,
    isVegetarian: Boolean,
    tags: List<String>,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Recipe Image
        RecipeImage(
            imageUrl = imageUrl,
            name = name,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.md))

        // Recipe Name with Dietary Indicator and Lock Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isVegetarian) DietaryColors.Vegetarian else DietaryColors.NonVegetarian)
            )
            Spacer(modifier = Modifier.width(spacing.sm))
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f, fill = false)
            )
            // Lock icon when recipe is locked in meal plan
            if (isLocked) {
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = "\uD83D\uDD12", // 🔒
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Cuisine + Region
        Text(
            text = cuisineText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs)
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        // Quick Info Row
        QuickInfoRow(
            totalTimeMinutes = totalTimeMinutes,
            servings = servings,
            calories = calories,
            modifier = Modifier.padding(horizontal = spacing.md)
        )

        Spacer(modifier = Modifier.height(spacing.md))

        // Dietary Tags
        TagsRow(
            tags = tags,
            isVegetarian = isVegetarian,
            modifier = Modifier.padding(horizontal = spacing.md)
        )
    }
}

@Composable
private fun RecipeImage(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            // Placeholder when no image
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "\uD83C\uDF72",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(spacing.sm))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickInfoRow(
    totalTimeMinutes: Int,
    servings: Int,
    calories: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        QuickInfoItem(
            icon = Icons.Default.Schedule,
            value = "$totalTimeMinutes min",
            label = "Time",
            modifier = Modifier.weight(1f)
        )
        QuickInfoItem(
            icon = Icons.Default.People,
            value = "$servings serv",
            label = "Servings",
            modifier = Modifier.weight(1f)
        )
        QuickInfoItem(
            icon = Icons.Default.LocalFireDepartment,
            value = "${calories ?: "--"} cal",
            label = "Calories",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickInfoItem(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(spacing.sm),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = spacing.sm, horizontal = spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsRow(
    tags: List<String>,
    isVegetarian: Boolean,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        tags.forEach { tag ->
            TagChip(
                text = tag,
                isHighlighted = tag.contains("Vegetarian", ignoreCase = true) ||
                               tag.contains("Vegan", ignoreCase = true)
            )
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(spacing.md),
        color = if (isHighlighted) {
            DietaryColors.Vegetarian.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isHighlighted) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(DietaryColors.Vegetarian)
                )
                Spacer(modifier = Modifier.width(spacing.xs))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (isHighlighted) {
                    DietaryColors.Vegetarian
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
