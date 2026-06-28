package com.klic.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "klic_tokens")

/** Persists access + refresh tokens. Swap to EncryptedSharedPreferences before release. */
class TokenStore(private val context: Context) {
    private val accessKey = stringPreferencesKey("access")
    private val refreshKey = stringPreferencesKey("refresh")

    @Volatile var cachedAccess: String? = null
        private set

    suspend fun load() {
        cachedAccess = context.dataStore.data.first()[accessKey]
    }

    suspend fun save(access: String, refresh: String) {
        cachedAccess = access
        context.dataStore.edit {
            it[accessKey] = access
            it[refreshKey] = refresh
        }
    }

    suspend fun clear() {
        cachedAccess = null
        context.dataStore.edit { it.clear() }
    }
}
