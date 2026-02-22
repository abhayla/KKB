# Flow 22: API Performance Testing — Meal Plan Generation

## Metadata
- **Flow Name:** `api-performance-testing`
- **Goal:** Autonomously test meal plan generation API for correctness, performance, and reliability
- **Preconditions:** Backend running at localhost:8000, DEBUG=true, PostgreSQL accessible
- **Estimated Duration:** 15-30 minutes (full flow); 3 minutes (smoke only)
- **Endpoints Covered:** /health, /auth/firebase, /meal-plans/generate, /meal-plans/current, /recipes/search, /grocery, /users/me, /stats/monthly
- **Depends On:** none
- **State Produced:** Performance reports in `backend/tests/performance/reports/`, test user with meal plans
- **Skip Policy:** Phases A-C mandatory. Phases D-F optional based on depth needed.
- **Failure Policy:** Phase A failure = STOP (infrastructure broken). Phase B failure = investigate, then continue. Phase C-F failures = log and continue.

## Prerequisites

| # | Check | Command | Expected |
|---|-------|---------|----------|
| P1 | Backend running | `curl -s http://localhost:8000/health` | `{"status":"healthy"}` |
| P2 | DEBUG mode enabled | Check backend logs for "DEBUG mode" | DEBUG=true |
| P3 | Locust installed | `pip show locust` | Version 2.20+ |
| P4 | PostgreSQL accessible | Backend starts without DB errors | No connection errors |
| P5 | Gemini API key set | `GOOGLE_AI_API_KEY` in `.env` | Non-empty |

---

## Phase A: Infrastructure Health (2 minutes)

**Purpose:** Verify all API infrastructure before running expensive AI tests.

### Step A1: Health Check
```bash
curl -s http://localhost:8000/health | python -m json.tool
```
- **PASS:** Returns `{"status": "healthy"}` with HTTP 200
- **FAIL:** Backend not running → start with `uvicorn app.main:app --reload`

### Step A2: Authentication
```bash
curl -s -X POST http://localhost:8000/api/v1/auth/firebase \
  -H "Content-Type: application/json" \
  -d '{"firebase_token": "fake-firebase-token"}' | python -m json.tool
```
- **PASS:** Returns JSON with `access_token`, `token_type: "bearer"`
- **FAIL:** Auth service broken → check Firebase config, DEBUG=true

### Step A3: Save JWT for subsequent steps
```bash
export TOKEN=$(curl -s -X POST http://localhost:8000/api/v1/auth/firebase \
  -H "Content-Type: application/json" \
  -d '{"firebase_token": "fake-firebase-token"}' | python -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
echo "Token: ${TOKEN:0:20}..."
```
- **PASS:** TOKEN variable is set (non-empty)
- **FAIL:** Auth endpoint returned error

### Step A4: CRUD Endpoint Baseline
```bash
# All should return 200 or 404 (not 500)
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" http://localhost:8000/api/v1/users/me
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" http://localhost:8000/api/v1/meal-plans/current
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://localhost:8000/api/v1/recipes/search?q=dal&limit=5"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" http://localhost:8000/api/v1/grocery
```
- **PASS:** All return 200 or 404
- **FAIL:** Any returns 500 → check backend logs for errors

### Phase A Gate
```
□ Phase A Gate:
  - Health check passed? → [YES / NO - STOP]
  - Auth token obtained? → [YES / NO - STOP]
  - CRUD endpoints responding? → [YES / NO - STOP]
```

---

## Phase B: Single Meal Generation Test (5-10 minutes)

**Purpose:** Verify meal generation works end-to-end with full validation.

### Step B1: Generate Meal Plan
```bash
WEEK_START=$(python -c "from datetime import date, timedelta; d=date.today(); print((d - timedelta(days=d.weekday())).isoformat())")
echo "Generating meal plan for week starting: $WEEK_START"

time curl -s -X POST http://localhost:8000/api/v1/meal-plans/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"week_start_date\": \"$WEEK_START\"}" \
  --max-time 150 \
  -o /tmp/meal_plan_response.json

echo "Exit code: $?"
```
- **PASS:** HTTP 200, file has valid JSON, completes within 120s
- **FAIL:** Timeout (504) or error → check Gemini API key, backend logs

### Step B2: Structural Validation
```bash
python -c "
import json, sys

with open('/tmp/meal_plan_response.json') as f:
    data = json.load(f)

errors = []

# V1: Has 'days' array
if 'days' not in data:
    errors.append('Missing days array')
    print('FAIL:', errors); sys.exit(1)

# V2: Exactly 7 days
if len(data['days']) != 7:
    errors.append(f'Expected 7 days, got {len(data[\"days\"])}')

# V3: Each day has all 4 meal slots
for i, day in enumerate(data['days']):
    meals = day.get('meals', {})
    for slot in ['breakfast', 'lunch', 'dinner', 'snacks']:
        if slot not in meals:
            errors.append(f'Day {i} ({day.get(\"day_name\",\"?\")}) missing {slot}')
        elif len(meals[slot]) < 1:
            errors.append(f'Day {i} {slot} is empty')

# V4: Count total items
total = sum(len(day.get('meals',{}).get(s,[]))
            for day in data['days']
            for s in ['breakfast','lunch','dinner','snacks'])

# V5: Each item has recipe_name
for i, day in enumerate(data['days']):
    for slot in ['breakfast','lunch','dinner','snacks']:
        for item in day.get('meals',{}).get(slot,[]):
            if not item.get('recipe_name'):
                errors.append(f'Day {i} {slot} item missing recipe_name')

if errors:
    print(f'FAIL: {len(errors)} validation errors:')
    for e in errors:
        print(f'  - {e}')
    sys.exit(1)
else:
    print(f'PASS: {total} items across 7 days, all slots populated')
    print(f'  Plan ID: {data.get(\"id\", \"?\")}')
    print(f'  Week: {data.get(\"week_start_date\")} → {data.get(\"week_end_date\")}')
"
```
- **PASS:** All 7 days have 4 meal slots, each with >= 1 item
- **FAIL:** Missing days/slots → check AI prompt, Gemini response parsing

### Step B3: Content Validation (Diet, Allergens, Dislikes)
```bash
python -c "
import json

with open('/tmp/meal_plan_response.json') as f:
    data = json.load(f)

# Known test user constraints (Sharma family defaults)
ALLERGENS = ['peanut', 'cashew']
DISLIKES = ['karela', 'baingan', 'mushroom']

warnings = []
for day in data['days']:
    for slot in ['breakfast', 'lunch', 'dinner', 'snacks']:
        for item in day.get('meals', {}).get(slot, []):
            name = item.get('recipe_name', '').lower()
            for a in ALLERGENS:
                if a in name:
                    warnings.append(f'ALLERGEN: \"{a}\" in \"{item[\"recipe_name\"]}\" ({day[\"day_name\"]} {slot})')
            for d in DISLIKES:
                if d in name:
                    warnings.append(f'DISLIKE: \"{d}\" in \"{item[\"recipe_name\"]}\" ({day[\"day_name\"]} {slot})')

if warnings:
    print(f'WARNING: {len(warnings)} constraint violations:')
    for w in warnings:
        print(f'  - {w}')
else:
    print('PASS: No allergens or dislikes found in recipe names')
"
```
- **PASS:** Zero allergen/dislike matches
- **WARN:** Dislikes found (soft fail — AI may include variants)
- **FAIL:** Allergens found (hard fail — safety-critical)

### Step B4: Timing Analysis
```bash
python -c "
import json

with open('/tmp/meal_plan_response.json') as f:
    data = json.load(f)

# Analyze prep times
times = []
for day in data['days']:
    for slot in ['breakfast', 'lunch', 'dinner', 'snacks']:
        for item in day.get('meals', {}).get(slot, []):
            t = item.get('prep_time_minutes', 0)
            times.append({'day': day['day_name'], 'slot': slot, 'name': item['recipe_name'], 'time': t})

if times:
    avg = sum(t['time'] for t in times) / len(times)
    max_item = max(times, key=lambda x: x['time'])
    min_item = min(times, key=lambda x: x['time'])
    print(f'Prep time stats ({len(times)} items):')
    print(f'  Average: {avg:.0f} min')
    print(f'  Fastest: {min_item[\"time\"]}min - {min_item[\"name\"]}')
    print(f'  Slowest: {max_item[\"time\"]}min - {max_item[\"name\"]}')
    over_60 = [t for t in times if t['time'] > 60]
    if over_60:
        print(f'  WARNING: {len(over_60)} items over 60 min')
    else:
        print(f'  All items within 60 min limit')
"
```

### Phase B Gate
```
□ Phase B Gate:
  - Meal plan generated successfully? → [YES: ID=___ / NO - investigate]
  - Structural validation passed? → [YES: ___/56 items / NO - investigate]
  - Allergen check passed? → [YES / WARN / FAIL - STOP]
  - Timing within limits? → [YES / WARN]
```

---

## Phase C: Locust Smoke Test (3 minutes)

**Purpose:** Verify the performance harness works and establish baseline metrics.

### Step C1: Run Smoke Test
```bash
cd backend && source venv/Scripts/activate && \
  bash tests/performance/run_perf_tests.sh smoke
```
- **PASS:** Completes without errors, HTML report generated
- **FAIL:** Locust import error → `pip install locust`; auth failure → check DEBUG=true

### Step C2: Review Results
```bash
# Check if report was generated
ls -la backend/tests/performance/reports/smoke_report.html

# Quick stats from CSV
python -c "
import csv
try:
    with open('tests/performance/reports/smoke_stats.csv') as f:
        reader = csv.DictReader(f)
        for row in reader:
            if row.get('Name') == 'Aggregated':
                print(f'Requests:  {row.get(\"Request Count\", \"?\")}')
                print(f'Failures:  {row.get(\"Failure Count\", \"?\")}')
                print(f'Avg (ms):  {row.get(\"Average Response Time\", \"?\")}')
                print(f'p95 (ms):  {row.get(\"95%\", \"?\")}')
                print(f'RPS:       {row.get(\"Requests/s\", \"?\")}')
except FileNotFoundError:
    print('No CSV report found — check Locust output above')
"
```

### Phase C Gate
```
□ Phase C Gate:
  - Smoke test completed? → [YES / NO - fix harness]
  - Error rate < 5%? → [YES / NO - investigate]
  - HTML report generated? → [YES: path / NO]
```

---

## Phase D: CRUD Baseline (5 minutes) — Optional

**Purpose:** Measure FastAPI + PostgreSQL performance without AI costs.

### Step D1: Run CRUD-Only Load Test
```bash
cd backend && locust -f tests/performance/locustfile.py CRUDOnlyUser \
  --config tests/performance/profiles/crud_only.conf
```

### Step D2: Analyze CRUD Performance
```bash
python -c "
import csv
try:
    with open('tests/performance/reports/crud_stats.csv') as f:
        reader = csv.DictReader(f)
        print(f'{\"Endpoint\":<45} {\"Avg(ms)\":>8} {\"p95(ms)\":>8} {\"Fail\":>6}')
        print('-' * 75)
        for row in reader:
            name = row.get('Name', '')
            if name and name != 'Aggregated':
                print(f'{name:<45} {row.get(\"Average Response Time\",\"?\"):>8} {row.get(\"95%\",\"?\"):>8} {row.get(\"Failure Count\",\"?\"):>6}')
        # Print aggregated last
        f.seek(0)
        reader = csv.DictReader(f)
        for row in reader:
            if row.get('Name') == 'Aggregated':
                print('-' * 75)
                print(f'{\"TOTAL\":<45} {row.get(\"Average Response Time\",\"?\"):>8} {row.get(\"95%\",\"?\"):>8} {row.get(\"Failure Count\",\"?\"):>6}')
except FileNotFoundError:
    print('No CRUD report — run Phase D1 first')
"
```

### Performance Targets (CRUD)
| Endpoint | p50 Target | p95 Target | p99 Target |
|----------|-----------|-----------|-----------|
| /health | <50ms | <100ms | <200ms |
| /meal-plans/current | <200ms | <500ms | <1s |
| /recipes/search | <300ms | <800ms | <2s |
| /grocery | <200ms | <500ms | <1s |
| /users/me | <100ms | <300ms | <500ms |

---

## Phase E: Focused Meal Generation Load (8-10 minutes) — Optional

**Purpose:** Measure AI endpoint performance under concurrent load.

**WARNING:** This phase makes ~15-20 Gemini API calls (~$0.04). Ensure API budget allows.

### Step E1: Run Meal Generation Test
```bash
cd backend && locust -f tests/performance/locustfile.py MealGenHeavyUser \
  --config tests/performance/profiles/meal_gen_focused.conf
```

### Step E2: Analyze AI Endpoint Performance
```bash
python -c "
import csv
try:
    with open('tests/performance/reports/meal_gen_stats.csv') as f:
        reader = csv.DictReader(f)
        for row in reader:
            name = row.get('Name', '')
            if 'generate' in name.lower():
                print('=== MEAL GENERATION PERFORMANCE ===')
                print(f'  Requests:     {row.get(\"Request Count\", \"?\")}')
                print(f'  Failures:     {row.get(\"Failure Count\", \"?\")}')
                print(f'  Avg latency:  {row.get(\"Average Response Time\", \"?\")}ms')
                print(f'  Median (p50): {row.get(\"Median Response Time\", \"?\")}ms')
                print(f'  p95:          {row.get(\"95%\", \"?\")}ms')
                print(f'  p99:          {row.get(\"99%\", \"?\")}ms')
                print(f'  Min:          {row.get(\"Min Response Time\", \"?\")}ms')
                print(f'  Max:          {row.get(\"Max Response Time\", \"?\")}ms')
                avg_s = float(row.get('Average Response Time', 0)) / 1000
                print(f'  Avg (seconds): {avg_s:.1f}s')
except FileNotFoundError:
    print('No meal gen report — run Phase E1 first')
"
```

### Performance Targets (Meal Generation)
| Metric | Target | Concern Level |
|--------|--------|---------------|
| p50 | <30s | Green |
| p95 | <60s | Yellow at 60-90s |
| p99 | <120s | Red above 120s (timeout) |
| Error rate | <5% | Red above 10% |
| Timeout (504) rate | <2% | Red above 5% |

---

## Phase F: Full Mixed Load (15 minutes) — Optional

**Purpose:** Simulate realistic app traffic (CRUD + AI mix).

### Step F1: Run Full Load Test
```bash
cd backend && locust -f tests/performance/locustfile.py \
  --config tests/performance/profiles/load.conf
```

### Step F2: Review HTML Report
```bash
# Open in browser
start tests/performance/reports/load_report.html  # Windows
# open tests/performance/reports/load_report.html  # macOS
```

---

## Autonomous Execution Prompt

Copy-paste this prompt into Claude Code for fully autonomous execution:

```
Run the API performance testing flow (flow22) autonomously:

1. Check prerequisites (Phase A): health, auth, CRUD endpoints
2. Generate and validate a single meal plan (Phase B): structure, allergens, timing
3. Run Locust smoke test (Phase C): verify harness, baseline metrics
4. Run CRUD-only load test (Phase D): FastAPI baseline without AI cost
5. Run focused meal generation test (Phase E): AI endpoint performance

For each phase:
- Run the commands from docs/testing/flows/flow22-api-performance-testing.md
- Check the gate criteria before proceeding
- If a phase fails, log the error and investigate before continuing
- Print a summary table at the end

Save all reports to backend/tests/performance/reports/.
Do NOT skip phases A-C. Phases D-E are optional — run them if Phase C passes.

At the end, print:
- Pass/fail for each phase
- Key metrics: avg latency, p95, error rate, total requests
- Any allergen or constraint violations found
- Recommendations for performance improvement
```

---

## Results Template

```
=== FLOW 22: API PERFORMANCE TEST RESULTS ===

Date: YYYY-MM-DD HH:MM
Backend: http://localhost:8000
Profile: [Sharma/Gupta/Reddy/Khan/Jain]

Phase A: Infrastructure Health     [PASS/FAIL]
  - Health endpoint:               [PASS/FAIL]
  - Authentication:                [PASS/FAIL]
  - CRUD endpoints:                [PASS/FAIL] (___/4 responding)

Phase B: Single Generation         [PASS/FAIL]
  - Generation time:               ___s
  - Items generated:               ___/56
  - Structural validation:         [PASS/FAIL]
  - Allergen check:                [PASS/WARN/FAIL]
  - Timing check:                  [PASS/WARN]

Phase C: Smoke Test                [PASS/FAIL/SKIP]
  - Total requests:                ___
  - Error rate:                    ___%
  - Avg latency:                   ___ms

Phase D: CRUD Baseline             [PASS/FAIL/SKIP]
  - Users simulated:               ___
  - Total requests:                ___
  - p95 latency:                   ___ms
  - Error rate:                    ___%

Phase E: Meal Gen Focused          [PASS/FAIL/SKIP]
  - Concurrent users:              ___
  - Generations completed:         ___
  - Avg generation time:           ___s
  - p95 generation time:           ___s
  - Timeout (504) rate:            ___%
  - Allergen violations:           ___

Phase F: Full Mixed Load           [PASS/FAIL/SKIP]
  - Users simulated:               ___
  - Duration:                      ___m
  - Total requests:                ___
  - Error rate:                    ___%

Overall: [PASS/FAIL]
Estimated Gemini cost: $___

Recommendations:
  1. ___
  2. ___
  3. ___
```
