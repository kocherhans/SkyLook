package com.example.planespotter.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.planespotter.data.AircraftLabelMode
import com.example.planespotter.data.AppThemeMode
import com.example.planespotter.data.DisplaySettings
import com.example.planespotter.data.HeadingState
import com.example.planespotter.data.RadarPlaneScale
import com.example.planespotter.data.UnitSystem
import com.example.planespotter.notifications.AlertFrequency
import com.example.planespotter.notifications.AlertSettings
import com.example.planespotter.ui.theme.*

@Composable
fun CompassSheet(
    headingState: HeadingState,
    forceManual: Boolean,
    manualHeading: Float,
    alertSettings: AlertSettings,
    displaySettings: DisplaySettings,
    activeFlightAlerts: Map<String, String>,
    notificationsEnabled: Boolean,
    onForceManualChange: (Boolean) -> Unit,
    onManualHeadingChange: (Float) -> Unit,
    onAlertSettingsChange: (AlertSettings) -> Unit,
    onDisplaySettingsChange: (DisplaySettings) -> Unit,
    onFlightAlertCancel: (String) -> Unit,
    onRequestNotifications: () -> Unit,
    onClose: () -> Unit
) {
    val sky = LocalSkyPalette.current
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(sky.surface)
                .clickable(onClick = {})
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextBlock("Settings", MaterialTheme.typography.titleLarge.fontSize, FontWeight.W700, sky.primaryText)
                IconButton(onClick = onClose, modifier = Modifier.background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = sky.primaryText)
                }
            }

            SettingsSection("ORIENTATION") {
                SettingsRow("Heading source", if (forceManual) "Manual" else "Compass + Gyro") {
                    onForceManualChange(!forceManual)
                }
                SettingsDivider()
                SettingsRow("Recalibrate", "Figure-8 motion") {
                    onManualHeadingChange(manualHeading)
                }
            }

            SettingsSection("ALERTS") {
                if (!notificationsEnabled) {
                    SettingsRow("Notifications", "Allow") {
                        onRequestNotifications()
                    }
                    SettingsDivider()
                }
                SettingsRow("Alert frequency", alertSettings.frequency.label) {
                    val next = when (alertSettings.frequency) {
                        AlertFrequency.QUIET -> AlertFrequency.BALANCED
                        AlertFrequency.BALANCED -> AlertFrequency.FREQUENT
                        AlertFrequency.FREQUENT -> AlertFrequency.QUIET
                    }
                    onAlertSettingsChange(alertSettings.copy(frequency = next))
                }
                SettingsDivider()
                SettingsSwitchRow("Aircraft directly overhead", alertSettings.overhead) {
                    onAlertSettingsChange(alertSettings.copy(overhead = it))
                }
                SettingsDivider()
                SettingsSwitchRow("Rare aircraft nearby", alertSettings.rare) {
                    onAlertSettingsChange(alertSettings.copy(rare = it))
                }
                SettingsDivider()
                SettingsSwitchRow("Heavy traffic in your area", alertSettings.traffic) {
                    onAlertSettingsChange(alertSettings.copy(traffic = it))
                }
            }

            if (activeFlightAlerts.isNotEmpty()) {
                SettingsSection("FLIGHT ALERTS") {
                    activeFlightAlerts.toList().forEachIndexed { index, (id, label) ->
                        AlertFlightRow(label = label, onCancel = { onFlightAlertCancel(id) })
                        if (index < activeFlightAlerts.size - 1) SettingsDivider()
                    }
                }
            }

            SettingsSection("DISPLAY") {
                SettingsRow("Units", if (displaySettings.unitSystem == UnitSystem.METRIC) "Metric (km, m)" else "Imperial (mi, ft)") {
                    onDisplaySettingsChange(
                        displaySettings.copy(
                            unitSystem = if (displaySettings.unitSystem == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC
                        )
                    )
                }
                SettingsDivider()
                SettingsRow("Aircraft labels", if (displaySettings.labelMode == AircraftLabelMode.CALLSIGN) "Callsign" else "Airline") {
                    onDisplaySettingsChange(
                        displaySettings.copy(
                            labelMode = if (displaySettings.labelMode == AircraftLabelMode.CALLSIGN) AircraftLabelMode.AIRLINE else AircraftLabelMode.CALLSIGN
                        )
                    )
                }
                SettingsDivider()
                SettingsRow("Plane size", displaySettings.radarPlaneScale.label) {
                    val nextScale = when (displaySettings.radarPlaneScale) {
                        RadarPlaneScale.SMALL -> RadarPlaneScale.NORMAL
                        RadarPlaneScale.NORMAL -> RadarPlaneScale.LARGE
                        RadarPlaneScale.LARGE -> RadarPlaneScale.SMALL
                    }
                    onDisplaySettingsChange(displaySettings.copy(radarPlaneScale = nextScale))
                }
                SettingsDivider()
                SettingsRow("Theme", displaySettings.themeMode.label) {
                    onDisplaySettingsChange(
                        displaySettings.copy(
                            themeMode = if (displaySettings.themeMode == AppThemeMode.DARK) AppThemeMode.LIGHT else AppThemeMode.DARK
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AlertFlightRow(label: String, onCancel: () -> Unit) {
    val sky = LocalSkyPalette.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            androidx.compose.material3.Text(
                label,
                color = sky.primaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            androidx.compose.material3.Text(
                "One-time notification armed",
                color = sky.secondaryText,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        TextButton(onClick = onCancel) {
            androidx.compose.material3.Text("Cancel", color = sky.red, fontWeight = FontWeight.W700)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val sky = LocalSkyPalette.current
    Spacer(Modifier.height(18.dp))
    TextBlock(title, 12.sp, FontWeight.W800, sky.secondaryText.copy(alpha = 0.75f), letterSpacing = 1.8.sp)
    Spacer(Modifier.height(10.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(sky.accent)
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(label: String, value: String, onClick: () -> Unit) {
    val sky = LocalSkyPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Text(
            label,
            color = sky.primaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            androidx.compose.material3.Text(
                value,
                color = sky.secondaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.W500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = sky.secondaryText, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val sky = LocalSkyPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Text(
            label,
            color = sky.primaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = sky.blue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = sky.secondaryText.copy(alpha = 0.22f)
            )
        )
    }
}

@Composable
private fun SettingsDivider() {
    val sky = LocalSkyPalette.current
    Box(Modifier.fillMaxWidth().height(1.dp).background(sky.divider))
}

@Composable
private fun TextBlock(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    color: Color,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp
) {
    androidx.compose.material3.Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing
    )
}
