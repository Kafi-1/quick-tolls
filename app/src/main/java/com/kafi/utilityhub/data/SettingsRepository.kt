package com.kafi.utilityhub.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_SPEED_NOTIFICATION = booleanPreferencesKey("show_speed_notification")
        val SHOW_FLOATING_SPEED = booleanPreferencesKey("show_floating_speed")
        val FLOATING_CAPTURE = booleanPreferencesKey("floating_capture")
        val FLOATING_ALPHA = intPreferencesKey("floating_alpha")
        val RECORD_RESOLUTION = stringPreferencesKey("record_resolution")
        val RECORD_FPS = intPreferencesKey("record_fps")
        val RECORD_BITRATE_KBPS = intPreferencesKey("record_bitrate_kbps")
        val RECORD_MIC = booleanPreferencesKey("record_mic")
        val SCREENSHOT_FORMAT = stringPreferencesKey("screenshot_format")
        val SCREENSHOT_QUALITY = intPreferencesKey("screenshot_quality")
        val SCREENSHOT_COUNTDOWN = intPreferencesKey("screenshot_countdown")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "system" }
    val showSpeedNotification: Flow<Boolean> = context.dataStore.data.map { it[SHOW_SPEED_NOTIFICATION] ?: false }
    val showFloatingSpeed: Flow<Boolean> = context.dataStore.data.map { it[SHOW_FLOATING_SPEED] ?: false }
    val floatingCapture: Flow<Boolean> = context.dataStore.data.map { it[FLOATING_CAPTURE] ?: false }
    val floatingAlpha: Flow<Int> = context.dataStore.data.map { it[FLOATING_ALPHA] ?: 80 }
    val recordResolution: Flow<String> = context.dataStore.data.map { it[RECORD_RESOLUTION] ?: "720p" }
    val recordFps: Flow<Int> = context.dataStore.data.map { it[RECORD_FPS] ?: 30 }
    val recordBitrateKbps: Flow<Int> = context.dataStore.data.map { it[RECORD_BITRATE_KBPS] ?: 6000 }
    val recordMic: Flow<Boolean> = context.dataStore.data.map { it[RECORD_MIC] ?: false }
    val screenshotFormat: Flow<String> = context.dataStore.data.map { it[SCREENSHOT_FORMAT] ?: "png" }
    val screenshotQuality: Flow<Int> = context.dataStore.data.map { it[SCREENSHOT_QUALITY] ?: 100 }

    suspend fun setString(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setInt(key: Preferences.Key<Int>, value: Int) {
        context.dataStore.edit { it[key] = value }
    }
}
