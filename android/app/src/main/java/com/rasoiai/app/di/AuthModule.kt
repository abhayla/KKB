package com.rasoiai.app.di

import com.rasoiai.app.presentation.auth.PhoneAuthClient
import com.rasoiai.app.presentation.auth.PhoneAuthClientInterface
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides auth-related dependencies.
 * This module binds the real PhoneAuthClient implementation.
 * For tests, this is replaced by TestAuthModule.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindPhoneAuthClient(
        impl: PhoneAuthClient
    ): PhoneAuthClientInterface
}
