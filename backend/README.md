# RasoiAI Backend

AI-powered Indian meal planning API built with FastAPI.

## Quick Start

### Prerequisites

- Python 3.11+
- PostgreSQL 15+
- Redis (optional, for caching)

### Local Development

1. **Create virtual environment**
   ```bash
   python -m venv venv
   source venv/bin/activate  # Linux/Mac
   # or
   .\venv\Scripts\activate  # Windows
   ```

2. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

3. **Set up environment**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

4. **Start PostgreSQL** (using Docker)
   ```bash
   docker run -d --name rasoiai-db \
     -e POSTGRES_USER=rasoiai \
     -e POSTGRES_PASSWORD=rasoiai_password \
     -e POSTGRES_DB=rasoiai \
     -p 5432:5432 \
     postgres:15-alpine
   ```

5. **Run migrations**
   ```bash
   alembic upgrade head
   ```

6. **Seed data**
   ```bash
   python -m scripts.seed_recipes
   python -m scripts.seed_festivals
   ```

7. **Start server**
   ```bash
   uvicorn app.main:app --reload
   ```

8. **Open API docs**: http://localhost:8000/docs

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f api

# Stop services
docker-compose down
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/firebase` | Exchange Firebase token for JWT |
| GET | `/api/v1/users/me` | Get current user |
| PUT | `/api/v1/users/preferences` | Update preferences |
| POST | `/api/v1/meal-plans/generate` | Generate meal plan (AI) |
| GET | `/api/v1/meal-plans/current` | Get current week's plan |
| GET | `/api/v1/meal-plans/{id}` | Get specific plan |
| POST | `/api/v1/meal-plans/{planId}/items/{itemId}/swap` | Swap meal |
| PUT | `/api/v1/meal-plans/{planId}/items/{itemId}/lock` | Lock/unlock meal |
| GET | `/api/v1/recipes/{id}` | Get recipe details |
| GET | `/api/v1/recipes/{id}/scale` | Scale recipe servings |
| GET | `/api/v1/recipes/search` | Search recipes |
| GET | `/api/v1/grocery` | Get grocery list |
| GET | `/api/v1/grocery/whatsapp` | WhatsApp formatted list |
| GET | `/api/v1/festivals/upcoming` | Upcoming festivals |
| POST | `/api/v1/chat/message` | AI chat |
| GET | `/api/v1/chat/history` | Chat history |
| GET | `/api/v1/stats/streak` | Cooking streak |
| GET | `/api/v1/stats/monthly` | Monthly stats |

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `DATABASE_URL` | PostgreSQL connection URL | Yes |
| `JWT_SECRET_KEY` | Secret for JWT signing | Yes |
| `ANTHROPIC_API_KEY` | Claude API key | For AI features |
| `FIREBASE_CREDENTIALS_PATH` | Path to Firebase service account | For auth |
| `DEBUG` | Enable debug mode | No (default: false) |

## Project Structure

```
backend/
├── app/
│   ├── main.py              # FastAPI entry point
│   ├── config.py            # Settings
│   ├── api/v1/              # API endpoints
│   ├── core/                # Security, exceptions
│   ├── db/                  # Database config
│   ├── models/              # SQLAlchemy models
│   ├── schemas/             # Pydantic schemas
│   ├── services/            # Business logic
│   └── ai/                  # Claude integration
├── alembic/                 # Database migrations
├── scripts/                 # Seed scripts
├── tests/                   # Test suite
├── requirements.txt
├── Dockerfile
└── docker-compose.yml
```

## Testing

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=app

# Run specific test file
pytest tests/test_auth.py -v
```

## Database Migrations

```bash
# Create new migration
alembic revision --autogenerate -m "Description"

# Apply migrations
alembic upgrade head

# Rollback
alembic downgrade -1
```
