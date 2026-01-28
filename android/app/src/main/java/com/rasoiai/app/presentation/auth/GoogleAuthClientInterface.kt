package com.rasoiai.app.presentation.auth

import android.content.Context
import com.google.firebase.auth.FirebaseUser

/**
 * Interface for Google Sign-In client.
 * Allows swapping real implementation with fake for testing.
 */
interface GoogleAuthClientInterface {
    /**
     * The currently signed-in user, or null if not signed in.
     */
    val currentUser: FirebaseUser?

    /**
     * Whether a user is currently signed in.
     */
    val isSignedIn: Boolean

    /**
     * Initiates Google Sign-In flow.
     *
     * @param activityContext Activity context required for Credential Manager
     * @param webClientId OAuth 2.0 Web Client ID from Firebase Console
     * @return Result of the sign-in attempt
     */
    suspend fun signIn(
        activityContext: Context,
        webClientId: String
    ): GoogleSignInResult

    /**
     * Signs out the current user.
     */
    suspend fun signOut()
}
