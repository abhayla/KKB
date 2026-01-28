package com.rasoiai.app.e2e.di

import com.rasoiai.data.repository.FakeChatRepository
import com.rasoiai.data.repository.FakeFavoritesRepository
import com.rasoiai.data.repository.FakeGroceryRepository
import com.rasoiai.data.repository.FakeMealPlanRepository
import com.rasoiai.data.repository.FakePantryRepository
import com.rasoiai.data.repository.FakeRecipeRepository
import com.rasoiai.data.repository.FakeRecipeRulesRepository
import com.rasoiai.data.repository.FakeSettingsRepository
import com.rasoiai.data.repository.FakeStatsRepository
import com.rasoiai.domain.repository.ChatRepository
import com.rasoiai.domain.repository.FavoritesRepository
import com.rasoiai.domain.repository.GroceryRepository
import com.rasoiai.domain.repository.MealPlanRepository
import com.rasoiai.domain.repository.PantryRepository
import com.rasoiai.domain.repository.RecipeRepository
import com.rasoiai.domain.repository.RecipeRulesRepository
import com.rasoiai.domain.repository.SettingsRepository
import com.rasoiai.domain.repository.StatsRepository
import com.rasoiai.data.repository.AuthRepositoryImpl
import com.rasoiai.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test module that replaces real repositories with fakes for E2E testing.
 * This allows tests to run without network or database dependencies.
 *
 * Note: This replaces RepositoryModule, which binds repository interfaces
 * to their implementations. DataModule provides the infrastructure (DAOs, etc.)
 * which we still need.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [com.rasoiai.data.di.RepositoryModule::class]
)
abstract class TestDataModule {

    @Binds
    @Singleton
    abstract fun bindMealPlanRepository(
        fakeRepository: FakeMealPlanRepository
    ): MealPlanRepository

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(
        fakeRepository: FakeRecipeRepository
    ): RecipeRepository

    @Binds
    @Singleton
    abstract fun bindGroceryRepository(
        fakeRepository: FakeGroceryRepository
    ): GroceryRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        fakeRepository: FakeChatRepository
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(
        fakeRepository: FakeFavoritesRepository
    ): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindPantryRepository(
        fakeRepository: FakePantryRepository
    ): PantryRepository

    @Binds
    @Singleton
    abstract fun bindRecipeRulesRepository(
        fakeRepository: FakeRecipeRulesRepository
    ): RecipeRulesRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        fakeRepository: FakeSettingsRepository
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(
        fakeRepository: FakeStatsRepository
    ): StatsRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository
}
