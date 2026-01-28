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
    const val GOOGLE_SIGN_IN_BUTTON = "google_sign_in_button"
    const val AUTH_WELCOME_TEXT = "auth_welcome_text"

    // Home Screen
    const val HOME_SCREEN = "home_screen"
    const val HOME_WEEK_SELECTOR = "home_week_selector"
    const val HOME_DAY_TAB_PREFIX = "home_day_tab_"
    const val MEAL_CARD_PREFIX = "meal_card_"
    const val MEAL_LOCK_BUTTON_PREFIX = "meal_lock_"
    const val MEAL_SWAP_BUTTON_PREFIX = "meal_swap_"

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

    // Cooking Mode Screen
    const val COOKING_MODE_SCREEN = "cooking_mode_screen"

    // Pantry Screen
    const val PANTRY_SCREEN = "pantry_screen"

    // Recipe Rules Screen
    const val RECIPE_RULES_SCREEN = "recipe_rules_screen"
}
