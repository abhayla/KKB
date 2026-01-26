package com.rasoiai.app.presentation.settings.components

import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.MemberType
import com.rasoiai.domain.model.SpecialDietaryNeed

@Composable
fun FamilySection(
    familyMembers: List<FamilyMember>,
    currentUserId: String,
    onEditMemberClick: (String) -> Unit,
    onAddMemberClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Text(
            text = "FAMILY",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                familyMembers.forEachIndexed { index, member ->
                    FamilyMemberRow(
                        member = member,
                        isCurrentUser = member.id == currentUserId,
                        onEditClick = { onEditMemberClick(member.id) }
                    )

                    if (index < familyMembers.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = spacing.md),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                // Add family member row
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = spacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddMemberClick)
                        .padding(spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(spacing.md))
                    Text(
                        text = "Add family member",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FamilyMemberRow(
    member: FamilyMember,
    isCurrentUser: Boolean,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Emoji based on member type
        Text(
            text = when (member.type) {
                MemberType.CHILD -> "\uD83D\uDC67"
                MemberType.SENIOR -> "\uD83D\uDC74"
                MemberType.ADULT -> "\uD83D\uDC64"
            },
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.width(spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isCurrentUser) {
                    Text(
                        text = " (You)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                member.age?.let { age ->
                    if (member.type != MemberType.ADULT) {
                        Text(
                            text = " ($age yrs)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Special needs
            val needsText = member.specialNeeds.joinToString(", ") { it.displayName }
            if (needsText.isNotEmpty()) {
                Text(
                    text = needsText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit ${member.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FamilySectionPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FamilySection(
                familyMembers = listOf(
                    FamilyMember(
                        id = "1",
                        name = "Priya",
                        type = MemberType.ADULT,
                        age = 32,
                        specialNeeds = emptyList()
                    ),
                    FamilyMember(
                        id = "2",
                        name = "Rahul",
                        type = MemberType.ADULT,
                        age = 35,
                        specialNeeds = emptyList()
                    ),
                    FamilyMember(
                        id = "3",
                        name = "Ananya",
                        type = MemberType.CHILD,
                        age = 8,
                        specialNeeds = listOf(SpecialDietaryNeed.NO_SPICY)
                    ),
                    FamilyMember(
                        id = "4",
                        name = "Dadi",
                        type = MemberType.SENIOR,
                        age = 72,
                        specialNeeds = listOf(SpecialDietaryNeed.DIABETIC, SpecialDietaryNeed.SOFT_FOOD)
                    )
                ),
                currentUserId = "1",
                onEditMemberClick = {},
                onAddMemberClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
