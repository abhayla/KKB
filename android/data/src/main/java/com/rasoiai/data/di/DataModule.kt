package com.rasoiai.data.di

import android.content.Context
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.core.network.NetworkMonitorImpl
import com.rasoiai.data.local.RasoiDatabase
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.repository.FakeMealPlanRepository
import com.rasoiai.data.repository.FakeRecipeRepository
import com.rasoiai.domain.repository.MealPlanRepository
import com.rasoiai.domain.repository.RecipeRepository
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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private const val BASE_URL = "https://api.rasoiai.app/"
    private const val TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
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
    fun provideMealPlanRepository(fakeMealPlanRepository: FakeMealPlanRepository): MealPlanRepository {
        return fakeMealPlanRepository
    }

    @Provides
    @Singleton
    fun provideRecipeRepository(fakeRecipeRepository: FakeRecipeRepository): RecipeRepository {
        return fakeRecipeRepository
    }
}
