package com.example.planespotter

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.planespotter.ui.phone.PlaneSpotterPhoneApp
import com.example.planespotter.ui.theme.PlaneSpotterTheme
import com.example.planespotter.ui.tv.TVAmbientScreen
import com.google.android.gms.cast.framework.CastContext

class MainActivity : ComponentActivity() {
    // Compose state so onNewIntent recomposition is automatic
    private var deepLinkFlightId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkFlightId = intent.getStringExtra("flight_id")
        enableEdgeToEdge()
        // Pre-warm CastContext so it's ready before the user taps cast
        try { CastContext.getSharedInstance(this) } catch (_: Exception) { }

        val isTV = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        setContent {
            PlaneSpotterTheme {
                if (isTV) {
                    TVAmbientScreen(modifier = Modifier.fillMaxSize())
                } else {
                    PlaneSpotterPhoneApp(
                        modifier = Modifier.fillMaxSize(),
                        deepLinkFlightId = deepLinkFlightId
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkFlightId = intent.getStringExtra("flight_id")
    }
}
