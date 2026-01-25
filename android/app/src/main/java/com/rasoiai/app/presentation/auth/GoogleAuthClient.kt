package com.rasoiai.app.presentation.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a Google Sign-In attempt
 */
sealed class GoogleSignInResult {
    data class Success(val user: FirebaseUser) : GoogleSignInResult()
    data class Error(val message: String, val exception: Exception? = null) : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
}

/**
 * Handles Google Sign-In using Credential Manager and Firebase Auth.
 *
 * Flow:
 * 1. Request Google ID token via Credential Manager
 * 2. Exchange token for Firebase credential
 * 3. Sign in to Firebase
 */
@Singleton
class GoogleAuthClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {
    private val credentialManager = CredentialManager.create(context)

    /**
     * The currently signed-in user, or null if not signed in.
     */
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    /**
     * Whether a user is currently signed in.
     */
    val isSignedIn: Boolean
        get() = currentUser != null

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
    ): GoogleSignInResult {
        return try {
            // Build the Google ID option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            // Build the credential request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Get credentials
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            handleSignInResult(result)
        } catch (e: GetCredentialCancellationException) {
            Timber.d("Google Sign-In cancelled by user")
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Timber.w(e, "No Google credentials available")
            GoogleSignInResult.Error("No Google account found on this device", e)
        } catch (e: GetCredentialException) {
            Timber.e(e, "Failed to get credentials")
            GoogleSignInResult.Error("Sign in failed: ${e.message}", e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during sign in")
            GoogleSignInResult.Error("An unexpected error occurred", e)
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse): GoogleSignInResult {
        return when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken

                        // Sign in to Firebase with the Google ID token
                        signInToFirebase(idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Timber.e(e, "Failed to parse Google ID token")
                        GoogleSignInResult.Error("Failed to parse credentials", e)
                    }
                } else {
                    Timber.w("Unexpected credential type: ${credential.type}")
                    GoogleSignInResult.Error("Unexpected credential type")
                }
            }
            else -> {
                Timber.w("Unexpected credential class: ${credential::class.java}")
                GoogleSignInResult.Error("Unexpected credential type")
            }
        }
    }

    private suspend fun signInToFirebase(idToken: String): GoogleSignInResult {
        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user

            if (user != null) {
                Timber.i("Successfully signed in user: ${user.email}")
                GoogleSignInResult.Success(user)
            } else {
                Timber.w("Firebase auth succeeded but user is null")
                GoogleSignInResult.Error("Sign in succeeded but user data is unavailable")
            }
        } catch (e: Exception) {
            Timber.e(e, "Firebase authentication failed")
            GoogleSignInResult.Error("Authentication failed: ${e.message}", e)
        }
    }

    /**
     * Signs out the current user from both Firebase and Credential Manager.
     */
    suspend fun signOut() {
        try {
            // Sign out from Firebase
            firebaseAuth.signOut()

            // Clear credential state
            credentialManager.clearCredentialState(ClearCredentialStateRequest())

            Timber.i("User signed out successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error during sign out")
        }
    }
}
