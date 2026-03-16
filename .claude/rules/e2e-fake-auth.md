---
description: >
  E2E tests use FakePhoneAuthClient with DEBUG=true backend bypass. Never use real Firebase auth
  in tests. All E2E API calls hit real PostgreSQL via the backend at 10.0.2.2:8000.
globs: ["android/app/src/androidTest/**/*.kt"]
synthesized: true
private: true
---

# E2E Fake Auth Pattern

E2E tests use a fake authentication flow that bypasses Firebase Phone Auth while keeping all other API interactions real (including PostgreSQL).

## Auth flow in E2E tests

```
Android Test                    Backend (DEBUG=true)
    |                                |
    |-- FakePhoneAuthClient          |
    |   sends "fake-firebase-token"  |
    |   ─────────────────────────>   |
    |                                |-- verify_firebase_token()
    |                                |   DEBUG=true → accepts any token
    |                                |-- Returns real JWT
    |   <─────────────────────────   |
    |                                |
    |-- Uses real JWT for all        |
    |   subsequent API calls         |
    |   ─────────────────────────>   |
    |                                |-- Real PostgreSQL queries
```

## Key components

| Component | Location | Purpose |
|-----------|----------|---------|
| `FakePhoneAuthClient` | `e2e/di/FakePhoneAuthClient.kt` | Sends `"fake-firebase-token"` instead of real Firebase OTP flow |
| `FakeAuthModule` | `e2e/di/FakeAuthModule.kt` | Hilt module that replaces real `PhoneAuthClient` with fake |
| `BaseE2ETest` | `e2e/base/BaseE2ETest.kt` | Base class with Hilt setup, auth state helpers |
| Backend DEBUG mode | `backend/app/core/firebase.py` | When `DEBUG=true`, `verify_firebase_token()` accepts any token |

## Backend URL

E2E tests connect to the backend at `http://10.0.2.2:8000` — the Android emulator maps `10.0.2.2` to the host machine's `localhost`.

## Prerequisites for running E2E tests

1. Backend running at `localhost:8000` with `DEBUG=true`
2. PostgreSQL running with seeded reference data (festivals, achievements, recipes)
3. Android emulator running (API 34 recommended — API 36 has Espresso compatibility issues)

## MUST NOT

- MUST NOT use real Firebase Phone Auth in E2E tests — OTP verification is external and flaky
- MUST NOT run E2E tests against a non-DEBUG backend — the fake token will be rejected
- MUST NOT use API 36 emulators for E2E tests — known Espresso compatibility issues. Use API 34 locally, API 29 in CI
- MUST NOT hardcode user data in E2E tests — use `TestDataFactory` and `TestProfileProvider` for deterministic test data
