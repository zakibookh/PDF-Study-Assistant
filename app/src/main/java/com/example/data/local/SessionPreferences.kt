package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.data.model.UserDto
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "studypdf_prefs")

class SessionPreferences(
    private val context: Context,
    private val moshi: Moshi,
) {
    private object Keys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val CACHED_USER = stringPreferencesKey("cached_user")
        val PREFERRED_MODE = stringPreferencesKey("preferred_mode")
    }

    private val userAdapter = moshi.adapter(UserDto::class.java)

    val authTokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.AUTH_TOKEN] }

    val apiBaseUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        normalizeBaseUrl(prefs[Keys.API_BASE_URL] ?: BuildConfig.API_BASE_URL)
    }

    val preferredModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.PREFERRED_MODE] ?: "smart_search"
    }

    suspend fun getAuthToken(): String? = authTokenFlow.first()

    suspend fun getApiBaseUrl(): String = apiBaseUrlFlow.first()

    suspend fun getCachedUser(): UserDto? {
        val raw = context.dataStore.data.map { it[Keys.CACHED_USER] }.first() ?: return null
        return runCatching { userAdapter.fromJson(raw) }.getOrNull()
    }

    suspend fun saveSession(token: String, user: UserDto) {
        val userJson = userAdapter.toJson(user)
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTH_TOKEN] = token
            prefs[Keys.CACHED_USER] = userJson
        }
    }

    suspend fun saveCachedUser(user: UserDto) {
        val userJson = userAdapter.toJson(user)
        context.dataStore.edit { prefs ->
            prefs[Keys.CACHED_USER] = userJson
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.CACHED_USER)
        }
    }

    suspend fun saveApiBaseUrl(url: String): String {
        val normalized = normalizeBaseUrl(url)
        context.dataStore.edit { prefs ->
            prefs[Keys.API_BASE_URL] = normalized
        }
        return normalized
    }

    suspend fun resetApiBaseUrl(): String {
        val defaultUrl = normalizeBaseUrl(BuildConfig.API_BASE_URL)
        context.dataStore.edit { prefs ->
            prefs[Keys.API_BASE_URL] = defaultUrl
        }
        return defaultUrl
    }

    suspend fun savePreferredMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PREFERRED_MODE] = mode
        }
    }

    companion object {
        fun normalizeBaseUrl(raw: String): String {
            val trimmed = raw.trim().ifEmpty { BuildConfig.API_BASE_URL }
            val withScheme = if (
                trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "https://$trimmed"
            }
            return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
        }
    }
}
