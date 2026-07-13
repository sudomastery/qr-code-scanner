package com.sudomastery.qrscanner.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class Settings(
    val vibrateOnScan: Boolean = true,
    val beepOnScan: Boolean = false,
    val autoOpenLinks: Boolean = false,
    val removeTrackers: Boolean = true,
    val saveHistory: Boolean = true,
    val autoVaultOtp: Boolean = false,
    val themeColor: String = "blue"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val vibrate = booleanPreferencesKey("vibrate_on_scan")
        val beep = booleanPreferencesKey("beep_on_scan")
        val autoOpen = booleanPreferencesKey("auto_open_links")
        val removeTrackers = booleanPreferencesKey("remove_trackers")
        val saveHistory = booleanPreferencesKey("save_history")
        val autoVaultOtp = booleanPreferencesKey("auto_vault_otp")
        val themeColor = stringPreferencesKey("theme_color")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            vibrateOnScan = prefs[Keys.vibrate] ?: true,
            beepOnScan = prefs[Keys.beep] ?: false,
            autoOpenLinks = prefs[Keys.autoOpen] ?: false,
            removeTrackers = prefs[Keys.removeTrackers] ?: true,
            saveHistory = prefs[Keys.saveHistory] ?: true,
            autoVaultOtp = prefs[Keys.autoVaultOtp] ?: false,
            themeColor = prefs[Keys.themeColor] ?: "blue"
        )
    }

    suspend fun setVibrateOnScan(value: Boolean) =
        context.dataStore.edit { it[Keys.vibrate] = value }

    suspend fun setBeepOnScan(value: Boolean) =
        context.dataStore.edit { it[Keys.beep] = value }

    suspend fun setAutoOpenLinks(value: Boolean) =
        context.dataStore.edit { it[Keys.autoOpen] = value }

    suspend fun setRemoveTrackers(value: Boolean) =
        context.dataStore.edit { it[Keys.removeTrackers] = value }

    suspend fun setSaveHistory(value: Boolean) =
        context.dataStore.edit { it[Keys.saveHistory] = value }

    suspend fun setAutoVaultOtp(value: Boolean) =
        context.dataStore.edit { it[Keys.autoVaultOtp] = value }

    suspend fun setThemeColor(value: String) =
        context.dataStore.edit { it[Keys.themeColor] = value }
}
