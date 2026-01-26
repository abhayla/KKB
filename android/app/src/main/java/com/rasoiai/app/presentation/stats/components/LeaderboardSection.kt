package com.rasoiai.app.presentation.stats.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.LeaderboardEntry

@Composable
fun LeaderboardSection(
    entries: List<LeaderboardEntry>,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LEADERBOARD",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(onClick = onViewAllClick) {
                Text("View All")
            }
        }

        // Leaderboard card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(spacing.sm)
            ) {
                entries.forEach { entry ->
                    LeaderboardRow(
                        entry = entry,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (entry.isCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank emoji
        Text(
            text = entry.rankEmoji,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.width(spacing.md))

        // User name
        Text(
            text = entry.userName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Normal,
            color = if (entry.isCurrentUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f)
        )

        // Meals count
        Text(
            text = "${entry.mealsCount} meals",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LeaderboardSectionPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LeaderboardSection(
                entries = listOf(
                    LeaderboardEntry(
                        rank = 1,
                        userName = "Anjali M.",
                        mealsCount = 18,
                        isCurrentUser = false
                    ),
                    LeaderboardEntry(
                        rank = 2,
                        userName = "You (Priya)",
                        mealsCount = 15,
                        isCurrentUser = true
                    ),
                    LeaderboardEntry(
                        rank = 3,
                        userName = "Meera S.",
                        mealsCount = 14,
                        isCurrentUser = false
                    )
                ),
                onViewAllClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
