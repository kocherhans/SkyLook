package com.example.planespotter.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.planespotter.MainActivity
import com.example.planespotter.R
import com.example.planespotter.data.Flight

object PlaneNotifications {
    const val CHANNEL_ID = "flight_alerts"
    const val PERMISSION = Manifest.permission.POST_NOTIFICATIONS

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Flight alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts for aircraft passing nearby or overhead"
        }
        manager.createNotificationChannel(channel)
    }

    fun notifyFlight(context: Context, id: Int, title: String, message: String, flightId: String = "") {
        if (!hasPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (flightId.isNotEmpty()) putExtra("flight_id", flightId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission can be revoked between the check and post.
        }
    }
}

data class AlertSettings(
    val overhead: Boolean = true,
    val rare: Boolean = true,
    val traffic: Boolean = false,
    val frequency: AlertFrequency = AlertFrequency.BALANCED
)

enum class AlertFrequency(
    val label: String,
    val cooldownMs: Long,
    val overheadMinutes: Int,
    val trackedMinutes: Int,
    val oneShotMinutes: Int,
    val oneShotDistanceKm: Float,
    val trafficCount: Int
) {
    QUIET(
        label = "Quiet",
        cooldownMs = 60 * 60 * 1000L,
        overheadMinutes = 2,
        trackedMinutes = 5,
        oneShotMinutes = 6,
        oneShotDistanceKm = 3f,
        trafficCount = 14
    ),
    BALANCED(
        label = "Balanced",
        cooldownMs = 30 * 60 * 1000L,
        overheadMinutes = 3,
        trackedMinutes = 10,
        oneShotMinutes = 10,
        oneShotDistanceKm = 5f,
        trafficCount = 10
    ),
    FREQUENT(
        label = "Frequent",
        cooldownMs = 10 * 60 * 1000L,
        overheadMinutes = 5,
        trackedMinutes = 15,
        oneShotMinutes = 15,
        oneShotDistanceKm = 8f,
        trafficCount = 7
    )
}

fun Flight.alertDescription(): String =
    when {
        overheadMin in 1..998 -> "${callsign} overhead in $overheadMin min"
        distanceKm < 2f -> "${callsign} is almost overhead"
        else -> "${callsign} is %.1f km away".format(distanceKm)
    }
