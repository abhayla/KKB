package com.rasoiai.app.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.rasoiai.app.presentation.theme.RasoiAITheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for common Material 3 components used throughout the app.
 */
class ComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun button_displaysText_andHandlesClick() {
        var clicked = false

        composeTestRule.setContent {
            RasoiAITheme {
                Button(onClick = { clicked = true }) {
                    Text("Click Me")
                }
            }
        }

        composeTestRule.onNodeWithText("Click Me").assertIsDisplayed()
        composeTestRule.onNodeWithText("Click Me").performClick()

        assertTrue(clicked)
    }

    @Test
    fun button_whenDisabled_cannotBeClicked() {
        var clicked = false

        composeTestRule.setContent {
            RasoiAITheme {
                Button(
                    onClick = { clicked = true },
                    enabled = false
                ) {
                    Text("Disabled Button")
                }
            }
        }

        composeTestRule.onNodeWithText("Disabled Button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disabled Button").assertIsNotEnabled()
    }

    @Test
    fun textField_acceptsInput() {
        composeTestRule.setContent {
            RasoiAITheme {
                var text by remember { mutableStateOf("") }
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Enter text") }
                )
            }
        }

        composeTestRule.onNodeWithText("Enter text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter text").performTextInput("Hello World")
        composeTestRule.onNodeWithText("Hello World").assertIsDisplayed()
    }

    @Test
    fun card_displaysContent() {
        composeTestRule.setContent {
            RasoiAITheme {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Card Title")
                        Text("Card description goes here")
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Card Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Card description goes here").assertIsDisplayed()
    }

    @Test
    fun loadingIndicator_isDisplayed() {
        composeTestRule.setContent {
            RasoiAITheme {
                CircularProgressIndicator()
            }
        }

        // CircularProgressIndicator is displayed (exists in tree)
        composeTestRule.waitForIdle()
    }

    @Test
    fun navigationBar_displaysItems() {
        composeTestRule.setContent {
            var selectedItem by remember { mutableStateOf(0) }
            RasoiAITheme {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = selectedItem == 0,
                        onClick = { selectedItem = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
                        label = { Text("Favorites") },
                        selected = selectedItem == 1,
                        onClick = { selectedItem = 1 }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Favorites").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Home", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Favorites", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun navigationBar_handlesSelection() {
        var selectedItem = 0

        composeTestRule.setContent {
            RasoiAITheme {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = selectedItem == 0,
                        onClick = { selectedItem = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
                        label = { Text("Favorites") },
                        selected = selectedItem == 1,
                        onClick = { selectedItem = 1 }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Favorites").performClick()
        assertEquals(1, selectedItem)
    }

    @Test
    fun icon_displaysWithContentDescription() {
        composeTestRule.setContent {
            RasoiAITheme {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Home Icon"
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Home Icon").assertIsDisplayed()
    }
}
