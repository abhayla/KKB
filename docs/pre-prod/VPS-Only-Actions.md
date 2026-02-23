# Pre-Production: VPS-Only Actions (Server Setup)

These actions require SSH access to your production VPS. They cover server hardening, deployment automation, and post-deployment monitoring.

**Assumes:** Ubuntu 22.04+ VPS in India region (e.g., DigitalOcean BLR1, Hetzner Asia, AWS Mumbai).

**Related:** [Generic-Anywhere-Actions.md](./Generic-Anywhere-Actions.md) — Code/config fixes (do these first).
**Related:** [Production-Deployment-Strategy.md](./Production-Deployment-Strategy.md) — High-level deployment overview.

---

## Pre-Deployment Setup (Do Once)

### 1. Create Application User

**Why:** Never run your app as root. Create a dedicated non-root user with limited permissions.

```bash
# Run as root
adduser rasoiai --disabled-password --gecos ""
usermod -aG sudo rasoiai

# Set up SSH key access for the rasoiai user
mkdir -p /home/rasoiai/.ssh
cp ~/.ssh/authorized_keys /home/rasoiai/.ssh/
chown -R rasoiai:rasoiai /home/rasoiai/.ssh
chmod 700 /home/rasoiai/.ssh
chmod 600 /home/rasoiai/.ssh/authorized_keys

# Verify
su - rasoiai
whoami  # Should print: rasoiai
```

---

### 2. SSH Hardening

**Why:** Default SSH config accepts password auth and root login — easy targets for brute force.

Edit `/etc/ssh/sshd_config`:
```bash
sudo nano /etc/ssh/sshd_config
```

```ini
# Change these settings:
Port 2222                          # Non-default port (pick your own)
PermitRootLogin no                 # Block root SSH
PasswordAuthentication no          # Key-only auth
PubkeyAuthentication yes
MaxAuthTries 3
LoginGraceTime 20
AllowUsers rasoiai                 # Only allow app user

# Keep these at default:
UsePAM yes
X11Forwarding no
```

```bash
# Test config before restarting (avoid lockout!)
sudo sshd -t
sudo systemctl restart sshd

# IMPORTANT: Test new SSH connection in a SEPARATE terminal before closing current session
ssh -p 2222 rasoiai@your-server-ip
```

---

### 3. UFW Firewall

**Why:** Only expose ports that are needed. Block everything else.

```bash
# Reset to default (deny all incoming)
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow SSH (use your custom port from step 2)
sudo ufw allow 2222/tcp comment 'SSH'

# Allow HTTP/HTTPS (Nginx will proxy to app)
sudo ufw allow 80/tcp comment 'HTTP'
sudo ufw allow 443/tcp comment 'HTTPS'

# PostgreSQL — only from localhost (no external access)
# Do NOT allow 5432 from outside

# Enable firewall
sudo ufw enable
sudo ufw status verbose
```

Expected output:
```
Status: active

To                         Action      From
--                         ------      ----
2222/tcp                   ALLOW       Anywhere        # SSH
80/tcp                     ALLOW       Anywhere        # HTTP
443/tcp                    ALLOW       Anywhere        # HTTPS
```

---

### 4. Fail2Ban

**Why:** Automatically bans IPs that fail SSH login repeatedly.

```bash
sudo apt install fail2ban -y
```

Create `/etc/fail2ban/jail.local`:
```ini
[DEFAULT]
bantime = 3600        # 1 hour ban
findtime = 600        # 10 minute window
maxretry = 3          # 3 failures = ban
banaction = ufw       # Use UFW for banning

[sshd]
enabled = true
port = 2222           # Match your SSH port
logpath = /var/log/auth.log
maxretry = 3
```

```bash
sudo systemctl enable fail2ban
sudo systemctl start fail2ban
sudo fail2ban-client status sshd
```

---

### 5. Nginx Reverse Proxy

**Why:** Nginx handles TLS termination, static files, rate limiting, and proxies to the FastAPI app.

```bash
sudo apt install nginx -y
```

Create `/etc/nginx/sites-available/rasoiai`:
```nginx
# Rate limiting zones
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=auth:10m rate=3r/s;
limit_req_zone $binary_remote_addr zone=ai:10m rate=1r/s;

# Upstream app server
upstream rasoiai_backend {
    server 127.0.0.1:8000;
    keepalive 32;
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name api.rasoiai.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS server
server {
    listen 443 ssl http2;
    server_name api.rasoiai.com;

    # SSL certificates (Let's Encrypt — see step 6)
    ssl_certificate /etc/letsencrypt/live/api.rasoiai.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.rasoiai.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_stapling on;
    ssl_stapling_verify on;

    # Security headers
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Client body size limit (for photo uploads)
    client_max_body_size 5M;

    # Request timeouts
    proxy_connect_timeout 10s;
    proxy_send_timeout 30s;
    proxy_read_timeout 120s;   # Long timeout for AI generation

    # Health check — no rate limit
    location /health {
        proxy_pass http://rasoiai_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Auth endpoints — strict rate limit
    location /api/v1/auth/ {
        limit_req zone=auth burst=5 nodelay;
        proxy_pass http://rasoiai_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # AI endpoints (meal gen, chat, photo) — tight rate limit
    location ~ ^/api/v1/(meal-plans/generate|chat/message|photos/) {
        limit_req zone=ai burst=3 nodelay;
        proxy_pass http://rasoiai_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # All other API endpoints — standard rate limit
    location /api/ {
        limit_req zone=api burst=20 nodelay;
        proxy_pass http://rasoiai_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Block Swagger UI in production (belt + suspenders with FastAPI's own check)
    location ~ ^/(docs|redoc|openapi.json) {
        return 404;
    }

    # Block all other paths
    location / {
        return 404;
    }
}
```

```bash
# Enable the site
sudo ln -s /etc/nginx/sites-available/rasoiai /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/default  # Remove default site

# Test and reload
sudo nginx -t
sudo systemctl reload nginx
```

---

### 6. SSL/TLS with Let's Encrypt

**Why:** Free, auto-renewing HTTPS certificates.

```bash
sudo apt install certbot python3-certbot-nginx -y

# Obtain certificate (Nginx plugin auto-configures)
sudo certbot --nginx -d api.rasoiai.com --non-interactive --agree-tos -m admin@rasoiai.com

# Verify auto-renewal
sudo certbot renew --dry-run

# Renewal runs automatically via systemd timer
sudo systemctl list-timers | grep certbot
```

---

### 7. PostgreSQL Production Configuration

**Why:** Default PostgreSQL settings are tuned for development, not production workloads.

```bash
sudo apt install postgresql postgresql-contrib -y
```

Create database and user:
```sql
-- Run as postgres superuser
sudo -u postgres psql

CREATE DATABASE rasoiai_prod;
CREATE USER rasoiai_app WITH ENCRYPTED PASSWORD 'generate-a-strong-password-here';

-- Minimal privileges
GRANT CONNECT ON DATABASE rasoiai_prod TO rasoiai_app;
GRANT USAGE ON SCHEMA public TO rasoiai_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO rasoiai_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO rasoiai_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rasoiai_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO rasoiai_app;

\q
```

Edit `/etc/postgresql/16/main/postgresql.conf` (adjust version number as needed):
```ini
# Connection settings
listen_addresses = 'localhost'         # Only local connections
max_connections = 50                   # Match app pool_size + max_overflow
port = 5432

# Memory (tune for your VPS RAM — example for 2GB VPS)
shared_buffers = 512MB                 # 25% of RAM
effective_cache_size = 1536MB          # 75% of RAM
work_mem = 10MB                        # Per-operation sort/hash memory
maintenance_work_mem = 128MB           # For VACUUM, CREATE INDEX

# Write performance
wal_buffers = 16MB
checkpoint_completion_target = 0.9
synchronous_commit = on                # Data safety first

# Query planner
random_page_cost = 1.1                 # For SSD storage
effective_io_concurrency = 200         # For SSD

# Logging
log_min_duration_statement = 500       # Log queries > 500ms
log_statement = 'none'                 # Don't log all queries
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a '
```

Edit `/etc/postgresql/16/main/pg_hba.conf`:
```
# Only allow local connections with password
local   all   all                 peer
host    all   all   127.0.0.1/32  scram-sha-256
host    all   all   ::1/128       scram-sha-256
# Do NOT add any 0.0.0.0/0 entries
```

```bash
sudo systemctl restart postgresql
```

---

### 8. systemd Service

**Why:** Runs the FastAPI app as a managed service with auto-restart, logging, and resource limits.

Create `/etc/systemd/system/rasoiai.service`:
```ini
[Unit]
Description=RasoiAI FastAPI Backend
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=exec
User=rasoiai
Group=rasoiai
WorkingDirectory=/home/rasoiai/app/backend
EnvironmentFile=/home/rasoiai/app/backend/.env

ExecStart=/home/rasoiai/app/backend/venv/bin/uvicorn \
    app.main:app \
    --host 127.0.0.1 \
    --port 8000 \
    --workers 2 \
    --limit-concurrency 100 \
    --timeout-keep-alive 5

Restart=always
RestartSec=5

# Resource limits
MemoryMax=1G
CPUQuota=80%

# Security hardening
NoNewPrivileges=yes
PrivateTmp=yes
ProtectSystem=strict
ProtectHome=read-only
ReadWritePaths=/home/rasoiai/app/backend/logs

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=rasoiai

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable rasoiai
sudo systemctl start rasoiai
sudo systemctl status rasoiai

# View logs
sudo journalctl -u rasoiai -f --no-pager
```

---

## Deployment Automation

### 9. Dockerfile

Create `backend/Dockerfile`:
```dockerfile
FROM python:3.11-slim AS base

# Security: don't run as root
RUN groupadd -r rasoiai && useradd -r -g rasoiai rasoiai

WORKDIR /app

# Install system dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends gcc libpq-dev && \
    rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app/ app/
COPY alembic/ alembic/
COPY alembic.ini .
COPY config/ config/

# Switch to non-root user
USER rasoiai

# Health check
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')"

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]
```

Create `backend/.dockerignore`:
```
venv/
__pycache__/
*.pyc
.env
tests/
scripts/
*.md
.git/
```

**Build & test locally:**
```bash
cd backend
docker build -t rasoiai-backend:latest .
docker run -p 8000:8000 --env-file .env rasoiai-backend:latest
curl http://localhost:8000/health
```

---

### 10. Docker Compose (Full Stack)

Create `docker-compose.prod.yml` in project root:
```yaml
version: '3.8'

services:
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: rasoiai-backend
    restart: always
    ports:
      - "127.0.0.1:8000:8000"    # Only exposed to localhost (Nginx proxies)
    env_file:
      - ./backend/.env.prod
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "python", "-c", "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')"]
      interval: 30s
      timeout: 5s
      retries: 3
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '1.0'
    networks:
      - rasoiai-net

  db:
    image: postgres:16-alpine
    container_name: rasoiai-db
    restart: always
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backups:/backups                       # Mount backup directory
    environment:
      POSTGRES_DB: rasoiai_prod
      POSTGRES_USER: rasoiai_app
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
    secrets:
      - db_password
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U rasoiai_app -d rasoiai_prod"]
      interval: 10s
      timeout: 5s
      retries: 5
    ports:
      - "127.0.0.1:5432:5432"    # Only local access
    networks:
      - rasoiai-net

  # Migration runner (runs once, then exits)
  migrate:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: rasoiai-migrate
    env_file:
      - ./backend/.env.prod
    command: ["python", "-m", "alembic", "upgrade", "head"]
    depends_on:
      db:
        condition: service_healthy
    networks:
      - rasoiai-net
    restart: "no"

volumes:
  postgres_data:
    driver: local

secrets:
  db_password:
    file: ./secrets/db_password.txt

networks:
  rasoiai-net:
    driver: bridge
```

**Deploy:**
```bash
# Create secrets directory
mkdir -p secrets
echo "your-strong-db-password" > secrets/db_password.txt
chmod 600 secrets/db_password.txt

# Start everything
docker compose -f docker-compose.prod.yml up -d

# Run migrations
docker compose -f docker-compose.prod.yml run --rm migrate

# Check status
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
```

---

### 11. GitHub Actions Deploy Workflow

Create `.github/workflows/deploy.yml`:
```yaml
name: Deploy to Production

on:
  workflow_dispatch:          # Manual trigger only
    inputs:
      confirm:
        description: 'Type "deploy" to confirm'
        required: true

jobs:
  deploy:
    if: github.event.inputs.confirm == 'deploy'
    runs-on: ubuntu-latest
    environment: production   # Requires approval in GitHub settings

    steps:
      - uses: actions/checkout@v4

      - name: Run backend tests
        run: |
          cd backend
          pip install -r requirements.txt
          PYTHONPATH=. pytest --tb=short -q

      - name: Deploy to VPS
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.VPS_HOST }}
          username: rasoiai
          key: ${{ secrets.VPS_SSH_KEY }}
          port: 2222
          script: |
            cd /home/rasoiai/app
            git pull origin main

            # Backend
            cd backend
            source venv/bin/activate
            pip install -r requirements.txt
            alembic upgrade head

            # Restart service
            sudo systemctl restart rasoiai

            # Wait and verify
            sleep 5
            curl -sf http://localhost:8000/health || exit 1
            echo "Deploy successful!"
```

**Required GitHub Secrets:**
- `VPS_HOST` — Your server IP
- `VPS_SSH_KEY` — SSH private key for `rasoiai` user

---

### 12. Blue-Green Deployment (Zero Downtime)

**Why:** Swap between two app instances so users never see downtime during deploys.

Create `/home/rasoiai/deploy.sh`:
```bash
#!/bin/bash
set -euo pipefail

APP_DIR="/home/rasoiai/app"
BLUE_PORT=8000
GREEN_PORT=8001

# Determine which environment is currently live
CURRENT_PORT=$(grep -oP 'server 127.0.0.1:\K\d+' /etc/nginx/sites-available/rasoiai | head -1)

if [ "$CURRENT_PORT" = "$BLUE_PORT" ]; then
    DEPLOY_PORT=$GREEN_PORT
    DEPLOY_NAME="green"
else
    DEPLOY_PORT=$BLUE_PORT
    DEPLOY_NAME="blue"
fi

echo "Deploying to $DEPLOY_NAME (port $DEPLOY_PORT)..."

# Pull latest code
cd "$APP_DIR"
git pull origin main

# Start new version on deploy port
cd backend
source venv/bin/activate
pip install -r requirements.txt
alembic upgrade head

# Start new instance
uvicorn app.main:app --host 127.0.0.1 --port $DEPLOY_PORT --workers 2 &
NEW_PID=$!

# Wait for health check
echo "Waiting for $DEPLOY_NAME to be healthy..."
for i in $(seq 1 30); do
    if curl -sf "http://127.0.0.1:$DEPLOY_PORT/health" > /dev/null 2>&1; then
        echo "$DEPLOY_NAME is healthy!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "FAILED: $DEPLOY_NAME didn't become healthy. Rolling back."
        kill $NEW_PID 2>/dev/null || true
        exit 1
    fi
    sleep 1
done

# Switch Nginx to new port
sudo sed -i "s/server 127.0.0.1:$CURRENT_PORT/server 127.0.0.1:$DEPLOY_PORT/" \
    /etc/nginx/sites-available/rasoiai
sudo nginx -t && sudo systemctl reload nginx

# Stop old instance
OLD_PID=$(lsof -ti:$CURRENT_PORT 2>/dev/null || true)
if [ -n "$OLD_PID" ]; then
    kill $OLD_PID
fi

echo "Deploy complete! $DEPLOY_NAME is now live on port $DEPLOY_PORT."
```

```bash
chmod +x /home/rasoiai/deploy.sh
```

---

## Post-Deployment Operations

### 13. PostgreSQL Automated Backups

**Why:** Daily backups with 30-day retention. Essential for disaster recovery.

Create `/home/rasoiai/scripts/backup_db.sh`:
```bash
#!/bin/bash
set -euo pipefail

BACKUP_DIR="/home/rasoiai/backups"
DB_NAME="rasoiai_prod"
DB_USER="rasoiai_app"
RETENTION_DAYS=30
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_${TIMESTAMP}.sql.gz"

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Dump and compress
pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_FILE"

# Verify backup is non-empty
if [ ! -s "$BACKUP_FILE" ]; then
    echo "ERROR: Backup file is empty!"
    rm -f "$BACKUP_FILE"
    exit 1
fi

BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "Backup created: $BACKUP_FILE ($BACKUP_SIZE)"

# Delete backups older than retention period
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime +$RETENTION_DAYS -delete
echo "Cleaned up backups older than $RETENTION_DAYS days"

# Optional: Copy to remote storage
# aws s3 cp "$BACKUP_FILE" "s3://rasoiai-backups/db/"
# rclone copy "$BACKUP_FILE" "remote:rasoiai-backups/db/"
```

Add to crontab:
```bash
chmod +x /home/rasoiai/scripts/backup_db.sh

# Edit crontab for rasoiai user
crontab -e
```

```cron
# Daily database backup at 2 AM IST
0 2 * * * /home/rasoiai/scripts/backup_db.sh >> /home/rasoiai/logs/backup.log 2>&1

# Weekly full backup on Sunday at 3 AM IST
0 3 * * 0 /home/rasoiai/scripts/backup_db.sh >> /home/rasoiai/logs/backup.log 2>&1
```

**Restore from backup:**
```bash
gunzip -c /home/rasoiai/backups/rasoiai_prod_20260222_020000.sql.gz | psql -U rasoiai_app rasoiai_prod
```

---

### 14. Log Rotation

**Why:** Logs grow unbounded without rotation. Disk fills up, app crashes.

Create `/etc/logrotate.d/rasoiai`:
```
/home/rasoiai/logs/*.log {
    daily
    missingok
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 rasoiai rasoiai
    dateext
    dateformat -%Y%m%d
}
```

For journald (systemd service logs), edit `/etc/systemd/journald.conf`:
```ini
[Journal]
SystemMaxUse=500M        # Max disk usage for all journals
SystemMaxFileSize=50M    # Max size per journal file
MaxRetentionSec=30day    # Keep 30 days of logs
```

```bash
sudo systemctl restart systemd-journald
```

For Nginx logs, verify `/etc/logrotate.d/nginx` exists (installed by default).

---

### 15. Automatic Security Updates

**Why:** Unattended security patches for OS packages.

```bash
sudo apt install unattended-upgrades -y
sudo dpkg-reconfigure -plow unattended-upgrades
```

Edit `/etc/apt/apt.conf.d/50unattended-upgrades`:
```
Unattended-Upgrade::Allowed-Origins {
    "${distro_id}:${distro_codename}";
    "${distro_id}:${distro_codename}-security";
};

// Email notification (optional)
Unattended-Upgrade::Mail "admin@rasoiai.com";
Unattended-Upgrade::MailReport "on-change";

// Auto-reboot if needed (at 4 AM)
Unattended-Upgrade::Automatic-Reboot "true";
Unattended-Upgrade::Automatic-Reboot-Time "04:00";

// Remove unused dependencies
Unattended-Upgrade::Remove-Unused-Dependencies "true";
```

**Verification:**
```bash
sudo unattended-upgrade --dry-run --debug
```

---

### 16. Health Monitoring & Alerting

**Why:** Know when the app is down before users report it.

#### Option A: Simple Script + Cron (Free)

Create `/home/rasoiai/scripts/health_check.sh`:
```bash
#!/bin/bash

API_URL="http://127.0.0.1:8000/health"
ALERT_EMAIL="admin@rasoiai.com"
LOG_FILE="/home/rasoiai/logs/health.log"
STATE_FILE="/tmp/rasoiai_health_state"

TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# Check API health
HTTP_CODE=$(curl -sf -o /dev/null -w "%{http_code}" --max-time 5 "$API_URL" 2>/dev/null || echo "000")

if [ "$HTTP_CODE" = "200" ]; then
    echo "$TIMESTAMP OK (HTTP $HTTP_CODE)" >> "$LOG_FILE"
    # If previously down, send recovery notification
    if [ -f "$STATE_FILE" ]; then
        echo "RasoiAI API recovered at $TIMESTAMP" | \
            mail -s "[RECOVERED] RasoiAI API" "$ALERT_EMAIL" 2>/dev/null || true
        rm -f "$STATE_FILE"
    fi
else
    echo "$TIMESTAMP FAIL (HTTP $HTTP_CODE)" >> "$LOG_FILE"
    # Only alert on first failure (avoid alert spam)
    if [ ! -f "$STATE_FILE" ]; then
        echo "RasoiAI API is DOWN. HTTP code: $HTTP_CODE. Timestamp: $TIMESTAMP" | \
            mail -s "[DOWN] RasoiAI API" "$ALERT_EMAIL" 2>/dev/null || true
        touch "$STATE_FILE"
    fi

    # Auto-restart if down
    echo "$TIMESTAMP Attempting restart..." >> "$LOG_FILE"
    sudo systemctl restart rasoiai
fi
```

```bash
chmod +x /home/rasoiai/scripts/health_check.sh

# Run every 2 minutes
crontab -e
```

```cron
*/2 * * * * /home/rasoiai/scripts/health_check.sh
```

#### Option B: UptimeRobot (Free Tier)

1. Sign up at [uptimerobot.com](https://uptimerobot.com)
2. Add monitor: `https://api.rasoiai.com/health`
3. Check interval: 5 minutes
4. Alert contacts: email + Telegram/Slack

#### Disk & Memory Monitoring

Create `/home/rasoiai/scripts/system_check.sh`:
```bash
#!/bin/bash

LOG_FILE="/home/rasoiai/logs/system.log"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# Check disk usage
DISK_USAGE=$(df / | tail -1 | awk '{print $5}' | tr -d '%')
if [ "$DISK_USAGE" -gt 85 ]; then
    echo "$TIMESTAMP WARNING: Disk usage at ${DISK_USAGE}%" >> "$LOG_FILE"
fi

# Check memory
MEM_AVAILABLE=$(free -m | awk '/^Mem:/{print $7}')
if [ "$MEM_AVAILABLE" -lt 256 ]; then
    echo "$TIMESTAMP WARNING: Only ${MEM_AVAILABLE}MB memory available" >> "$LOG_FILE"
fi

# Check PostgreSQL
if ! pg_isready -q; then
    echo "$TIMESTAMP ERROR: PostgreSQL is not responding" >> "$LOG_FILE"
fi

# Check Nginx
if ! systemctl is-active --quiet nginx; then
    echo "$TIMESTAMP ERROR: Nginx is not running" >> "$LOG_FILE"
fi
```

```cron
*/5 * * * * /home/rasoiai/scripts/system_check.sh
```

---

## Production Environment File

Create `/home/rasoiai/app/backend/.env.prod`:
```bash
# Database (local PostgreSQL)
DATABASE_URL=postgresql+asyncpg://rasoiai_app:STRONG_PASSWORD_HERE@localhost:5432/rasoiai_prod

# Firebase (real credentials, not debug)
FIREBASE_CREDENTIALS_PATH=/home/rasoiai/app/backend/rasoiai-firebase-service-account.json

# AI API Keys
ANTHROPIC_API_KEY=sk-ant-REAL_KEY_HERE
GOOGLE_AI_API_KEY=REAL_GEMINI_KEY_HERE

# Security
JWT_SECRET_KEY=GENERATE_WITH_openssl_rand_base64_64
DEBUG=false

# Sentry (production)
SENTRY_DSN=https://REAL_DSN@sentry.io/PROJECT_ID
```

Generate JWT secret:
```bash
openssl rand -base64 64
```

Set restrictive permissions:
```bash
chmod 600 /home/rasoiai/app/backend/.env.prod
chown rasoiai:rasoiai /home/rasoiai/app/backend/.env.prod
```

---

## Summary Checklist

| # | Phase | Item | Est. Time |
|---|-------|------|-----------|
| 1 | Pre-Deploy | App user creation | 5 min |
| 2 | Pre-Deploy | SSH hardening | 15 min |
| 3 | Pre-Deploy | UFW firewall | 10 min |
| 4 | Pre-Deploy | Fail2Ban | 10 min |
| 5 | Pre-Deploy | Nginx reverse proxy | 30 min |
| 6 | Pre-Deploy | SSL/TLS (Let's Encrypt) | 10 min |
| 7 | Pre-Deploy | PostgreSQL prod config | 20 min |
| 8 | Pre-Deploy | systemd service | 15 min |
| 9 | Automation | Dockerfile | 15 min |
| 10 | Automation | Docker Compose | 20 min |
| 11 | Automation | GitHub Actions deploy | 20 min |
| 12 | Automation | Blue-green deploy | 30 min |
| 13 | Post-Deploy | DB backups (cron) | 15 min |
| 14 | Post-Deploy | Log rotation | 10 min |
| 15 | Post-Deploy | Security updates | 10 min |
| 16 | Post-Deploy | Health monitoring | 20 min |

**Recommended order:**
1. Items 1-4 (server hardening) — do immediately after VPS provisioning
2. Items 5-8 (web server + app service) — minimum viable deployment
3. Items 13-16 (backups, monitoring) — do within first day
4. Items 9-12 (Docker, CI/CD) — automate after manual deploy is working

**Total estimated time:** ~4-5 hours for a complete VPS setup

---

*Created: February 2026*
*Companion doc: [Generic-Anywhere-Actions.md](./Generic-Anywhere-Actions.md)*
