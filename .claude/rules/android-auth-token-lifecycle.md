---
description: >
  End-to-end auth token lifecycle on Android — EncryptedSharedPreferences
  (AES256-GCM) primary storage with DataStore fallback, expiry gating,
  and OkHttp AuthInterceptor with public-endpoint skip and runBlocking
  bridge for suspend token fetches.
globs: ["android/data/src/main/java/com/rasoiai/data/local/datastore/SecureTokenStorage.kt", "android/data/src/main/java/com/rasoiai/data/remote/interceptor/*.kt", "android/data/src/main/java/com/rasoiai/data/remote/**/*.kt"]
synthesized: true
private: true
version: "1.0.0"
---

# Android Auth Token Lifecycle

Covers how RasoiAI stores, expires, and attaches Firebase-derived JWTs. This rule
binds two layers: `SecureTokenStorage` (persistence) and `AuthInterceptor`
(outbound request decoration). They MUST evolve together — changing one without
the other leaves tokens orphaned or requests unauthenticated.

## Storage — `SecureTokenStorage`

- MUST use `EncryptedSharedPreferences` with `AES256-GCM` value encryption and
  `AES256_SIV` key encryption. Keys are managed by Android Keystore.
- MUST expose `isAvailable()`, `isTokenExpired()`, `getAccessToken()`,
  `getRefreshToken()`, `getExpiresAt()`, and atomic `saveTokens(...)` /
  `clearTokens()` operations.
- `isTokenExpired()` MUST compare `System.currentTimeMillis()` against
  the stored `expiresAt` epoch millis. No wall-clock timezone math.
- MUST NOT fail hard when the Keystore is unavailable (rooted devices,
  corrupted keys). `isAvailable()` returns `false` and callers fall back
  to `UserPreferencesDataStore`.

## Fallback — `UserPreferencesDataStore`

- Serves as the second-tier token store only when `SecureTokenStorage.isAvailable()`
  is `false`. MUST NOT be the primary store.
- Access token lookup from DataStore MUST use `getAccessTokenSync()` (blocking
  wrapper around the Flow) since OkHttp's interceptor chain is synchronous.

## Interceptor — `AuthInterceptor`

- MUST skip auth header attachment for these public endpoints:
  `/api/v1/auth/firebase` (login), and any other endpoint explicitly listed
  in the `publicPaths` set inside the interceptor. Adding a new public endpoint
  requires editing `publicPaths` — do not skip auth by URL pattern matching
  outside this set.
- MUST read token via: check `secureTokenStorage.isAvailable() && !isTokenExpired()`
  → return secure token → else `runBlocking { userPreferencesDataStore.getAccessTokenSync() }`.
- `runBlocking` is REQUIRED here because `Interceptor.intercept()` is a synchronous
  method and the token fetch is a suspend function. MUST NOT use
  `GlobalScope.launch` or fire-and-forget coroutines inside the interceptor — the
  request would be sent without the header.
- If the resolved token is `null` or empty, MUST pass the request through without
  an `Authorization` header (let the backend reject with 401 and trigger re-auth
  via `AuthRepository`) — MUST NOT attach `"Bearer null"` or `"Bearer "`.

## Typifying snippet

```kotlin
private fun getAccessToken(): String? {
    if (secureTokenStorage.isAvailable() && !secureTokenStorage.isTokenExpired()) {
        val secureToken = secureTokenStorage.getAccessToken()
        if (!secureToken.isNullOrEmpty()) return secureToken
    }
    return runBlocking { userPreferencesDataStore.getAccessTokenSync() }
}
```

## Why this rule exists

A previous regression attached stale tokens to every request because the
interceptor read from DataStore directly without consulting
`isTokenExpired()`. The backend returned 401 for hours until the token
refresh flow eventually ran. The storage+interceptor contract above prevents
recurrence by making expiry the gate, not the token's presence.

## Critical constraints

- MUST NOT log token values. Log only `"authenticated=true/false"` and the
  endpoint path.
- MUST NOT persist a token without also persisting its `expiresAt`. A token
  with no expiry is treated as expired by `isTokenExpired()`.
- MUST clear both stores (`secureTokenStorage.clearTokens()` +
  `userPreferencesDataStore.clearTokens()`) on logout — leaving one populated
  causes "zombie" re-login where the app appears logged in but requests 401.
- MUST NOT bypass this layer by reading Firebase ID tokens directly in
  repositories. Only `AuthRepository.authenticateWithFirebase()` calls the
  auth endpoint; all other callers go through the interceptor.
