package com.rasoiai.app.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for theme and design system verification.
 */
class ThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rasoiAITheme_appliesCorrectly() {
        composeTestRule.setContent {
            RasoiAITheme {
                Text(text = "Theme Test")
            }
        }

        composeTestRule.onNodeWithText("Theme Test").assertIsDisplayed()
    }

    @Test
    fun rasoiAITheme_darkMode_appliesCorrectly() {
        composeTestRule.setContent {
            RasoiAITheme(darkTheme = true) {
                Text(text = "Dark Theme Test")
            }
        }

        composeTestRule.onNodeWithText("Dark Theme Test").assertIsDisplayed()
    }

    @Test
    fun rasoiAITheme_lightMode_appliesCorrectly() {
        composeTestRule.setContent {
            RasoiAITheme(darkTheme = false) {
                Text(text = "Light Theme Test")
            }
        }

        composeTestRule.onNodeWithText("Light Theme Test").assertIsDisplayed()
    }

    @Test
    fun materialTheme_colorScheme_isAccessible() {
        composeTestRule.setContent {
            RasoiAITheme {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Primary Color",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Primary Color").assertIsDisplayed()
    }

    @Test
    fun materialTheme_typography_isAccessible() {
        composeTestRule.setContent {
            RasoiAITheme {
                Text(
                    text = "Title Text",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        composeTestRule.onNodeWithText("Title Text").assertIsDisplayed()
    }
}
