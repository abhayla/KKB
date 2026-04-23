package com.rasoiai.data.local.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure token storage using EncryptedSharedPreferences.
 *
 * Stores auth tokens (access_token, refresh_token, expiry) encrypted at rest
 * using AES256-GCM for values and AES256-SIV for keys. The encryption key is
 * managed by Android Keystore via [MasterKey].
 *
 * This is used as the primary token storage, with the regular DataStore serving
 * as a fallback for backward compatibility during the migration period.
 */
@Singleton
class SecureTokenStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    private companion object {
        const val PREFS_NAME = "secure_auth"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
    }

    private val prefs: SharedPreferences? by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // On some devices (especially older ones or after backup/restore),
            // EncryptedSharedPreferences can fail. Fall back gracefully.
            Timber.e(e, "Failed to initialize EncryptedSharedPreferences — tokens will use DataStore only")
            null
        }
    }

    /**
     * Save all auth tokens to encrypted storage.
     *
     * @param accessToken The JWT access token
     * @param refreshToken The refresh token for obtaining new access tokens
     * @param expiresAt Absolute timestamp (millis since epoch) when the access token expires
     */
    fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        prefs?.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_EXPIRES_AT, expiresAt)
        }
    }

    /**
     * Get the stored access token, or null if not present or encrypted storage unavailable.
     */
    fun getAccessToken(): String? = prefs?.getString(KEY_ACCESS_TOKEN, null)

    /**
     * Get the stored refresh token, or null if not present or encrypted storage unavailable.
     */
    fun getRefreshToken(): String? = prefs?.getString(KEY_REFRESH_TOKEN, null)

    /**
     * Get the token expiry timestamp (millis since epoch), or 0 if not set.
     */
    fun getExpiresAt(): Long = prefs?.getLong(KEY_EXPIRES_AT, 0L) ?: 0L

    /**
     * Check whether the stored access token has expired.
     * Returns true if expired or if no expiry is stored.
     */
    fun isTokenExpired(): Boolean = System.currentTimeMillis() > getExpiresAt()

    /**
     * Returns true if encrypted storage initialized successfully.
     */
    fun isAvailable(): Boolean = prefs != null

    /**
     * Clear all stored tokens from encrypted storage.
     */
    fun clearTokens() {
        prefs?.edit { clear() }
    }
}
