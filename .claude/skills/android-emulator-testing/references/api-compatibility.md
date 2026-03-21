# API Level Compatibility Matrix

## Current Recommendation

| Context | API Level | Reason |
|---------|:---------:|--------|
| **Local E2E tests** | 34 | Proven stable. Avoids API 36 behavior changes. |
| **CI/CD** | 29 | Lower overhead, configured in `android-ci.yml` |
| **Manual QA** | 34 or 36 | Both work for manual testing |

## API 36 (Android 16) — Status: LIKELY COMPATIBLE

**Original concern:** CLAUDE.md stated "API 36 has Espresso compatibility issues" — added early in project.

**Web research (March 2026) found:**
- No documented Espresso-specific bug for API 36
- The issues are likely:
  1. **16KB page size images** — some API 36 emulator images use 16KB pages, which crash NDK/native libs. Use standard `google_apis` images (not `16KBPageSize` variants).
  2. **Auto-resizability** — API 36 apps targeting it become auto-resizable + multi-window, which can change layout during tests.
  3. **Espresso version** — older Espresso 3.5.x had `IncompatibleClassChangeError` on some APIs. Fixed in 3.6.1+/3.7.0.

**Recommendation:** Try API 36 with standard `google_apis` image + Espresso 3.7.0. If it works, update this file.

## API 34 — Status: CONFIRMED WORKING

- Compose UI Testing works
- Espresso works
- HiltTestRunner works
- System image: `system-images;android-34;google_apis;x86_64`

## API 29 — Status: CI ONLY

- Used in GitHub Actions (`android-ci.yml`)
- Lower resource requirements
- Some UI features may render differently

## System Image Selection

| Image Type | 16KB Pages | Google Play | Recommended |
|-----------|:----------:|:-----------:|:-----------:|
| `google_apis` | No | No | Yes (testing) |
| `google_apis_playstore` | No | Yes | OK (manual QA) |
| `16KBPageSize` | Yes | Varies | No (native lib crashes) |

## Installed Images on This Machine

```
android-34/google_apis/x86_64     ← Used for Pixel_8a_API_34
android-34/google_apis/arm64-v8a
android-36/                        ← All existing AVDs use this
android-36.1/
```

## AVD Inventory

| AVD | API | Compatible | Notes |
|-----|:---:|:----------:|-------|
| Pixel_8a_API_34 | 34 | Yes | Created for E2E testing |
| Pixel_6a | 36 | Untested | Standard google_apis |
| Pixel_7_Pro | 36 | Untested | Standard google_apis |
| Pixel_7a | 36 | Untested | 16KBPageSize variant — avoid |
| Pixel_8_Pro | 36 | Untested | Standard google_apis |
| Pixel_8a | 36 | Untested | Standard google_apis |
| Pixel_9_Pro | 36 | Untested | Standard google_apis |
| Pixel_9_Pro_XL | 36 | Untested | Standard google_apis |
| Pixel_9a | 36 | Untested | Standard google_apis |
