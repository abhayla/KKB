package com.rasoiai.app.di

import com.rasoiai.app.presentation.auth.GoogleAuthClient
import com.rasoiai.app.presentation.auth.GoogleAuthClientInterface
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides auth-related dependencies.
 * This module binds the real GoogleAuthClient implementation.
 * For tests, this is replaced by TestAuthModule.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindGoogleAuthClient(
        impl: GoogleAuthClient
    ): GoogleAuthClientInterface
}
