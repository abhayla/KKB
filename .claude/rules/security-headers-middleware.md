---
description: SecurityHeadersMiddleware convention — required response headers and production-only HSTS.
globs: ["backend/app/main.py", "backend/app/core/**/*.py"]
---

# Security Headers Middleware

## Required Headers

All API responses MUST include security headers via `SecurityHeadersMiddleware` in `backend/app/main.py`:

```python
class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["X-XSS-Protection"] = "1; mode=block"
        response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
        response.headers["X-API-Version"] = "1.0"
        if not settings.debug:
            response.headers["Strict-Transport-Security"] = (
                "max-age=31536000; includeSubDomains"
            )
        return response
```

## Header Purpose

| Header | Value | Why |
|--------|-------|-----|
| `X-Content-Type-Options` | `nosniff` | Prevents MIME-type sniffing attacks |
| `X-Frame-Options` | `DENY` | Blocks clickjacking via iframes |
| `X-XSS-Protection` | `1; mode=block` | Legacy XSS filter for older browsers |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Limits referrer leakage to origin only |
| `X-API-Version` | `1.0` | API version for client compatibility |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | Forces HTTPS (production only) |

## Rules

1. **HSTS is production-only** — `Strict-Transport-Security` MUST NOT be set when `settings.debug` is `True`. HSTS on localhost breaks local development (browsers cache the directive and refuse HTTP).

2. **Middleware registration order** — `SecurityHeadersMiddleware` MUST be added before CORS middleware. Middleware runs in reverse registration order — security headers should be the outermost wrapper.

3. **CORS is conditional** — CORS middleware is only added when `settings.cors_origins` is non-empty. The Android app communicates directly (no browser CORS), so the default is no CORS.

```python
# Correct order in main.py
app.add_middleware(SecurityHeadersMiddleware)  # First (outermost)
if settings.cors_origins:
    app.add_middleware(CORSMiddleware, ...)     # Second (only if needed)
```

## Anti-Patterns

- NEVER add HSTS in debug mode — it poisons the browser's HSTS cache for localhost
- NEVER remove `X-Frame-Options: DENY` unless the API explicitly serves embeddable content (it doesn't)
- NEVER add security headers per-endpoint — use the middleware so no endpoint can accidentally skip them
- NEVER add CORS for all origins (`allow_origins=["*"]`) — if CORS is needed, list explicit origins
