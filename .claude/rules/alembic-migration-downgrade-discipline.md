---
description: >
  Every Alembic migration under backend/alembic/versions/ MUST define both
  upgrade() and downgrade(), with downgrade operations in reverse order of
  upgrade, and a module docstring describing the schema change.
globs: ["backend/alembic/versions/*.py", "backend/alembic/env.py"]
synthesized: true
private: false
version: "1.0.0"
---

# Alembic Migration — Downgrade Discipline

Every migration file in `backend/alembic/versions/` MUST ship with a working
`downgrade()`. A one-way migration is a production landmine — when a deploy
goes wrong at 2am, the operator needs `alembic downgrade -1`, not a forensics
session.

## Required structure

```python
"""add favorites table

Revision ID: 75558d6b1fbc
Revises: <prev_rev>
Create Date: YYYY-MM-DD HH:MM:SS

Changes:
- Creates favorites table with user_id + recipe_id composite uniqueness
- Adds index ix_favorites_user_id for fast lookup

Downgrade behavior:
- Drops ix_favorites_user_id then the favorites table
- No data loss on re-upgrade (table is re-created empty)
"""
```

MUST include a module docstring with the three sections above: human title,
*Changes*, *Downgrade behavior*. Reviewers use the downgrade-behavior section
to decide whether the migration is hot-deployable.

## Ordering rule

`downgrade()` MUST reverse the operations of `upgrade()` in opposite order.
Indexes are created last in upgrade, so they are dropped first in downgrade.
Tables are created first in upgrade, so they are dropped last.

```python
def upgrade() -> None:
    op.create_table("favorites", ...)
    op.create_index("ix_favorites_user_id", "favorites", ["user_id"])

def downgrade() -> None:
    op.drop_index("ix_favorites_user_id", table_name="favorites")
    op.drop_table("favorites")
```

Counter-example (forbidden — drops table with index still attached on some
backends):

```python
def downgrade() -> None:
    op.drop_table("favorites")                # WRONG — index should go first
    op.drop_index("ix_favorites_user_id")     # unreachable on some DBs
```

## Data migrations

If `upgrade()` runs a data migration (SQL `UPDATE` / bulk insert), `downgrade()`
MUST either restore the prior state or declare explicitly that rollback is
irreversible:

```python
def downgrade() -> None:
    raise NotImplementedError(
        "Downgrading this revision would lose household_id assignments "
        "made after upgrade. Restore from pre-migration backup instead."
    )
```

MUST NOT leave `pass` or an empty downgrade for data migrations — silent
no-ops cause operators to think they rolled back when they didn't.

## Column defaults

When adding a `NOT NULL` column to an existing table, the upgrade MUST provide
`server_default=` so existing rows get a value. The downgrade then drops the
column cleanly. Pattern seen in
`bb17ac1e73db_add_household_tables_and_scope_columns.py`:

```python
op.add_column(
    "recipe_rules",
    sa.Column("scope", sa.String(20), nullable=False, server_default="PERSONAL"),
)
```

MUST NOT add a `NOT NULL` column without a server_default on a non-empty
table — the upgrade crashes on production data.

## Critical constraints

- MUST NOT use `op.execute("DROP TABLE IF EXISTS foo")` as a stand-in for a
  proper downgrade. IF EXISTS hides bugs where the table was never created.
- MUST NOT rename a revision ID after it has been applied in any environment.
  Revisions are identity; renaming breaks the dependency graph.
- Every new migration MUST be created via `alembic revision --autogenerate` and
  then reviewed — autogenerate often misses indexes and check constraints.
  Manually verify both `upgrade()` and `downgrade()` before committing.
- CI (`db-migrate-verify` skill) runs `upgrade head` → `downgrade base` →
  `upgrade head` on every PR. A migration that fails any leg of this cycle
  blocks merge.
