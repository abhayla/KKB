package com.rasoiai.data.remote.interceptor

import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that adds Authorization header to API requests.
 * Reads the access token from DataStore and adds it as a Bearer token.
 *
 * Note: runBlocking is used here because OkHttp interceptors are synchronous by design
 * and run on OkHttp's dispatcher threads, not the main thread. This is the standard
 * pattern for accessing suspend functions in OkHttp interceptors.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStoreInterface
) : Interceptor {

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val TOKEN_TYPE = "Bearer"

        // Endpoints that don't require authentication
        private val PUBLIC_ENDPOINTS = listOf(
            "api/v1/auth/firebase"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestPath = originalRequest.url.encodedPath

        // Skip auth for public endpoints
        if (PUBLIC_ENDPOINTS.any { requestPath.contains(it) }) {
            return chain.proceed(originalRequest)
        }

        // Get access token synchronously (required for interceptor)
        val accessToken = runBlocking {
            userPreferencesDataStore.getAccessTokenSync()
        }

        return if (!accessToken.isNullOrEmpty()) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, "$TOKEN_TYPE $accessToken")
                .build()
            // Log only path to avoid exposing sensitive query parameters
            Timber.d("Added auth header to request: $requestPath")
            chain.proceed(authenticatedRequest)
        } else {
            // Log only path to avoid exposing sensitive query parameters
            Timber.w("No access token available for request: $requestPath")
            chain.proceed(originalRequest)
        }
    }
}
