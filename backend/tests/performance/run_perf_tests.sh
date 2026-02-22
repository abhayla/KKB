#!/usr/bin/env bash
# RasoiAI Performance Test Runner
#
# Usage:
#   ./tests/performance/run_perf_tests.sh smoke          # Quick sanity (1 user, 60s)
#   ./tests/performance/run_perf_tests.sh load           # Normal load (20 users, 10m)
#   ./tests/performance/run_perf_tests.sh stress         # Find breaking point (50 users, 15m)
#   ./tests/performance/run_perf_tests.sh crud           # CRUD only, no AI cost (50 users, 5m)
#   ./tests/performance/run_perf_tests.sh mealgen        # AI-only focused (3 users, 5m)
#   ./tests/performance/run_perf_tests.sh web            # Open web UI for interactive testing
#
# Prerequisites:
#   - Run from backend/ directory
#   - Backend running: uvicorn app.main:app --reload --port 8000
#   - DEBUG=true in .env (for fake-firebase-token auth)
#   - locust installed: pip install locust

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
PROFILE_DIR="$SCRIPT_DIR/profiles"
REPORT_DIR="$SCRIPT_DIR/reports"

cd "$BACKEND_DIR"

# Ensure reports directory exists
mkdir -p "$REPORT_DIR"

# Check backend is running
echo "Checking backend health..."
if ! curl -s http://localhost:8000/health > /dev/null 2>&1; then
    echo "ERROR: Backend not running at http://localhost:8000"
    echo "Start it with: uvicorn app.main:app --reload --port 8000"
    exit 1
fi
echo "Backend is healthy."

PROFILE="${1:-smoke}"

case "$PROFILE" in
    smoke)
        echo ""
        echo "=== SMOKE TEST ==="
        echo "1 user, 60 seconds — quick sanity check"
        echo "Estimated Gemini cost: ~\$0.002"
        echo ""
        locust -f tests/performance/locustfile.py \
            --config "$PROFILE_DIR/smoke.conf"
        ;;
    load)
        echo ""
        echo "=== AVERAGE LOAD TEST ==="
        echo "20 users, 10 minutes — normal traffic simulation"
        echo "Estimated Gemini cost: ~\$0.03"
        echo ""
        locust -f tests/performance/locustfile.py \
            --config "$PROFILE_DIR/load.conf"
        ;;
    stress)
        echo ""
        echo "=== STRESS TEST ==="
        echo "50 users, 15 minutes — find breaking point"
        echo "Estimated Gemini cost: ~\$0.08"
        echo ""
        locust -f tests/performance/locustfile.py \
            --config "$PROFILE_DIR/stress.conf"
        ;;
    crud)
        echo ""
        echo "=== CRUD-ONLY TEST ==="
        echo "50 users, 5 minutes — no AI calls, \$0 Gemini cost"
        echo ""
        locust -f tests/performance/locustfile.py CRUDOnlyUser \
            --config "$PROFILE_DIR/crud_only.conf"
        ;;
    mealgen)
        echo ""
        echo "=== FOCUSED MEAL GENERATION TEST ==="
        echo "3 users, 5 minutes — AI endpoint isolation"
        echo "Estimated Gemini cost: ~\$0.04"
        echo ""
        locust -f tests/performance/locustfile.py MealGenHeavyUser \
            --config "$PROFILE_DIR/meal_gen_focused.conf"
        ;;
    web)
        echo ""
        echo "=== INTERACTIVE WEB UI ==="
        echo "Open http://localhost:8089 to configure and run tests"
        echo "Press Ctrl+C to stop"
        echo ""
        locust -f tests/performance/locustfile.py \
            --host http://localhost:8000
        ;;
    *)
        echo "Usage: $0 {smoke|load|stress|crud|mealgen|web}"
        echo ""
        echo "Profiles:"
        echo "  smoke   - Quick sanity check (1 user, 60s, ~\$0.002)"
        echo "  load    - Normal traffic (20 users, 10m, ~\$0.03)"
        echo "  stress  - Breaking point (50 users, 15m, ~\$0.08)"
        echo "  crud    - CRUD only, no AI (\$0)"
        echo "  mealgen - AI-only focused (3 users, 5m, ~\$0.04)"
        echo "  web     - Interactive Locust web UI"
        exit 1
        ;;
esac

echo ""
echo "Reports saved to: $REPORT_DIR/"
echo "Open the HTML report for detailed analysis."
