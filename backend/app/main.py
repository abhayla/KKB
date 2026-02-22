"""FastAPI application entry point."""

import logging
from contextlib import asynccontextmanager

import sentry_sdk
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.api.v1.router import api_router
from app.cache import warm_recipe_cache
from app.config import settings

# Initialize Sentry (before app creation)
if settings.sentry_dsn:
    sentry_sdk.init(
        dsn=settings.sentry_dsn,
        send_default_pii=True,
        traces_sample_rate=0.2,
        environment="production" if not settings.debug else "development",
    )
from app.core.exceptions import (
    AuthenticationError,
    BadRequestError,
    ForbiddenError,
    NotFoundError,
)
from app.core.firebase import initialize_firebase
from app.db.postgres import init_db, close_db

# Configure logging
logging.basicConfig(
    level=logging.DEBUG if settings.debug else logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


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

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
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


# Health check endpoint
@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "version": "1.0.0"}


@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "name": "RasoiAI API",
        "version": "1.0.0",
        "docs": "/docs" if settings.debug else "Disabled in production",
    }
