---
description: >
  RecipeRule rows declare scope (PERSONAL|HOUSEHOLD) and optional household_id
  FK. Endpoints MUST filter by scope+household_id; household rules apply to
  every member via shared meal plan generation.
globs: ["backend/app/models/recipe_rule.py", "backend/app/api/v1/endpoints/households.py", "backend/app/api/v1/endpoints/recipe_rules.py", "backend/app/services/ai_meal_service.py"]
synthesized: true
private: false
version: "1.0.0"
---

# Household-Scoped Recipe Rules

Recipe rules (avoid-onion, include-protein, no-garlic-Wednesdays, etc.) are
multi-tenant. A rule may be **PERSONAL** (one user) or **HOUSEHOLD**
(all members of a family). This is a foundational domain constraint — get
it wrong and one user's "no garlic" preference silently leaks into another
family's meal plan.

## Model shape

`backend/app/models/recipe_rule.py` MUST define these columns exactly:

```python
class RecipeRule(Base):
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"))
    household_id: Mapped[Optional[str]] = mapped_column(
        String(36),
        ForeignKey("households.id", ondelete="CASCADE"),
        nullable=True,
    )
    scope: Mapped[str] = mapped_column(
        String(20), default="PERSONAL", nullable=False
    )  # PERSONAL or HOUSEHOLD
    # ... rule_type, ingredient, enforcement, force_override, etc.
```

## Invariants

- `scope == "PERSONAL"` ⇒ `household_id IS NULL` (enforced by the service
  layer; DB does NOT enforce it via check constraint to keep migrations
  simple).
- `scope == "HOUSEHOLD"` ⇒ `household_id IS NOT NULL` AND the requesting
  user MUST be a member of that household.
- `user_id` is always set — even HOUSEHOLD rules record who authored them,
  for audit and the "created_by X" UI badge.

Service layer MUST reject any rule creation that violates the above. MUST NOT
rely on the DB to catch these — validate in Python and return `400`.

## Endpoint filtering

Any list endpoint that returns rules for a user MUST union:

```python
# GOOD — returns personal rules + household rules for all households user belongs to
rules = await session.execute(
    select(RecipeRule)
    .where(
        or_(
            and_(RecipeRule.user_id == user_id, RecipeRule.scope == "PERSONAL"),
            and_(RecipeRule.scope == "HOUSEHOLD",
                 RecipeRule.household_id.in_(user_household_ids)),
        )
    )
)

# BAD — only returns personal rules, household rules invisible
rules = await session.execute(
    select(RecipeRule).where(RecipeRule.user_id == user_id)
)
```

Endpoints under `/api/v1/households/{id}/rules` MUST filter to
`scope == "HOUSEHOLD"` AND `household_id == path_id` AND the caller is a
member of that household. Non-member access MUST return 404 (not 403) to
avoid leaking household existence.

## Meal generation integration

`ai_meal_service.generate_meal_plan()` MUST fetch BOTH personal and
household rules for the user, flatten them into a single enforcement list,
and pass that to the Gemini prompt. Household rules MUST NOT be filtered
out — the feature's whole point is cross-member enforcement.

When two rules conflict (personal says "include onion" + household says
"exclude onion"), the household rule wins unless `force_override=True` is
set on the personal rule. See `recipe-rule-enforcement-required-vs-preferred.md`
for the full precedence table.

## Migration

The household/scope columns were added in
`bb17ac1e73db_add_household_tables_and_scope_columns.py`. That migration is
the reference for how to add tenant-scoping to an existing table — it uses
`server_default='PERSONAL'` so backfill is automatic.

## Critical constraints

- MUST NOT add an endpoint that lists `RecipeRule` without a scope filter.
  Unscoped reads can return cross-household rules and cause cross-tenant
  data leakage.
- MUST NOT delete a household without cascading its rules. The
  `ondelete="CASCADE"` FK on `household_id` handles this — MUST NOT change
  it to `SET NULL` (orphans scope=HOUSEHOLD rules with no household).
- When a user leaves a household, their authored HOUSEHOLD rules MUST NOT
  be auto-deleted. They remain attached to the household. Deletion flow
  lives in `HouseholdService.remove_member()`.
- All tests that construct a RecipeRule MUST use `make_recipe_rule()`
  (not inline dicts). See `test-data-factories-make-pattern.md`.
