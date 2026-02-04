# RasoiAI - Product Requirements Document (PRD)

## AI-Powered Meal Planning App for Indian Families

**Version**: 1.0
**Last Updated**: January 2025
**Status**: Requirements Finalized
**Reference**: Based on Ollie.ai research (`docs/research/Ollie App Research.md`)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Product Vision & Positioning](#2-product-vision--positioning)
3. [Target Audience](#3-target-audience)
4. [User Personas](#4-user-personas)
5. [Feature Comparison: Ollie vs RasoiAI](#5-feature-comparison-ollie-vs-rasoiai)
6. [Functional Requirements](#6-functional-requirements)
7. [Non-Functional Requirements](#7-non-functional-requirements)
8. [Technical Architecture](#8-technical-architecture)
9. [Database Schema](#9-database-schema)
10. [API Specifications](#10-api-specifications)
11. [UI/UX Requirements](#11-uiux-requirements)
12. [Localization Requirements](#12-localization-requirements)
13. [MVP Scope Definition](#13-mvp-scope-definition)
14. [Phase 2 Roadmap](#14-phase-2-roadmap)
15. [Success Metrics](#15-success-metrics)
16. [Appendices](#16-appendices)

---

## 1. Executive Summary

### 1.1 Product Overview

**RasoiAI** is an AI-powered meal planning application designed specifically for Indian families. The app generates personalized weekly meal plans based on family preferences, dietary restrictions, regional cuisines, and cultural considerations including festivals and fasting days.

### 1.2 Key Facts

| Attribute | Details |
|-----------|---------|
| **App Name** | RasoiAI (रसोई AI) |
| **Tagline** | "Ghar ki rasoi, AI ki madad se" / "Smart meal planning for Indian families" |
| **Platform** | Android Native (Kotlin + Jetpack Compose) |
| **Languages** | English + Hindi |
| **Target Market** | Pan-India (Tier 1, 2, 3 cities) |
| **Pricing** | Free (all features) |
| **Monetization** | Future monetization (user base first) |

### 1.3 Problem Statement

Indian families face unique meal planning challenges:
- **"Aaj kya banau?"** (What to cook today?) - Daily decision fatigue
- **Multi-generational dietary needs** - Kids, adults, and elders with different requirements
- **Festival and fasting complexity** - 30+ festivals with specific food requirements
- **Regional cuisine diversity** - 4 distinct food zones with different ingredients and styles
- **No existing solution** - Current apps are either US-focused (Ollie) or recipe-only (Tarla Dalal)

### 1.4 Solution

RasoiAI solves these problems through:
1. **AI-Generated Personalized Meal Plans** - Weekly plans tailored to family preferences
2. **Festival Intelligence** - Auto-suggested menus for Navratri, Diwali, Eid, Pongal, etc.
3. **Multi-generational Support** - One plan that works for kids, adults, and elders
4. **Comprehensive Dietary Filters** - Vegetarian, Jain, Sattvic, Fasting, Halal support
5. **Regional Cuisine Depth** - Authentic recipes from North, South, East, and West India
6. **WhatsApp Grocery Sharing** - One-tap share to kirana stores

---

## 2. Product Vision & Positioning

### 2.1 Vision Statement

> "To become the default meal planning assistant for every Indian household, making home cooking easier, healthier, and more connected to our cultural roots."

### 2.2 Mission Statement

> "Eliminate 'what to cook' stress for Indian families by providing AI-powered, culturally-aware meal planning that respects dietary diversity, celebrates festivals, and brings families together at the dinner table."

### 2.3 Positioning

| Aspect | Positioning |
|--------|-------------|
| **Category** | AI Meal Planning App |
| **Target** | Indian families (nuclear + joint) |
| **Differentiation** | Festival intelligence + Regional cuisine depth + Multi-generational support |
| **Competitor Alternative** | Unlike recipe apps (Tarla Dalal) that only show recipes, RasoiAI plans your entire week |
| **Price Position** | Free (vs Ollie's $10/month) |

### 2.4 Key Differentiators (Priority Order)

| Rank | Differentiator | Description | Uniqueness |
|------|----------------|-------------|------------|
| 1 | **Festival Intelligence** | Auto-suggests menus for 30+ Indian festivals, fasting modes | No competitor has this |
| 2 | **AI Personalization** | Learns family taste, improves over time | Ollie-level, localized for India |
| 3 | **Multi-generational Support** | Elder-friendly + kid-friendly in same plan | Unique for India |
| 4 | **Regional Cuisine Depth** | Authentic 4-zone recipes (N/S/E/W) | Better than any Indian app |
| 5 | **Dietary Complexity** | Jain, Sattvic, Vrat, Halal handled properly | Poorly done elsewhere |
| 6 | **WhatsApp-native Grocery** | Share list to kirana in one tap | Simple but practical |
| 7 | **Seasonal Awareness** | Mango season, monsoon foods, winter warmers | Unique feature |

---

## 3. Target Audience

### 3.1 Market Segment

| Attribute | Specification |
|-----------|---------------|
| **Geography** | Pan-India - Tier 1, Tier 2, Tier 3 cities |
| **Demographics** | Urban households with smartphones |
| **Family Size** | 3-8 members |
| **Family Type** | Nuclear families + Small joint families (with grandparents) |
| **Primary User** | Person responsible for meal planning (typically homemaker or working adult) |

### 3.2 Market Size (Estimated)

| Segment | Households | Addressable |
|---------|------------|-------------|
| Tier 1 (Metros) | ~25M | High |
| Tier 2 (Large cities) | ~40M | High |
| Tier 3 (Small cities) | ~50M | Medium |
| **Total Addressable** | ~115M households | |
| **Realistic Target (Year 1)** | 100K-500K users | |

### 3.3 User Characteristics

| Characteristic | Description |
|----------------|-------------|
| **Tech Comfort** | Comfortable with smartphone apps; uses WhatsApp daily |
| **Language** | Hindi + English (bilingual); may prefer Hindi UI |
| **Connectivity** | Variable; may have inconsistent internet in kitchen |
| **Pain Points** | Daily cooking decisions, managing diverse dietary needs, festival meal planning |
| **Current Solutions** | YouTube recipes, family WhatsApp groups, recipe apps, memory |

---

## 4. User Personas

### 4.1 Primary Persona: The Working Mother

| Attribute | Details |
|-----------|---------|
| **Name** | Priya Sharma, 34 |
| **Location** | Noida (Tier 1) |
| **Occupation** | IT Professional (Work from home) |
| **Family** | Husband (36), Son (8), Daughter (5), Mother-in-law (62) |
| **Languages** | Hindi, English |
| **Tech Savvy** | High - uses multiple apps daily |

**Daily Challenges:**
- Manages work calls while planning dinner
- Mother-in-law needs low-oil, easy-to-digest food
- Kids are picky, want different things
- Husband prefers non-veg twice a week
- Struggles during Navratri fasting (9 days of special cooking)

**Goals:**
- Reduce mental load of "what to cook"
- Keep everyone in the family happy with meals
- Cook healthier, reduce outside food
- Plan festival meals without stress

**Quote:** *"By 4 PM every day, I start panicking about dinner. I wish someone would just tell me what to cook."*

---

### 4.2 Secondary Persona: The Traditional Homemaker

| Attribute | Details |
|-----------|---------|
| **Name** | Sunita Devi, 48 |
| **Location** | Lucknow (Tier 2) |
| **Occupation** | Homemaker |
| **Family** | Husband (52), Son (24), Daughter-in-law (22), Mother (75) |
| **Languages** | Hindi (primary), basic English |
| **Tech Savvy** | Medium - uses WhatsApp, YouTube |

**Daily Challenges:**
- Cooks for joint family with different tastes
- Mother is diabetic, needs special meals
- Follows all fasts (Ekadashi, Karva Chauth, etc.)
- Wants to try new recipes but relies on YouTube
- Shares grocery list verbally with local kirana

**Goals:**
- Learn new recipes while keeping traditional ones
- Manage diabetic-friendly meals for mother
- Get organized grocery lists
- Reduce food waste

**Quote:** *"Mujhe naye recipes try karne hain, par sab ki pasand alag hai. Kaise manage karoon?"*

---

### 4.3 Tertiary Persona: The Health-Conscious Young Professional

| Attribute | Details |
|-----------|---------|
| **Name** | Arjun Reddy, 28 |
| **Location** | Hyderabad (Tier 1) |
| **Occupation** | Software Engineer |
| **Family** | Wife (26), planning for kids |
| **Languages** | Telugu, English, Hindi |
| **Tech Savvy** | Very High |

**Daily Challenges:**
- Both working, limited time to cook
- Want to eat healthy, reduce Swiggy orders
- Wife is vegetarian (Brahmin), he eats non-veg
- Trying to plan meals for the week on Sunday
- Miss home-cooked Andhra food

**Goals:**
- Meal prep on weekends
- Balance vegetarian and non-vegetarian in same week
- Track nutrition loosely
- Learn regional recipes (South Indian focus)

**Quote:** *"We spend ₹15K on Swiggy monthly. If we could just plan better, we'd cook more at home."*

---

### 4.4 Anti-Persona (Not Target User)

| Attribute | Details |
|-----------|---------|
| **Who** | Single college student, PG resident |
| **Why Not** | Single-person meals, extreme budget constraints, minimal cooking |
| **Who** | Fitness enthusiast needing macro tracking |
| **Why Not** | RasoiAI is family meal planning, not calorie counting (use HealthifyMe) |
| **Who** | Restaurant/cloud kitchen owner |
| **Why Not** | Commercial needs, bulk cooking (different product) |

---

## 5. Feature Comparison: Ollie vs RasoiAI

### 5.1 Feature Matrix

| Feature | Ollie (US) | RasoiAI (India) | Notes |
|---------|------------|-----------------|-------|
| **AI Meal Planning** | ✅ Weekly plans | ✅ Weekly plans | Core feature, same |
| **Recipe Generation** | ✅ AI-generated | ✅ AI-generated | Localized for Indian cuisine |
| **Family Profiles** | ✅ Basic | ✅ Enhanced | Age-based dietary needs |
| **Dietary Filters** | ✅ Western diets | ✅ Indian diets | Jain, Sattvic, Fasting, Halal |
| **Pantry Scanning** | ✅ Camera AI | ❌ Phase 2 | Deferred for MVP |
| **Grocery Integration** | ✅ Instacart, Amazon | ✅ WhatsApp Share | India-appropriate |
| **Chat Modifications** | ✅ Full chat | ❌ Phase 2 | Deferred for MVP |
| **Cooking Mode** | ✅ Screen stays on | ✅ Screen stays on | Same |
| **Offline Support** | ❌ Online only | ✅ Smart offline | Key differentiator |
| **Festival Calendar** | ❌ None | ✅ 30+ festivals | Key differentiator |
| **Regional Cuisines** | ❌ Generic American | ✅ 4 Indian zones | Key differentiator |
| **Multi-generational** | ❌ Basic family | ✅ Elder + kid support | Key differentiator |
| **Seasonal Awareness** | ❌ None | ✅ Indian seasons | Key differentiator |
| **Languages** | English only | English + Hindi | Localized |
| **Pricing** | $9.99/month | Free | Different model |
| **Platforms** | iOS + Android | Android only | MVP focus |

### 5.2 Features RasoiAI Skips (vs Ollie)

| Ollie Feature | RasoiAI Decision | Rationale |
|---------------|------------------|-----------|
| iOS App | Not in roadmap | 95% India is Android |
| Pantry Scanning | Phase 2 | High complexity, needs Indian ingredient training |
| AI Chat | Phase 2 | Can do basic swaps without full chat |
| Instacart Integration | Never | Not available in India |
| Gamification/Streaks | Phase 2 | Nice-to-have, not core |
| Recipe Import (URL) | Phase 2 | Complexity |

### 5.3 Features RasoiAI Adds (vs Ollie)

| New Feature | Description | Why Needed |
|-------------|-------------|------------|
| Festival Intelligence | 30+ festivals with auto-suggestions | Core Indian need |
| Fasting Mode | Navratri, Ekadashi, Shravan recipes | Religious practices |
| Regional Cuisine Zones | North/South/East/West India | Cuisine diversity |
| Elder-Friendly Tags | Low-oil, soft, diabetic-friendly | Joint families |
| WhatsApp Grocery | Share to kirana | India's shopping pattern |
| Hindi Language | Full Hindi UI + content | 55% of India |
| Offline Mode | Core features work offline | Connectivity issues |
| Seasonal Produce | Mango season, monsoon foods | Indian food culture |

---

## 6. Functional Requirements

### 6.1 User Onboarding (FR-ONB)

#### FR-ONB-001: User Registration
- User can sign up with Google account (OAuth)
- User can sign up with phone number (OTP)
- Email/password registration optional
- Collect: Name, preferred language (English/Hindi)

#### FR-ONB-002: Onboarding Quiz (5 Steps)

**Step 1: Household Setup**
- How many people cooking for? (1-8+)
- Add family members:
  - Name
  - Type: Adult / Child / Senior
  - Age (for children and seniors)
  - Individual dietary restrictions (optional)

**Step 2: Dietary Preferences**
- Primary diet type:
  - Vegetarian (no meat, fish, eggs)
  - Eggetarian (vegetarian + eggs)
  - Non-vegetarian (all foods)
- Special dietary needs (multi-select):
  - Jain (no root vegetables, no onion-garlic)
  - Sattvic / No onion-garlic
  - Vegan (no animal products)
  - Halal (for non-veg)
  - Diabetic-friendly
  - Low-oil / Heart-healthy
  - Gluten-free

**Step 3: Regional Cuisine Preferences**
- Primary cuisine zone:
  - North Indian (Punjabi, Mughlai, UP, Rajasthani)
  - South Indian (Tamil, Kerala, Andhra, Karnataka)
  - West Indian (Gujarati, Maharashtrian, Goan)
  - East Indian (Bengali, Odia, Bihari)
  - Mix of all
- Spice level: Mild / Medium / Spicy / Very Spicy
- Cuisine types liked (multi-select):
  - Traditional Indian
  - Indo-Chinese
  - Continental/Western
  - Street Food style

**Step 4: Disliked Ingredients**
- Search and add ingredients to avoid
- Common suggestions: Karela (bitter gourd), Baingan (eggplant), Lauki (bottle gourd), Turai, Bhindi

**Step 5: Cooking Preferences**
- Weekday cooking time: 15 / 30 / 45 / 60 minutes
- Weekend cooking time: 30 / 45 / 60 / 90+ minutes
- Busy days (quick meals needed): Select days
- Kitchen equipment available:
  - Gas stove (default)
  - Pressure cooker (default for India)
  - Microwave
  - OTG/Oven
  - Air Fryer
  - Mixer/Grinder (default for India)

#### FR-ONB-003: First Meal Plan Generation
- After quiz completion, generate first weekly plan
- Show loading animation: "RasoiAI aapke liye plan bana rahi hai..."
- Display plan within 30 seconds

---

### 6.2 Meal Planning (FR-MP)

#### FR-MP-001: Weekly Meal Plan Generation
- Generate 7-day meal plan based on preferences
- Include: Breakfast, Lunch, Dinner for each day
- Consider:
  - Dietary restrictions of all family members
  - Cooking time constraints per day
  - Ingredient reuse across week (reduce waste)
  - No protein repetition on consecutive days
  - Regional cuisine preferences
  - Seasonal ingredients

#### FR-MP-002: Meal Plan Display
- Calendar view showing week at a glance
- Each meal shows:
  - Recipe name (English + Hindi)
  - Cooking time
  - Cuisine type tag
  - Dietary tags (Veg, Jain, etc.)
- Expandable to see recipe preview
- Today's meals highlighted

#### FR-MP-003: Meal Swapping
- User can tap "Swap" on any meal
- System suggests 3-5 alternatives:
  - Same dietary restrictions
  - Similar cooking time
  - Different protein/main ingredient
- User can select alternative or request more options

#### FR-MP-004: Meal Skipping
- User can mark meal as "Skip"
- Options:
  - Eating out
  - Using leftovers
  - Fasting
- Grocery list updates automatically

#### FR-MP-005: Plan Regeneration
- User can regenerate entire week
- Regeneration respects:
  - Locked/favorite meals (kept as-is)
  - Updated preferences
  - Current pantry (Phase 2)

#### FR-MP-006: Leftover Management
- User can mark recipe as "Make extra"
- System suggests using leftovers next day
- Leftover meals marked differently in plan

---

### 6.3 Festival & Fasting Intelligence (FR-FEST)

#### FR-FEST-001: Festival Calendar
App maintains calendar of 30+ festivals:

| Festival | Type | Dates (Variable) | Food Focus |
|----------|------|------------------|------------|
| Makar Sankranti | Hindu | Jan 14 | Til-gur, khichdi |
| Pongal | Tamil | Jan 14-17 | Pongal, payasam |
| Republic Day | National | Jan 26 | Tricolor foods (optional) |
| Maha Shivaratri | Hindu | Feb/Mar | Fasting recipes |
| Holi | Hindu | Mar | Gujiya, thandai, puran poli |
| Ugadi | Telugu/Kannada | Mar/Apr | Pachadi, holige |
| Gudi Padwa | Marathi | Mar/Apr | Shrikhand, puran poli |
| Baisakhi | Punjabi/Sikh | Apr 13 | Kadhi, makki roti |
| Vishu | Kerala | Apr | Sadhya, vishu kani |
| Ramadan | Muslim | Variable | Iftar recipes, sehri |
| Eid ul-Fitr | Muslim | Variable | Biryani, seviyan, kebabs |
| Eid ul-Adha | Muslim | Variable | Meat dishes, biryani |
| Raksha Bandhan | Hindu | Aug | Sweets, brother's favorites |
| Janmashtami | Hindu | Aug | Fasting, prasad recipes |
| Onam | Kerala | Aug/Sep | Sadhya (26 dishes) |
| Ganesh Chaturthi | Marathi | Aug/Sep | Modak, ukdiche modak |
| Navratri | Hindu | Sep/Oct | 9-day fasting menu |
| Durga Puja | Bengali | Sep/Oct | Fish, sweets, feast |
| Dussehra | Hindu | Oct | Feast recipes |
| Karva Chauth | North Indian | Oct | Sargi, pre-fast meal |
| Diwali | Hindu | Oct/Nov | Sweets, snacks, feast |
| Bhai Dooj | Hindu | Nov | Sweets |
| Chhath Puja | Bihari | Nov | Thekua, prasad |
| Guru Nanak Jayanti | Sikh | Nov | Langar recipes |
| Christmas | Christian | Dec 25 | Cakes, roasts, sweets |

#### FR-FEST-002: Festival Awareness
- 7 days before major festival: Notification "Diwali in 7 days - start planning?"
- Auto-suggest festival-appropriate recipes during festival week
- Fasting mode auto-activates during known fasting periods (with user consent)

#### FR-FEST-003: Fasting Mode
- User can manually activate fasting mode
- Fasting types supported:
  - Navratri fast (no grains, specific ingredients allowed)
  - Ekadashi fast (no grains, no beans)
  - Monday/Thursday fast (flexible)
  - Shravan fast (no non-veg, specific restrictions)
  - Karva Chauth (pre-fast and post-fast meals)
  - Ramadan (sehri and iftar appropriate)
- Fasting recipes tagged and filtered automatically

#### FR-FEST-004: Seasonal Awareness
- System knows Indian seasons:
  - Summer (Mar-Jun): Light, cooling foods, mango recipes
  - Monsoon (Jul-Sep): Pakoras, chai-time snacks, warm foods
  - Winter (Oct-Feb): Rich foods, gajar halwa, sarson ka saag
- Seasonal ingredient suggestions in meal plans
- "Mango season specials" type collections when appropriate

---

### 6.4 Recipe Management (FR-REC)

#### FR-REC-001: Recipe Display
Each recipe includes:
- Recipe name (English + Hindi)
- High-quality image
- Cooking time (prep + cook)
- Difficulty level (Easy / Medium / Hard)
- Servings (adjustable based on family size)
- Dietary tags (Veg, Jain, Fasting-friendly, etc.)
- Cuisine tag (Punjabi, Tamil, etc.)
- Nutrition info (calories, protein, carbs, fat per serving)

#### FR-REC-002: Recipe Ingredients
- Ingredient list with quantities
- Quantities auto-scaled for family size
- Indian measurements supported:
  - Katori (bowl)
  - Chammach (spoon)
  - Cups, grams, ml
- Ingredient available in Hindi (धनिया, जीरा, हल्दी)
- "Add all to grocery" button

#### FR-REC-003: Recipe Instructions
- Step-by-step instructions
- Each step shows:
  - Clear instruction text
  - Ingredients used in this step
  - Time for this step (if applicable)
- Instructions in user's preferred language

#### FR-REC-004: Cooking Mode
- Full-screen cooking mode
- Large, readable text
- Step-by-step navigation (Previous / Next)
- Screen stays ON (wake lock)
- Timer integration for steps that need it
- Works fully offline (if recipe is cached)

#### FR-REC-005: Recipe Favorites
- User can favorite/save recipes
- Favorites organized in collections:
  - All Favorites
  - Custom collections (user-created)
- Favorited recipes prioritized in future plans (optional setting)

#### FR-REC-006: Recipe Scaling
- User can adjust servings (2-8 people)
- All ingredient quantities recalculated
- Cooking times may adjust for larger quantities

---

### 6.5 Grocery List Management (FR-GRO)

#### FR-GRO-001: Auto-Generated Grocery List
- Grocery list auto-generated from weekly meal plan
- Aggregates ingredients across all recipes
- Removes duplicates, combines quantities
- Organized by category:
  - Sabzi/Vegetables (सब्जी)
  - Fruits (फल)
  - Dairy (दूध/दही)
  - Meat/Fish (मांस/मछली) - if applicable
  - Grocery/Staples (किराना)
  - Spices (मसाले)
  - Others

#### FR-GRO-002: Grocery List Display
- Shows total items count
- Shows estimated cost range (if possible)
- Each item shows:
  - Ingredient name (English + Hindi)
  - Quantity needed
  - Checkbox to mark as purchased
- Items can be manually added
- Items can be removed

#### FR-GRO-003: WhatsApp Sharing
- One-tap "Share to WhatsApp" button
- Formats list as clean text message:
```
🛒 RasoiAI Grocery List
Week: Jan 20-26

🥬 SABZI (8 items)
• Pyaaz/Onion - 1 kg
• Tamatar/Tomato - 500g
• Aloo/Potato - 1 kg
• Shimla Mirch - 4 pcs
...

🥛 DAIRY (3 items)
• Doodh/Milk - 2 L
• Dahi/Curd - 500g
• Paneer - 400g

🫘 KIRANA (5 items)
...

Generated by RasoiAI 🍳
```
- User can share to any WhatsApp contact (kirana, family member)
- Deep link opens WhatsApp with pre-filled message

#### FR-GRO-004: Grocery List Updates
- When meal plan changes, grocery list updates automatically
- User notified of changes
- Checked-off items remain checked

#### FR-GRO-005: Print/Copy Options
- Copy list as plain text
- Generate print-friendly PDF (optional, Phase 1.5)

---

### 6.6 Family Profile Management (FR-FAM)

#### FR-FAM-001: Family Member Profiles
- Add up to 8 family members
- Each member has:
  - Name
  - Type: Child / Adult / Senior
  - Age (for children: affects portion size; for seniors: dietary suggestions)
  - Individual dietary restrictions
  - Health conditions (optional):
    - Diabetic
    - High BP
    - High cholesterol
    - Lactose intolerant
    - Other allergies

#### FR-FAM-002: Multi-generational Intelligence
- Meal plans consider all family members' needs
- Example:
  - If senior is diabetic: Suggest low-sugar dessert alternatives
  - If child is 5 years old: Suggest milder spice versions
  - If someone is Jain: Main meal is Jain-friendly OR separate option suggested

#### FR-FAM-003: Per-Member Dietary Overrides
- Family-level: Non-vegetarian
- Member-level: Mother-in-law is pure vegetarian
- System handles mixed families:
  - Suggests veg + non-veg options for same meal
  - Or suggests dishes that can be made both ways

---

### 6.7 User Preferences & Settings (FR-SET)

#### FR-SET-001: Language Settings
- App language: English / Hindi
- Recipe language: English / Hindi / Bilingual
- Ingredient names: English / Hindi / Both

#### FR-SET-002: Notification Settings
- Daily dinner reminder (time configurable, default 4 PM)
- Weekly plan ready notification
- Festival reminders (on/off)
- Fasting day reminders (on/off)

#### FR-SET-003: Dietary Preferences (Editable)
- All onboarding preferences can be edited anytime
- Changes trigger plan regeneration prompt

#### FR-SET-004: Account Settings
- Edit profile (name, phone, email)
- Change password (if applicable)
- Delete account
- Export my data

---

## 7. Non-Functional Requirements

### 7.1 Performance (NFR-PERF)

| Requirement | Target | Notes |
|-------------|--------|-------|
| App launch time | < 3 seconds | Cold start on mid-range device |
| Meal plan generation | < 30 seconds | First plan; regeneration < 15s |
| Recipe load time | < 2 seconds | With image |
| Offline recipe access | < 1 second | From local cache |
| API response time | < 500ms | 95th percentile |
| App size | < 50 MB | APK size; keeps tier-3 users |

### 7.2 Reliability (NFR-REL)

| Requirement | Target |
|-------------|--------|
| Uptime | 99.5% |
| Crash-free sessions | 99.5% |
| API success rate | 99% |
| Data sync reliability | No data loss |

### 7.3 Scalability (NFR-SCALE)

| Requirement | Target |
|-------------|--------|
| Concurrent users | 10,000 (Year 1) |
| Meal plans/day | 50,000 |
| Database size | 1M users |

### 7.4 Security (NFR-SEC)

| Requirement | Implementation |
|-------------|----------------|
| Data in transit | HTTPS/TLS 1.3 |
| Data at rest | Encrypted |
| Authentication | JWT tokens, OAuth 2.0 |
| Password storage | Bcrypt hashed |
| PII protection | Minimal collection, encrypted |
| Session management | Secure token refresh |

### 7.5 Offline Support (NFR-OFF)

| Feature | Offline Capability |
|---------|-------------------|
| View current meal plan | ✅ Full |
| View cached recipes | ✅ Full |
| Cooking mode | ✅ Full |
| View grocery list | ✅ Full |
| Check off grocery items | ✅ Syncs when online |
| Generate new plan | ❌ Requires internet |
| Change preferences | ❌ Requires internet |
| WhatsApp share | ✅ Opens WhatsApp |

### 7.6 Compatibility (NFR-COMPAT)

| Requirement | Target |
|-------------|--------|
| Android version | 8.0+ (API 26+) |
| Screen sizes | Phone (5"-7"), small tablets |
| Orientations | Portrait primary |
| Network | 3G minimum, optimized for 4G |

---

## 8. Technical Architecture

### 8.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CLIENT LAYER                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                    Android App (Kotlin)                          │   │
│   │                    Jetpack Compose UI                            │   │
│   ├─────────────────────────────────────────────────────────────────┤   │
│   │  • Onboarding Flow      • Meal Plan Display    • Recipe Detail   │   │
│   │  • Cooking Mode         • Grocery List         • Settings        │   │
│   │  • Offline Cache (Room) • Local Preferences    • Push Handler    │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                      │
│                                    │ HTTPS/REST                          │
└────────────────────────────────────┼────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          API GATEWAY LAYER                                │
├─────────────────────────────────────────────────────────────────────────┤
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                    API Gateway (AWS/GCP)                         │   │
│   │  • Rate Limiting    • Auth Validation    • Request Routing       │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                   Authentication Service                          │   │
│   │         Google OAuth  |  Phone OTP (Firebase Auth)               │   │
│   └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         BACKEND SERVICES                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐       │
│  │   USER SERVICE   │  │   MEAL SERVICE   │  │  RECIPE SERVICE  │       │
│  ├──────────────────┤  ├──────────────────┤  ├──────────────────┤       │
│  │ • User Profiles  │  │ • Plan Generation│  │ • Recipe Storage │       │
│  │ • Family Members │  │ • Plan CRUD      │  │ • Recipe Search  │       │
│  │ • Preferences    │  │ • Meal Swapping  │  │ • Favorites      │       │
│  │ • Auth           │  │ • Festival Logic │  │ • Scaling        │       │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘       │
│                                                                           │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐       │
│  │ GROCERY SERVICE  │  │  FESTIVAL SERVICE│  │ NOTIFICATION SVC │       │
│  ├──────────────────┤  ├──────────────────┤  ├──────────────────┤       │
│  │ • List Generation│  │ • Festival Calendar│ │ • Push (FCM)    │       │
│  │ • WhatsApp Format│  │ • Fasting Rules   │  │ • Scheduling    │       │
│  │ • Aggregation    │  │ • Seasonal Data   │  │ • Preferences   │       │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘       │
│                                                                           │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            AI/ML LAYER                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                    LLM Integration (Claude/GPT)                  │   │
│   ├─────────────────────────────────────────────────────────────────┤   │
│   │  • Meal Plan Generation    • Recipe Customization               │   │
│   │  • Preference Learning     • Natural Language Processing        │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                                                           │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                    Recipe Embedding / Vector DB                  │   │
│   │  • Recipe similarity search    • Personalization vectors        │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                                                           │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            DATA LAYER                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐       │
│  │   PostgreSQL     │  │     Redis        │  │   Cloud Storage  │       │
│  │  (Primary DB)    │  │    (Cache)       │  │  (S3/GCS)        │       │
│  ├──────────────────┤  ├──────────────────┤  ├──────────────────┤       │
│  │ • Users          │  │ • Session cache  │  │ • Recipe images  │       │
│  │ • Preferences    │  │ • Meal plan cache│  │ • User uploads   │       │
│  │ • Meal Plans     │  │ • Recipe cache   │  │                  │       │
│  │ • Recipes        │  │ • Rate limiting  │  │                  │       │
│  │ • Families       │  │                  │  │                  │       │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘       │
│                                                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

### 8.2 Technology Stack

| Layer | Technology | Rationale |
|-------|------------|-----------|
| **Android App** | Kotlin + Jetpack Compose | Modern Android standard, declarative UI |
| **Local DB** | Room (SQLite) | Offline caching, Android standard |
| **State Management** | ViewModel + StateFlow | Jetpack recommended |
| **Networking** | Retrofit + OkHttp | Industry standard |
| **Image Loading** | Coil | Kotlin-first, lightweight |
| **DI** | Hilt | Google recommended for Android |
| **Backend** | Node.js (NestJS) OR Python (FastAPI) | Rapid development |
| **Primary DB** | PostgreSQL | Relational data, reliable |
| **Cache** | Redis | Session, caching |
| **Auth** | Firebase Auth | Phone OTP, Google OAuth |
| **Push** | Firebase Cloud Messaging | Android notifications |
| **Storage** | AWS S3 / GCS | Recipe images |
| **LLM** | Claude API / OpenAI GPT-4 | Meal plan generation |
| **Hosting** | AWS / GCP | Scalability |
| **CI/CD** | GitHub Actions | Automation |

### 8.3 Android App Architecture

```
app/
├── data/
│   ├── local/
│   │   ├── RasoiDatabase.kt (Room DB)
│   │   ├── dao/ (MealPlanDao, RecipeDao, etc.)
│   │   └── entity/ (Local entities)
│   ├── remote/
│   │   ├── api/ (Retrofit interfaces)
│   │   └── dto/ (API data classes)
│   └── repository/ (Repository implementations)
├── domain/
│   ├── model/ (Domain models)
│   ├── repository/ (Repository interfaces)
│   └── usecase/ (Business logic)
├── presentation/
│   ├── onboarding/
│   ├── home/
│   ├── mealplan/
│   ├── recipe/
│   ├── grocery/
│   ├── settings/
│   └── common/ (Shared composables)
├── di/ (Hilt modules)
└── util/ (Extensions, helpers)
```

---

## 9. Database Schema

### 9.1 Core Tables

```sql
-- Users Table
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    phone_number VARCHAR(15) UNIQUE,
    email VARCHAR(255) UNIQUE,
    name VARCHAR(100) NOT NULL,
    auth_provider ENUM('google', 'phone', 'email') NOT NULL,
    preferred_language ENUM('en', 'hi') DEFAULT 'en',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Family Members Table
CREATE TABLE family_members (
    member_id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    member_type ENUM('child', 'adult', 'senior') NOT NULL,
    age INT,
    dietary_restrictions JSONB DEFAULT '[]',
    health_conditions JSONB DEFAULT '[]',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Preferences Table
CREATE TABLE user_preferences (
    preference_id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    household_size INT NOT NULL,
    primary_diet ENUM('vegetarian', 'eggetarian', 'non_vegetarian') NOT NULL,
    special_diets JSONB DEFAULT '[]', -- ['jain', 'sattvic', 'halal', etc.]
    cuisine_zones JSONB DEFAULT '[]', -- ['north', 'south', 'east', 'west']
    spice_level ENUM('mild', 'medium', 'spicy', 'very_spicy') DEFAULT 'medium',
    weekday_cooking_time INT DEFAULT 30, -- minutes
    weekend_cooking_time INT DEFAULT 60, -- minutes
    busy_days JSONB DEFAULT '[]', -- [1, 3, 5] for Mon, Wed, Fri
    kitchen_equipment JSONB DEFAULT '["gas_stove", "pressure_cooker", "mixer"]',
    disliked_ingredients JSONB DEFAULT '[]',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Recipes Table
CREATE TABLE recipes (
    recipe_id UUID PRIMARY KEY,
    name_en VARCHAR(200) NOT NULL,
    name_hi VARCHAR(200),
    description_en TEXT,
    description_hi TEXT,
    cuisine_zone ENUM('north', 'south', 'east', 'west', 'fusion') NOT NULL,
    cuisine_type VARCHAR(50), -- 'punjabi', 'tamil', 'bengali', etc.
    prep_time INT NOT NULL, -- minutes
    cook_time INT NOT NULL, -- minutes
    total_time INT GENERATED ALWAYS AS (prep_time + cook_time) STORED,
    servings INT DEFAULT 4,
    difficulty ENUM('easy', 'medium', 'hard') DEFAULT 'easy',
    dietary_tags JSONB DEFAULT '[]', -- ['vegetarian', 'jain', 'fasting', etc.]
    meal_type JSONB DEFAULT '[]', -- ['breakfast', 'lunch', 'dinner', 'snack']
    season_tags JSONB DEFAULT '[]', -- ['summer', 'monsoon', 'winter', 'all']
    festival_tags JSONB DEFAULT '[]', -- ['navratri', 'diwali', 'holi', etc.]
    calories INT,
    protein DECIMAL(5,2),
    carbs DECIMAL(5,2),
    fat DECIMAL(5,2),
    image_url VARCHAR(500),
    is_ai_generated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Recipe Ingredients Table
CREATE TABLE recipe_ingredients (
    id UUID PRIMARY KEY,
    recipe_id UUID REFERENCES recipes(recipe_id) ON DELETE CASCADE,
    ingredient_name_en VARCHAR(100) NOT NULL,
    ingredient_name_hi VARCHAR(100),
    quantity DECIMAL(10,2) NOT NULL,
    unit VARCHAR(30) NOT NULL, -- 'gram', 'cup', 'katori', 'piece', etc.
    category ENUM('vegetable', 'fruit', 'dairy', 'meat', 'grocery', 'spice', 'other') NOT NULL,
    is_optional BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0
);

-- Recipe Steps Table
CREATE TABLE recipe_steps (
    step_id UUID PRIMARY KEY,
    recipe_id UUID REFERENCES recipes(recipe_id) ON DELETE CASCADE,
    step_number INT NOT NULL,
    instruction_en TEXT NOT NULL,
    instruction_hi TEXT,
    duration_minutes INT,
    UNIQUE(recipe_id, step_number)
);

-- Meal Plans Table
CREATE TABLE meal_plans (
    plan_id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    status ENUM('active', 'archived') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, week_start_date)
);

-- Meal Plan Items Table
CREATE TABLE meal_plan_items (
    item_id UUID PRIMARY KEY,
    plan_id UUID REFERENCES meal_plans(plan_id) ON DELETE CASCADE,
    recipe_id UUID REFERENCES recipes(recipe_id),
    day_of_week INT NOT NULL, -- 1-7 (Monday-Sunday)
    meal_type ENUM('breakfast', 'lunch', 'dinner', 'snack') NOT NULL,
    status ENUM('planned', 'skipped', 'completed', 'leftover') DEFAULT 'planned',
    is_locked BOOLEAN DEFAULT FALSE,
    notes TEXT,
    UNIQUE(plan_id, day_of_week, meal_type)
);

-- Grocery Lists Table
CREATE TABLE grocery_lists (
    list_id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    plan_id UUID REFERENCES meal_plans(plan_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Grocery List Items Table
CREATE TABLE grocery_list_items (
    item_id UUID PRIMARY KEY,
    list_id UUID REFERENCES grocery_lists(list_id) ON DELETE CASCADE,
    ingredient_name_en VARCHAR(100) NOT NULL,
    ingredient_name_hi VARCHAR(100),
    quantity DECIMAL(10,2) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    category ENUM('vegetable', 'fruit', 'dairy', 'meat', 'grocery', 'spice', 'other') NOT NULL,
    is_checked BOOLEAN DEFAULT FALSE,
    is_manual BOOLEAN DEFAULT FALSE, -- manually added by user
    sort_order INT DEFAULT 0
);

-- User Favorites Table
CREATE TABLE user_favorites (
    favorite_id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    recipe_id UUID REFERENCES recipes(recipe_id) ON DELETE CASCADE,
    collection_name VARCHAR(100) DEFAULT 'Favorites',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, recipe_id)
);

-- Festival Calendar Table (Reference Data)
CREATE TABLE festivals (
    festival_id UUID PRIMARY KEY,
    name_en VARCHAR(100) NOT NULL,
    name_hi VARCHAR(100),
    festival_type ENUM('hindu', 'muslim', 'christian', 'sikh', 'jain', 'national', 'regional') NOT NULL,
    regions JSONB DEFAULT '[]', -- ['north', 'south', 'all', etc.]
    date_type ENUM('fixed', 'lunar', 'variable') NOT NULL,
    fixed_date VARCHAR(10), -- 'MM-DD' for fixed dates
    requires_fasting BOOLEAN DEFAULT FALSE,
    fasting_type VARCHAR(50),
    food_focus TEXT,
    recipes_tags JSONB DEFAULT '[]'
);
```

---

## 10. API Specifications

### 10.1 Authentication APIs

```
POST   /api/v1/auth/google          - Google OAuth login
POST   /api/v1/auth/phone/send-otp  - Send OTP to phone
POST   /api/v1/auth/phone/verify    - Verify OTP and login
POST   /api/v1/auth/refresh         - Refresh JWT token
POST   /api/v1/auth/logout          - Logout user
```

### 10.2 User APIs

```
GET    /api/v1/users/me             - Get current user profile
PUT    /api/v1/users/me             - Update user profile
DELETE /api/v1/users/me             - Delete account

GET    /api/v1/users/preferences    - Get user preferences
PUT    /api/v1/users/preferences    - Update preferences
POST   /api/v1/users/onboarding     - Complete onboarding

GET    /api/v1/users/family         - Get family members
POST   /api/v1/users/family         - Add family member
PUT    /api/v1/users/family/{id}    - Update family member
DELETE /api/v1/users/family/{id}    - Remove family member
```

### 10.3 Meal Plan APIs

```
GET    /api/v1/meal-plans           - Get all meal plans
GET    /api/v1/meal-plans/current   - Get current week's plan
POST   /api/v1/meal-plans/generate  - Generate new meal plan
PUT    /api/v1/meal-plans/{id}      - Update meal plan
DELETE /api/v1/meal-plans/{id}      - Delete meal plan

GET    /api/v1/meal-plans/{id}/items           - Get plan items
PUT    /api/v1/meal-plans/{id}/items/{itemId}  - Update plan item
POST   /api/v1/meal-plans/{id}/items/{itemId}/swap  - Get swap suggestions
POST   /api/v1/meal-plans/{id}/items/{itemId}/skip  - Skip meal
POST   /api/v1/meal-plans/{id}/items/{itemId}/lock  - Lock/unlock meal
```

### 10.4 Recipe APIs

```
GET    /api/v1/recipes              - Search/list recipes
GET    /api/v1/recipes/{id}         - Get recipe detail
GET    /api/v1/recipes/{id}/scale   - Get scaled ingredients

GET    /api/v1/recipes/favorites    - Get user favorites
POST   /api/v1/recipes/favorites    - Add to favorites
DELETE /api/v1/recipes/favorites/{recipeId}  - Remove from favorites

GET    /api/v1/recipes/collections  - Get user collections
POST   /api/v1/recipes/collections  - Create collection
```

### 10.5 Grocery APIs

```
GET    /api/v1/grocery              - Get current grocery list
POST   /api/v1/grocery/generate     - Generate from meal plan
PUT    /api/v1/grocery/items/{id}   - Update item (check/uncheck)
POST   /api/v1/grocery/items        - Add manual item
DELETE /api/v1/grocery/items/{id}   - Remove item
GET    /api/v1/grocery/whatsapp     - Get WhatsApp-formatted text
```

### 10.6 Festival APIs

```
GET    /api/v1/festivals            - Get festival calendar
GET    /api/v1/festivals/upcoming   - Get upcoming festivals (next 30 days)
GET    /api/v1/festivals/{id}/recipes  - Get festival recipes
POST   /api/v1/fasting/activate     - Activate fasting mode
POST   /api/v1/fasting/deactivate   - Deactivate fasting mode
```

---

## 11. UI/UX Requirements

### 11.1 Design Principles

| Principle | Description |
|-----------|-------------|
| **Simplicity** | Minimal clicks to complete tasks; clear navigation |
| **Bilingual** | All text available in English and Hindi |
| **Offline-first** | Core features work without internet; clear offline indicators |
| **Accessibility** | Large touch targets, readable fonts, good contrast |
| **Indian Context** | Familiar patterns, local imagery, culturally appropriate |

### 11.2 Key Screens

1. **Splash Screen** - App logo, branding
2. **Login/Signup** - Google + Phone OTP options
3. **Onboarding Quiz** - 5-step questionnaire
4. **Home / Meal Plan** - Weekly calendar view
5. **Recipe Detail** - Full recipe with ingredients, steps
6. **Cooking Mode** - Step-by-step, screen-on
7. **Grocery List** - Categorized list, WhatsApp share
8. **Favorites** - Saved recipes
9. **Profile / Settings** - Family, preferences, language

### 11.3 Design Specifications

| Element | Specification |
|---------|---------------|
| **Primary Color** | Orange/Saffron (#FF6B35) - Indian, appetizing |
| **Secondary Color** | Green (#2ECC71) - Fresh, vegetarian |
| **Typography** | Noto Sans (supports Hindi) |
| **Min Touch Target** | 48dp x 48dp |
| **Font Size (Body)** | 16sp minimum |
| **Font Size (Hindi)** | 18sp minimum (for readability) |

### 11.4 Offline Indicators

- Banner at top when offline: "ऑफलाइन मोड / Offline Mode"
- Grayed-out features that require internet
- Sync indicator when back online

---

## 12. Localization Requirements

### 12.1 Language Support

| Element | English | Hindi |
|---------|---------|-------|
| App UI | ✅ | ✅ |
| Recipe names | ✅ | ✅ |
| Ingredient names | ✅ | ✅ (local names) |
| Instructions | ✅ | ✅ |
| Notifications | ✅ | ✅ |
| Error messages | ✅ | ✅ |

### 12.2 Hindi Ingredient Names (Examples)

| English | Hindi | Transliteration |
|---------|-------|-----------------|
| Coriander | धनिया | Dhaniya |
| Cumin | जीरा | Jeera |
| Turmeric | हल्दी | Haldi |
| Onion | प्याज़ | Pyaaz |
| Tomato | टमाटर | Tamatar |
| Potato | आलू | Aloo |
| Ginger | अदरक | Adrak |
| Garlic | लहसुन | Lahsun |
| Yogurt | दही | Dahi |
| Paneer | पनीर | Paneer |

### 12.3 Measurement Localization

| Standard | Indian Equivalent |
|----------|-------------------|
| Cup | कप / Katori |
| Tablespoon | बड़ा चम्मच |
| Teaspoon | छोटा चम्मच |
| Grams | ग्राम |
| Liters | लीटर |
| Piece | पीस |

---

## 13. MVP Scope Definition

### 13.1 MVP Features (Must Have)

| Feature | Priority | Complexity |
|---------|----------|------------|
| User registration (Google + Phone) | P0 | Medium |
| Onboarding quiz (5 steps) | P0 | Medium |
| AI weekly meal plan generation | P0 | High |
| Recipe display (detail + ingredients) | P0 | Medium |
| Cooking mode (step-by-step) | P0 | Low |
| Meal swapping | P0 | Medium |
| Grocery list generation | P0 | Medium |
| WhatsApp share for grocery | P0 | Low |
| Family member profiles | P0 | Medium |
| Dietary filters (Veg/Jain/Fasting/Halal) | P0 | Medium |
| Festival calendar (15+ festivals) | P0 | Medium |
| Fasting mode activation | P0 | Medium |
| Recipe favorites | P0 | Low |
| Offline support (view plan, recipes, grocery) | P0 | High |
| English + Hindi language | P0 | Medium |
| Settings / Preferences | P0 | Low |

### 13.2 Post-MVP Features (Phase 2)

| Feature | Priority | Notes |
|---------|----------|-------|
| Pantry scanning (Camera AI) | P1 | Requires ML model for Indian ingredients |
| AI chat for recipe modifications | P1 | Natural language recipe changes |
| Gamification (streaks, achievements) | P2 | Engagement features |
| Regional languages (Tamil, Telugu, etc.) | P1 | Based on user demand |
| Grocery platform integrations | P2 | Blinkit, BigBasket APIs |
| Recipe import (URL/photo) | P2 | User-submitted recipes |
| Nutrition tracking | P2 | Calorie/macro goals |
| Meal rating and learning | P1 | Improve AI over time |
| Social sharing | P3 | Share recipes with friends |
| Web app | P3 | Based on demand |

### 13.3 MVP Timeline Estimate

| Phase | Tasks | Duration |
|-------|-------|----------|
| Design | UI/UX design, prototyping | 4-6 weeks |
| Backend Setup | DB, APIs, Auth | 4-6 weeks |
| Recipe Database | Curate 500+ initial recipes | 4-6 weeks (parallel) |
| Android App Core | Home, recipes, grocery | 6-8 weeks |
| AI Integration | LLM meal planning | 4-6 weeks |
| Festival System | Calendar, fasting logic | 2-3 weeks |
| Offline Support | Room caching, sync | 2-3 weeks |
| Testing | QA, beta testing | 3-4 weeks |
| **Total Estimate** | | **5-7 months** |

---

## 14. Phase 2 Roadmap

### 14.1 Phase 2A: AI Enhancement (Post-MVP + 3 months)

- AI chat for recipe modifications
- Meal rating system
- Personalization improvements (learn from ratings)
- Recipe recommendations

### 14.2 Phase 2B: Pantry & Scanning (Post-MVP + 6 months)

- Camera-based pantry scanning
- Indian ingredient recognition model
- "Cook with what you have" feature
- Expiry tracking

### 14.3 Phase 2C: Language Expansion (Post-MVP + 6-9 months)

Based on user demand:
1. Tamil
2. Telugu
3. Marathi
4. Bengali
5. Gujarati

### 14.4 Phase 2D: Integrations (Post-MVP + 9-12 months)

- Grocery platform APIs (Blinkit, BigBasket, JioMart)
- Smart appliance integration
- Health app integration

---

## 15. Success Metrics

### 15.1 Acquisition Metrics

| Metric | Target (Year 1) |
|--------|-----------------|
| Total Downloads | 100,000 |
| Monthly Active Users (MAU) | 30,000 |
| Organic vs Paid Ratio | 60% organic |

### 15.2 Engagement Metrics

| Metric | Target |
|--------|--------|
| Weekly Active Users / MAU | 50%+ |
| Daily Active Users / MAU | 25%+ |
| Meal plans generated / week | 20,000 |
| Avg. session duration | 4+ minutes |
| Recipes viewed / user / week | 10+ |

### 15.3 Retention Metrics

| Metric | Target |
|--------|--------|
| Day 1 retention | 40% |
| Day 7 retention | 25% |
| Day 30 retention | 15% |

### 15.4 Quality Metrics

| Metric | Target |
|--------|--------|
| App Store rating | 4.3+ stars |
| Crash-free sessions | 99.5% |
| API success rate | 99% |

### 15.5 Feature Usage Metrics

| Feature | Target Usage |
|---------|--------------|
| Onboarding completion rate | 70%+ |
| Meal plan generation (first week) | 80% of new users |
| Grocery list usage | 50% of active users |
| WhatsApp share usage | 30% of active users |
| Fasting mode activation | 20% during Navratri |
| Recipe favorites saved | 5+ per active user |

---

## 16. Appendices

### 16.1 Glossary

| Term | Definition |
|------|------------|
| **Rasoi** | Kitchen (Hindi) |
| **Kirana** | Local grocery store |
| **Sattvic** | Pure vegetarian diet without onion/garlic (Hindu tradition) |
| **Jain** | Diet without root vegetables, onion, garlic |
| **Navratri** | 9-day Hindu festival with fasting |
| **Ekadashi** | Bi-monthly fasting day (11th day of lunar cycle) |
| **Sadhya** | Traditional Kerala feast (26 dishes) |
| **Vrat** | Religious fast |
| **Halal** | Permissible food under Islamic law |

### 16.2 Competitor Reference

| App | Strengths | Weaknesses | RasoiAI Opportunity |
|-----|-----------|------------|---------------------|
| Ollie.ai | AI planning, UX | US-only, expensive, no Indian food | Localized AI planning |
| Tarla Dalal | Recipe database, trust | No planning, outdated UI | AI planning layer |
| HealthifyMe | Nutrition tracking | Fitness-focused, not family | Family meal planning |
| Cookpad | Community recipes | No AI, no planning | AI curation |

### 16.3 References

- Ollie.ai Research: `docs/research/Ollie App Research.md`
- Indian food culture research: ICMR dietary guidelines
- Festival calendar: drikpanchang.com
- Market data: Statista India nutrition apps report

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | Jan 2025 | Requirements gathered via Claude Code | Initial PRD |

---

*This document serves as the single source of truth for RasoiAI product development.*
