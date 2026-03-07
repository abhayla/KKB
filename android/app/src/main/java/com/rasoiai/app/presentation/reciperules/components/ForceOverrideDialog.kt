package com.rasoiai.app.presentation.reciperules.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rasoiai.domain.model.ConflictDetail

@Composable
fun ForceOverrideDialog(
    conflictDetails: List<ConflictDetail>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Family Safety Conflict",
                modifier = Modifier.testTag("conflict_dialog_title")
            )
        },
        text = {
            Column {
                Text(
                    text = "This rule conflicts with family member health conditions:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                conflictDetails.forEach { detail ->
                    Text(
                        text = "\u2022 ${detail.memberName} has ${detail.condition} \u2014 '${detail.keyword}' found in '${detail.ruleTarget}'",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Are you sure you want to add this rule?",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("conflict_dialog_override")
            ) {
                Text("Override & Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("conflict_dialog_cancel")
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("force_override_dialog")
    )
}
