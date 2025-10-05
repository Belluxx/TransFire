package com.belluxx.transfire.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    private companion object {
        val FIREBASE_APIKEY_KEY = stringPreferencesKey("firebase_apikey")
        val FIREBASE_URL_KEY = stringPreferencesKey("firebase_url")
        val FIREBASE_PASSWORD_KEY = stringPreferencesKey("firebase_password")
        val POLL_INTERVAL_KEY = longPreferencesKey("poll_interval")
        val MODEL_NAME_KEY = stringPreferencesKey("model_name")
        val SYSTEM_PROMPT_KEY = stringPreferencesKey("system_prompt")
    }

    suspend fun saveFirebaseSettings(url: String, password: String, apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[FIREBASE_URL_KEY] = url
            preferences[FIREBASE_PASSWORD_KEY] = password
            preferences[FIREBASE_APIKEY_KEY] = apiKey
        }
    }

    suspend fun saveMiscSettings(pollInterval: Long, modelName: String, systemPrompt: String) {
        context.dataStore.edit { preferences ->
            preferences[POLL_INTERVAL_KEY] = pollInterval
            preferences[MODEL_NAME_KEY] = modelName
            preferences[SYSTEM_PROMPT_KEY] = systemPrompt
        }
    }

    suspend fun getFirebaseUrl(): String {
        val preferences = context.dataStore.data.first()
        return preferences[FIREBASE_URL_KEY] ?: ""
    }

    suspend fun getFirebasePassword(): String {
        val preferences = context.dataStore.data.first()
        return preferences[FIREBASE_PASSWORD_KEY] ?: ""
    }

    suspend fun getFirebaseApiKey(): String {
        val preferences = context.dataStore.data.first()
        return preferences[FIREBASE_APIKEY_KEY] ?: ""
    }

    suspend fun getPollingInterval(): Long {
        val preferences = context.dataStore.data.first()
        return preferences[POLL_INTERVAL_KEY] ?: 2000L
    }

    suspend fun getModelName(): String {
        val preferences = context.dataStore.data.first()
        return preferences[MODEL_NAME_KEY] ?: ""
    }

    suspend fun getSystemPrompt(): String {
        val preferences = context.dataStore.data.first()
        return preferences[SYSTEM_PROMPT_KEY] ?: ""
    }


    suspend fun clearSettings() {
        context.dataStore.edit { preferences ->
            preferences.remove(FIREBASE_URL_KEY)
            preferences.remove(FIREBASE_PASSWORD_KEY)
            preferences.remove(FIREBASE_APIKEY_KEY)
            preferences.remove(POLL_INTERVAL_KEY)
            preferences.remove(MODEL_NAME_KEY)
            preferences.remove(SYSTEM_PROMPT_KEY)
        }
    }

    suspend fun hasFirebaseSettings(): Boolean {
        val preferences = context.dataStore.data.first()
        val hasUrl = preferences[FIREBASE_URL_KEY]?.isNotBlank() ?: false
        val hasPassword = preferences[FIREBASE_PASSWORD_KEY]?.isNotBlank() ?: false
        val hasApiKey = preferences[FIREBASE_APIKEY_KEY]?.isNotBlank() ?: false

        return hasUrl && hasPassword && hasApiKey
    }
}