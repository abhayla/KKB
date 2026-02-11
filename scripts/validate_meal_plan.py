#!/usr/bin/env python3
"""Meal plan validation script for ADB test flows.

Fetches the current meal plan and user preferences from the backend API,
then runs V4a-V4k validation checks against them.

Usage:
    python scripts/validate_meal_plan.py \
        --jwt "eyJ..." \
        --checks "V4a,V4b,V4c,V4d,V4e,V4f,V4g,V4h,V4i,V4j,V4k" \
        --expected-diet "Vegetarian" \
        --expected-items-per-meal 3 \
        --disliked "Karela,Lauki,Capsicum" \
        --output "path/to/output.json"

Exit codes:
    0 = all checks pass
    1 = at least one HARD check failed
    2 = only SOFT warnings (no HARD failures)
"""

import argparse
import json
import sys
from datetime import datetime, timezone
from typing import Any

import requests


def fetch_meal_plan(backend_url: str, jwt: str) -> dict[str, Any]:
    """Fetch the current meal plan from the backend."""
    resp = requests.get(
        f"{backend_url}/api/v1/meal-plans/current",
        headers={"Authorization": f"Bearer {jwt}"},
        timeout=30,
    )
    resp.raise_for_status()
    return resp.json()


def fetch_preferences(backend_url: str, jwt: str) -> dict[str, Any]:
    """Fetch user preferences from the backend."""
    resp = requests.get(
        f"{backend_url}/api/v1/users/me",
        headers={"Authorization": f"Bearer {jwt}"},
        timeout=15,
    )
    resp.raise_for_status()
    return resp.json()


def fetch_recipe_rules(backend_url: str, jwt: str) -> list[dict[str, Any]]:
    """Fetch recipe rules from the backend."""
    resp = requests.get(
        f"{backend_url}/api/v1/recipe-rules",
        headers={"Authorization": f"Bearer {jwt}"},
        timeout=15,
    )
    resp.raise_for_status()
    data = resp.json()
    return data.get("rules", [])


def get_all_items(plan: dict) -> list[dict]:
    """Extract all meal items from the plan."""
    items = []
    for day in plan.get("days", []):
        meals = day.get("meals", {})
        for meal_type in ["breakfast", "lunch", "dinner", "snacks"]:
            for item in meals.get(meal_type, []):
                item["_day"] = day.get("day_name", "")
                item["_date"] = day.get("date", "")
                item["_meal_type"] = meal_type
                items.append(item)
    return items


# =============================================================================
# Validation Checks V4a - V4k
# =============================================================================


def check_v4a(plan: dict, **_kwargs: Any) -> dict:
    """V4a: Days count = 7."""
    days = plan.get("days", [])
    count = len(days)
    passed = count == 7
    return {
        "id": "V4a",
        "name": "Days count",
        "status": "PASS" if passed else "FAIL",
        "severity": "HARD",
        "details": f"{count} days found" + ("" if passed else " (expected 7)"),
    }


def check_v4b(plan: dict, **_kwargs: Any) -> dict:
    """V4b: All 4 meal types per day."""
    days = plan.get("days", [])
    missing = []
    for day in days:
        meals = day.get("meals", {})
        day_name = day.get("day_name", "Unknown")
        for mt in ["breakfast", "lunch", "dinner", "snacks"]:
            if not meals.get(mt):
                missing.append(f"{day_name}: no {mt}")
    passed = len(missing) == 0
    return {
        "id": "V4b",
        "name": "All meal types present",
        "status": "PASS" if passed else "FAIL",
        "severity": "HARD",
        "details": "All 4 meal types on all days"
        if passed
        else f"Missing: {'; '.join(missing[:5])}"
        + (f" (+{len(missing) - 5} more)" if len(missing) > 5 else ""),
    }


def check_v4c(plan: dict, expected_items_per_meal: int = 2, **_kwargs: Any) -> dict:
    """V4c: Items per slot >= N."""
    days = plan.get("days", [])
    violations = []
    for day in days:
        meals = day.get("meals", {})
        day_name = day.get("day_name", "Unknown")
        for mt in ["breakfast", "lunch", "dinner", "snacks"]:
            items = meals.get(mt, [])
            if len(items) < expected_items_per_meal:
                violations.append(
                    f"{day_name} {mt}: {len(items)} items (need {expected_items_per_meal})"
                )
    passed = len(violations) == 0
    return {
        "id": "V4c",
        "name": "Items per slot",
        "status": "PASS" if passed else "FAIL",
        "severity": "HARD",
        "details": f"All slots have >= {expected_items_per_meal} items"
        if passed
        else f"Under-filled: {'; '.join(violations[:5])}"
        + (f" (+{len(violations) - 5} more)" if len(violations) > 5 else ""),
    }


def check_v4d(plan: dict, expected_diet: str | None = None, **_kwargs: Any) -> dict:
    """V4d: Diet compliance."""
    if not expected_diet:
        return {
            "id": "V4d",
            "name": "Diet compliance",
            "status": "SKIP",
            "severity": "SOFT",
            "details": "No expected diet specified",
        }

    items = get_all_items(plan)
    violations = []
    diet_lower = expected_diet.lower()

    for item in items:
        tags = [t.lower() for t in item.get("dietary_tags", [])]
        name = item.get("recipe_name", "").lower()

        # Check for vegetarian diet violations
        if diet_lower in ("vegetarian", "vegan", "jain"):
            if "non_vegetarian" in tags or "non-vegetarian" in tags:
                violations.append(
                    f"{item['_day']} {item['_meal_type']}: '{item.get('recipe_name', '')}' is non-vegetarian"
                )

        # Check for vegan violations
        if diet_lower == "vegan":
            if "vegetarian" in tags and "vegan" not in tags:
                violations.append(
                    f"{item['_day']} {item['_meal_type']}: '{item.get('recipe_name', '')}' is vegetarian but not vegan"
                )

    passed = len(violations) == 0
    return {
        "id": "V4d",
        "name": "Diet compliance",
        "status": "PASS" if passed else "FAIL",
        "severity": "SOFT",
        "details": f"All items comply with {expected_diet} diet"
        if passed
        else f"Violations: {'; '.join(violations[:3])}"
        + (f" (+{len(violations) - 3} more)" if len(violations) > 3 else ""),
    }


def check_v4e(plan: dict, disliked: list[str] | None = None, **_kwargs: Any) -> dict:
    """V4e: No disliked ingredients."""
    if not disliked:
        return {
            "id": "V4e",
            "name": "Disliked exclusion",
            "status": "SKIP",
            "severity": "HARD",
            "details": "No disliked ingredients specified",
        }

    items = get_all_items(plan)
    disliked_lower = [d.lower().strip() for d in disliked]
    violations = []

    for item in items:
        name_lower = item.get("recipe_name", "").lower()
        for d in disliked_lower:
            if d in name_lower:
                violations.append(
                    f"{item['_day']} {item['_meal_type']}: '{item.get('recipe_name', '')}' contains '{d}'"
                )

    passed = len(violations) == 0
    return {
        "id": "V4e",
        "name": "Disliked exclusion",
        "status": "PASS" if passed else "FAIL",
        "severity": "HARD",
        "details": "No disliked ingredients found in recipe names"
        if passed
        else f"Found: {'; '.join(violations[:3])}"
        + (f" (+{len(violations) - 3} more)" if len(violations) > 3 else ""),
    }


def check_v4f(plan: dict, cuisines: list[str] | None = None, **_kwargs: Any) -> dict:
    """V4f: Cuisine coverage."""
    if not cuisines:
        return {
            "id": "V4f",
            "name": "Cuisine coverage",
            "status": "SKIP",
            "severity": "SOFT",
            "details": "No cuisines specified",
        }

    # We can't directly verify cuisine from the meal plan response (no cuisine field on items).
    # This check is informational — report what we can.
    return {
        "id": "V4f",
        "name": "Cuisine coverage",
        "status": "INFO",
        "severity": "SOFT",
        "details": f"Expected cuisines: {', '.join(cuisines)}. "
        "Cuisine type is not in meal item response — manual verification needed.",
    }


def check_v4g(plan: dict, max_spice: str | None = None, **_kwargs: Any) -> dict:
    """V4g: Spice compliance."""
    if not max_spice:
        return {
            "id": "V4g",
            "name": "Spice compliance",
            "status": "SKIP",
            "severity": "SOFT",
            "details": "No max spice level specified",
        }

    # Spice level is a generation parameter, not on individual items.
    return {
        "id": "V4g",
        "name": "Spice compliance",
        "status": "INFO",
        "severity": "SOFT",
        "details": f"Max spice: {max_spice}. "
        "Spice level is a generation parameter — verified at prompt level, not per-item.",
    }


def check_v4h(
    plan: dict,
    weekday_cooking_time: int | None = None,
    weekend_cooking_time: int | None = None,
    **_kwargs: Any,
) -> dict:
    """V4h: Cooking time limits."""
    if not weekday_cooking_time and not weekend_cooking_time:
        return {
            "id": "V4h",
            "name": "Cooking time",
            "status": "SKIP",
            "severity": "SOFT",
            "details": "No cooking time limits specified",
        }

    items = get_all_items(plan)
    violations = []
    weekend_days = {"Saturday", "Sunday"}

    for item in items:
        day_name = item.get("_day", "")
        prep = item.get("prep_time_minutes", 0)
        is_weekend = day_name in weekend_days
        limit = weekend_cooking_time if is_weekend else weekday_cooking_time

        if limit and prep > limit:
            violations.append(
                f"{day_name} {item['_meal_type']}: '{item.get('recipe_name', '')}' "
                f"takes {prep}min (limit {limit}min)"
            )

    passed = len(violations) == 0
    return {
        "id": "V4h",
        "name": "Cooking time",
        "status": "PASS" if passed else "FAIL",
        "severity": "SOFT",
        "details": "All items within cooking time limits"
        if passed
        else f"Over limit: {'; '.join(violations[:3])}"
        + (f" (+{len(violations) - 3} more)" if len(violations) > 3 else ""),
    }


def check_v4i(
    plan: dict,
    busy_days: list[str] | None = None,
    weekday_cooking_time: int | None = None,
    **_kwargs: Any,
) -> dict:
    """V4i: Busy day simplicity — items on busy days should be <= weekday time."""
    if not busy_days:
        return {
            "id": "V4i",
            "name": "Busy day simplicity",
            "status": "SKIP",
            "severity": "SOFT",
            "details": "No busy days specified",
        }

    limit = weekday_cooking_time or 30  # default 30 min
    items = get_all_items(plan)
    busy_lower = [b.lower().strip() for b in busy_days]
    violations = []

    for item in items:
        day_name = item.get("_day", "").lower()
        if day_name in busy_lower:
            prep = item.get("prep_time_minutes", 0)
            if prep > limit:
                violations.append(
                    f"{item['_day']} {item['_meal_type']}: '{item.get('recipe_name', '')}' "
                    f"takes {prep}min on busy day (limit {limit}min)"
                )

    passed = len(violations) == 0
    return {
        "id": "V4i",
        "name": "Busy day simplicity",
        "status": "PASS" if passed else "FAIL",
        "severity": "SOFT",
        "details": f"All busy day items within {limit}min"
        if passed
        else f"Over limit: {'; '.join(violations[:3])}"
        + (f" (+{len(violations) - 3} more)" if len(violations) > 3 else ""),
    }


def check_v4j(plan: dict, allow_repeats: bool = False, **_kwargs: Any) -> dict:
    """V4j: No recipe repeats (unless allowed)."""
    if allow_repeats:
        return {
            "id": "V4j",
            "name": "No repeats",
            "status": "SKIP",
            "severity": "SOFT",
            "details": "Repeats allowed — check skipped",
        }

    items = get_all_items(plan)
    seen: dict[str, str] = {}  # recipe_name -> first occurrence
    repeats = []

    for item in items:
        name = item.get("recipe_name", "").strip()
        if not name:
            continue
        key = name.lower()
        loc = f"{item['_day']} {item['_meal_type']}"
        if key in seen:
            repeats.append(f"'{name}' repeated: {seen[key]} and {loc}")
        else:
            seen[key] = loc

    passed = len(repeats) == 0
    return {
        "id": "V4j",
        "name": "No repeats",
        "status": "PASS" if passed else "FAIL",
        "severity": "SOFT",
        "details": f"No recipe repeats across {len(seen)} unique items"
        if passed
        else f"Repeats: {'; '.join(repeats[:3])}"
        + (f" (+{len(repeats) - 3} more)" if len(repeats) > 3 else ""),
    }


def check_v4k(
    plan: dict,
    rules: list[dict] | None = None,
    **_kwargs: Any,
) -> dict:
    """V4k: Rule enforcement — INCLUDE and EXCLUDE rules honored."""
    if not rules:
        return {
            "id": "V4k",
            "name": "Rule enforcement",
            "status": "SKIP",
            "severity": "HARD",
            "details": "No recipe rules to verify",
        }

    items = get_all_items(plan)
    all_names_lower = [item.get("recipe_name", "").lower() for item in items]
    all_names_str = " ".join(all_names_lower)

    violations = []
    warnings = []

    for rule in rules:
        if not rule.get("is_active", True):
            continue

        target = rule.get("target_name", "").lower().strip()
        action = rule.get("action", "").upper()

        if action == "EXCLUDE":
            # HARD: excluded items must NOT appear
            for item in items:
                if target in item.get("recipe_name", "").lower():
                    violations.append(
                        f"EXCLUDE '{rule.get('target_name', '')}' violated: "
                        f"found '{item.get('recipe_name', '')}' on {item['_day']} {item['_meal_type']}"
                    )

        elif action == "INCLUDE":
            # SOFT: included items SHOULD appear
            if target not in all_names_str:
                warnings.append(
                    f"INCLUDE '{rule.get('target_name', '')}' not found in any meal"
                )

    has_hard_fail = len(violations) > 0
    has_soft_fail = len(warnings) > 0
    passed = not has_hard_fail and not has_soft_fail

    details_parts = []
    if violations:
        details_parts.append(f"EXCLUDE violations: {'; '.join(violations[:3])}")
    if warnings:
        details_parts.append(f"INCLUDE warnings: {'; '.join(warnings[:3])}")
    if passed:
        details_parts.append(f"All {len(rules)} rules honored")

    return {
        "id": "V4k",
        "name": "Rule enforcement",
        "status": "PASS" if passed else "FAIL",
        "severity": "HARD" if has_hard_fail else "SOFT",
        "details": "; ".join(details_parts),
    }


# =============================================================================
# Main
# =============================================================================

CHECK_FUNCTIONS = {
    "V4a": check_v4a,
    "V4b": check_v4b,
    "V4c": check_v4c,
    "V4d": check_v4d,
    "V4e": check_v4e,
    "V4f": check_v4f,
    "V4g": check_v4g,
    "V4h": check_v4h,
    "V4i": check_v4i,
    "V4j": check_v4j,
    "V4k": check_v4k,
}


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Validate a meal plan against V4a-V4k checks."
    )
    parser.add_argument("--jwt", required=True, help="JWT token for API auth")
    parser.add_argument(
        "--backend-url",
        default="http://localhost:8000",
        help="Backend base URL (default: http://localhost:8000)",
    )
    parser.add_argument(
        "--checks",
        default="V4a,V4b,V4c,V4d,V4e,V4f,V4g,V4h,V4i,V4j,V4k",
        help="Comma-separated check IDs to run (default: all)",
    )
    parser.add_argument("--expected-diet", help="Expected dietary type (e.g., Vegetarian)")
    parser.add_argument(
        "--expected-items-per-meal",
        type=int,
        default=2,
        help="Min items per meal slot (default: 2)",
    )
    parser.add_argument("--disliked", help="Comma-separated disliked ingredients")
    parser.add_argument("--cuisines", help="Comma-separated expected cuisines")
    parser.add_argument("--max-spice", help="Max spice level (Mild, Medium, Spicy)")
    parser.add_argument("--weekday-cooking-time", type=int, help="Weekday cooking time limit (min)")
    parser.add_argument("--weekend-cooking-time", type=int, help="Weekend cooking time limit (min)")
    parser.add_argument("--busy-days", help="Comma-separated busy days (e.g., Monday,Wednesday)")
    parser.add_argument(
        "--allow-repeats",
        action="store_true",
        default=False,
        help="Allow recipe repeats (skip V4j)",
    )
    parser.add_argument("--skip-checks", help="Comma-separated check IDs to skip")
    parser.add_argument("--output", help="Output JSON file path")

    args = parser.parse_args()

    # Parse list arguments
    checks_to_run = [c.strip() for c in args.checks.split(",")]
    skip_checks = (
        [c.strip() for c in args.skip_checks.split(",")]
        if args.skip_checks
        else []
    )
    disliked = (
        [d.strip() for d in args.disliked.split(",")]
        if args.disliked
        else None
    )
    cuisines = (
        [c.strip() for c in args.cuisines.split(",")]
        if args.cuisines
        else None
    )
    busy_days = (
        [d.strip() for d in args.busy_days.split(",")]
        if args.busy_days
        else None
    )

    # Fetch data
    print(f"Fetching meal plan from {args.backend_url}...")
    try:
        plan = fetch_meal_plan(args.backend_url, args.jwt)
    except requests.HTTPError as e:
        print(f"ERROR: Failed to fetch meal plan: {e}")
        return 1
    except requests.ConnectionError:
        print(f"ERROR: Cannot connect to backend at {args.backend_url}")
        return 1

    print(f"Fetching preferences from {args.backend_url}...")
    try:
        user_data = fetch_preferences(args.backend_url, args.jwt)
    except requests.HTTPError as e:
        print(f"WARNING: Failed to fetch preferences: {e}")
        user_data = {}

    # Fetch rules for V4k
    rules = None
    if "V4k" in checks_to_run and "V4k" not in skip_checks:
        print(f"Fetching recipe rules from {args.backend_url}...")
        try:
            rules = fetch_recipe_rules(args.backend_url, args.jwt)
        except requests.HTTPError as e:
            print(f"WARNING: Failed to fetch recipe rules: {e}")

    # Build kwargs for check functions
    kwargs = {
        "expected_diet": args.expected_diet,
        "expected_items_per_meal": args.expected_items_per_meal,
        "disliked": disliked,
        "cuisines": cuisines,
        "max_spice": args.max_spice,
        "weekday_cooking_time": args.weekday_cooking_time,
        "weekend_cooking_time": args.weekend_cooking_time,
        "busy_days": busy_days,
        "allow_repeats": args.allow_repeats,
        "rules": rules,
    }

    # Run checks
    results = []
    for check_id in checks_to_run:
        if check_id in skip_checks:
            results.append({
                "id": check_id,
                "name": CHECK_FUNCTIONS.get(check_id, lambda **_: {}).__doc__ or check_id,
                "status": "SKIP",
                "severity": "—",
                "details": "Skipped by --skip-checks",
            })
            continue

        fn = CHECK_FUNCTIONS.get(check_id)
        if not fn:
            print(f"WARNING: Unknown check '{check_id}', skipping")
            continue

        result = fn(plan, **kwargs)
        results.append(result)

    # Count items
    all_items = get_all_items(plan)
    total_days = len(plan.get("days", []))
    total_meals = sum(
        1
        for day in plan.get("days", [])
        for mt in ["breakfast", "lunch", "dinner", "snacks"]
        if day.get("meals", {}).get(mt)
    )

    # Summary
    passed = sum(1 for r in results if r["status"] == "PASS")
    failed_hard = sum(1 for r in results if r["status"] == "FAIL" and r["severity"] == "HARD")
    failed_soft = sum(1 for r in results if r["status"] == "FAIL" and r["severity"] == "SOFT")
    skipped = sum(1 for r in results if r["status"] in ("SKIP", "INFO"))

    exit_code = 0
    if failed_hard > 0:
        exit_code = 1
    elif failed_soft > 0:
        exit_code = 2

    output = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "meal_plan_id": plan.get("id", ""),
        "total_days": total_days,
        "total_meals": total_meals,
        "total_items": len(all_items),
        "checks": results,
        "summary": {
            "total_checks": len(results),
            "passed": passed,
            "failed_hard": failed_hard,
            "failed_soft": failed_soft,
            "skipped": skipped,
        },
        "exit_code": exit_code,
    }

    # Print results
    print("\n" + "=" * 60)
    print("  MEAL PLAN VALIDATION REPORT")
    print("=" * 60)
    print(f"Plan ID: {plan.get('id', 'unknown')}")
    print(f"Days: {total_days} | Meals: {total_meals} | Items: {len(all_items)}")
    print("-" * 60)

    for r in results:
        icon = {"PASS": "OK", "FAIL": "FAIL", "SKIP": "SKIP", "INFO": "INFO"}.get(
            r["status"], "?"
        )
        print(f"  [{icon:4s}] {r['id']}: {r['name']} ({r['severity']})")
        print(f"         {r['details']}")

    print("-" * 60)
    print(
        f"TOTAL: {passed} passed | {failed_hard} HARD fail | "
        f"{failed_soft} SOFT fail | {skipped} skipped"
    )
    print(f"EXIT CODE: {exit_code}")
    print("=" * 60)

    # Write output file
    if args.output:
        import os

        os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
        with open(args.output, "w") as f:
            json.dump(output, f, indent=2)
        print(f"\nResults written to: {args.output}")

    return exit_code


if __name__ == "__main__":
    sys.exit(main())
