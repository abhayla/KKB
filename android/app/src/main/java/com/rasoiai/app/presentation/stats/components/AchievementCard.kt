package com.rasoiai.app.presentation.stats.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.Achievement
import java.time.LocalDate

@Composable
fun AchievementsSection(
    achievements: List<Achievement>,
    onViewAllClick: () -> Unit,
    onShareAchievement: ((Achievement) -> Unit)? = null,
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
                text = "ACHIEVEMENTS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
                onClick = onViewAllClick,
                modifier = Modifier.testTag(TestTags.ACHIEVEMENTS_VIEW_ALL)
            ) {
                Text("View All")
            }
        }

        // Horizontal scroll of achievements
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            contentPadding = PaddingValues(vertical = spacing.xs)
        ) {
            items(
                items = achievements.filter { it.isUnlocked },
                key = { it.id }
            ) { achievement ->
                AchievementCard(
                    achievement = achievement,
                    onShareClick = onShareAchievement?.let { { it(achievement) } }
                )
            }
        }
    }
}

@Composable
fun AchievementCard(
    achievement: Achievement,
    onShareClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(90.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Emoji
                Text(
                    text = if (achievement.isUnlocked) achievement.emoji else "\uD83D\uDD12",
                    fontSize = 28.sp
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                // Name
                Text(
                    text = achievement.displayText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (achievement.isUnlocked) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )

                // Share button for unlocked achievements
                if (achievement.isUnlocked && onShareClick != null) {
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share achievement",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onShareClick() },
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AchievementsSectionPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AchievementsSection(
                achievements = listOf(
                    Achievement(
                        id = "1",
                        name = "First Meal",
                        description = "Cook your first meal",
                        emoji = "\uD83C\uDFC5",
                        isUnlocked = true,
                        unlockedDate = LocalDate.now()
                    ),
                    Achievement(
                        id = "2",
                        name = "7-Day Streak",
                        description = "Cook for 7 days",
                        emoji = "\uD83E\uDD47",
                        isUnlocked = true,
                        unlockedDate = LocalDate.now()
                    ),
                    Achievement(
                        id = "3",
                        name = "Master Chef",
                        description = "Cook 25 recipes",
                        emoji = "\uD83D\uDC68\u200D\uD83C\uDF73",
                        isUnlocked = true,
                        unlockedDate = LocalDate.now()
                    ),
                    Achievement(
                        id = "4",
                        name = "50 Meals",
                        description = "Cook 50 meals",
                        emoji = "\uD83C\uDF1F",
                        isUnlocked = true,
                        unlockedDate = LocalDate.now()
                    )
                ),
                onViewAllClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AchievementCardPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AchievementCard(
                    achievement = Achievement(
                        id = "1",
                        name = "First Meal",
                        description = "Cook your first meal",
                        emoji = "\uD83C\uDFC5",
                        isUnlocked = true
                    )
                )
                AchievementCard(
                    achievement = Achievement(
                        id = "2",
                        name = "Locked",
                        description = "Not unlocked yet",
                        emoji = "\uD83D\uDD12",
                        isUnlocked = false
                    )
                )
            }
        }
    }
}
