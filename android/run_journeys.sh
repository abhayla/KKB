#!/bin/bash
# E2E Journey Runner — runs J01-J17 sequentially with progress logging
RESULTS_FILE="D:/Abhay/VibeCoding/KKB/android/journey_results.txt"
PROGRESS_FILE="D:/Abhay/VibeCoding/KKB/android/journey_progress.txt"

> "$RESULTS_FILE"
> "$PROGRESS_FILE"

JOURNEYS=(
  "J01_FirstTimeUserSuite"
  "J02_NewUserFirstMealPlanSuite"
  "J03_CompleteE2EJourneySuite"
  "J04_DailyMealPlanningSuite"
  "J05_WeeklyGroceryShoppingSuite"
  "J06_CookingAMealSuite"
  "J07_ManagingDietaryPrefsSuite"
  "J08_AIMealPlanQualitySuite"
  "J09_FamilyProfileMgmtSuite"
  "J10_ExploringAppFeaturesSuite"
  "J11_CustomizingSettingsSuite"
  "J12_OfflineErrorResilienceSuite"
  "J13_ReturningUserQuickCheckSuite"
  "J14_AIChatRecipeDiscoverySuite"
  "J15_HouseholdSetupSuite"
  "J16_HouseholdMealCollaborationSuite"
  "J17_HouseholdNotificationsSuite"
)

TOTAL=${#JOURNEYS[@]}
PASS=0
FAIL=0
FAIL_LIST=""

for i in "${!JOURNEYS[@]}"; do
  IDX=$((i + 1))
  SUITE="${JOURNEYS[$i]}"
  echo "[$IDX/$TOTAL] RUNNING: $SUITE" > "$PROGRESS_FILE"
  echo "[$IDX/$TOTAL] RUNNING: $SUITE"

  OUTPUT=$(cd D:/Abhay/VibeCoding/KKB/android && ./gradlew :app:connectedDebugAndroidTest \
    --console=plain \
    -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.$SUITE 2>&1)

  EXIT_CODE=$?

  # Extract test counts from output
  TEST_LINE=$(echo "$OUTPUT" | grep -oE "Tests [0-9]+/[0-9]+ completed.*" | tail -1)

  if [ $EXIT_CODE -eq 0 ]; then
    STATUS="PASS"
    PASS=$((PASS + 1))
    echo "[$IDX/$TOTAL] PASS: $SUITE — $TEST_LINE" >> "$RESULTS_FILE"
    echo "[$IDX/$TOTAL] PASS: $SUITE — $TEST_LINE" > "$PROGRESS_FILE"
    echo "[$IDX/$TOTAL] PASS: $SUITE — $TEST_LINE"
  else
    STATUS="FAIL"
    FAIL=$((FAIL + 1))
    FAIL_LIST="$FAIL_LIST $SUITE"
    # Capture last 30 lines of failure output
    FAIL_TAIL=$(echo "$OUTPUT" | tail -30)
    echo "[$IDX/$TOTAL] FAIL: $SUITE — $TEST_LINE" >> "$RESULTS_FILE"
    echo "--- FAILURE OUTPUT ---" >> "$RESULTS_FILE"
    echo "$FAIL_TAIL" >> "$RESULTS_FILE"
    echo "--- END ---" >> "$RESULTS_FILE"
    echo "[$IDX/$TOTAL] FAIL: $SUITE" > "$PROGRESS_FILE"
    echo "[$IDX/$TOTAL] FAIL: $SUITE"
  fi
done

echo "" >> "$RESULTS_FILE"
echo "========== SUMMARY ==========" >> "$RESULTS_FILE"
echo "Total: $TOTAL | Pass: $PASS | Fail: $FAIL" >> "$RESULTS_FILE"
echo "Failed: $FAIL_LIST" >> "$RESULTS_FILE"
echo "DONE: Total=$TOTAL Pass=$PASS Fail=$FAIL" > "$PROGRESS_FILE"
echo "========== DONE: Total=$TOTAL Pass=$PASS Fail=$FAIL =========="
