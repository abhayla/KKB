package com.rasoiai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.rasoiai.app.presentation.RasoiAIApp
import com.rasoiai.app.presentation.theme.RasoiAITheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Test-specific Activity that does NOT use installSplashScreen().
 *
 * The production MainActivity uses installSplashScreen() which triggers
 * SurfaceComposerClient permission checks. On Android emulators, these
 * permissions are often denied, causing the activity to pause before
 * Compose can render - breaking E2E tests with "No compose hierarchies found".
 *
 * This activity is identical to MainActivity except it skips the splash screen
 * API, allowing tests to run reliably on emulators.
 *
 * This is only included in debug builds (not release).
 */
@AndroidEntryPoint
class TestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // NO installSplashScreen() - avoids surface permission issues on emulators
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RasoiAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RasoiAIApp()
                }
            }
        }
    }
}
