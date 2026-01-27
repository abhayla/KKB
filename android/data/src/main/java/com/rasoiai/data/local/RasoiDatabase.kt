package com.rasoiai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rasoiai.data.local.dao.MealPlanDao
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.dao.GroceryDao
import com.rasoiai.data.local.dao.FavoriteDao
import com.rasoiai.data.local.dao.CollectionDao
import com.rasoiai.data.local.dao.PantryDao
import com.rasoiai.data.local.dao.StatsDao
import com.rasoiai.data.local.dao.RecipeRulesDao
import com.rasoiai.data.local.dao.ChatDao
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
        ChatMessageEntity::class
    ],
    version = 7,
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

    companion object {
        private const val DATABASE_NAME = "rasoi_database"

        fun create(context: Context): RasoiDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                RasoiDatabase::class.java,
                DATABASE_NAME
            )
                // NOTE: Implement proper migrations before production release.
                // fallbackToDestructiveMigration() was removed to prevent data loss.
                // Add migrations for each schema version change:
                // .addMigrations(MIGRATION_7_8, MIGRATION_8_9, etc.)
                .build()
        }
    }
}
