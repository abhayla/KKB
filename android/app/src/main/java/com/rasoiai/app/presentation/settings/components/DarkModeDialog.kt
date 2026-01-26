package com.rasoiai.app.presentation.settings.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.DarkModePreference

@Composable
fun DarkModeDialog(
    currentPreference: DarkModePreference,
    onPreferenceSelected: (DarkModePreference) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Dark Mode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                DarkModePreference.entries.forEach { preference ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPreferenceSelected(preference) }
                            .padding(vertical = spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preference == currentPreference,
                            onClick = { onPreferenceSelected(preference) }
                        )
                        Spacer(modifier = Modifier.width(spacing.sm))
                        Text(
                            text = preference.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DarkModeDialogPreview() {
    RasoiAITheme {
        DarkModeDialog(
            currentPreference = DarkModePreference.SYSTEM,
            onPreferenceSelected = {},
            onDismiss = {}
        )
    }
}
