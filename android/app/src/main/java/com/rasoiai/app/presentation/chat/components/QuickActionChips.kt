package com.rasoiai.app.presentation.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rasoiai.app.presentation.theme.spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionChips(
    actions: List<String>,
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        actions.forEach { action ->
            AssistChip(
                onClick = { onActionClick(action) },
                label = {
                    Text(
                        text = action,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.primary
                ),
                border = AssistChipDefaults.assistChipBorder(
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    enabled = true
                )
            )
        }
    }
}
