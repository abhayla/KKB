#!/usr/bin/env python3
"""
Test Map Generator for KKB.

Maps source files to their test files using convention-based patterns and
import-based discovery. Generates .claude/test-map.json for use by auto-verify.

Usage:
    python .claude/scripts/generate_test_map.py                              # Generate full map
    python .claude/scripts/generate_test_map.py lookup <source_file>         # Lookup tests for file
    python .claude/scripts/generate_test_map.py affected --base HEAD~1       # Tests for git diff
"""

import argparse
import json
import os
import re
import subprocess
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent.parent
TEST_MAP_PATH = SCRIPT_DIR.parent / "test-map.json"

BACKEND_DIR = PROJECT_ROOT / "backend"
ANDROID_DIR = PROJECT_ROOT / "android"


def find_files(base_dir, pattern):
    """Find files matching glob pattern under base_dir."""
    return sorted(base_dir.glob(pattern))


def relative(path, base=PROJECT_ROOT):
    """Return path relative to project root, with forward slashes."""
    try:
        return str(path.relative_to(base)).replace("\\", "/")
    except ValueError:
        return str(path).replace("\\", "/")


def grep_importers(target_name, search_dir, file_glob="*.py"):
    """Find files that import a given module/name."""
    results = []
    for f in search_dir.rglob(file_glob):
        try:
            text = f.read_text(encoding="utf-8", errors="ignore")
            if re.search(rf'\b{re.escape(target_name)}\b', text):
                results.append(f)
        except OSError:
            continue
    return results


def map_backend():
    """Generate backend source → test mappings."""
    mappings = {}
    test_dir = BACKEND_DIR / "tests"
    if not test_dir.exists():
        return mappings

    # Collect all test files
    test_files = {f.stem: f for f in test_dir.glob("test_*.py")}

    # Pattern 1: endpoints/{name}.py → test_{name}_api.py or test_{name}.py
    endpoints_dir = BACKEND_DIR / "app" / "api" / "v1" / "endpoints"
    if endpoints_dir.exists():
        for src in endpoints_dir.glob("*.py"):
            if src.name == "__init__.py":
                continue
            name = src.stem
            key = relative(src)
            tests = []
            # Direct convention matches
            for suffix in [f"test_{name}_api", f"test_{name}", f"test_{name}s_api", f"test_{name}s"]:
                if suffix in test_files:
                    tests.append(relative(test_files[suffix]))
            # Special: recipe_rules.py also has nutrition_goals router
            if name == "recipe_rules":
                for extra in ["test_recipe_rules_dedup", "test_recipe_rules_sync",
                               "test_recipe_rules_lifecycle", "test_nutrition_goals_api"]:
                    if extra in test_files:
                        tests.append(relative(test_files[extra]))
            if tests:
                mappings[key] = {"tests": tests, "priority": "P1"}

    # Pattern 2: services/{name}_service.py → test_{name}_service.py or test_{name}.py
    services_dir = BACKEND_DIR / "app" / "services"
    if services_dir.exists():
        for src in services_dir.glob("*_service.py"):
            name = src.stem.replace("_service", "")
            key = relative(src)
            tests = []
            for suffix in [f"test_{name}_service", f"test_{name}", f"test_{name}s", f"test_{name}s_api", f"test_{src.stem}"]:
                if suffix in test_files:
                    tests.append(relative(test_files[suffix]))
            # AI services have special test files
            if "ai" in name or "meal" in name:
                for extra in ["test_ai_meal_service", "test_family_aware_meal_gen"]:
                    if extra in test_files:
                        tests.append(relative(test_files[extra]))
            if tests:
                mappings.setdefault(key, {"tests": [], "priority": "P1"})
                mappings[key]["tests"] = list(set(mappings[key]["tests"] + tests))

    # Pattern 3: repositories/{name}_repository.py → grep-based (P2)
    repos_dir = BACKEND_DIR / "app" / "repositories"
    if repos_dir.exists():
        for src in repos_dir.glob("*_repository.py"):
            name = src.stem.replace("_repository", "")
            key = relative(src)
            if key in mappings:
                continue
            # Find test files that import this repository
            importers = grep_importers(src.stem, test_dir)
            tests = [relative(f) for f in importers if f.name.startswith("test_")]
            if tests:
                mappings[key] = {"tests": tests[:10], "priority": "P2"}

    # Pattern 4: models/{name}.py → grep-based (P2)
    models_dir = BACKEND_DIR / "app" / "models"
    if models_dir.exists():
        for src in models_dir.glob("*.py"):
            if src.name == "__init__.py":
                continue
            key = relative(src)
            if key in mappings:
                continue
            importers = grep_importers(src.stem, test_dir)
            tests = [relative(f) for f in importers if f.name.startswith("test_")]
            if tests:
                mappings[key] = {"tests": tests[:10], "priority": "P2"}

    # Pattern 5: ai/*.py → AI test files (P2)
    ai_dir = BACKEND_DIR / "app" / "ai"
    if ai_dir.exists():
        for src in ai_dir.glob("*.py"):
            if src.name == "__init__.py":
                continue
            key = relative(src)
            if key in mappings:
                continue
            tests = []
            for prefix in ["test_ai_meal_service", "test_chat_api", "test_chat_integration",
                            "test_photo_analysis", "test_family_aware_meal_gen"]:
                if prefix in test_files:
                    tests.append(relative(test_files[prefix]))
            if tests:
                mappings[key] = {"tests": tests, "priority": "P2"}

    # Pattern 6: conftest.py / config.py → broad impact
    for special in ["app/config.py", "tests/conftest.py"]:
        path = BACKEND_DIR / special
        if path.exists():
            key = relative(path)
            mappings[key] = {
                "tests": [relative(f) for f in sorted(test_dir.glob("test_*.py"))[:15]],
                "priority": "P3",
                "note": "broad-impact"
            }

    return mappings


def map_android():
    """Generate Android source → test mappings."""
    mappings = {}

    app_src = ANDROID_DIR / "app" / "src"
    main_java = app_src / "main" / "java" / "com" / "rasoiai" / "app"
    test_java = app_src / "test" / "java" / "com" / "rasoiai" / "app"
    android_test = app_src / "androidTest" / "java" / "com" / "rasoiai" / "app"

    if not main_java.exists():
        return mappings

    # Pattern 1: presentation/{feat}/{Feat}ViewModel.kt → {Feat}ViewModelTest.kt (P1)
    for vm_file in main_java.rglob("*ViewModel.kt"):
        key = relative(vm_file)
        feat_name = vm_file.stem  # e.g., HomeViewModel
        test_name = f"{feat_name}Test.kt"
        # Search in test directory
        test_matches = list(test_java.rglob(test_name)) if test_java.exists() else []
        tests = [relative(t) for t in test_matches]
        if tests:
            mappings[key] = {"tests": tests, "priority": "P1"}

    # Pattern 2: presentation/{feat}/{Feat}Screen.kt → {Feat}ScreenTest.kt (P1)
    for screen_file in main_java.rglob("*Screen.kt"):
        if screen_file.stem.endswith("ScreenTest"):
            continue
        key = relative(screen_file)
        test_name = f"{screen_file.stem}Test.kt"
        test_matches = list(android_test.rglob(test_name)) if android_test.exists() else []
        tests = [relative(t) for t in test_matches]
        if tests:
            mappings[key] = {"tests": tests, "priority": "P1"}

    # Pattern 3: data repositories
    data_src = ANDROID_DIR / "data" / "src" / "main" / "java"
    if data_src.exists():
        for repo_file in data_src.rglob("*RepositoryImpl.kt"):
            key = relative(repo_file)
            # Map to ViewModel tests that use this repository
            repo_name = repo_file.stem.replace("Impl", "")
            if test_java.exists():
                for test_file in test_java.rglob("*ViewModelTest.kt"):
                    try:
                        text = test_file.read_text(encoding="utf-8", errors="ignore")
                        if repo_name in text:
                            mappings.setdefault(key, {"tests": [], "priority": "P2"})
                            mappings[key]["tests"].append(relative(test_file))
                    except OSError:
                        continue

    # Pattern 4: domain models
    domain_src = ANDROID_DIR / "domain" / "src" / "main" / "java"
    if domain_src.exists():
        for model_file in domain_src.rglob("*.kt"):
            if model_file.parent.name != "model":
                continue
            key = relative(model_file)
            model_name = model_file.stem
            tests = []
            if test_java.exists():
                for test_file in test_java.rglob("*ViewModelTest.kt"):
                    try:
                        text = test_file.read_text(encoding="utf-8", errors="ignore")
                        if model_name in text:
                            tests.append(relative(test_file))
                    except OSError:
                        continue
            if tests:
                mappings[key] = {"tests": tests[:10], "priority": "P2"}

    return mappings


def generate_full_map():
    """Generate complete test map and write to .claude/test-map.json."""
    test_map = {
        "version": 1,
        "generated": __import__("datetime").datetime.now(
            __import__("datetime").timezone.utc
        ).isoformat(),
        "backend": map_backend(),
        "android": map_android()
    }

    # Deduplicate test lists
    for platform in ["backend", "android"]:
        for key, val in test_map[platform].items():
            val["tests"] = sorted(set(val["tests"]))

    TEST_MAP_PATH.parent.mkdir(parents=True, exist_ok=True)
    with open(TEST_MAP_PATH, "w") as f:
        json.dump(test_map, f, indent=2)

    total = sum(len(v["tests"]) for v in test_map["backend"].values()) + \
            sum(len(v["tests"]) for v in test_map["android"].values())
    sources = len(test_map["backend"]) + len(test_map["android"])
    print(f"Generated test map: {sources} source files -> {total} test mappings")
    print(f"  Backend: {len(test_map['backend'])} sources")
    print(f"  Android: {len(test_map['android'])} sources")
    print(f"  Written to: {TEST_MAP_PATH}")
    return test_map


def lookup(source_file):
    """Look up tests for a given source file."""
    if not TEST_MAP_PATH.exists():
        print("No test-map.json found. Generating...")
        generate_full_map()

    with open(TEST_MAP_PATH) as f:
        test_map = json.load(f)

    # Normalize the source file path
    source_norm = source_file.replace("\\", "/")

    for platform in ["backend", "android"]:
        for key, val in test_map.get(platform, {}).items():
            if key == source_norm or source_norm.endswith(key) or key.endswith(source_norm):
                print(f"Source: {key} (priority: {val['priority']})")
                for t in val["tests"]:
                    print(f"  -> {t}")
                return val["tests"]

    print(f"No test mapping found for: {source_file}")
    return []


def affected(base="HEAD~1"):
    """Get all affected tests for files changed since base."""
    try:
        result = subprocess.run(
            ["git", "diff", "--name-only", base],
            capture_output=True, text=True, cwd=str(PROJECT_ROOT)
        )
        changed = [f.strip() for f in result.stdout.strip().split("\n") if f.strip()]
    except Exception as e:
        print(f"Error running git diff: {e}")
        return []

    if not TEST_MAP_PATH.exists():
        print("No test-map.json found. Generating...")
        generate_full_map()

    with open(TEST_MAP_PATH) as f:
        test_map = json.load(f)

    all_tests = set()
    for source_file in changed:
        source_norm = source_file.replace("\\", "/")
        for platform in ["backend", "android"]:
            for key, val in test_map.get(platform, {}).items():
                if key == source_norm or source_norm.endswith(key):
                    all_tests.update(val["tests"])

    if all_tests:
        print(f"Changed files: {len(changed)}")
        print(f"Affected tests: {len(all_tests)}")
        for t in sorted(all_tests):
            print(f"  {t}")
    else:
        print(f"No test mappings found for {len(changed)} changed files")

    return sorted(all_tests)


def main():
    parser = argparse.ArgumentParser(description='KKB Test Map Generator')
    sub = parser.add_subparsers(dest='command')

    sub.add_parser('generate', help='Generate full test map (default)')

    p = sub.add_parser('lookup', help='Look up tests for a source file')
    p.add_argument('source_file', help='Source file path')

    p = sub.add_parser('affected', help='Get affected tests for git diff')
    p.add_argument('--base', default='HEAD~1', help='Git base ref (default: HEAD~1)')

    args = parser.parse_args()

    if args.command == 'lookup':
        lookup(args.source_file)
    elif args.command == 'affected':
        affected(args.base)
    else:
        generate_full_map()


if __name__ == '__main__':
    main()
