package com.klic.app

import android.app.Application
import com.klic.app.calling.CallManager
import com.klic.app.calling.CallNotifications
import com.klic.app.data.KlicRepository
import com.klic.app.data.Network
import com.klic.app.data.TokenStore
import com.klic.app.realtime.SocketService

/** Tiny manual DI container — swap for Hilt as the app grows. */
class KlicApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CallNotifications.createChannels(this)
    }
}

class AppContainer(app: Application) {
    val tokenStore = TokenStore(app)
    val repository = KlicRepository(Network.create(tokenStore), tokenStore)
    val socket = SocketService()
    val callManager = CallManager(app)
}
