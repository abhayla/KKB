---
description: >
  RecipeRule.enforcement is REQUIRED or PREFERRED. REQUIRED rules are hard
  constraints in meal-plan generation; PREFERRED are soft. force_override
  allows an authored rule to override a household safety rule; meal
  generation MUST surface the enforcement level in the Gemini prompt.
globs: ["backend/app/models/recipe_rule.py", "backend/app/services/ai_meal_service.py", "backend/app/api/v1/endpoints/recipe_rules.py", "backend/alembic/versions/*.py"]
synthesized: true
private: false
version: "1.0.0"
---

# Recipe Rule Enforcement — REQUIRED vs PREFERRED

Every `RecipeRule` carries an `enforcement` value of `"REQUIRED"` or
`"PREFERRED"`. This is the knob that determines how much the meal-plan
generator bends to the user's preference. Getting this wrong produces meal
plans that either ignore dietary restrictions (dangerous) or reject every
recipe (useless).

## Model contract

```python
class RecipeRule(Base):
    # ...
    enforcement: Mapped[str] = mapped_column(
        String(10), default="REQUIRED", nullable=False
    )  # REQUIRED | PREFERRED
    force_override: Mapped[bool] = mapped_column(
        Boolean, default=False, nullable=False
    )
```

- `enforcement` defaults to `"REQUIRED"` — the safe default. A new rule with
  no explicit value is treated as a hard constraint. MUST NOT change the
  default without careful thought; users trust the app to honor constraints.
- `force_override` defaults to `False`. When `True`, this personal rule
  takes precedence over a conflicting HOUSEHOLD rule (see
  `household-scoped-recipe-rules.md` for scoping).

## Meaning

| enforcement | Effect in meal plan generation |
|-------------|--------------------------------|
| `REQUIRED` | Rule is a hard constraint. Any recipe violating it is excluded. If exclusion empties the candidate pool, generation fails with `InsufficientRecipeCandidates` rather than relaxing the rule. |
| `PREFERRED` | Rule is a soft constraint. Candidates that satisfy it are scored higher; candidates that violate it are allowed when better options don't exist. |

## Gemini prompt integration

`ai_meal_service.build_prompt()` MUST split rules into two blocks:

```
HARD CONSTRAINTS (MUST be followed — do not violate):
- Exclude: onion, garlic (from household rule)
- Must include: paneer (from user rule, REQUIRED)

SOFT PREFERENCES (follow when possible):
- Prefer: low-spice (from user rule, PREFERRED)
- Prefer: under 45 minutes (from user rule, PREFERRED)
```

MUST NOT collapse both blocks into one section — Gemini treats the
language literally. Mixing HARD and SOFT in one list produces plans that
silently violate REQUIRED rules because the model treated "prefer" as
advisory across the whole block.

## force_override precedence

When a user authors a PERSONAL rule that conflicts with a HOUSEHOLD rule
(e.g. household forbids onion, user authors "include caramelized onion
weekly"):

1. Default behavior: **household rule wins**. User's rule is silently
   suppressed from generation. UI shows a "conflict with household rule"
   badge on the personal rule.
2. If user sets `force_override=True` on the personal rule, the personal
   rule wins. The household rule is suppressed for this user's generation
   only; other household members still see the household rule enforced.
3. `force_override=True` MUST NOT be settable on PERSONAL rules that
   conflict with household SAFETY rules (allergies, religious
   restrictions). The service layer classifies certain rule types as
   "safety" and rejects the override with `HTTPException(status_code=400,
   detail="Cannot override household safety rule: <rule_type>")`.

## Migration history

`force_override` was added in
`h8i9j0k1l2m3_add_force_override_to_recipe_rules.py` with
`server_default='false'` so existing rows got a safe default. The
`enforcement` column has existed since initial schema.

## Endpoint validation

Rule-creation endpoints MUST validate:

- `enforcement in ("REQUIRED", "PREFERRED")` — any other string is 422.
- `force_override == True` requires `enforcement == "PREFERRED"`. A
  REQUIRED rule overriding a household is contradictory
  (you're forcing your opinion onto the family while also declaring it
  a hard constraint for yourself — collapse these into one semantic).
- Rejecting `force_override=True` on safety rule types (allergy, religious,
  medical). List of restricted types lives in
  `app/services/recipe_rules_service.py::SAFETY_RULE_TYPES`.

## Test coverage

Tests MUST cover:

- A PREFERRED rule is violated gracefully when better options don't exist.
- A REQUIRED rule blocks generation rather than compromising.
- A `force_override=True` personal rule wins over a conflicting PREFERRED
  household rule.
- A `force_override=True` attempt on a safety-type household rule is
  rejected with 400.

## Critical constraints

- MUST NOT add a third enforcement value ("RECOMMENDED", "OPTIONAL") without
  a migration and a prompt-template update. The binary is intentional;
  Gemini's state machine does not gracefully handle a three-way spectrum.
- MUST NOT treat `enforcement == "PREFERRED"` as "ignore it". It is scored
  into the meal-plan prompt; silent removal of PREFERRED rules produces
  plans that feel wrong to the user even though no hard rule was broken.
- `force_override` is per-rule. MUST NOT add a user-level "override all
  household rules" flag. That's how cross-tenant leakage gets designed in.
- The Android `RuleEditor` UI MUST surface both `enforcement` and
  `force_override` explicitly. Hiding `force_override` behind an
  "advanced" toggle causes accidental household overrides.
