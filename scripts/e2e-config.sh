#!/usr/bin/env bash
# E2E Test Pipeline Configuration
# Shared by run-e2e.sh — edit values here, not in the main script.

# ─── Emulator ───────────────────────────────────────────────
AVD_NAME="Pixel_8a_API_34"
EMULATOR_API=34
BOOT_TIMEOUT=240  # seconds to wait for emulator boot (first boot can be slow)

# ─── Backend ────────────────────────────────────────────────
BACKEND_PORT=8000
BACKEND_HOST="0.0.0.0"
HEALTH_TIMEOUT=30  # seconds to wait for /health
HEALTH_URL="http://localhost:${BACKEND_PORT}/health"

# ─── Test User ──────────────────────────────────────────────
TEST_USER_EMAIL="e2e-test@rasoiai.test"
TEST_USER_PHONE="+911111111111"

# ─── Paths ──────────────────────────────────────────────────
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${PROJECT_ROOT}/backend"
ANDROID_DIR="${PROJECT_ROOT}/android"
SCREENSHOTS_DIR="${PROJECT_ROOT}/docs/testing/screenshots"
RESULTS_DIR="${PROJECT_ROOT}/docs/testing/e2e-results"

# ─── ADB ────────────────────────────────────────────────────
if [[ -n "$LOCALAPPDATA" ]]; then
    ADB="${LOCALAPPDATA}/Android/Sdk/platform-tools/adb.exe"
else
    ADB="${ANDROID_HOME:-$HOME/Android/Sdk}/platform-tools/adb"
fi

# ─── Journey Suites ─────────────────────────────────────────
PKG="com.rasoiai.app.e2e.journeys"

TIER1_SUITES=(
    "${PKG}.J01_FirstTimeUserSuite"
    "${PKG}.J03_CompleteE2EJourneySuite"
    "${PKG}.J04_DailyMealPlanningSuite"
)

TIER2_SUITES=(
    "${PKG}.J06_CookingAMealSuite"
    "${PKG}.J07_ManagingDietaryPrefsSuite"
    "${PKG}.J05_WeeklyGroceryShoppingSuite"
)

TIER3_SUITES=(
    "${PKG}.J09_FamilyProfileMgmtSuite"
    "${PKG}.J10_ExploringAppFeaturesSuite"
    "${PKG}.J11_CustomizingSettingsSuite"
    "${PKG}.J13_ReturningUserQuickCheckSuite"
    "${PKG}.J14_AIChatRecipeDiscoverySuite"
)

TIER4_SUITES=(
    "${PKG}.J12_OfflineErrorResilienceSuite"
    "${PKG}.J15_HouseholdSetupSuite"
    "${PKG}.J16_HouseholdMealCollaborationSuite"
    "${PKG}.J17_HouseholdNotificationsSuite"
)

# Suite display names (parallel arrays)
declare -A SUITE_NAMES=(
    ["${PKG}.J01_FirstTimeUserSuite"]="J01 First-Time User"
    ["${PKG}.J03_CompleteE2EJourneySuite"]="J03 Complete E2E Journey"
    ["${PKG}.J04_DailyMealPlanningSuite"]="J04 Daily Meal Planning"
    ["${PKG}.J05_WeeklyGroceryShoppingSuite"]="J05 Weekly Grocery"
    ["${PKG}.J06_CookingAMealSuite"]="J06 Cooking a Meal"
    ["${PKG}.J07_ManagingDietaryPrefsSuite"]="J07 Dietary Preferences"
    ["${PKG}.J09_FamilyProfileMgmtSuite"]="J09 Family Profile"
    ["${PKG}.J10_ExploringAppFeaturesSuite"]="J10 Exploring Features"
    ["${PKG}.J11_CustomizingSettingsSuite"]="J11 Settings"
    ["${PKG}.J12_OfflineErrorResilienceSuite"]="J12 Offline Resilience"
    ["${PKG}.J13_ReturningUserQuickCheckSuite"]="J13 Returning User"
    ["${PKG}.J14_AIChatRecipeDiscoverySuite"]="J14 AI Chat"
    ["${PKG}.J15_HouseholdSetupSuite"]="J15 Household Setup"
    ["${PKG}.J16_HouseholdMealCollaborationSuite"]="J16 Household Collab"
    ["${PKG}.J17_HouseholdNotificationsSuite"]="J17 Household Notifs"
)

# ─── Timeouts ───────────────────────────────────────────────
SUITE_TIMEOUT=600  # 10 min per suite (includes Gradle overhead + Gemini AI calls)
