package com.rasoiai.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simple instrumented test without Compose to verify basic test infrastructure.
 */
@RunWith(AndroidJUnit4::class)
class SimpleInstrumentedTest {

    @Test
    fun context_isNotNull() {
        val context: Context = ApplicationProvider.getApplicationContext()
        assertNotNull(context)
    }

    @Test
    fun packageName_isCorrect() {
        val context: Context = ApplicationProvider.getApplicationContext()
        // Test package is com.rasoiai.app.test, app package is com.rasoiai.app
        assertTrue(context.packageName.startsWith("com.rasoiai.app"))
    }
}
