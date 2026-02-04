# Screen 1: Splash & Authentication

## Summary Table

| ID | Element | Behavior | Status | Test Reference |
|----|---------|----------|--------|----------------|
| SPLASH-001 | Splash Screen | Display on app launch | Implemented | `SplashScreenTest.kt` |
| SPLASH-002 | App Logo | Show RasoiAI branding | Implemented | `SplashScreenTest.kt` |
| SPLASH-003 | Loading Indicator | Show while checking auth | Implemented | `SplashScreenTest.kt` |
| SPLASH-004 | Auth Check | Verify user authentication state | Implemented | `SplashViewModelTest.kt` |
| SPLASH-005 | Navigation: Authenticated | Route to Home if logged in | Implemented | `SplashViewModelTest.kt` |
| SPLASH-006 | Navigation: New User | Route to Auth if not logged in | Implemented | `SplashViewModelTest.kt` |
| SPLASH-007 | Offline Banner | Show if no network | Implemented | `SplashScreenTest.kt` |
| AUTH-001 | Auth Screen | Display login options | Implemented | `AuthScreenTest.kt` |
| AUTH-002 | Google Sign-In Button | Initiate Google OAuth | Implemented | `AuthScreenTest.kt` |
| AUTH-003 | Google OAuth Flow | Complete authentication | Implemented | `AuthViewModelTest.kt` |
| AUTH-004 | Loading State | Show during authentication | Implemented | `AuthScreenTest.kt` |
| AUTH-005 | Error Handling | Display auth errors | Implemented | `AuthScreenTest.kt` |
| AUTH-006 | Terms Link | Navigate to Terms of Service | Implemented | `AuthScreenTest.kt` |
| AUTH-007 | Privacy Link | Navigate to Privacy Policy | Implemented | `AuthScreenTest.kt` |
| AUTH-008 | Navigation: First User | Route to Onboarding after auth | Implemented | `AuthViewModelTest.kt` |
| AUTH-009 | Navigation: Returning User | Route to Home after auth | Implemented | `AuthViewModelTest.kt` |
| AUTH-010 | Token Storage | Save JWT to secure storage | Implemented | `AuthViewModelTest.kt` |

---

## Detailed Requirements

### SPLASH-001: Splash Screen Display

| Field | Value |
|-------|-------|
| **Screen** | Splash |
| **Element** | Full screen splash |
| **Trigger** | App launch |
| **Status** | Implemented |
| **Test** | `SplashScreenTest.kt:splashScreen_displaysCorrectly` |

**Preconditions:**
- App is launched

**Acceptance Criteria:**
- Given: User launches the app
- When: App starts
- Then: Splash screen displays with cream background (#FDFAF4)
- And: App logo is centered
- And: Tagline "AI Meal Planning for Indian Families" is visible
- And: Loading indicator animates

**Design Specs:**
| Element | Specification |
|---------|---------------|
| Background | Cream `#FDFAF4` |
| Logo | Orange `#FF6838` cooking pot icon |
| Duration | 2-3 seconds |

---

### SPLASH-002: App Logo Display

| Field | Value |
|-------|-------|
| **Screen** | Splash |
| **Element** | RasoiAI logo |
| **Trigger** | Screen load |
| **Status** | Implemented |
| **Test** | `SplashScreenTest.kt:logo_isDisplayed` |

**Acceptance Criteria:**
- Given: Splash screen is displayed
- When: Screen renders
- Then: Logo shows cooking pot icon
- And: "RASOIAI" text is displayed below icon
- And: Logo is centered horizontally and vertically

---

### SPLASH-003: Loading Indicator

| Field | Value |
|-------|-------|
| **Screen** | Splash |
| **Element** | Loading spinner |
| **Trigger** | Auth check in progress |
| **Status** | Implemented |
| **Test** | `SplashScreenTest.kt:loadingIndicator_showsDuringAuthCheck` |

**Acceptance Criteria:**
- Given: App is checking authentication state
- When: Auth check is in progress
- Then: Circular progress indicator is visible
- And: "Loading..." text appears below indicator

---

### SPLASH-004: Authentication State Check

| Field | Value |
|-------|-------|
| **Screen** | Splash |
| **Element** | Auth verification logic |
| **Trigger** | Screen load |
| **Status** | Implemented |
| **Test** | `SplashViewModelTest.kt:checkAuthState_verifiesToken` |

**Preconditions:**
- App launched successfully

**Acceptance Criteria:**
- Given: Splash screen is displayed
- When: Screen loads
- Then: ViewModel checks for stored JWT token
- And: If token exists, validates with backend
- And: Sets navigation destination based on result

---

### SPLASH-005: Navigation - Authenticated User

| Field | Value |
|-------|-------|
| **Screen** | Splash |
| **Element** | Navigation event |
| **Trigger** | Valid auth token found |
| **Status** | Implemented |
| **Test** | `SplashViewModelTest.kt:authenticatedUser_navigatesToHome` |

**Preconditions:**
- Valid JWT token in storage
- Token not expired

**Acceptance Criteria:**
- Given: User has valid authentication
- When: Auth check completes
- Then: Navigation event fires for Home screen
- And: Splash screen dismisses

---

### SPLASH-006: Navigation - New/Unauthenticated User

| Field | Value |
|-------|-------|
| **Screen** | Splash |
| **Element** | Navigation event |
| **Trigger** | No valid auth token |
| **Status** | Implemented |
| **Test** | `SplashViewModelTest.kt:unauthenticatedUser_navigatesToAuth` |

**Preconditions:**
- No JWT token stored OR token expired

**Acceptance Criteria:**
- Given: User is not authenticated
- When: Auth check completes
- Then: Navigation event fires for Auth screen
- And: Splash screen dismisses

---

### SPLASH-007: Offline Banner

| Field | Value |
|-------|-------|
| **Screen** | Splash |
| **Element** | Offline indicator |
| **Trigger** | No network connection |
| **Status** | Implemented |
| **Test** | `SplashScreenTest.kt:offlineBanner_showsWhenNoNetwork` |

**Preconditions:**
- Device has no network connectivity

**Acceptance Criteria:**
- Given: Device is offline
- When: Splash screen displays
- Then: Offline banner appears at top
- And: Banner text: "You're offline. Some features may be limited."
- And: App continues to Home if cached data exists

---

### AUTH-001: Auth Screen Display

| Field | Value |
|-------|-------|
| **Screen** | Auth |
| **Element** | Full screen auth |
| **Trigger** | Navigation from Splash |
| **Status** | Implemented |
| **Test** | `AuthScreenTest.kt:authScreen_displaysCorrectly` |

**Acceptance Criteria:**
- Given: User is not authenticated
- When: Auth screen displays
- Then: Logo appears at top
- And: "Welcome!" heading is visible
- And: Google Sign-In button is prominent
- And: Terms and Privacy links are at bottom

---

### AUTH-002: Google Sign-In Button

| Field | Value |
|-------|-------|
| **Screen** | Auth |
| **Element** | Google OAuth button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `AuthScreenTest.kt:googleButton_whenTapped_initiatesOAuth` |

**Preconditions:**
- Auth screen displayed
- Network available

**Acceptance Criteria:**
- Given: User is on Auth screen
- When: User taps "Continue with Google" button
- Then: Google account picker appears
- And: Button shows Google "G" icon
- And: Button has Google-branded styling

---

### AUTH-003: Google OAuth Flow

| Field | Value |
|-------|-------|
| **Screen** | Auth |
| **Element** | OAuth completion |
| **Trigger** | Google account selected |
| **Status** | Implemented |
| **Test** | `AuthViewModelTest.kt:googleOAuth_exchangesTokenWithBackend` |

**Preconditions:**
- User selected Google account

**Acceptance Criteria:**
- Given: User selected a Google account
- When: Google returns OAuth token
- Then: App exchanges token with backend `/api/v1/auth/firebase`
- And: Backend returns JWT token
- And: JWT stored securely in DataStore

---

### AUTH-004: Loading State During Auth

| Field | Value |
|-------|-------|
| **Screen** | Auth |
| **Element** | Loading overlay |
| **Trigger** | Auth in progress |
| **Status** | Implemented |
| **Test** | `AuthScreenTest.kt:loadingState_showsDuringAuth` |

**Acceptance Criteria:**
- Given: User initiated sign-in
- When: Authentication is in progress
- Then: Loading indicator overlays the screen
- And: Google button is disabled
- And: User cannot interact with other elements

---

### AUTH-005: Authentication Error Handling

| Field | Value |
|-------|-------|
| **Screen** | Auth |
| **Element** | Error display |
| **Trigger** | Auth failure |
| **Status** | Implemented |
| **Test** | `AuthScreenTest.kt:error_displaysMessage` |

**Acceptance Criteria:**
- Given: Authentication attempt failed
- When: Error occurs
- Then: Error message displays below button
- And: Google button re-enables
- And: User can retry authentication

**Error Messages:**
| Error Type | Message |
|------------|---------|
| Network Error | "No internet connection. Please try again." |
| Google Cancelled | "Sign in was cancelled." |
| Server Error | "Something went wrong. Please try again." |

---

### AUTH-006: Terms of Service Link

| Field | Value |
|-------|-------|
| **Screen** | Auth |
| **Element** | Terms link |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `AuthScreenTest.kt:termsLink_opensTermsPage` |

**Acceptance Criteria:**
- Given: User is on Auth screen
- When: User taps "Terms of Service"
- Then: Opens Terms of Service in browser/webview

---

### AUTH-007: Privacy Policy Link

| Field | Value |
|-------|-------|
| **Screen** | Auth |
| **Element** | Privacy link |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `AuthScreenTest.kt:privacyLink_opensPrivacyPage` |

**Acceptance Criteria:**
- Given: User is on Auth screen
- When: User taps "Privacy Policy"
- Then: Opens Privacy Policy in browser/webview

---

### AUTH-008: Navigation - First-Time User

| Field | Value |
|-------|-------|
| **Screen** | Auth |
| **Element** | Navigation event |
| **Trigger** | Successful auth, no preferences |
| **Status** | Implemented |
| **Test** | `AuthViewModelTest.kt:firstTimeUser_navigatesToOnboarding` |

**Preconditions:**
- Authentication successful
- User has no saved preferences (first time)

**Acceptance Criteria:**
- Given: User successfully authenticated
- When: Backend indicates no preferences exist
- Then: Navigate to Onboarding screen
- And: Auth screen dismisses

---

### AUTH-009: Navigation - Returning User

| Field | Value |
|-------|-------|
| **Screen** | Auth |
| **Element** | Navigation event |
| **Trigger** | Successful auth, has preferences |
| **Status** | Implemented |
| **Test** | `AuthViewModelTest.kt:returningUser_navigatesToHome` |

**Preconditions:**
- Authentication successful
- User has saved preferences

**Acceptance Criteria:**
- Given: User successfully authenticated
- When: Backend indicates preferences exist
- Then: Navigate to Home screen
- And: Auth screen dismisses

---

### AUTH-010: Secure Token Storage

| Field | Value |
|-------|-------|
| **Screen** | Auth |
| **Element** | Token persistence |
| **Trigger** | Successful authentication |
| **Status** | Implemented |
| **Test** | `AuthViewModelTest.kt:token_storedSecurely` |

**Acceptance Criteria:**
- Given: User successfully authenticated
- When: JWT token received from backend
- Then: Token stored in encrypted DataStore
- And: Token accessible for subsequent API calls
- And: Token persists across app restarts

---

## Implementation Files

| Component | File Path |
|-----------|-----------|
| Splash Screen | `presentation/splash/SplashScreen.kt` |
| Splash ViewModel | `presentation/splash/SplashViewModel.kt` |
| Auth Screen | `presentation/auth/AuthScreen.kt` |
| Auth ViewModel | `presentation/auth/AuthViewModel.kt` |
| Google Auth Client | `data/auth/GoogleAuthClient.kt` |

## Test Files

| Test Type | File Path |
|-----------|-----------|
| Splash UI Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/splash/SplashScreenTest.kt` |
| Splash Unit Tests | `app/src/test/java/com/rasoiai/app/presentation/splash/SplashViewModelTest.kt` |
| Auth UI Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/auth/AuthScreenTest.kt` |
| Auth Unit Tests | `app/src/test/java/com/rasoiai/app/presentation/auth/AuthViewModelTest.kt` |
| E2E Auth Flow | `app/src/androidTest/java/com/rasoiai/app/e2e/flows/AuthFlowTest.kt` |

---

*Requirements derived from wireframes: `01-splash.md`, `02-auth.md`*
