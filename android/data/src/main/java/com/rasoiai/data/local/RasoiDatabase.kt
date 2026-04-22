package com.rasoiai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rasoiai.data.local.dao.MealPlanDao
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.dao.GroceryDao
import com.rasoiai.data.local.dao.FavoriteDao
import com.rasoiai.data.local.dao.CollectionDao
import com.rasoiai.data.local.dao.PantryDao
import com.rasoiai.data.local.dao.StatsDao
import com.rasoiai.data.local.dao.RecipeRulesDao
import com.rasoiai.data.local.dao.ChatDao
import com.rasoiai.data.local.dao.NotificationDao
import com.rasoiai.data.local.dao.HouseholdDao
import com.rasoiai.data.local.dao.OfflineQueueDao
import com.rasoiai.data.local.entity.HouseholdEntity
import com.rasoiai.data.local.entity.HouseholdMemberEntity
import com.rasoiai.data.local.entity.MealPlanEntity
import com.rasoiai.data.local.entity.MealPlanFestivalEntity
import com.rasoiai.data.local.entity.MealPlanItemEntity
import com.rasoiai.data.local.entity.RecipeEntity
import com.rasoiai.data.local.entity.GroceryItemEntity
import com.rasoiai.data.local.entity.FavoriteEntity
import com.rasoiai.data.local.entity.FavoriteCollectionEntity
import com.rasoiai.data.local.entity.RecentlyViewedEntity
import com.rasoiai.data.local.entity.PantryItemEntity
import com.rasoiai.data.local.entity.CookingStreakEntity
import com.rasoiai.data.local.entity.CookingDayEntity
import com.rasoiai.data.local.entity.AchievementEntity
import com.rasoiai.data.local.entity.WeeklyChallengeEntity
import com.rasoiai.data.local.entity.RecipeRuleEntity
import com.rasoiai.data.local.entity.NutritionGoalEntity
import com.rasoiai.data.local.entity.ChatMessageEntity
import com.rasoiai.data.local.entity.NotificationEntity
import com.rasoiai.data.local.entity.OfflineQueueEntity
import com.rasoiai.data.local.entity.CookedRecipeEntity
import com.rasoiai.data.local.entity.KnownIngredientEntity

@Database(
    entities = [
        MealPlanEntity::class,
        MealPlanItemEntity::class,
        MealPlanFestivalEntity::class,
        RecipeEntity::class,
        GroceryItemEntity::class,
        FavoriteEntity::class,
        FavoriteCollectionEntity::class,
        RecentlyViewedEntity::class,
        PantryItemEntity::class,
        CookingStreakEntity::class,
        CookingDayEntity::class,
        AchievementEntity::class,
        WeeklyChallengeEntity::class,
        RecipeRuleEntity::class,
        NutritionGoalEntity::class,
        ChatMessageEntity::class,
        NotificationEntity::class,
        OfflineQueueEntity::class,
        CookedRecipeEntity::class,
        KnownIngredientEntity::class,
        HouseholdEntity::class,
        HouseholdMemberEntity::class
    ],
    version = 16,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RasoiDatabase : RoomDatabase() {

    abstract fun mealPlanDao(): MealPlanDao
    abstract fun recipeDao(): RecipeDao
    abstract fun groceryDao(): GroceryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun collectionDao(): CollectionDao
    abstract fun pantryDao(): PantryDao
    abstract fun statsDao(): StatsDao
    abstract fun recipeRulesDao(): RecipeRulesDao
    abstract fun chatDao(): ChatDao
    abstract fun notificationDao(): NotificationDao
    abstract fun offlineQueueDao(): OfflineQueueDao
    abstract fun householdDao(): HouseholdDao

    companion object {
        private const val DATABASE_NAME = "rasoi_database"

        /**
         * Migration from version 8 to 9: Add cooked_recipes table for cuisine breakdown stats.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Recreate recipe_rules: rename mealSlot→mealSlots, add syncStatus
                //    Cannot use ALTER TABLE RENAME COLUMN (requires SQLite 3.25+, min SDK 24 has 3.9)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipe_rules_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL,
                        action TEXT NOT NULL,
                        targetId TEXT NOT NULL,
                        targetName TEXT NOT NULL,
                        frequencyType TEXT NOT NULL,
                        frequencyCount INTEGER,
                        frequencyDays TEXT,
                        enforcement TEXT NOT NULL,
                        mealSlots TEXT,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        syncStatus TEXT NOT NULL DEFAULT 'SYNCED',
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO recipe_rules_new (id, type, action, targetId, targetName,
                        frequencyType, frequencyCount, frequencyDays, enforcement, mealSlots,
                        isActive, syncStatus, createdAt, updatedAt)
                    SELECT id, type, action, targetId, targetName,
                        frequencyType, frequencyCount, frequencyDays, enforcement, mealSlot,
                        isActive, 'SYNCED', createdAt, updatedAt
                    FROM recipe_rules
                """)
                db.execSQL("DROP TABLE recipe_rules")
                db.execSQL("ALTER TABLE recipe_rules_new RENAME TO recipe_rules")

                // 2. Add syncStatus to nutrition_goals
                db.execSQL("ALTER TABLE nutrition_goals ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")

                // 3. Create cooked_recipes table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cooked_recipes (
                        id TEXT NOT NULL PRIMARY KEY,
                        recipeId TEXT NOT NULL,
                        recipeName TEXT NOT NULL,
                        cuisineType TEXT NOT NULL,
                        cookedDate TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cooked_recipes_cuisineType ON cooked_recipes (cuisineType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cooked_recipes_cookedDate ON cooked_recipes (cookedDate)")
            }
        }

        /**
         * Migration from version 9 to 10: Add known_ingredients table for Recipe Rules search.
         * Seeds with popular Indian cooking ingredients so suggestions work immediately.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS known_ingredients (
                        name TEXT NOT NULL PRIMARY KEY,
                        source TEXT NOT NULL,
                        addedAt INTEGER NOT NULL
                    )
                """)

                // Seed with popular ingredients
                seedKnownIngredients(db)
            }
        }

        /**
         * Migration from version 7 to 8: Add notifications and offline_queue tables.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create notifications table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS notifications (
                        id TEXT NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        imageUrl TEXT,
                        actionType TEXT,
                        actionData TEXT,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        expiresAt INTEGER
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_isRead ON notifications (isRead)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_createdAt ON notifications (createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_type ON notifications (type)")

                // Create offline_queue table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS offline_queue (
                        id TEXT NOT NULL PRIMARY KEY,
                        actionType TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'pending',
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        errorMessage TEXT,
                        createdAt INTEGER NOT NULL,
                        lastAttemptAt INTEGER
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_queue_status ON offline_queue (status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_queue_createdAt ON offline_queue (createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_queue_actionType ON offline_queue (actionType)")
            }
        }

        /**
         * Migration from version 10 to 11: Recreate meal_plan_items with proper id PK.
         * Old composite PK (mealPlanId, date, mealType, recipeId) caused deduplication
         * when recipe_id was empty. New PK uses the backend-provided unique item id.
         * Data is a cache and will be re-fetched from the backend.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS meal_plan_items")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS meal_plan_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        mealPlanId TEXT NOT NULL,
                        date TEXT NOT NULL,
                        dayName TEXT NOT NULL,
                        mealType TEXT NOT NULL,
                        recipeId TEXT NOT NULL,
                        recipeName TEXT NOT NULL,
                        recipeImageUrl TEXT,
                        prepTimeMinutes INTEGER NOT NULL,
                        calories INTEGER NOT NULL,
                        dietaryTags TEXT NOT NULL,
                        isLocked INTEGER NOT NULL DEFAULT 0,
                        `order` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (mealPlanId) REFERENCES meal_plans(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_plan_items_mealPlanId ON meal_plan_items (mealPlanId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_plan_items_date ON meal_plan_items (date)")
            }
        }

        /**
         * Popular ingredients seeded into known_ingredients table on fresh install
         * and during migration 9→10.
         */
        internal val SEED_INGREDIENTS = listOf(
            "Paneer", "Chicken", "Mutton", "Fish", "Prawns", "Egg", "Tofu",
            "Dal", "Chana Dal", "Moong Dal", "Toor Dal", "Masoor Dal",
            "Rajma", "Chole",
            "Aloo", "Tamatar", "Pyaz", "Palak", "Gobi",
            "Matar", "Bhindi", "Baingan", "Gajar", "Shimla Mirch",
            "Mushroom", "Methi", "Karela", "Lauki", "Bandh Gobi",
            "Dahi", "Ghee", "Malai",
            "Chawal", "Atta", "Suji", "Besan",
            "Chai", "Moringa",
            "Cashew", "Badam", "Nariyal"
        )

        private fun seedKnownIngredients(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            for (ingredient in SEED_INGREDIENTS) {
                db.execSQL(
                    "INSERT OR IGNORE INTO known_ingredients (name, source, addedAt) VALUES (?, 'popular', ?)",
                    arrayOf(ingredient, now)
                )
            }
        }

        /**
         * Migration from version 11 to 12: Add forceOverride column to recipe_rules.
         * Tracks when user explicitly overrode a family safety conflict.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipe_rules ADD COLUMN forceOverride INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration from version 12 to 13: Add household tables.
         * Households and household_members for family/household management.
         */
        /**
         * Migration from version 13 to 14: Add day/meal lock columns to meal_plan_items.
         * Persists lock states that were previously UI-only (lost on restart).
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal_plan_items ADD COLUMN isDayLocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE meal_plan_items ADD COLUMN isMealTypeLocked INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration from version 14 to 15: Rename `order` column -> `item_order`.
         *
         * `order` is a SQL reserved keyword. Room does not reliably backtick-quote
         * it in generated INSERT statements, causing failures on stricter SQLite
         * versions (e.g., API 29 emulator). Renaming avoids the collision entirely.
         *
         * SQLite on min-SDK 24 (Android 7.0) predates ALTER TABLE RENAME COLUMN
         * (added in SQLite 3.25). Use the standard "recreate table" dance.
         *
         * Downgrade behavior: rolls back to a table with `order` column; data
         * preserved. Re-upgrade re-renames the column.
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE meal_plan_items_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        mealPlanId TEXT NOT NULL,
                        date TEXT NOT NULL,
                        dayName TEXT NOT NULL,
                        mealType TEXT NOT NULL,
                        recipeId TEXT NOT NULL,
                        recipeName TEXT NOT NULL,
                        recipeImageUrl TEXT,
                        prepTimeMinutes INTEGER NOT NULL,
                        calories INTEGER NOT NULL,
                        dietaryTags TEXT NOT NULL,
                        isLocked INTEGER NOT NULL DEFAULT 0,
                        isDayLocked INTEGER NOT NULL DEFAULT 0,
                        isMealTypeLocked INTEGER NOT NULL DEFAULT 0,
                        item_order INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (mealPlanId) REFERENCES meal_plans(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("""
                    INSERT INTO meal_plan_items_new
                    (id, mealPlanId, date, dayName, mealType, recipeId, recipeName,
                     recipeImageUrl, prepTimeMinutes, calories, dietaryTags, isLocked,
                     isDayLocked, isMealTypeLocked, item_order)
                    SELECT
                     id, mealPlanId, date, dayName, mealType, recipeId, recipeName,
                     recipeImageUrl, prepTimeMinutes, calories, dietaryTags, isLocked,
                     isDayLocked, isMealTypeLocked, `order`
                    FROM meal_plan_items
                """)
                db.execSQL("DROP TABLE meal_plan_items")
                db.execSQL("ALTER TABLE meal_plan_items_new RENAME TO meal_plan_items")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_plan_items_mealPlanId ON meal_plan_items (mealPlanId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_plan_items_date ON meal_plan_items (date)")
            }
        }

        /**
         * Migration from version 15 to 16: Add rating aggregate columns to recipes.
         *
         * Satisfies the deferred offline-caching acceptance criterion from #21.
         * Backend RecipeResponse already carries average_rating / rating_count /
         * user_rating (commit a48fb10). Without persisting them on RecipeEntity
         * the aggregate was dropped at the Room layer, so the next offline open
         * showed stale or missing ratings.
         *
         * Additive-only migration. Existing rows get NULL/0 defaults. No data
         * loss. Downgrade would drop the three columns (data is a cache and
         * refetched from the backend).
         */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN averageRating REAL")
                db.execSQL("ALTER TABLE recipes ADD COLUMN ratingCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE recipes ADD COLUMN userRating REAL")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS households (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        inviteCode TEXT NOT NULL,
                        ownerId TEXT NOT NULL,
                        slotConfigJson TEXT,
                        maxMembers INTEGER NOT NULL DEFAULT 8,
                        memberCount INTEGER NOT NULL DEFAULT 0,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS household_members (
                        id TEXT NOT NULL PRIMARY KEY,
                        householdId TEXT NOT NULL,
                        userId TEXT,
                        familyMemberId TEXT,
                        name TEXT NOT NULL,
                        role TEXT NOT NULL,
                        canEditSharedPlan INTEGER NOT NULL DEFAULT 0,
                        isTemporary INTEGER NOT NULL DEFAULT 0,
                        joinDate TEXT NOT NULL,
                        leaveDate TEXT,
                        portionSize REAL NOT NULL DEFAULT 1.0,
                        status TEXT NOT NULL DEFAULT 'active',
                        FOREIGN KEY (householdId) REFERENCES households(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_household_members_householdId ON household_members (householdId)")
            }
        }

        fun create(context: Context): RasoiDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                RasoiDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedKnownIngredients(db)
                    }
                })
                .build()
        }
    }
}
