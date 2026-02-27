package com.rasoiai.app.presentation.auth

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Handles phone-based authentication using Firebase Auth.
 *
 * Flow:
 * 1. Send OTP to phone number via PhoneAuthProvider
 * 2. User enters OTP code
 * 3. Verify code → Firebase credential → sign in
 * 4. Get Firebase ID token for backend auth
 */
@Singleton
class PhoneAuthClient @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : PhoneAuthClientInterface {

    override val isSignedIn: Boolean
        get() = firebaseAuth.currentUser != null

    override suspend fun sendOtp(phoneNumber: String, activity: Activity): PhoneAuthResult {
        return suspendCancellableCoroutine { continuation ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-verification (e.g., Google Play Services auto-read SMS)
                    Timber.i("Phone verification auto-completed")
                    // Sign in with the auto-verified credential
                    firebaseAuth.signInWithCredential(credential)
                        .addOnSuccessListener { authResult ->
                            val user = authResult.user
                            if (user != null) {
                                user.getIdToken(false).addOnSuccessListener { tokenResult ->
                                    val idToken = tokenResult?.token
                                    if (idToken != null) {
                                        val userData = SignInUserData(
                                            userId = user.uid,
                                            phoneNumber = user.phoneNumber,
                                            email = user.email,
                                            displayName = user.displayName,
                                            photoUrl = user.photoUrl?.toString(),
                                            firebaseIdToken = idToken
                                        )
                                        if (continuation.isActive) {
                                            continuation.resume(PhoneAuthResult.AutoVerified(userData))
                                        }
                                    } else {
                                        if (continuation.isActive) {
                                            continuation.resume(
                                                PhoneAuthResult.Error("Failed to get authentication token")
                                            )
                                        }
                                    }
                                }.addOnFailureListener { e ->
                                    if (continuation.isActive) {
                                        continuation.resume(
                                            PhoneAuthResult.Error("Failed to get token: ${e.message}", e)
                                        )
                                    }
                                }
                            } else {
                                if (continuation.isActive) {
                                    continuation.resume(
                                        PhoneAuthResult.Error("Sign in succeeded but user is null")
                                    )
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            if (continuation.isActive) {
                                continuation.resume(
                                    PhoneAuthResult.Error("Auto-verification sign in failed: ${e.message}", e)
                                )
                            }
                        }
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    Timber.e(e, "Phone verification failed")
                    if (continuation.isActive) {
                        continuation.resume(
                            PhoneAuthResult.Error("Verification failed: ${e.message}", e)
                        )
                    }
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Timber.i("OTP code sent to $phoneNumber")
                    if (continuation.isActive) {
                        continuation.resume(PhoneAuthResult.CodeSent(verificationId))
                    }
                }
            }

            val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    override suspend fun verifyOtp(verificationId: String, code: String): OtpVerificationResult {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user

            if (user != null) {
                val tokenResult = user.getIdToken(false).await()
                val idToken = tokenResult?.token

                if (idToken != null) {
                    val userData = SignInUserData(
                        userId = user.uid,
                        phoneNumber = user.phoneNumber,
                        email = user.email,
                        displayName = user.displayName,
                        photoUrl = user.photoUrl?.toString(),
                        firebaseIdToken = idToken
                    )
                    OtpVerificationResult.Success(userData)
                } else {
                    OtpVerificationResult.Error("Failed to get authentication token")
                }
            } else {
                OtpVerificationResult.Error("Verification succeeded but user is null")
            }
        } catch (e: Exception) {
            Timber.e(e, "OTP verification failed")
            OtpVerificationResult.Error("Invalid OTP code. Please try again.")
        }
    }

    override suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            Timber.i("User signed out successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error during sign out")
        }
    }
}
