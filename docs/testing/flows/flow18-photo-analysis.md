# Flow 18: Food Photo Analysis

## Metadata
- **Flow Name:** `photo-analysis`
- **Goal:** Test food photo analysis — upload image, identify food, add ingredients to pantry
- **Preconditions:** Backend running with Gemini Vision API key configured (Phase 5 feature)
- **Estimated Duration:** 5-8 minutes
- **Screens Covered:** Pantry, Recipe Detail (optional)
- **Depends On:** Flow 01 (authenticated user), Phase 5 (photo analysis endpoint)
- **State Produced:** New pantry items from photo analysis

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] User authenticated
- [ ] Backend running with `GOOGLE_AI_API_KEY` configured
- [ ] Test food image available (`test-food-photo.jpg` in test assets)
- [ ] Photo analysis endpoint `/api/v1/pantry/analyze-photo` implemented
- [ ] ADB connected to emulator

## Test User Persona

Uses existing Sharma family data. Focus is on photo analysis, not user preferences.

## Steps

### Phase A: Prepare Test Image (Steps 1-3)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| A1 | Push test food image to emulator | `adb push test-food-photo.jpg /sdcard/DCIM/test-food.jpg` | API | — | — |
| A2 | Broadcast media scanner | `adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/DCIM/test-food.jpg` | API | — | — |
| A3 | Verify image available in gallery | `adb shell ls -l /sdcard/DCIM/test-food.jpg` shows non-zero size | API | — | HARD |

### Phase B: Navigate to Pantry (Steps 4-6)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| B1 | Navigate: Profile icon → Settings → scroll to "Pantry" | Pantry link visible | UI | — | — |
| B2 | Tap "Pantry" | Pantry screen loads | UI | `flow18_pantry.png` | — |
| B3 | Verify Camera/Gallery buttons exist | content-desc "Camera" or "Scan" + "Gallery" visible in XML | UI | — | HARD |

### Phase C: Select Image from Gallery (Steps 7-10)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| C1 | Tap "Gallery" button | Gallery/file picker opens | UI | `flow18_gallery_picker.png` | — |
| C2 | Navigate to DCIM folder | `test-food.jpg` visible in gallery | UI | — | — |
| C3 | Tap `test-food.jpg` image | Image selected, gallery closes | UI | — | — |
| C4 | Wait for photo analysis (up to 10s) | Loading indicator appears | UI | `flow18_analyzing.png` | — |

### Phase D: Verify AI Analysis Results (Steps 11-15)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| D1 | Wait for analysis completion | Analysis results sheet/dialog appears | UI | `flow18_analysis_results.png` | HARD |
| D2 | Verify identified ingredients listed | At least 2 ingredients identified (e.g., "Tomatoes", "Onions", "Potatoes") | UI | — | HARD |
| D3 | Verify confidence scores shown (optional) | Confidence % or labels visible per ingredient | UI | — | — |
| D4 | Verify "Add to Pantry" button exists | Button visible in results sheet | UI | — | HARD |
| D5 | Verify "Edit" or "Select" option exists | User can deselect unwanted ingredients | UI | — | — |

### Phase E: Add Ingredients to Pantry (Steps 16-18)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| E1 | Deselect one ingredient (if UI allows) | Checkbox/toggle unchecked for one item | UI | `flow18_deselect.png` | — |
| E2 | Tap "Add to Pantry" button | Ingredients added to pantry, dialog dismisses | UI | — | HARD |
| E3 | Verify pantry list updated | New items appear in pantry list | UI | `flow18_pantry_updated.png` | HARD |

### Backend API Cross-Validation: Pantry Items Persisted

```bash
# Verify ingredients added to pantry on backend
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/pantry | \
  python -c "
import sys, json
d = json.load(sys.stdin)
items = d if isinstance(d, list) else d.get('items', [])
print(f'Total pantry items: {len(items)}')
# Verify at least 2 items added from photo (adjust based on test image)
if len(items) >= 2:
    print('Pantry items added -> PASS')
    for item in items[-2:]:  # Last 2 items added
        print(f'  - {item.get(\"name\", \"unknown\")}')
else:
    print('WARNING: Expected at least 2 items from photo analysis')
"
```

### Phase F: Contradictions C40-C42 (Steps 19-25)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| F1 | **C40:** Upload non-food image (e.g., landscape photo) | Push `test-landscape.jpg` via ADB, select from gallery | API | — | — |
| F2 | Tap "Gallery" → select non-food image | Analysis starts | UI | — | — |
| F3 | Wait for analysis | Error message: "No food detected" or "Could not identify ingredients" | UI | `flow18_c40_nonfood.png` | HARD |
| F4 | Dismiss error | Return to Pantry, no items added | UI | — | HARD |
| F5 | **C41:** Upload very low-quality/blurry food image | Push `test-blurry-food.jpg`, select from gallery | API | — | — |
| F6 | Wait for analysis | Either identifies with low confidence OR error "Image quality too low" | UI | `flow18_c41_blurry.png` | — |
| F7 | Dismiss results | Return to Pantry | UI | — | — |
| F8 | **C42:** Upload image with 10+ ingredients (complex dish) | Push `test-complex-dish.jpg` (e.g., thali), select | API | — | — |
| F9 | Wait for analysis (may take longer) | Identifies multiple ingredients (at least 5) | UI | `flow18_c42_complex.png` | — |
| F10 | Verify all identified items listed | Scrollable list of ingredients | UI | — | HARD |
| F11 | Tap "Add All" or select specific items | Items added to pantry | UI | — | — |

### Phase G: Cleanup (Steps 26-27)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| G1 | Optionally delete test pantry items | Swipe/delete to clean up | UI | — | — |
| G2 | Press BACK to Settings, then Home | Home screen | UI | — | — |

## Validation Checkpoints

No `validate_meal_plan.py` checkpoints — validation is photo analysis-focused:
- **Analysis:** Gemini Vision API returns identified ingredients
- **Parsing:** Backend parses AI response into ingredient list
- **UI Display:** Android displays results in user-friendly sheet
- **Persistence:** Items added to Room + backend PostgreSQL
- **Error Handling:** Graceful failure for non-food or low-quality images

## Fix Strategy

**Relevant files for this flow:**
- Android Pantry UI: `app/presentation/pantry/PantryViewModel.kt`, `PantryScreen.kt`
- Photo picker: `app/presentation/pantry/components/PhotoAnalysisSheet.kt` (if implemented)
- Backend endpoint: `backend/app/api/v1/endpoints/pantry.py` (POST `/analyze-photo`)
- Gemini Vision service: `backend/app/ai/photo_analysis_service.py` (if implemented)
- Pantry DAO: `data/local/dao/PantryDao.kt`

**Common issues:**
- Gallery picker doesn't show pushed image → ADB media scanner broadcast failed
- Analysis timeout → Gemini Vision API key not configured or network issue
- No ingredients returned → Image format not supported (use JPG/PNG)
- UI doesn't show results → ViewModel not emitting analysis state
- Items not persisting → Pantry DAO or backend sync issue
- Crash on non-food image → Error handling missing in backend or ViewModel

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Settings | B1 | Navigation to Pantry |
| Pantry | B2, E3, G1 | Photo analysis, results display, item addition |
| Gallery Picker | C1-C3 | Image selection |
| Analysis Results Sheet | D1-D5 | AI-identified ingredients, Add button |

## Contradictions

| ID | Description | Steps | Expected Outcome | Type |
|----|-------------|-------|------------------|------|
| C40 | Non-food image upload | F1-F4 | Error message, no items added | UI |
| C41 | Low-quality image upload | F5-F7 | Low confidence or error | UI |
| C42 | Complex dish with 10+ ingredients | F8-F11 | All ingredients identified and listed | UI |
