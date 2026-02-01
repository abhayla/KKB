package com.rasoiai.app.presentation.common

/**
 * Test tags for UI testing with Compose/Espresso.
 * Use these constants with Modifier.testTag() for reliable element selection.
 */
object TestTags {
    // Onboarding - General
    const val ONBOARDING_PROGRESS_BAR = "onboarding_progress_bar"
    const val ONBOARDING_STEP_INDICATOR = "onboarding_step_indicator"
    const val ONBOARDING_NEXT_BUTTON = "onboarding_next_button"
    const val ONBOARDING_BACK_BUTTON = "onboarding_back_button"

    // Onboarding - Step 1: Household Size
    const val HOUSEHOLD_SIZE_DROPDOWN = "household_size_dropdown"
    const val HOUSEHOLD_SIZE_OPTION_PREFIX = "household_size_option_"
    const val FAMILY_MEMBERS_LIST = "family_members_list"
    const val ADD_FAMILY_MEMBER_BUTTON = "add_family_member_button"
    const val FAMILY_MEMBER_ROW_PREFIX = "family_member_row_"
    const val FAMILY_MEMBER_EDIT_PREFIX = "family_member_edit_"
    const val FAMILY_MEMBER_DELETE_PREFIX = "family_member_delete_"

    // Family Member Bottom Sheet
    const val MEMBER_NAME_FIELD = "member_name_field"
    const val MEMBER_TYPE_DROPDOWN = "member_type_dropdown"
    const val MEMBER_TYPE_OPTION_PREFIX = "member_type_option_"
    const val MEMBER_AGE_DROPDOWN = "member_age_dropdown"
    const val MEMBER_AGE_OPTION_PREFIX = "member_age_option_"
    const val MEMBER_DIETARY_NEED_PREFIX = "member_dietary_need_"
    const val MEMBER_CANCEL_BUTTON = "member_cancel_button"
    const val MEMBER_SAVE_BUTTON = "member_save_button"

    // Onboarding - Step 2: Dietary Preferences
    const val PRIMARY_DIET_PREFIX = "primary_diet_"
    const val DIETARY_RESTRICTION_PREFIX = "dietary_restriction_"

    // Onboarding - Step 3: Cuisine Preferences
    const val CUISINE_CARD_PREFIX = "cuisine_card_"
    const val SPICE_LEVEL_DROPDOWN = "spice_level_dropdown"
    const val SPICE_LEVEL_OPTION_PREFIX = "spice_level_option_"

    // Onboarding - Step 4: Disliked Ingredients
    const val INGREDIENT_SEARCH_FIELD = "ingredient_search_field"
    const val INGREDIENT_ADD_BUTTON = "ingredient_add_button"
    const val INGREDIENT_CHIP_PREFIX = "ingredient_chip_"
    const val CUSTOM_INGREDIENT_PREFIX = "custom_ingredient_"

    // Onboarding - Step 5: Cooking Time
    const val WEEKDAY_TIME_DROPDOWN = "weekday_time_dropdown"
    const val WEEKEND_TIME_DROPDOWN = "weekend_time_dropdown"
    const val BUSY_DAY_CHIP_PREFIX = "busy_day_chip_"

    // Generating Screen
    const val GENERATING_SCREEN = "generating_screen"
    const val GENERATING_PROGRESS_ANALYZING = "generating_progress_analyzing"
    const val GENERATING_PROGRESS_FESTIVALS = "generating_progress_festivals"
    const val GENERATING_PROGRESS_RECIPES = "generating_progress_recipes"
    const val GENERATING_PROGRESS_GROCERY = "generating_progress_grocery"
    const val GENERATING_RETRY_BUTTON = "generating_retry_button"
    const val GENERATING_ERROR_MESSAGE = "generating_error_message"
    const val GENERATING_SUCCESS_MESSAGE = "generating_success_message"

    // Auth Screen
    const val AUTH_SCREEN = "auth_screen"
    const val AUTH_SIGN_IN_BUTTON = "google_sign_in_button"
    const val GOOGLE_SIGN_IN_BUTTON = "google_sign_in_button" // Alias for AUTH_SIGN_IN_BUTTON
    const val AUTH_WELCOME_TEXT = "auth_welcome_text"

    // Onboarding Screen (container)
    const val ONBOARDING_SCREEN = "onboarding_screen"

    // Home Screen
    const val HOME_SCREEN = "home_screen"
    const val HOME_LOADING = "home_loading"
    const val HOME_MENU_BUTTON = "home_menu_button"
    const val HOME_NOTIFICATIONS_BUTTON = "home_notifications_button"
    const val HOME_PROFILE_BUTTON = "home_profile_button"
    const val HOME_WEEK_SELECTOR = "home_week_selector"
    const val HOME_DAY_TAB_PREFIX = "home_day_tab_"
    const val HOME_MEAL_LIST = "home_meal_list"
    const val MEAL_CARD_PREFIX = "meal_card_"
    const val MEAL_LOCK_BUTTON_PREFIX = "meal_lock_"
    const val MEAL_SWAP_BUTTON_PREFIX = "meal_swap_"
    const val MEAL_ADD_BUTTON_PREFIX = "meal_add_"

    // Home Screen - Day Header
    const val HOME_DAY_LOCK_BUTTON = "home_day_lock_button"
    const val HOME_REFRESH_BUTTON = "home_refresh_button"

    // Home Screen - Refresh Options Sheet
    const val REFRESH_OPTIONS_SHEET = "refresh_options_sheet"
    const val REFRESH_DAY_OPTION = "refresh_day_option"
    const val REFRESH_WEEK_OPTION = "refresh_week_option"

    // Home Screen - Recipe Action Sheet
    const val RECIPE_ACTION_SHEET = "recipe_action_sheet"
    const val ACTION_VIEW_RECIPE = "action_view_recipe"
    const val ACTION_SWAP_RECIPE = "action_swap_recipe"
    const val ACTION_LOCK_RECIPE = "action_lock_recipe"
    const val ACTION_REMOVE_RECIPE = "action_remove_recipe"

    // Home Screen - Swap Recipe Sheet
    const val SWAP_RECIPE_SHEET = "swap_recipe_sheet"
    const val SWAP_SEARCH_FIELD = "swap_search_field"
    const val SWAP_RECIPE_GRID = "swap_recipe_grid"
    const val SWAP_RECIPE_ITEM_PREFIX = "swap_recipe_item_"

    // Home Screen - Add Recipe Sheet
    const val ADD_RECIPE_SHEET = "add_recipe_sheet"

    // Navigation Drawer
    const val NAVIGATION_DRAWER = "navigation_drawer"
    const val DRAWER_SETTINGS_ITEM = "drawer_settings_item"
    const val DRAWER_PROFILE_ITEM = "drawer_profile_item"
    const val DRAWER_LOGOUT_ITEM = "drawer_logout_item"

    // Notifications Screen
    const val NOTIFICATIONS_SCREEN = "notifications_screen"
    const val NOTIFICATIONS_LIST = "notifications_list"
    const val NOTIFICATION_ITEM_PREFIX = "notification_item_"

    // Home Screen - Festival Banner
    const val HOME_FESTIVAL_BANNER = "home_festival_banner"

    // Home Screen - Meal Item Content
    const val MEAL_ITEM_PREFIX = "meal_item_"
    const val MEAL_ITEM_NAME_PREFIX = "meal_item_name_"
    const val MEAL_ITEM_TIME_PREFIX = "meal_item_time_"

    // Bottom Navigation
    const val BOTTOM_NAV = "bottom_navigation"
    const val BOTTOM_NAV_HOME = "bottom_nav_home"
    const val BOTTOM_NAV_GROCERY = "bottom_nav_grocery"
    const val BOTTOM_NAV_CHAT = "bottom_nav_chat"
    const val BOTTOM_NAV_FAVORITES = "bottom_nav_favorites"
    const val BOTTOM_NAV_STATS = "bottom_nav_stats"

    // Grocery Screen
    const val GROCERY_SCREEN = "grocery_screen"
    const val GROCERY_ITEM_PREFIX = "grocery_item_"
    const val GROCERY_CATEGORY_PREFIX = "grocery_category_"
    const val GROCERY_WHATSAPP_BUTTON = "grocery_whatsapp_button"

    // Recipe Detail Screen
    const val RECIPE_DETAIL_SCREEN = "recipe_detail_screen"
    const val RECIPE_FAVORITE_BUTTON = "recipe_favorite_button"
    const val RECIPE_START_COOKING_BUTTON = "recipe_start_cooking_button"
    const val RECIPE_SERVINGS_SELECTOR = "recipe_servings_selector"
    const val RECIPE_INGREDIENTS_LIST = "recipe_ingredients_list"
    const val RECIPE_INSTRUCTIONS_LIST = "recipe_instructions_list"

    // Chat Screen
    const val CHAT_SCREEN = "chat_screen"
    const val CHAT_INPUT_FIELD = "chat_input_field"
    const val CHAT_SEND_BUTTON = "chat_send_button"
    const val CHAT_MESSAGE_PREFIX = "chat_message_"

    // Favorites Screen
    const val FAVORITES_SCREEN = "favorites_screen"
    const val FAVORITES_LIST = "favorites_list"

    // Stats Screen
    const val STATS_SCREEN = "stats_screen"
    const val STATS_STREAK_WIDGET = "stats_streak_widget"
    const val STATS_CUISINE_CHART = "stats_cuisine_chart"

    // Settings Screen
    const val SETTINGS_SCREEN = "settings_screen"
    const val SETTINGS_MEAL_GENERATION_SECTION = "settings_meal_generation_section"
    const val SETTINGS_ITEMS_PER_MEAL = "settings_items_per_meal"
    const val SETTINGS_STRICT_ALLERGEN_TOGGLE = "settings_strict_allergen_toggle"
    const val SETTINGS_STRICT_DIETARY_TOGGLE = "settings_strict_dietary_toggle"
    const val SETTINGS_ALLOW_REPEAT_TOGGLE = "settings_allow_repeat_toggle"

    // Cooking Mode Screen
    const val COOKING_MODE_SCREEN = "cooking_mode_screen"

    // Pantry Screen
    const val PANTRY_SCREEN = "pantry_screen"

    // Recipe Rules Screen
    const val RECIPE_RULES_SCREEN = "recipe_rules_screen"
}
