package com.rasoiai.app.presentation.auth

import android.app.Activity

/**
 * User data returned after successful phone authentication.
 * Contains the Firebase ID token for backend authentication.
 */
data class SignInUserData(
    val userId: String,
    val phoneNumber: String?,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val firebaseIdToken: String
)

/**
 * Result of sending an OTP to a phone number.
 */
sealed class PhoneAuthResult {
    data class CodeSent(val verificationId: String) : PhoneAuthResult()
    data class AutoVerified(val userData: SignInUserData) : PhoneAuthResult()
    data class Error(val message: String, val exception: Exception? = null) : PhoneAuthResult()
}

/**
 * Result of verifying an OTP code.
 */
sealed class OtpVerificationResult {
    data class Success(val userData: SignInUserData) : OtpVerificationResult()
    data class Error(val message: String) : OtpVerificationResult()
}

/**
 * Interface for phone-based authentication client.
 * Allows swapping real implementation with fake for testing.
 */
interface PhoneAuthClientInterface {
    /**
     * Whether a user is currently signed in.
     */
    val isSignedIn: Boolean

    /**
     * Send an OTP to the given phone number.
     *
     * @param phoneNumber Full phone number with country code (e.g., "+911111111111")
     * @param activity Activity required for reCAPTCHA verification
     * @return Result of the OTP send attempt
     */
    suspend fun sendOtp(phoneNumber: String, activity: Activity): PhoneAuthResult

    /**
     * Verify an OTP code against a verification ID.
     *
     * @param verificationId The verification ID from a CodeSent result
     * @param code The 6-digit OTP code entered by the user
     * @return Result of the OTP verification attempt
     */
    suspend fun verifyOtp(verificationId: String, code: String): OtpVerificationResult

    /**
     * Signs out the current user.
     */
    suspend fun signOut()
}
