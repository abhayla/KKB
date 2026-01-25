# Ollie.ai - Complete Technical Documentation & Research

## Table of Contents
1. [Company Overview](#1-company-overview)
2. [System Architecture Diagram](#2-system-architecture-diagram)
3. [Data Flow Diagram](#3-data-flow-diagram)
4. [User Flow Diagram](#4-user-flow-diagram)
5. [Screen Details](#5-screen-details)
6. [Database Schema (ERD)](#6-database-schema-erd)
7. [API Endpoints](#7-api-endpoints)
8. [Technology Stack Summary](#8-technology-stack-summary)
9. [Key Features Deep Dive](#9-key-features-deep-dive)
10. [Pricing & Business Model](#10-pricing--business-model)
11. [Third-Party Integrations](#11-third-party-integrations)
12. [Competitor Analysis](#12-competitor-analysis)
13. [User Personas](#13-user-personas)
14. [User Reviews & Pain Points](#14-user-reviews--pain-points)
15. [Gamification & Engagement](#15-gamification--engagement)
16. [Notification Strategy](#16-notification-strategy)
17. [Onboarding Details](#17-onboarding-details)
18. [Known Limitations & Issues](#18-known-limitations--issues)
19. [Future Roadmap](#19-future-roadmap)
20. [Localization & Accessibility](#20-localization--accessibility)
21. [Security & Compliance](#21-security--compliance)
22. [Success Metrics & KPIs](#22-success-metrics--kpis)
23. [Content Strategy](#23-content-strategy)
24. [Sources & References](#24-sources--references)

---

## 1. Company Overview

### Basic Information

| Detail | Information |
|--------|-------------|
| **Company** | Confabulation Corporation |
| **Location** | 8910 University Center Lane, Suite 400, San Diego, CA 92122 |
| **Founded** | 2023 |
| **Website** | https://ollie.ai |
| **Users** | 75,000+ families |
| **Rating** | 4.8/5 stars |

### Founding Team

| Name | Role | Background |
|------|------|------------|
| **Bill Lennon** | Co-Founder & CEO | - |
| **Christy Shannon** | Co-Founder & CMO | Working mom of four kids |
| **Rushabh Doshi** | Co-Founder & CPTO | Ex-Facebook, Ex-YouTube, Former CTO at Oportun/Digit, MS CS Stanford |

### Engineering Team
- **Minghong Lin** – Head of Personalization
- **Anton Borvanov** – Software Engineer
- **Yan Koshelev** – Software Engineer
- **Luke Kollman** – UI/UX Designer
- **Luca Kader** – Software Engineer

### Funding & Investors
- **Khosla Ventures** - Lead investor
- **AI2** (Allen Institute for AI) - Strategic investor
- **ADS Ventures** - Institutional investor

### Media Coverage
Featured in: The Washington Post, Forbes, Fox, CNET

---

## 2. System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                   CLIENT LAYER                                        │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                       │
│   ┌─────────────────────┐         ┌─────────────────────┐                           │
│   │      iOS App        │         │    Android App       │                           │
│   │  (Swift/UIKit)      │         │  (Kotlin/Jetpack)    │                           │
│   ├─────────────────────┤         ├─────────────────────┤                           │
│   │ • Onboarding UI     │         │ • Onboarding UI      │                           │
│   │ • Meal Plan View    │         │ • Meal Plan View     │                           │
│   │ • Recipe Detail     │         │ • Recipe Detail      │                           │
│   │ • Grocery List      │         │ • Grocery List       │                           │
│   │ • Chat Interface    │         │ • Chat Interface     │                           │
│   │ • Camera (Pantry)   │         │ • Camera (Pantry)    │                           │
│   │ • Cooking Mode      │         │ • Cooking Mode       │                           │
│   │ • Local Cache       │         │ • Local Cache        │                           │
│   └──────────┬──────────┘         └──────────┬──────────┘                           │
│              │                               │                                        │
│              └───────────────┬───────────────┘                                        │
│                              │ HTTPS/REST + WebSocket                                 │
└──────────────────────────────┼──────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              API GATEWAY LAYER                                        │
├─────────────────────────────────────────────────────────────────────────────────────┤
│   ┌─────────────────────────────────────────────────────────────────────────────┐   │
│   │                         API Gateway (AWS/GCP)                                │   │
│   ├─────────────────────────────────────────────────────────────────────────────┤   │
│   │  • Rate Limiting           • Request Routing         • Load Balancing       │   │
│   │  • Authentication          • SSL Termination         • Request Validation   │   │
│   │  • API Versioning          • Response Caching        • Logging/Monitoring   │   │
│   └─────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                       │
│   ┌─────────────────────────────────────────────────────────────────────────────┐   │
│   │                      Authentication Service                                   │   │
│   │  OAuth 2.0: Google | Facebook | Apple | Email/Password                       │   │
│   │  JWT Token Generation & Validation                                           │   │
│   └─────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           MICROSERVICES LAYER                                         │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                       │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐                   │
│  │   USER SERVICE   │  │   MEAL SERVICE   │  │  RECIPE SERVICE  │                   │
│  ├──────────────────┤  ├──────────────────┤  ├──────────────────┤                   │
│  │ • User Profiles  │  │ • Weekly Plans   │  │ • Recipe CRUD    │                   │
│  │ • Preferences    │  │ • Calendar Mgmt  │  │ • AI Generation  │                   │
│  │ • Family Members │  │ • Meal Scheduling│  │ • Recipe Import  │                   │
│  │ • Diet Settings  │  │ • Meal Ratings   │  │ • Customization  │                   │
│  │ • Allergies      │  │ • Streak Tracking│  │ • Collections    │                   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘                   │
│                                                                                       │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐                   │
│  │ GROCERY SERVICE  │  │   AI/ML SERVICE  │  │  VISION SERVICE  │                   │
│  ├──────────────────┤  ├──────────────────┤  ├──────────────────┤                   │
│  │ • List Generate  │  │ • LLM Integration│  │ • Image Upload   │                   │
│  │ • Aisle Organize │  │ • Recipe Gen     │  │ • Ingredient     │                   │
│  │ • Instacart API  │  │ • Personalization│  │   Recognition    │                   │
│  │ • Amazon Fresh   │  │ • Chat/NLP       │  │ • Pantry Scan    │                   │
│  │ • Walmart API    │  │ • Recommendations│  │ • Receipt OCR    │                   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘                   │
│                                                                                       │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐                   │
│  │NOTIFICATION SERV │  │ NUTRITION SERV   │  │ ANALYTICS SERV   │                   │
│  ├──────────────────┤  ├──────────────────┤  ├──────────────────┤                   │
│  │ • Push (FCM/APNS)│  │ • Calorie Calc   │  │ • User Behavior  │                   │
│  │ • Email (SES)    │  │ • Macro Tracking │  │ • Usage Metrics  │                   │
│  │ • Reminders      │  │ • USDA API       │  │ • A/B Testing    │                   │
│  │ • Meal Alerts    │  │ • Diet Tags      │  │ • Funnel Analysis│                   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘                   │
│                                                                                       │
└─────────────────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              AI/ML LAYER                                              │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                       │
│  ┌────────────────────────────────────────────────────────────────────────────┐     │
│  │                         LLM ORCHESTRATOR                                    │     │
│  │  (LangChain / Custom Orchestration Framework)                               │     │
│  ├────────────────────────────────────────────────────────────────────────────┤     │
│  │                                                                              │     │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │     │
│  │  │  Recipe Agent   │  │ Personalization │  │   Chat Agent    │             │     │
│  │  │                 │  │     Agent       │  │                 │             │     │
│  │  │ • Generate new  │  │ • User profile  │  │ • Intent detect │             │     │
│  │  │   recipes       │  │   analysis      │  │ • Modification  │             │     │
│  │  │ • Modify exist  │  │ • Taste model   │  │   requests      │             │     │
│  │  │ • Nutrition     │  │ • Family prefs  │  │ • Q&A           │             │     │
│  │  │   calculation   │  │ • Learning loop │  │ • Suggestions   │             │     │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘             │     │
│  │                                                                              │     │
│  └────────────────────────────────────────────────────────────────────────────┘     │
│                                                                                       │
│  ┌─────────────────────────┐  ┌─────────────────────────┐                           │
│  │   VISION ML PIPELINE    │  │   VECTOR DATABASE       │                           │
│  ├─────────────────────────┤  ├─────────────────────────┤                           │
│  │ • ResNet/EfficientNet   │  │ • Recipe Embeddings     │                           │
│  │ • YOLO Object Detection │  │ • User Preference       │                           │
│  │ • Ingredient Classifier │  │   Vectors               │                           │
│  │ • MobileNetV2 (on-edge) │  │ • Semantic Search       │                           │
│  │ • OCR (Tesseract/Cloud) │  │ • (Pinecone/pgvector)   │                           │
│  └─────────────────────────┘  └─────────────────────────┘                           │
│                                                                                       │
└─────────────────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              DATA LAYER                                               │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                       │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐         │
│  │    PostgreSQL       │  │      MongoDB        │  │       Redis         │         │
│  │   (Primary DB)      │  │   (Document Store)  │  │      (Cache)        │         │
│  ├─────────────────────┤  ├─────────────────────┤  ├─────────────────────┤         │
│  │ • Users             │  │ • Recipe Documents  │  │ • Session Store     │         │
│  │ • Preferences       │  │ • Meal Plans        │  │ • Meal Plan Cache   │         │
│  │ • Family Members    │  │ • Chat History      │  │ • Recipe Cache      │         │
│  │ • Subscriptions     │  │ • User Interactions │  │ • Rate Limiting     │         │
│  │ • Payments          │  │ • AI Responses      │  │ • Real-time Data    │         │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘         │
│                                                                                       │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐         │
│  │    Cloud Storage    │  │   Message Queue     │  │    Search Engine    │         │
│  │     (S3/GCS)        │  │   (SQS/RabbitMQ)    │  │   (Elasticsearch)   │         │
│  ├─────────────────────┤  ├─────────────────────┤  ├─────────────────────┤         │
│  │ • Pantry Photos     │  │ • AI Job Queue      │  │ • Recipe Search     │         │
│  │ • Recipe Images     │  │ • Notification Queue│  │ • Ingredient Search │         │
│  │ • User Uploads      │  │ • Analytics Events  │  │ • Full-text Search  │         │
│  │ • (30-day retention)│  │ • Async Processing  │  │ • Filters           │         │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘         │
│                                                                                       │
└─────────────────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                        EXTERNAL INTEGRATIONS                                          │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                       │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐               │
│  │  Instacart   │ │ Amazon Fresh │ │   Walmart    │ │  USDA API    │               │
│  │  Connect API │ │     API      │ │     API      │ │ (Nutrition)  │               │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘               │
│                                                                                       │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐               │
│  │   OpenAI/    │ │   Google     │ │    Stripe    │ │   Firebase   │               │
│  │   Anthropic  │ │  Analytics   │ │  (Payments)  │ │ (Push/Auth)  │               │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘               │
│                                                                                       │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                                 │
│  │   PostHog    │ │  AppsFlyer   │ │   Twilio     │                                 │
│  │ (Analytics)  │ │ (Attribution)│ │   (SMS)      │                                 │
│  └──────────────┘ └──────────────┘ └──────────────┘                                 │
│                                                                                       │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Data Flow Diagram

### Level 0 - Context Diagram

```
                                    ┌─────────────────┐
                                    │   Grocery APIs  │
                                    │ (Instacart,     │
                                    │  Amazon Fresh)  │
                                    └────────▲────────┘
                                             │
                                             │ Grocery Orders
                                             │ Product Data
                                             │
┌─────────────┐                    ┌─────────┴─────────┐                    ┌─────────────┐
│             │  Preferences       │                   │  Nutrition Data    │             │
│    User     │  Meal Ratings      │                   │                    │  USDA API   │
│   (Family)  │  Photos            │    OLLIE.AI       │◄───────────────────│ (Nutrition  │
│             │─────────────────►  │                   │                    │  Database)  │
│             │                    │   MEAL PLANNING   │                    │             │
│             │◄─────────────────  │      SYSTEM       │                    └─────────────┘
│             │  Meal Plans        │                   │
│             │  Recipes           │                   │                    ┌─────────────┐
│             │  Grocery Lists     │                   │  Recipe Generation │             │
│             │  Notifications     │                   │◄───────────────────│   LLM API   │
└─────────────┘                    └───────────────────┘                    │  (GPT/etc)  │
                                             │                              │             │
                                             │ Payment                      └─────────────┘
                                             │ Processing
                                             ▼
                                    ┌─────────────────┐
                                    │  Payment Gateway│
                                    │  (Apple/Google  │
                                    │   In-App)       │
                                    └─────────────────┘
```

### Level 1 - Main Processes

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              OLLIE.AI DATA FLOW - LEVEL 1                             │
└──────────────────────────────────────────────────────────────────────────────────────┘

                          ┌─────────────────────────────────────┐
                          │              D1: USER DATA          │
                          │  ┌─────────────────────────────┐    │
                          │  │ • User Profiles              │    │
                          │  │ • Family Members             │    │
                          │  │ • Dietary Preferences        │    │
                          │  │ • Allergies & Restrictions   │    │
                          │  │ • Subscription Status        │    │
                          │  └─────────────────────────────┘    │
                          └───────────────┬─────────────────────┘
                                          │
                     ┌────────────────────┼────────────────────┐
                     │                    │                    │
                     ▼                    ▼                    ▼
          ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
          │                  │ │                  │ │                  │
  User ──►│  1.0 ONBOARDING  │ │ 2.0 MEAL PLANNING│ │ 3.0 GROCERY      │
          │    PROCESS       │ │    ENGINE        │ │    MANAGEMENT    │
          │                  │ │                  │ │                  │
          │ • Registration   │ │ • Generate Plans │ │ • List Creation  │
          │ • Questionnaire  │ │ • AI Recipes     │ │ • Store Mapping  │
          │ • Preference     │ │ • Personalization│ │ • Order Handling │
          │   Collection     │ │ • Modifications  │ │                  │
          └────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘
                   │                    │                    │
                   │                    ▼                    │
                   │         ┌──────────────────┐           │
                   │         │                  │           │
                   └────────►│   D2: RECIPES    │◄──────────┘
                             │     & MEALS      │
                             │                  │
                             │ • Generated      │
                             │   Recipes        │
                             │ • Weekly Plans   │
                             │ • Saved Favorites│
                             │ • Nutrition Data │
                             └────────┬─────────┘
                                      │
            ┌─────────────────────────┼─────────────────────────┐
            │                         │                         │
            ▼                         ▼                         ▼
 ┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
 │                  │     │                  │     │                  │
 │ 4.0 PANTRY       │     │ 5.0 COOKING      │     │ 6.0 LEARNING     │
 │    RECOGNITION   │     │    ASSISTANT     │     │    & FEEDBACK    │
 │                  │     │                  │     │                  │
 │ • Photo Upload   │     │ • Step-by-step   │     │ • Meal Ratings   │
 │ • AI Detection   │     │   Instructions   │     │ • Skip Tracking  │
 │ • Ingredient     │     │ • Cook Mode UI   │     │ • Preference     │
 │   Matching       │     │ • Timer          │     │   Updates        │
 │ • Waste Reduction│     │ • Substitutions  │     │ • Model Training │
 └────────┬─────────┘     └────────┬─────────┘     └────────┬─────────┘
          │                        │                        │
          │                        │                        │
          └────────────────────────┼────────────────────────┘
                                   │
                                   ▼
                          ┌─────────────────────────────────────┐
                          │         D3: AI/ML MODELS            │
                          │  ┌─────────────────────────────┐    │
                          │  │ • Recipe Generation Model    │    │
                          │  │ • Personalization Model      │    │
                          │  │ • Vision Model (Ingredients) │    │
                          │  │ • User Preference Vectors    │    │
                          │  │ • Recommendation Engine      │    │
                          │  └─────────────────────────────┘    │
                          └─────────────────────────────────────┘
```

### Level 2 - Meal Planning Process Detail

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                    2.0 MEAL PLANNING ENGINE - DETAILED FLOW                           │
└──────────────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────────┐
                              │   User Request  │
                              │ "Generate Week" │
                              └────────┬────────┘
                                       │
                                       ▼
                    ┌──────────────────────────────────────┐
                    │       2.1 PREFERENCE LOADER          │
                    │  ┌────────────────────────────────┐  │
                    │  │ Load from D1: USER DATA        │  │
                    │  │ • Family size                  │  │
                    │  │ • Dietary restrictions         │  │
                    │  │ • Cuisine preferences          │  │
                    │  │ • Time constraints             │  │
                    │  │ • Budget preferences           │  │
                    │  │ • Kitchen equipment            │  │
                    │  │ • Disliked ingredients         │  │
                    │  └────────────────────────────────┘  │
                    └──────────────────┬───────────────────┘
                                       │
                                       ▼
                    ┌──────────────────────────────────────┐
                    │      2.2 SCHEDULE ANALYZER           │
                    │  ┌────────────────────────────────┐  │
                    │  │ • Busy days (quick meals)      │  │
                    │  │ • Weekend (elaborate cooking)  │  │
                    │  │ • Special events               │  │
                    │  │ • Leftover days                │  │
                    │  └────────────────────────────────┘  │
                    └──────────────────┬───────────────────┘
                                       │
                                       ▼
         ┌─────────────────────────────┴─────────────────────────────┐
         │                                                           │
         ▼                                                           ▼
┌─────────────────────┐                                   ┌─────────────────────┐
│ 2.3 RECIPE GENERATOR│                                   │ 2.4 PANTRY CHECKER  │
│    (LLM-Powered)    │                                   │                     │
│                     │                                   │ Query existing      │
│ • Prompt with       │                                   │ pantry items        │
│   preferences       │                                   │                     │
│ • Generate 7 days   │                                   │ Prioritize recipes  │
│ • Variety rules     │                                   │ using available     │
│ • No repeat protein │                                   │ ingredients         │
│ • Ingredient reuse  │                                   │                     │
└──────────┬──────────┘                                   └──────────┬──────────┘
           │                                                         │
           └─────────────────────────┬───────────────────────────────┘
                                     │
                                     ▼
                    ┌──────────────────────────────────────┐
                    │      2.5 NUTRITION CALCULATOR        │
                    │  ┌────────────────────────────────┐  │
                    │  │ For each recipe:               │  │
                    │  │ • Calculate calories           │  │
                    │  │ • Calculate macros (P/C/F)     │  │
                    │  │ • Identify allergens           │  │
                    │  │ • Add diet tags                │  │
                    │  │   (keto, low-sodium, etc.)     │  │
                    │  └────────────────────────────────┘  │
                    └──────────────────┬───────────────────┘
                                       │
                                       ▼
                    ┌──────────────────────────────────────┐
                    │       2.6 PLAN OPTIMIZER             │
                    │  ┌────────────────────────────────┐  │
                    │  │ • Balance nutrition across     │  │
                    │  │   week                         │  │
                    │  │ • Minimize grocery waste       │  │
                    │  │ • Smart ingredient reuse       │  │
                    │  │   (herbs, proteins)            │  │
                    │  │ • Cost optimization            │  │
                    │  └────────────────────────────────┘  │
                    └──────────────────┬───────────────────┘
                                       │
                                       ▼
                    ┌──────────────────────────────────────┐
                    │         2.7 OUTPUT GENERATOR         │
                    │  ┌────────────────────────────────┐  │
                    │  │ Store to D2: RECIPES & MEALS   │  │
                    │  │                                │  │
                    │  │ Generate:                      │  │
                    │  │ • Weekly meal plan JSON        │  │
                    │  │ • Individual recipe documents  │  │
                    │  │ • Grocery list (auto-sorted)   │  │
                    │  │ • Calendar events              │  │
                    │  └────────────────────────────────┘  │
                    └──────────────────┬───────────────────┘
                                       │
                                       ▼
                              ┌─────────────────┐
                              │  Return Plan    │
                              │   to User       │
                              └─────────────────┘
```

---

## 4. User Flow Diagram

### Complete User Journey

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              OLLIE.AI USER FLOW                                       │
└─────────────────────────────────────────────────────────────────────────────────────┘

    ┌───────────┐
    │   START   │
    └─────┬─────┘
          │
          ▼
    ┌───────────────┐     ┌───────────────┐
    │  Download App │────►│  Splash Screen │
    │ (iOS/Android) │     │  + Branding    │
    └───────────────┘     └───────┬───────┘
                                  │
                                  ▼
                    ┌─────────────────────────┐
                    │     AUTHENTICATION      │
                    │  ┌───────────────────┐  │
                    │  │ • Sign in Google  │  │
                    │  │ • Sign in Apple   │  │
                    │  │ • Sign in Facebook│  │
                    │  │ • Email/Password  │  │
                    │  └───────────────────┘  │
                    └────────────┬────────────┘
                                 │
            ┌────────────────────┴────────────────────┐
            │                                         │
            ▼                                         ▼
    ┌───────────────┐                        ┌───────────────┐
    │  NEW USER     │                        │ EXISTING USER │
    │  Onboarding   │                        │    Login      │
    └───────┬───────┘                        └───────┬───────┘
            │                                         │
            ▼                                         │
┌───────────────────────────────────────┐            │
│         5-PART ONBOARDING QUIZ        │            │
├───────────────────────────────────────┤            │
│                                       │            │
│  STEP 1: HOUSEHOLD SIZE               │            │
│  • How many people cooking for?       │            │
│  • Add adults and children            │            │
│                                       │            │
│  STEP 2: DIETARY RESTRICTIONS         │            │
│  • Vegetarian, Vegan, Gluten-free     │            │
│  • Allergies (nuts, shellfish, etc.)  │            │
│  • Religious (Halal, Kosher)          │            │
│                                       │            │
│  STEP 3: DISLIKED INGREDIENTS         │            │
│  • Search and add ingredients         │            │
│                                       │            │
│  STEP 4: FLAVOR PREFERENCES           │            │
│  • Cuisine types (Italian, Asian...)  │            │
│  • Spice level preference             │            │
│                                       │            │
│  STEP 5: SCHEDULE & TIME              │            │
│  • Cooking time weekday/weekend       │            │
│  • Busy days for quick meals          │            │
│  • Kitchen equipment available        │            │
│                                       │            │
└───────────────────┬───────────────────┘            │
                    │                                 │
                    └────────────────┬────────────────┘
                                     │
                                     ▼
                    ┌─────────────────────────────────┐
                    │       AI GENERATING PLAN        │
                    │  "Creating your perfect         │
                    │   meal plan..."                 │
                    └─────────────────┬───────────────┘
                                      │
                                      ▼
                              ┌───────────────┐
                              │   MAIN HOME   │
                              │  (Weekly Menu)│
                              └───────┬───────┘
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          │                           │                           │
          ▼                           ▼                           ▼
    [TAP RECIPE]              [TAP GROCERY]                [TAP CHAT]
          │                           │                           │
          ▼                           ▼                           ▼
┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
│   RECIPE DETAIL     │   │    GROCERY LIST     │   │    CHAT INTERFACE   │
│   • Ingredients     │   │    • By category    │   │    • Modify meals   │
│   • Instructions    │   │    • Order online   │   │    • Get suggestions│
│   • Nutrition       │   │    • Check off      │   │    • Ask questions  │
│   • Cooking Mode    │   │                     │   │                     │
└─────────────────────┘   └─────────────────────┘   └─────────────────────┘
```

### Meal Modification Flow

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           MEAL MODIFICATION FLOW                                      │
└─────────────────────────────────────────────────────────────────────────────────────┘

    User taps [Swap] on a meal
              │
              ▼
    ┌─────────────────────────┐
    │   MODIFICATION OPTIONS  │
    │                         │
    │ ○ Swap entire meal      │
    │ ○ Modify recipe         │
    │ ○ Use leftovers         │
    │ ○ Skip this meal        │
    │ ○ Ask Ollie             │
    └────────────┬────────────┘
                 │
    ┌────────────┴────────────┬─────────────────┬─────────────────┐
    │                         │                 │                 │
    ▼                         ▼                 ▼                 ▼
┌─────────────┐      ┌─────────────┐    ┌─────────────┐   ┌─────────────┐
│ SWAP MEAL   │      │MODIFY RECIPE│    │ LEFTOVERS   │   │  ASK OLLIE  │
│             │      │             │    │             │   │   (Chat)    │
│ AI suggests │      │ Quick opts: │    │ Mark as     │   │             │
│ alternatives│      │ • Vegetarian│    │ "using      │   │ "Make this  │
│ based on:   │      │ • Faster    │    │ leftovers"  │   │  spicier"   │
│ • Same time │      │ • Kid-friend│    │             │   │             │
│ • Same      │      │ • Add protein│   │ Updates     │   │ AI modifies │
│   cuisine   │      │             │    │ grocery     │   │ recipe and  │
│ • Pantry    │      │ Opens chat  │    │ list        │   │ nutrition   │
│   items     │      │ for custom  │    │             │   │             │
└──────┬──────┘      └──────┬──────┘    └──────┬──────┘   └──────┬──────┘
       │                    │                  │                 │
       └────────────────────┴──────────────────┴─────────────────┘
                                       │
                                       ▼
                          ┌─────────────────────────┐
                          │   PLAN UPDATED          │
                          │ • Recipe changed        │
                          │ • Nutrition recalculated│
                          │ • Grocery list updated  │
                          └─────────────────────────┘
```

---

## 5. Screen Details

### Screen 1: Splash & Authentication

```
┌─────────────────────────────────────┐
│                                     │
│            ┌───────────┐            │
│            │   OLLIE   │            │
│            └───────────┘            │
│                                     │
│      AI Meal Planning for           │
│           Families                  │
│                                     │
│   ┌─────────────────────────────┐   │
│   │  Continue with Google    G  │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │  Continue with Apple        │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │  Continue with Email        │   │
│   └─────────────────────────────┘   │
│                                     │
│         Already have account?       │
│              Sign In                │
│                                     │
│   Terms of Service • Privacy Policy │
│                                     │
└─────────────────────────────────────┘
```

### Screen 2: Onboarding Quiz

```
┌─────────────────────────────────────┐
│  ←                          1 of 5  │
│─────────────────────────────────────│
│                                     │
│       How many people are           │
│       you cooking for?              │
│                                     │
│   ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐   │
│   │  1  │ │  2  │ │  3  │ │  4  │   │
│   └─────┘ └─────┘ └─────┘ └─────┘   │
│                                     │
│   ┌─────┐ ┌─────┐                   │
│   │  5  │ │ 6+  │                   │
│   └─────┘ └─────┘                   │
│                                     │
│   Family members:                   │
│   ┌─────────────────────────────┐   │
│   │ + Add adult                 │   │
│   └─────────────────────────────┘   │
│   ┌─────────────────────────────┐   │
│   │ + Add child (with age)      │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │           NEXT              │   │
│   └─────────────────────────────┘   │
│   ●○○○○                             │
└─────────────────────────────────────┘
```

### Screen 3: Home / Weekly Menu

```
┌─────────────────────────────────────┐
│  ☰  Your Menu            🗓️ 📸 ⚙️   │
│─────────────────────────────────────│
│                                     │
│  This Week                          │
│  Jan 20 - 26         [Regenerate ↻] │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ TODAY - Monday            ✓Done ││
│  │                                 ││
│  │ 🍳 Breakfast                    ││
│  │ Avocado Toast with Poached Eggs ││
│  │ ⏱️ 15 min  •  🔥 420 cal        ││
│  │                          [Swap] ││
│  │                                 ││
│  │ 🥗 Lunch                        ││
│  │ Mediterranean Quinoa Bowl       ││
│  │ ⏱️ 20 min  •  🔥 380 cal        ││
│  │                          [Swap] ││
│  │                                 ││
│  │ 🍝 Dinner                       ││
│  │ Lemon Herb Chicken              ││
│  │ with Roasted Vegetables         ││
│  │ ⏱️ 35 min  •  🔥 520 cal        ││
│  │                          [Swap] ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ TUESDAY                      ▶  ││
│  │ Overnight Oats • Leftovers •    ││
│  │ Taco Night                      ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  🏠      📋      💬      📖      👤 │
│ Home  Grocery   Chat  Cookbook Profile│
└─────────────────────────────────────┘
```

### Screen 4: Recipe Detail

```
┌─────────────────────────────────────┐
│  ←  Recipe                    ♡  ⋮  │
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │      [Recipe Image Here]       ││
│  │   Lemon Herb Chicken with      ││
│  │   Roasted Vegetables           ││
│  └─────────────────────────────────┘│
│                                     │
│  ⏱️ 35 min   👥 4 servings   🔥 520cal│
│                                     │
│  Tags: [High Protein] [Gluten-Free] │
│                                     │
│  NUTRITION PER SERVING              │
│  ┌────────┬────────┬────────┬─────┐ │
│  │Calories│Protein │ Carbs  │ Fat │ │
│  │  520   │  42g   │  28g   │ 24g │ │
│  └────────┴────────┴────────┴─────┘ │
│                                     │
│  INGREDIENTS              [Add All] │
│  □ 4 chicken breasts (1.5 lbs)      │
│  □ 2 lemons, juiced                 │
│  □ 4 cloves garlic, minced          │
│  □ 2 tbsp olive oil                 │
│  □ 1 tsp dried oregano              │
│  □ 2 cups broccoli florets          │
│                                     │
│  INSTRUCTIONS                       │
│  1. Preheat oven to 400°F (200°C)   │
│  2. Mix lemon juice, garlic, oil... │
│  3. Place chicken in baking dish... │
│                                     │
│  ┌─────────────────────────────────┐│
│  │     🍳 START COOKING MODE       ││
│  └─────────────────────────────────┘│
│  ┌─────────────────────────────────┐│
│  │     💬 Modify with Ollie        ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

### Screen 5: Cooking Mode

```
┌─────────────────────────────────────┐
│  ✕  Cooking Mode         Step 2/5   │
│─────────────────────────────────────│
│                                     │
│         STEP 2                      │
│                                     │
│   In a bowl, mix together:          │
│                                     │
│   • Lemon juice (2 lemons)          │
│   • Minced garlic (4 cloves)        │
│   • Olive oil (2 tbsp)              │
│   • Dried oregano (1 tsp)           │
│   • Salt and pepper                 │
│                                     │
│         [Image/Animation]           │
│         Mixing marinade             │
│                                     │
│   ⏱️ No timer needed                │
│                                     │
│   ┌──────────┐    ┌──────────────┐  │
│   │  ← PREV  │    │    NEXT →    │  │
│   └──────────┘    └──────────────┘  │
│                                     │
│   💬 "Make it spicier"              │
│                                     │
│─────────────────────────────────────│
│        Screen stays ON 🔒           │
└─────────────────────────────────────┘
```

### Screen 6: Grocery List

```
┌─────────────────────────────────────┐
│  ←  Grocery List              ⋮     │
│─────────────────────────────────────│
│                                     │
│  Week of Jan 20-26                  │
│  23 items • Est. $85-95             │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🛒 Order from:                  ││
│  │ [Instacart] [Amazon] [Walmart]  ││
│  └─────────────────────────────────┘│
│                                     │
│  🥬 PRODUCE (8 items)           ▼   │
│  ┌─────────────────────────────────┐│
│  │ □ Avocados (4)                  ││
│  │ □ Lemons (4)                    ││
│  │ □ Cherry tomatoes (2 pints)     ││
│  │ □ Broccoli (2 heads)            ││
│  └─────────────────────────────────┘│
│                                     │
│  🥩 MEAT & PROTEIN (3 items)    ▼   │
│  ┌─────────────────────────────────┐│
│  │ □ Chicken breasts (2 lbs)       ││
│  │ □ Ground beef (1 lb)            ││
│  │ □ Eggs (1 dozen)                ││
│  └─────────────────────────────────┘│
│                                     │
│  🥛 DAIRY (4 items)             ▼   │
│  🥫 PANTRY (8 items)            ▼   │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ + Add custom item               ││
│  └─────────────────────────────────┘│
│─────────────────────────────────────│
│  🏠      📋      💬      📖      👤 │
└─────────────────────────────────────┘
```

### Screen 7: Chat Interface

```
┌─────────────────────────────────────┐
│  ←  Chat with Ollie                 │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🤖 Ollie                        ││
│  │                                 ││
│  │ Hi! I'm Ollie, your AI meal     ││
│  │ planning assistant. How can I   ││
│  │ help you today?                 ││
│  │                                 ││
│  │ Quick actions:                  ││
│  │ • Swap a meal                   ││
│  │ • Modify a recipe               ││
│  │ • What's in my pantry?          ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 👤 You                          ││
│  │ Can you make Tuesday's dinner   ││
│  │ vegetarian? My sister visiting. ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🤖 Ollie                        ││
│  │ Of course! I've updated         ││
│  │ Tuesday's Taco Night to:        ││
│  │ 🌮 Black Bean & Sweet Potato    ││
│  │    Tacos (30 min • 450 cal)     ││
│  │                                 ││
│  │ [View Recipe] [View Grocery]    ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ Type a message...          📎 🎤││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

### Screen 8: Pantry Scanner

```
┌─────────────────────────────────────┐
│  ←  Scan Pantry                     │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │                                 ││
│  │       [CAMERA VIEWFINDER]       ││
│  │                                 ││
│  │    Point at your fridge or      ││
│  │    pantry shelves               ││
│  │                                 ││
│  └─────────────────────────────────┘│
│                                     │
│           [📸 Capture]              │
│                                     │
│  Or upload from gallery:            │
│  ┌─────────────────────────────────┐│
│  │      📁 Choose from Photos      ││
│  └─────────────────────────────────┘│
│                                     │
│  DETECTED ITEMS (from last scan):   │
│  🥚 Eggs        🧀 Cheese           │
│  🥛 Milk        🍅 Tomatoes         │
│  🧅 Onions      🥬 Lettuce          │
│                                     │
│  ┌─────────────────────────────────┐│
│  │  🍳 Get recipes with these      ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

### Screen 9: Cooking Calendar

```
┌─────────────────────────────────────┐
│  ←  Cooking Calendar                │
│─────────────────────────────────────│
│                                     │
│       January 2025                  │
│  ◀  ─────────────────────────  ▶   │
│                                     │
│  Su   Mo   Tu   We   Th   Fr   Sa   │
│            1    2    3    4         │
│            🍝   🥗   🍗   🌮         │
│  5    6    7    8    9    10   11   │
│  🍜   🥘   🍛   🥙   🍲   🍕   🎉    │
│  12   13   14   15   16   17   18   │
│  🥗   🍝   🌯   🥘   🍗   🍜   🌮    │
│  19   20   21   22   23   24   25   │
│  🍛   ●    ○    ○    ○    ○    ○    │
│      TODAY                          │
│                                     │
│  YOUR STATS                         │
│  ┌─────────────────────────────────┐│
│  │  🔥 Current Streak: 12 days     ││
│  │  📊 This Month: 18 meals cooked ││
│  │  🏆 Longest Streak: 23 days     ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

### Screen 10: Profile & Settings

```
┌─────────────────────────────────────┐
│  ←  Profile                         │
│─────────────────────────────────────│
│         ┌───────────────┐           │
│         │   [Photo]     │           │
│         └───────────────┘           │
│         Sarah Johnson               │
│         sarah@email.com             │
│                                     │
│  SUBSCRIPTION                       │
│  ┌─────────────────────────────────┐│
│  │ ⭐ Ollie Plus - Annual          ││
│  │    Renews Feb 15, 2025 [Manage] ││
│  └─────────────────────────────────┘│
│                                     │
│  FAMILY                             │
│  ┌─────────────────────────────────┐│
│  │ 👨 Mike (Adult)           [Edit]││
│  │ 👧 Emma, 8 years          [Edit]││
│  │ 👦 Jack, 5 years          [Edit]││
│  │ + Add family member             ││
│  └─────────────────────────────────┘│
│                                     │
│  PREFERENCES                        │
│  ┌─────────────────────────────────┐│
│  │ Dietary Restrictions      ▶     ││
│  │ Disliked Ingredients      ▶     ││
│  │ Cuisine Preferences       ▶     ││
│  │ Cooking Time              ▶     ││
│  └─────────────────────────────────┘│
│                                     │
│  CONNECTED SERVICES                 │
│  ┌─────────────────────────────────┐│
│  │ Instacart           Connected ✓ ││
│  │ Amazon Fresh        Connect ▶   ││
│  └─────────────────────────────────┘│
│                                     │
│  SETTINGS                           │
│  ┌─────────────────────────────────┐│
│  │ Notifications / Privacy / Help  ││
│  │ Sign Out                        ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

---

## 6. Database Schema (ERD)

### Core Tables

```
┌─────────────────────────────┐          ┌─────────────────────────────┐
│          USERS              │          │       FAMILY_MEMBERS        │
├─────────────────────────────┤          ├─────────────────────────────┤
│ PK  user_id          UUID   │──────┐   │ PK  member_id        UUID   │
│     email            VARCHAR│      │   │ FK  user_id          UUID   │
│     password_hash    VARCHAR│      │   │     name             VARCHAR│
│     auth_provider    ENUM   │      │   │     type (adult/child) ENUM │
│     created_at       TIMESTAMP     │   │     age              INT    │
│     subscription_status ENUM│      │   │     created_at       TIMESTAMP
└──────────────┬──────────────┘      │   └─────────────────────────────┘
               │                      │
               ▼                      │
┌─────────────────────────────┐      │
│      USER_PREFERENCES       │      │
├─────────────────────────────┤      │
│ PK  preference_id    UUID   │      │
│ FK  user_id          UUID   │◄─────┘
│     household_size   INT    │
│     spice_level      INT    │
│     cooking_time_weekday INT│
│     cooking_time_weekend INT│
│     busy_days        JSONB  │
│     kitchen_equipment JSONB │
└─────────────────────────────┘

┌─────────────────────────────┐          ┌─────────────────────────────┐
│    DIETARY_RESTRICTIONS     │          │     DISLIKED_INGREDIENTS    │
├─────────────────────────────┤          ├─────────────────────────────┤
│ PK  restriction_id   UUID   │          │ PK  dislike_id       UUID   │
│ FK  user_id          UUID   │          │ FK  user_id          UUID   │
│     restriction_type ENUM   │          │ FK  ingredient_id    UUID   │
│     (vegetarian, vegan,     │          │     created_at       TIMESTAMP
│      gluten_free, etc.)     │          └─────────────────────────────┘
└─────────────────────────────┘

┌─────────────────────────────┐          ┌─────────────────────────────┐
│        INGREDIENTS          │          │      CUISINE_PREFERENCES    │
├─────────────────────────────┤          ├─────────────────────────────┤
│ PK  ingredient_id    UUID   │          │ PK  cuisine_pref_id  UUID   │
│     name             VARCHAR│          │ FK  user_id          UUID   │
│     category         ENUM   │          │     cuisine_type     ENUM   │
│     unit_type        VARCHAR│          │     preference_level INT    │
│     calories_per_unit DECIMAL          └─────────────────────────────┘
│     protein_per_unit DECIMAL│
│     carbs_per_unit   DECIMAL│
│     fat_per_unit     DECIMAL│
└─────────────────────────────┘
```

### Recipe & Meal Plan Tables

```
┌─────────────────────────────┐          ┌─────────────────────────────┐
│          RECIPES            │          │     RECIPE_INGREDIENTS      │
├─────────────────────────────┤          ├─────────────────────────────┤
│ PK  recipe_id        UUID   │──────────│ PK  recipe_ing_id    UUID   │
│     title            VARCHAR│          │ FK  recipe_id        UUID   │
│     description      TEXT   │          │ FK  ingredient_id    UUID   │
│     prep_time        INT    │          │     quantity         DECIMAL│
│     cook_time        INT    │          │     unit             VARCHAR│
│     total_time       INT    │          └─────────────────────────────┘
│     servings         INT    │
│     difficulty       ENUM   │          ┌─────────────────────────────┐
│     cuisine_type     ENUM   │          │      RECIPE_STEPS           │
│     is_ai_generated  BOOLEAN│          ├─────────────────────────────┤
│     image_url        VARCHAR│          │ PK  step_id          UUID   │
│     calories         INT    │          │ FK  recipe_id        UUID   │
│     protein          DECIMAL│          │     step_number      INT    │
│     carbs            DECIMAL│          │     instruction      TEXT   │
│     fat              DECIMAL│          │     duration_minutes INT    │
└─────────────────────────────┘          └─────────────────────────────┘

┌─────────────────────────────┐          ┌─────────────────────────────┐
│         MEAL_PLANS          │          │       MEAL_PLAN_ITEMS       │
├─────────────────────────────┤          ├─────────────────────────────┤
│ PK  meal_plan_id     UUID   │──────────│ PK  plan_item_id     UUID   │
│ FK  user_id          UUID   │          │ FK  meal_plan_id     UUID   │
│     week_start_date  DATE   │          │ FK  recipe_id        UUID   │
│     week_end_date    DATE   │          │     day_of_week      INT    │
│     status           ENUM   │          │     meal_type        ENUM   │
│     (active, archived)      │          │     (breakfast, lunch,      │
│     created_at       TIMESTAMP         │      dinner, snack)         │
└─────────────────────────────┘          │     is_leftover      BOOLEAN│
                                         │     rating           INT    │
                                         └─────────────────────────────┘
```

### Grocery & Pantry Tables

```
┌─────────────────────────────┐          ┌─────────────────────────────┐
│       GROCERY_LISTS         │          │     GROCERY_LIST_ITEMS      │
├─────────────────────────────┤          ├─────────────────────────────┤
│ PK  grocery_list_id  UUID   │──────────│ PK  list_item_id     UUID   │
│ FK  user_id          UUID   │          │ FK  grocery_list_id  UUID   │
│ FK  meal_plan_id     UUID   │          │ FK  ingredient_id    UUID   │
│     created_at       TIMESTAMP         │     quantity         DECIMAL│
│     status           ENUM   │          │     category         ENUM   │
│     order_total      DECIMAL│          │     is_checked       BOOLEAN│
└─────────────────────────────┘          └─────────────────────────────┘

┌─────────────────────────────┐          ┌─────────────────────────────┐
│       USER_COOKBOOK         │          │      PANTRY_ITEMS           │
├─────────────────────────────┤          ├─────────────────────────────┤
│ PK  cookbook_entry_id UUID  │          │ PK  pantry_item_id   UUID   │
│ FK  user_id          UUID   │          │ FK  user_id          UUID   │
│ FK  recipe_id        UUID   │          │ FK  ingredient_id    UUID   │
│     collection_name  VARCHAR│          │     quantity         DECIMAL│
│     is_favorite      BOOLEAN│          │     expiry_date      DATE   │
│     modifications    JSONB  │          │     source (manual/scanned) │
└─────────────────────────────┘          └─────────────────────────────┘

┌─────────────────────────────┐          ┌─────────────────────────────┐
│       CHAT_HISTORY          │          │      PANTRY_SCANS           │
├─────────────────────────────┤          ├─────────────────────────────┤
│ PK  chat_id          UUID   │          │ PK  scan_id          UUID   │
│ FK  user_id          UUID   │          │ FK  user_id          UUID   │
│     message          TEXT   │          │     image_url        VARCHAR│
│     role (user/assistant)   │          │     detected_items   JSONB  │
│     context          JSONB  │          │     confidence_scores JSONB │
│     created_at       TIMESTAMP         │     created_at       TIMESTAMP
└─────────────────────────────┘          └─────────────────────────────┘

┌─────────────────────────────┐
│       SUBSCRIPTIONS         │
├─────────────────────────────┤
│ PK  subscription_id  UUID   │
│ FK  user_id          UUID   │
│     plan_type (monthly/annual)
│     status           ENUM   │
│     start_date       DATE   │
│     end_date         DATE   │
│     payment_provider ENUM   │
│     amount           DECIMAL│
└─────────────────────────────┘
```

---

## 7. API Endpoints

### Authentication APIs
```
POST   /api/v1/auth/register          - New user registration
POST   /api/v1/auth/login             - Email/password login
POST   /api/v1/auth/oauth/{provider}  - OAuth login (google/apple/facebook)
POST   /api/v1/auth/refresh           - Refresh JWT token
POST   /api/v1/auth/logout            - Logout user
POST   /api/v1/auth/forgot-password   - Password reset request
POST   /api/v1/auth/reset-password    - Reset password with token
```

### User & Preferences APIs
```
GET    /api/v1/users/me               - Get current user profile
PUT    /api/v1/users/me               - Update user profile
DELETE /api/v1/users/me               - Delete account

GET    /api/v1/users/preferences      - Get user preferences
PUT    /api/v1/users/preferences      - Update preferences
POST   /api/v1/users/preferences/onboarding - Complete onboarding

GET    /api/v1/users/family           - Get family members
POST   /api/v1/users/family           - Add family member
PUT    /api/v1/users/family/{id}      - Update family member
DELETE /api/v1/users/family/{id}      - Remove family member

GET    /api/v1/users/dietary          - Get dietary restrictions
PUT    /api/v1/users/dietary          - Update dietary restrictions
```

### Meal Planning APIs
```
GET    /api/v1/meal-plans             - Get all meal plans
GET    /api/v1/meal-plans/current     - Get current week's plan
POST   /api/v1/meal-plans/generate    - Generate new meal plan
PUT    /api/v1/meal-plans/{id}        - Update meal plan
DELETE /api/v1/meal-plans/{id}        - Delete meal plan

GET    /api/v1/meal-plans/{id}/items  - Get plan items
PUT    /api/v1/meal-plans/{id}/items/{itemId}  - Update plan item
POST   /api/v1/meal-plans/{id}/items/{itemId}/swap  - Swap meal
POST   /api/v1/meal-plans/{id}/items/{itemId}/skip  - Skip meal
POST   /api/v1/meal-plans/{id}/items/{itemId}/rate  - Rate meal
```

### Recipe APIs
```
GET    /api/v1/recipes                - Search/list recipes
GET    /api/v1/recipes/{id}           - Get recipe detail
POST   /api/v1/recipes                - Create custom recipe
PUT    /api/v1/recipes/{id}           - Update recipe
DELETE /api/v1/recipes/{id}           - Delete recipe

POST   /api/v1/recipes/generate       - AI generate recipe
POST   /api/v1/recipes/modify         - AI modify recipe
POST   /api/v1/recipes/import         - Import from URL/photo
GET    /api/v1/recipes/{id}/nutrition - Get nutrition info
```

### Grocery APIs
```
GET    /api/v1/grocery-lists          - Get all grocery lists
GET    /api/v1/grocery-lists/current  - Get current list
POST   /api/v1/grocery-lists          - Create grocery list
PUT    /api/v1/grocery-lists/{id}     - Update list
DELETE /api/v1/grocery-lists/{id}     - Delete list

GET    /api/v1/grocery-lists/{id}/items    - Get list items
POST   /api/v1/grocery-lists/{id}/items    - Add item
PUT    /api/v1/grocery-lists/{id}/items/{itemId}  - Update item
DELETE /api/v1/grocery-lists/{id}/items/{itemId}  - Remove item
POST   /api/v1/grocery-lists/{id}/items/{itemId}/check  - Toggle checked

POST   /api/v1/grocery-lists/{id}/order/instacart    - Order via Instacart
POST   /api/v1/grocery-lists/{id}/order/amazon-fresh - Order via Amazon
```

### Pantry APIs
```
GET    /api/v1/pantry                 - Get pantry items
POST   /api/v1/pantry                 - Add pantry item
PUT    /api/v1/pantry/{id}            - Update pantry item
DELETE /api/v1/pantry/{id}            - Remove pantry item

POST   /api/v1/pantry/scan            - Upload pantry photo for scanning
GET    /api/v1/pantry/scans           - Get scan history
POST   /api/v1/pantry/suggest-recipes - Get recipes from pantry items
```

### Chat/AI APIs
```
POST   /api/v1/chat                   - Send message to AI
GET    /api/v1/chat/history           - Get chat history
DELETE /api/v1/chat/history           - Clear chat history
POST   /api/v1/chat/quick-action      - Execute quick action
```

### Cookbook APIs
```
GET    /api/v1/cookbook               - Get saved recipes
POST   /api/v1/cookbook               - Save recipe to cookbook
PUT    /api/v1/cookbook/{id}          - Update cookbook entry
DELETE /api/v1/cookbook/{id}          - Remove from cookbook
GET    /api/v1/cookbook/collections   - Get collections
POST   /api/v1/cookbook/collections   - Create collection
```

### Subscription APIs
```
GET    /api/v1/subscriptions          - Get subscription status
POST   /api/v1/subscriptions          - Create subscription
PUT    /api/v1/subscriptions          - Update subscription
DELETE /api/v1/subscriptions          - Cancel subscription
POST   /api/v1/subscriptions/restore  - Restore subscription
```

---

## 8. Technology Stack Summary

| Layer | Technology | Purpose |
|-------|------------|---------|
| **iOS App** | Swift, UIKit | Native iOS development |
| **Android App** | Kotlin, Jetpack Compose | Native Android development |
| **API Gateway** | AWS API Gateway / Kong | Request routing, rate limiting |
| **Backend** | Node.js (NestJS) or Python (FastAPI) | API services |
| **Primary DB** | PostgreSQL | User data, structured data |
| **Document DB** | MongoDB | Recipes, meal plans, chat history |
| **Cache** | Redis | Session, caching, real-time |
| **Search** | Elasticsearch | Recipe search, ingredient search |
| **Queue** | RabbitMQ / AWS SQS | Async processing |
| **Storage** | AWS S3 / GCS | Images, photos |
| **LLM** | OpenAI GPT-4 / Anthropic Claude | Recipe generation, chat |
| **Vision AI** | Custom CNN (EfficientNet) / Google Vision | Pantry scanning |
| **Vector DB** | Pinecone / pgvector | Recipe embeddings, semantic search |
| **Analytics** | Google Analytics, PostHog, AppsFlyer | User analytics, attribution |
| **Push Notifications** | Firebase Cloud Messaging, APNS | Notifications |
| **Payments** | Apple In-App Purchase, Google Play Billing | Subscriptions |
| **Grocery APIs** | Instacart Connect, Amazon Fresh API | Grocery ordering |
| **Nutrition API** | USDA FoodData Central | Nutrition data |

---

## 9. Key Features Deep Dive

### 9.1 AI-Powered Meal Planning

| Feature | Implementation |
|---------|----------------|
| **Personalization Engine** | 5-part onboarding questionnaire captures preferences, restrictions, flavor likes |
| **Recipe Generation** | AI-generated recipes (not from static database) |
| **Adaptive Learning** | Model improves with every interaction, remembers family patterns |
| **Real-time Customization** | Chat interface for modifications ("make it vegetarian", "quicker dinner") |

### 9.2 Computer Vision (Pantry Scanning)
- Users photograph fridge/pantry
- AI identifies ingredients using image recognition
- Suggests meals to reduce food waste
- EXIF metadata (including geolocation) may be captured

### 9.3 Smart Grocery Lists
- Auto-organized by store section (produce, dairy, pantry)
- Prevents duplicate purchases
- Updates instantly when plan changes
- Integrations: Instacart, Amazon Fresh, Walmart

### 9.4 Cooking Calendar
- Track meal ratings and streaks
- View past and upcoming meals
- Cook mode (keeps screen on while cooking)

### 9.5 Recipe Management
- Import via links, cookbook photos, or descriptions
- Save to custom collections
- Modifications noted and saved

---

## 10. Pricing & Business Model

| Plan | Price |
|------|-------|
| **Free Trial** | 7 days (no credit card required) |
| **Monthly** | $9.99/month |
| **Annual** | ~$7/month ($79.99/year) |

### Business Model
- Subscription-based SaaS
- No ads ("no ads, ever" policy)
- Potential affiliate revenue from grocery integrations

---

## 11. Third-Party Integrations

| Category | Services |
|----------|----------|
| **Analytics** | Google Analytics, PostHog, AppsFlyer |
| **Advertising** | Google Ads, Facebook, Twitter, Pinterest |
| **Auth** | Google, Facebook, Instagram, Twitter, LinkedIn OAuth |
| **Payments** | Apple In-App Purchase, Google Play Billing |
| **Grocery APIs** | Instacart Connect API, Amazon Fresh API |

### Data Retention Policy

| Data Type | Retention |
|-----------|-----------|
| Photos/Videos | 30 days for service delivery |
| De-identified copies | Up to 5 years (model training) |
| Usage data | Varies by security needs |

---

## 12. Competitor Analysis

### Direct Competitors Comparison

| Feature | Ollie | Mealime | eMeals | Plan to Eat | Yummly |
|---------|-------|---------|--------|-------------|--------|
| **AI Planning** | ✅ Fully automated | ❌ Manual selection | ❌ Pre-made menus | ❌ Manual | ❌ Recommendations only |
| **Recipe Generation** | ✅ AI-generated unique | ❌ Database | ❌ Database | ❌ User imports | ❌ Database |
| **Pantry Scanning** | ✅ Computer vision | ❌ | ❌ | ❌ | ❌ |
| **Grocery Integration** | ✅ Instacart, Amazon, Walmart | ✅ Automated lists | ✅ Instacart, Kroger | ✅ Basic list | ✅ Multiple stores |
| **Chat Interface** | ✅ Natural language | ❌ | ❌ | ❌ | ❌ |
| **Family Profiles** | ✅ Multiple members | ✅ Basic | ✅ Basic | ❌ | ✅ Basic |
| **Pricing** | $7-10/mo | Free / $2.99-5.99/mo | $4.99-9.99/mo | $5.95/mo | Free / Premium |
| **Free Tier** | 7-day trial only | ✅ Yes | 14-day trial | 14-day trial | ✅ Yes |

### Competitive Advantages of Ollie

1. **AI-First Approach**: Only app with fully AI-generated recipes (not from database)
2. **Adaptive Learning**: Learns family patterns ("Taco Tuesdays", "Friday leftovers")
3. **Pantry Recognition**: Computer vision for ingredient identification
4. **Chat Modifications**: Natural language recipe customization
5. **Three-in-One**: Replaces meal planner + grocery list + AI assistant

### Competitive Disadvantages

1. **No Free Tier**: Subscription-only after trial (competitors have free versions)
2. **Higher Price Point**: More expensive than Mealime
3. **Limited Cuisine Database**: May not cover all regional cuisines
4. **Internet Dependent**: Requires connectivity for AI features

### Market Position

Ollie positions itself as the **premium AI-powered solution** for families willing to pay for convenience and automation, unlike competitors focusing on budget-conscious users or manual planners.

---

## 13. User Personas

### Primary Persona: The Overwhelmed Working Mom

| Attribute | Details |
|-----------|---------|
| **Name** | Sarah, 38 |
| **Occupation** | Marketing Manager |
| **Family** | Married, 2 kids (ages 6 and 10) |
| **Pain Points** | "What's for dinner?" stress, no time for planning, picky eaters, food waste |
| **Goals** | Reduce mental load, healthier family meals, save time |
| **Tech Savvy** | High (uses multiple apps daily) |
| **Budget** | Willing to pay for convenience |
| **Quote** | "I was getting tired of spending so much on takeout" |

### Secondary Persona: The Sports Family Parent

| Attribute | Details |
|-----------|---------|
| **Name** | Mike, 42 |
| **Occupation** | Sales Executive |
| **Family** | Married, 3 kids in sports (ages 8, 12, 15) |
| **Pain Points** | 6pm dinner scramble, multiple schedules, quick meal needs |
| **Goals** | Coordinate meals around activities, quick healthy options |
| **Tech Savvy** | Moderate |
| **Budget** | Time is more valuable than money |
| **Quote** | "Practice ends at 6, games on weekends - when do we eat?" |

### Tertiary Persona: The Health-Conscious Family

| Attribute | Details |
|-----------|---------|
| **Name** | Priya, 35 |
| **Occupation** | Healthcare Professional |
| **Family** | Married, 1 child (age 4), elderly parent |
| **Pain Points** | Multiple dietary needs (low-sodium, kid-friendly, diabetic-friendly) |
| **Goals** | Manage family health through nutrition, track macros |
| **Tech Savvy** | High |
| **Budget** | Values health features over price |
| **Quote** | "I need meals that work for my whole family's different needs" |

### Anti-Persona (Not Target User)

| Attribute | Details |
|-----------|---------|
| **Name** | College Student |
| **Why Not** | Single person, budget-constrained, simple meals only |
| **Better Fit** | Mealime (free tier), basic recipe apps |

---

## 14. User Reviews & Pain Points

### Positive Feedback Themes

| Theme | Sample Review | Frequency |
|-------|---------------|-----------|
| **Reduced Stress** | "It wasn't the cooking I didn't love. It was everything that went into it up to that point" | Very High |
| **Time Savings** | "Ollie has changed my life - I feel a bit foolish saying an app has changed my life, but it truly has" | High |
| **Personalization** | "I really love the detailed nutritional info and how personalized Ollie has been for our low sodium health concerns" | High |
| **Picky Eaters** | "Before Ollie, figuring out what was for dinner was always a battle" | Medium |
| **Grocery Integration** | "The grocery list is dynamic with every meal you add" | Medium |

### Negative Feedback Themes

| Theme | Sample Review | Frequency | Severity |
|-------|---------------|-----------|----------|
| **Cooking Time Inaccurate** | "The recipes do take longer to make than the app says" | Medium | Low |
| **Subscription Only** | "They went to a paid subscription and now I can't see my 250+ saved recipes" | Medium | High |
| **Repetitive Suggestions** | "It only recommends Korean, Mediterranean, and stir fries despite my varied preferences" | Low | Medium |
| **Ignores Preferences** | "It includes ingredients I put on the dislike list" | Low | High |
| **Technical Issues** | "Ever since the new update, the app gets stuck on a white screen" | Low | High |
| **Price Concerns** | "Can no longer afford it due to monthly fees" | Medium | Medium |

### Net Promoter Score (Estimated)
- App Store Rating: **4.8/5** (874 ratings)
- Google Play Rating: **4.5+/5**
- Estimated NPS: **50-60** (Strong)

---

## 15. Gamification & Engagement

### Current Gamification Features

| Feature | Description | Purpose |
|---------|-------------|---------|
| **Cooking Streaks** | Track consecutive days of cooking | Habit formation |
| **Meal Ratings** | Rate meals after cooking (1-5 stars) | Feedback loop for AI |
| **Cooking Calendar** | Visual calendar showing meal history | Progress visualization |
| **Stats Dashboard** | Monthly meals cooked, new recipes tried | Achievement tracking |
| **Longest Streak** | Record of best cooking streak | Motivation |

### Engagement Loops

```
┌─────────────────────────────────────────────────────────────────┐
│                     ENGAGEMENT LOOP                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐  │
│   │  TRIGGER │───►│  ACTION  │───►│  REWARD  │───►│INVESTMENT│  │
│   │          │    │          │    │          │    │          │  │
│   │ Push     │    │ Open app │    │ See meal │    │ Rate     │  │
│   │ reminder │    │ view plan│    │ ready    │    │ meal     │  │
│   │ at 4pm   │    │          │    │          │    │          │  │
│   └──────────┘    └──────────┘    └──────────┘    └──────────┘  │
│        ▲                                              │          │
│        │                                              │          │
│        └──────────────────────────────────────────────┘          │
│                    (AI learns preferences)                       │
└─────────────────────────────────────────────────────────────────┘
```

### Missing Gamification (Opportunities)

- Social sharing / family leaderboards
- Achievement badges (e.g., "Tried 10 cuisines")
- Challenges (e.g., "Meatless Monday streak")
- Recipe creator community
- Points/rewards system

---

## 16. Notification Strategy

### Notification Types

| Type | Timing | Content | Purpose |
|------|--------|---------|---------|
| **Dinner Reminder** | ~4-5pm daily | "Tonight's dinner: [Recipe Name]. Ready in 30 min" | Pre-cooking prompt |
| **Weekly Plan Ready** | Sunday/Monday AM | "Your meal plan for the week is ready!" | Engagement |
| **Grocery Reminder** | Before shopping day | "Your grocery list has 23 items" | Action prompt |
| **Streak Alert** | Evening (if not cooked) | "Keep your streak going! 🔥" | Retention |
| **New Features** | Occasional | "New: Cooking Calendar is here!" | Feature adoption |
| **Daily Summary Audio** | Morning | Audio briefing of day's meals | Premium engagement |

### Notification Principles

1. **Proactive**: "Sending reminders and dinner ideas before you even think about it"
2. **Gentle Nudges**: "The app gently reminds you that you have a meal plan right there"
3. **Contextual**: Based on time of day and user patterns
4. **Non-Intrusive**: Respects user preferences

### Estimated Notification Frequency

- Daily: 1-2 notifications (meal reminder, optional summary)
- Weekly: 1-2 notifications (plan ready, weekly recap)
- Monthly: 1-2 notifications (features, engagement)

---

## 17. Onboarding Details

### 5-Part Onboarding Questionnaire

#### Step 1: Household Size
```
Questions:
- How many people are you cooking for? [1-6+]
- Add family members:
  - Adults: [Name, dietary needs]
  - Children: [Name, Age, dietary needs]
```

#### Step 2: Dietary Restrictions
```
Options (multi-select):
- Vegetarian
- Vegan
- Gluten-free
- Dairy-free
- Nut allergy
- Shellfish allergy
- Keto
- Low-sodium
- Halal
- Kosher
- None
```

#### Step 3: Disliked Ingredients
```
Interface:
- Search bar to find ingredients
- Add/remove disliked items
- Common suggestions shown
```

#### Step 4: Flavor Preferences
```
Cuisine types (multi-select):
- Italian
- Mexican
- Asian
- Mediterranean
- American
- Indian
- Middle Eastern
- French

Spice level:
- Slider from Mild to Hot
```

#### Step 5: Schedule & Time
```
Cooking time available:
- Weekdays: [15] [30] [45] [60] min
- Weekends: [30] [60] [90] [120] min

Busy days (quick meals needed):
- [Mon] [Tue] [Wed] [Thu] [Fri] [Sat] [Sun]

Kitchen equipment:
- Stove/Oven
- Instant Pot
- Air Fryer
- Slow Cooker
- Grill
- Blender
```

### Onboarding Metrics (Industry Standard)

| Metric | Target | Purpose |
|--------|--------|---------|
| Completion Rate | >70% | Reduce drop-off |
| Time to Complete | <3 min | Minimize friction |
| First Plan Generated | <1 min after quiz | Quick value delivery |
| First Recipe Cooked | Within 7 days | Activation |

---

## 18. Known Limitations & Issues

### Technical Limitations

| Limitation | Impact | Workaround |
|------------|--------|------------|
| **Internet Required** | Cannot use offline | No offline mode available |
| **English Only** | Limited to English-speaking markets | No localization yet |
| **US-Centric** | Grocery integrations US-focused | Limited international support |
| **iOS/Android Only** | No web app | Must use mobile device |

### Functional Limitations

| Limitation | User Impact | Status |
|------------|-------------|--------|
| **Recipe Time Estimates** | Often underestimate actual cooking time | Known issue |
| **Preference Learning** | Sometimes suggests disliked ingredients | Needs improvement |
| **Cuisine Variety** | May get repetitive within preferred cuisines | Feedback loop issue |
| **Recipe Lock-in** | Saved recipes inaccessible after subscription ends | User complaint |

### Known Bugs (From User Reviews)

| Bug | Platform | Status |
|-----|----------|--------|
| White screen on launch | iOS (after update) | Reported 3/2025 |
| Slow recipe loading | Both | Intermittent |
| Grocery list sync issues | Both | Occasional |

### Feature Gaps vs Competitors

- No free tier (Mealime has one)
- No web version (Plan to Eat has web)
- Limited international grocery integrations
- No social/sharing features
- No recipe import from URL (some competitors have this)

---

## 19. Future Roadmap

### Confirmed/Mentioned Features

| Feature | Status | Source |
|---------|--------|--------|
| **Real-time Grocery Substitutions** | Planned | Official blog |
| **Enhanced Fridge Inventory Scanning** | In progress | Official blog |
| **Voice Assistant Integration** | Planned | Official blog |
| **Daily Summary Audio** | Released | App update notes |

### Speculated Features (Based on Market Trends)

| Feature | Likelihood | Rationale |
|---------|------------|-----------|
| Web app version | High | User demand, accessibility |
| Family sharing/collaboration | High | Competitor feature |
| Meal prep mode | Medium | Popular feature request |
| Integration with smart appliances | Medium | IoT trend |
| Nutrition goal tracking | Medium | Health app integration |
| International grocery partners | High | Market expansion |
| Multi-language support | High | Global growth |

---

## 20. Localization & Accessibility

### Current Language Support

| Language | Status |
|----------|--------|
| English | ✅ Fully supported |
| Other languages | ❌ Not available |

### Accessibility Features

| Feature | Status |
|---------|--------|
| Screen reader support | Not disclosed |
| Voice control | Not disclosed |
| High contrast mode | Not disclosed |
| Text size adjustment | Not disclosed |
| Color blind mode | Not disclosed |

**Note**: The App Store listing states "The developer has not yet indicated which accessibility features this app supports."

### Offline Capabilities

| Feature | Offline Status |
|---------|----------------|
| View saved recipes | ❌ Requires internet |
| Generate new plans | ❌ Requires AI backend |
| Modify recipes | ❌ Requires AI backend |
| View grocery list | Partial (cached) |
| Cooking mode | Partial (if recipe cached) |

---

## 21. Security & Compliance

### Data Collection (From Privacy Policy)

| Data Type | Collected | Purpose |
|-----------|-----------|---------|
| Name, Email, Phone | Yes | Account management |
| IP Address | Yes | Security, analytics |
| Usage data | Yes | Product improvement |
| Conversation content | Yes | AI training, personalization |
| Pantry/fridge photos | Yes | Ingredient recognition |
| EXIF metadata (incl. geolocation) | Yes | From uploaded photos |
| Device info | Yes | Analytics |

### Data Retention

| Data Type | Retention Period |
|-----------|-----------------|
| Photos/videos | 30 days (service delivery) |
| De-identified data | Up to 5 years (model training) |
| Usage data | Varies |
| Account data | Until deletion requested |

### Third-Party Data Sharing

| Partner Type | Data Shared | Purpose |
|--------------|-------------|---------|
| Analytics (GA, PostHog) | Usage data | Analytics |
| Advertising (FB, Google) | Identifiers | Marketing |
| Payment (Apple, Google) | Transaction data | Billing |
| Grocery (Instacart, Amazon) | Order data | Fulfillment |

### Compliance Status

| Regulation | Status |
|------------|--------|
| GDPR | Not explicitly stated |
| CCPA | Likely compliant (US-based) |
| HIPAA | Not applicable (not health data) |
| SOC 2 | Not disclosed |

---

## 22. Success Metrics & KPIs

### Business Metrics

| Metric | Value | Source |
|--------|-------|--------|
| Total Users | 75,000+ families | Official website |
| App Store Rating | 4.8/5 | App Store |
| Total Ratings | 874+ | App Store |
| Google Play Rating | 4.5+/5 | Google Play |

### Estimated Metrics (Industry Benchmarks)

| Metric | Estimated Range | Benchmark Source |
|--------|-----------------|------------------|
| Trial-to-Paid Conversion | 10-20% | Subscription app average |
| Monthly Churn Rate | 5-10% | Consumer subscription apps |
| Daily Active Users (DAU) | 15-25% of total | Meal planning apps |
| Weekly Active Users (WAU) | 40-60% of total | Meal planning apps |
| Avg. Session Duration | 3-5 minutes | Utility apps |
| Sessions per Week | 5-10 | Meal planning context |

### Product Success Indicators

| Indicator | Measurement |
|-----------|-------------|
| Activation | First recipe cooked within 7 days |
| Engagement | 3+ meals planned per week |
| Retention | Active after 30 days |
| Revenue | Conversion to annual plan |
| Advocacy | App store review left |

---

## 23. Content Strategy

### Recipe Content

| Aspect | Approach |
|--------|----------|
| **Generation** | AI-generated (not from database) |
| **Uniqueness** | Each recipe customized to user |
| **Updates** | Real-time based on modifications |
| **Nutrition Data** | Auto-calculated from ingredients |

### Content Types

| Type | Description | Frequency |
|------|-------------|-----------|
| AI Recipes | Personalized generated recipes | On-demand |
| Meal Plans | Weekly curated plans | Weekly |
| Blog Articles | SEO content, tips, comparisons | Regular |
| Email Newsletter | Tips, features, engagement | Weekly/Monthly |

### SEO Content Strategy

Ollie publishes comparison articles targeting keywords like:
- "Best meal planning apps 2025"
- "Meal planning apps for families"
- "AI meal planning"
- "Ollie vs Mealime vs eMeals"

### User-Generated Content

| Type | Status |
|------|--------|
| Recipe sharing | Not available |
| User reviews in-app | Meal ratings only |
| Community features | Not available |
| Recipe imports | Via URL, photo, description |

---

## 24. Sources & References

### Official Sources
- [Ollie.ai Official Website](https://ollie.ai/)
- [Ollie iOS App Store](https://apps.apple.com/us/app/ollie-ai-family-meal-planner/id6480014476)
- [Ollie Google Play Store](https://play.google.com/store/apps/details?id=com.confabulation.ollie)
- [Ollie Privacy Policy](https://ollie.ai/privacy-policy/)
- [Ollie FAQ](https://ollie.ai/2024/03/28/frequently-asked-questions-about-ollie/)
- [Ollie About Page](https://ollie.ai/about/)
- [Ollie for Sports Families](https://ollie.ai/ollie-for-sports-families/)

### Media Coverage
- [The Washington Post - AI Meal Planning Apps](https://www.washingtonpost.com/technology/2025/08/21/ai-meal-planning-home-apps/)
- [Article Factory - Ollie Overview](https://article-factory.ai/news/ai-powered-meal-planning-app-ollie-aims-to-ease-family-cooking-stress)

### Technical References
- [GeekyAnts - AI Diet Planning App Development](https://geekyants.com/en-us/blog/how-to-build-a-diet-planning-app-in-the-usa-geekyants-ultimate-ai-nutrition-coaching-guide)
- [DEV Community - Cookbook Database Design](https://dev.to/amckean12/designing-a-relational-database-for-a-cookbook-4nj6)
- [Instacart Connect API Documentation](https://docs.instacart.com/connect/)
- [PMC - Mobile Apps for Family Food Provision](https://pmc.ncbi.nlm.nih.gov/articles/PMC6320405/)
- [PMC - Meal Planning App User Testing](https://pmc.ncbi.nlm.nih.gov/articles/PMC8140382/)

### Market Research
- [Statista - Nutrition Apps India](https://www.statista.com/outlook/hmo/digital-health/digital-fitness-well-being/health-wellness-coaching/nutrition-apps/india)
- [Intel Market Research - Meal Planning App Market](https://www.intelmarketresearch.com/meal-planning-app-346)
- [My Subscription Addiction - Best Meal Planning Apps](https://www.mysubscriptionaddiction.com/meal-planning-service-apps)

### Competitor References
- [Mealime](https://www.mealime.com/)
- [eMeals](https://emeals.com/)
- [Plan to Eat](https://www.plantoeat.com/)
- [Yummly](https://www.yummly.com/)

---

## Appendix A: Glossary

| Term | Definition |
|------|------------|
| **LLM** | Large Language Model - AI for text generation |
| **CV** | Computer Vision - AI for image recognition |
| **RAG** | Retrieval Augmented Generation - AI technique |
| **NLP** | Natural Language Processing |
| **CPTO** | Chief Product & Technology Officer |
| **DAU/WAU/MAU** | Daily/Weekly/Monthly Active Users |
| **NPS** | Net Promoter Score |
| **ARPU** | Average Revenue Per User |

---

## Appendix B: Research Methodology

This document was compiled through:

1. **Official Website Analysis**: ollie.ai and all subpages
2. **App Store Research**: iOS App Store and Google Play listings
3. **User Review Analysis**: App store reviews, ratings
4. **Competitor Analysis**: Feature comparison with alternatives
5. **Technical Research**: Architecture patterns for similar apps
6. **Market Research**: Industry reports and trends
7. **Privacy Policy Analysis**: Data collection and handling practices
8. **Media Coverage Review**: Washington Post, Forbes, etc.

---

*Document Created: January 2025*
*Last Updated: January 2025*
*Research conducted for building similar app for India market*
*Total Sections: 24 + 2 Appendices*
