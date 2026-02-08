package com.rasoiai.data.di

import android.content.Context
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
import com.rasoiai.data.local.dao.NotificationDao
import com.rasoiai.data.local.dao.OfflineQueueDao
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.interceptor.AuthInterceptor
import com.rasoiai.data.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for API service with extended read timeout for AI-powered endpoints
 * (meal generation, chat) where backend calls Gemini and can take 45-90s.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LongTimeout

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // Note: Base URL should NOT include /api/v1/ since endpoint paths already include it
    // BASE_URL is now configured via BuildConfig in build.gradle.kts
    // - Debug: http://10.0.2.2:8000/ (emulator accessing host machine)
    // - Release: https://api.rasoiai.com/
    private const val TIMEOUT_SECONDS = 30L
    private const val LONG_READ_TIMEOUT_SECONDS = 120L

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
            .baseUrl(BuildConfig.BASE_URL)
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
    @LongTimeout
    fun provideLongTimeoutApiService(okHttpClient: OkHttpClient): RasoiApiService {
        val longTimeoutClient = okHttpClient.newBuilder()
            .readTimeout(LONG_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(longTimeoutClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RasoiApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRasoiDatabase(@ApplicationContext context: Context): RasoiDatabase {
        return RasoiDatabase.create(context)
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
    fun provideNotificationDao(database: RasoiDatabase): NotificationDao {
        return database.notificationDao()
    }

    @Provides
    @Singleton
    fun provideOfflineQueueDao(database: RasoiDatabase): OfflineQueueDao {
        return database.offlineQueueDao()
    }

    // Repository bindings moved to RepositoryModule using @Binds for efficiency
}
