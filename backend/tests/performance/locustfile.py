"""
RasoiAI API Performance Testing Harness

Uses Locust for load testing the meal plan generation API and CRUD endpoints.
AI endpoints (meal generation) have fundamentally different latency characteristics
(4-90s) vs CRUD endpoints (<500ms), so they are separated into distinct task sets
with appropriate timeouts and weights.

Usage:
    # Web UI (recommended for first run)
    cd backend && locust -f tests/performance/locustfile.py

    # Headless with profile configs
    cd backend && locust -f tests/performance/locustfile.py --config tests/performance/profiles/smoke.conf

    # Quick smoke test (no config file needed)
    cd backend && locust -f tests/performance/locustfile.py --headless -u 1 -r 1 -t 60s --host http://localhost:8000

Prerequisites:
    - Backend running: uvicorn app.main:app --reload --port 8000
    - Backend in DEBUG mode (DEBUG=true in .env) for fake-firebase-token auth
    - PostgreSQL accessible
"""

import json
import logging
import os
import random
import time
from datetime import date, timedelta
from pathlib import Path

from locust import HttpUser, between, events, task
from locust.runners import MasterRunner

logger = logging.getLogger("rasoiai.perf")

# ---------------------------------------------------------------------------
# Test profile data (varied user preferences for realistic AI prompts)
# ---------------------------------------------------------------------------

PROFILES_PATH = Path(__file__).parent / "test_profiles.json"


def load_profiles() -> list[dict]:
    """Load test user profiles for varied meal generation requests."""
    if PROFILES_PATH.exists():
        with open(PROFILES_PATH) as f:
            return json.load(f)
    # Fallback: single default profile
    return [{"name": "Default", "firebase_token": "fake-firebase-token", "preferences": {}}]


TEST_PROFILES = load_profiles()

# ---------------------------------------------------------------------------
# Metrics tracking
# ---------------------------------------------------------------------------

# Track AI API costs (approximate token counts)
_generation_count = 0
_generation_errors = 0
_generation_timeouts = 0
_total_latency_ms = 0


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """Print summary metrics when test completes."""
    if _generation_count > 0:
        avg_latency = _total_latency_ms / _generation_count
        print("\n" + "=" * 70)
        print("MEAL GENERATION PERFORMANCE SUMMARY")
        print("=" * 70)
        print(f"  Total generations:   {_generation_count}")
        print(f"  Errors:              {_generation_errors}")
        print(f"  Timeouts (504):      {_generation_timeouts}")
        print(f"  Avg latency:         {avg_latency:.0f}ms ({avg_latency/1000:.1f}s)")
        print(f"  Success rate:        {(1 - _generation_errors / _generation_count) * 100:.1f}%")
        print(
            f"  Est. Gemini calls:   {_generation_count} "
            f"(~${_generation_count * 0.002:.2f} at ~2000 tokens/call)"
        )
        print("=" * 70 + "\n")


# ---------------------------------------------------------------------------
# Validation helpers
# ---------------------------------------------------------------------------


def validate_meal_plan(response_data: dict) -> tuple[bool, str]:
    """Validate meal plan response structure (lightweight, for use during load).

    Returns (is_valid, error_message).
    """
    if "days" not in response_data:
        return False, "Missing 'days' array"

    days = response_data["days"]
    if len(days) != 7:
        return False, f"Expected 7 days, got {len(days)}"

    for i, day in enumerate(days):
        if "meals" not in day:
            return False, f"Day {i} missing 'meals'"

        meals = day["meals"]
        for slot in ["breakfast", "lunch", "dinner", "snacks"]:
            if slot not in meals:
                return False, f"Day {i} missing '{slot}'"
            items = meals[slot]
            if len(items) < 1:
                return False, f"Day {i} {slot} is empty"

            # Validate each item has required fields
            for item in items:
                if not item.get("recipe_name"):
                    return False, f"Day {i} {slot} item missing recipe_name"

    return True, ""


def validate_allergens(response_data: dict, allergens: list[str]) -> tuple[bool, str]:
    """Check that no allergens appear in recipe names."""
    allergen_set = {a.lower() for a in allergens}
    for day in response_data.get("days", []):
        meals = day.get("meals", {})
        for slot in ["breakfast", "lunch", "dinner", "snacks"]:
            for item in meals.get(slot, []):
                name_lower = item.get("recipe_name", "").lower()
                for allergen in allergen_set:
                    if allergen in name_lower:
                        return False, f"Allergen '{allergen}' found in '{item['recipe_name']}'"
    return True, ""


# ---------------------------------------------------------------------------
# Locust User Classes
# ---------------------------------------------------------------------------


class RasoiAIUser(HttpUser):
    """Simulates a RasoiAI app user with realistic traffic patterns.

    Traffic mix (by task weight):
    - 40% health checks (lightweight, baseline)
    - 25% get current meal plan (most common read)
    - 15% get recipes / search
    - 10% grocery list
    -  5% generate meal plan (expensive AI call)
    -  5% chat / other AI endpoints
    """

    wait_time = between(2, 8)  # Realistic think time between actions
    host = "http://localhost:8000"

    # Auth state
    _token: str | None = None
    _current_plan_id: str | None = None
    _profile: dict | None = None

    def on_start(self):
        """Authenticate and get a JWT token on user spawn."""
        self._profile = random.choice(TEST_PROFILES)
        token = self._profile.get("firebase_token", "fake-firebase-token")

        with self.client.post(
            "/api/v1/auth/firebase",
            json={"firebase_token": token},
            catch_response=True,
            name="/api/v1/auth/firebase",
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                self._token = data.get("access_token")
                resp.success()
                logger.info(
                    f"Authenticated as profile: {self._profile.get('name', 'unknown')}"
                )
            else:
                resp.failure(f"Auth failed: {resp.status_code}")
                logger.error(f"Auth failed: {resp.status_code} {resp.text[:200]}")

    @property
    def auth_headers(self) -> dict:
        """Authorization header dict."""
        if self._token:
            return {"Authorization": f"Bearer {self._token}"}
        return {}

    # ------ Lightweight endpoints (high frequency) ------

    @task(40)
    def health_check(self):
        """GET /health — baseline latency measurement."""
        self.client.get("/health", name="/health")

    @task(25)
    def get_current_meal_plan(self):
        """GET /api/v1/meal-plans/current — most common read operation."""
        with self.client.get(
            "/api/v1/meal-plans/current",
            headers=self.auth_headers,
            catch_response=True,
            name="/api/v1/meal-plans/current",
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                # Cache plan ID for swap/lock tasks
                self._current_plan_id = data.get("id")
                resp.success()
            elif resp.status_code == 404:
                # No plan yet — expected for new users
                resp.success()
            else:
                resp.failure(f"Status {resp.status_code}")

    @task(15)
    def search_recipes(self):
        """GET /api/v1/recipes/search — recipe search endpoint."""
        queries = ["dal", "paneer", "chai", "rice", "roti", "dosa", "idli", "biryani"]
        q = random.choice(queries)
        with self.client.get(
            f"/api/v1/recipes/search?q={q}&limit=10",
            headers=self.auth_headers,
            catch_response=True,
            name="/api/v1/recipes/search",
        ) as resp:
            if resp.status_code in (200, 404):
                resp.success()
            else:
                resp.failure(f"Status {resp.status_code}")

    @task(10)
    def get_grocery_list(self):
        """GET /api/v1/grocery — grocery list derived from meal plan."""
        with self.client.get(
            "/api/v1/grocery",
            headers=self.auth_headers,
            catch_response=True,
            name="/api/v1/grocery",
        ) as resp:
            if resp.status_code in (200, 404):
                resp.success()
            else:
                resp.failure(f"Status {resp.status_code}")

    # ------ AI endpoints (low frequency, high latency) ------

    @task(5)
    def generate_meal_plan(self):
        """POST /api/v1/meal-plans/generate — AI meal generation (4-90s).

        This is the primary performance-critical endpoint.
        Uses 120s timeout matching the backend's asyncio.wait_for().
        """
        global _generation_count, _generation_errors, _generation_timeouts, _total_latency_ms

        # Vary the week start date to avoid caching effects
        days_offset = random.randint(0, 52) * 7
        week_start = date.today() + timedelta(days=days_offset)
        # Align to Monday
        week_start = week_start - timedelta(days=week_start.weekday())

        start_time = time.time()

        with self.client.post(
            "/api/v1/meal-plans/generate",
            json={"week_start_date": week_start.isoformat()},
            headers=self.auth_headers,
            timeout=150,  # 150s client timeout > 120s server timeout
            catch_response=True,
            name="/api/v1/meal-plans/generate",
        ) as resp:
            elapsed_ms = (time.time() - start_time) * 1000
            _generation_count += 1
            _total_latency_ms += elapsed_ms

            if resp.status_code == 200:
                try:
                    data = resp.json()

                    # Structural validation
                    is_valid, error = validate_meal_plan(data)
                    if not is_valid:
                        resp.failure(f"Invalid structure: {error}")
                        _generation_errors += 1
                        return

                    # Allergen validation (if profile has allergies)
                    prefs = self._profile.get("preferences", {}) if self._profile else {}
                    allergens = [a["ingredient"] for a in prefs.get("allergies", [])]
                    if allergens:
                        is_safe, error = validate_allergens(data, allergens)
                        if not is_safe:
                            resp.failure(f"Allergen violation: {error}")
                            _generation_errors += 1
                            return

                    # Cache plan ID
                    self._current_plan_id = data.get("id")
                    resp.success()
                    logger.info(
                        f"Meal plan generated in {elapsed_ms:.0f}ms "
                        f"({elapsed_ms/1000:.1f}s) - {self._profile.get('name', '?')}"
                    )

                except json.JSONDecodeError:
                    resp.failure("Invalid JSON response")
                    _generation_errors += 1

            elif resp.status_code == 504:
                resp.failure("Generation timeout (504)")
                _generation_timeouts += 1
                _generation_errors += 1
            else:
                resp.failure(f"Status {resp.status_code}")
                _generation_errors += 1

    @task(5)
    def get_user_profile(self):
        """GET /api/v1/users/me — user profile read."""
        with self.client.get(
            "/api/v1/users/me",
            headers=self.auth_headers,
            catch_response=True,
            name="/api/v1/users/me",
        ) as resp:
            if resp.status_code in (200, 404):
                resp.success()
            else:
                resp.failure(f"Status {resp.status_code}")


class MealGenHeavyUser(HttpUser):
    """Specialized user that ONLY generates meal plans.

    Use this for focused AI endpoint stress testing.
    Spawns fewer users but all hit the generation endpoint.

    Usage:
        locust -f tests/performance/locustfile.py MealGenHeavyUser --headless -u 3 -r 1 -t 5m
    """

    wait_time = between(5, 15)  # Longer wait — AI calls are expensive
    host = "http://localhost:8000"

    _token: str | None = None
    _profile: dict | None = None

    def on_start(self):
        """Authenticate on spawn."""
        self._profile = random.choice(TEST_PROFILES)
        resp = self.client.post(
            "/api/v1/auth/firebase",
            json={"firebase_token": self._profile.get("firebase_token", "fake-firebase-token")},
            name="/api/v1/auth/firebase [MealGen]",
        )
        if resp.status_code == 200:
            self._token = resp.json().get("access_token")

    @property
    def auth_headers(self) -> dict:
        if self._token:
            return {"Authorization": f"Bearer {self._token}"}
        return {}

    @task
    def generate_and_validate(self):
        """Generate meal plan with full validation.

        Validates:
        1. HTTP 200 response
        2. 7 days present
        3. All 4 meal slots per day
        4. Each slot has >= 1 item
        5. No allergens in recipe names
        6. Each item has recipe_name
        """
        global _generation_count, _generation_errors, _generation_timeouts, _total_latency_ms

        # Random week start (aligned to Monday)
        offset = random.randint(0, 12) * 7
        week_start = date.today() + timedelta(days=offset)
        week_start -= timedelta(days=week_start.weekday())

        start = time.time()

        with self.client.post(
            "/api/v1/meal-plans/generate",
            json={"week_start_date": week_start.isoformat()},
            headers=self.auth_headers,
            timeout=150,
            catch_response=True,
            name="/api/v1/meal-plans/generate [FOCUSED]",
        ) as resp:
            elapsed_ms = (time.time() - start) * 1000
            _generation_count += 1
            _total_latency_ms += elapsed_ms

            if resp.status_code == 200:
                try:
                    data = resp.json()
                    valid, err = validate_meal_plan(data)
                    if not valid:
                        resp.failure(f"Validation: {err}")
                        _generation_errors += 1
                        return

                    prefs = self._profile.get("preferences", {}) if self._profile else {}
                    allergens = [a["ingredient"] for a in prefs.get("allergies", [])]
                    if allergens:
                        safe, err = validate_allergens(data, allergens)
                        if not safe:
                            resp.failure(f"Allergen: {err}")
                            _generation_errors += 1
                            return

                    # Count total items
                    total_items = sum(
                        len(day.get("meals", {}).get(slot, []))
                        for day in data["days"]
                        for slot in ["breakfast", "lunch", "dinner", "snacks"]
                    )

                    resp.success()
                    logger.info(
                        f"[FOCUSED] Generated {total_items} items in "
                        f"{elapsed_ms/1000:.1f}s - {self._profile.get('name', '?')}"
                    )
                except json.JSONDecodeError:
                    resp.failure("Invalid JSON")
                    _generation_errors += 1
            elif resp.status_code == 504:
                resp.failure("Timeout 504")
                _generation_timeouts += 1
                _generation_errors += 1
            else:
                resp.failure(f"HTTP {resp.status_code}")
                _generation_errors += 1


class CRUDOnlyUser(HttpUser):
    """User that only hits CRUD endpoints (no AI calls).

    Use this for baseline performance testing without Gemini API costs.

    Usage:
        locust -f tests/performance/locustfile.py CRUDOnlyUser --headless -u 50 -r 10 -t 5m
    """

    wait_time = between(1, 3)
    host = "http://localhost:8000"

    _token: str | None = None

    def on_start(self):
        resp = self.client.post(
            "/api/v1/auth/firebase",
            json={"firebase_token": "fake-firebase-token"},
            name="/api/v1/auth/firebase [CRUD]",
        )
        if resp.status_code == 200:
            self._token = resp.json().get("access_token")

    @property
    def auth_headers(self) -> dict:
        if self._token:
            return {"Authorization": f"Bearer {self._token}"}
        return {}

    @task(10)
    def health(self):
        self.client.get("/health", name="/health")

    @task(8)
    def get_current_plan(self):
        with self.client.get(
            "/api/v1/meal-plans/current",
            headers=self.auth_headers,
            catch_response=True,
            name="/api/v1/meal-plans/current [CRUD]",
        ) as resp:
            if resp.status_code in (200, 404):
                resp.success()

    @task(6)
    def search_recipes(self):
        q = random.choice(["dal", "paneer", "chai", "rice", "dosa", "samosa"])
        with self.client.get(
            f"/api/v1/recipes/search?q={q}&limit=10",
            headers=self.auth_headers,
            catch_response=True,
            name="/api/v1/recipes/search [CRUD]",
        ) as resp:
            if resp.status_code in (200, 404):
                resp.success()

    @task(4)
    def get_grocery(self):
        with self.client.get(
            "/api/v1/grocery",
            headers=self.auth_headers,
            catch_response=True,
            name="/api/v1/grocery [CRUD]",
        ) as resp:
            if resp.status_code in (200, 404):
                resp.success()

    @task(3)
    def get_stats(self):
        with self.client.get(
            "/api/v1/stats/monthly",
            headers=self.auth_headers,
            catch_response=True,
            name="/api/v1/stats/monthly [CRUD]",
        ) as resp:
            if resp.status_code in (200, 404):
                resp.success()

    @task(2)
    def get_favorites(self):
        with self.client.get(
            "/api/v1/recipes/favorites",
            headers=self.auth_headers,
            catch_response=True,
            name="/api/v1/recipes/favorites [CRUD]",
        ) as resp:
            if resp.status_code in (200, 404):
                resp.success()

    @task(2)
    def get_profile(self):
        with self.client.get(
            "/api/v1/users/me",
            headers=self.auth_headers,
            catch_response=True,
            name="/api/v1/users/me [CRUD]",
        ) as resp:
            if resp.status_code in (200, 404):
                resp.success()
