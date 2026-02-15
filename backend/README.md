# RasoiAI Backend

AI-powered Indian meal planning API built with FastAPI and PostgreSQL.

## Quick Start

### Prerequisites

- Python 3.11+
- PostgreSQL database
- Anthropic API key (for AI chat features)
- Google AI API key (for Gemini meal generation)

### Local Development

1. **Create virtual environment**
   ```bash
   python -m venv venv
   source venv/bin/activate  # Linux/Mac/Git Bash
   # .\venv\Scripts\activate  # Windows PowerShell
   ```

2. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

3. **Configure environment**
   ```bash
   # Create .env file with:
   DATABASE_URL=postgresql+asyncpg://rasoiai_user:password@localhost:5432/rasoiai
   FIREBASE_CREDENTIALS_PATH=./rasoiai-firebase-service-account.json
   ANTHROPIC_API_KEY=sk-ant-...
   GOOGLE_AI_API_KEY=your-gemini-api-key
   JWT_SECRET_KEY=your-secret-key
   DEBUG=true
   ```

4. **Setup PostgreSQL**
   ```sql
   CREATE DATABASE rasoiai;
   CREATE USER rasoiai_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE rasoiai TO rasoiai_user;
   ```

5. **Run migrations and seed data**
   ```bash
   alembic upgrade head
   PYTHONPATH=. python scripts/seed_festivals.py
   PYTHONPATH=. python scripts/seed_achievements.py
   PYTHONPATH=. python scripts/import_recipes_postgres.py
   PYTHONPATH=. python scripts/sync_config_postgres.py
   ```

6. **Start server**
   ```bash
   uvicorn app.main:app --reload
   ```

7. **Open API docs**: http://localhost:8000/docs

## Project Structure

```
backend/
├── app/
│   ├── main.py                  # FastAPI entry point
│   ├── config.py                # Settings
│   ├── api/v1/                  # API endpoints
│   ├── core/                    # Security, exceptions
│   ├── db/                      # PostgreSQL connection
│   ├── repositories/            # Data access layer
│   ├── schemas/                 # Pydantic schemas
│   ├── services/                # Business logic
│   │   ├── meal_generation_service.py  # 2-item pairing logic
│   │   ├── preference_update_service.py # INCLUDE/EXCLUDE rules
│   │   └── config_service.py    # YAML config management
│   └── ai/                      # Claude integration
│       ├── chat_assistant.py    # Tool calling orchestration
│       └── tools/               # Chat tool definitions
├── config/                      # YAML configuration files
│   ├── meal_generation.yaml     # Pairing rules, meal structure
│   └── reference_data/          # Ingredients, dishes, cuisines
├── scripts/                     # Utility scripts
├── tests/                       # Test suite (~447 tests, 35 files)
└── requirements.txt
```

## Testing

```bash
# Run all tests (~351 total)
PYTHONPATH=. pytest

# Run with coverage
PYTHONPATH=. pytest --cov=app

# Run specific test file
PYTHONPATH=. pytest tests/test_auth.py -v
PYTHONPATH=. pytest tests/test_preference_service.py -v
PYTHONPATH=. pytest tests/test_chat_integration.py -v
PYTHONPATH=. pytest tests/test_meal_generation.py -v

# Run single test method
PYTHONPATH=. pytest tests/test_preference_service.py::test_add_include_rule -v
```

**Note:** Always run tests from the backend directory with PYTHONPATH set.

## Recipe Database

- **3,580 recipes** in PostgreSQL
- Imported from khanakyabanega dataset
- Distribution: North (majority), South, West, East

## Config Sync

Meal generation configuration is stored in YAML files and synced to PostgreSQL:

```bash
# Sync all config
PYTHONPATH=. python scripts/sync_config_postgres.py
```
