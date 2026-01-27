package com.rasoiai.data.di

import android.content.Context
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.core.network.NetworkMonitorImpl
import com.rasoiai.data.local.RasoiDatabase
import com.rasoiai.data.local.dao.CollectionDao
import com.rasoiai.data.local.dao.FavoriteDao
import com.rasoiai.data.local.dao.GroceryDao
import com.rasoiai.data.local.dao.MealPlanDao
import com.rasoiai.data.local.dao.PantryDao
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.dao.StatsDao
import com.rasoiai.data.local.dao.RecipeRulesDao
import com.rasoiai.data.local.dao.ChatDao
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.interceptor.AuthInterceptor
import com.rasoiai.data.repository.AuthRepositoryImpl
import com.rasoiai.data.repository.MealPlanRepositoryImpl
import com.rasoiai.data.repository.RecipeRepositoryImpl
import com.rasoiai.data.repository.GroceryRepositoryImpl
import com.rasoiai.data.repository.FavoritesRepositoryImpl
import com.rasoiai.data.repository.PantryRepositoryImpl
import com.rasoiai.data.repository.StatsRepositoryImpl
import com.rasoiai.data.repository.SettingsRepositoryImpl
import com.rasoiai.data.repository.RecipeRulesRepositoryImpl
import com.rasoiai.data.repository.ChatRepositoryImpl
import com.rasoiai.domain.repository.AuthRepository
import com.rasoiai.domain.repository.ChatRepository
import com.rasoiai.domain.repository.FavoritesRepository
import com.rasoiai.domain.repository.GroceryRepository
import com.rasoiai.domain.repository.MealPlanRepository
import com.rasoiai.domain.repository.PantryRepository
import com.rasoiai.domain.repository.RecipeRepository
import com.rasoiai.domain.repository.RecipeRulesRepository
import com.rasoiai.domain.repository.SettingsRepository
import com.rasoiai.domain.repository.StatsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.rasoiai.data.BuildConfig
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private const val BASE_URL = "https://api.rasoiai.app/"
    private const val TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Use BASIC in debug to avoid logging sensitive data like JWT tokens
            // Use NONE in release builds for security and performance
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // Add auth header to requests
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideRasoiApiService(retrofit: Retrofit): RasoiApiService {
        return retrofit.create(RasoiApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRasoiDatabase(@ApplicationContext context: Context): RasoiDatabase {
        return RasoiDatabase.create(context)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitorImpl(context)
    }

    @Provides
    @Singleton
    fun provideMealPlanDao(database: RasoiDatabase): MealPlanDao {
        return database.mealPlanDao()
    }

    @Provides
    @Singleton
    fun provideRecipeDao(database: RasoiDatabase): RecipeDao {
        return database.recipeDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteDao(database: RasoiDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    @Singleton
    fun provideGroceryDao(database: RasoiDatabase): GroceryDao {
        return database.groceryDao()
    }

    @Provides
    @Singleton
    fun provideCollectionDao(database: RasoiDatabase): CollectionDao {
        return database.collectionDao()
    }

    @Provides
    @Singleton
    fun providePantryDao(database: RasoiDatabase): PantryDao {
        return database.pantryDao()
    }

    @Provides
    @Singleton
    fun provideStatsDao(database: RasoiDatabase): StatsDao {
        return database.statsDao()
    }

    @Provides
    @Singleton
    fun provideRecipeRulesDao(database: RasoiDatabase): RecipeRulesDao {
        return database.recipeRulesDao()
    }

    @Provides
    @Singleton
    fun provideChatDao(database: RasoiDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository {
        return authRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideMealPlanRepository(mealPlanRepositoryImpl: MealPlanRepositoryImpl): MealPlanRepository {
        return mealPlanRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideRecipeRepository(recipeRepositoryImpl: RecipeRepositoryImpl): RecipeRepository {
        return recipeRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideGroceryRepository(groceryRepositoryImpl: GroceryRepositoryImpl): GroceryRepository {
        return groceryRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideFavoritesRepository(favoritesRepositoryImpl: FavoritesRepositoryImpl): FavoritesRepository {
        return favoritesRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideChatRepository(chatRepositoryImpl: ChatRepositoryImpl): ChatRepository {
        return chatRepositoryImpl
    }

    @Provides
    @Singleton
    fun providePantryRepository(pantryRepositoryImpl: PantryRepositoryImpl): PantryRepository {
        return pantryRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideStatsRepository(statsRepositoryImpl: StatsRepositoryImpl): StatsRepository {
        return statsRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(settingsRepositoryImpl: SettingsRepositoryImpl): SettingsRepository {
        return settingsRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideRecipeRulesRepository(recipeRulesRepositoryImpl: RecipeRulesRepositoryImpl): RecipeRulesRepository {
        return recipeRulesRepositoryImpl
    }
}
