package com.rasoiai.app.e2e.di

import com.rasoiai.data.di.DataStoreModule
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test module that replaces DataStoreModule with fake implementations.
 * Allows E2E tests to control onboarding and authentication state.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataStoreModule::class]
)
abstract class FakeDataStoreModule {

    @Binds
    @Singleton
    abstract fun bindUserPreferencesDataStore(
        impl: FakeUserPreferencesDataStore
    ): UserPreferencesDataStoreInterface
}
