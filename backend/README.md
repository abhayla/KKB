# RasoiAI Backend

AI-powered Indian meal planning API built with FastAPI and Firebase Firestore.

## Quick Start

### Prerequisites

- Python 3.11+
- Firebase project with Firestore enabled
- Anthropic API key (for AI features)

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

3. **Configure Firebase**
   ```bash
   # Download service account key from Firebase Console
   # Save as rasoiai-firebase-service-account.json

   export FIREBASE_CREDENTIALS_PATH=./rasoiai-firebase-service-account.json  # Linux/Mac/Git Bash
   # $env:FIREBASE_CREDENTIALS_PATH = "./rasoiai-firebase-service-account.json"  # Windows PowerShell
   ```

4. **Seed Firestore with initial data**
   ```bash
   PYTHONPATH=. python scripts/seed_firestore.py
   ```

5. **Start server**
   ```bash
   uvicorn app.main:app --reload
   ```

6. **Open API docs**: http://localhost:8000/docs

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
| POST | `/api/v1/chat/message` | AI chat with tool calling |
| GET | `/api/v1/chat/history` | Chat history |
| GET | `/api/v1/stats/streak` | Cooking streak |
| GET | `/api/v1/stats/monthly` | Monthly stats |

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `FIREBASE_CREDENTIALS_PATH` | Path to Firebase service account JSON | Yes |
| `JWT_SECRET_KEY` | Secret for JWT signing | Yes |
| `ANTHROPIC_API_KEY` | Claude API key | For AI features |
| `DEBUG` | Enable debug mode (`fake-firebase-token` accepted) | No (default: false) |

## Project Structure

```
backend/
├── app/
│   ├── main.py                  # FastAPI entry point
│   ├── config.py                # Settings
│   ├── api/v1/                  # API endpoints
│   ├── core/                    # Security, exceptions
│   ├── repositories/            # Firestore data access
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
├── tests/                       # Test suite (92 tests)
└── requirements.txt
```

## Testing

```bash
# Run all tests (92 total)
pytest

# Run with coverage
pytest --cov=app

# Run specific test file
pytest tests/test_auth.py -v
pytest tests/test_preference_service.py -v
pytest tests/test_chat_integration.py -v
pytest tests/test_meal_generation.py -v
pytest tests/test_chat_api.py -v

# Run single test method
pytest tests/test_preference_service.py::test_add_include_rule -v
```

**Note:** Run tests from the backend directory with PYTHONPATH set:
```bash
cd backend
PYTHONPATH=. pytest tests/
```

## Recipe Database

- **3,590 recipes** in Firestore (project: `rasoiai-6dcdd`)
- 3,580 imported from khanakyabanega + 10 seed recipes
- Distribution: North (3,124), South (358), West (85), East (23)

## Config Sync

Meal generation configuration is stored in YAML files and synced to Firestore:

```bash
# Preview changes
python scripts/sync_config.py --dry-run

# Sync all config
python scripts/sync_config.py
```
