package com.rasoiai.data.di

import com.rasoiai.data.repository.AuthRepositoryImpl
import com.rasoiai.data.repository.HouseholdRepositoryImpl
import com.rasoiai.data.repository.ChatRepositoryImpl
import com.rasoiai.data.repository.FavoritesRepositoryImpl
import com.rasoiai.data.repository.GroceryRepositoryImpl
import com.rasoiai.data.repository.MealPlanRepositoryImpl
import com.rasoiai.data.repository.NotificationRepositoryImpl
import com.rasoiai.data.repository.PantryRepositoryImpl
import com.rasoiai.data.repository.RecipeRepositoryImpl
import com.rasoiai.data.repository.RecipeRulesRepositoryImpl
import com.rasoiai.data.repository.SettingsRepositoryImpl
import com.rasoiai.data.repository.StatsRepositoryImpl
import com.rasoiai.domain.repository.AuthRepository
import com.rasoiai.domain.repository.HouseholdRepository
import com.rasoiai.domain.repository.ChatRepository
import com.rasoiai.domain.repository.FavoritesRepository
import com.rasoiai.domain.repository.GroceryRepository
import com.rasoiai.domain.repository.MealPlanRepository
import com.rasoiai.domain.repository.NotificationRepository
import com.rasoiai.domain.repository.PantryRepository
import com.rasoiai.domain.repository.RecipeRepository
import com.rasoiai.domain.repository.RecipeRulesRepository
import com.rasoiai.domain.repository.SettingsRepository
import com.rasoiai.domain.repository.StatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds repository interfaces to their implementations.
 * Uses @Binds for efficiency - Hilt generates optimized code without provider methods.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMealPlanRepository(impl: MealPlanRepositoryImpl): MealPlanRepository

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository

    @Binds
    @Singleton
    abstract fun bindGroceryRepository(impl: GroceryRepositoryImpl): GroceryRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindPantryRepository(impl: PantryRepositoryImpl): PantryRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindRecipeRulesRepository(impl: RecipeRulesRepositoryImpl): RecipeRulesRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindHouseholdRepository(impl: HouseholdRepositoryImpl): HouseholdRepository
}
