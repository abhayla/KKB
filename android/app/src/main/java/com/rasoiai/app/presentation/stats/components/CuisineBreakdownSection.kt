package com.rasoiai.app.presentation.stats.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rasoiai.app.presentation.stats.CuisineBreakdown
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing

// Cuisine colors
private val cuisineColors = mapOf(
    "North" to Color(0xFFFF6838), // Primary orange
    "South" to Color(0xFF5A822B), // Secondary green
    "East" to Color(0xFF2196F3), // Blue
    "West" to Color(0xFF9C27B0)  // Purple
)

@Composable
fun CuisineBreakdownSection(
    breakdown: List<CuisineBreakdown>,
    modifier: Modifier = Modifier
) {
    val total = breakdown.sumOf { it.count }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Text(
            text = "CUISINE BREAKDOWN",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(spacing.md))

        // Bar chart visualization
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            breakdown.forEach { item ->
                Box(
                    modifier = Modifier
                        .weight(item.percentage / 100f)
                        .height(24.dp)
                        .background(cuisineColors[item.cuisine] ?: MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.md))

        // Legend with counts
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            breakdown.forEach { item ->
                CuisineBreakdownItem(
                    cuisine = item.cuisine,
                    count = item.count,
                    percentage = item.percentage,
                    color = cuisineColors[item.cuisine] ?: MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.xs))

        // Total
        Text(
            text = "Total: $total recipes this month",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CuisineBreakdownItem(
    cuisine: String,
    count: Int,
    percentage: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(modifier = Modifier.width(spacing.sm))

        // Cuisine name
        Text(
            text = "$cuisine Indian",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // Count and percentage
        Text(
            text = "$count (${percentage.toInt()}%)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CuisineBreakdownSectionPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CuisineBreakdownSection(
                breakdown = listOf(
                    CuisineBreakdown("North", 18, 40f),
                    CuisineBreakdown("South", 12, 27f),
                    CuisineBreakdown("East", 6, 13f),
                    CuisineBreakdown("West", 9, 20f)
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
