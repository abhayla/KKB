package com.rasoiai.app.presentation.reciperules.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rasoiai.app.presentation.reciperules.RulesTab

@Composable
fun RulesTabBar(
    selectedTab: RulesTab,
    onTabSelected: (RulesTab) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
            if (selectedTab.ordinal < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        RulesTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = "${tab.emoji} ${tab.title}",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
