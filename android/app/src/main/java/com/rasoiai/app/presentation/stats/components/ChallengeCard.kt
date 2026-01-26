package com.rasoiai.app.presentation.stats.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.WeeklyChallenge

@Composable
fun ChallengeCard(
    challenge: WeeklyChallenge,
    onJoinClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            // Header with challenge name and join button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "THIS WEEK'S CHALLENGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(spacing.xs))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "\uD83C\uDFC6",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(spacing.xs))
                        Text(
                            text = challenge.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                if (!challenge.isJoined && !challenge.isCompleted) {
                    Button(
                        onClick = onJoinClick,
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text(if (isLoading) "Joining..." else "Join")
                    }
                } else if (challenge.isCompleted) {
                    OutlinedButton(
                        onClick = {},
                        enabled = false
                    ) {
                        Text("\u2713 Done")
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Description
            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(spacing.md))

            // Progress section
            Text(
                text = "Progress: ${challenge.progressText}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            LinearProgressIndicator(
                progress = { challenge.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            // Reward
            Text(
                text = "\uD83C\uDF81 Reward: ${challenge.rewardBadge}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChallengeCardPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChallengeCard(
                challenge = WeeklyChallenge(
                    id = "south-indian",
                    name = "South Indian Week",
                    description = "Cook 5 South Indian dishes",
                    targetCount = 5,
                    currentProgress = 2,
                    rewardBadge = "Explorer Badge",
                    isJoined = false
                ),
                onJoinClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChallengeCardJoinedPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChallengeCard(
                challenge = WeeklyChallenge(
                    id = "south-indian",
                    name = "South Indian Week",
                    description = "Cook 5 South Indian dishes",
                    targetCount = 5,
                    currentProgress = 3,
                    rewardBadge = "Explorer Badge",
                    isJoined = true
                ),
                onJoinClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
