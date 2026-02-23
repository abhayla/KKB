# Pre-Production: VPS-Only Actions (Server Setup)

These actions require access to the production VPS. They cover deploying RasoiAI's FastAPI backend alongside existing apps, Nginx configuration, PM2 process management, CI/CD setup, and post-deployment monitoring.

**Target VPS:** 544934-ABHAYVPS (Windows Server 2022, IP `103.118.16.189`)
**Existing apps:** bestdemataccount (3002), firekaro (3003), ipodhan (3001), algochanakya (3004 + 8000)
**Reference:** AlgoChanakya is the closest reference — also a FastAPI/Python backend on port 8000.

**Related:** [Generic-Anywhere-Actions.md](./Generic-Anywhere-Actions.md) — Code/config fixes (do these first).
**Related:** [Production-Deployment-Strategy.md](./Production-Deployment-Strategy.md) — High-level deployment overview.
**VPS docs:** `C:\Apps\shared\docs\README.md` — Full VPS documentation index (DO NOT modify shared docs).

---

## Current VPS Architecture

```
Internet (HTTPS)
       │
  Cloudflare (SSL termination, CDN, WAF)
       │
  Nginx (Port 80, reverse proxy)
       │
  ┌────┼────┬────┬────┬────┐
  │    │    │    │    │    │
 3001 3002 3003 3004 8000 [NEW]
 IPO  BDA  FIRE  AC   AC  RasoiAI
 Dhan      Karo  FE   BE  Backend
```

| Component | Version |
|-----------|---------|
| Node.js | v24.1.0 |
| PM2 | 6.0.13 |
| Nginx | 1.26.2 |
| PostgreSQL | 16.8 |
| Redis | Port 6379 |
| Python | Needs to be installed (3.11+) |

**Key constraint:** AlgoChanakya already uses port 8000. RasoiAI backend needs a different port (e.g., **8001**).

---

## Pre-Deployment Setup (Do Once)

### 1. Choose Port and Domain

| Decision | Value | Notes |
|----------|-------|-------|
| Backend port | `8001` | 8000 is taken by AlgoChanakya |
| Domain | `api.rasoiai.com` | Configure in Cloudflare DNS |
| Directory | `C:\Apps\rasoiai\current\` | Follows existing pattern |

---

### 2. Create Application Directory

Following the established pattern from other apps on this VPS:

```powershell
# Create directory structure (matching other apps)
New-Item -ItemType Directory -Force -Path "C:\Apps\rasoiai\current"
New-Item -ItemType Directory -Force -Path "C:\Apps\rasoiai\current\backend"
New-Item -ItemType Directory -Force -Path "C:\Apps\rasoiai\backups"
New-Item -ItemType Directory -Force -Path "C:\Apps\rasoiai\logs"
```

---

### 3. PostgreSQL Database Setup

Following the pattern from `C:\Apps\shared\docs\setup\DATABASE-QUICK-REFERENCE.md`:

```powershell
# Connect to PostgreSQL (as admin)
psql -U postgres
```

```sql
-- Create database and user
CREATE DATABASE rasoiai;
CREATE USER rasoiai_user WITH PASSWORD 'GENERATE_STRONG_PASSWORD_HERE';
GRANT ALL PRIVILEGES ON DATABASE rasoiai TO rasoiai_user;

-- PostgreSQL 16+ requires explicit schema grant
\c rasoiai
GRANT ALL ON SCHEMA public TO rasoiai_user;

-- Verify
\l   -- List databases (rasoiai should appear)
\du  -- List users (rasoiai_user should appear)
\q
```

**Test connection:**
```powershell
psql -U rasoiai_user -d rasoiai -h localhost
```

**Connection string for `.env`:**
```
DATABASE_URL=postgresql+asyncpg://rasoiai_user:PASSWORD@localhost:5432/rasoiai
```

---

### 4. Python Virtual Environment

```powershell
cd C:\Apps\rasoiai\current\backend

# Create virtual environment
python -m venv venv

# Activate and install dependencies
.\venv\Scripts\activate
pip install -r requirements.txt

# Run migrations
$env:PYTHONPATH = "."
alembic upgrade head

# Seed data
python scripts/seed_festivals.py
python scripts/seed_achievements.py
python scripts/import_recipes_postgres.py
python scripts/sync_config_postgres.py

# Verify
uvicorn app.main:app --host 127.0.0.1 --port 8001
# Should respond at http://localhost:8001/health
```

---

### 5. Production Environment File

Create `C:\Apps\rasoiai\current\backend\.env`:
```bash
# Database (local PostgreSQL)
DATABASE_URL=postgresql+asyncpg://rasoiai_user:STRONG_PASSWORD_HERE@localhost:5432/rasoiai

# Firebase (real credentials, not debug)
FIREBASE_CREDENTIALS_PATH=C:\Apps\rasoiai\current\backend\rasoiai-firebase-service-account.json

# AI API Keys
ANTHROPIC_API_KEY=sk-ant-REAL_KEY_HERE
GOOGLE_AI_API_KEY=REAL_GEMINI_KEY_HERE

# Security
JWT_SECRET_KEY=GENERATE_WITH_openssl_rand_base64_64
DEBUG=false

# Sentry (production monitoring)
SENTRY_DSN=https://REAL_DSN@sentry.io/PROJECT_ID
```

Generate JWT secret:
```powershell
python -c "import secrets; print(secrets.token_urlsafe(64))"
```

**CRITICAL:** `DEBUG=false` in production. When `DEBUG=true`, the backend accepts `fake-firebase-token` — this would let anyone authenticate without a real Firebase account.

---

### 6. PM2 Ecosystem Configuration

Create `C:\Apps\rasoiai\current\ecosystem.config.js`:

```javascript
/**
 * PM2 Ecosystem Configuration for RasoiAI
 *
 * Production deployment configuration
 * Manages FastAPI backend (Python/Uvicorn)
 *
 * Server: Windows Server 2022 VPS (103.118.16.189)
 * Pattern: Same as AlgoChanakya backend
 */

module.exports = {
  apps: [
    // FastAPI Backend
    {
      name: 'rasoiai-backend',
      script: 'venv\\Scripts\\python.exe',
      args: '-m uvicorn app.main:app --host 0.0.0.0 --port 8001',
      cwd: 'C:\\Apps\\rasoiai\\current\\backend',
      instances: 1,
      exec_mode: 'fork',
      interpreter: 'none',  // Important: running python directly, not Node.js

      env: {
        NODE_ENV: 'production',
      },

      max_memory_restart: '500M',
      error_file: 'C:\\Apps\\rasoiai\\logs\\backend-error.log',
      out_file: 'C:\\Apps\\rasoiai\\logs\\backend-out.log',
      log_date_format: 'YYYY-MM-DD HH:mm:ss Z',
      merge_logs: true,

      autorestart: true,
      watch: false,
      max_restarts: 10,
      min_uptime: '10s',
      kill_timeout: 10000,
    },
  ],
};
```

**Key settings (matching AlgoChanakya pattern):**
- `interpreter: 'none'` — tells PM2 not to use Node.js to run the script
- `script: 'venv\\Scripts\\python.exe'` — uses the local venv Python
- `max_memory_restart: '500M'` — auto-restart if memory exceeds limit
- `max_restarts: 10` + `min_uptime: '10s'` — prevents infinite restart loops

**Start and save:**
```powershell
cd C:\Apps\rasoiai\current
pm2 start ecosystem.config.js

# CRITICAL: Save state for auto-start after reboot
pm2 save

# Verify
pm2 list
# Should show: rasoiai-backend | online | fork | port 8001
```

---

### 7. Nginx Reverse Proxy

Create `C:\Apps\nginx\conf\sites\rasoiai.conf` (following the AlgoChanakya pattern):

```nginx
# RasoiAI Site Configuration
# AI-powered meal planning API - FastAPI backend
# Created: [date]

server {
    listen 80;
    server_name api.rasoiai.com;

    # Logging
    access_log logs/rasoiai-access.log;
    error_log logs/rasoiai-error.log;

    # Security headers
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Client body size limit (for photo uploads via /api/v1/photos/)
    client_max_body_size 5M;

    # Health check — no rate limit, no logging
    location /health {
        access_log off;
        proxy_pass http://127.0.0.1:8001;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # API endpoints
    location /api/ {
        proxy_pass http://127.0.0.1:8001/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts — meal generation takes 4-7 seconds, use 90s for safety
        # MUST be < 100s (Cloudflare hard limit)
        proxy_connect_timeout 30s;
        proxy_send_timeout 90s;
        proxy_read_timeout 90s;
    }

    # Block Swagger UI in production (DEBUG=false already disables it, this is belt+suspenders)
    location ~ ^/(docs|redoc|openapi.json) {
        return 404;
    }

    # Block all other paths
    location / {
        return 404;
    }
}
```

**Apply configuration:**
```powershell
# Test config before reload (ALWAYS do this)
cd C:\Apps\nginx
.\nginx.exe -t

# If test passes, reload (zero-downtime)
.\nginx.exe -s reload
```

---

### 8. Cloudflare DNS Setup

1. Log in to Cloudflare dashboard
2. Add DNS record:
   - **Type:** A
   - **Name:** `api.rasoiai` (or `api` if domain is `rasoiai.com`)
   - **Content:** `103.118.16.189`
   - **Proxy status:** Proxied (orange cloud) — enables SSL + CDN
3. SSL/TLS mode: **Flexible** (Cloudflare terminates HTTPS, sends HTTP to Nginx on port 80)

**Verify after DNS propagation:**
```powershell
# From VPS
curl http://localhost:8001/health

# From external (after DNS propagation)
curl https://api.rasoiai.com/health
```

See `C:\Apps\shared\docs\cloudflare\CLOUDFLARE-SETUP.md` for detailed Cloudflare configuration patterns.

---

## Deployment Automation

### 9. GitHub Actions Self-Hosted Runner

Following the pattern from `C:\Apps\shared\docs\cicd\GITHUB-ACTIONS-RUNNER-SETUP.md`:

```powershell
# Create runner directory
New-Item -ItemType Directory -Force -Path "C:\actions-runner-rasoiai"
cd C:\actions-runner-rasoiai

# Download runner (get URL from GitHub repo > Settings > Actions > Runners > New self-hosted runner)
# Follow GitHub's instructions to download and extract

# Configure
.\config.cmd --url https://github.com/abhayla/KKB --token YOUR_TOKEN

# Install as Windows Service (CRITICAL: must use LocalSystem for PM2 access)
.\svc.ps1 install
.\svc.ps1 start

# Verify service is running
Get-Service -Name "actions.runner.abhayla-KKB.*"
```

**CRITICAL:** The runner service MUST use `LocalSystem` account. If it uses a different account, PM2 commands will fail with permission errors because PM2's socket (`C:\ProgramData\pm2\home\rpc.sock`) is owned by LocalSystem.

---

### 10. GitHub Actions Deploy Workflow

Create `.github/workflows/deploy-backend.yml`:

```yaml
name: Deploy RasoiAI Backend

on:
  workflow_dispatch:          # Manual trigger only
    inputs:
      confirm:
        description: 'Type "deploy" to confirm'
        required: true

jobs:
  # Stage 1: Quality checks (GitHub-hosted runner)
  test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-python@v5
        with:
          python-version: '3.11'

      - name: Install dependencies
        run: pip install -r requirements.txt

      - name: Run tests
        run: PYTHONPATH=. pytest --tb=short -q
        env:
          DATABASE_URL: "sqlite+aiosqlite://"

  # Stage 2: Deploy to VPS (self-hosted runner)
  deploy:
    if: github.event.inputs.confirm == 'deploy'
    needs: test
    runs-on: self-hosted    # Runs on VPS
    environment: production  # Requires approval in GitHub settings

    steps:
      - uses: actions/checkout@v4

      - name: Create backup
        shell: powershell
        run: |
          $timestamp = Get-Date -Format "yyyy-MM-dd-HHmmss"
          $backupDir = "C:\Apps\rasoiai\backups\backup-$timestamp"
          Copy-Item -Path "C:\Apps\rasoiai\current\backend" -Destination $backupDir -Recurse -Force
          Write-Host "Backup created: $backupDir"

      - name: Deploy backend
        shell: powershell
        run: |
          # Copy backend files (excluding venv, __pycache__, .env, tests)
          $source = "${{ github.workspace }}\backend"
          $dest = "C:\Apps\rasoiai\current\backend"

          # Sync app code (preserve venv and .env)
          robocopy "$source\app" "$dest\app" /MIR /XD __pycache__
          robocopy "$source\alembic" "$dest\alembic" /MIR /XD __pycache__
          robocopy "$source\config" "$dest\config" /MIR
          Copy-Item "$source\alembic.ini" "$dest\alembic.ini" -Force
          Copy-Item "$source\requirements.txt" "$dest\requirements.txt" -Force

          Write-Host "Backend files deployed"

      - name: Install dependencies & migrate
        shell: powershell
        run: |
          cd C:\Apps\rasoiai\current\backend
          .\venv\Scripts\activate
          pip install -r requirements.txt
          $env:PYTHONPATH = "."
          alembic upgrade head
          Write-Host "Dependencies installed and migrations applied"

      - name: Restart PM2 process
        shell: powershell
        run: |
          # Check if process exists
          $pm2List = pm2 jlist 2>$null | ConvertFrom-Json
          $exists = $pm2List | Where-Object { $_.name -eq "rasoiai-backend" }

          if ($exists) {
            pm2 restart rasoiai-backend --update-env
          } else {
            cd C:\Apps\rasoiai\current
            pm2 start ecosystem.config.js
          }

          # CRITICAL: Save state for auto-start after reboot
          pm2 save
          Write-Host "PM2 process restarted and saved"

      - name: Health check
        shell: powershell
        run: |
          $maxAttempts = 10
          $delay = 3

          for ($i = 1; $i -le $maxAttempts; $i++) {
            try {
              $response = Invoke-WebRequest -Uri "http://localhost:8001/health" -TimeoutSec 5 -UseBasicParsing
              if ($response.StatusCode -eq 200) {
                Write-Host "Health check passed on attempt $i"
                exit 0
              }
            } catch {
              Write-Host "Attempt $i/$maxAttempts failed, waiting ${delay}s..."
              Start-Sleep -Seconds $delay
            }
          }

          Write-Host "ERROR: Health check failed after $maxAttempts attempts"
          exit 1

      - name: Rollback on failure
        if: failure()
        shell: powershell
        run: |
          # Find most recent backup
          $latestBackup = Get-ChildItem "C:\Apps\rasoiai\backups" -Directory |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1

          if ($latestBackup) {
            Write-Host "Rolling back to $($latestBackup.FullName)"
            Copy-Item -Path "$($latestBackup.FullName)\*" -Destination "C:\Apps\rasoiai\current\backend" -Recurse -Force
            pm2 restart rasoiai-backend --update-env
            pm2 save
          } else {
            Write-Host "No backup found for rollback!"
          }

      - name: Clean old backups
        if: success()
        shell: powershell
        run: |
          # Keep only 5 most recent backups
          Get-ChildItem "C:\Apps\rasoiai\backups" -Directory |
            Sort-Object LastWriteTime -Descending |
            Select-Object -Skip 5 |
            ForEach-Object {
              Write-Host "Removing old backup: $($_.Name)"
              Remove-Item $_.FullName -Recurse -Force
            }
```

**Key patterns (matching existing VPS workflows):**
- Two-stage: quality checks on GitHub-hosted Ubuntu, deployment on self-hosted VPS
- Timestamped backup before every deploy
- Auto-rollback if health check fails
- Keeps 5 most recent backups
- `pm2 save` after every PM2 change

---

## Post-Deployment Operations

### 11. Health Monitoring Integration

The VPS already runs `C:\Apps\shared\scripts\health-check.ps1` every 5 minutes via Windows Scheduled Task. It monitors each app by port.

**To include RasoiAI in monitoring, the health check script needs to know about port 8001.** Check if the script auto-discovers PM2 processes or has a hardcoded port list:

```powershell
# View health check script (DO NOT modify — it's in shared/)
Get-Content C:\Apps\shared\scripts\health-check.ps1 | Select-String "8000|port|PM2"

# View recent health check logs
Get-Content C:\Apps\logs\health-check.log -Tail 30
Get-Content C:\Apps\logs\health-alerts.log -Tail 20
```

If the health check uses PM2's process list (`pm2 jlist`), RasoiAI will be automatically included after `pm2 start`. If it uses a hardcoded port list, coordinate with the VPS admin to add port 8001.

---

### 12. Log Management

PM2 handles log rotation for app logs. View logs with:

```powershell
# View RasoiAI logs
pm2 logs rasoiai-backend --lines 100

# View error logs only
pm2 logs rasoiai-backend --err --lines 100

# View log files directly
Get-Content C:\Apps\rasoiai\logs\backend-out.log -Tail 50
Get-Content C:\Apps\rasoiai\logs\backend-error.log -Tail 50

# Flush old logs (if disk space is low)
pm2 flush rasoiai-backend
```

**Nginx logs** for RasoiAI:
```powershell
Get-Content C:\Apps\nginx\logs\rasoiai-access.log -Tail 50
Get-Content C:\Apps\nginx\logs\rasoiai-error.log -Tail 50
```

---

### 13. Database Backups

Create `C:\Apps\rasoiai\scripts\backup_db.ps1`:

```powershell
# RasoiAI Database Backup Script
# Schedule via Windows Task Scheduler for daily backups

$BackupDir = "C:\Apps\rasoiai\backups\db"
$DbName = "rasoiai"
$DbUser = "rasoiai_user"
$RetentionDays = 30
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$BackupFile = "$BackupDir\${DbName}_${Timestamp}.sql"

# Create backup directory
New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null

# Dump database
& "C:\Program Files\PostgreSQL\16\bin\pg_dump.exe" -U $DbUser -d $DbName -f $BackupFile

# Verify backup is non-empty
if ((Get-Item $BackupFile).Length -eq 0) {
    Write-Host "ERROR: Backup file is empty!"
    Remove-Item $BackupFile
    exit 1
}

$Size = (Get-Item $BackupFile).Length / 1MB
Write-Host "Backup created: $BackupFile ($([math]::Round($Size, 2)) MB)"

# Delete backups older than retention period
Get-ChildItem "$BackupDir\${DbName}_*.sql" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } |
    ForEach-Object {
        Write-Host "Removing old backup: $($_.Name)"
        Remove-Item $_.FullName
    }
```

**Schedule daily backup via Windows Task Scheduler:**
```powershell
# Create scheduled task for daily backup at 2 AM
$Action = New-ScheduledTaskAction -Execute "PowerShell.exe" `
    -Argument "-ExecutionPolicy Bypass -File C:\Apps\rasoiai\scripts\backup_db.ps1"
$Trigger = New-ScheduledTaskTrigger -Daily -At "02:00"
$Principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -RunLevel Highest
Register-ScheduledTask -TaskName "RasoiAI-DBBackup" -Action $Action -Trigger $Trigger -Principal $Principal
```

**Restore from backup:**
```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U rasoiai_user -d rasoiai -f C:\Apps\rasoiai\backups\db\rasoiai_20260222_020000.sql
```

---

### 14. Operational Quick Reference

```powershell
# ── Status ──
pm2 list                                    # All apps status
pm2 describe rasoiai-backend                # Detailed process info
pm2 monit                                   # Real-time CPU/memory monitor
netstat -ano | findstr 8001                 # Verify port is listening

# ── Restart/Stop ──
pm2 restart rasoiai-backend                 # Restart app
pm2 stop rasoiai-backend                    # Stop app
pm2 delete rasoiai-backend                  # Remove from PM2
pm2 save                                    # ALWAYS run after PM2 changes

# ── Logs ──
pm2 logs rasoiai-backend --lines 100        # View recent logs
pm2 flush rasoiai-backend                   # Clear log files

# ── Nginx ──
cd C:\Apps\nginx
.\nginx.exe -t                              # Test config (always before reload!)
.\nginx.exe -s reload                       # Apply config changes

# ── Database ──
psql -U rasoiai_user -d rasoiai -h localhost # Connect to DB

# ── Python/Backend ──
cd C:\Apps\rasoiai\current\backend
.\venv\Scripts\activate
$env:PYTHONPATH = "."
alembic upgrade head                        # Run migrations
python scripts/seed_festivals.py            # Seed data

# ── Troubleshooting ──
Get-Content C:\Apps\rasoiai\logs\backend-error.log -Tail 50
Get-Content C:\Apps\nginx\logs\rasoiai-error.log -Tail 50
Get-Content C:\Apps\logs\health-alerts.log -Tail 20
```

---

## Android App Configuration

The Android app needs to point to the production backend instead of the emulator's `10.0.2.2:8000`.

**Build flavor approach** (in `android/app/build.gradle.kts`):
```kotlin
android {
    buildTypes {
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.rasoiai.com\"")
        }
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000\"")
        }
    }
}
```

**Firebase:** Production needs a separate Firebase project (or at minimum, the debug auth bypass must be disabled via `DEBUG=false`).

---

## Summary Checklist

| # | Phase | Item | Est. Time | Reference |
|---|-------|------|-----------|-----------|
| 1 | Planning | Choose port (8001) and domain | 5 min | — |
| 2 | Setup | Create directory structure | 5 min | AlgoChanakya pattern |
| 3 | Setup | PostgreSQL database + user | 10 min | `shared/docs/setup/DATABASE-QUICK-REFERENCE.md` |
| 4 | Setup | Python venv + dependencies + migrations + seed data | 20 min | — |
| 5 | Setup | Production `.env` file (DEBUG=false) | 10 min | — |
| 6 | Setup | PM2 ecosystem config + start + save | 10 min | AlgoChanakya ecosystem.config.js |
| 7 | Setup | Nginx site config + test + reload | 15 min | `shared/docs/nginx/NGINX-SITE-TEMPLATES.md` |
| 8 | Setup | Cloudflare DNS A record | 10 min | `shared/docs/cloudflare/CLOUDFLARE-SETUP.md` |
| 9 | CI/CD | GitHub Actions self-hosted runner | 20 min | `shared/docs/cicd/GITHUB-ACTIONS-RUNNER-SETUP.md` |
| 10 | CI/CD | Deploy workflow (test → backup → deploy → health check → rollback) | 30 min | Other app workflows |
| 11 | Post-Deploy | Verify health monitoring includes port 8001 | 10 min | `shared/docs/monitoring/HEALTH-MONITORING-SYSTEM.md` |
| 12 | Post-Deploy | Log management (PM2 + Nginx) | 5 min | — |
| 13 | Post-Deploy | Database backup script + scheduled task | 15 min | — |
| 14 | Post-Deploy | Verify operational commands work | 10 min | — |

**Recommended order:**
1. Items 1-5 (database + backend running locally on VPS) — validate it works
2. Items 6-7 (PM2 + Nginx) — app accessible via reverse proxy
3. Item 8 (Cloudflare) — app accessible via HTTPS domain
4. Items 9-10 (CI/CD) — automated deployments
5. Items 11-13 (monitoring + backups) — production readiness

**Total estimated time:** ~3 hours for complete VPS setup

---

*Created: February 2026*
*Updated: February 2026 — Rewritten for actual VPS (Windows Server 2022 + PM2 + Nginx + Cloudflare)*
*Companion doc: [Generic-Anywhere-Actions.md](./Generic-Anywhere-Actions.md)*
