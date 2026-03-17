---
description: Decision criteria for Firestore vs PostgreSQL data storage, sync responsibilities, and query patterns.
globs: ["backend/app/db/**", "backend/app/repositories/**", "backend/app/services/**"]
---

# Firestore vs PostgreSQL Usage

## Database Roles

RasoiAI uses two databases with distinct responsibilities:

| Database | Role | Access Pattern |
|----------|------|----------------|
| **PostgreSQL** | Source of truth for transactional, structured data | SQLAlchemy async ORM via repositories |
| **Firestore** | Real-time, unstructured, and ephemeral data | `firebase_admin` SDK via helper functions |

## What Goes Where

### PostgreSQL (Structured, Transactional)

| Data Type | Table/Model | Why PostgreSQL |
|-----------|-------------|----------------|
| User accounts | `users` | Auth tokens, profile, needs JOINs |
| Meal plans | `meal_plans`, `meal_plan_items` | Parent-child relations, complex queries |
| Recipes | `recipes` | Full-text search, indexed lookups |
| Grocery lists | `grocery_items` | Aggregate queries, user-scoped |
| Refresh tokens | `refresh_tokens` | SHA256 hashed, needs reuse detection |
| Usage logs | `usage_logs` | Rate limiting queries, daily counts |
| Statistics | `stats` | Aggregations, time-series queries |
| Festivals | `festivals` | Calendar lookups, date range queries |
| Recipe rules | `recipe_rules` | Constraint checking, validated structures |
| AI recipe catalog | `ai_recipe_catalog` | Searchable, indexed, large dataset |
| Notifications | `notifications` | Persistent, queryable by user |
| Config | `config` | Synced from YAML, structured key-value |

### Firestore (Real-Time, Flexible)

| Data Type | Collection | Why Firestore |
|-----------|------------|---------------|
| Chat messages | `chat_messages` | Time-series append-only, real-time sync |
| User preferences (subcollection) | `users/{uid}/preferences` | Flexible schema, subcollection hierarchy |
| Family members (subcollection) | `users/{uid}/family_members` | Per-user scope, denormalized |
| Cooking streaks | `cooking_streaks` | Activity tracking, denormalized counters |
| Achievements | `achievements` | Badge system, read-heavy |
| System config | `system_config` | Reference data, infrequent changes |
| Reference data | `reference_data` | Shared config synced from YAML |

## Decision Criteria for New Data

When adding a new data type, use this decision tree:

1. **Does it need JOINs or complex queries?** → PostgreSQL
2. **Does it need transactional consistency?** → PostgreSQL
3. **Does it need full-text search?** → PostgreSQL
4. **Is it time-series / append-only?** → Firestore
5. **Does it need real-time sync to mobile?** → Firestore
6. **Is it user-scoped with flexible schema?** → Firestore
7. **Is it a large, searchable dataset?** → PostgreSQL

When in doubt, default to PostgreSQL — it's the primary database.

## Collection Names

Firestore collection names are constants in `backend/app/db/firestore.py`:

```python
class Collections:
    USERS = "users"
    CHAT_MESSAGES = "chat_messages"
    # ... never use inline string literals
```

NEVER use bare string literals for collection names — always reference `Collections.NAME`.

## Repository Separation

- PostgreSQL repositories live in `backend/app/repositories/` and use `AsyncSession`
- Firestore access uses helper functions in `backend/app/db/firestore.py`
- A single service MAY read from both databases, but writes should target one

## Sync Rules

- There is NO automatic sync between Firestore and PostgreSQL
- Each data type lives exclusively in one database — no dual-writes
- If data needs to exist in both (e.g., user profile), PostgreSQL is authoritative
- Migration from Firestore to PostgreSQL should be done via scripts, not runtime sync

## Anti-Patterns

- NEVER store auth tokens or refresh tokens in Firestore — use PostgreSQL with hashing
- NEVER use Firestore for data that needs JOINs — flatten or move to PostgreSQL
- NEVER use inline collection name strings — use `Collections` constants
- NEVER write to both databases in the same operation without explicit documentation of why
