package com.example.planespotter.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

data class UserLocation(val lat: Double, val lon: Double)

@SuppressLint("MissingPermission")
@Composable
fun rememberUserLocation(intervalMs: Long = 60_000L): UserLocation? {
    val context = LocalContext.current
    var location by remember { mutableStateOf<UserLocation?>(null) }

    // Re-key on intervalMs so the listener re-registers at the new rate when the
    // caller adjusts based on nearby aircraft (fast when planes are close, slow when empty).
    DisposableEffect(intervalMs) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val listener = LocationListener { loc ->
            location = UserLocation(loc.latitude, loc.longitude)
        }

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        try {
            for (provider in providers) {
                if (lm.isProviderEnabled(provider)) {
                    lm.getLastKnownLocation(provider)?.let { loc ->
                        if (location == null) location = UserLocation(loc.latitude, loc.longitude)
                    }
                    lm.requestLocationUpdates(provider, intervalMs, 50f, listener)
                }
            }
        } catch (_: SecurityException) {}

        onDispose { try { lm.removeUpdates(listener) } catch (_: Exception) {} }
    }

    return location
}
