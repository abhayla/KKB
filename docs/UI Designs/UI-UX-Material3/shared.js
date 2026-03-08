/* ============================================
   RasoiAI Shared JS — State, Mock Data, Helpers
   ============================================ */

// --- LocalStorage Helpers ---
function getState(key, defaultVal) {
  try {
    const v = localStorage.getItem('rasoiai_' + key);
    if (v === null) return defaultVal;
    if (typeof defaultVal === 'boolean') return v === 'true';
    if (typeof defaultVal === 'number') return Number(v);
    try { return JSON.parse(v); } catch { return v; }
  } catch { return defaultVal; }
}

function setState(key, value) {
  try {
    if (typeof value === 'object') {
      localStorage.setItem('rasoiai_' + key, JSON.stringify(value));
    } else {
      localStorage.setItem('rasoiai_' + key, String(value));
    }
  } catch (e) { console.warn('setState error:', e); }
}

function clearAllState() {
  const keys = Object.keys(localStorage).filter(k => k.startsWith('rasoiai_'));
  keys.forEach(k => localStorage.removeItem(k));
}

// --- Theme ---
function applyTheme() {
  const mode = getState('dark_mode', 'light');
  if (mode === 'system') {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.setAttribute('data-theme', prefersDark ? 'dark' : 'light');
  } else {
    document.documentElement.setAttribute('data-theme', mode);
  }
}

function toggleDarkMode() {
  const current = getState('dark_mode', 'light');
  const next = current === 'light' ? 'dark' : 'light';
  setState('dark_mode', next);
  applyTheme();
}

function setTheme(mode) {
  setState('dark_mode', mode);
  applyTheme();
}

// --- Sharma Family Defaults ---
const SHARMA_DEFAULTS = {
  auth: true,
  phone: '+911234567890',
  onboarded: true,
  household_size: 3,
  family_members: [
    { id: '1', name: 'Ramesh', age: 45, type: 'FATHER', health: ['DIABETIC', 'LOW_OIL'] },
    { id: '2', name: 'Sunita', age: 42, type: 'MOTHER', health: ['LOW_SALT'] },
    { id: '3', name: 'Aarav', age: 12, type: 'CHILD', health: ['NO_SPICY'] }
  ],
  primary_diet: 'vegetarian',
  dietary_restrictions: ['sattvic'],
  cuisines: ['north', 'south'],
  spice_level: 'medium',
  allergies: [
    { ingredient: 'Peanuts', severity: 'SEVERE' },
    { ingredient: 'Cashews', severity: 'MILD' }
  ],
  dislikes: ['Karela', 'Baingan', 'Mushroom'],
  recipe_rules: [
    { id: 'r1', type: 'INCLUDE', target: 'Chai', frequency: 'DAILY', meals: ['breakfast', 'snacks'], enforcement: 'STRICT', force_override: false },
    { id: 'r2', type: 'INCLUDE', target: 'Dal', frequency: 'TIMES_PER_WEEK', times: 4, meals: ['lunch', 'dinner'], enforcement: 'STRICT', force_override: false },
    { id: 'r3', type: 'EXCLUDE', target: 'Mushroom', frequency: 'NEVER', meals: [], enforcement: 'STRICT', force_override: false },
    { id: 'r4', type: 'EXCLUDE', target: 'Onion', frequency: 'SPECIFIC_DAYS', specific_days: ['TUESDAY'], meals: [], enforcement: 'STRICT', force_override: false }
  ],
  weekday_time: 30,
  weekend_time: 60,
  busy_days: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
  items_per_meal: 2,
  units: 'metric',
  dark_mode: 'light',
  notifications: { meal_reminders: true, achievements: true, weekly_summary: true, promotions: false },
  notification_read: [],
  // --- Household / User Management ---
  household: {
    id: 'hh-001', name: 'Sharma Family', invite_code: 'A1B2C3D4',
    invite_code_expires: '2026-03-14', owner_id: 'user-1', is_active: true,
    slot_config: { breakfast: 'personal', lunch: 'shared', dinner: 'shared', snacks: 'personal' }
  },
  household_members: [
    { id: 'hm-1', user_id: 'user-1', name: 'Ramesh', role: 'OWNER', can_edit_shared_plan: true, is_temporary: false, leave_date: null, status: 'ACTIVE', phone: '+911234567890', health: ['DIABETIC', 'LOW_OIL'], portion_size: 'regular', active_slots: ['breakfast', 'lunch', 'dinner', 'snacks'] },
    { id: 'hm-2', user_id: 'user-2', name: 'Sunita', role: 'MEMBER', can_edit_shared_plan: true, is_temporary: false, leave_date: null, status: 'ACTIVE', phone: '+919876543210', health: ['LOW_SALT'], portion_size: 'regular', active_slots: ['breakfast', 'lunch', 'dinner', 'snacks'] },
    { id: 'hm-3', user_id: null, name: 'Aarav', role: 'MEMBER', can_edit_shared_plan: false, is_temporary: false, leave_date: null, status: 'ACTIVE', phone: null, health: ['NO_SPICY'], portion_size: 'large', active_slots: ['breakfast', 'dinner', 'snacks'] }
  ],
  active_plan_view: 'family',
  accessibility_mode: 'standard',
  dietary_profiles: { home: { diet: 'vegetarian', cuisine: 'north', time: 30 }, hostel: { diet: 'eggetarian', cuisine: 'south', time: 15 } },
  active_profile: 'home'
};

function loadSharmaDefaults() {
  if (getState('_initialized', false)) return;
  Object.entries(SHARMA_DEFAULTS).forEach(([k, v]) => setState(k, v));
  setState('favorites', ['aloo-paratha', 'masala-chai', 'dal-fry', 'paneer-tikka']);
  setState('grocery_checked', {});
  setState('grocery_custom', []);
  setState('locked_meals', {});
  setState('locked_days', {});
  setState('chat_messages', MOCK_CHAT);
  setState('_initialized', true);
}

// --- Mock Meal Plan ---
const MOCK_MEAL_PLAN = {
  id: 'mp-001',
  week_start_date: '2026-03-02',
  week_end_date: '2026-03-08',
  days: [
    {
      date: '2026-03-02', day_name: 'Monday',
      breakfast: [
        { id: 'b1-1', recipe_id: 'masala-chai', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 'b1-2', recipe_id: 'aloo-paratha', recipe_name: 'Aloo Paratha', prep_time: 25, calories: 320, dietary_tags: ['vegetarian'], category: 'paratha' }
      ],
      lunch: [
        { id: 'l1-1', recipe_id: 'dal-fry', recipe_name: 'Dal Fry', prep_time: 20, calories: 180, dietary_tags: ['vegetarian', 'vegan'], category: 'dal' },
        { id: 'l1-2', recipe_id: 'jeera-rice', recipe_name: 'Jeera Rice', prep_time: 15, calories: 220, dietary_tags: ['vegetarian'], category: 'rice' }
      ],
      dinner: [
        { id: 'd1-1', recipe_id: 'paneer-tikka', recipe_name: 'Paneer Tikka Masala', prep_time: 30, calories: 350, dietary_tags: ['vegetarian'], category: 'curry' },
        { id: 'd1-2', recipe_id: 'butter-naan', recipe_name: 'Butter Naan', prep_time: 15, calories: 260, dietary_tags: ['vegetarian'], category: 'bread' }
      ],
      snacks: [
        { id: 's1-1', recipe_id: 'masala-chai-2', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 's1-2', recipe_id: 'dhokla', recipe_name: 'Dhokla', prep_time: 20, calories: 150, dietary_tags: ['vegetarian'], category: 'snack' }
      ],
      festival: null
    },
    {
      date: '2026-03-03', day_name: 'Tuesday',
      breakfast: [
        { id: 'b2-1', recipe_id: 'masala-chai', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 'b2-2', recipe_id: 'poha', recipe_name: 'Kanda Poha', prep_time: 15, calories: 240, dietary_tags: ['vegetarian'], category: 'breakfast' }
      ],
      lunch: [
        { id: 'l2-1', recipe_id: 'rajma', recipe_name: 'Rajma Masala', prep_time: 25, calories: 280, dietary_tags: ['vegetarian'], category: 'curry' },
        { id: 'l2-2', recipe_id: 'steamed-rice', recipe_name: 'Steamed Rice', prep_time: 15, calories: 200, dietary_tags: ['vegetarian', 'vegan'], category: 'rice' }
      ],
      dinner: [
        { id: 'd2-1', recipe_id: 'dal-tadka', recipe_name: 'Dal Tadka', prep_time: 20, calories: 190, dietary_tags: ['vegetarian'], category: 'dal' },
        { id: 'd2-2', recipe_id: 'roti', recipe_name: 'Chapati', prep_time: 20, calories: 120, dietary_tags: ['vegetarian', 'vegan'], category: 'bread' }
      ],
      snacks: [
        { id: 's2-1', recipe_id: 'chai', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 's2-2', recipe_id: 'samosa', recipe_name: 'Samosa', prep_time: 20, calories: 260, dietary_tags: ['vegetarian'], category: 'snack' }
      ],
      festival: null
    },
    {
      date: '2026-03-04', day_name: 'Wednesday',
      breakfast: [
        { id: 'b3-1', recipe_id: 'chai-3', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 'b3-2', recipe_id: 'upma', recipe_name: 'Rava Upma', prep_time: 15, calories: 200, dietary_tags: ['vegetarian'], category: 'breakfast' }
      ],
      lunch: [
        { id: 'l3-1', recipe_id: 'chana-masala', recipe_name: 'Chana Masala', prep_time: 25, calories: 260, dietary_tags: ['vegetarian', 'vegan'], category: 'curry' },
        { id: 'l3-2', recipe_id: 'paratha-3', recipe_name: 'Lacha Paratha', prep_time: 20, calories: 280, dietary_tags: ['vegetarian'], category: 'bread' }
      ],
      dinner: [
        { id: 'd3-1', recipe_id: 'palak-paneer', recipe_name: 'Palak Paneer', prep_time: 30, calories: 300, dietary_tags: ['vegetarian'], category: 'curry' },
        { id: 'd3-2', recipe_id: 'naan-3', recipe_name: 'Garlic Naan', prep_time: 15, calories: 270, dietary_tags: ['vegetarian'], category: 'bread' }
      ],
      snacks: [
        { id: 's3-1', recipe_id: 'chai-3b', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 's3-2', recipe_id: 'pakora', recipe_name: 'Onion Pakora', prep_time: 15, calories: 200, dietary_tags: ['vegetarian'], category: 'snack' }
      ],
      festival: null
    },
    {
      date: '2026-03-05', day_name: 'Thursday',
      breakfast: [
        { id: 'b4-1', recipe_id: 'chai-4', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 'b4-2', recipe_id: 'idli', recipe_name: 'Idli Sambar', prep_time: 20, calories: 180, dietary_tags: ['vegetarian', 'vegan'], category: 'breakfast' }
      ],
      lunch: [
        { id: 'l4-1', recipe_id: 'dal-palak', recipe_name: 'Dal Palak', prep_time: 20, calories: 170, dietary_tags: ['vegetarian', 'vegan'], category: 'dal' },
        { id: 'l4-2', recipe_id: 'rice-4', recipe_name: 'Jeera Rice', prep_time: 15, calories: 220, dietary_tags: ['vegetarian'], category: 'rice' }
      ],
      dinner: [
        { id: 'd4-1', recipe_id: 'matar-paneer', recipe_name: 'Matar Paneer', prep_time: 25, calories: 310, dietary_tags: ['vegetarian'], category: 'curry' },
        { id: 'd4-2', recipe_id: 'roti-4', recipe_name: 'Tandoori Roti', prep_time: 15, calories: 110, dietary_tags: ['vegetarian', 'vegan'], category: 'bread' }
      ],
      snacks: [
        { id: 's4-1', recipe_id: 'chai-4b', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 's4-2', recipe_id: 'kachori', recipe_name: 'Moong Dal Kachori', prep_time: 25, calories: 280, dietary_tags: ['vegetarian'], category: 'snack' }
      ],
      festival: null
    },
    {
      date: '2026-03-06', day_name: 'Friday',
      breakfast: [
        { id: 'b5-1', recipe_id: 'chai-5', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 'b5-2', recipe_id: 'dosa', recipe_name: 'Masala Dosa', prep_time: 25, calories: 250, dietary_tags: ['vegetarian', 'vegan'], category: 'breakfast' }
      ],
      lunch: [
        { id: 'l5-1', recipe_id: 'mixed-veg', recipe_name: 'Mixed Veg Curry', prep_time: 25, calories: 200, dietary_tags: ['vegetarian'], category: 'sabzi' },
        { id: 'l5-2', recipe_id: 'pulao', recipe_name: 'Veg Pulao', prep_time: 20, calories: 250, dietary_tags: ['vegetarian'], category: 'rice' }
      ],
      dinner: [
        { id: 'd5-1', recipe_id: 'dal-makhani', recipe_name: 'Dal Makhani', prep_time: 30, calories: 280, dietary_tags: ['vegetarian'], category: 'dal' },
        { id: 'd5-2', recipe_id: 'naan-5', recipe_name: 'Butter Naan', prep_time: 15, calories: 260, dietary_tags: ['vegetarian'], category: 'bread' }
      ],
      snacks: [
        { id: 's5-1', recipe_id: 'chai-5b', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 's5-2', recipe_id: 'mathri', recipe_name: 'Mathri', prep_time: 20, calories: 180, dietary_tags: ['vegetarian'], category: 'snack' }
      ],
      festival: null
    },
    {
      date: '2026-03-07', day_name: 'Saturday',
      breakfast: [
        { id: 'b6-1', recipe_id: 'chai-6', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 'b6-2', recipe_id: 'chole-bhature', recipe_name: 'Chole Bhature', prep_time: 45, calories: 450, dietary_tags: ['vegetarian'], category: 'breakfast' }
      ],
      lunch: [
        { id: 'l6-1', recipe_id: 'biryani', recipe_name: 'Veg Biryani', prep_time: 50, calories: 380, dietary_tags: ['vegetarian'], category: 'rice' },
        { id: 'l6-2', recipe_id: 'raita', recipe_name: 'Boondi Raita', prep_time: 10, calories: 100, dietary_tags: ['vegetarian'], category: 'side' }
      ],
      dinner: [
        { id: 'd6-1', recipe_id: 'malai-kofta', recipe_name: 'Malai Kofta', prep_time: 45, calories: 380, dietary_tags: ['vegetarian'], category: 'curry' },
        { id: 'd6-2', recipe_id: 'naan-6', recipe_name: 'Tandoori Naan', prep_time: 15, calories: 240, dietary_tags: ['vegetarian'], category: 'bread' }
      ],
      snacks: [
        { id: 's6-1', recipe_id: 'chai-6b', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 's6-2', recipe_id: 'gulab-jamun', recipe_name: 'Gulab Jamun', prep_time: 30, calories: 300, dietary_tags: ['vegetarian'], category: 'dessert' }
      ],
      festival: null
    },
    {
      date: '2026-03-08', day_name: 'Sunday',
      breakfast: [
        { id: 'b7-1', recipe_id: 'chai-7', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 'b7-2', recipe_id: 'stuffed-paratha', recipe_name: 'Gobi Paratha', prep_time: 30, calories: 340, dietary_tags: ['vegetarian'], category: 'paratha' }
      ],
      lunch: [
        { id: 'l7-1', recipe_id: 'kadhi', recipe_name: 'Punjabi Kadhi Pakora', prep_time: 40, calories: 250, dietary_tags: ['vegetarian'], category: 'curry' },
        { id: 'l7-2', recipe_id: 'rice-7', recipe_name: 'Steamed Rice', prep_time: 15, calories: 200, dietary_tags: ['vegetarian'], category: 'rice' }
      ],
      dinner: [
        { id: 'd7-1', recipe_id: 'shahi-paneer', recipe_name: 'Shahi Paneer', prep_time: 35, calories: 360, dietary_tags: ['vegetarian'], category: 'curry' },
        { id: 'd7-2', recipe_id: 'kulcha', recipe_name: 'Amritsari Kulcha', prep_time: 25, calories: 290, dietary_tags: ['vegetarian'], category: 'bread' }
      ],
      snacks: [
        { id: 's7-1', recipe_id: 'chai-7b', recipe_name: 'Masala Chai', prep_time: 10, calories: 80, dietary_tags: ['vegetarian'], category: 'chai' },
        { id: 's7-2', recipe_id: 'gajar-halwa', recipe_name: 'Gajar Ka Halwa', prep_time: 40, calories: 350, dietary_tags: ['vegetarian'], category: 'dessert' }
      ],
      festival: null
    }
  ]
};

// --- Mock Grocery ---
const MOCK_GROCERY = {
  'Vegetables': [
    { id: 'g1', name: 'Potato', qty: '1', unit: 'kg' },
    { id: 'g2', name: 'Onion', qty: '500', unit: 'g' },
    { id: 'g3', name: 'Tomato', qty: '500', unit: 'g' },
    { id: 'g4', name: 'Green Peas', qty: '250', unit: 'g' },
    { id: 'g5', name: 'Spinach', qty: '2', unit: 'bunches' },
    { id: 'g6', name: 'Cauliflower', qty: '1', unit: 'medium' },
    { id: 'g7', name: 'Carrot', qty: '250', unit: 'g' },
    { id: 'g8', name: 'Green Chili', qty: '100', unit: 'g' }
  ],
  'Dairy': [
    { id: 'g9', name: 'Paneer', qty: '500', unit: 'g' },
    { id: 'g10', name: 'Milk', qty: '2', unit: 'L' },
    { id: 'g11', name: 'Yogurt', qty: '500', unit: 'g' },
    { id: 'g12', name: 'Butter', qty: '200', unit: 'g' },
    { id: 'g13', name: 'Cream', qty: '200', unit: 'ml' }
  ],
  'Grains & Flour': [
    { id: 'g14', name: 'Basmati Rice', qty: '2', unit: 'kg' },
    { id: 'g15', name: 'Wheat Flour (Atta)', qty: '2', unit: 'kg' },
    { id: 'g16', name: 'Besan', qty: '250', unit: 'g' },
    { id: 'g17', name: 'Rava (Semolina)', qty: '250', unit: 'g' },
    { id: 'g18', name: 'Poha (Flattened Rice)', qty: '250', unit: 'g' }
  ],
  'Pulses & Lentils': [
    { id: 'g19', name: 'Toor Dal', qty: '500', unit: 'g' },
    { id: 'g20', name: 'Moong Dal', qty: '250', unit: 'g' },
    { id: 'g21', name: 'Rajma (Kidney Beans)', qty: '250', unit: 'g' },
    { id: 'g22', name: 'Chana (Chickpeas)', qty: '250', unit: 'g' },
    { id: 'g23', name: 'Urad Dal', qty: '250', unit: 'g' }
  ],
  'Spices & Masala': [
    { id: 'g24', name: 'Cumin Seeds', qty: '100', unit: 'g' },
    { id: 'g25', name: 'Turmeric Powder', qty: '100', unit: 'g' },
    { id: 'g26', name: 'Red Chili Powder', qty: '100', unit: 'g' },
    { id: 'g27', name: 'Garam Masala', qty: '50', unit: 'g' },
    { id: 'g28', name: 'Coriander Powder', qty: '100', unit: 'g' },
    { id: 'g29', name: 'Tea Leaves', qty: '100', unit: 'g' },
    { id: 'g30', name: 'Ginger', qty: '100', unit: 'g' }
  ],
  'Oil & Ghee': [
    { id: 'g31', name: 'Mustard Oil', qty: '500', unit: 'ml' },
    { id: 'g32', name: 'Ghee', qty: '200', unit: 'g' },
    { id: 'g33', name: 'Sunflower Oil', qty: '1', unit: 'L' }
  ]
};

// --- Mock Chat ---
const MOCK_CHAT = [
  { id: 'c1', role: 'ai', text: 'Namaste! I\'m your RasoiAI assistant. I can help you with meal planning, recipe suggestions, and cooking tips. What would you like to know?', time: '9:00 AM' },
  { id: 'c2', role: 'user', text: 'What\'s for dinner tonight?', time: '9:01 AM' },
  { id: 'c3', role: 'ai', text: 'Tonight you have Paneer Tikka Masala with Butter Naan! The paneer tikka takes about 30 minutes and has 350 calories. Would you like the recipe, or would you prefer to swap it for something else?', time: '9:01 AM' },
  { id: 'c4', role: 'user', text: 'Sounds great! Any quick breakfast ideas for tomorrow?', time: '9:02 AM' },
  { id: 'c5', role: 'ai', text: 'Tomorrow morning you have Kanda Poha with Masala Chai! Poha is perfect for a busy Tuesday \u2014 it takes just 15 minutes. It\'s light, nutritious, and Aarav will love it since it\'s not spicy. Would you like me to suggest something different?', time: '9:02 AM' }
];

// --- Mock Notifications ---
const MOCK_NOTIFICATIONS = [
  { id: 'n1', icon: '\uD83C\uDF89', title: 'Holi is coming!', body: 'Special Holi recipes have been added to your meal plan for March 14.', time: '2 hours ago', read: false, type: 'festival' },
  { id: 'n2', icon: '\uD83D\uDD25', title: '7 Day Streak!', body: 'You\'ve cooked every day this week. Keep it up!', time: '1 day ago', read: false, type: 'achievement' },
  { id: 'n3', icon: '\uD83C\uDF7D\uFE0F', title: 'Meal Plan Ready', body: 'Your meal plan for Mar 2-8 is ready. Check out this week\'s recipes!', time: '2 days ago', read: true, type: 'meal' },
  { id: 'n4', icon: '\uD83D\uDED2', title: 'Shopping Reminder', body: 'You have 33 items on your grocery list. Time to shop!', time: '3 days ago', read: true, type: 'grocery' },
  { id: 'n5', icon: '\u2B50', title: 'New Achievement Unlocked', body: 'You earned "Recipe Explorer" \u2014 tried 10 different recipes!', time: '5 days ago', read: true, type: 'achievement' }
];

// --- Mock Recipe Detail ---
const MOCK_RECIPE = {
  id: 'paneer-tikka',
  name: 'Paneer Tikka Masala',
  prep_time: 30,
  cook_time: 20,
  total_time: 50,
  servings: 4,
  calories: 350,
  dietary_tags: ['vegetarian'],
  cuisine: 'North Indian',
  difficulty: 'Medium',
  description: 'A rich and creamy paneer dish with tikka masala gravy, perfect for dinner with naan or rice.',
  ingredients: [
    { name: 'Paneer', qty: '250', unit: 'g' },
    { name: 'Yogurt', qty: '2', unit: 'tbsp' },
    { name: 'Onion (large)', qty: '2', unit: 'pcs' },
    { name: 'Tomato', qty: '3', unit: 'pcs' },
    { name: 'Ginger-Garlic Paste', qty: '1', unit: 'tbsp' },
    { name: 'Red Chili Powder', qty: '1', unit: 'tsp' },
    { name: 'Turmeric', qty: '0.5', unit: 'tsp' },
    { name: 'Garam Masala', qty: '1', unit: 'tsp' },
    { name: 'Cream', qty: '2', unit: 'tbsp' },
    { name: 'Oil', qty: '2', unit: 'tbsp' },
    { name: 'Salt', qty: '', unit: 'to taste' },
    { name: 'Kasuri Methi', qty: '1', unit: 'tsp' },
    { name: 'Coriander Leaves', qty: '', unit: 'for garnish' }
  ],
  instructions: [
    'Cut paneer into 1-inch cubes. Marinate with yogurt, red chili powder, turmeric, and salt for 15 minutes.',
    'Thread paneer onto skewers and grill or pan-fry until lightly charred on all sides. Set aside.',
    'Heat oil in a pan. Add chopped onions and saute until golden brown.',
    'Add ginger-garlic paste and cook for 1 minute until fragrant.',
    'Add pureed tomatoes, red chili powder, turmeric, and salt. Cook for 8-10 minutes until oil separates.',
    'Add garam masala and kasuri methi. Stir well.',
    'Add cream and mix until well combined. Simmer for 2 minutes.',
    'Add the grilled paneer pieces to the gravy. Gently fold in and cook for 3-4 minutes.',
    'Garnish with fresh coriander leaves and serve hot with naan or rice.'
  ],
  nutrition: {
    calories: 350,
    protein: 18,
    fat: 22,
    carbs: 20,
    fiber: 3,
    sodium: 580
  }
};

// --- Mock Achievements ---
const MOCK_ACHIEVEMENTS = [
  { id: 'a1', emoji: '\uD83D\uDD25', name: 'First Flame', description: 'Cook your first meal', unlocked: true, date: 'Feb 28, 2026' },
  { id: 'a2', emoji: '\uD83C\uDF1F', name: 'Week Warrior', description: '7-day cooking streak', unlocked: true, date: 'Mar 6, 2026' },
  { id: 'a3', emoji: '\uD83C\uDF0E', name: 'Recipe Explorer', description: 'Try 10 different recipes', unlocked: true, date: 'Mar 5, 2026' },
  { id: 'a4', emoji: '\uD83D\uDC68\u200D\uD83C\uDF73', name: 'Master Chef', description: 'Cook 50 meals', unlocked: false, progress: 28, total: 50 },
  { id: 'a5', emoji: '\uD83E\uDD57', name: 'Health Guru', description: 'Log 30 healthy meals', unlocked: false, progress: 12, total: 30 },
  { id: 'a6', emoji: '\uD83C\uDF1E', name: 'Month of Meals', description: '30-day cooking streak', unlocked: false, progress: 7, total: 30 }
];

// --- Mock Pantry ---
const MOCK_PANTRY = [
  { id: 'p1', name: 'Rice (Basmati)', qty: '2', unit: 'kg', category: 'Grains', expiry: '2026-06-15', status: 'ok' },
  { id: 'p2', name: 'Wheat Flour', qty: '1', unit: 'kg', category: 'Grains', expiry: '2026-05-20', status: 'ok' },
  { id: 'p3', name: 'Toor Dal', qty: '500', unit: 'g', category: 'Pulses', expiry: '2026-08-10', status: 'ok' },
  { id: 'p4', name: 'Paneer', qty: '200', unit: 'g', category: 'Dairy', expiry: '2026-03-09', status: 'expiring' },
  { id: 'p5', name: 'Yogurt', qty: '250', unit: 'g', category: 'Dairy', expiry: '2026-03-05', status: 'expired' },
  { id: 'p6', name: 'Milk', qty: '1', unit: 'L', category: 'Dairy', expiry: '2026-03-08', status: 'expiring' },
  { id: 'p7', name: 'Cumin Seeds', qty: '100', unit: 'g', category: 'Spices', expiry: '2027-01-01', status: 'ok' },
  { id: 'p8', name: 'Turmeric', qty: '50', unit: 'g', category: 'Spices', expiry: '2027-01-01', status: 'ok' },
  { id: 'p9', name: 'Ghee', qty: '200', unit: 'g', category: 'Oil & Ghee', expiry: '2026-09-01', status: 'ok' },
  { id: 'p10', name: 'Mustard Oil', qty: '500', unit: 'ml', category: 'Oil & Ghee', expiry: '2026-12-01', status: 'ok' }
];

// --- Emoji maps for meal types ---
const MEAL_ICONS = {
  breakfast: '\u2615',
  lunch: '\uD83C\uDF5B',
  dinner: '\uD83C\uDF5D',
  snacks: '\uD83C\uDF6A'
};

const CATEGORY_ICONS = {
  'Vegetables': '\uD83E\uDD66',
  'Dairy': '\uD83E\uDDC0',
  'Grains & Flour': '\uD83C\uDF3E',
  'Pulses & Lentils': '\uD83E\uDED8',
  'Spices & Masala': '\uD83C\uDF36\uFE0F',
  'Oil & Ghee': '\uD83E\uDED3'
};

// --- Food emoji map for recipe thumbnails ---
const FOOD_EMOJI = {
  'chai': '\u2615', 'paratha': '\uD83E\uDDC7', 'dal': '\uD83E\uDD63', 'rice': '\uD83C\uDF5A',
  'curry': '\uD83C\uDF5B', 'bread': '\uD83C\uDF5E', 'snack': '\uD83C\uDF5F', 'dessert': '\uD83C\uDF6E',
  'breakfast': '\uD83C\uDF73', 'sabzi': '\uD83E\uDD57', 'side': '\uD83E\uDD63', 'dosa': '\uD83E\uDD59',
  'default': '\uD83C\uDF7D\uFE0F'
};

function getFoodEmoji(category) {
  return FOOD_EMOJI[category] || FOOD_EMOJI['default'];
}

// --- Component Helpers ---

function renderStatusBar() {
  return `<div class="phone-status-bar">
    <span class="time">9:41</span>
    <span class="icons">\uD83D\uDCF6 \uD83D\uDD0B</span>
  </div>`;
}

function renderTopBar(opts = {}) {
  const { title = 'RasoiAI', backLink = '', actions = '', navIcon = '' } = opts;
  const nav = backLink
    ? `<a href="${backLink}" class="nav-icon">\u2190</a>`
    : navIcon
      ? `<a href="${navIcon}" class="nav-icon">\u2630</a>`
      : '';
  return `<div class="top-app-bar">
    ${nav}
    <span class="title">${title}</span>
    <div class="actions">${actions}</div>
  </div>`;
}

function renderBottomNav(activeTab) {
  const tabs = [
    { id: 'home', label: 'Home', icon: '\uD83C\uDFE0', href: 'main-home.html' },
    { id: 'grocery', label: 'Grocery', icon: '\uD83D\uDED2', href: 'main-grocery.html' },
    { id: 'chat', label: 'Chat', icon: '\uD83D\uDCAC', href: 'main-chat.html' },
    { id: 'favorites', label: 'Favorites', icon: '\u2764\uFE0F', href: 'main-favorites.html' },
    { id: 'stats', label: 'Stats', icon: '\uD83D\uDCCA', href: 'main-stats.html' }
  ];

  const unreadCount = MOCK_NOTIFICATIONS.filter(n => !n.read).length;

  return `<nav class="bottom-nav">
    ${tabs.map(t => {
      const isActive = t.id === activeTab;
      const badge = t.id === 'home' && unreadCount > 0 ? `<span class="badge">${unreadCount}</span>` : '';
      return `<a href="${t.href}" class="${isActive ? 'active' : ''}">
        ${badge}
        <span class="nav-indicator">${t.icon}</span>
        <span>${t.label}</span>
      </a>`;
    }).join('')}
  </nav>`;
}

function renderOnboardingProgress(currentStep, totalSteps = 5) {
  return `<div class="onboarding-progress">
    ${Array.from({ length: totalSteps }, (_, i) => {
      const cls = i < currentStep ? 'completed' : i === currentStep ? 'current' : '';
      return `<div class="step-dot ${cls}"></div>`;
    }).join('')}
  </div>`;
}

// --- Utility ---
function getMealPlan() { return MOCK_MEAL_PLAN; }
function getGrocery() { return MOCK_GROCERY; }

function getDayData(dayIndex) {
  const plan = getMealPlan();
  return plan.days[dayIndex] || plan.days[0];
}

function formatDate(dateStr) {
  const d = new Date(dateStr + 'T00:00:00');
  return d.toLocaleDateString('en-IN', { month: 'short', day: 'numeric' });
}

function getWeekDates() {
  const plan = getMealPlan();
  return plan.days.map((d, i) => ({
    index: i,
    label: d.day_name.substring(0, 3),
    number: new Date(d.date + 'T00:00:00').getDate(),
    date: d.date,
    isToday: i === 5 // Saturday = "today" for demo
  }));
}

// --- Household Helpers ---
function getHousehold() { return getState('household', SHARMA_DEFAULTS.household); }
function getHouseholdMembers() { return getState('household_members', SHARMA_DEFAULTS.household_members); }

function renderPlanToggle(activeView) {
  const active = activeView || getState('active_plan_view', 'family');
  return `<div class="plan-toggle">
    <button class="plan-toggle-btn ${active === 'family' ? 'active' : ''}" onclick="setPlanView('family')">Family Plan</button>
    <button class="plan-toggle-btn ${active === 'personal' ? 'active' : ''}" onclick="setPlanView('personal')">My Plan</button>
  </div>`;
}

function setPlanView(view) {
  setState('active_plan_view', view);
  document.querySelectorAll('.plan-toggle-btn').forEach(b => b.classList.remove('active'));
  event.target.classList.add('active');
  document.querySelectorAll('.scope-label').forEach(el => el.style.display = view === 'family' ? 'inline-block' : 'none');
}

function renderMemberAvatar(name, size) {
  const s = size || 40;
  const colors = ['var(--primary-container)', 'var(--secondary-container)', 'var(--tertiary-container)'];
  const textColors = ['var(--on-primary-container)', 'var(--on-secondary-container)', 'var(--on-tertiary-container)'];
  const idx = name.charCodeAt(0) % 3;
  const initials = name.charAt(0).toUpperCase();
  return `<div style="width:${s}px;height:${s}px;border-radius:50%;background:${colors[idx]};color:${textColors[idx]};display:flex;align-items:center;justify-content:center;font-size:${Math.round(s*0.4)}px;font-weight:700;flex-shrink:0;">${initials}</div>`;
}

function renderRoleBadge(role) {
  const styles = {
    OWNER: 'background:var(--primary-container);color:var(--on-primary-container)',
    MEMBER: 'background:var(--secondary-container);color:var(--on-secondary-container)',
    GUEST: 'background:var(--tertiary-container);color:var(--on-tertiary-container)'
  };
  return `<span class="role-badge" style="${styles[role] || styles.MEMBER}">${role}</span>`;
}

function copyInviteCode() {
  const code = getHousehold().invite_code || 'A1B2C3D4';
  navigator.clipboard.writeText(code).then(() => {
    const btn = event.target;
    const orig = btn.textContent;
    btn.textContent = 'Copied!';
    setTimeout(() => btn.textContent = orig, 1500);
  }).catch(() => alert('Code: ' + code));
}

// --- Warm Modern Helpers ---
function getWarmGradient() {
  return 'var(--gradient-warm)';
}

function getHeroGradient() {
  return 'var(--gradient-hero)';
}

// --- Page Init ---
function initPage() {
  applyTheme();
  loadSharmaDefaults();
}

// Auto-init on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initPage);
} else {
  initPage();
}

// --- Per-Screen Scope Toggle ---
function renderScopeToggle(screenKey, familyLabel, personalLabel) {
  familyLabel = familyLabel || 'Family';
  personalLabel = personalLabel || 'Personal';
  const active = getState('scope_' + screenKey, 'family');
  return '<div class="plan-toggle" style="margin:0 var(--sp-md) var(--sp-md);">' +
    '<button class="plan-toggle-btn ' + (active === 'family' ? 'active' : '') + '" onclick="setScopeView(\'' + screenKey + '\', \'family\')">' + familyLabel + '</button>' +
    '<button class="plan-toggle-btn ' + (active === 'personal' ? 'active' : '') + '" onclick="setScopeView(\'' + screenKey + '\', \'personal\')">' + personalLabel + '</button>' +
  '</div>';
}

function setScopeView(screenKey, view) {
  setState('scope_' + screenKey, view);
  if (typeof render === 'function') render();
}

function getScopeView(screenKey) {
  return getState('scope_' + screenKey, 'family');
}

function isOwner() {
  var hh = getHousehold();
  if (!hh) return false;
  return true; // In prototype, current user is always the owner
}

function isSoloUser() {
  return !getHousehold();
}

// --- Dual Section Helper for Settings ---
function renderDualSectionHeader(title, isHousehold) {
  var hh = getHousehold();
  if (isHousehold) {
    var canEdit = isOwner();
    return '<div class="section-header" style="margin-top:var(--sp-md);">' +
      '<span class="section-title">' + (hh ? hh.name : 'Household') + '</span>' +
      (!canEdit ? '<span class="scope-label personal" style="background:var(--secondary-container);color:var(--on-secondary-container);">READ ONLY</span>' : '') +
    '</div>';
  }
  return '<div class="section-header" style="margin-top:var(--sp-lg);">' +
    '<span class="section-title">My Preferences</span>' +
  '</div>';
}

// --- Mock Household Notifications ---
const MOCK_HOUSEHOLD_NOTIFICATIONS = [
  { id: 'hn1', icon: '\uD83D\uDC65', title: 'Sunita joined the household', body: 'Sunita Sharma has joined Sharma Family via invite code.', time: '1 day ago', read: false, type: 'household' },
  { id: 'hn2', icon: '\uD83C\uDF7D', title: 'Meal plan regenerated', body: 'Ramesh regenerated the family meal plan for Mar 2-8.', time: '2 days ago', read: true, type: 'household' },
  { id: 'hn3', icon: '\uD83D\uDED2', title: 'Grocery suggestion', body: 'Sunita suggested adding "Paneer (500g)" to the grocery list.', time: '3 hours ago', read: false, type: 'grocery' },
  { id: 'hn4', icon: '\uD83D\uDD04', title: 'Aarav swapped a meal', body: 'Aarav swapped Wednesday dinner from Palak Paneer to Chole.', time: '1 day ago', read: true, type: 'household' }
];

// --- Mock Household Recipe Rules ---
const MOCK_HOUSEHOLD_RULES = [
  { id: 'hr1', type: 'INCLUDE', target: 'Chai', frequency: 'DAILY', meals: ['breakfast', 'snacks'], enforcement: 'STRICT', scope: 'household' },
  { id: 'hr2', type: 'INCLUDE', target: 'Dal', frequency: 'TIMES_PER_WEEK', times: 4, meals: ['lunch', 'dinner'], enforcement: 'STRICT', scope: 'household' },
  { id: 'hr3', type: 'EXCLUDE', target: 'Mushroom', frequency: 'NEVER', meals: [], enforcement: 'STRICT', scope: 'household' }
];

const MOCK_PERSONAL_RULES = [
  { id: 'pr1', type: 'INCLUDE', target: 'Egg Bhurji', frequency: 'TIMES_PER_WEEK', times: 3, meals: ['breakfast'], enforcement: 'FLEXIBLE', scope: 'personal' },
  { id: 'pr2', type: 'EXCLUDE', target: 'Onion', frequency: 'SPECIFIC_DAYS', specific_days: ['TUESDAY'], meals: [], enforcement: 'STRICT', scope: 'personal' }
];

// --- Mock Grocery Suggestions ---
const MOCK_GROCERY_SUGGESTIONS = [
  { id: 'gs1', name: 'Paneer', qty: '500', unit: 'g', suggested_by: 'Sunita', status: 'pending' },
  { id: 'gs2', name: 'Coconut Milk', qty: '400', unit: 'ml', suggested_by: 'Aarav', status: 'pending' },
  { id: 'gs3', name: 'Saffron', qty: '1', unit: 'g', suggested_by: 'Sunita', status: 'approved' }
];

// --- Mock Family Favorites ---
const MOCK_FAMILY_FAVORITES = [
  { recipe_id: 'dal-fry', recipe_name: 'Dal Fry', category: 'dal', prep_time: 20, favorited_by: ['Ramesh', 'Sunita', 'Aarav'], count: 3 },
  { recipe_id: 'aloo-paratha', recipe_name: 'Aloo Paratha', category: 'paratha', prep_time: 25, favorited_by: ['Ramesh', 'Sunita'], count: 2 },
  { recipe_id: 'masala-chai', recipe_name: 'Masala Chai', category: 'chai', prep_time: 10, favorited_by: ['Ramesh', 'Aarav'], count: 2 },
  { recipe_id: 'paneer-tikka', recipe_name: 'Paneer Tikka Masala', category: 'curry', prep_time: 30, favorited_by: ['Sunita'], count: 1 }
];

// --- Mock Household Stats ---
const MOCK_HOUSEHOLD_STATS = {
  members: [
    { name: 'Sunita', meals_cooked: 18, streak: 7 },
    { name: 'Ramesh', meals_cooked: 12, streak: 7 },
    { name: 'Aarav', meals_cooked: 5, streak: 3 }
  ],
  total_meals: 35,
  total_recipes_tried: 22,
  most_popular: 'Dal Fry'
};

// --- Mock Household Preferences (for Settings dual-section) ---
const MOCK_HOUSEHOLD_PREFERENCES = {
  dietary_restrictions: ['sattvic'],
  dislikes: ['Karela', 'Baingan', 'Mushroom'],
  cuisines: ['north', 'south'],
  spice_level: 'medium',
  weekday_time: 30,
  weekend_time: 60,
  busy_days: ['MONDAY', 'WEDNESDAY', 'FRIDAY']
};

const MOCK_PERSONAL_PREFERENCES = {
  dietary_restrictions: [],
  dislikes: ['Lauki'],
  cuisines: ['north', 'west'],
  spice_level: 'spicy',
  weekday_time: 20,
  weekend_time: 45,
  busy_days: ['MONDAY']
};

// --- Mock Personal Grocery ---
const MOCK_PERSONAL_GROCERY = {
  'Breakfast Items': [
    { id: 'pg1', name: 'Eggs', qty: '12', unit: 'pcs' },
    { id: 'pg2', name: 'Bread', qty: '1', unit: 'loaf' },
    { id: 'pg3', name: 'Jam', qty: '1', unit: 'jar' }
  ],
  'Snacks': [
    { id: 'pg4', name: 'Biscuits', qty: '2', unit: 'packs' },
    { id: 'pg5', name: 'Namkeen', qty: '200', unit: 'g' }
  ]
};
