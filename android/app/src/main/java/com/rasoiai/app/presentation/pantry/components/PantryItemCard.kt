package com.rasoiai.app.presentation.pantry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rasoiai.app.presentation.theme.LocalRasoiColors
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.PantryItem

@Composable
fun PantryItemCard(
    item: PantryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rasoiColors = LocalRasoiColors.current
    Card(
        modifier = modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isExpired)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else if (item.isExpiringSoon)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            else
                rasoiColors.surfaceWarm
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emoji
            Text(
                text = item.category.emoji,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            // Name
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            // Expiry with warning icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.expiryDisplayText,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        item.isExpired -> MaterialTheme.colorScheme.error
                        item.isExpiringSoon -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (item.isExpiringSoon || item.isExpired) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Expiring soon",
                        modifier = Modifier.size(12.dp),
                        tint = if (item.isExpired)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun PantryItemCardLarge(
    item: PantryItem,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rasoiColors = LocalRasoiColors.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isExpired)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else if (item.isExpiringSoon)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            else
                rasoiColors.surfaceWarm
        )
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.category.emoji,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.width(spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(spacing.xxs))

                Text(
                    text = "${item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(spacing.xxs))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.expiryDate != null) "Expires: ${item.expiryDisplayText}" else "No expiry",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            item.isExpired -> MaterialTheme.colorScheme.error
                            item.isExpiringSoon -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (item.isExpiringSoon || item.isExpired) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (item.isExpired)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // Remove button
            Text(
                text = "✕",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onRemoveClick)
                    .padding(spacing.sm)
            )
        }
    }
}
