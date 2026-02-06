package com.rasoiai.app.e2e.di

import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.di.NetworkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Singleton

/**
 * Test module that REPLACES NetworkModule with fake implementation.
 * Uses @TestInstallIn to ensure all code injecting NetworkMonitor
 * receives FakeNetworkMonitor during E2E tests.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
object FakeNetworkModule {

    private val _fakeNetworkMonitor = FakeNetworkMonitor()

    @Provides
    @Singleton
    fun provideNetworkMonitor(): NetworkMonitor {
        return _fakeNetworkMonitor
    }

    @Provides
    @Singleton
    fun provideFakeNetworkMonitor(): FakeNetworkMonitor {
        return _fakeNetworkMonitor
    }
}

/**
 * Fake NetworkMonitor for tests. Defaults to ONLINE.
 */
class FakeNetworkMonitor : NetworkMonitor {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: Flow<Boolean> = _isOnline

    fun setOnline(online: Boolean) { _isOnline.value = online }
    fun goOffline() { _isOnline.value = false }
    fun goOnline() { _isOnline.value = true }
    fun reset() { _isOnline.value = true }
}
