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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing

data class SettingsItem(
    val title: String,
    val value: String? = null,
    val testTag: String? = null,
    val onClick: () -> Unit
)

data class SettingsToggleItem(
    val title: String,
    val subtitle: String? = null,
    val isChecked: Boolean,
    val testTag: String? = null,
    val onToggle: (Boolean) -> Unit
)

@Composable
fun SettingsSection(
    title: String,
    items: List<SettingsItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Text(
            text = title,
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
                items.forEachIndexed { index, item ->
                    SettingsItemRow(
                        title = item.title,
                        value = item.value,
                        testTag = item.testTag,
                        onClick = item.onClick
                    )

                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = spacing.md),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionWithToggles(
    title: String,
    items: List<SettingsItem> = emptyList(),
    toggleItems: List<SettingsToggleItem> = emptyList(),
    sectionTestTag: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (sectionTestTag != null) Modifier.testTag(sectionTestTag) else Modifier)
    ) {
        Text(
            text = title,
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
                val totalItems = items.size + toggleItems.size
                var currentIndex = 0

                items.forEach { item ->
                    SettingsItemRow(
                        title = item.title,
                        value = item.value,
                        testTag = item.testTag,
                        onClick = item.onClick
                    )
                    currentIndex++
                    if (currentIndex < totalItems) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = spacing.md),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                toggleItems.forEach { toggleItem ->
                    SettingsToggleRow(
                        title = toggleItem.title,
                        subtitle = toggleItem.subtitle,
                        isChecked = toggleItem.isChecked,
                        testTag = toggleItem.testTag,
                        onToggle = toggleItem.onToggle
                    )
                    currentIndex++
                    if (currentIndex < totalItems) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = spacing.md),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String?,
    isChecked: Boolean,
    testTag: String? = null,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable { onToggle(!isChecked) }
            .padding(spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(spacing.sm))

        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsItemRow(
    title: String,
    value: String?,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
            .padding(spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(spacing.xs))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate to $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsSectionPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsSection(
                title = "MEAL PREFERENCES",
                items = listOf(
                    SettingsItem("Dietary Restrictions", onClick = {}),
                    SettingsItem("Disliked Ingredients", onClick = {}),
                    SettingsItem("Cuisine Preferences", onClick = {}),
                    SettingsItem("Cooking Time", onClick = {}),
                    SettingsItem("Spice Level", onClick = {})
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsSectionWithValuesPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsSection(
                title = "APP SETTINGS",
                items = listOf(
                    SettingsItem("Notifications", onClick = {}),
                    SettingsItem("Dark Mode", value = "System", onClick = {}),
                    SettingsItem("Units & Measurements", onClick = {})
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
