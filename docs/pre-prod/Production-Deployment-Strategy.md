# RasoiAI Production Deployment Strategy

Based on the current architecture (Android + FastAPI + PostgreSQL + Firebase + Gemini/Claude APIs), here's a production plan customized for this app.

---

## Phase 1: Security Hardening (Week 1-2)

### Backend

| Area | Current State | Production Action |
|------|--------------|-------------------|
| `DEBUG=true` | Fake Firebase token accepted | **Remove debug auth bypass** in `firebase.py`. Only real Firebase tokens in prod. |
| CORS `["*"]` in `config.py` | Open to all origins | Restrict to your domain + `localhost` for dev |
| `JWT_SECRET_KEY` | Likely short/simple | Generate 256-bit random key, rotate quarterly |
| `.env` secrets | Flat file | Use a secrets manager (GCP Secret Manager / AWS SSM) |
| API rate limiting | None | Add `slowapi` or similar — especially on `/chat`, `/meal-plans/generate` (AI calls are expensive) |
| Input validation | Pydantic models | Audit all endpoints for injection vectors, especially recipe search and chat |

### Android

| Area | Action |
|------|--------|
| `WEB_CLIENT_ID` | Already externalized via `local.properties` / env — good |
| API base URL | Move from hardcoded `10.0.2.2:8000` to a configurable `BuildConfig.API_BASE_URL` with separate debug/release flavors |
| Certificate pinning | Add for your production API domain |
| ProGuard/R8 | Enable for release builds — obfuscate code, shrink APK |
| `google-services.json` | Separate debug vs release Firebase projects |

### Critical: Firebase Auth Production

```
1. Create SEPARATE Firebase project for production
2. Enable Google Sign-In with production SHA-1 fingerprint
3. Configure OAuth consent screen (Google Cloud Console)
4. Add your production domain to authorized domains
5. Generate production google-services.json → android/app/src/release/
```

---

## Phase 2: Infrastructure Setup (Week 2-3)

### Recommended Stack (India-optimized)

| Component | Recommendation | Why |
|-----------|---------------|-----|
| **Backend hosting** | GCP Cloud Run (Mumbai `asia-south1`) | Auto-scaling, pay-per-request, lowest latency for India |
| **Database** | Cloud SQL PostgreSQL (Mumbai) | Managed, automated backups, replicas |
| **CDN/Static** | Cloudflare | Free tier covers recipe images, global edge |
| **Monitoring** | Sentry (already configured) + GCP Cloud Monitoring | Already have `SENTRY_DSN` in config |
| **CI/CD** | GitHub Actions (already set up) | Extend existing `android-ci.yml` and `backend-ci.yml` |

### Why GCP over AWS for this app

- Firebase is GCP-native (auth, analytics, crashlytics — zero-latency integration)
- Mumbai region (`asia-south1`) has excellent coverage for Tier 2/3 Indian cities
- Cloud Run handles async FastAPI naturally
- Gemini API is Google's — lower latency from GCP

### Backend Deployment Architecture

```
                    ┌─────────────────┐
                    │   Cloudflare    │
                    │   (CDN + WAF)   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  GCP Load       │
                    │  Balancer       │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        ┌───────────┐ ┌───────────┐ ┌───────────┐
        │ Cloud Run │ │ Cloud Run │ │ Cloud Run │
        │ Instance  │ │ Instance  │ │ Instance  │
        │ (auto)    │ │ (auto)    │ │ (auto)    │
        └─────┬─────┘ └─────┬─────┘ └─────┬─────┘
              │              │              │
              └──────────────┼──────────────┘
                             │
                    ┌────────▼────────┐
                    │  Cloud SQL      │
                    │  PostgreSQL     │
                    │  (asia-south1)  │
                    └─────────────────┘
```

### Dockerfile for Backend

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]
```

### Key Infrastructure Decisions

| Decision | Recommendation |
|----------|---------------|
| **Recipe cache** | In-memory `recipe_cache.py` works on Cloud Run but resets on cold start. Add Redis (Memorystore) if cold starts become problematic |
| **File storage** | Recipe images / user photos → GCS bucket with signed URLs |
| **Background jobs** | Meal generation takes 4-7s — Cloud Run handles this fine (timeout 300s default). No task queue needed initially |

---

## Phase 3: Cost Management (Critical for AI Apps)

### AI API Cost Estimation

| API | Usage Pattern | Estimated Cost |
|-----|--------------|----------------|
| **Gemini 2.5 Flash** (meal gen) | 1 generation/user/week, ~2K input + 8K output tokens | ~$0.001/generation |
| **Gemini** (photo analysis) | Occasional, ~1K tokens | ~$0.0005/analysis |
| **Claude API** (chat) | 5-10 messages/user/day, ~1K tokens each | ~$0.01-0.03/user/day |
| **Firebase Auth** | Free tier covers 10K MAU | Free |

### Cost Controls to Implement

```python
# Add to config.py
DAILY_CHAT_LIMIT: int = 50           # messages per user per day
WEEKLY_GENERATION_LIMIT: int = 3      # meal plan regenerations per week
MONTHLY_PHOTO_ANALYSIS_LIMIT: int = 20  # photo analyses per month
```

**Claude chat is the biggest cost driver.** At 10 messages/day across 10K users = $3,000-9,000/month. Implement:
1. Per-user daily message limits
2. Response caching for common queries (e.g., "what can I make with dal?")
3. Consider switching chat to Gemini Flash for cost reduction (currently Claude)

---

## Phase 4: Play Store Preparation (Week 3-4)

### Pre-Launch Checklist

| Item | Status | Action |
|------|--------|--------|
| App signing | Needed | Generate upload key, enroll in Play App Signing |
| Privacy Policy | Needed | Required — covers Firebase auth data, AI usage, meal preferences |
| Store listing | Needed | Screenshots (6-8), feature graphic, description (Hindi + English) |
| Content rating | Needed | IARC questionnaire (likely "Everyone") |
| Target audience | Needed | Adults 25-55, Indian families |
| Data safety form | Needed | Declare: Google account info, dietary preferences, meal history |
| Release track | — | Use Internal Testing → Closed Beta → Open Beta → Production |

### Build Configuration

```kotlin
// android/app/build.gradle.kts — release signing
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"https://api.rasoiai.com\"")
        }
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000\"")
        }
    }
}
```

### India-Specific Store Optimization

| Factor | Action |
|--------|--------|
| **App size** | Target < 30MB (Tier 2/3 cities have limited storage). Use App Bundle. |
| **Language** | Add Hindi store listing. Consider Marathi, Tamil, Telugu descriptions |
| **Screenshots** | Show Indian food, Indian family context, festival awareness |
| **Keywords** | "meal planner India", "Indian recipe", "weekly menu", "rasoi", "khana kya banega" |

---

## Phase 5: Observability & Monitoring (Week 4-5)

### Monitoring Stack

| Layer | Tool | What to Monitor |
|-------|------|----------------|
| **Crashes** | Firebase Crashlytics | ANRs, exceptions, crash-free rate |
| **Backend errors** | Sentry (already configured) | 500s, AI API failures, DB timeouts |
| **API latency** | GCP Cloud Monitoring | P50/P95/P99 for all endpoints |
| **AI reliability** | Custom metrics | Gemini generation success rate, retry counts |
| **User analytics** | Firebase Analytics | Onboarding completion, meal plan views, feature adoption |
| **Business metrics** | Custom dashboard | DAU, retention, recipes viewed, meals cooked |

### Critical Alerts to Set Up

```
1. Meal generation failure rate > 5% in 1 hour
2. API P95 latency > 3 seconds
3. Database connection pool exhaustion
4. AI API quota approaching limit
5. Crash-free rate drops below 99%
6. Cloud Run instance count > 10 (cost spike)
```

### Key Backend Metrics to Add

```python
# Add structured logging for production
import structlog
logger = structlog.get_logger()

# In ai_meal_service.py
logger.info("meal_generation_complete",
    user_id=user_id,
    duration_seconds=elapsed,
    retry_count=attempts,
    rules_applied=len(include_rules) + len(exclude_rules))
```

---

## Phase 6: Performance Optimization (Week 5-6)

### Backend

| Optimization | Impact | Effort |
|-------------|--------|--------|
| Add Redis for recipe cache | Eliminates cold-start cache rebuild of 3,580 recipes | Medium |
| Connection pooling tuning | `asyncpg` pool size based on Cloud Run concurrency | Low |
| Meal plan response caching | Cache current week's plan per user (invalidate on swap/regenerate) | Medium |
| Database indexes | Add indexes on `meal_plans(user_id, week_start_date)`, `recipes(cuisine_type, dietary_tags)` | Low |

### Android

| Optimization | Impact |
|-------------|--------|
| Enable R8 full mode | 30-50% APK size reduction |
| Baseline Profiles | Faster cold start (measure with Macrobenchmark) |
| Image loading | Use Coil with disk cache for recipe images |
| Room pre-population | Ship `known_ingredients` as pre-packaged DB instead of runtime seed |

---

## Phase 7: Beta Testing Strategy (Week 6-8)

### Rollout Plan

```
Week 6:  Internal Testing (5-10 team/family members)
         └─ Focus: crash detection, auth flow, meal generation quality

Week 7:  Closed Beta (50-100 users via Google Groups)
         └─ Focus: onboarding completion rate, recipe relevance, offline behavior
         └─ Recruit from: Indian cooking communities, family WhatsApp groups

Week 8:  Open Beta (500-1000 users)
         └─ Focus: scale testing, AI cost monitoring, retention metrics
         └─ Collect: NPS score, feature requests, meal quality feedback
```

### Key Metrics to Track During Beta

| Metric | Target | Why |
|--------|--------|-----|
| Onboarding completion | > 80% | 5-step wizard may lose users |
| Day-7 retention | > 30% | Do users come back for next week's plan? |
| Meal generation success | > 95% | AI failures = lost users |
| Meals marked "cooked" | > 3/week | Are people actually using the plans? |
| Chat engagement | > 2 messages/session | Is the AI assistant useful? |

---

## Phase 8: Production Launch (Week 8-10)

### Launch Day Checklist

```
□ Backend deployed to Cloud Run (asia-south1)
□ Cloud SQL PostgreSQL with automated daily backups
□ 3,580 recipes imported and cache warmed
□ Festival data seeded for next 12 months
□ Firebase production project configured
□ Sentry alerts configured
□ Play Store listing live (Internal → Production)
□ Rate limiting enabled on AI endpoints
□ DEBUG=false in production .env
□ SSL/TLS on all endpoints
□ Domain configured (api.rasoiai.com)
□ Privacy policy URL accessible
□ Crash-free baseline established from beta
```

### Staged Rollout

```
Day 1:   1% rollout (Play Store staged rollout)
Day 3:   5% rollout (monitor crash rate)
Day 7:   20% rollout (monitor AI costs)
Day 14:  50% rollout
Day 21:  100% rollout
```

---

## Phase 9: Post-Launch Growth (Month 2+)

### India-Specific Growth Levers

| Strategy | Implementation |
|----------|---------------|
| **WhatsApp sharing** | Already have grocery list WhatsApp share — add meal plan sharing |
| **Festival marketing** | Push notifications for major festivals (Navratri, Diwali, Pongal) with special meal plans |
| **Regional expansion** | Currently North+South heavy. Add East (Bengali) and West (Gujarati, Maharashtrian) recipes |
| **Family referral** | "Invite family members" — shared meal plan across households |
| **Vernacular support** | Hindi UI first, then regional languages |

### Revenue Model Options

| Model | Fit for RasoiAI |
|-------|-----------------|
| **Freemium** | Free: 1 generation/week, 5 chat messages/day. Premium: unlimited |
| **Subscription** | Rs 99/month or Rs 799/year (standard India pricing) |
| **Brand partnerships** | Sponsored recipes from brands (MDH, Everest, Amul) |
| **Grocery delivery** | BigBasket/Blinkit API integration for one-tap ordering |

---

## Estimated Timeline & Costs

| Phase | Duration | Estimated Monthly Cost |
|-------|----------|----------------------|
| Development (current) | Done | $0 (local) |
| Infrastructure setup | 2 weeks | ~$50-100/month (Cloud Run + Cloud SQL minimal) |
| Beta (100 users) | 4 weeks | ~$100-200/month |
| Launch (1K users) | Ongoing | ~$200-500/month |
| Growth (10K users) | Ongoing | ~$500-2,000/month |
| Scale (50K users) | Ongoing | ~$2,000-8,000/month |

**Biggest cost driver:** Claude API for chat. Consider Gemini Flash as a cheaper alternative for chat (~10x cheaper) and reserve Claude for complex tool-calling scenarios only.

---

## Immediate Next Steps (This Week)

1. **Remove `DEBUG=true` auth bypass** from production path (keep for dev only via build config)
2. **Create Dockerfile** for backend
3. **Set up GCP project** with Cloud Run + Cloud SQL in `asia-south1`
4. **Create release build flavor** with production API URL
5. **Draft privacy policy** (required for Play Store)

---

*Created: February 2026*
*Architecture: Android (Kotlin/Compose) + FastAPI + PostgreSQL + Firebase + Gemini/Claude*
