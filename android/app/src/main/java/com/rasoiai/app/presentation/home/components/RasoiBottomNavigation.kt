package com.rasoiai.app.presentation.home.components

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.navigation.Screen
import com.rasoiai.app.presentation.theme.RasoiAITheme

/**
 * Bottom navigation item data
 */
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Bottom navigation items for RasoiAI
 */
val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Home,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        screen = Screen.Grocery,
        label = "Grocery",
        selectedIcon = Icons.Filled.ShoppingCart,
        unselectedIcon = Icons.Outlined.ShoppingCart
    ),
    BottomNavItem(
        screen = Screen.Chat,
        label = "Chat",
        selectedIcon = Icons.Filled.Chat,
        unselectedIcon = Icons.Outlined.Chat
    ),
    BottomNavItem(
        screen = Screen.Favorites,
        label = "Favs",
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    ),
    BottomNavItem(
        screen = Screen.Stats,
        label = "Stats",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )
)

/**
 * Reusable bottom navigation bar for RasoiAI.
 *
 * Displays 5 tabs: Home, Grocery, Chat, Favorites, Stats
 * Uses Material 3 NavigationBar with custom colors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RasoiBottomNavigation(
    currentScreen: Screen,
    onItemClick: (Screen) -> Unit,
    notificationBadgeCount: Int = 0
) {
    NavigationBar(
        modifier = Modifier.testTag(TestTags.BOTTOM_NAV),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentScreen.route == item.screen.route
            val testTag = when (item.screen) {
                Screen.Home -> TestTags.BOTTOM_NAV_HOME
                Screen.Grocery -> TestTags.BOTTOM_NAV_GROCERY
                Screen.Chat -> TestTags.BOTTOM_NAV_CHAT
                Screen.Favorites -> TestTags.BOTTOM_NAV_FAVORITES
                Screen.Stats -> TestTags.BOTTOM_NAV_STATS
                else -> ""
            }

            NavigationBarItem(
                modifier = Modifier.testTag(testTag),
                selected = isSelected,
                onClick = { onItemClick(item.screen) },
                icon = {
                    val showBadge = item.screen == Screen.Home && notificationBadgeCount > 0
                    if (showBadge) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    modifier = Modifier.testTag(TestTags.NOTIFICATION_BADGE)
                                ) {
                                    Text(
                                        text = if (notificationBadgeCount > 99) "99+" else notificationBadgeCount.toString()
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun RasoiBottomNavigationPreview() {
    RasoiAITheme {
        Surface {
            RasoiBottomNavigation(
                currentScreen = Screen.Home,
                onItemClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RasoiBottomNavigationGrocerySelectedPreview() {
    RasoiAITheme {
        Surface {
            RasoiBottomNavigation(
                currentScreen = Screen.Grocery,
                onItemClick = { }
            )
        }
    }
}
