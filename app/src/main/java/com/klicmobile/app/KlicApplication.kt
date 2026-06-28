package com.klicmobile.app

import android.app.Application
import com.klicmobile.app.calling.CallManager
import com.klicmobile.app.calling.CallNotifications
import com.klicmobile.app.data.KlicRepository
import com.klicmobile.app.data.Network
import com.klicmobile.app.data.TokenStore
import com.klicmobile.app.realtime.SocketService

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

    private val prefs = app.getSharedPreferences("klic_prefs", android.content.Context.MODE_PRIVATE)
    var isDark: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) { prefs.edit().putBoolean("dark_theme", value).apply() }
}
