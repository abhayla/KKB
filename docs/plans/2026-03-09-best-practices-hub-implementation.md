# Best Practices Hub — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a GitHub template repo (`claude-best-practices`) that collates reusable Claude Code patterns from multiple projects, scans the internet for best practices, and auto-syncs updates via PRs.

**Architecture:** Layered repo (core + opt-in stacks) with GitHub Actions for scanning/syncing, Python scripts for collation/dedup/doc generation, and 4 Claude Code skills for local interaction. All changes go through PR review.

**Tech Stack:** Python 3.12, GitHub Actions, Claude API (Haiku for semantic dedup), PyYAML, requests, beautifulsoup4, pytest, Jinja2 (templates)

**Design Doc:** `docs/plans/2026-03-09-best-practices-hub-design.md`

---

## Phase Overview

| Phase | Tasks | What It Delivers |
|-------|-------|-----------------|
| 1 | 1-4 | Repo skeleton, config files, registry, README |
| 2 | 5-8 | Core patterns seeded from KKB (skills, agents, hooks, rules) |
| 3 | 9-12 | Python scripts: dedup, collate, bootstrap |
| 4 | 13-16 | Python scripts: scan_web, sync, generate_docs, freshness |
| 5 | 17-20 | GitHub Actions workflows (scan, sync, validate, docs) |
| 6 | 21-24 | Claude Code skills (update-practices, contribute-practice, scan-url, scan-repo) |
| 7 | 25-27 | HTML dashboard, bootstrap.sh, template repo config |
| 8 | 28-29 | Seed from KKB, end-to-end validation |

---

## Task 1: Create GitHub Repo and Directory Skeleton

**Files:**
- Create: `claude-best-practices/` (new repo root — all paths below are relative to this)
- Create: `core/.claude/skills/.gitkeep`
- Create: `core/.claude/agents/.gitkeep`
- Create: `core/.claude/hooks/.gitkeep`
- Create: `core/.claude/rules/.gitkeep`
- Create: `stacks/superpowers/.claude/skills/.gitkeep`
- Create: `stacks/android-compose/.claude/skills/.gitkeep`
- Create: `stacks/android-compose/.claude/agents/.gitkeep`
- Create: `stacks/android-compose/.claude/rules/.gitkeep`
- Create: `stacks/android-compose/examples/.gitkeep`
- Create: `stacks/fastapi-python/.claude/skills/.gitkeep`
- Create: `stacks/fastapi-python/.claude/rules/.gitkeep`
- Create: `stacks/fastapi-python/examples/.gitkeep`
- Create: `stacks/ai-gemini/.claude/skills/.gitkeep`
- Create: `stacks/ai-gemini/examples/.gitkeep`
- Create: `stacks/firebase-auth/examples/.gitkeep`
- Create: `stacks/react-nextjs/examples/.gitkeep`
- Create: `internet-sources/pending/.gitkeep`
- Create: `internet-sources/archived/.gitkeep`
- Create: `registry/`
- Create: `config/`
- Create: `docs/`
- Create: `scripts/tests/fixtures/`
- Create: `.github/workflows/`

**Step 1: Create the GitHub repo**

```bash
cd C:/Abhay/VibeCoding
gh repo create abhayla/claude-best-practices --public --description "Reusable Claude Code best practices: skills, agents, hooks, rules. Template repo with auto-sync." --clone
cd claude-best-practices
```

**Step 2: Create directory structure**

```bash
# Core
mkdir -p core/.claude/{skills,agents,hooks,rules}

# Stacks
mkdir -p stacks/superpowers/.claude/skills
mkdir -p stacks/superpowers/examples
mkdir -p stacks/android-compose/.claude/{skills,agents,rules}
mkdir -p stacks/android-compose/examples
mkdir -p stacks/fastapi-python/.claude/{skills,rules}
mkdir -p stacks/fastapi-python/examples
mkdir -p stacks/ai-gemini/.claude/skills
mkdir -p stacks/ai-gemini/examples
mkdir -p stacks/firebase-auth/examples
mkdir -p stacks/react-nextjs/examples

# Infrastructure
mkdir -p internet-sources/{pending,archived}
mkdir -p registry
mkdir -p config
mkdir -p docs
mkdir -p scripts/tests/fixtures
mkdir -p .github/workflows

# Gitkeep for empty dirs
find . -type d -empty -not -path './.git/*' -exec touch {}/.gitkeep \;
```

**Step 3: Create .gitignore**

```gitignore
# .gitignore
__pycache__/
*.pyc
.pytest_cache/
*.egg-info/
dist/
build/
.env
venv/
.venv/
```

**Step 4: Commit**

```bash
git add -A
git commit -m "chore: initialize repo skeleton with core + stacks + infrastructure dirs"
```

---

## Task 2: Create Configuration Files

**Files:**
- Create: `config/repos.yml`
- Create: `config/topics.yml`
- Create: `config/urls.yml`
- Create: `config/settings.yml`
- Create: `config/.secretsignore`

**Step 1: Create repos.yml**

```yaml
# config/repos.yml
# Registered project repos to scan for patterns
repos:
  - repo: abhayla/KKB
    stacks: [android-compose, fastapi-python, ai-gemini, firebase-auth]
    auto_sync: true
```

**Step 2: Create topics.yml**

```yaml
# config/topics.yml
# Topics to search for best practices
topics:
  - name: claude-code-skills
    keywords: ["Claude Code skills best practices", "SKILL.md patterns"]
    category: core
  - name: claude-code-hooks
    keywords: ["Claude Code hooks automation", "PreToolUse PostToolUse hooks"]
    category: core
  - name: claude-code-agents
    keywords: ["Claude Code agents subagent patterns"]
    category: core
  - name: jetpack-compose-testing
    keywords: ["Jetpack Compose testing patterns 2026", "Compose UI test best practices"]
    category: stack:android-compose
  - name: fastapi-patterns
    keywords: ["FastAPI best practices 2026", "FastAPI testing patterns"]
    category: stack:fastapi-python
  - name: gemini-api
    keywords: ["Google Gemini API structured output", "Gemini 2.5 Flash best practices"]
    category: stack:ai-gemini
```

**Step 3: Create urls.yml**

```yaml
# config/urls.yml
# URLs/blogs to watch for updates
urls:
  - url: https://code.claude.com/docs/en/skills
    last_verified: "2026-03-09"
    expires_after: 90d
    trust_level: high
  - url: https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices
    last_verified: "2026-03-09"
    expires_after: 90d
    trust_level: high
  - url: https://github.com/hesreallyhim/awesome-claude-code
    last_verified: "2026-03-09"
    expires_after: 60d
    trust_level: high
```

**Step 4: Create settings.yml**

```yaml
# config/settings.yml
# Global settings for the best practices hub
scan:
  project_schedule: "0 9 * * 1"    # Monday 9am UTC
  internet_schedule: "0 10 * * 1"  # Monday 10am UTC
  expire_schedule: "0 8 * * 1"     # Monday 8am UTC

limits:
  max_prs_per_scan: 5
  max_urls_per_scan: 20
  max_semantic_comparisons: 50
  claude_api_monthly_budget_usd: 5.0

pr:
  labels_project: ["auto-scan", "project-source"]
  labels_internet: ["auto-scan", "internet-source"]
  labels_contribution: ["contribution"]
  labels_sync: ["best-practices", "auto-sync"]

dedup:
  semantic_threshold_strong: 85
  semantic_threshold_weak: 70
  structural_threshold: 3
```

**Step 5: Create .secretsignore**

```
# config/.secretsignore
# Patterns to never sync (private/sensitive content)
# One pattern name per line
test-accounts
```

**Step 6: Commit**

```bash
git add config/
git commit -m "chore: add configuration files (repos, topics, urls, settings)"
```

---

## Task 3: Create Registry and Stack Configs

**Files:**
- Create: `registry/patterns.json`
- Create: `registry/changelog.md`
- Create: `stacks/superpowers/stack-config.yml`
- Create: `stacks/android-compose/stack-config.yml`
- Create: `stacks/fastapi-python/stack-config.yml`
- Create: `stacks/ai-gemini/stack-config.yml`
- Create: `stacks/firebase-auth/stack-config.yml`
- Create: `stacks/react-nextjs/stack-config.yml`

**Step 1: Create empty registry**

```json
{
  "_meta": {
    "version": "1.0.0",
    "last_updated": "2026-03-09",
    "total_patterns": 0
  }
}
```

**Step 2: Create changelog.md**

```markdown
# Changelog

All notable pattern additions, updates, and removals.

## [Unreleased]

_Initial setup — no patterns yet._
```

**Step 3: Create stack-config.yml for each stack**

```yaml
# stacks/superpowers/stack-config.yml
name: superpowers
description: Brainstorming, TDD, debugging, code-review, and planning skills from the superpowers marketplace
namespace: superpowers
conflicts_with: []
merges_with: [android-compose, fastapi-python, ai-gemini, firebase-auth, react-nextjs]
file_precedence: stack
dependencies: []
```

```yaml
# stacks/android-compose/stack-config.yml
name: android-compose
description: Android Jetpack Compose with Hilt DI, Room DB, Navigation Compose
namespace: android
conflicts_with: []
merges_with: [fastapi-python, superpowers, ai-gemini, firebase-auth]
file_precedence: stack
dependencies: []
```

```yaml
# stacks/fastapi-python/stack-config.yml
name: fastapi-python
description: Python FastAPI backend with SQLAlchemy async, PostgreSQL, Alembic migrations
namespace: fastapi
conflicts_with: []
merges_with: [android-compose, superpowers, ai-gemini, firebase-auth]
file_precedence: stack
dependencies: []
```

```yaml
# stacks/ai-gemini/stack-config.yml
name: ai-gemini
description: Google Gemini API integration for AI-powered features (structured output, vision)
namespace: gemini
conflicts_with: []
merges_with: [android-compose, fastapi-python, superpowers]
file_precedence: stack
dependencies: []
```

```yaml
# stacks/firebase-auth/stack-config.yml
name: firebase-auth
description: Firebase Authentication (Phone OTP) with backend JWT verification
namespace: firebase
conflicts_with: []
merges_with: [android-compose, fastapi-python, superpowers]
file_precedence: stack
dependencies: []
```

```yaml
# stacks/react-nextjs/stack-config.yml
name: react-nextjs
description: React + Next.js frontend (future stack placeholder)
namespace: react
conflicts_with: [android-compose]
merges_with: [fastapi-python, superpowers]
file_precedence: stack
dependencies: []
```

**Step 4: Commit**

```bash
git add registry/ stacks/*/stack-config.yml
git commit -m "chore: add registry and stack configuration files"
```

---

## Task 4: Create CLAUDE.md Templates and README

**Files:**
- Create: `core/CLAUDE.md.template`
- Create: `core/CLAUDE.local.md.template`
- Create: `README.md`

**Step 1: Create CLAUDE.md.template**

```markdown
# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

**{{PROJECT_NAME}}** — {{PROJECT_DESCRIPTION}}

| Attribute | Details |
|-----------|---------|
| **Platform** | {{PLATFORM}} |
| **Build Tools** | {{BUILD_TOOLS}} |

## Development Commands

{{DEVELOPMENT_COMMANDS}}

## Patterns

{{#if STACK_ANDROID}}
### ViewModel Pattern
ViewModels extend `BaseViewModel<T : BaseUiState>`, providing `updateState`/`setState` helpers.
{{/if}}

{{#if STACK_FASTAPI}}
### Repository Pattern
Repositories abstract data access behind interfaces. Use async SQLAlchemy for all DB operations.
{{/if}}

## Troubleshooting

| Issue | Solution |
|-------|----------|
| | |

## Rules for Claude

1. **Bash Syntax**: Use forward slashes `/`, quote paths with spaces. Shell is Unix-style bash even on Windows.
2. **Document Output**: Generated documents go to `docs/claude-docs/`.

{{#if STACK_ANDROID}}
3. **Offline-First**: All features must use Room as source of truth with offline support.
{{/if}}

## Claude Code Configuration

The `.claude/` directory contains skills, agents, hooks, and rules.
Synced from: {{HUB_REPO}} ({{SELECTED_STACKS}})
Last sync: {{LAST_SYNC_TIMESTAMP}}
```

**Step 2: Create CLAUDE.local.md.template**

```markdown
# Personal Project Preferences

## Local Environment
{{LOCAL_ENV_NOTES}}

## Workflow Preferences
- Prefer running single tests over full suite for speed

## Quick Commands
```bash
# Add your frequently used commands here
```
```

**Step 3: Create README.md**

```markdown
# Claude Best Practices Hub

Reusable Claude Code best practices: skills, agents, hooks, and rules. Collated from real projects, enriched from internet sources, and auto-synced across your repositories.

## Quick Start

### Option A: Use as GitHub Template
1. Click **"Use this template"** on GitHub
2. Clone your new repo
3. Run the bootstrap script:
   ```bash
   python scripts/bootstrap.py --stacks android-compose,fastapi-python
   ```

### Option B: Bootstrap Existing Project
```bash
curl -sL https://raw.githubusercontent.com/abhayla/claude-best-practices/main/bootstrap.sh | bash -s -- --stacks android-compose,fastapi-python
```

### Option C: Copy Individual Patterns
Browse `core/` and `stacks/` directories, copy what you need.

## Structure

| Directory | Purpose |
|-----------|---------|
| `core/` | Universal patterns (always included) |
| `stacks/` | Stack-specific patterns (opt-in) |
| `config/` | Scan configuration (repos, topics, URLs) |
| `registry/` | Pattern registry with dedup and provenance |
| `scripts/` | Python tools for collation, scanning, syncing |
| `docs/` | Auto-generated documentation and dashboard |

## Available Stacks

| Stack | Description |
|-------|-------------|
| `superpowers` | Brainstorming, TDD, debugging, planning skills |
| `android-compose` | Jetpack Compose, Hilt, Room, Navigation |
| `fastapi-python` | FastAPI, SQLAlchemy async, PostgreSQL, Alembic |
| `ai-gemini` | Google Gemini API, structured output, vision |
| `firebase-auth` | Firebase Phone Auth, JWT verification |
| `react-nextjs` | React + Next.js (placeholder) |

## Skills

| Skill | Purpose |
|-------|---------|
| `/update-practices` | Pull latest from hub into your project |
| `/contribute-practice` | Push a pattern from your project to the hub |
| `/scan-url` | Trigger internet scan for a URL or topic |
| `/scan-repo` | Trigger project scan for a repository |

## How It Works

See [docs/SYNC-ARCHITECTURE.md](docs/SYNC-ARCHITECTURE.md) for the full sync flow.

## License

MIT
```

**Step 4: Commit**

```bash
git add core/CLAUDE.md.template core/CLAUDE.local.md.template README.md
git commit -m "chore: add CLAUDE.md templates and README"
```

---

## Task 5: Create Test Fixtures

**Files:**
- Create: `scripts/tests/fixtures/sample_skill/SKILL.md`
- Create: `scripts/tests/fixtures/sample_agent.md`
- Create: `scripts/tests/fixtures/sample_hook.sh`
- Create: `scripts/tests/fixtures/invalid_skill/SKILL.md`
- Create: `scripts/tests/fixtures/duplicate_skill/SKILL.md`
- Create: `scripts/tests/fixtures/sample_registry.json`
- Create: `scripts/tests/fixtures/sample_urls.yml`
- Create: `scripts/tests/fixtures/sample_webpage.html`
- Create: `scripts/tests/conftest.py`
- Create: `scripts/requirements.txt`

**Step 1: Create sample_skill/SKILL.md**

```markdown
---
name: sample-skill
description: A sample skill for testing pattern extraction and dedup
version: "1.0.0"
allowed-tools: "Bash Read Grep"
---

# Sample Skill

This is a test skill used by the test suite.

## Steps
1. Do something
2. Do something else
```

**Step 2: Create sample_agent.md**

```markdown
---
name: sample-agent
description: A sample agent for testing
model: sonnet
version: "1.0.0"
---

# Sample Agent

Test agent for pattern extraction tests.
```

**Step 3: Create sample_hook.sh**

```bash
#!/bin/bash
# version: 1.0.0
# description: Sample hook for testing
echo "sample hook executed"
```

**Step 4: Create invalid_skill/SKILL.md (no frontmatter)**

```markdown
# Invalid Skill

This skill has no YAML frontmatter and should fail validation.
```

**Step 5: Create duplicate_skill/SKILL.md (exact copy of sample)**

```markdown
---
name: sample-skill
description: A sample skill for testing pattern extraction and dedup
version: "1.0.0"
allowed-tools: "Bash Read Grep"
---

# Sample Skill

This is a test skill used by the test suite.

## Steps
1. Do something
2. Do something else
```

**Step 6: Create sample_registry.json**

```json
{
  "_meta": {
    "version": "1.0.0",
    "last_updated": "2026-03-09",
    "total_patterns": 2
  },
  "fix-loop": {
    "hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "type": "skill",
    "category": "core",
    "version": "1.2.0",
    "source": "project:abhayla/KKB",
    "discovered": "2026-02-10",
    "last_updated": "2026-03-05",
    "dependencies": ["hook-utils.sh"],
    "visibility": "public",
    "description": "Iterative test-fix cycle with thinking escalation",
    "tags": ["testing", "debugging"],
    "changelog": "v1.2: Added thinking escalation"
  },
  "auto-format": {
    "hash": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
    "type": "hook",
    "category": "core",
    "version": "1.0.0",
    "source": "project:abhayla/KKB",
    "discovered": "2026-02-10",
    "last_updated": "2026-02-10",
    "dependencies": ["hook-utils.sh"],
    "visibility": "public",
    "description": "Auto-format Python with Black and Ruff after edits",
    "tags": ["formatting", "python"],
    "changelog": "v1.0: Initial"
  }
}
```

**Step 7: Create sample_urls.yml**

```yaml
urls:
  - url: https://example.com/claude-tips
    last_verified: "2026-01-01"
    expires_after: 30d
    trust_level: high
  - url: https://example.com/old-post
    last_verified: "2025-01-01"
    expires_after: 30d
    trust_level: low
```

**Step 8: Create sample_webpage.html**

```html
<!DOCTYPE html>
<html>
<head><title>Claude Code Tips</title></head>
<body>
<h1>Best Practices for Claude Code Skills</h1>
<p>Here are some patterns:</p>
<h2>Auto-Retry Hook</h2>
<pre><code>#!/bin/bash
# Auto-retry failed commands
if [ $? -ne 0 ]; then
  sleep 2 && "$@"
fi
</code></pre>
<h2>Test Coverage Skill</h2>
<p>A skill that checks test coverage before allowing commits.</p>
<pre><code>---
name: test-coverage
description: Check test coverage before commits
version: "1.0.0"
---
</code></pre>
</body>
</html>
```

**Step 9: Create conftest.py**

```python
"""Shared test fixtures for best practices hub scripts."""

import json
from pathlib import Path

import pytest
import yaml


FIXTURES_DIR = Path(__file__).parent / "fixtures"


@pytest.fixture
def fixtures_dir():
    return FIXTURES_DIR


@pytest.fixture
def sample_registry():
    with open(FIXTURES_DIR / "sample_registry.json") as f:
        return json.load(f)


@pytest.fixture
def sample_urls():
    with open(FIXTURES_DIR / "sample_urls.yml") as f:
        return yaml.safe_load(f)


@pytest.fixture
def sample_skill_path():
    return FIXTURES_DIR / "sample_skill" / "SKILL.md"


@pytest.fixture
def invalid_skill_path():
    return FIXTURES_DIR / "invalid_skill" / "SKILL.md"


@pytest.fixture
def duplicate_skill_path():
    return FIXTURES_DIR / "duplicate_skill" / "SKILL.md"


@pytest.fixture
def sample_webpage():
    with open(FIXTURES_DIR / "sample_webpage.html") as f:
        return f.read()


@pytest.fixture
def temp_registry(tmp_path):
    """Create a temporary registry for write tests."""
    registry = {
        "_meta": {"version": "1.0.0", "last_updated": "2026-03-09", "total_patterns": 0}
    }
    path = tmp_path / "patterns.json"
    path.write_text(json.dumps(registry, indent=2))
    return path
```

**Step 10: Create requirements.txt**

```
pyyaml>=6.0
requests>=2.31
beautifulsoup4>=4.12
anthropic>=0.40
pytest>=8.0
jinja2>=3.1
```

**Step 11: Commit**

```bash
git add scripts/tests/ scripts/requirements.txt
git commit -m "chore: add test fixtures and requirements"
```

---

## Task 6: Implement dedup_check.py (Level 1 + 2)

**Files:**
- Create: `scripts/dedup_check.py`
- Create: `scripts/tests/test_dedup_check.py`

**Step 1: Write the failing tests**

```python
# scripts/tests/test_dedup_check.py
"""Tests for 3-level deduplication logic."""

import json
from pathlib import Path

import pytest

from scripts.dedup_check import (
    hash_pattern,
    check_exact_duplicate,
    check_structural_duplicate,
    parse_frontmatter,
    validate_pattern_integrity,
)


class TestHashPattern:
    def test_same_content_same_hash(self, tmp_path):
        f1 = tmp_path / "a.md"
        f2 = tmp_path / "b.md"
        f1.write_text("hello world")
        f2.write_text("hello world")
        assert hash_pattern(str(f1)) == hash_pattern(str(f2))

    def test_whitespace_normalized(self, tmp_path):
        f1 = tmp_path / "a.md"
        f2 = tmp_path / "b.md"
        f1.write_text("hello  world  ")
        f2.write_text("hello world")
        assert hash_pattern(str(f1)) == hash_pattern(str(f2))

    def test_different_content_different_hash(self, tmp_path):
        f1 = tmp_path / "a.md"
        f2 = tmp_path / "b.md"
        f1.write_text("hello")
        f2.write_text("world")
        assert hash_pattern(str(f1)) != hash_pattern(str(f2))


class TestExactDuplicate:
    def test_finds_exact_match(self, sample_registry):
        known_hash = sample_registry["fix-loop"]["hash"]
        result = check_exact_duplicate(known_hash, sample_registry)
        assert result == "fix-loop"

    def test_no_match_returns_none(self, sample_registry):
        result = check_exact_duplicate("nonexistent_hash", sample_registry)
        assert result is None

    def test_skips_meta_key(self, sample_registry):
        result = check_exact_duplicate("_meta", sample_registry)
        assert result is None


class TestStructuralDuplicate:
    def test_same_name_matches(self, sample_registry):
        new = {"name": "fix-loop", "type": "skill", "category": "core", "dependencies": []}
        matches = check_structural_duplicate(new, sample_registry)
        assert "fix-loop" in matches

    def test_case_insensitive_name(self, sample_registry):
        new = {"name": "Fix-Loop", "type": "skill", "category": "core", "dependencies": []}
        matches = check_structural_duplicate(new, sample_registry)
        assert "fix-loop" in matches

    def test_no_match_for_different_pattern(self, sample_registry):
        new = {"name": "brand-new", "type": "agent", "category": "stack:react", "dependencies": []}
        matches = check_structural_duplicate(new, sample_registry)
        assert len(matches) == 0

    def test_shared_deps_increase_score(self, sample_registry):
        new = {"name": "different-name", "type": "skill", "category": "core", "dependencies": ["hook-utils.sh"]}
        matches = check_structural_duplicate(new, sample_registry)
        # type(1) + category(1) + dep(1) = 3, meets threshold
        assert "fix-loop" in matches


class TestParseFrontmatter:
    def test_valid_frontmatter(self, sample_skill_path):
        fm = parse_frontmatter(sample_skill_path)
        assert fm["name"] == "sample-skill"
        assert fm["version"] == "1.0.0"

    def test_missing_frontmatter(self, invalid_skill_path):
        fm = parse_frontmatter(invalid_skill_path)
        assert fm is None


class TestValidateIntegrity:
    def test_valid_pattern_passes(self, sample_skill_path):
        errors = validate_pattern_integrity(sample_skill_path)
        assert len(errors) == 0

    def test_missing_frontmatter_fails(self, invalid_skill_path):
        errors = validate_pattern_integrity(invalid_skill_path)
        assert any("frontmatter" in e.lower() for e in errors)
```

**Step 2: Run tests to verify they fail**

```bash
cd claude-best-practices
pip install -r scripts/requirements.txt
PYTHONPATH=. pytest scripts/tests/test_dedup_check.py -v
```

Expected: FAIL — `ModuleNotFoundError: No module named 'scripts.dedup_check'`

**Step 3: Implement dedup_check.py**

```python
# scripts/dedup_check.py
"""Three-level deduplication for best practices patterns.

Level 1: Exact hash match (SHA256 of normalized content)
Level 2: Structural match (name + type + category + dependencies)
Level 3: Semantic similarity (Claude API — separate function, called by scan_web.py)
"""

import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Optional

import yaml


def hash_pattern(file_path: str) -> str:
    """SHA256 of file content, normalized (collapse whitespace, strip trailing)."""
    content = Path(file_path).read_text(encoding="utf-8")
    lines = [line.strip() for line in content.splitlines()]
    normalized = "\n".join(lines)
    # Collapse multiple spaces
    normalized = re.sub(r"  +", " ", normalized)
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()


def check_exact_duplicate(new_hash: str, registry: dict) -> Optional[str]:
    """Returns existing pattern name if exact hash match found."""
    for name, entry in registry.items():
        if name.startswith("_"):
            continue
        if isinstance(entry, dict) and entry.get("hash") == new_hash:
            return name
    return None


def check_structural_duplicate(
    new_pattern: dict, registry: dict, threshold: int = 3
) -> list[str]:
    """Find patterns with similar name, type, category, or dependencies."""
    matches = []
    for name, entry in registry.items():
        if name.startswith("_"):
            continue
        if not isinstance(entry, dict):
            continue

        score = 0
        # Same name (case-insensitive): strong signal
        if new_pattern.get("name", "").lower() == name.lower():
            score += 3
        # Same type
        if new_pattern.get("type") == entry.get("type"):
            score += 1
        # Same category
        if new_pattern.get("category") == entry.get("category"):
            score += 1
        # Shared dependencies
        new_deps = set(new_pattern.get("dependencies", []))
        existing_deps = set(entry.get("dependencies", []))
        score += len(new_deps & existing_deps)

        if score >= threshold:
            matches.append(name)

    return matches


def parse_frontmatter(file_path: Path) -> Optional[dict]:
    """Extract YAML frontmatter from a markdown file."""
    content = Path(file_path).read_text(encoding="utf-8")
    match = re.match(r"^---\s*\n(.*?)\n---", content, re.DOTALL)
    if not match:
        return None
    try:
        return yaml.safe_load(match.group(1))
    except yaml.YAMLError:
        return None


def validate_pattern_integrity(file_path: Path) -> list[str]:
    """Validate a pattern file has required structure. Returns list of errors."""
    errors = []
    path = Path(file_path)

    if not path.exists():
        return [f"File not found: {path}"]

    fm = parse_frontmatter(path)
    if fm is None:
        errors.append("Missing or invalid YAML frontmatter")
        return errors

    if "name" not in fm:
        errors.append("Frontmatter missing 'name' field")
    if "description" not in fm:
        errors.append("Frontmatter missing 'description' field")
    if "version" not in fm:
        errors.append("Frontmatter missing 'version' field")

    return errors


def validate_registry(registry_path: Path, patterns_root: Path) -> list[str]:
    """Validate registry consistency with actual files."""
    errors = []
    with open(registry_path) as f:
        registry = json.load(f)

    for name, entry in registry.items():
        if name.startswith("_"):
            continue
        if not isinstance(entry, dict):
            errors.append(f"Invalid entry for '{name}': not a dict")
            continue

        required = ["hash", "type", "category", "version", "source"]
        for field in required:
            if field not in entry:
                errors.append(f"Pattern '{name}' missing field: {field}")

    return errors


def scan_for_secrets(file_path: Path) -> list[str]:
    """Scan a file for common secret patterns."""
    content = Path(file_path).read_text(encoding="utf-8")
    findings = []
    patterns = [
        (r"sk-ant-[a-zA-Z0-9_-]{20,}", "Anthropic API key"),
        (r"AIza[a-zA-Z0-9_-]{35}", "Google API key"),
        (r"ghp_[a-zA-Z0-9]{36}", "GitHub PAT"),
        (r"AKIA[A-Z0-9]{16}", "AWS access key"),
        (r'password\s*=\s*["\'][^"\']+["\']', "Hardcoded password"),
        (r'secret\s*=\s*["\'][^"\']+["\']', "Hardcoded secret"),
    ]
    for pattern, desc in patterns:
        if re.search(pattern, content):
            findings.append(f"{desc} found in {file_path}")
    return findings


if __name__ == "__main__":
    # CLI: python scripts/dedup_check.py --validate-all
    if "--validate-all" in sys.argv:
        root = Path(__file__).parent.parent
        registry_path = root / "registry" / "patterns.json"
        if registry_path.exists():
            errors = validate_registry(registry_path, root)
            if errors:
                print("Validation errors:")
                for e in errors:
                    print(f"  - {e}")
                sys.exit(1)
            else:
                print("Registry validation passed")
        else:
            print("No registry found — skipping")
    elif "--secret-scan" in sys.argv:
        root = Path(__file__).parent.parent
        all_findings = []
        for ext in ["*.md", "*.sh", "*.py", "*.yml", "*.yaml", "*.json"]:
            for f in root.rglob(ext):
                if ".git" in str(f) or "node_modules" in str(f):
                    continue
                all_findings.extend(scan_for_secrets(f))
        if all_findings:
            print("Secret scan findings:")
            for finding in all_findings:
                print(f"  - {finding}")
            sys.exit(1)
        else:
            print("No secrets found")
```

**Step 4: Run tests to verify they pass**

```bash
PYTHONPATH=. pytest scripts/tests/test_dedup_check.py -v
```

Expected: ALL PASS

**Step 5: Commit**

```bash
git add scripts/dedup_check.py scripts/tests/test_dedup_check.py
git commit -m "feat: add dedup_check.py with Level 1 (hash) and Level 2 (structural) dedup"
```

---

## Task 7: Implement collate.py

**Files:**
- Create: `scripts/collate.py`
- Create: `scripts/tests/test_collate.py`

**Step 1: Write the failing tests**

```python
# scripts/tests/test_collate.py
"""Tests for pattern collation from project repos."""

import json
from pathlib import Path

import pytest

from scripts.collate import (
    extract_patterns_from_dir,
    detect_pattern_type,
    build_pattern_entry,
)


class TestDetectPatternType:
    def test_skill_directory(self, tmp_path):
        skill_dir = tmp_path / "skills" / "my-skill"
        skill_dir.mkdir(parents=True)
        (skill_dir / "SKILL.md").write_text("---\nname: my-skill\n---\n# Skill")
        assert detect_pattern_type(skill_dir / "SKILL.md") == "skill"

    def test_agent_file(self, tmp_path):
        agents_dir = tmp_path / "agents"
        agents_dir.mkdir()
        agent = agents_dir / "debugger.md"
        agent.write_text("---\nname: debugger\n---\n# Agent")
        assert detect_pattern_type(agent) == "agent"

    def test_hook_file(self, tmp_path):
        hooks_dir = tmp_path / "hooks"
        hooks_dir.mkdir()
        hook = hooks_dir / "auto-format.sh"
        hook.write_text("#!/bin/bash\necho hi")
        assert detect_pattern_type(hook) == "hook"

    def test_rule_file(self, tmp_path):
        rules_dir = tmp_path / "rules"
        rules_dir.mkdir()
        rule = rules_dir / "workflow.md"
        rule.write_text("---\npaths:\n  - '**/*.py'\n---\n# Rules")
        assert detect_pattern_type(rule) == "rule"


class TestExtractPatterns:
    def test_extracts_skills(self, tmp_path):
        skills = tmp_path / ".claude" / "skills" / "test-skill"
        skills.mkdir(parents=True)
        (skills / "SKILL.md").write_text("---\nname: test-skill\nversion: '1.0.0'\ndescription: test\n---\n# Test")
        patterns = extract_patterns_from_dir(tmp_path / ".claude")
        assert len(patterns) >= 1
        assert any(p["name"] == "test-skill" for p in patterns)

    def test_skips_gitkeep(self, tmp_path):
        claude = tmp_path / ".claude" / "skills"
        claude.mkdir(parents=True)
        (claude / ".gitkeep").write_text("")
        patterns = extract_patterns_from_dir(tmp_path / ".claude")
        assert len(patterns) == 0

    def test_handles_empty_dir(self, tmp_path):
        claude = tmp_path / ".claude"
        claude.mkdir()
        patterns = extract_patterns_from_dir(claude)
        assert len(patterns) == 0


class TestBuildPatternEntry:
    def test_builds_valid_entry(self, sample_skill_path):
        entry = build_pattern_entry(
            name="sample-skill",
            pattern_type="skill",
            file_path=sample_skill_path,
            source="project:test/repo",
            category="core",
        )
        assert entry["type"] == "skill"
        assert entry["source"] == "project:test/repo"
        assert "hash" in entry
        assert "version" in entry
```

**Step 2: Run tests — expect FAIL**

```bash
PYTHONPATH=. pytest scripts/tests/test_collate.py -v
```

**Step 3: Implement collate.py**

```python
# scripts/collate.py
"""Extract reusable patterns from project repositories."""

import json
import os
import subprocess
import sys
import tempfile
from datetime import date
from pathlib import Path
from typing import Optional

import yaml

from scripts.dedup_check import hash_pattern, parse_frontmatter


def detect_pattern_type(file_path: Path) -> Optional[str]:
    """Detect pattern type from file location."""
    parts = file_path.parts
    for i, part in enumerate(parts):
        if part == "skills":
            return "skill"
        if part == "agents":
            return "agent"
        if part == "hooks":
            return "hook"
        if part == "rules":
            return "rule"
    return None


def extract_patterns_from_dir(claude_dir: Path) -> list[dict]:
    """Extract all patterns from a .claude/ directory."""
    patterns = []

    if not claude_dir.exists():
        return patterns

    # Skills: .claude/skills/*/SKILL.md
    skills_dir = claude_dir / "skills"
    if skills_dir.exists():
        for skill_dir in skills_dir.iterdir():
            if not skill_dir.is_dir():
                continue
            skill_file = skill_dir / "SKILL.md"
            if skill_file.exists():
                fm = parse_frontmatter(skill_file)
                if fm and "name" in fm:
                    patterns.append({
                        "name": fm["name"],
                        "type": "skill",
                        "file_path": str(skill_file),
                        "frontmatter": fm,
                    })

    # Agents: .claude/agents/*.md
    agents_dir = claude_dir / "agents"
    if agents_dir.exists():
        for agent_file in agents_dir.glob("*.md"):
            fm = parse_frontmatter(agent_file)
            name = fm.get("name") if fm else agent_file.stem
            if name:
                patterns.append({
                    "name": name,
                    "type": "agent",
                    "file_path": str(agent_file),
                    "frontmatter": fm or {},
                })

    # Hooks: .claude/hooks/*.sh
    hooks_dir = claude_dir / "hooks"
    if hooks_dir.exists():
        for hook_file in hooks_dir.glob("*.sh"):
            patterns.append({
                "name": hook_file.stem,
                "type": "hook",
                "file_path": str(hook_file),
                "frontmatter": {},
            })
        # Also .py hooks
        for hook_file in hooks_dir.glob("*.py"):
            if hook_file.name == "__pycache__":
                continue
            patterns.append({
                "name": hook_file.stem,
                "type": "hook",
                "file_path": str(hook_file),
                "frontmatter": {},
            })

    # Rules: .claude/rules/*.md
    rules_dir = claude_dir / "rules"
    if rules_dir.exists():
        for rule_file in rules_dir.glob("*.md"):
            fm = parse_frontmatter(rule_file)
            patterns.append({
                "name": rule_file.stem,
                "type": "rule",
                "file_path": str(rule_file),
                "frontmatter": fm or {},
            })

    return patterns


def build_pattern_entry(
    name: str,
    pattern_type: str,
    file_path: Path,
    source: str,
    category: str = "core",
) -> dict:
    """Build a registry entry for a pattern."""
    fm = parse_frontmatter(file_path) or {}
    return {
        "hash": hash_pattern(str(file_path)),
        "type": pattern_type,
        "category": category,
        "version": fm.get("version", "1.0.0"),
        "source": source,
        "discovered": date.today().isoformat(),
        "last_updated": date.today().isoformat(),
        "dependencies": fm.get("dependencies", []),
        "visibility": "public",
        "description": fm.get("description", ""),
        "tags": fm.get("tags", []),
        "changelog": f"v{fm.get('version', '1.0.0')}: Initial import",
    }


def collate_repo(repo_url: str, registry: dict) -> list[dict]:
    """Clone a repo and extract new/updated patterns."""
    with tempfile.TemporaryDirectory() as tmpdir:
        # Sparse clone — only .claude/ directory
        subprocess.run(
            ["git", "clone", "--depth=1", "--filter=blob:none", "--sparse", repo_url, tmpdir],
            capture_output=True, text=True, check=True,
        )
        subprocess.run(
            ["git", "-C", tmpdir, "sparse-checkout", "set", ".claude/"],
            capture_output=True, text=True, check=True,
        )

        claude_dir = Path(tmpdir) / ".claude"
        return extract_patterns_from_dir(claude_dir)


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Collate patterns from project repos")
    parser.add_argument("--repo", help="Specific repo to scan (owner/repo)")
    parser.add_argument("--all", action="store_true", help="Scan all repos from config/repos.yml")
    args = parser.parse_args()

    root = Path(__file__).parent.parent
    config_path = root / "config" / "repos.yml"

    if args.repo:
        repos = [{"repo": args.repo}]
    elif args.all and config_path.exists():
        with open(config_path) as f:
            repos = yaml.safe_load(f).get("repos", [])
    else:
        print("Usage: collate.py --repo owner/repo OR --all")
        sys.exit(1)

    registry_path = root / "registry" / "patterns.json"
    with open(registry_path) as f:
        registry = json.load(f)

    for repo_config in repos:
        repo = repo_config["repo"]
        print(f"Scanning {repo}...")
        try:
            patterns = collate_repo(f"https://github.com/{repo}.git", registry)
            print(f"  Found {len(patterns)} patterns")
            for p in patterns:
                print(f"  - {p['name']} ({p['type']})")
        except Exception as e:
            print(f"  Error: {e}")
```

**Step 4: Run tests**

```bash
PYTHONPATH=. pytest scripts/tests/test_collate.py -v
```

Expected: ALL PASS

**Step 5: Commit**

```bash
git add scripts/collate.py scripts/tests/test_collate.py
git commit -m "feat: add collate.py for extracting patterns from project repos"
```

---

## Task 8: Implement bootstrap.py

**Files:**
- Create: `scripts/bootstrap.py`
- Create: `scripts/tests/test_bootstrap.py`

**Step 1: Write failing tests**

```python
# scripts/tests/test_bootstrap.py
"""Tests for project bootstrapping from hub patterns."""

import json
import shutil
from pathlib import Path

import pytest
import yaml

from scripts.bootstrap import (
    load_stack_config,
    validate_stack_selection,
    copy_layer,
    render_template,
    generate_sync_config,
)


class TestLoadStackConfig:
    def test_loads_valid_config(self, tmp_path):
        config_file = tmp_path / "stack-config.yml"
        config_file.write_text(yaml.dump({
            "name": "test-stack",
            "namespace": "test",
            "conflicts_with": [],
            "merges_with": [],
        }))
        config = load_stack_config(config_file)
        assert config["name"] == "test-stack"

    def test_missing_file_returns_none(self, tmp_path):
        config = load_stack_config(tmp_path / "nonexistent.yml")
        assert config is None


class TestValidateStackSelection:
    def test_no_conflicts_passes(self):
        configs = {
            "android": {"conflicts_with": []},
            "fastapi": {"conflicts_with": []},
        }
        errors = validate_stack_selection(["android", "fastapi"], configs)
        assert len(errors) == 0

    def test_conflict_detected(self):
        configs = {
            "android": {"conflicts_with": ["react"]},
            "react": {"conflicts_with": ["android"]},
        }
        errors = validate_stack_selection(["android", "react"], configs)
        assert len(errors) > 0

    def test_unknown_stack_rejected(self):
        configs = {"android": {"conflicts_with": []}}
        errors = validate_stack_selection(["android", "nonexistent"], configs)
        assert any("nonexistent" in e for e in errors)


class TestCopyLayer:
    def test_copies_files(self, tmp_path):
        src = tmp_path / "src" / ".claude" / "skills" / "test"
        src.mkdir(parents=True)
        (src / "SKILL.md").write_text("# test")
        dst = tmp_path / "dst"
        dst.mkdir()
        copy_layer(tmp_path / "src", dst)
        assert (dst / ".claude" / "skills" / "test" / "SKILL.md").exists()

    def test_skips_gitkeep(self, tmp_path):
        src = tmp_path / "src" / ".claude" / "skills"
        src.mkdir(parents=True)
        (src / ".gitkeep").write_text("")
        dst = tmp_path / "dst"
        dst.mkdir()
        copy_layer(tmp_path / "src", dst)
        assert not (dst / ".claude" / "skills" / ".gitkeep").exists()


class TestRenderTemplate:
    def test_replaces_variables(self):
        template = "# {{PROJECT_NAME}}\n{{PROJECT_DESCRIPTION}}"
        result = render_template(template, {
            "PROJECT_NAME": "MyApp",
            "PROJECT_DESCRIPTION": "A test app",
        })
        assert "MyApp" in result
        assert "A test app" in result


class TestGenerateSyncConfig:
    def test_generates_valid_yaml(self):
        config = generate_sync_config(
            hub_repo="owner/hub",
            stacks=["android-compose", "fastapi-python"],
        )
        parsed = yaml.safe_load(config)
        assert parsed["hub_repo"] == "owner/hub"
        assert "android-compose" in parsed["selected_stacks"]
```

**Step 2: Run tests — expect FAIL**

```bash
PYTHONPATH=. pytest scripts/tests/test_bootstrap.py -v
```

**Step 3: Implement bootstrap.py**

```python
# scripts/bootstrap.py
"""Bootstrap a new project from the best practices hub."""

import argparse
import re
import shutil
import sys
from datetime import datetime
from pathlib import Path
from typing import Optional

import yaml


def load_stack_config(config_path: Path) -> Optional[dict]:
    """Load a stack's configuration file."""
    if not config_path.exists():
        return None
    with open(config_path) as f:
        return yaml.safe_load(f)


def validate_stack_selection(stacks: list[str], configs: dict) -> list[str]:
    """Validate that selected stacks are compatible."""
    errors = []
    for stack in stacks:
        if stack not in configs:
            errors.append(f"Unknown stack: '{stack}'. Available: {list(configs.keys())}")
            continue
        conflicts = configs[stack].get("conflicts_with", [])
        for other in stacks:
            if other in conflicts:
                errors.append(f"Stack '{stack}' conflicts with '{other}'")
    return errors


def copy_layer(src_dir: Path, dst_dir: Path) -> list[str]:
    """Copy a layer's .claude/ contents to destination. Returns copied file paths."""
    copied = []
    claude_src = src_dir / ".claude"
    if not claude_src.exists():
        return copied

    for src_file in claude_src.rglob("*"):
        if not src_file.is_file():
            continue
        if src_file.name == ".gitkeep":
            continue

        rel = src_file.relative_to(src_dir)
        dst_file = dst_dir / rel
        dst_file.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src_file, dst_file)
        copied.append(str(rel))

    return copied


def render_template(template: str, variables: dict) -> str:
    """Replace {{VARIABLE}} placeholders in template."""
    result = template
    for key, value in variables.items():
        result = result.replace(f"{{{{{key}}}}}", str(value))
    # Remove unreplaced conditionals
    result = re.sub(r"\{\{#if .*?\}\}.*?\{\{/if\}\}", "", result, flags=re.DOTALL)
    return result


def generate_sync_config(hub_repo: str, stacks: list[str], sync_target: str = "project") -> str:
    """Generate a sync-config.yml for a project."""
    config = {
        "hub_repo": hub_repo,
        "sync_target": sync_target,
        "selected_stacks": stacks,
        "last_sync_version": "v1.0",
        "last_sync_timestamp": datetime.utcnow().isoformat() + "Z",
        "auto_check_on_session_start": True,
    }
    return yaml.dump(config, default_flow_style=False, sort_keys=False)


def bootstrap(hub_root: Path, target_dir: Path, stacks: list[str], hub_repo: str, dry_run: bool = False):
    """Bootstrap a project from the hub."""
    # Load all stack configs
    configs = {}
    stacks_dir = hub_root / "stacks"
    if stacks_dir.exists():
        for stack_dir in stacks_dir.iterdir():
            if stack_dir.is_dir():
                cfg = load_stack_config(stack_dir / "stack-config.yml")
                if cfg:
                    configs[cfg["name"]] = cfg

    # Validate
    errors = validate_stack_selection(stacks, configs)
    if errors:
        print("Stack validation errors:")
        for e in errors:
            print(f"  - {e}")
        sys.exit(1)

    if dry_run:
        print(f"DRY RUN: Would bootstrap {target_dir} with stacks: {stacks}")
        print(f"  Core: {hub_root / 'core'}")
        for s in stacks:
            print(f"  Stack: {hub_root / 'stacks' / s}")
        return

    # Copy core layer
    print("Copying core patterns...")
    copied = copy_layer(hub_root / "core", target_dir)
    print(f"  Copied {len(copied)} files")

    # Copy selected stack layers
    for stack in stacks:
        stack_dir = hub_root / "stacks" / stack
        if stack_dir.exists():
            print(f"Copying {stack} stack...")
            copied = copy_layer(stack_dir, target_dir)
            print(f"  Copied {len(copied)} files")

    # Generate sync config
    sync_config = generate_sync_config(hub_repo, stacks)
    sync_path = target_dir / ".claude" / "sync-config.yml"
    sync_path.parent.mkdir(parents=True, exist_ok=True)
    sync_path.write_text(sync_config)
    print(f"Generated {sync_path}")

    # Copy and render CLAUDE.md template
    template_path = hub_root / "core" / "CLAUDE.md.template"
    if template_path.exists():
        template = template_path.read_text()
        rendered = render_template(template, {
            "PROJECT_NAME": target_dir.name,
            "PROJECT_DESCRIPTION": "A new project",
            "PLATFORM": ", ".join(stacks),
            "BUILD_TOOLS": "See stack documentation",
            "DEVELOPMENT_COMMANDS": "# Add your commands here",
            "HUB_REPO": hub_repo,
            "SELECTED_STACKS": ", ".join(stacks),
            "LAST_SYNC_TIMESTAMP": datetime.utcnow().isoformat(),
        })
        claude_md = target_dir / "CLAUDE.md"
        if not claude_md.exists():
            claude_md.write_text(rendered)
            print(f"Generated {claude_md}")

    print(f"\nBootstrap complete! Stacks: {', '.join(stacks)}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Bootstrap a project from the hub")
    parser.add_argument("--stacks", required=True, help="Comma-separated stack names")
    parser.add_argument("--target", default=".", help="Target directory")
    parser.add_argument("--hub", default=None, help="Hub repo root (default: this repo)")
    parser.add_argument("--hub-repo", default="abhayla/claude-best-practices", help="Hub repo name")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    hub_root = Path(args.hub) if args.hub else Path(__file__).parent.parent
    target = Path(args.target)
    stacks = [s.strip() for s in args.stacks.split(",")]

    bootstrap(hub_root, target, stacks, args.hub_repo, args.dry_run)
```

**Step 4: Run tests**

```bash
PYTHONPATH=. pytest scripts/tests/test_bootstrap.py -v
```

Expected: ALL PASS

**Step 5: Commit**

```bash
git add scripts/bootstrap.py scripts/tests/test_bootstrap.py
git commit -m "feat: add bootstrap.py for project setup from hub"
```

---

## Task 9: Implement scan_web.py

**Files:**
- Create: `scripts/scan_web.py`
- Create: `scripts/tests/test_scan_web.py`

**Step 1: Write failing tests**

```python
# scripts/tests/test_scan_web.py
"""Tests for internet scanning and pattern extraction."""

from pathlib import Path
from unittest.mock import patch, MagicMock
from datetime import datetime, timedelta

import pytest

from scripts.scan_web import (
    fetch_url,
    extract_code_blocks,
    is_source_expired,
    filter_by_trust_level,
)


class TestFetchUrl:
    @patch("scripts.scan_web.requests.get")
    def test_successful_fetch(self, mock_get):
        mock_get.return_value = MagicMock(status_code=200, text="<html>content</html>")
        result = fetch_url("https://example.com")
        assert result == "<html>content</html>"

    @patch("scripts.scan_web.requests.get")
    def test_404_returns_none(self, mock_get):
        mock_get.return_value = MagicMock(status_code=404)
        result = fetch_url("https://example.com/missing")
        assert result is None

    @patch("scripts.scan_web.requests.get")
    def test_timeout_returns_none(self, mock_get):
        mock_get.side_effect = Exception("Timeout")
        result = fetch_url("https://example.com/slow")
        assert result is None


class TestExtractCodeBlocks:
    def test_extracts_from_pre_code(self, sample_webpage):
        blocks = extract_code_blocks(sample_webpage)
        assert len(blocks) >= 2
        assert any("auto-retry" in b.lower() or "auto_retry" in b.lower() or "retry" in b.lower() for b in blocks)

    def test_empty_html_returns_empty(self):
        blocks = extract_code_blocks("<html><body>No code here</body></html>")
        assert len(blocks) == 0


class TestIsSourceExpired:
    def test_not_expired(self):
        source = {"last_verified": datetime.now().isoformat()[:10], "expires_after": "90d"}
        assert is_source_expired(source) is False

    def test_expired(self):
        old_date = (datetime.now() - timedelta(days=100)).isoformat()[:10]
        source = {"last_verified": old_date, "expires_after": "90d"}
        assert is_source_expired(source) is True


class TestFilterByTrustLevel:
    def test_filters_low_trust(self):
        sources = [
            {"url": "a.com", "trust_level": "high"},
            {"url": "b.com", "trust_level": "low"},
            {"url": "c.com", "trust_level": "medium"},
        ]
        filtered = filter_by_trust_level(sources, min_level="medium")
        assert len(filtered) == 2
        assert all(s["trust_level"] in ("high", "medium") for s in filtered)
```

**Step 2: Run tests — expect FAIL**

```bash
PYTHONPATH=. pytest scripts/tests/test_scan_web.py -v
```

**Step 3: Implement scan_web.py**

```python
# scripts/scan_web.py
"""Scan internet sources for Claude Code best practices."""

import json
import re
import sys
from datetime import datetime, timedelta
from pathlib import Path
from typing import Optional

import requests
import yaml
from bs4 import BeautifulSoup


TRUST_LEVELS = {"high": 3, "medium": 2, "low": 1}


def fetch_url(url: str, timeout: int = 30) -> Optional[str]:
    """Fetch a URL and return its content. Returns None on failure."""
    try:
        resp = requests.get(url, timeout=timeout, headers={
            "User-Agent": "ClaudeBestPracticesHub/1.0"
        })
        if resp.status_code != 200:
            return None
        return resp.text
    except Exception:
        return None


def extract_code_blocks(html: str) -> list[str]:
    """Extract code blocks from HTML content."""
    soup = BeautifulSoup(html, "html.parser")
    blocks = []
    for pre in soup.find_all("pre"):
        code = pre.find("code")
        text = code.get_text() if code else pre.get_text()
        if text.strip():
            blocks.append(text.strip())
    return blocks


def is_source_expired(source: dict) -> bool:
    """Check if an internet source has expired."""
    last_verified = source.get("last_verified", "2000-01-01")
    expires_after = source.get("expires_after", "90d")

    # Parse expires_after (e.g., "90d" → 90 days)
    match = re.match(r"(\d+)d", expires_after)
    if not match:
        return False
    days = int(match.group(1))

    try:
        verified_date = datetime.fromisoformat(last_verified)
    except ValueError:
        verified_date = datetime.strptime(last_verified, "%Y-%m-%d")

    return datetime.now() - verified_date > timedelta(days=days)


def filter_by_trust_level(sources: list[dict], min_level: str = "medium") -> list[dict]:
    """Filter sources by minimum trust level."""
    min_score = TRUST_LEVELS.get(min_level, 2)
    return [s for s in sources if TRUST_LEVELS.get(s.get("trust_level", "low"), 1) >= min_score]


def extract_patterns_from_content(content: str, source_url: str) -> list[dict]:
    """Extract potential patterns from page content.

    In production, this calls Claude API for intelligent extraction.
    For now, it uses heuristic extraction of code blocks with skill/hook markers.
    """
    blocks = extract_code_blocks(content)
    patterns = []

    for block in blocks:
        # Look for SKILL.md frontmatter
        if "---" in block and ("name:" in block or "description:" in block):
            fm_match = re.search(r"---\s*\n(.*?)\n---", block, re.DOTALL)
            if fm_match:
                try:
                    fm = yaml.safe_load(fm_match.group(1))
                    if fm and "name" in fm:
                        patterns.append({
                            "name": fm["name"],
                            "type": "skill" if "allowed-tools" in fm else "agent",
                            "content": block,
                            "source_url": source_url,
                            "frontmatter": fm,
                        })
                except yaml.YAMLError:
                    pass

        # Look for bash hooks
        if block.startswith("#!/bin/bash") or block.startswith("#!/usr/bin/env bash"):
            patterns.append({
                "name": "extracted-hook",
                "type": "hook",
                "content": block,
                "source_url": source_url,
                "frontmatter": {},
            })

    return patterns


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Scan internet for best practices")
    parser.add_argument("--url", help="Specific URL to scan")
    parser.add_argument("--topic", help="Topic to search")
    parser.add_argument("--all", action="store_true", help="Scan all from config")
    args = parser.parse_args()

    root = Path(__file__).parent.parent

    if args.url:
        print(f"Scanning URL: {args.url}")
        content = fetch_url(args.url)
        if content:
            patterns = extract_patterns_from_content(content, args.url)
            print(f"Found {len(patterns)} potential patterns")
            for p in patterns:
                print(f"  - {p['name']} ({p['type']}) from {p['source_url']}")
        else:
            print("Failed to fetch URL")
    elif args.all:
        urls_path = root / "config" / "urls.yml"
        if urls_path.exists():
            with open(urls_path) as f:
                config = yaml.safe_load(f)
            sources = config.get("urls", [])
            active = [s for s in sources if not is_source_expired(s)]
            active = filter_by_trust_level(active)
            print(f"Scanning {len(active)} active sources...")
            for source in active:
                print(f"  Fetching {source['url']}...")
                content = fetch_url(source["url"])
                if content:
                    patterns = extract_patterns_from_content(content, source["url"])
                    print(f"    Found {len(patterns)} patterns")
    else:
        print("Usage: scan_web.py --url URL | --topic TOPIC | --all")
```

**Step 4: Run tests**

```bash
PYTHONPATH=. pytest scripts/tests/test_scan_web.py -v
```

Expected: ALL PASS

**Step 5: Commit**

```bash
git add scripts/scan_web.py scripts/tests/test_scan_web.py
git commit -m "feat: add scan_web.py for internet best practices scanning"
```

---

## Task 10: Implement generate_docs.py

**Files:**
- Create: `scripts/generate_docs.py`
- Create: `scripts/tests/test_generate_docs.py`

**Step 1: Write failing tests**

```python
# scripts/tests/test_generate_docs.py
"""Tests for documentation generation."""

import json
from pathlib import Path

import pytest

from scripts.generate_docs import (
    count_patterns,
    generate_dashboard_md,
    generate_stack_catalog,
    generate_getting_started,
)


class TestCountPatterns:
    def test_counts_correctly(self, sample_registry):
        counts = count_patterns(sample_registry)
        assert counts["total"] == 2
        assert counts["core"] == 2
        assert counts["by_type"]["skill"] == 1
        assert counts["by_type"]["hook"] == 1


class TestGenerateDashboardMd:
    def test_has_required_sections(self, sample_registry):
        md = generate_dashboard_md(sample_registry, [], {})
        assert "# Claude Best Practices Hub" in md
        assert "At a Glance" in md
        assert "Pattern Inventory" in md

    def test_includes_pattern_count(self, sample_registry):
        md = generate_dashboard_md(sample_registry, [], {})
        assert "2" in md  # total patterns


class TestGenerateStackCatalog:
    def test_lists_stacks(self, tmp_path):
        stack_dir = tmp_path / "stacks" / "android-compose"
        stack_dir.mkdir(parents=True)
        (stack_dir / "stack-config.yml").write_text(
            "name: android-compose\ndescription: Android stack\n"
        )
        catalog = generate_stack_catalog(tmp_path / "stacks")
        assert "android-compose" in catalog


class TestGenerateGettingStarted:
    def test_has_quick_start(self):
        doc = generate_getting_started("abhayla/claude-best-practices", ["android-compose"])
        assert "Quick Start" in doc
        assert "android-compose" in doc
```

**Step 2: Run tests — expect FAIL**

```bash
PYTHONPATH=. pytest scripts/tests/test_generate_docs.py -v
```

**Step 3: Implement generate_docs.py**

```python
# scripts/generate_docs.py
"""Generate auto-updated documentation for the best practices hub."""

import json
import sys
from datetime import datetime
from pathlib import Path

import yaml


def count_patterns(registry: dict) -> dict:
    """Count patterns by category and type."""
    counts = {"total": 0, "core": 0, "stack_specific": 0, "by_type": {}}
    for name, entry in registry.items():
        if name.startswith("_") or not isinstance(entry, dict):
            continue
        counts["total"] += 1
        if entry.get("category") == "core":
            counts["core"] += 1
        else:
            counts["stack_specific"] += 1
        ptype = entry.get("type", "unknown")
        counts["by_type"][ptype] = counts["by_type"].get(ptype, 0) + 1
    return counts


def generate_dashboard_md(registry: dict, scan_history: list, sync_status: dict) -> str:
    """Generate DASHBOARD.md content."""
    counts = count_patterns(registry)
    now = datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC")

    lines = [
        "# Claude Best Practices Hub — Dashboard",
        f"> Last updated: {now} (auto-generated)",
        "",
        "## At a Glance",
        "| Metric | Value |",
        "|--------|-------|",
        f"| Total Patterns | {counts['total']} |",
        f"| Core (universal) | {counts['core']} |",
        f"| Stack-specific | {counts['stack_specific']} |",
    ]

    for ptype, count in sorted(counts["by_type"].items()):
        lines.append(f"| {ptype.title()}s | {count} |")

    lines.extend(["", "## Pattern Inventory", ""])

    # Group by category
    core_patterns = {k: v for k, v in registry.items()
                     if not k.startswith("_") and isinstance(v, dict) and v.get("category") == "core"}

    if core_patterns:
        lines.extend(["### Core Patterns", "", "| Name | Type | Version | Source | Dependencies |",
                      "|------|------|---------|--------|--------------|"])
        for name, entry in sorted(core_patterns.items()):
            deps = ", ".join(entry.get("dependencies", [])) or "—"
            lines.append(
                f"| {name} | {entry.get('type', '?')} | {entry.get('version', '?')} | "
                f"{entry.get('source', '?')} | {deps} |"
            )

    lines.extend([
        "",
        "## How to Use",
        "- **Bootstrap new project:** `python scripts/bootstrap.py --stacks android-compose,fastapi-python`",
        "- **Update local practices:** Run `/update-practices` in any Claude Code session",
        "- **Contribute a pattern:** Run `/contribute-practice .claude/skills/my-skill/`",
        "- **Scan a URL:** `gh workflow run scan-internet.yml -f url=\"https://...\"`",
        "- **Scan a repo:** `gh workflow run scan-projects.yml -f repo=\"owner/repo\"`",
        "",
    ])

    return "\n".join(lines)


def generate_stack_catalog(stacks_dir: Path) -> str:
    """Generate STACK-CATALOG.md content."""
    lines = ["# Stack Catalog", "", "Available stacks and their contents.", ""]

    if not stacks_dir.exists():
        return "\n".join(lines + ["_No stacks found._"])

    for stack_dir in sorted(stacks_dir.iterdir()):
        if not stack_dir.is_dir():
            continue
        config_file = stack_dir / "stack-config.yml"
        if config_file.exists():
            with open(config_file) as f:
                config = yaml.safe_load(f)
            name = config.get("name", stack_dir.name)
            desc = config.get("description", "")
            lines.extend([f"## {name}", f"_{desc}_", ""])

            # List contents
            claude_dir = stack_dir / ".claude"
            if claude_dir.exists():
                for subdir in ["skills", "agents", "hooks", "rules"]:
                    sub = claude_dir / subdir
                    if sub.exists():
                        items = [f.name for f in sub.iterdir() if f.name != ".gitkeep"]
                        if items:
                            lines.append(f"**{subdir.title()}:** {', '.join(items)}")
                lines.append("")

    return "\n".join(lines)


def generate_getting_started(hub_repo: str, available_stacks: list[str]) -> str:
    """Generate GETTING-STARTED.md content."""
    stacks_str = ",".join(available_stacks[:2]) if available_stacks else "core"
    lines = [
        "# Getting Started",
        "",
        "## Quick Start",
        "",
        "### Option A: GitHub Template",
        f'1. Click **"Use this template"** on [{hub_repo}](https://github.com/{hub_repo})',
        "2. Clone your new repo",
        "3. Run bootstrap:",
        "```bash",
        f"python scripts/bootstrap.py --stacks {stacks_str}",
        "```",
        "",
        "### Option B: Bootstrap Existing Project",
        "```bash",
        f"curl -sL https://raw.githubusercontent.com/{hub_repo}/main/bootstrap.sh | bash -s -- --stacks {stacks_str}",
        "```",
        "",
        "## Available Stacks",
        "",
        "| Stack | Description |",
        "|-------|-------------|",
    ]

    for stack in available_stacks:
        lines.append(f"| `{stack}` | See stack-config.yml for details |")

    lines.extend([
        "",
        "## Skills Included",
        "",
        "| Skill | Purpose |",
        "|-------|---------|",
        "| `/update-practices` | Pull latest from hub |",
        "| `/contribute-practice` | Push pattern to hub |",
        "| `/scan-url` | Trigger internet scan |",
        "| `/scan-repo` | Trigger project scan |",
        "",
    ])

    return "\n".join(lines)


if __name__ == "__main__":
    root = Path(__file__).parent.parent
    docs_dir = root / "docs"
    docs_dir.mkdir(exist_ok=True)

    # Load registry
    registry_path = root / "registry" / "patterns.json"
    with open(registry_path) as f:
        registry = json.load(f)

    # Generate dashboard
    dashboard = generate_dashboard_md(registry, [], {})
    (docs_dir / "DASHBOARD.md").write_text(dashboard)
    print("Generated docs/DASHBOARD.md")

    # Generate stack catalog
    catalog = generate_stack_catalog(root / "stacks")
    (docs_dir / "STACK-CATALOG.md").write_text(catalog)
    print("Generated docs/STACK-CATALOG.md")

    # Get available stacks
    stacks = []
    stacks_dir = root / "stacks"
    if stacks_dir.exists():
        for d in stacks_dir.iterdir():
            if d.is_dir() and (d / "stack-config.yml").exists():
                stacks.append(d.name)

    # Generate getting started
    getting_started = generate_getting_started("abhayla/claude-best-practices", sorted(stacks))
    (docs_dir / "GETTING-STARTED.md").write_text(getting_started)
    print("Generated docs/GETTING-STARTED.md")

    if "--validate" in sys.argv:
        print("Validation passed — all docs generated without errors")
```

**Step 4: Run tests**

```bash
PYTHONPATH=. pytest scripts/tests/test_generate_docs.py -v
```

Expected: ALL PASS

**Step 5: Commit**

```bash
git add scripts/generate_docs.py scripts/tests/test_generate_docs.py
git commit -m "feat: add generate_docs.py for auto-updated dashboard and catalog"
```

---

## Task 11-16: Remaining Scripts + Workflows

Due to plan length, these tasks follow the same TDD pattern:

### Task 11: `sync_to_local.py` + tests
- Version comparison logic
- File copy with conflict detection
- Dry-run mode
- Atomic git commit generation

### Task 12: `sync_to_projects.py` + tests
- Read `config/repos.yml`
- For each repo, check `.claude/sync-config.yml` via GitHub API
- Diff hub patterns vs project patterns
- Create PR via `gh` CLI

### Task 13: `check_freshness.py` + tests
- Read `config/urls.yml`
- Check `last_verified` + `expires_after`
- Generate expired sources report

### Task 14: GitHub Actions — `scan-projects.yml` + `scan-internet.yml`
- Weekly cron + `workflow_dispatch` with inputs
- Python setup, install deps, run scripts
- Create PR if changes found

### Task 15: GitHub Actions — `sync-to-projects.yml` + `update-docs.yml` + `validate-pr.yml`
- Push-triggered workflows
- Doc regeneration
- PR quality gate (integrity, deps, secrets)

### Task 16: GitHub Actions — `expire-sources.yml` + `test.yml`
- Weekly freshness check
- Unit + integration test runner

---

## Task 17-20: Claude Code Skills

### Task 17: `/update-practices` skill
**File:** `core/.claude/skills/update-practices/SKILL.md`
- Read `sync-config.yml` for hub repo + selected stacks
- Fetch hub `patterns.json` via `gh api`
- Compare versions, present changelog
- Copy files on approve, git commit

### Task 18: `/contribute-practice` skill
**File:** `core/.claude/skills/contribute-practice/SKILL.md`
- Validate pattern (frontmatter, version)
- Auto-detect category (core vs stack)
- Dedup check via `gh api`
- Create PR to hub via `gh pr create`

### Task 19: `/scan-url` skill
**File:** `core/.claude/skills/scan-url/SKILL.md`
- Detect URL vs topic input
- Trigger `gh workflow run scan-internet.yml`
- Optionally add to watchlist

### Task 20: `/scan-repo` skill
**File:** `core/.claude/skills/scan-repo/SKILL.md`
- Normalize repo input
- Trigger `gh workflow run scan-projects.yml`
- Optionally add to tracked repos

---

## Task 21-24: Dashboard + Bootstrap Shell + Template Config

### Task 21: HTML Dashboard (`docs/dashboard.html`)
- Single self-contained HTML file
- Inline CSS + vanilla JS
- Search/filter, collapsible sections, sortable tables
- SVG dependency graph
- Dark/light mode
- Generated by `generate_docs.py`

### Task 22: `bootstrap.sh` (one-liner setup)
```bash
#!/bin/bash
# Usage: curl -sL .../bootstrap.sh | bash -s -- --stacks android-compose,fastapi-python
set -e
REPO="abhayla/claude-best-practices"
STACKS="${1#--stacks }"

# Clone hub to temp dir
TMPDIR=$(mktemp -d)
git clone --depth=1 "https://github.com/$REPO.git" "$TMPDIR"

# Run bootstrap
cd "$TMPDIR"
pip install -q pyyaml
python scripts/bootstrap.py --stacks "$STACKS" --target "$(pwd -P)" --hub "$TMPDIR"

# Cleanup
rm -rf "$TMPDIR"
echo "Done! Run 'git add .claude/ && git commit -m \"chore: bootstrap best practices\"'"
```

### Task 23: Enable GitHub Template
- Go to repo Settings → check "Template repository"
- Add topic tags: `claude-code`, `best-practices`, `template`

### Task 24: Create `LICENSE` (MIT)

---

## Task 25-27: Seed from KKB

### Task 25: Copy universal patterns from KKB to core/
**Source:** `C:/Abhay/VibeCoding/KKB/.claude/`
**Core skills** (universal): `fix-loop`, `implement`, `auto-verify`, `fix-issue`, `status`, `clean-pyc`, `reflect`, `skill-factory`, `sync-check`, `plan-to-issues`, `post-fix-pipeline`, `claude-guardian`, `test-knowledge`, `strategic-architect`, `ui-ux-pro-max`, `verify-screenshots`
**Core agents** (universal): `code-reviewer`, `debugger`, `test-failure-analyzer`, `context-reducer`, `docs-manager`, `git-manager`, `plan-executor`, `planner-researcher`, `session-summarizer`, `tester`
**Core hooks** (universal): `hook-utils.sh`, `auto-format.sh`, `validate-workflow-step.sh`, `post-test-update.sh`, `log-workflow.sh`, `verify-evidence-artifacts.sh`, `resize_screenshot.py`
**Core rules** (universal): `workflow.md`, `testing.md`

### Task 26: Copy stack-specific patterns from KKB
**android-compose**: skills (`run-android-tests`, `adb-test`, `run-e2e`), agents (`android-compose`), rules (`android.md`, `compose-ui.md`)
**fastapi-python**: skills (`run-backend-tests`, `db-migrate`, `deploy`), agents (`database-admin`, `api-tester`), rules (`backend.md`, `database.md`)
**ai-gemini**: skills (`gemini-api`, `generate-meal`), agents (`meal-generation`)
**firebase-auth**: (currently part of android rules — extract relevant sections)

### Task 27: Populate registry and generate initial docs
- Run `collate.py` against the seeded patterns
- Build `registry/patterns.json` with all entries
- Run `generate_docs.py` to create initial dashboard + catalog
- Tag as `v1.0`

---

## Task 28-29: End-to-End Validation

### Task 28: Test bootstrap flow
```bash
# Create temp project
mkdir /tmp/test-project && cd /tmp/test-project
git init

# Bootstrap from hub
python /path/to/claude-best-practices/scripts/bootstrap.py \
  --stacks android-compose,fastapi-python \
  --hub /path/to/claude-best-practices

# Verify structure
ls .claude/skills/    # Should have core + android + fastapi skills
ls .claude/agents/    # Should have core + android agents
ls .claude/hooks/     # Should have core hooks
ls .claude/rules/     # Should have core + android + fastapi rules
cat .claude/sync-config.yml  # Should have hub_repo, selected_stacks
```

### Task 29: Run full test suite and tag release
```bash
cd claude-best-practices
PYTHONPATH=. pytest scripts/tests/ -v
git tag v1.0
git push origin main --tags
```

---

## Dependency Graph

```
Task 1 (skeleton) ──> Task 2 (config) ──> Task 3 (registry + stacks)
                                               │
Task 4 (templates) ─────────────────────────> Task 5 (fixtures)
                                               │
                                    ┌──────────┼──────────┐
                                    ▼          ▼          ▼
                              Task 6       Task 7     Task 8
                              (dedup)    (collate)  (bootstrap)
                                    │          │          │
                                    ▼          │          │
                              Task 9 ◄─────────┘          │
                              (scan_web)                   │
                                    │                      │
                                    ▼                      │
                              Task 10 ◄────────────────────┘
                              (generate_docs)
                                    │
                         ┌──────────┼──────────┐
                         ▼          ▼          ▼
                   Task 11     Task 12     Task 13
                 (sync_local) (sync_proj) (freshness)
                         │          │          │
                         ▼          ▼          ▼
                   Tasks 14-16 (GitHub Actions)
                         │
                         ▼
                   Tasks 17-20 (Skills)
                         │
                         ▼
                   Tasks 21-24 (Dashboard + Bootstrap.sh + Template)
                         │
                         ▼
                   Tasks 25-27 (Seed from KKB)
                         │
                         ▼
                   Tasks 28-29 (E2E Validation + Release)
```

---

## Estimated Effort

| Phase | Tasks | Estimate |
|-------|-------|----------|
| Phase 1: Skeleton + Config | 1-4 | Small |
| Phase 2: Seed Patterns | 5-8 | Medium |
| Phase 3: Core Scripts | 9-12 | Medium |
| Phase 4: Remaining Scripts | 13-16 | Medium |
| Phase 5: GitHub Actions | 17-20 | Small |
| Phase 6: Skills | 21-24 | Small |
| Phase 7: Dashboard + Polish | 25-27 | Medium |
| Phase 8: Seed + Validate | 28-29 | Small |
