"""FastAPI application entry point."""

import logging
from contextlib import asynccontextmanager

import sentry_sdk
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi.errors import RateLimitExceeded
from starlette.middleware.base import BaseHTTPMiddleware

from app.api.v1.router import api_router
from app.cache import warm_recipe_cache
from app.config import settings
from app.core.rate_limit import limiter

# Initialize Sentry (before app creation)
if settings.sentry_dsn:
    sentry_sdk.init(
        dsn=settings.sentry_dsn,
        send_default_pii=False,  # Never send PII to Sentry
        traces_sample_rate=0.1,  # Lower sample rate for production
        environment="production" if not settings.debug else "development",
    )
from app.core.exceptions import (
    AuthenticationError,
    BadRequestError,
    ForbiddenError,
    NotFoundError,
)
from app.core.firebase import initialize_firebase
from app.db.postgres import engine, init_db, close_db

# Configure structured logging
if not settings.debug:
    try:
        from pythonjsonlogger import jsonlogger

        handler = logging.StreamHandler()
        formatter = jsonlogger.JsonFormatter(
            "%(asctime)s %(name)s %(levelname)s %(message)s",
            rename_fields={"asctime": "timestamp", "levelname": "level"},
        )
        handler.setFormatter(formatter)
        logging.root.handlers = [handler]
    except ImportError:
        pass  # Fall back to basic logging if python-json-logger not installed

logging.basicConfig(
    level=logging.DEBUG if settings.debug else logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    """Add security headers to all responses."""

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


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler for startup and shutdown."""
    # Startup
    logger.info("Starting RasoiAI Backend...")

    # Initialize Firebase (for authentication only)
    initialize_firebase()
    logger.info("Firebase initialized (authentication)")

    # Initialize PostgreSQL connection
    await init_db()
    logger.info("PostgreSQL connection initialized")

    # Warm recipe cache with popular categories
    # This reduces database reads for the first meal plan generation
    try:
        await warm_recipe_cache()
    except Exception as e:
        # Cache warming is optional - don't fail startup if it fails
        logger.warning(f"Cache warm-up failed (non-fatal): {e}")

    logger.info("RasoiAI Backend started successfully")

    yield

    # Shutdown
    logger.info("Shutting down RasoiAI Backend...")
    await close_db()
    logger.info("PostgreSQL connection pool closed")


# Create FastAPI application
app = FastAPI(
    title="RasoiAI API",
    description="AI-powered Indian meal planning API",
    version="1.0.0",
    docs_url="/docs" if settings.debug else None,
    redoc_url="/redoc" if settings.debug else None,
    lifespan=lifespan,
)

# Add security headers middleware
app.add_middleware(SecurityHeadersMiddleware)

# Configure rate limiter
app.state.limiter = limiter


@app.exception_handler(RateLimitExceeded)
async def rate_limit_handler(request: Request, exc: RateLimitExceeded):
    return JSONResponse(
        status_code=429,
        content={"detail": "Rate limit exceeded. Please try again later."},
    )


# Add CORS middleware only when origins are configured
if settings.cors_origins:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins,
        allow_credentials=True,
        allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH"],
        allow_headers=["Authorization", "Content-Type"],
    )


# Exception handlers
@app.exception_handler(AuthenticationError)
async def authentication_error_handler(request: Request, exc: AuthenticationError):
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail},
        headers=exc.headers,
    )


@app.exception_handler(ForbiddenError)
async def forbidden_error_handler(request: Request, exc: ForbiddenError):
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail},
    )


@app.exception_handler(NotFoundError)
async def not_found_error_handler(request: Request, exc: NotFoundError):
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail},
    )


@app.exception_handler(BadRequestError)
async def bad_request_error_handler(request: Request, exc: BadRequestError):
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail},
    )


# Include API router
app.include_router(api_router, prefix=settings.api_v1_prefix)


# Health check endpoint with database connectivity test
@app.get("/health")
async def health_check():
    """Health check endpoint with database connectivity test."""
    from sqlalchemy import text

    try:
        async with engine.connect() as conn:
            await conn.execute(text("SELECT 1"))
        return {"status": "healthy", "version": "1.0.0"}
    except Exception:
        return JSONResponse(
            status_code=503,
            content={"status": "unhealthy", "version": "1.0.0"},
        )


@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "name": "RasoiAI API",
        "version": "1.0.0",
        "docs": "/docs" if settings.debug else "Disabled in production",
    }
