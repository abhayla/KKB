---
paths:
  - "backend/app/models/**/*.py"
  - "backend/app/db/**/*.py"
  - "backend/app/repositories/**/*.py"
  - "backend/alembic/**/*.py"
  - "android/data/src/main/java/com/rasoiai/data/local/**/*.kt"
---

# Database Rules

## PostgreSQL (Backend)
- 5-location model update rule: `models/__init__.py`, `postgres.py` (3 blocks), `conftest.py`
- Always generate Alembic migration after model changes: `alembic revision --autogenerate -m "description"`
- `postgres.py` has connection pool: `pool_size=10`, `max_overflow=20`, `pool_recycle=1800`
- `expire_on_commit=False` and `selectinload()` are required for async
- `firestore.py` is legacy — do not use, do not delete

## Room (Android) — Version 11
- 20 entities, 11 DAOs
- Migrations in `RasoiDatabase.kt`: MIGRATION_7_8 through MIGRATION_10_11
- Room stores MealType as uppercase: `BREAKFAST`, `LUNCH`, `DINNER`, `SNACKS`
- Fresh installs seed `known_ingredients` with 40+ Indian cooking ingredients
- Room-only entities (no domain model): `KnownIngredientEntity`, `OfflineQueueEntity`, `CookedRecipeEntity`, `RecentlyViewedEntity`
