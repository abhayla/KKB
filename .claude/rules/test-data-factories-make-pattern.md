---
description: >
  Backend test fixtures use function-based make_* factories located in
  backend/tests/factories.py. Kwarg overrides, sensible defaults, no
  external factory library (no factory_boy, no model_bakery).
globs: ["backend/tests/**/*.py"]
synthesized: true
private: false
version: "1.0.0"
---

# Test Data Factories — `make_*` Pattern

All test data construction MUST go through a `make_*` function in
`backend/tests/factories.py`. Inline dictionaries in individual tests are
forbidden — they drift from the real model shape as fields are added and
silently hide schema errors.

## Signature

Every factory MUST follow this shape:

```python
def make_user(**overrides) -> User:
    user_id = overrides.pop("id", str(uuid.uuid4()))
    defaults = {
        "id": user_id,
        "firebase_uid": f"firebase-test-{user_id[:8]}",
        "email": f"test-{user_id[:8]}@example.com",
        "name": "Test User",
        "is_onboarded": True,
    }
    defaults.update(overrides)
    return User(**defaults)
```

Requirements:

- Name MUST start with `make_` followed by the model's snake_case name.
  E.g. `User` → `make_user`, `UserPreferences` → `make_preferences`
  (short form is OK if unambiguous in the file).
- MUST accept only `**overrides` — no positional args. This keeps call sites
  self-documenting (`make_user(email="x@y")` not `make_user("x@y")`).
- MUST generate fresh identifiers per call (`uuid.uuid4()` or time-based IDs).
  MUST NOT hardcode IDs — parallel-safe tests (see `testing.md`) depend on it.
- MUST return the fully-constructed model or Pydantic object, ready to use.

## Why no factory_boy

The codebase intentionally avoids `factory_boy`, `model_bakery`, and similar
libraries. Reasons:

1. **Zero dependency weight.** Factories are 5–15 lines; the library is 20+KB
   and ships its own DSL developers have to re-learn.
2. **Explicit override scope.** `**overrides` + `defaults.update(overrides)`
   makes it obvious which fields are defaulted and which are test-specified.
3. **Plays cleanly with async.** Factory_boy's `post_generation` hooks don't
   compose well with async SQLAlchemy sessions.
4. **Composable.** A factory may call other factories
   (`make_household` calls `make_user` for the owner). That's plain Python;
   no DSL learning curve.

MUST NOT introduce a test-data library without removing `factories.py` in
the same PR — mixing styles is worse than either alone.

## Fixture wiring

Pytest fixtures in `conftest.py` MUST use factories — not inline construction:

```python
# GOOD — tests/conftest.py
@pytest.fixture
def test_user(db_session) -> User:
    user = make_user()
    db_session.add(user)
    db_session.commit()
    return user

# BAD — inline dict, will rot as User gains fields
@pytest.fixture
def test_user(db_session):
    user = User(id="123", email="x@y", name="T", firebase_uid="fb")
    db_session.add(user); db_session.commit()
    return user
```

## Override composition

Tests that need a slight variation MUST override rather than copy:

```python
# GOOD
test_user = make_user(email="admin@example.com", is_onboarded=False)

# BAD — whole-object construction that will drift
test_user = User(id="...", firebase_uid="...", email="admin@example.com", ...)
```

## Critical constraints

- MUST NOT commit factory-produced objects to the session inside the factory.
  Factories return transient objects; the test decides whether to persist.
- MUST NOT store state across factory calls (e.g. auto-incrementing counters).
  Parallel test runs will race and collide.
- When adding a new field to a model, update the matching factory in the same
  commit. Missing defaults produce `NotNullViolation` errors in unrelated
  tests.
- Factories MUST live in exactly one file (`backend/tests/factories.py`).
  MUST NOT duplicate `make_user` in `tests/api/factories.py` or elsewhere —
  import from the single source.
