package com.rasoiai.app.e2e.di

import com.rasoiai.core.network.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.InstallIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Singleton

/**
 * Test module that provides a fake NetworkMonitor for offline testing.
 * This module is installed alongside other modules and provides FakeNetworkMonitor
 * for tests that need to control network state.
 *
 * Note: This doesn't replace any module - it just provides an additional binding
 * for FakeNetworkMonitor that tests can inject.
 */
@Module
@InstallIn(SingletonComponent::class)
object FakeNetworkModule {

    private val _fakeNetworkMonitor = FakeNetworkMonitor()

    @Provides
    @Singleton
    fun provideFakeNetworkMonitor(): FakeNetworkMonitor {
        return _fakeNetworkMonitor
    }
}

/**
 * Fake NetworkMonitor that allows tests to control online/offline state.
 */
class FakeNetworkMonitor : NetworkMonitor {

    private val _isOnline = MutableStateFlow(true)
    override val isOnline: Flow<Boolean> = _isOnline

    /**
     * Set the network state for testing.
     */
    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }

    /**
     * Simulate going offline.
     */
    fun goOffline() {
        _isOnline.value = false
    }

    /**
     * Simulate coming back online.
     */
    fun goOnline() {
        _isOnline.value = true
    }
}
