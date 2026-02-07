package com.rasoiai.app.presentation.reciperules.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rasoiai.app.presentation.reciperules.RulesTab
import com.rasoiai.app.presentation.theme.spacing

@Composable
fun EmptyRulesState(
    tabType: RulesTab,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (emoji, title, description, buttonText) = when (tabType) {
        RulesTab.RULES -> listOf(
            "\uD83C\uDF7D\uFE0F",
            "No rules yet",
            "Add rules to customize your meal plans. Include or exclude recipes and ingredients.",
            "+ Add Rule"
        )
        RulesTab.NUTRITION -> listOf(
            "\uD83E\uDD57",
            "No nutrition goals yet",
            "Set weekly targets for food categories to ensure balanced nutrition",
            "+ Add Nutrition Goal"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(spacing.lg))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(spacing.lg))

        Button(onClick = onAddClick) {
            Text(text = buttonText)
        }
    }
}
