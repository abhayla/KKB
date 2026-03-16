---
name: seed-reference-data
description: >
  Seed or refresh reference data in the backend PostgreSQL database: festivals, achievements,
  recipes, and YAML config. Required after fresh DB setup or when reference data changes.
  Use for first-time setup, data refresh, or CI environment provisioning.
type: workflow
allowed-tools: "Bash Read Grep Glob"
argument-hint: "[all|festivals|achievements|recipes|config] [--dry-run]"
version: "1.0.0"
synthesized: true
---

# Seed Reference Data

Seed or refresh reference data in PostgreSQL. Required after fresh database setup or when reference data sources change.

**Request:** $ARGUMENTS

---

## STEP 1: Parse Arguments

| Argument | What it seeds | Script |
|----------|--------------|--------|
| `all` (default) | Everything below in order | All scripts sequentially |
| `festivals` | Indian festival calendar (2025) | `scripts/seed_festivals.py` |
| `achievements` | Achievement definitions (streaks, milestones) | `scripts/seed_achievements.py` |
| `recipes` | 3,580 recipes from KKB dataset | `scripts/import_recipes_postgres.py` |
| `config` | Meal generation YAML config → PostgreSQL | `scripts/sync_config_postgres.py` |

`--dry-run`: Preview what would be seeded without writing to DB.

## STEP 2: Verify Prerequisites

```bash
cd backend
source venv/bin/activate 2>/dev/null || source venv/Scripts/activate 2>/dev/null

# Check PostgreSQL is reachable
PYTHONPATH=. python -c "
from app.config import settings
print(f'Database: {settings.database_url[:30]}...')
print('Connection: OK')
" 2>&1

# Check migrations are current
alembic current 2>&1 | tail -1
```

If migrations are not current, run `alembic upgrade head` first.

## STEP 3: Seed Festivals

```bash
cd backend && PYTHONPATH=. python scripts/seed_festivals.py
```

Seeds Indian festival calendar with: name, Hindi name, date, regions, fasting days, special foods. Used by meal generation to suggest festival-appropriate meals.

## STEP 4: Seed Achievements

```bash
cd backend && PYTHONPATH=. python scripts/seed_achievements.py
```

Seeds achievement definitions: streak achievements, milestone achievements, collection achievements. Used by the stats/gamification system.

## STEP 5: Seed Recipes

```bash
# Dry run first to verify
cd backend && PYTHONPATH=. python scripts/import_recipes_postgres.py --dry-run

# Import all (3,580 recipes)
cd backend && PYTHONPATH=. python scripts/import_recipes_postgres.py --all

# Or import only missing (incremental)
cd backend && PYTHONPATH=. python scripts/import_recipes_postgres.py --missing-only
```

Imports recipes from the KKB Firebase dataset. Each recipe includes: name, ingredients, instructions, nutrition, dietary tags, cuisine type, prep time.

## STEP 6: Sync Config

```bash
cd backend && PYTHONPATH=. python scripts/sync_config_postgres.py
```

Syncs YAML config files from `backend/config/` to PostgreSQL `system_config` and `reference_data` tables. This bridges version-controlled config with runtime config.

## STEP 7: Verify

```bash
cd backend && PYTHONPATH=. python -c "
import asyncio
from app.db.postgres import async_session_maker
from sqlalchemy import text

async def check():
    async with async_session_maker() as session:
        for table in ['festivals', 'achievements', 'recipes', 'system_config']:
            result = await session.execute(text(f'SELECT COUNT(*) FROM {table}'))
            count = result.scalar()
            print(f'{table}: {count} rows')

asyncio.run(check())
"
```

Expected counts: festivals ~30+, achievements ~20+, recipes ~3,580, system_config ~10+.

---

## CRITICAL RULES

- MUST run seeds in order: festivals → achievements → recipes → config. Some seeds reference others.
- MUST verify migrations are current before seeding — seeds fail silently against stale schemas
- MUST use `--missing-only` for recipe imports in production — `--all` truncates and reimports
- Seeds are idempotent — running twice won't create duplicates (they use upsert logic)
