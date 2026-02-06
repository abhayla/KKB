package com.rasoiai.data.di

import android.content.Context
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.core.network.NetworkMonitorImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides network-related dependencies.
 * Separated from DataModule to allow tests to replace NetworkMonitor
 * without replacing all data dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitorImpl(context)
    }
}
