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
import com.rasoiai.data.local.dao.OfflineQueueDao
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
        KnownIngredientEntity::class
    ],
    version = 10,
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

        fun create(context: Context): RasoiDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                RasoiDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
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
