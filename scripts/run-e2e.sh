#!/usr/bin/env bash
set -euo pipefail

# ═══════════════════════════════════════════════════════════════
#  RasoiAI E2E Test Pipeline
#  Usage: ./scripts/run-e2e.sh [OPTIONS]
#
#  Options:
#    --tier 1|2|3|4|all  Test tier to run (default: all)
#    --suite J01         Run a single journey suite (e.g., J01, J03)
#    --skip-backend      Assume backend already running
#    --skip-emulator     Assume emulator already running
#    --skip-build        Skip APK rebuild (use existing)
#    --skip-cleanup      Skip test data cleanup
#    --cleanup-only      Clean test data and exit
#    --keep-alive        Don't stop backend/emulator after tests
#    --help              Show this help
# ═══════════════════════════════════════════════════════════════

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/e2e-config.sh"

# ─── Defaults ───────────────────────────────────────────────
TIER="all"
SINGLE_SUITE=""
SKIP_BACKEND=false
SKIP_EMULATOR=false
SKIP_BUILD=false
SKIP_CLEANUP=false
CLEANUP_ONLY=false
KEEP_ALIVE=false
BACKEND_PID=""
STARTED_BACKEND=false
STARTED_EMULATOR=false

# ─── Parse Args ─────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --tier)       TIER="$2"; shift 2 ;;
        --suite)      SINGLE_SUITE="$2"; shift 2 ;;
        --skip-backend)   SKIP_BACKEND=true; shift ;;
        --skip-emulator)  SKIP_EMULATOR=true; shift ;;
        --skip-build)     SKIP_BUILD=true; shift ;;
        --skip-cleanup)   SKIP_CLEANUP=true; shift ;;
        --cleanup-only)   CLEANUP_ONLY=true; shift ;;
        --keep-alive)     KEEP_ALIVE=true; shift ;;
        --help|-h)
            head -16 "$0" | tail -14
            exit 0
            ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

# ─── Colors ─────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

step()  { echo -e "\n${BLUE}${BOLD}[$1]${NC} $2"; }
ok()    { echo -e "  ${GREEN}✓${NC} $1"; }
warn()  { echo -e "  ${YELLOW}!${NC} $1"; }
fail()  { echo -e "  ${RED}✗${NC} $1"; }

# ─── Results Tracking ───────────────────────────────────────
declare -a RESULT_SUITE=()
declare -a RESULT_STATUS=()
declare -a RESULT_TIER=()
declare -a RESULT_DURATION=()
PASS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0

# ─── Cleanup Trap ───────────────────────────────────────────
cleanup_on_exit() {
    if [[ "$KEEP_ALIVE" == "false" ]]; then
        if [[ "$STARTED_BACKEND" == "true" && -n "$BACKEND_PID" ]]; then
            kill "$BACKEND_PID" 2>/dev/null && echo -e "\n${YELLOW}Backend stopped (PID $BACKEND_PID)${NC}" || true
        fi
    fi
}
trap cleanup_on_exit EXIT

# ═══════════════════════════════════════════════════════════════
#  STEP 1: PREFLIGHT CHECK
# ═══════════════════════════════════════════════════════════════
step "1/9" "Preflight check"

# Verify ADB
if [[ -x "$ADB" ]]; then
    ok "adb found: $ADB"
else
    # Try PATH
    if command -v adb &>/dev/null; then
        ADB="adb"
        ok "adb found in PATH"
    else
        fail "adb not found. Set ANDROID_HOME or install Android SDK."
        exit 1
    fi
fi

# Verify Gradle
if [[ -x "${ANDROID_DIR}/gradlew" ]]; then
    ok "gradlew found"
else
    fail "gradlew not found at ${ANDROID_DIR}/gradlew"
    exit 1
fi

# Verify backend .env
if [[ -f "${BACKEND_DIR}/.env" ]]; then
    if grep -q "DEBUG=true" "${BACKEND_DIR}/.env" 2>/dev/null || grep -q "DEBUG=True" "${BACKEND_DIR}/.env" 2>/dev/null; then
        ok "backend/.env has DEBUG=true"
    else
        fail "backend/.env must have DEBUG=true for E2E tests"
        exit 1
    fi
else
    fail "backend/.env not found"
    exit 1
fi

# Verify Python
if command -v python &>/dev/null; then
    ok "python found: $(python --version 2>&1)"
else
    fail "python not found"
    exit 1
fi

# ═══════════════════════════════════════════════════════════════
#  STEP 2: PORT CHECK
# ═══════════════════════════════════════════════════════════════
step "2/9" "Port check (${BACKEND_PORT})"

check_port() {
    if command -v netstat &>/dev/null; then
        netstat -ano 2>/dev/null | grep -q ":${BACKEND_PORT}.*LISTENING"
    elif command -v ss &>/dev/null; then
        ss -tlnp 2>/dev/null | grep -q ":${BACKEND_PORT}"
    else
        curl -s --max-time 2 "$HEALTH_URL" &>/dev/null
    fi
}

if check_port; then
    if curl -s --max-time 3 "$HEALTH_URL" 2>/dev/null | grep -q "healthy"; then
        ok "Backend already running on port ${BACKEND_PORT}"
        SKIP_BACKEND=true
    else
        warn "Port ${BACKEND_PORT} occupied by unknown process"
        if [[ "$SKIP_BACKEND" == "false" ]]; then
            fail "Kill the process on port ${BACKEND_PORT} first, or use --skip-backend"
            exit 1
        fi
    fi
else
    ok "Port ${BACKEND_PORT} is free"
fi

# ═══════════════════════════════════════════════════════════════
#  STEP 3: BACKEND START
# ═══════════════════════════════════════════════════════════════
step "3/9" "Backend"

if [[ "$SKIP_BACKEND" == "true" ]]; then
    ok "Skipped (already running or --skip-backend)"
else
    cd "$BACKEND_DIR"

    # Activate venv
    if [[ -f "venv/Scripts/activate" ]]; then
        source venv/Scripts/activate
    elif [[ -f "venv/bin/activate" ]]; then
        source venv/bin/activate
    else
        fail "Backend venv not found"
        exit 1
    fi

    # Start uvicorn in background
    PYTHONPATH=. uvicorn app.main:app --host "$BACKEND_HOST" --port "$BACKEND_PORT" &
    BACKEND_PID=$!
    STARTED_BACKEND=true
    ok "uvicorn started (PID $BACKEND_PID)"

    # Poll health
    elapsed=0
    while [[ $elapsed -lt $HEALTH_TIMEOUT ]]; do
        if curl -s --max-time 2 "$HEALTH_URL" 2>/dev/null | grep -q "healthy"; then
            ok "Backend healthy after ${elapsed}s"
            break
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done

    if [[ $elapsed -ge $HEALTH_TIMEOUT ]]; then
        fail "Backend health check timed out after ${HEALTH_TIMEOUT}s"
        exit 1
    fi

    cd "$PROJECT_ROOT"
fi

# ═══════════════════════════════════════════════════════════════
#  STEP 4: EMULATOR
# ═══════════════════════════════════════════════════════════════
step "4/9" "Emulator"

if [[ "$SKIP_EMULATOR" == "true" ]]; then
    ok "Skipped (--skip-emulator)"
elif "$ADB" devices 2>/dev/null | grep -q "emulator\|device$" | grep -v "List"; then
    DEVICE=$("$ADB" devices | grep -E "emulator|device$" | head -1 | awk '{print $1}')
    ok "Emulator already running: $DEVICE"
else
    # Find emulator binary
    EMULATOR=""
    if [[ -n "${ANDROID_HOME:-}" && -x "${ANDROID_HOME}/emulator/emulator" ]]; then
        EMULATOR="${ANDROID_HOME}/emulator/emulator"
    elif [[ -n "${LOCALAPPDATA:-}" && -x "${LOCALAPPDATA}/Android/Sdk/emulator/emulator.exe" ]]; then
        EMULATOR="${LOCALAPPDATA}/Android/Sdk/emulator/emulator.exe"
    fi

    if [[ -z "$EMULATOR" ]]; then
        fail "Emulator binary not found. Start emulator manually and use --skip-emulator"
        exit 1
    fi

    # Check AVD exists
    if ! "$EMULATOR" -list-avds 2>/dev/null | grep -q "$AVD_NAME"; then
        fail "AVD '$AVD_NAME' not found. Available AVDs:"
        "$EMULATOR" -list-avds 2>/dev/null
        exit 1
    fi

    # Launch emulator
    "$EMULATOR" -avd "$AVD_NAME" -no-snapshot-load -no-audio -no-window &
    STARTED_EMULATOR=true
    ok "Emulator launching: $AVD_NAME"

    # Wait for boot
    "$ADB" wait-for-device
    elapsed=0
    while [[ $elapsed -lt $BOOT_TIMEOUT ]]; do
        if [[ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
            ok "Emulator booted after ${elapsed}s"
            break
        fi
        sleep 3
        elapsed=$((elapsed + 3))
    done

    if [[ $elapsed -ge $BOOT_TIMEOUT ]]; then
        fail "Emulator boot timed out after ${BOOT_TIMEOUT}s"
        exit 1
    fi

    # Disable animations
    "$ADB" shell settings put global window_animation_scale 0
    "$ADB" shell settings put global transition_animation_scale 0
    "$ADB" shell settings put global animator_duration_scale 0
    ok "Animations disabled"
fi

# ═══════════════════════════════════════════════════════════════
#  STEP 5: TEST DATA CLEANUP
# ═══════════════════════════════════════════════════════════════
step "5/9" "Test data cleanup"

if [[ "$SKIP_CLEANUP" == "true" ]]; then
    ok "Skipped (--skip-cleanup)"
else
    # Backend cleanup
    if [[ -f "${BACKEND_DIR}/scripts/cleanup_user.py" ]]; then
        cd "$BACKEND_DIR"
        if [[ -f "venv/Scripts/activate" ]]; then source venv/Scripts/activate; fi
        PYTHONPATH=. python scripts/cleanup_user.py "$TEST_USER_EMAIL" 2>/dev/null && ok "Backend test user cleaned" || warn "Backend cleanup failed (may be first run)"
        cd "$PROJECT_ROOT"
    else
        warn "cleanup_user.py not found, skipping backend cleanup"
    fi

    # Clear app data on emulator
    "$ADB" shell pm clear com.rasoiai.app 2>/dev/null && ok "App data cleared on emulator" || warn "App not installed yet"
fi

if [[ "$CLEANUP_ONLY" == "true" ]]; then
    echo -e "\n${GREEN}${BOLD}Cleanup complete.${NC}"
    exit 0
fi

# ═══════════════════════════════════════════════════════════════
#  STEP 6: BUILD & INSTALL
# ═══════════════════════════════════════════════════════════════
step "6/9" "Build & install"

cd "$ANDROID_DIR"

if [[ "$SKIP_BUILD" == "true" ]]; then
    ok "Skipped build (--skip-build)"
else
    echo "  Building APKs (this takes a few minutes)..."
    ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon -q 2>&1 | tail -3
    ok "APKs built"
fi

# Install (always — even with --skip-build, app may not be installed)
./gradlew :app:installDebug :app:installDebugAndroidTest --no-daemon -q 2>&1 | tail -3
ok "APKs installed on emulator"

cd "$PROJECT_ROOT"

# ═══════════════════════════════════════════════════════════════
#  STEP 7: RUN TESTS
# ═══════════════════════════════════════════════════════════════
step "7/9" "Running E2E tests"

# Build suite list based on --tier and --suite flags
SUITES_TO_RUN=()

if [[ -n "$SINGLE_SUITE" ]]; then
    # Find matching suite
    MATCH="${PKG}.${SINGLE_SUITE}"
    # Try exact match first, then prefix match
    for suite in "${TIER1_SUITES[@]}" "${TIER2_SUITES[@]}" "${TIER3_SUITES[@]}" "${TIER4_SUITES[@]}"; do
        if [[ "$suite" == *"${SINGLE_SUITE}"* ]]; then
            SUITES_TO_RUN+=("$suite")
            break
        fi
    done
    if [[ ${#SUITES_TO_RUN[@]} -eq 0 ]]; then
        fail "Suite '$SINGLE_SUITE' not found"
        exit 1
    fi
else
    case "$TIER" in
        1)   SUITES_TO_RUN=("${TIER1_SUITES[@]}") ;;
        2)   SUITES_TO_RUN=("${TIER2_SUITES[@]}") ;;
        3)   SUITES_TO_RUN=("${TIER3_SUITES[@]}") ;;
        4)   SUITES_TO_RUN=("${TIER4_SUITES[@]}") ;;
        all) SUITES_TO_RUN=("${TIER1_SUITES[@]}" "${TIER2_SUITES[@]}" "${TIER3_SUITES[@]}" "${TIER4_SUITES[@]}") ;;
        *)   fail "Invalid tier: $TIER (use 1, 2, 3, 4, or all)"; exit 1 ;;
    esac
fi

echo "  Running ${#SUITES_TO_RUN[@]} suite(s)..."
echo ""

# Determine tier for display
get_tier() {
    local suite="$1"
    for s in "${TIER1_SUITES[@]}"; do [[ "$s" == "$suite" ]] && echo "1" && return; done
    for s in "${TIER2_SUITES[@]}"; do [[ "$s" == "$suite" ]] && echo "2" && return; done
    for s in "${TIER3_SUITES[@]}"; do [[ "$s" == "$suite" ]] && echo "3" && return; done
    for s in "${TIER4_SUITES[@]}"; do [[ "$s" == "$suite" ]] && echo "4" && return; done
    echo "?"
}

cd "$ANDROID_DIR"

for suite in "${SUITES_TO_RUN[@]}"; do
    display_name="${SUITE_NAMES[$suite]:-$suite}"
    tier=$(get_tier "$suite")
    start_time=$SECONDS

    echo -ne "  [Tier ${tier}] ${display_name}... "

    if timeout "$SUITE_TIMEOUT" ./gradlew :app:connectedDebugAndroidTest \
        -Pandroid.testInstrumentationRunnerArguments.class="$suite" \
        --no-daemon -q 2>&1 | tail -5 > /tmp/e2e_suite_output.txt 2>&1; then
        duration=$((SECONDS - start_time))
        echo -e "${GREEN}PASS${NC} (${duration}s)"
        RESULT_SUITE+=("$display_name")
        RESULT_STATUS+=("PASS")
        RESULT_TIER+=("$tier")
        RESULT_DURATION+=("${duration}s")
        PASS_COUNT=$((PASS_COUNT + 1))
    else
        duration=$((SECONDS - start_time))
        echo -e "${RED}FAIL${NC} (${duration}s)"
        RESULT_SUITE+=("$display_name")
        RESULT_STATUS+=("FAIL")
        RESULT_TIER+=("$tier")
        RESULT_DURATION+=("${duration}s")
        FAIL_COUNT=$((FAIL_COUNT + 1))

        # Capture failure screenshot
        mkdir -p "$SCREENSHOTS_DIR"
        "$ADB" exec-out screencap -p > "${SCREENSHOTS_DIR}/e2e_${display_name// /_}_failure.png" 2>/dev/null || true
    fi
done

cd "$PROJECT_ROOT"

# ═══════════════════════════════════════════════════════════════
#  STEP 8: RESULTS
# ═══════════════════════════════════════════════════════════════
step "8/9" "Results"

# Copy HTML reports
RUN_DATE=$(date +%Y-%m-%d)
REPORT_DIR="${RESULTS_DIR}/${RUN_DATE}"
mkdir -p "$REPORT_DIR"
if [[ -d "${ANDROID_DIR}/app/build/reports/androidTests/connected" ]]; then
    cp -r "${ANDROID_DIR}/app/build/reports/androidTests/connected/"* "$REPORT_DIR/" 2>/dev/null || true
    ok "Reports saved to ${REPORT_DIR}"
fi

# Print summary
TOTAL=$((PASS_COUNT + FAIL_COUNT + SKIP_COUNT))

echo ""
echo -e "${BOLD}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  RasoiAI E2E Test Results — $(date '+%Y-%m-%d %H:%M:%S')${NC}"
echo -e "${BOLD}═══════════════════════════════════════════════════════════${NC}"
echo ""

printf "  %-6s %-35s %-8s %-10s\n" "Tier" "Suite" "Status" "Duration"
printf "  %-6s %-35s %-8s %-10s\n" "------" "-----------------------------------" "--------" "----------"

for i in "${!RESULT_SUITE[@]}"; do
    status="${RESULT_STATUS[$i]}"
    if [[ "$status" == "PASS" ]]; then
        color="$GREEN"
    elif [[ "$status" == "FAIL" ]]; then
        color="$RED"
    else
        color="$YELLOW"
    fi
    printf "  %-6s %-35s ${color}%-8s${NC} %-10s\n" \
        "${RESULT_TIER[$i]}" "${RESULT_SUITE[$i]}" "$status" "${RESULT_DURATION[$i]}"
done

echo ""
echo -e "  ${GREEN}${PASS_COUNT} PASS${NC} | ${RED}${FAIL_COUNT} FAIL${NC} | ${YELLOW}${SKIP_COUNT} SKIP${NC}  (${TOTAL} total)"
echo -e "  Reports: ${REPORT_DIR}"
if [[ $FAIL_COUNT -gt 0 ]]; then
    echo -e "  Screenshots: ${SCREENSHOTS_DIR}/e2e_*_failure.png"
fi
echo -e "${BOLD}═══════════════════════════════════════════════════════════${NC}"

# ═══════════════════════════════════════════════════════════════
#  STEP 9: CLEANUP
# ═══════════════════════════════════════════════════════════════
step "9/9" "Cleanup"

if [[ "$KEEP_ALIVE" == "true" ]]; then
    ok "Skipped (--keep-alive)"
    if [[ -n "$BACKEND_PID" ]]; then
        echo -e "  Backend running on PID $BACKEND_PID — kill manually when done"
    fi
else
    # Backend killed by trap
    if [[ "$STARTED_EMULATOR" == "true" ]]; then
        "$ADB" emu kill 2>/dev/null && ok "Emulator stopped" || true
    fi
    ok "Done"
fi

# Exit with failure code if any tests failed
[[ $FAIL_COUNT -eq 0 ]] && exit 0 || exit 1
