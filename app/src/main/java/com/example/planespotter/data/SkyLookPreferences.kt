package com.example.planespotter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.planespotter.notifications.AlertFrequency
import com.example.planespotter.notifications.AlertSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "skylook")

private object PrefsKeys {
    val TRACKED_IDS    = stringSetPreferencesKey("tracked_ids")
    val UNIT_SYSTEM    = stringPreferencesKey("unit_system")
    val LABEL_MODE     = stringPreferencesKey("label_mode")
    val THEME_MODE     = stringPreferencesKey("theme_mode")
    val RADAR_SCALE    = stringPreferencesKey("radar_scale")
    val ALERT_OVERHEAD = booleanPreferencesKey("alert_overhead")
    val ALERT_RARE     = booleanPreferencesKey("alert_rare")
    val ALERT_TRAFFIC  = booleanPreferencesKey("alert_traffic")
    val ALERT_FREQ     = stringPreferencesKey("alert_freq")
}

data class SkyLookSettings(
    val trackedIds: Set<String> = emptySet(),
    val displaySettings: DisplaySettings = DisplaySettings(),
    val alertSettings: AlertSettings = AlertSettings()
)

fun skyLookSettingsFlow(context: Context): Flow<SkyLookSettings> =
    context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            SkyLookSettings(
                trackedIds = prefs[PrefsKeys.TRACKED_IDS] ?: emptySet(),
                displaySettings = DisplaySettings(
                    unitSystem      = runCatching { UnitSystem.valueOf(prefs[PrefsKeys.UNIT_SYSTEM] ?: "") }.getOrDefault(UnitSystem.METRIC),
                    labelMode       = runCatching { AircraftLabelMode.valueOf(prefs[PrefsKeys.LABEL_MODE] ?: "") }.getOrDefault(AircraftLabelMode.CALLSIGN),
                    themeMode       = runCatching { AppThemeMode.valueOf(prefs[PrefsKeys.THEME_MODE] ?: "") }.getOrDefault(AppThemeMode.DARK),
                    radarPlaneScale = runCatching { RadarPlaneScale.valueOf(prefs[PrefsKeys.RADAR_SCALE] ?: "") }.getOrDefault(RadarPlaneScale.NORMAL)
                ),
                alertSettings = AlertSettings(
                    overhead  = prefs[PrefsKeys.ALERT_OVERHEAD] ?: true,
                    rare      = prefs[PrefsKeys.ALERT_RARE]     ?: true,
                    traffic   = prefs[PrefsKeys.ALERT_TRAFFIC]  ?: false,
                    frequency = runCatching { AlertFrequency.valueOf(prefs[PrefsKeys.ALERT_FREQ] ?: "") }.getOrDefault(AlertFrequency.BALANCED)
                )
            )
        }

suspend fun saveTrackedIds(context: Context, ids: Set<String>) {
    context.dataStore.edit { it[PrefsKeys.TRACKED_IDS] = ids }
}

suspend fun saveDisplaySettings(context: Context, s: DisplaySettings) {
    context.dataStore.edit { prefs ->
        prefs[PrefsKeys.UNIT_SYSTEM] = s.unitSystem.name
        prefs[PrefsKeys.LABEL_MODE]  = s.labelMode.name
        prefs[PrefsKeys.THEME_MODE]  = s.themeMode.name
        prefs[PrefsKeys.RADAR_SCALE] = s.radarPlaneScale.name
    }
}

suspend fun saveAlertSettings(context: Context, s: AlertSettings) {
    context.dataStore.edit { prefs ->
        prefs[PrefsKeys.ALERT_OVERHEAD] = s.overhead
        prefs[PrefsKeys.ALERT_RARE]     = s.rare
        prefs[PrefsKeys.ALERT_TRAFFIC]  = s.traffic
        prefs[PrefsKeys.ALERT_FREQ]     = s.frequency.name
    }
}
