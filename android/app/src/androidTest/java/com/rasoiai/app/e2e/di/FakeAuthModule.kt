package com.rasoiai.app.e2e.di

import com.rasoiai.app.di.AuthModule
import com.rasoiai.app.presentation.auth.PhoneAuthClientInterface
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test module that replaces AuthModule with fake implementations.
 * Bypasses real Firebase Phone Auth for E2E testing.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AuthModule::class]
)
abstract class FakeAuthModule {

    @Binds
    @Singleton
    abstract fun bindPhoneAuthClient(
        impl: FakePhoneAuthClient
    ): PhoneAuthClientInterface
}
