# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Project Status

Splash and Auth screens are IMPLEMENTED. Firebase is configured. Ready for Onboarding.

| Phase | Status | Document |
|-------|--------|----------|
| Requirements | ✅ Complete | `docs/requirements/RasoiAI Requirements.md` |
| Technical Design | ✅ Complete | `docs/design/RasoiAI Technical Design.md` |
| Architecture Decisions | ✅ Complete | `docs/design/Android Architecture Decisions.md` |
| Design System | ✅ Complete | `docs/design/RasoiAI Design System.md` |
| Screen Wireframes | ✅ Complete | `docs/design/RasoiAI Screen Wireframes.md` |
| Android Project Setup | ✅ Complete | `android/` folder |
| Pre-Dev Infrastructure | ✅ Complete | CI/CD, Testing, Firebase, Logging |
| Splash Screen | ✅ Complete | `presentation/splash/` |
| Auth Screen | ✅ Complete | `presentation/auth/` |
| Firebase Setup | ✅ Complete | `google-services.json` added |
| **Onboarding** | ⏳ **Next Step** | 5-step flow |

## Screens Implemented

| Screen | Files | Status |
|--------|-------|--------|
| Splash | `SplashScreen.kt`, `SplashViewModel.kt`, `AppLogo.kt` | ✅ Complete |
| Auth | `AuthScreen.kt`, `AuthViewModel.kt`, `GoogleAuthClient.kt` | ✅ Complete |
| Onboarding | - | ⏳ Next |

## Firebase Configuration (Complete)

| Item | Status | Details |
|------|--------|---------|
| Firebase Project | ✅ | `rasoiai-6dcdd` |
| google-services.json | ✅ | In `android/app/` |
| Web Client ID | ✅ | In `BuildConfig.WEB_CLIENT_ID` |
| Google Sign-In | ✅ | Enabled in Firebase Console |
| SHA-1 Fingerprint | ⚠️ **PENDING** | Need to add for Google Sign-In to work |

## IMPORTANT: Add SHA-1 Before Testing Auth

Run this command to get your SHA-1:
```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android
```

Then add it in Firebase Console:
1. Go to Project Settings → Your apps → Android app
2. Click "Add fingerprint"
3. Paste the SHA1 value

## Key Documents to Read

1. **CLAUDE.md** (root) - Project overview, architecture summary
2. **Screen Wireframes** (`docs/design/RasoiAI Screen Wireframes.md`) - Onboarding design (5 steps)
3. **Architecture Decisions** - Tech stack & patterns

## Your Task

**Implement the Onboarding Screen** (5 steps):
1. Family Size - How many people in family
2. Dietary Preferences - Veg/Non-veg/Vegan/Jain etc.
3. Cuisine Preferences - North/South/East/West Indian
4. Allergies & Restrictions - Common allergens
5. Cooking Skill Level - Beginner/Intermediate/Expert

Requirements:
- Use dropdowns for selections (as per wireframe)
- Store preferences in DataStore
- Navigate to Home after completion
- Support back navigation between steps

## Working Directory
Project root: `D:/Abhay/VibeCoding/KKB`

Start by reading the Screen Wireframes for Onboarding design, then implement it.
```

---

## QUICK START PROMPT (Shorter Version):

```
I'm building **RasoiAI** - an AI meal planning app for Indian families.

**COMPLETED:**
- ✅ Splash Screen (logo, tagline, offline banner, navigation)
- ✅ Auth Screen (Google Sign-In with Credential Manager + Firebase)
- ✅ Firebase configured (google-services.json, Web Client ID)

**Auth Implementation Details:**
- `GoogleAuthClient.kt` - Handles Credential Manager + Firebase Auth
- `AuthViewModel.kt` - Manages sign-in state, uses `BuildConfig.WEB_CLIENT_ID`
- `AuthScreen.kt` - UI with Google button, terms/privacy links
- `FirebaseModule.kt` - Hilt module providing FirebaseAuth

**PENDING:** Add SHA-1 fingerprint in Firebase Console for Google Sign-In to work on device.

**NEXT STEP:** Implement Onboarding (5 steps)

**Read:** `docs/design/RasoiAI Screen Wireframes.md` for Onboarding design

**Key Files:**
- `android/app/src/main/java/com/rasoiai/app/presentation/auth/` - Auth implementation
- `android/app/src/main/java/com/rasoiai/app/presentation/splash/` - Splash implementation

Implement the 5-step Onboarding flow with DataStore persistence.
```

---

## FILES TO REFERENCE:

| File | Path | Priority | Description |
|------|------|----------|-------------|
| Project Guide | `CLAUDE.md` | **HIGH** | Project overview, all summaries & decisions |
| Screen Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | **HIGH** | Onboarding design (5 steps) |
| Architecture Decisions | `docs/design/Android Architecture Decisions.md` | **HIGH** | Tech stack, code patterns |
| Auth Implementation | `app/presentation/auth/` | **HIGH** | Reference for screen patterns |
| Splash Implementation | `app/presentation/splash/` | MEDIUM | Reference for ViewModel patterns |
| Design System | `docs/design/RasoiAI Design System.md` | MEDIUM | Colors, typography, Theme.kt code |

---

## PROJECT STATUS:

| Phase | Status | Notes |
|-------|--------|-------|
| Ollie.ai Research | ✅ Complete | Reference material |
| RasoiAI Requirements | ✅ Complete | India-specific PRD |
| Technical Design | ✅ Complete | Architecture, DB, APIs |
| Architecture Decisions | ✅ Complete | Kotlin, Hilt, KSP, Compose |
| Design System | ✅ Complete | Colors, typography, theme code |
| Screen Wireframes | ✅ Complete | 12 screens approved (v2.0) |
| Android Project Setup | ✅ Complete | Multi-module, Gradle, Hilt, Theme |
| Pre-Dev Infrastructure | ✅ Complete | CI/CD, Testing, Firebase, Logging |
| **Splash Screen** | ✅ **Complete** | Logo, tagline, offline banner |
| **Auth Screen** | ✅ **Complete** | Google Sign-In with Credential Manager |
| **Firebase Setup** | ✅ **Complete** | google-services.json, Web Client ID |
| **Onboarding** | ⏳ **Next Step** | 5-step preference collection |

---

## IMPLEMENTED SCREENS:

### 1. Splash Screen (`presentation/splash/`)

| File | Description |
|------|-------------|
| `SplashScreen.kt` | Main composable with logo, tagline, loading indicator, offline banner |
| `SplashViewModel.kt` | Handles 2s delay, auth check, navigation events, network monitoring |
| `components/AppLogo.kt` | Custom cooking pot logo drawn with Canvas |

Features:
- Custom cooking pot logo with steam animation
- "RasoiAI" branding + tagline
- Circular loading indicator
- Offline banner (shows when no network)
- Navigation: Auth (not logged in) → Onboarding (logged in, not onboarded) → Home

### 2. Auth Screen (`presentation/auth/`)

| File | Description |
|------|-------------|
| `AuthScreen.kt` | UI with logo, welcome text, Google button, terms/privacy |
| `AuthViewModel.kt` | Manages sign-in flow, error handling, navigation |
| `GoogleAuthClient.kt` | Credential Manager + Firebase Auth integration |

Features:
- Google Sign-In button with loading state
- Error handling with Snackbar
- Terms of Service / Privacy Policy links
- Navigation: → Onboarding (new user) or → Home (returning user)

### 3. DI Module (`di/`)

| File | Description |
|------|-------------|
| `FirebaseModule.kt` | Provides FirebaseAuth instance via Hilt |

---

## FIREBASE SETUP (Complete):

| Item | Value/Status |
|------|--------------|
| Project ID | `rasoiai-6dcdd` |
| Project Number | `1016523916534` |
| Package Name | `com.rasoiai.app` |
| App ID | `1:1016523916534:android:0de2c6d0930c38508c58d7` |
| Web Client ID | `1016523916534-tiop62vjrd3ak3sh91ru76bj8p04v49f.apps.googleusercontent.com` |
| google-services.json | ✅ In `android/app/` |
| Google Sign-In Provider | ✅ Enabled |
| SHA-1 Fingerprint | ⚠️ **NOT ADDED** - Required for sign-in to work |

### To Add SHA-1:

1. Get SHA-1 from debug keystore:
```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android
```

2. In Firebase Console → Project Settings → Your apps → Android
3. Click "Add fingerprint" → Paste SHA1 value

---

## ONBOARDING DESIGN (From Wireframes):

### 5-Step Flow:

| Step | Title | Input Type | Options |
|------|-------|------------|---------|
| 1 | Family Size | Dropdown | 1-8+ members |
| 2 | Dietary Preferences | Multi-select | Vegetarian, Non-veg, Vegan, Jain, Sattvic, Eggetarian |
| 3 | Cuisine Preferences | Multi-select | North Indian, South Indian, East Indian, West Indian |
| 4 | Allergies | Multi-select | Nuts, Dairy, Gluten, Shellfish, None |
| 5 | Cooking Skill | Single select | Beginner, Intermediate, Expert |

### UI Elements:
- Progress indicator (step X of 5)
- Back button (except step 1)
- Next/Finish button
- Skip option (optional)

### Data Storage:
- Use DataStore Preferences
- Create `UserPreferencesRepository` in data layer

---

## APP SCREENS (12 Total):

| # | Screen | Implementation | Key Features |
|---|--------|----------------|--------------|
| 1 | Splash | ✅ **DONE** | Logo, loading, offline banner |
| 2 | Auth | ✅ **DONE** | Google OAuth |
| 3 | Onboarding | ⏳ **NEXT** | 5 steps with dropdowns |
| 4 | Home | ⏳ Pending | 4 meal types, recipes, lock/swap |
| 5 | Recipe Detail | ⏳ Pending | Tabs (Ingredients/Instructions) |
| 6 | Cooking Mode | ⏳ Pending | Full-screen steps, timer |
| 7 | Grocery List | ⏳ Pending | Categorized, WhatsApp share |
| 8 | Favorites | ⏳ Pending | 2-column grid, reorder |
| 9 | Chat | ⏳ Pending | History, time-based actions |
| 10 | Pantry Scan | ⏳ Pending | Expiry tracking |
| 11 | Stats | ⏳ Pending | Leaderboards, challenges |
| 12 | Settings | ⏳ Pending | Profile, family, preferences |

---

## DESIGN SYSTEM QUICK REFERENCE:

| Element | Light Mode | Dark Mode |
|---------|------------|-----------|
| Primary | `#FF6838` | `#FFB59C` |
| Secondary | `#5A822B` | `#A8D475` |
| Background | `#FDFAF4` | `#1C1B1F` |
| Surface | `#FFFFFF` | `#2B2930` |

| Token | Value |
|-------|-------|
| Typography | Roboto (System Default) |
| Spacing | 8dp grid (4, 8, 16, 24, 32, 48dp) |
| Shapes | Rounded (8dp small, 16dp medium, 24dp large) |

---

*Last Updated: January 2025*
*Project: RasoiAI - AI Meal Planning for Indian Families*
*Next Step: Onboarding Screen (5 steps)*
