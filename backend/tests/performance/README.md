# RasoiAI API Performance Testing

Locust-based performance test harness for the RasoiAI backend API, with special handling for high-latency AI meal generation endpoints.

## Quick Start

```bash
cd backend
source venv/bin/activate  # or venv/Scripts/activate on Windows

# 1. Ensure backend is running
uvicorn app.main:app --reload --port 8000

# 2. Run a smoke test
bash tests/performance/run_perf_tests.sh smoke

# 3. Or use the interactive web UI
bash tests/performance/run_perf_tests.sh web
# Open http://localhost:8089
```

## Test Profiles

| Profile | Users | Duration | AI Calls | Est. Cost | Command |
|---------|-------|----------|----------|-----------|---------|
| **smoke** | 1 | 60s | ~1 | $0.002 | `run_perf_tests.sh smoke` |
| **load** | 20 | 10m | ~10-15 | $0.03 | `run_perf_tests.sh load` |
| **stress** | 50 | 15m | ~25-40 | $0.08 | `run_perf_tests.sh stress` |
| **crud** | 50 | 5m | 0 | $0 | `run_perf_tests.sh crud` |
| **mealgen** | 3 | 5m | ~15-20 | $0.04 | `run_perf_tests.sh mealgen` |
| **web** | Custom | Custom | Varies | Varies | `run_perf_tests.sh web` |

## User Classes

### `RasoiAIUser` (default)
Simulates realistic app traffic. Task weights:
- 40% health checks
- 25% get current meal plan
- 15% recipe search
- 10% grocery list
- 5% meal generation (AI)
- 5% user profile

### `MealGenHeavyUser`
AI-only: every request is a meal plan generation. Use for focused AI endpoint testing.
```bash
locust -f tests/performance/locustfile.py MealGenHeavyUser --headless -u 3 -r 1 -t 5m
```

### `CRUDOnlyUser`
No AI calls at all. Use for baseline FastAPI + PostgreSQL performance.
```bash
locust -f tests/performance/locustfile.py CRUDOnlyUser --headless -u 50 -r 5 -t 5m
```

## Performance Targets

| Endpoint | p50 | p95 | p99 |
|----------|-----|-----|-----|
| Health check | <50ms | <100ms | <200ms |
| CRUD reads | <200ms | <500ms | <1s |
| Recipe search | <300ms | <800ms | <2s |
| Meal generation | <60s | <90s | <120s |

## Validation

During load tests, responses are validated for:
1. Correct HTTP status codes
2. 7 days in meal plan
3. 4 meal slots per day (breakfast, lunch, dinner, snacks)
4. Each slot has >= 1 item with a recipe_name
5. No allergens in recipe names (matched against test profile)

## Test Profiles Data

`test_profiles.json` contains 5 varied Indian family profiles:
- **Sharma** — Vegetarian + Sattvic, peanut allergy, 3 members
- **Gupta** — Eggetarian, shellfish allergy, 4 members
- **Reddy** — South Indian vegetarian, 2 members, all busy days
- **Khan** — Non-veg + Halal, soy allergy, 5 members with senior
- **Jain** — Strict Jain dietary restrictions, 3 members

Profiles are randomly assigned to simulated users to ensure varied AI prompts.

## Reports

HTML and CSV reports are saved to `tests/performance/reports/` (gitignored).
Open the HTML file for charts showing latency percentiles, throughput, and error rates.

## File Structure

```
tests/performance/
├── README.md              # This file
├── __init__.py
├── locustfile.py          # Main test file (3 user classes)
├── test_profiles.json     # 5 varied family profiles
├── run_perf_tests.sh      # Runner script with profile selection
├── profiles/
│   ├── smoke.conf         # 1 user, 60s
│   ├── load.conf          # 20 users, 10m
│   ├── stress.conf        # 50 users, 15m
│   ├── crud_only.conf     # 50 users, 5m, no AI
│   └── meal_gen_focused.conf  # 3 users, 5m, AI only
└── reports/               # Output directory (gitignored)
    └── .gitkeep
```
