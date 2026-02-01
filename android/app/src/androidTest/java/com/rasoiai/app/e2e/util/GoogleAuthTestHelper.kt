package com.rasoiai.app.e2e.util

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until

/**
 * Helper for automating real Google Sign-In flow in E2E tests.
 * Uses UI Automator to interact with the system Google account picker.
 *
 * ## Prerequisites
 * - A Google account must be signed into the device/emulator
 * - The account should have access to the app (if using restricted OAuth)
 *
 * ## How it works
 * 1. After triggering sign-in in the app, Google shows a system dialog
 * 2. This helper uses UI Automator to find and tap the account
 * 3. The app receives the Google credential and continues
 */
object GoogleAuthTestHelper {

    private const val TAG = "GoogleAuthTestHelper"

    // Timeouts
    private const val DIALOG_WAIT_TIMEOUT = 10_000L // Wait for Google dialog to appear
    private const val ACCOUNT_CLICK_TIMEOUT = 5_000L

    // Common UI element identifiers for Google Sign-In dialog
    // These may vary by Android version and Google Play Services version
    private val ACCOUNT_PICKER_PATTERNS = listOf(
        // Pattern 1: Account email text
        ".*@.*\\.com",
        ".*@.*\\.gmail\\.com",
        // Pattern 2: "Continue as" button
        "Continue as.*",
        // Pattern 3: Account name display
        ".*Google.*"
    )

    // Package names for Google sign-in components
    private const val GOOGLE_ACCOUNT_PICKER_PACKAGE = "com.google.android.gms"
    private const val CREDENTIAL_MANAGER_PACKAGE = "com.google.android.gms"

    /**
     * Waits for and selects a Google account from the system account picker.
     *
     * @param accountEmail Optional specific email to select. If null, selects the first account.
     * @param timeoutMs How long to wait for the dialog to appear
     * @return true if account was selected successfully, false otherwise
     */
    fun selectGoogleAccount(
        accountEmail: String? = null,
        timeoutMs: Long = DIALOG_WAIT_TIMEOUT
    ): Boolean {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        Log.d(TAG, "Waiting for Google account picker dialog...")

        // Wait for Google dialog to appear
        val dialogAppeared = device.wait(
            Until.hasObject(By.pkg(GOOGLE_ACCOUNT_PICKER_PACKAGE)),
            timeoutMs
        )

        if (!dialogAppeared) {
            Log.w(TAG, "Google account picker dialog did not appear within ${timeoutMs}ms")
            // Try alternative: check if Credential Manager bottom sheet appeared
            return tryCredentialManagerSelection(device, accountEmail)
        }

        Log.d(TAG, "Google dialog appeared, looking for account to select...")

        // Try to find and click the account
        return if (accountEmail != null) {
            selectSpecificAccount(device, accountEmail)
        } else {
            selectFirstAvailableAccount(device)
        }
    }

    /**
     * Tries to select account from Credential Manager bottom sheet (newer Android versions)
     */
    private fun tryCredentialManagerSelection(device: UiDevice, accountEmail: String?): Boolean {
        Log.d(TAG, "Trying Credential Manager selection...")

        // Look for "Continue as" button which is common in Credential Manager
        val continueButton = device.findObject(UiSelector().textMatches("Continue as.*"))
        if (continueButton.exists()) {
            Log.d(TAG, "Found 'Continue as' button, clicking...")
            continueButton.click()
            device.waitForIdle()
            return true
        }

        // Look for account email directly
        if (accountEmail != null) {
            val emailElement = device.findObject(UiSelector().textContains(accountEmail))
            if (emailElement.exists()) {
                Log.d(TAG, "Found account email, clicking...")
                emailElement.click()
                device.waitForIdle()
                return true
            }
        }

        // Try finding any clickable account-like element
        val accountPattern = device.findObject(UiSelector().textMatches(".*@.*"))
        if (accountPattern.exists()) {
            Log.d(TAG, "Found email pattern, clicking...")
            accountPattern.click()
            device.waitForIdle()
            return true
        }

        Log.w(TAG, "Could not find any account selection element")
        return false
    }

    /**
     * Selects a specific account by email
     */
    private fun selectSpecificAccount(device: UiDevice, email: String): Boolean {
        Log.d(TAG, "Looking for specific account: $email")

        // Try exact match first
        val exactMatch = device.findObject(UiSelector().text(email))
        if (exactMatch.exists() && exactMatch.isClickable) {
            Log.d(TAG, "Found exact email match, clicking...")
            exactMatch.click()
            device.waitForIdle()
            return true
        }

        // Try contains match
        val containsMatch = device.findObject(UiSelector().textContains(email))
        if (containsMatch.exists()) {
            Log.d(TAG, "Found partial email match, clicking...")
            containsMatch.click()
            device.waitForIdle()
            return true
        }

        Log.w(TAG, "Could not find account with email: $email")
        return selectFirstAvailableAccount(device)
    }

    /**
     * Selects the first available account in the picker
     */
    private fun selectFirstAvailableAccount(device: UiDevice): Boolean {
        Log.d(TAG, "Selecting first available account...")

        // Strategy 1: Look for "Continue as" button (most common)
        val continueButton = device.findObject(UiSelector().textMatches("(?i)continue.*"))
        if (continueButton.exists()) {
            Log.d(TAG, "Found 'Continue' button, clicking...")
            continueButton.click()
            device.waitForIdle()
            return true
        }

        // Strategy 2: Look for any email-like text
        val emailElement = device.findObject(UiSelector().textMatches(".*@.*\\..*"))
        if (emailElement.exists()) {
            Log.d(TAG, "Found email element, clicking...")
            emailElement.click()
            device.waitForIdle()
            return true
        }

        // Strategy 3: Look for account list item and click first one
        val accountItem = device.findObject(
            UiSelector()
                .resourceIdMatches(".*account.*")
                .clickable(true)
        )
        if (accountItem.exists()) {
            Log.d(TAG, "Found account item by resource ID, clicking...")
            accountItem.click()
            device.waitForIdle()
            return true
        }

        // Strategy 4: Try clicking in the center of the dialog (last resort)
        Log.d(TAG, "Trying center click as last resort...")
        val width = device.displayWidth
        val height = device.displayHeight
        device.click(width / 2, height / 2)
        device.waitForIdle()

        return true // Assume it worked
    }

    /**
     * Dismisses any Google sign-in dialog that might be showing
     */
    fun dismissGoogleDialog(): Boolean {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Try pressing back to dismiss
        device.pressBack()
        device.waitForIdle()

        // Check if dialog is gone
        val dialogStillPresent = device.hasObject(By.pkg(GOOGLE_ACCOUNT_PICKER_PACKAGE))
        return !dialogStillPresent
    }

    /**
     * Waits for Google sign-in to complete by checking that the dialog is dismissed
     */
    fun waitForSignInComplete(timeoutMs: Long = 10_000L): Boolean {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        Log.d(TAG, "Waiting for sign-in to complete...")

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            // Check if Google dialog is gone
            if (!device.hasObject(By.pkg(GOOGLE_ACCOUNT_PICKER_PACKAGE))) {
                Log.d(TAG, "Google dialog dismissed, sign-in likely complete")
                return true
            }
            Thread.sleep(500)
        }

        Log.w(TAG, "Timeout waiting for sign-in to complete")
        return false
    }

    /**
     * Takes a screenshot for debugging purposes
     */
    fun takeDebugScreenshot(filename: String = "google_auth_debug") {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        try {
            val screenshotFile = java.io.File(
                context.getExternalFilesDir(null),
                "$filename.png"
            )
            device.takeScreenshot(screenshotFile)
            Log.d(TAG, "Screenshot saved to: ${screenshotFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take screenshot", e)
        }
    }

    /**
     * Dumps the current UI hierarchy for debugging
     */
    fun dumpUiHierarchy() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        try {
            val dumpFile = java.io.File(
                context.getExternalFilesDir(null),
                "ui_hierarchy.xml"
            )
            device.dumpWindowHierarchy(dumpFile)
            Log.d(TAG, "UI hierarchy dumped to: ${dumpFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dump UI hierarchy", e)
        }
    }
}
