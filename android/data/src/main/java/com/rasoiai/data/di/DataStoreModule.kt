package com.rasoiai.data.di

import com.rasoiai.data.local.datastore.UserPreferencesDataStore
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides UserPreferencesDataStore bindings.
 * This can be replaced in tests with FakeDataStoreModule.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {

    @Binds
    @Singleton
    abstract fun bindUserPreferencesDataStore(
        impl: UserPreferencesDataStore
    ): UserPreferencesDataStoreInterface
}
