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
import com.rasoiai.data.local.entity.MealPlanEntity
import com.rasoiai.data.local.entity.MealPlanItemEntity
import com.rasoiai.data.local.entity.RecipeEntity
import com.rasoiai.data.local.entity.GroceryItemEntity
import com.rasoiai.data.local.entity.FavoriteEntity

@Database(
    entities = [
        MealPlanEntity::class,
        MealPlanItemEntity::class,
        RecipeEntity::class,
        GroceryItemEntity::class,
        FavoriteEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RasoiDatabase : RoomDatabase() {

    abstract fun mealPlanDao(): MealPlanDao
    abstract fun recipeDao(): RecipeDao
    abstract fun groceryDao(): GroceryDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        private const val DATABASE_NAME = "rasoi_database"

        fun create(context: Context): RasoiDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                RasoiDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
