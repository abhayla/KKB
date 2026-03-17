---
name: project-scaffold
description: >
  Initialize a fully configured project skeleton with build, lint, test, CI,
  Docker, security baseline, 12-factor compliance, and conventional commits.
  Multi-stack: Python, Node/TypeScript, Android, React/Next.js, Go, Rust.
triggers:
  - scaffold project
  - init project
  - project setup
  - new project
  - bootstrap project
  - project skeleton
allowed-tools: "Bash Read Write Edit Grep Glob Agent"
argument-hint: "<stack name and project description, or PRD file path>"
version: "1.0.0"
type: workflow
---

# Project Scaffold — Full Project Initialization

Initialize a production-ready project skeleton with build, lint, test, CI, security, and dev environment — all configured and verified.

**Input:** $ARGUMENTS

---

## STEP 1: Determine Stack & Scope

1. **Parse input** — Accept a stack name (e.g., "fastapi", "next.js", "android"), a PRD file path, or inline description
2. **If PRD provided** — Extract tech stack from the PRD's NFRs, constraints, and milestones
3. **Confirm stack** — Present the detected stack and ask for confirmation:

```
Detected stack: FastAPI + PostgreSQL + React (Next.js)
Scaffolding includes:
  ✓ Package manifests & lockfiles
  ✓ Folder structure (Clean Architecture layers)
  ✓ Linter + formatter
  ✓ Test framework
  ✓ Git hooks (pre-commit)
  ✓ CI pipeline (GitHub Actions)
  ✓ Docker Compose dev environment
  ✓ Security baseline (dependency audit, SAST, Dependabot)
  ✓ 12-factor compliance
  ✓ Commitlint + semantic versioning
  ✓ .editorconfig + shared IDE settings
  ✓ Health endpoint
  ✓ License file

Proceed? [Y/n]
```

Wait for confirmation before generating files.

---

## STEP 2: Generate Project Structure

Create the folder structure following Clean Architecture layer separation:

### 2.1 Stack Templates

**Python (FastAPI/Django):**
```
<project>/
├── src/
│   ├── domain/           # Entities, value objects, domain errors
│   ├── application/      # Use cases, ports (interfaces)
│   ├── infrastructure/   # DB repos, external APIs, adapters
│   └── presentation/     # API routes, serializers, middleware
├── tests/
│   ├── unit/
│   ├── integration/
│   └── e2e/
├── migrations/
├── scripts/
├── pyproject.toml
├── requirements.txt      # or poetry.lock / uv.lock
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .editorconfig
├── .gitignore
├── .pre-commit-config.yaml
├── .github/
│   ├── workflows/ci.yml
│   └── dependabot.yml
├── .semgrepconfig.yml
├── LICENSE
└── README.md
```

**Node/TypeScript (Next.js, Express, NestJS):**
```
<project>/
├── src/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── presentation/     # or app/ for Next.js
├── tests/
│   ├── unit/
│   ├── integration/
│   └── e2e/
├── package.json
├── package-lock.json      # or pnpm-lock.yaml / yarn.lock
├── tsconfig.json
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .editorconfig
├── .gitignore
├── .husky/
│   └── pre-commit
├── .github/
│   ├── workflows/ci.yml
│   └── dependabot.yml
├── .semgrepconfig.yml
├── commitlint.config.js
├── LICENSE
└── README.md
```

**React (Next.js + TypeScript):**
```
<project>/
├── src/
│   ├── app/                 # App Router (Next.js 13+)
│   │   ├── layout.tsx
│   │   ├── page.tsx
│   │   ├── api/
│   │   │   └── health/
│   │   │       └── route.ts
│   │   └── (routes)/        # Route groups
│   ├── components/
│   │   ├── ui/              # Reusable UI primitives
│   │   └── features/        # Feature-specific components
│   ├── lib/                 # Shared utilities, API clients
│   ├── hooks/               # Custom React hooks
│   └── types/               # TypeScript type definitions
├── public/
├── tests/
│   ├── unit/
│   ├── integration/
│   └── e2e/                 # Playwright tests
├── next.config.ts
├── package.json
├── package-lock.json
├── tsconfig.json
├── tailwind.config.ts       # If using Tailwind
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .env.local.example
├── .editorconfig
├── .gitignore
├── .husky/
│   └── pre-commit
├── .github/
│   ├── workflows/ci.yml
│   └── dependabot.yml
├── .semgrepconfig.yml
├── commitlint.config.js
├── playwright.config.ts     # E2E test config
├── LICENSE
└── README.md
```

**Android (Compose + Kotlin):**
```
<project>/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/<package>/
│       │   │   ├── domain/
│       │   │   ├── data/
│       │   │   ├── di/
│       │   │   └── ui/
│       │   ├── AndroidManifest.xml
│       │   └── res/
│       ├── test/          # Unit tests
│       └── androidTest/   # Instrumented tests
├── build-logic/           # Convention plugins
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .editorconfig
├── .gitignore
├── .github/
│   ├── workflows/ci.yml
│   └── dependabot.yml
├── LICENSE
└── README.md
```

**Go:**
```
<project>/
├── cmd/<app>/main.go
├── internal/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── handler/
├── pkg/                   # Public libraries (if any)
├── tests/
├── go.mod
├── go.sum
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .editorconfig
├── .gitignore
├── .golangci.yml
├── .github/
│   ├── workflows/ci.yml
│   └── dependabot.yml
├── LICENSE
└── README.md
```

**Rust:**
```
<project>/
├── src/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── main.rs
├── tests/
├── Cargo.toml
├── Cargo.lock
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .editorconfig
├── .gitignore
├── .github/
│   ├── workflows/ci.yml
│   └── dependabot.yml
├── clippy.toml
├── LICENSE
└── README.md
```

### 2.2 Shared Files (All Stacks)

Generate these regardless of stack:

**.editorconfig:**
```ini
root = true

[*]
indent_style = space
indent_size = 2
end_of_line = lf
charset = utf-8
trim_trailing_whitespace = true
insert_final_newline = true

[*.{py,rs}]
indent_size = 4

[*.md]
trim_trailing_whitespace = false

[Makefile]
indent_style = tab
```

**.env.example** — Document every variable with comments:
```bash
# Database
DATABASE_URL=postgresql://user:pass@localhost:5432/dbname  # Connection string (12-Factor III)

# Server
PORT=8000            # Bind port (12-Factor VII)
HOST=0.0.0.0         # Bind address
LOG_LEVEL=info       # Structured log level (12-Factor XI)
LOG_FORMAT=json      # json | text

# External Services
REDIS_URL=redis://localhost:6379  # Cache/queue backing service (12-Factor IV)

# Security
SECRET_KEY=change-me-in-production  # NEVER commit real secrets
```

---

## STEP 3: Linter & Formatter Configuration

Configure the stack-appropriate linter and formatter:

| Stack | Linter | Formatter | Type Checker |
|-------|--------|-----------|-------------|
| Python | ruff (lint) | ruff (format) | mypy / pyright |
| Node/TS | ESLint 9+ (flat config) or Biome | Prettier or Biome | tsc --noEmit |
| Android | ktlint / detekt | ktlint | Kotlin compiler |
| Go | golangci-lint | gofmt / goimports | go vet |
| Rust | clippy | rustfmt | cargo check |

Include the linter config file with sensible defaults. Enable strict mode where available.

---

## STEP 4: Git Hooks & Conventional Commits

### 4.1 Pre-commit Hook

**Python** — Use `.pre-commit-config.yaml`:
```yaml
repos:
  - repo: local
    hooks:
      - id: lint
        name: ruff check
        entry: ruff check --fix
        language: system
        types: [python]
      - id: format
        name: ruff format
        entry: ruff format
        language: system
        types: [python]
      - id: typecheck
        name: mypy
        entry: mypy
        language: system
        types: [python]
        pass_filenames: false
```

**Node/TS** — Use Husky + lint-staged:
```json
// package.json
{
  "lint-staged": {
    "*.{ts,tsx,js,jsx}": ["eslint --fix", "prettier --write"],
    "*.{json,md,yml}": ["prettier --write"]
  }
}
```

### 4.2 Commitlint

Enforce conventional commits:

**Node** — `commitlint.config.js`:
```js
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [2, 'always', [
      'feat', 'fix', 'docs', 'style', 'refactor',
      'perf', 'test', 'build', 'ci', 'chore', 'revert'
    ]],
    'subject-max-length': [2, 'always', 72],
  },
};
```

**Python/Go/Rust** — Use a commit-msg hook script:
```bash
#!/usr/bin/env bash
# .git/hooks/commit-msg or .husky/commit-msg
commit_regex='^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\(.+\))?: .{1,72}$'
if ! grep -qE "$commit_regex" "$1"; then
  echo "ERROR: Commit message must follow Conventional Commits format."
  echo "  Example: feat(auth): add OAuth2 login flow"
  exit 1
fi
```

### 4.3 Semantic Versioning

For libraries or services with releases, set up version management:

**Node** — Add to `package.json`:
```json
{
  "version": "0.1.0",
  "scripts": {
    "release": "semantic-release"
  }
}
```

**Python** — Add to `pyproject.toml`:
```toml
[tool.semantic_release]
version_toml = ["pyproject.toml:project.version"]
branch = "main"
```

---

## STEP 5: CI Pipeline (GitHub Actions)

Generate `.github/workflows/ci.yml` covering:

```yaml
name: CI
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - # Stack-specific lint setup
      - run: <lint command>

  test:
    runs-on: ubuntu-latest
    needs: lint
    services:
      # DB services if needed (postgres, redis)
    steps:
      - uses: actions/checkout@v4
      - # Stack-specific test setup
      - run: <test command>
      - # Upload coverage report

  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Dependency audit
        run: <stack-specific audit command>
      - name: SAST scan
        uses: returntocorp/semgrep-action@v1
        with:
          config: >-
            p/default
            p/owasp-top-ten

  build:
    runs-on: ubuntu-latest
    needs: [lint, test, security]
    steps:
      - uses: actions/checkout@v4
      - run: <build command>
```

Stack-specific audit commands:

| Stack | Audit Command |
|-------|--------------|
| Python | `pip audit` or `safety check` |
| Node | `npm audit --audit-level=high` |
| Go | `govulncheck ./...` |
| Rust | `cargo audit` |
| Android | `./gradlew dependencyCheckAnalyze` (OWASP plugin) |

---

## STEP 6: Security Baseline

### 6.1 Dependabot Configuration

Generate `.github/dependabot.yml`:
```yaml
version: 2
updates:
  - package-ecosystem: "<stack ecosystem>"  # pip, npm, gomod, cargo, gradle
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
    labels:
      - "dependencies"
      - "security"
    reviewers:
      - "<team or user>"

  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
```

### 6.2 .gitignore Audit

Verify these secret patterns are in `.gitignore`:
```
# Secrets & credentials
.env
.env.local
.env.*.local
*.pem
*.key
*.p12
credentials.json
service-account*.json
*-secret.yml

# IDE
.idea/
.vscode/settings.json
*.swp

# OS
.DS_Store
Thumbs.db

# Build artifacts
dist/
build/
*.egg-info/
node_modules/
__pycache__/
target/
```

### 6.3 Semgrep Configuration

Generate `.semgrepconfig.yml` or add to CI:
```yaml
rules:
  - p/default
  - p/owasp-top-ten
  - p/secrets
```

### 6.4 Security Gate

The CI pipeline MUST fail if:
- Any dependency has a CRITICAL or HIGH vulnerability
- Semgrep finds any HIGH-confidence finding
- Secrets are detected in committed files

---

## STEP 7: Docker & Dev Environment

### 7.1 Dockerfile (multi-stage)

```dockerfile
# Build stage
FROM <base-image> AS builder
WORKDIR /app
COPY <manifest-files> .
RUN <install-dependencies>
COPY . .
RUN <build-command>

# Production stage
FROM <slim-base-image>
WORKDIR /app
COPY --from=builder /app/<build-output> .
EXPOSE ${PORT:-8000}
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8000}/health || exit 1
CMD ["<start-command>"]
```

### 7.2 Docker Compose

```yaml
services:
  app:
    build: .
    ports:
      - "${PORT:-8000}:${PORT:-8000}"
    env_file: .env
    depends_on:
      db:
        condition: service_healthy
    volumes:
      - .:/app  # Dev hot-reload

  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ${DB_NAME:-app}
      POSTGRES_USER: ${DB_USER:-postgres}
      POSTGRES_PASSWORD: ${DB_PASS:-postgres}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 3s
      retries: 5

volumes:
  pgdata:
```

Adjust services based on the stack's backing services (Redis, MongoDB, etc.).

---

## STEP 8: Health Endpoint

Create a health check endpoint per stack:

**Python (FastAPI):**
```python
@app.get("/health")
async def health():
    return {"status": "healthy", "version": settings.VERSION}
```

**Node (Express/Next.js API):**
```typescript
app.get('/health', (req, res) => {
  res.json({ status: 'healthy', version: process.env.npm_package_version });
});
```

**Go:**
```go
http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
    json.NewEncoder(w).Encode(map[string]string{"status": "healthy"})
})
```

**Android (Kotlin — for apps with an embedded HTTP server or health check Activity):**
```kotlin
// If using Ktor embedded server or similar:
get("/health") {
    call.respond(mapOf("status" to "healthy", "version" to BuildConfig.VERSION_NAME))
}

// If no HTTP server, create a health-check mechanism via a ContentProvider or WorkManager diagnostic:
class HealthCheckProvider : ContentProvider() {
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        return Bundle().apply { putString("status", "healthy") }
    }
    // ... other required overrides return null/false/0
}
```

**Rust (Actix Web):**
```rust
#[get("/health")]
async fn health() -> impl Responder {
    HttpResponse::Ok().json(serde_json::json!({
        "status": "healthy",
        "version": env!("CARGO_PKG_VERSION")
    }))
}
```

The health endpoint MUST:
- Return HTTP 200 when the service is ready
- Include version information
- NOT require authentication
- Be excluded from request logging (to avoid log noise)

---

## STEP 9: 12-Factor Compliance Check

After generating all files, audit against all 12 factors:

| # | Factor | Check | How |
|---|--------|-------|-----|
| I | Codebase | One repo, many deploys | Verify single git repo with no vendored deps |
| II | Dependencies | Explicitly declared & isolated | Verify manifest file + lockfile exist |
| III | Config | Store in environment | Verify all config reads from env vars, no hardcoded values |
| IV | Backing Services | Treat as attached resources | Verify DB/cache URLs are env vars, not hardcoded hosts |
| V | Build, Release, Run | Strictly separate stages | Verify Dockerfile has separate build and run stages |
| VI | Processes | Stateless & share-nothing | Verify no local file storage for session/state |
| VII | Port Binding | Export services via port | Verify PORT env var used for binding |
| VIII | Concurrency | Scale via process model | Verify no in-process state that prevents horizontal scaling |
| IX | Disposability | Fast startup, graceful shutdown | Verify signal handling (SIGTERM) in entrypoint |
| X | Dev/Prod Parity | Keep gaps small | Verify docker-compose mirrors production topology |
| XI | Logs | Treat as event streams | Verify structured logging to stdout, no file writes |
| XII | Admin Processes | Run as one-off | Verify migrations/seeds run as separate commands, not on startup |

Report any violations and fix them before completing.

---

## STEP 10: License & README

### 10.1 License

Ask the user which license to use. Default to MIT for open source, or add a copyright header for proprietary:

```
Available licenses: MIT, Apache-2.0, GPL-3.0, BSD-3-Clause, proprietary
Which license? [MIT]:
```

### 10.2 README

Generate a minimal README:
```markdown
# <Project Name>

<One-line description from PRD or user input>

## Quick Start

```bash
# Clone and setup
git clone <repo-url>
cd <project>
cp .env.example .env

# Start dev environment
docker compose up -d

# Run tests
<test command>
```

## Development

- **Lint:** `<lint command>`
- **Test:** `<test command>`
- **Build:** `<build command>`

## Architecture

<Brief description of folder structure and layer responsibilities>
```

---

## STEP 11: Verify & Gate

Run the scaffold gate check using the stack-specific commands:

### Stack-Specific Gate Commands

| Check | Python | Node/React | Android | Go | Rust |
|-------|--------|-----------|---------|-----|------|
| Build | `python -m py_compile src/**/*.py` | `npm run build` | `./gradlew assembleDebug` | `go build ./...` | `cargo build` |
| Lint | `ruff check .` | `npx eslint .` | `./gradlew ktlintCheck` | `golangci-lint run` | `cargo clippy -- -D warnings` |
| Test | `pytest tests/ -x` | `npm test` | `./gradlew testDebugUnitTest` | `go test ./...` | `cargo test` |
| Lockfile | `requirements.txt` or `uv.lock` | `package-lock.json` | `gradle.lockfile` (optional) | `go.sum` | `Cargo.lock` |
| Health | `curl localhost:8000/health` | `curl localhost:3000/api/health` | `adb shell am broadcast` (if applicable) | `curl localhost:8080/health` | `curl localhost:8080/health` |

### Gate Check Script

```bash
# 1. Build succeeds
<build command>

# 2. Linter passes
<lint command>

# 3. Tests pass (smoke test at minimum)
<test command>

# 4. Docker builds (skip for Android)
docker build -t <project>:dev .

# 5. .gitignore covers secrets
grep -q ".env" .gitignore && echo "PASS: .env in .gitignore"

# 6. Lockfile exists
test -f <lockfile> && echo "PASS: Lockfile exists"

# 7. Health endpoint responds (skip for Android if no HTTP server)
docker compose up -d && curl -s http://localhost:${PORT:-8000}/health
```

All 7 checks MUST pass before the scaffold is considered complete.

Report results:
```
Scaffold Gate:
  ✅ Build:     PASS
  ✅ Lint:      PASS
  ✅ Tests:     PASS (1 smoke test)
  ✅ Docker:    PASS
  ✅ Secrets:   PASS (.env in .gitignore)
  ✅ Lockfile:  PASS
  ✅ Health:    PASS (HTTP 200)
  ✅ 12-Factor: 12/12 compliant
```

---

## STEP 12: Suggest Next Steps

After scaffold completion, recommend:

- **`/writing-plans`** — Break the feature into implementation tasks
- **`/ci-cd-setup`** — Extend the CI pipeline with deployment stages
- **`/security-audit`** — Run a deeper security assessment
- **Stage 4 (HTML Prototype)** — Build a clickable demo if UI is involved
- **Stage 5 (Schema Design)** — Design the database schema

---

## MUST DO

- Always confirm the stack before generating files
- Always generate .editorconfig, .gitignore, .env.example, and LICENSE
- Always include a security job in CI (dependency audit + SAST)
- Always run the 12-factor compliance check (Step 9)
- Always verify the scaffold with the gate check (Step 11)
- Always set up commitlint or a commit-msg hook for conventional commits
- Always configure Dependabot for automated dependency updates
- Always include a health endpoint

## MUST NOT DO

- MUST NOT generate files without confirming the stack first
- MUST NOT skip the security baseline — every project gets dependency audit + SAST from day 0
- MUST NOT hardcode configuration values — all config MUST come from environment variables
- MUST NOT create a scaffold without a working smoke test
- MUST NOT commit .env files — only .env.example
- MUST NOT skip the 12-factor audit even for simple projects
- MUST NOT begin implementation during this skill — this skill produces a skeleton, not feature code
