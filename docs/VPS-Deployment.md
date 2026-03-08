# VPS Deployment

This project's development VPS is **544934-ABHAYVPS** (Windows Server 2022, IP `103.118.16.189`). All deployment happens from `C:\Apps\`. VPS documentation lives at `C:\Apps\shared\docs\` — **do NOT modify files in `C:\Apps\shared\`**.

| Component | Version |
|-----------|---------|
| Node.js | v24.1.0 |
| PM2 | 6.0.13 |
| Nginx | 1.26.2 |
| PostgreSQL | 16.8 |
| Redis | Port 6379 |

**Architecture:** Internet → Cloudflare (HTTPS) → Nginx (port 80, reverse proxy) → PM2 apps (ports 3001-3004, 8000).

**Key VPS commands (PowerShell):**
```powershell
pm2 ls                                    # List all apps
pm2 logs <app-name> --lines 100           # View logs
pm2 restart <app-name> && pm2 save        # Restart + persist state
cd C:\Apps\nginx && .\nginx.exe -t        # Test Nginx config
cd C:\Apps\nginx && .\nginx.exe -s reload # Reload Nginx (zero-downtime)
netstat -ano | findstr "3001 3002 3003 3004 8000"  # Check ports
```

**VPS documentation index:** `C:\Apps\shared\docs\README.md` — covers setup, PM2, Nginx, Cloudflare, CI/CD, monitoring, troubleshooting.
