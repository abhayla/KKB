# User Management — Persona Brainstorm

**Status:** Complete
**Last Updated:** 2026-03-07
**Method:** First Principles Thinking + Chain of Density (3 passes)
**Scope:** Full user lifecycle — registration, onboarding, profile, household, settings, account deletion

---

## Personas

| # | Persona | Role | Key Characteristics |
|---|---------|------|---------------------|
| 1 | Mom (Household Owner) | OWNER | Primary cook, manages family plan, makes food decisions |
| 2 | Dad (Family Member) | MEMBER | Views plan, occasionally swaps meals, wants grocery list |
| 3 | Teenager (Child Member) | MEMBER | Own breakfast preferences, limited agency, on the app |
| 4 | Grandparent (Senior Member) | MEMBER | Diabetic/low salt, may not be tech-savvy |
| 5 | College Student (Solo to Guest) | SOLO/GUEST | Lives alone, visits parents on weekends |
| 6 | Guest -- Adult Male | GUEST | Visiting uncle/cousin, has own dietary preferences |
| 7 | Guest -- Adult Female | GUEST | Visiting aunt, may have fasting days |
| 8 | Guest -- Child | GUEST | Visiting nephew, no spicy, parent manages profile |
| 9 | Guest -- Senior Citizen | GUEST | Visiting grandparent, multiple health conditions |
| 10 | Working Professional (Solo) | SOLO | Lives alone, no household, baseline single-user |

---

## Pass 1: Raw Pain Points (10 Per Persona)

### Persona 1: Mom (Household Owner)

| # | Need/Pain Point |
|---|----------------|
| 1 | I cook for the whole family but the app only knows about MY preferences -- it doesn't consider my husband's diabetes or my son's no-spicy requirement unless I manually enter them as metadata |
| 2 | When I generate a meal plan, my husband can't see it on his phone -- I have to screenshot and WhatsApp it to him |
| 3 | I entered all family details during onboarding but if my husband also installs the app, he has to re-enter everything from scratch -- there's no way to share what I already set up |
| 4 | My mother-in-law is visiting for 2 weeks and she's diabetic with strict sattvic diet -- I need to temporarily adjust the family plan but I don't want to lose my original settings |
| 5 | I want my husband to be able to swap a dinner item if he's picking up groceries, but I don't want him to regenerate the entire week's plan without asking me |
| 6 | The grocery list is on my phone only -- my husband goes to the market and has to ask me what to buy instead of checking the app |
| 7 | During Navratri, the whole family fasts but my son doesn't -- I need the plan to handle mixed fasting/non-fasting within the same household for the same meal |
| 8 | I set up recipe rules (no mushroom, chai daily) but if my husband installs the app, his account has none of these rules -- he'd generate a completely different plan |
| 9 | I spend 10 minutes on onboarding entering detailed family info, but there's no way to verify it's all correct in one summary view before generating |
| 10 | When I delete my account, all the family meal plan history disappears -- my husband loses access to recipes we both liked |

### Persona 2: Dad (Family Member)

| # | Need/Pain Point |
|---|----------------|
| 1 | My wife manages the meal plan but I can't see today's dinner on my phone -- I have to ask her every evening |
| 2 | I'm at the grocery store and don't know what ingredients are needed for this week's meals -- the list is on my wife's phone |
| 3 | I want to swap tomorrow's lunch to something I'm craving but I don't want to mess up my wife's carefully planned week |
| 4 | I installed the app but it asks me to go through the full 5-step onboarding as if I'm a new household -- I just want to join my wife's family |
| 5 | I have different breakfast preferences than the rest of the family (I want eggs, family is vegetarian) -- the plan doesn't accommodate individual meal preferences |
| 6 | I can't mark a meal as "cooked" or "skipped" -- there's no way for me to update the family on what actually happened with dinner |
| 7 | I travel for work 2-3 days a week -- the family plan generates food for 3 people on days when only 2 are eating at home |
| 8 | I want to see nutrition information for my meals specifically (I'm tracking calories) but the plan is family-level, not personalized |
| 9 | When my wife regenerates the plan mid-week, meals I already locked or liked get wiped out without warning |
| 10 | The app doesn't show me whose dietary constraint caused a particular recipe to be excluded -- I don't understand why certain dishes never appear |

### Persona 3: Teenager (Child Member)

| # | Need/Pain Point |
|---|----------------|
| 1 | My mom decides all the meals -- I want to at least choose my own breakfast and snacks without changing the family plan |
| 2 | I have to eat what the family eats for lunch and dinner even though I hate some of the dishes -- I can't express my preferences separately |
| 3 | The app has no concept of "I really loved this meal" that my mom would see -- my favorites don't influence future plans |
| 4 | I'm a teenager with a big appetite but the portion sizes are based on family average -- I want more food for my meals |
| 5 | I want to suggest a recipe I saw on Instagram to the family meal plan -- there's no way to request or suggest dishes |
| 6 | The onboarding asks about dietary type for the whole family but I eat eggs and the rest of the family is strict vegetarian -- there's no per-person dietary setting that works for shared meals |
| 7 | I don't care about the grocery list or cooking times -- the app shows me too much information that's irrelevant to me |
| 8 | My friends are coming over on Saturday -- I want to tell the app we need snacks for 6 people, not 3, just for that day |
| 9 | I go to school and eat lunch there on weekdays -- the family plan generates lunch for me that nobody eats, wasting groceries |
| 10 | I want a simpler view of just "what am I eating today" without all the family management features |

### Persona 4: Grandparent (Senior Member)

| # | Need/Pain Point |
|---|----------------|
| 1 | I'm diabetic and need low-sugar meals but the family plan just says "vegetarian" -- my specific health condition isn't individually tracked in the plan |
| 2 | The text in the app is too small and the navigation has too many steps -- I just want to see today's meals on one screen |
| 3 | I don't understand what "swap" or "lock" means in the app -- the terminology is confusing for someone who doesn't use apps regularly |
| 4 | I eat dinner at 7 PM but the family eats at 8:30 PM -- the app assumes everyone eats at the same time |
| 5 | My medicines require me to eat specific foods at specific times (e.g., protein-rich breakfast) -- the app doesn't account for medical dietary schedules |
| 6 | I can't use phone OTP easily because my hands shake and the OTP expires before I can type it -- the authentication is frustrating |
| 7 | I live with my son's family but I'm from a different region (South Indian) and prefer different cuisine than the family's North Indian preference |
| 8 | I fast on Ekadashi and other specific days but the rest of the family doesn't -- the plan doesn't handle per-person fasting |
| 9 | I want my daughter-in-law to manage my meals for me -- I don't want to learn the app, just see what's for lunch today |
| 10 | My doctor changes my dietary restrictions periodically -- updating them requires going through settings menus I find hard to navigate |

### Persona 5: College Student (Solo to Guest)

| # | Need/Pain Point |
|---|----------------|
| 1 | I live alone in a hostel and use the app for solo meal planning, but when I go home for Diwali I want to temporarily join my parents' family plan |
| 2 | When I visit home, I eat family meals for lunch/dinner but have my hostel eating habits for breakfast (just chai and toast) -- switching between contexts is impossible |
| 3 | I have a tight budget -- the app doesn't consider cost when suggesting recipes, and a college student's grocery budget is very different from a family's |
| 4 | After visiting home for a week, I want to go back to MY meal plan exactly where I left it -- not start fresh |
| 5 | I share a flat with roommates -- we're not a "family" but we do want to coordinate dinner sometimes. The household model assumes family relationships |
| 6 | My parents added me as a family member with "child" type and "no spicy" restriction from when I was 12 -- I'm 19 now and those constraints are outdated but I can't update them from my account |
| 7 | I'm vegetarian at home (family rule) but eat non-veg at hostel -- my dietary preference changes based on which household context I'm in |
| 8 | The app requires a phone number but I sometimes change prepaid SIMs -- I lose my account history when my number changes |
| 9 | I want quick recipes under 15 minutes with minimal ingredients -- the app's "weekday cooking time" of 30 min is set by my parents and doesn't apply to my hostel cooking |
| 10 | When I'm at home, my mom generates the plan -- but I don't get any notification that a new plan is ready, I have to keep checking |

### Persona 6: Guest -- Adult Male (Visiting Uncle/Cousin)

| # | Need/Pain Point |
|---|----------------|
| 1 | I'm visiting my sister's family for a week -- I want to see what's for meals today without asking her every time |
| 2 | I'm non-vegetarian but the family is vegetarian -- my presence shouldn't force the family to change their diet, but I'd like non-veg options for my personal meals |
| 3 | I don't want to install the app just for a 3-day visit -- there should be a lighter way to see the meal plan (link? web view?) |
| 4 | I have a specific allergy (shellfish) that the host family doesn't know about -- how do I communicate this so the shared meals are safe for me? |
| 5 | After I leave, I don't want my dietary preferences to remain in the host family's system -- it should clean up automatically |
| 6 | I'm visiting from another city and I'm used to different cuisine (Bengali vs the family's Gujarati) -- I'd appreciate at least one familiar dish during my stay |
| 7 | I brought my own groceries for breakfast but the family's grocery list doesn't reflect that I'm eating differently for one meal -- it over-buys |
| 8 | I want to offer to cook one night during my visit -- there's no way to "claim" a dinner slot and put my own recipe |
| 9 | When I return home and open the app, it still shows the host family's plan for a moment before switching back -- the transition is jarring |
| 10 | I'm planning my visit dates but the host family has already generated their plan -- joining mid-week shouldn't disrupt meals already cooked |

### Persona 7: Guest -- Adult Female (Visiting Aunt)

| # | Need/Pain Point |
|---|----------------|
| 1 | I keep Karva Chauth and specific fasting days that the host family doesn't observe -- my fasting shouldn't remove meals for everyone else, just for me |
| 2 | I have pregnancy dietary restrictions (no raw papaya, limited caffeine) -- these are temporary constraints that shouldn't permanently affect the host family's preferences |
| 3 | I usually manage my own family's meal plan -- while visiting, I want to still monitor my home family's plan remotely without being removed as their owner |
| 4 | I want to help the host by cooking sometimes -- I'd like to see the ingredient list for tomorrow's meals so I can prep |
| 5 | My child is with me (Guest Child persona) -- the host needs to know about both my constraints AND my child's constraints |
| 6 | I follow a strict Jain diet (no root vegetables) but the host family is regular vegetarian -- shared meals need to respect the most restrictive diet or give me alternatives |
| 7 | I'm visiting for a wedding week -- lots of outside meals and restaurant dinners. Some days we skip home cooking entirely and the app should handle "no meal needed today" |
| 8 | I want to share a recipe from my region that the host family might enjoy -- there's no "suggest a recipe" feature |
| 9 | The host family generated the plan before I confirmed my visit dates -- now my constraints aren't in this week's plan and it needs regeneration |
| 10 | I'm sensitive about my dietary restrictions being visible to everyone in the household -- health conditions should be private to me and the household owner only |

### Persona 8: Guest -- Child (Visiting Nephew)

| # | Need/Pain Point |
|---|----------------|
| 1 | I'm 8 years old and can't use the app -- my parent (Guest Adult) needs to manage my dietary needs within the host family |
| 2 | I'm allergic to peanuts but the host family doesn't know -- my parent needs to communicate this urgently before any meals are generated |
| 3 | I only eat "kid-friendly" food (no bitter vegetables, mild spice) -- the family plan's general spice level doesn't work for me |
| 4 | I eat smaller portions -- the grocery quantity shouldn't count me as a full adult |
| 5 | I'm a fussy eater and my list of dislikes is long -- adding 15 dislikes shouldn't overwhelm the family's meal plan with too many restrictions |
| 6 | I eat lunch at school/with other kids at the host's house -- I only need the family dinner plan, not lunch |
| 7 | I want special snacks that adults don't eat (chocolate milk, cookies) -- the snack slot should let my parent pick kid-specific items |
| 8 | I'm visiting without my parents (staying at grandma's house for summer) -- grandma needs to manage my profile but she's not tech-savvy |
| 9 | I have a medical condition (lactose intolerant) that requires dairy-free options only for me -- the rest of the family uses dairy normally |
| 10 | When I leave, the host family shouldn't have to remember to remove my peanut allergy -- it should happen automatically |

### Persona 9: Guest -- Senior Citizen (Visiting Grandparent)

| # | Need/Pain Point |
|---|----------------|
| 1 | I take 5 medications and each has dietary interactions (no grapefruit, limited potassium) -- the app doesn't understand medicine-food interactions |
| 2 | I need soft, easy-to-chew food -- the app's recipe suggestions don't consider food texture |
| 3 | I eat very early (6 AM breakfast, 7 PM dinner) -- the app's meal plan doesn't account for timing differences |
| 4 | I'm visiting for 3 months (winter stay) -- that's neither a short visit nor a permanent member. The "guest" concept feels wrong for a long stay |
| 5 | I can't read English well -- I need Hindi/regional language recipe names and instructions |
| 6 | My son's family eats too much spice for me -- I need a completely different dinner sometimes, not just a mild version |
| 7 | I forget what I ate yesterday -- a simple "meal history" view for just my meals would help me track my diet for doctor visits |
| 8 | I need specific portions (smaller rice, more dal) -- per-person portion customization doesn't exist |
| 9 | My dietary restrictions change based on my latest blood test results -- I need my son to update them easily without re-doing onboarding |
| 10 | I want to add my traditional recipes that the family doesn't know -- recipes passed down generations that aren't in any database |

### Persona 10: Working Professional (Solo User)

| # | Need/Pain Point |
|---|----------------|
| 1 | I live alone and the app asks me about "household size" and "family members" during onboarding -- this feels irrelevant and I can't skip it |
| 2 | I meal prep on Sundays for the whole week -- the app doesn't support batch cooking or "cook once, eat multiple days" |
| 3 | I eat out for lunch most weekdays -- I only need dinner and breakfast plans but the app generates all 4 slots |
| 4 | I want to invite a date/friend over for dinner on Friday -- I need to temporarily scale to 2 people for one meal without changing my whole plan |
| 5 | I'm considering joining my parents' household plan on weekends -- but I want to keep my weekday solo plan independent |
| 6 | I track macros (protein, carbs, fat) not just calories -- the nutrition info is too basic |
| 7 | My cooking skill is beginner -- the app suggests recipes that assume I know techniques like tempering, making roux, etc. |
| 8 | I order groceries online (BigBasket/Zepto) -- I want the grocery list to integrate or at least be in a copy-paste friendly format |
| 9 | I want to see the cost estimate for the week's meals -- budget matters when you're a solo earner |
| 10 | When my parents invite me home for the weekend, I want to seamlessly "join" their plan for Saturday-Sunday without disrupting my Monday-Friday |

---

## Pass 2: Clustered Themes

### Theme 1: Shared Meal Plan Visibility

**Pain points:** Mom #2, Dad #1, Dad #2, Guest Male #1, Teenager #10, College Student #10

**Core need:** Any family member with the app should see today's meals without asking the cook. The grocery list should be visible to anyone going to the market.

**Gap found:** No persona mentioned **real-time sync** -- when Mom swaps a dinner item at 4 PM, does Dad see it immediately?

---

### Theme 2: Personal Overrides Within a Family Plan

**Pain points:** Dad #5, Teenager #1, #6, #9, College Student #2, #7, Grandparent #7, Guest Female #6, Guest Senior #6, Solo #3

**Core need:** Per-person meal slots (breakfast, snacks) alongside shared meals (lunch, dinner). Members who eat differently for some meals need their own items without disrupting the family plan.

**Gap found:** What happens when dietary types conflict within shared meals? Does the shared dinner default to the most restrictive diet, or offer per-person alternatives?

---

### Theme 3: Joining Without Full Onboarding

**Pain points:** Dad #4, College Student #1, Guest Male #3

**Core need:** A "join household" path that skips redundant onboarding steps. Minimal information needed: name, allergies, dietary type if different.

**Gap found:** What is the minimum required information for a joining member? Just safety-critical fields (allergies) or full profile?

---

### Theme 4: Guest/Temporary Membership

**Pain points:** College Student #1, #4, Guest Male #5, #10, Guest Female #9, Guest Senior #4, Solo #5, #10, Mom #4

**Core need:** Time-bounded membership with automatic cleanup on departure. Guest returns to their previous state.

**Gap found:** Long-term guests (3-month winter stay) blur guest vs member. Need a spectrum, not binary.

---

### Theme 5: Constraint Merging and Safety

**Pain points:** Mom #1, Guest Male #4, Guest Child #2, #10, Guest Female #5, Guest Senior #1

**Core need:** All active members' allergies auto-merge into shared meal generation. Auto-cleanup on departure.

**Gap found:** Conflict resolution -- what if merging everyone's constraints leaves almost no viable recipes? (Jain + diabetic + nut allergy + no spicy = very few options.)

---

### Theme 6: Permissions and Control

**Pain points:** Mom #5, Dad #3, #9, Teenager #2, Guest Male #8

**Core need:** Granular permissions (view, swap/lock, regenerate, invite) with owner control.

**Gap found:** Undo and notification -- Dad #9 mentions regeneration wiping locked items. No persona raised "notify before regeneration" or "undo a swap."

---

### Theme 7: Context Switching (Solo vs Family)

**Pain points:** College Student #1, #4, #7, Solo #5, #10, Guest Female #3

**Core need:** Toggle between household plan and personal plan. Preserve both states.

**Gap found:** Guest Female #3 raised remote monitoring -- she wants to see her own family's plan while visiting another family. This is a "member of two households simultaneously" need.

---

### Theme 8: Per-Person Customization (Portions, Timing, Nutrition)

**Pain points:** Teenager #4, Grandparent #4, Guest Child #4, Guest Senior #3, #8, Dad #8, Solo #6

**Core need:** Per-person portion sizes, meal timing preferences, and nutrition tracking.

**Gap found:** Age-appropriate portions -- the app has family_size but no per-person serving calculation.

---

### Theme 9: Accessibility and Simplicity

**Pain points:** Grandparent #2, #3, #6, Guest Senior #5, Teenager #7, #10, Guest Child #1

**Core need:** Simpler views for less tech-savvy or less interested users. Larger text. Regional language support.

**Gap found:** Multi-language support beyond Hindi -- Tamil, Bengali, Marathi for Pan-India target.

---

### Theme 10: Dietary Flexibility and Exceptions

**Pain points:** College Student #6, #7, Guest Female #1, #2, Guest Senior #9, Grandparent #5, #8

**Core need:** Dietary preferences that vary by context (home vs hostel), are temporary (pregnancy), or update frequently (post blood test).

**Gap found:** Contextual dietary profiles -- College Student #7 implies needing two profiles (home vs away).

---

### Theme 11: Family Communication Layer

**Pain points:** Teenager #3, #5, Guest Male #8, Guest Female #8, Guest Senior #10, Dad #10

**Core need:** Non-owners should be able to suggest, request, or react to meals.

**Gap found:** Feedback loop to AI -- if multiple family members dislike a recipe, should the AI learn to avoid it?

---

### Theme 12: Data Lifecycle and Safety

**Pain points:** Mom #10, College Student #8, Guest Male #5, Guest Male #9, Guest Child #10

**Core need:** Clean join/leave transitions, data preservation, automatic cleanup.

**Gap found:** Ownership transfer -- if the owner deletes their account, the family loses everything.

---

### Duplicates Identified (13 overlapping pain points)

| Pain Point | Appeared In | Consolidated Into |
|------------|------------|-------------------|
| Can't see family plan on my phone | Dad #1, Guest Male #1, Teenager #10 | Theme 1 |
| Full onboarding when just want to join | Dad #4, College Student #1 | Theme 3 |
| Different breakfast than family | Dad #5, Teenager #1, College Student #2 | Theme 2 |
| Guest cleanup on departure | Guest Male #5, Guest Child #10, Mom #4 | Theme 4 |
| Can't skip meal slots I don't need | Teenager #9, Solo #3 | Theme 2 |
| My allergy not known to host | Guest Male #4, Guest Child #2 | Theme 5 |

### Gaps Not Raised by Any Persona

| # | Gap | Why It Matters |
|---|-----|---------------|
| 1 | Real-time sync of meal plan changes | Critical for same-day coordination |
| 2 | Constraint overload -- merged constraints leave no recipes | System needs a fallback strategy |
| 3 | Undo/notification before regeneration | Prevents loss of locked/liked meals |
| 4 | Multi-household passive membership | Viewing one family while visiting another |
| 5 | Ownership transfer before account deletion | Data loss prevention |
| 6 | Feedback loop to AI from family-wide signals | Improves generation quality over time |
| 7 | Multi-language support (Pan-India) | Senior citizens, Tier 2-3 cities |
| 8 | Per-person serving sizes by age/appetite | Teenager vs senior portions differ significantly |

---

## Pass 3: Consolidated Output (Chain of Density)

Each theme below includes the strongest needs from all personas plus gaps filled from the analysis.

### 1. Shared Plan Visibility

Every family member with the app sees the same family meal plan in real-time. Changes (swaps, locks, regeneration) push immediately to all members. Non-app family members can receive a daily WhatsApp/SMS summary. The grocery list is visible to all members, not just the owner.

**Personas served:** Mom, Dad, Guest Male, Teenager, College Student
**Gap filled:** Real-time sync specified

---

### 2. Personal Override Slots

Each member can have personal meal slots (configurable -- default: breakfast and snacks are personal, lunch and dinner are shared). Personal slots are generated using that member's individual dietary profile. Shared slots use merged family constraints defaulting to the most restrictive diet, with per-person alternatives surfaced when the gap is significant (e.g., "Dad's version: Egg Bhurji").

**Personas served:** Dad, Teenager, College Student, Grandparent, Guest Female, Guest Senior, Solo
**Gap filled:** Per-person alternatives for shared meals when dietary types differ

---

### 3. Streamlined Joining

New family members joining via invite code or phone auto-link skip full onboarding. They provide only: (a) their name, (b) allergies (safety-critical), (c) dietary type if different from family default. Everything else inherits from the household. A member can later fill out their full profile in settings.

**Personas served:** Dad, College Student, Guest Male
**Gap filled:** Minimum required fields defined (name + allergies + dietary type)

---

### 4. Flexible Membership Duration

Membership has a time dimension: permanent (MEMBER), temporary with end date (GUEST), and long-stay (GUEST with extended duration -- same mechanism, just longer). On departure: constraints auto-removed, family plan auto-regenerates, guest returns to their previous state. Long-stay guests (>2 weeks) should be prompted to become permanent members.

**Personas served:** College Student, Guest Male, Guest Female, Guest Senior, Solo, Mom
**Gap filled:** Long-stay guests handled as extended GUEST, not a separate role

---

### 5. Constraint Merging with Conflict Detection

All active members' allergies are always merged (safety-critical, union of all allergies). Dislikes are merged with a threshold (if >50% of members dislike an ingredient, exclude it; otherwise note it). If merged constraints become too restrictive (<10 viable recipes), warn the owner and suggest relaxing non-safety constraints. Constraints auto-cleanup when members leave.

**Personas served:** Mom, Guest Male, Guest Child, Guest Female, Guest Senior
**Gap filled:** Constraint overload detection with fallback strategy

---

### 6. Role-Based Permissions with Notifications

Owner has full control. Members get view-only by default; owner can grant swap/lock permission per member. Only owner can regenerate. Before regeneration, all members with locked items get a notification: "Mom is regenerating the plan. Your 2 locked items will be preserved." Undo available for swaps (last swap revertible within 1 hour).

**Personas served:** Mom, Dad, Teenager, Guest Male
**Gap filled:** Pre-regeneration notification + undo for swaps

---

### 7. Dual Context: Family + Personal

Any member can toggle between "Family Plan" and "My Plan" views. When visiting another family, the member's home family plan remains visible in read-only mode. A user can belong to one household actively (eating there) and monitor one other household passively (viewing plan only). This covers the college student visiting parents and the aunt monitoring her home while visiting.

**Personas served:** College Student, Solo, Guest Female
**Gap filled:** Passive monitoring of home household while visiting another

---

### 8. Per-Person Customization

Beyond dietary preferences, each member can have: portion size preference (small/regular/large), meal timing preferences (early/regular/late dinner), active meal slots (opt out of lunch if eating at school/office), and nutrition tracking level (none/basic calories/detailed macros). These are optional -- defaults are inherited from household.

**Personas served:** Teenager, Grandparent, Guest Child, Guest Senior, Dad, Solo
**Gap filled:** Age-appropriate portions and per-person slot opt-out

---

### 9. Accessibility Modes

A "simple view" mode shows only: today's meals, one tap to see tomorrow. No settings, no swap, no management. Ideal for seniors and children. Larger text option. Regional language support for recipe names (Hindi first, then expand). Voice readout of today's meals for visually impaired or low-literacy users.

**Personas served:** Grandparent, Guest Senior, Teenager, Guest Child
**Gap filled:** Multi-language support and voice readout

---

### 10. Contextual Dietary Profiles

A user can have multiple dietary profiles tied to contexts: "At Home" (vegetarian, sattvic), "At Hostel" (non-veg, quick meals), "During Fasting" (no grains, no onion/garlic). The active profile switches automatically when they join/leave a household, or can be manually toggled. Temporary health restrictions (pregnancy, post-surgery) have an expiry date and auto-revert.

**Personas served:** College Student, Guest Female, Guest Senior, Grandparent
**Gap filled:** Automatic profile switching on household join/leave

---

### 11. Family Communication Layer

Non-owners can: suggest recipes (appears as a notification to owner), react to meals (thumbs up/down -- feeds into AI learning), request a swap (owner approves/rejects), and mark meals as "cooked," "skipped," or "ordered out." The AI learns from collective family feedback over time.

**Personas served:** Teenager, Guest Male, Guest Female, Guest Senior, Dad
**Gap filled:** AI learning from family-wide feedback signals

---

### 12. Data Lifecycle and Safety

Before owner can delete account, they must transfer household ownership to another member. Guest data is automatically purged on departure (with option to "remember my preferences for next visit"). GDPR export includes household membership history. Phone number changes handled via Firebase UID continuity -- account persists even if SIM changes.

**Personas served:** Mom, College Student, Guest Male, Guest Child
**Gap filled:** Mandatory ownership transfer before deletion

---

## Summary Statistics

| Metric | Count |
|--------|-------|
| Total raw pain points | 100 |
| Personas | 10 |
| Themes identified | 12 |
| Duplicate pain points consolidated | 13 |
| Gaps not raised by any persona | 8 |
| Final consolidated themes | 12 |

---

*Generated: 2026-03-07*
*Method: First Principles Thinking + Chain of Density (3 passes)*
