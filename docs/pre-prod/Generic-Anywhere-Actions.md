# Pre-Production: Generic Actions (Dev Machine)

These fixes can be applied from any development machine — no server access required. Work through them in priority order before deploying to production.

**Related:** [VPS-Only-Actions.md](./VPS-Only-Actions.md) — Server-side setup, deployment, infrastructure.
**Related:** [Production-Deployment-Strategy.md](./Production-Deployment-Strategy.md) — High-level deployment overview.

---

## Critical Priority (Fix Before Any Deployment)

### 1. JWT Secret Has Hardcoded Default

**Issue:** If `.env` is missing or `JWT_SECRET_KEY` is unset, the app silently uses a known default. Any attacker can forge valid JWTs.

**File:** `backend/app/config.py:25`
```python
# CURRENT (vulnerable)
jwt_secret_key: str = "development-secret-key-change-in-production"
```

**Fix:** Remove the default value. App should crash on startup if secret is missing.
```python
# FIXED
jwt_secret_key: str  # No default — requires JWT_SECRET_KEY in .env
```

**Verification:**
```bash
cd backend
# Remove JWT_SECRET_KEY from .env temporarily, then:
PYTHONPATH=. python -c "from app.config import settings; print(settings.jwt_secret_key)"
# Should raise ValidationError, not print a default
```

---

### 2. DEBUG Mode Defaults to True

**Issue:** If `.env` is missing or `DEBUG` is unset, the app runs in debug mode — accepting fake Firebase tokens, exposing `/docs`, logging SQL queries.

**File:** `backend/app/config.py:40`
```python
# CURRENT (dangerous)
debug: bool = True
```

**Fix:** Default to `False`. Development environments must explicitly opt in.
```python
# FIXED
debug: bool = False
```

**Verification:**
```bash
cd backend
# Remove DEBUG from .env temporarily, then:
PYTHONPATH=. python -c "from app.config import settings; print(settings.debug)"
# Should print: False
```

---

### 3. CORS Allows All Origins

**Issue:** `["*"]` means any website can make authenticated API requests. Enables CSRF and data exfiltration.

**File:** `backend/app/config.py:43`
```python
# CURRENT (open to all)
cors_origins: list[str] = ["*"]
```

**Fix:** Whitelist specific origins. Use env var for flexibility.
```python
# FIXED — default to empty, require explicit configuration
cors_origins: list[str] = []  # Set CORS_ORIGINS=["https://rasoiai.com"] in .env
```

Also update `backend/app/main.py:83-89` — when `cors_origins` is empty, skip adding CORS middleware entirely (the Android app doesn't need CORS since it's not a browser).

**Verification:**
```bash
# Test that cross-origin requests are rejected
curl -H "Origin: https://evil.com" -I http://localhost:8000/health
# Should NOT contain: access-control-allow-origin: *
```

---

### 4. No User Deletion / Account Data Export Endpoint

**Issue:** GDPR and India's Digital Personal Data Protection Act 2023 require data deletion and export capabilities. Currently no such endpoints exist.

**File:** `backend/app/api/v1/endpoints/users.py` — only has `GET /me`, `PUT /me`, `PUT /me/preferences`

**Fix:** Add two new endpoints:
- `DELETE /api/v1/users/me` — soft-delete account (mark inactive, schedule data purge after 30 days)
- `GET /api/v1/users/me/export` — return all user data as JSON (preferences, meal plans, rules, chat history)

Create `backend/app/services/user_deletion_service.py` with cascade logic. Reference the existing `scripts/cleanup_user.py` for the deletion pattern.

**Verification:**
```bash
cd backend
PYTHONPATH=. pytest tests/test_users_api.py -v -k "delete or export"
```

---

### 5. Release Signing Not Configured (Android)

**Issue:** Release builds fall back to debug signing key. Google Play Store will reject the APK, and the app can be tampered with.

**File:** `android/app/build.gradle.kts:51-69`
```kotlin
// CURRENT — signing config is empty and commented out
signingConfigs {
    create("release") {
        // TODO: Configure release signing
        // storeFile = file("../keystore/release.keystore")
    }
}
buildTypes {
    release {
        // signingConfig = signingConfigs.getByName("release")  // ← commented out
    }
}
```

**Fix:** Generate a keystore and configure signing via environment variables:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "../keystore/release.keystore")
        storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
        keyAlias = System.getenv("KEY_ALIAS") ?: ""
        keyPassword = System.getenv("KEY_PASSWORD") ?: ""
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ... existing minify/proguard config
    }
}
```

Generate the keystore (do once, store securely):
```bash
keytool -genkey -v -keystore release.keystore -alias rasoiai \
  -keyalg RSA -keysize 2048 -validity 10000
```

**Verification:**
```bash
cd android
./gradlew assembleRelease
# Should produce signed APK at app/build/outputs/apk/release/
```

---

### 6. Crashlytics Reporting Disabled in Release

**Issue:** `CrashReportingTree` is planted in release builds but the actual Crashlytics calls are commented out. Production crashes will be silently swallowed.

**File:** `android/app/src/main/java/com/rasoiai/app/RasoiAIApplication.kt:99-109`
```kotlin
// CURRENT — does nothing
private class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
            return
        }
        // FirebaseCrashlytics.getInstance().log("$tag: $message")      // ← commented out
        // if (t != null) {
        //     FirebaseCrashlytics.getInstance().recordException(t)     // ← commented out
        // }
    }
}
```

**Fix:** Uncomment the Crashlytics calls:
```kotlin
private class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
            return
        }
        FirebaseCrashlytics.getInstance().log("$tag: $message")
        if (t != null) {
            FirebaseCrashlytics.getInstance().recordException(t)
        }
    }
}
```

**Verification:**
```bash
cd android
./gradlew assembleRelease  # Should compile without errors
# Then install on device and trigger a crash — check Firebase Console for report
```

---

## High Priority (Fix Before Public Launch)

### 7. No Rate Limiting on API

**Issue:** No rate limiting middleware. Attackers can brute-force auth, spam AI generation (expensive Gemini calls), or DoS the API.

**File:** `backend/app/main.py` — no `slowapi` or similar middleware present

**Fix:** Install and configure `slowapi`:
```bash
cd backend && pip install slowapi && pip freeze | grep slowapi >> requirements.txt
```

Add to `backend/app/main.py`:
```python
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded

limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)
```

Apply to expensive endpoints:
```python
# In meal_plans.py
@router.post("/generate")
@limiter.limit("5/hour")  # Max 5 meal plan generations per hour
async def generate_meal_plan(request: Request, ...):

# In chat.py
@router.post("/message")
@limiter.limit("30/minute")  # Max 30 chat messages per minute
async def send_message(request: Request, ...):

# In auth.py
@router.post("/firebase")
@limiter.limit("10/minute")  # Max 10 auth attempts per minute
async def firebase_auth(request: Request, ...):
```

**Verification:**
```bash
cd backend
# Hit the auth endpoint 11 times rapidly
for i in $(seq 1 11); do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8000/api/v1/auth/firebase \
    -H "Content-Type: application/json" -d '{"firebase_token":"test"}'
done
# Last request should return 429 Too Many Requests
```

---

### 8. AI Usage Limits / Cost Controls

**Issue:** No per-user limits on Gemini API calls (meal generation, photo analysis, chat). A single user could rack up significant API costs.

**File:** `backend/app/api/v1/endpoints/meal_plans.py`, `chat.py`, `photos.py`

**Fix:** Add a usage tracking table and check before AI calls:
```python
# In a new file: backend/app/services/usage_limit_service.py
async def check_usage_limit(db: AsyncSession, user_id: str, action: str) -> bool:
    """Check if user has exceeded their daily/weekly limit for an action."""
    # Limits: meal_generation=3/day, chat_message=50/day, photo_analysis=10/day
    ...
```

This requires:
1. New `usage_log` model (user_id, action, timestamp)
2. Check before each AI endpoint call
3. Return 429 with `Retry-After` header when exceeded

**Verification:**
```bash
cd backend
PYTHONPATH=. pytest tests/test_usage_limits.py -v
```

---

### 9. Missing Database Indexes

**Issue:** Frequently queried columns lack indexes, causing full table scans.

**File:** `backend/app/models/user.py`
```python
# CURRENT — email column has no index
email: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
```

**File:** `backend/app/models/meal_plan.py` — `user_id` + `is_active` queried together frequently

**File:** `backend/app/models/recipe_rule.py` — `user_id` queried on every meal generation

**Fix:** Add indexes via Alembic migration:
```bash
cd backend
alembic revision --autogenerate -m "add_performance_indexes"
```

Key indexes to add:
```python
# user.py
email: Mapped[Optional[str]] = mapped_column(String(255), nullable=True, index=True)

# meal_plan.py — composite index for common query pattern
__table_args__ = (
    Index('ix_meal_plans_user_active', 'user_id', 'is_active'),
)

# recipe_rule.py
__table_args__ = (
    Index('ix_recipe_rules_user_id', 'user_id'),
)

# chat_message.py
__table_args__ = (
    Index('ix_chat_messages_user_created', 'user_id', 'created_at'),
)
```

**Verification:**
```bash
cd backend
alembic upgrade head
PYTHONPATH=. python -c "
from sqlalchemy import inspect
from app.db.postgres import engine
import asyncio
async def check():
    async with engine.connect() as conn:
        # Check indexes exist
        result = await conn.execute(text(\"SELECT indexname FROM pg_indexes WHERE tablename='users'\"))
        print(result.fetchall())
asyncio.run(check())
"
```

---

### 10. Health Check Doesn't Verify Dependencies

**Issue:** `/health` always returns `{"status": "healthy"}` even if PostgreSQL is down.

**File:** `backend/app/main.py:131-134`
```python
@app.get("/health")
async def health_check():
    return {"status": "healthy", "version": "1.0.0"}
```

**Fix:** Check database connectivity:
```python
from app.db.postgres import engine

@app.get("/health")
async def health_check():
    try:
        async with engine.connect() as conn:
            await conn.execute(text("SELECT 1"))
        return {"status": "healthy", "version": "1.0.0"}
    except Exception:
        return JSONResponse(
            status_code=503,
            content={"status": "unhealthy", "version": "1.0.0"}
        )
```

**Verification:**
```bash
curl http://localhost:8000/health
# With DB running: {"status": "healthy", "version": "1.0.0"}
# With DB stopped: 503 {"status": "unhealthy", "version": "1.0.0"}
```

---

### 11. Stale Data Cleanup Missing

**Issue:** No scheduled cleanup for old meal plans, expired tokens, stale chat messages. Database will grow unbounded.

**Files affected:** All repository files — no TTL or cleanup logic

**Fix:** Create `backend/app/services/cleanup_service.py`:
```python
async def cleanup_stale_data(db: AsyncSession):
    """Remove data older than retention period."""
    cutoff_90_days = datetime.utcnow() - timedelta(days=90)
    cutoff_30_days = datetime.utcnow() - timedelta(days=30)

    # Delete inactive meal plans older than 90 days
    await db.execute(
        delete(MealPlan).where(
            MealPlan.is_active == False,
            MealPlan.updated_at < cutoff_90_days
        )
    )
    # Delete chat messages older than 30 days
    await db.execute(
        delete(ChatMessage).where(ChatMessage.created_at < cutoff_30_days)
    )
    await db.commit()
```

Trigger via a management command or cron (see VPS doc for cron setup).

**Verification:**
```bash
cd backend
PYTHONPATH=. pytest tests/test_cleanup_service.py -v
```

---

### 12. Sentry Sends PII by Default

**Issue:** `send_default_pii=True` sends user IDs, emails, and IP addresses to Sentry.

**File:** `backend/app/main.py:17-22`
```python
sentry_sdk.init(
    dsn=settings.sentry_dsn,
    send_default_pii=True,   # ← sends user data to Sentry
    traces_sample_rate=0.2,
)
```

**Fix:** Disable PII, use custom scrubbing if needed:
```python
sentry_sdk.init(
    dsn=settings.sentry_dsn,
    send_default_pii=False,
    traces_sample_rate=0.1,  # Lower for production cost
    environment="production" if not settings.debug else "development",
    before_send=_scrub_sensitive_data,  # Optional custom scrubber
)
```

**Verification:**
```bash
# Trigger an error and check Sentry dashboard — should not contain email/IP
cd backend
PYTHONPATH=. python -c "
import sentry_sdk
sentry_sdk.capture_message('Test PII scrubbing')
"
```

---

### 13. Image Upload Size/Type Validation Incomplete

**Issue:** Photo endpoint checks file size (10MB) but doesn't validate file type or sanitize filename.

**File:** `backend/app/api/v1/endpoints/photos.py:25-35`

**Fix:** Add content-type validation and reduce max size:
```python
ALLOWED_TYPES = {"image/jpeg", "image/png", "image/webp"}
MAX_SIZE_BYTES = 5 * 1024 * 1024  # 5MB

@router.post("/analyze")
async def analyze_photo(file: UploadFile = File(...)):
    if file.content_type not in ALLOWED_TYPES:
        raise BadRequestError(f"Unsupported file type: {file.content_type}")
    image_data = await file.read()
    if len(image_data) > MAX_SIZE_BYTES:
        raise BadRequestError(f"Image too large (max {MAX_SIZE_BYTES // 1024 // 1024}MB)")
```

**Verification:**
```bash
cd backend
# Test with invalid file type
curl -X POST http://localhost:8000/api/v1/photos/analyze \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test.txt;type=text/plain"
# Should return 400
```

---

### 14. SQL Echo Tied to DEBUG Flag

**Issue:** `echo=settings.debug` logs all SQL queries when DEBUG is on. If DEBUG is accidentally left on in production, all queries appear in logs.

**File:** `backend/app/db/postgres.py:19-21`
```python
engine = create_async_engine(
    settings.database_url,
    echo=settings.debug,  # ← logs ALL SQL in debug mode
)
```

**Fix:** Use a separate setting or always disable:
```python
engine = create_async_engine(
    settings.database_url,
    echo=False,  # Never echo SQL; use query logging middleware if needed
)
```

**Verification:**
```bash
cd backend
DEBUG=true PYTHONPATH=. python -c "from app.db.postgres import engine; print('echo:', engine.echo)"
# Should print: echo: False
```

---

### 15. Certificate Pinning Not Active (Android)

**Issue:** Network security config has pinning section commented out. MITM attacks possible on production API.

**File:** `android/app/src/main/res/xml/network_security_config.xml:26-33`
```xml
<!-- Uncomment and configure when API is deployed
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">api.rasoiai.app</domain>
    <pin-set expiration="2025-12-31">
        <pin digest="SHA-256">YOUR_CERTIFICATE_PIN_HERE</pin>
    </pin-set>
</domain-config>
-->
```

**Fix:** After deploying API with HTTPS, get the certificate pin:
```bash
# Get SHA-256 pin from your production API certificate
openssl s_client -connect api.rasoiai.com:443 </dev/null 2>/dev/null | \
  openssl x509 -pubkey -noout | \
  openssl pkey -pubin -outform der | \
  openssl dgst -sha256 -binary | openssl enc -base64
```

Then uncomment and populate the pin. Set expiration to 1 year from deployment.

Also remove the cleartext exception for `10.0.2.2` and `localhost` in release builds (use `debugOverrides` section instead).

**Verification:**
```bash
cd android
./gradlew assembleRelease
# Install on device, verify HTTPS connection works
# Use mitmproxy to verify pinning rejects intercepted certificates
```

---

### 16. Security Headers Missing

**Issue:** No `X-Frame-Options`, `X-Content-Type-Options`, `Strict-Transport-Security`, or `X-XSS-Protection` headers.

**File:** `backend/app/main.py` — no security headers middleware

**Fix:** Add middleware in `backend/app/main.py`:
```python
from starlette.middleware.base import BaseHTTPMiddleware

class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["X-XSS-Protection"] = "1; mode=block"
        response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
        if not settings.debug:
            response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
        return response

app.add_middleware(SecurityHeadersMiddleware)
```

**Verification:**
```bash
curl -I http://localhost:8000/health
# Should contain: x-content-type-options: nosniff
# Should contain: x-frame-options: DENY
```

---

## Medium Priority (Fix Before Scale)

### 17. Unbounded List Queries (Pagination)

**Issue:** Several repository methods call `.all()` without limits. A user with thousands of entries can OOM the server.

**Files:**
- `backend/app/repositories/chat_repository.py:78` — loads all chat messages
- `backend/app/repositories/meal_plan_repository.py:210` — loads all plans for deactivation check

**Fix:** Add `.limit()` to all `.all()` calls, or implement cursor-based pagination:
```python
# Example: chat_repository.py
messages = result.scalars().all()
# →
messages = result.scalars().all()[:100]  # Quick fix
# Better: add limit param to the query
.limit(request.page_size).offset(request.page * request.page_size)
```

**Verification:**
```bash
cd backend
PYTHONPATH=. pytest tests/test_chat_api.py -v  # Ensure existing tests still pass
```

---

### 18. Structured Logging (JSON)

**Issue:** Plain text logs are hard to parse, search, and alert on in production.

**File:** `backend/app/main.py:33-36`
```python
logging.basicConfig(
    level=logging.DEBUG if settings.debug else logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
```

**Fix:** Use `python-json-logger` for structured output:
```bash
cd backend && pip install python-json-logger && pip freeze | grep json >> requirements.txt
```

```python
import logging
from pythonjsonlogger import jsonlogger

handler = logging.StreamHandler()
formatter = jsonlogger.JsonFormatter(
    "%(asctime)s %(name)s %(levelname)s %(message)s",
    rename_fields={"asctime": "timestamp", "levelname": "level"}
)
handler.setFormatter(formatter)
logging.root.handlers = [handler]
logging.root.setLevel(logging.DEBUG if settings.debug else logging.INFO)
```

**Verification:**
```bash
cd backend
uvicorn app.main:app --reload 2>&1 | head -5
# Should output JSON lines like: {"timestamp": "...", "level": "INFO", "message": "..."}
```

---

### 19. Generic Exception Responses Leak Stack Traces

**Issue:** Some endpoints `raise` raw exceptions, which FastAPI converts to 500 responses containing stack trace details.

**File:** `backend/app/api/v1/endpoints/meal_plans.py:286-287`
```python
except Exception as e:
    logger.error(f"Error generating meal plan: {e}")
    logger.error(traceback.format_exc())
    raise  # ← re-raises raw exception to client
```

**Fix:** Catch and return a generic error message:
```python
except Exception as e:
    logger.error(f"Error generating meal plan: {e}", exc_info=True)
    raise HTTPException(
        status_code=500,
        detail="Meal plan generation failed. Please try again."
    )
```

Audit all endpoints for similar patterns:
```bash
cd backend
grep -rn "raise$" app/api/ --include="*.py"
```

**Verification:**
```bash
# Trigger an error and verify response doesn't contain file paths
curl -X POST http://localhost:8000/api/v1/meal-plans/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"week_start_date": "invalid"}'
# Should NOT contain "Traceback", "File \"/app/..."
```

---

### 20. ProGuard Rules — Data Module Inconsistency

**Issue:** App module has `isMinifyEnabled = true` for release, but data module has `isMinifyEnabled = false`.

**File:** `android/data/build.gradle.kts:30`
```kotlin
release {
    isMinifyEnabled = false  // ← inconsistent with app module
}
```

**Fix:** Enable minification in data module to match:
```kotlin
release {
    isMinifyEnabled = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

Ensure data module's ProGuard rules keep DTOs and entities (already configured in app module's rules).

**Verification:**
```bash
cd android
./gradlew assembleRelease  # Should build without ClassNotFoundException
```

---

### 21. API Versioning — No Deprecation Strategy

**Issue:** API is at `/api/v1/` with no mechanism for version negotiation or deprecation headers.

**File:** `backend/app/config.py:39`
```python
api_v1_prefix: str = "/api/v1"
```

**Fix:** Add version headers to responses (lightweight, no breaking changes):
```python
# In SecurityHeadersMiddleware or separate middleware
response.headers["X-API-Version"] = "1.0"
response.headers["X-API-Deprecation"] = ""  # Empty = not deprecated
```

This allows future `v2` migration by setting `X-API-Deprecation: 2027-01-01` on v1 responses.

**Verification:**
```bash
curl -I http://localhost:8000/api/v1/health
# Should contain: x-api-version: 1.0
```

---

### 22. Accessibility Audit Needed

**Issue:** Spot checks show good `contentDescription` usage, but no comprehensive audit has been done. Screen readers may miss interactive elements.

**Files:** All Compose screen files in `android/app/src/main/java/com/rasoiai/app/presentation/`

**Fix:** Run the Android Accessibility Scanner:
1. Install "Accessibility Scanner" from Play Store on emulator
2. Run through each screen
3. Fix any flagged issues (missing descriptions, low contrast, small touch targets)

Key areas to check:
- All `IconButton` components have `contentDescription`
- Touch targets are at least 48dp
- Color contrast ratios meet WCAG AA (4.5:1 for text)

**Verification:**
```bash
cd android
./gradlew lint --check AccessibilityDescription
```

---

### 23. Refresh Token Rotation Not Implemented

**Issue:** JWT access tokens expire after 7 days (`access_token_expire_minutes: 10080`). No refresh token rotation, no token blacklist.

**File:** `backend/app/config.py:27`
```python
access_token_expire_minutes: int = 10080  # 7 days
```

**Fix:** Implement proper token lifecycle:
1. Reduce access token expiry to 15-30 minutes
2. Issue refresh tokens with 30-day expiry
3. Rotate refresh tokens on each use (one-time use)
4. Add a token blacklist table for logout/revocation

```python
# config.py
access_token_expire_minutes: int = 30
refresh_token_expire_days: int = 30
```

This requires a new `refresh_tokens` table and updates to the auth endpoints.

**Verification:**
```bash
cd backend
PYTHONPATH=. pytest tests/test_auth.py -v
# Verify short-lived access tokens + refresh flow
```

---

### 24. Data Encryption at Rest (Android)

**Issue:** Room database and DataStore store user data in plaintext. A rooted device can extract JWT tokens, preferences, and meal plans.

**Files:**
- `android/data/src/main/java/com/rasoiai/data/local/RasoiDatabase.kt` — Room DB (plaintext)
- DataStore preferences files (plaintext)

**Fix:** For sensitive data (JWT tokens), use `EncryptedSharedPreferences`:
```kotlin
// For storing JWT token
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context, "secure_prefs", masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

Add dependency:
```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

Full Room encryption with SQLCipher is optional (significant performance cost for meal plan data that isn't highly sensitive).

**Verification:**
```bash
cd android
./gradlew assembleDebug
# Pull DB from device, verify JWT is not in plaintext
adb exec-out run-as com.rasoiai.app.debug cat shared_prefs/secure_prefs.xml
# Should be encrypted, not readable
```

---

## Summary Checklist

| # | Priority | Item | Est. Effort |
|---|----------|------|-------------|
| 1 | CRITICAL | JWT secret default | 5 min |
| 2 | CRITICAL | DEBUG default | 5 min |
| 3 | CRITICAL | CORS wildcard | 15 min |
| 4 | CRITICAL | User deletion endpoint | 2-3 hrs |
| 5 | CRITICAL | Release signing | 30 min |
| 6 | CRITICAL | Crashlytics enable | 5 min |
| 7 | HIGH | Rate limiting | 1 hr |
| 8 | HIGH | AI usage limits | 2-3 hrs |
| 9 | HIGH | Database indexes | 30 min |
| 10 | HIGH | Health check deps | 15 min |
| 11 | HIGH | Data cleanup service | 1-2 hrs |
| 12 | HIGH | Sentry PII | 5 min |
| 13 | HIGH | Image validation | 15 min |
| 14 | HIGH | SQL echo disable | 5 min |
| 15 | HIGH | Certificate pinning | 30 min |
| 16 | HIGH | Security headers | 15 min |
| 17 | MEDIUM | Pagination limits | 1-2 hrs |
| 18 | MEDIUM | Structured logging | 30 min |
| 19 | MEDIUM | Exception sanitizing | 1 hr |
| 20 | MEDIUM | ProGuard consistency | 15 min |
| 21 | MEDIUM | API versioning headers | 15 min |
| 22 | MEDIUM | Accessibility audit | 2-3 hrs |
| 23 | MEDIUM | Refresh token rotation | 3-4 hrs |
| 24 | MEDIUM | Data encryption | 2-3 hrs |

**Quick wins (< 15 min each):** Items 1, 2, 6, 12, 14 — do these first.
**Total estimated effort:** ~20-25 hours

---

*Created: February 2026*
*Companion doc: [VPS-Only-Actions.md](./VPS-Only-Actions.md)*
