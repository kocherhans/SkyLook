package com.example.planespotter.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.planespotter.data.*
import com.example.planespotter.ui.components.AirlineLogo
import com.example.planespotter.ui.theme.*

@Composable
fun FlightDetailSheet(
    flight: Flight,
    heading: Float,
    isTracked: Boolean,
    isNotifyEnabled: Boolean,
    displaySettings: DisplaySettings,
    onTrackToggle: () -> Unit,
    onNotifyToggle: () -> Unit,
    onMapOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val rel = relDeg(flight.bearing, heading)
    val color = Color(bearingColor(rel).toInt())
    val sky = LocalSkyPalette.current

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(sky.surface)
                .clickable(onClick = {}) // consume clicks
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .navigationBarsPadding()
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AirlineLogo(
                    iataCode = flight.airlineIataCode,
                    airlineName = flight.airline,
                    size = 52.dp,
                    modifier = Modifier.padding(end = 14.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        flight.primaryLabel(displaySettings.labelMode),
                        style = MaterialTheme.typography.headlineMedium,
                        color = sky.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        flight.secondaryLabel(displaySettings.labelMode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = sky.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = sky.primaryText)
                }
            }

            Spacer(Modifier.height(18.dp))

            // Route progress — only shown when origin/destination are known
            if (flight.fromCode.isNotEmpty() && flight.toCode.isNotEmpty()) {
                RouteCard(flight = flight, color = color)
                Spacer(Modifier.height(12.dp))
            }

            // Aircraft backstory — registration + age from hexdb.io
            if (flight.registration.isNotEmpty() || flight.yearBuilt > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = sky.accent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Text("AIRCRAFT", color = sky.secondaryText.copy(alpha = 0.75f),
                            fontWeight = FontWeight.W800, fontSize = 11.sp, letterSpacing = 1.6.sp)
                        Spacer(Modifier.height(10.dp))
                        if (flight.registration.isNotEmpty()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Registration", color = sky.secondaryText, fontSize = 13.sp)
                                Text(flight.registration, color = sky.primaryText, fontSize = 13.sp, fontWeight = FontWeight.W700)
                            }
                        }
                        if (flight.yearBuilt > 0) {
                            if (flight.registration.isNotEmpty()) Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Built", color = sky.secondaryText, fontSize = 13.sp)
                                Text("${flight.yearBuilt} · ${2026 - flight.yearBuilt} years old",
                                    color = sky.primaryText, fontSize = 13.sp, fontWeight = FontWeight.W700)
                            }
                        }
                    }
                }
            }

            // Stats grid
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("AIRCRAFT", flight.model.ifEmpty { "Unknown" }, Modifier.weight(1f))
                StatCard("ALTITUDE", formatAltitude(flight.altitudeFt, displaySettings.unitSystem), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("SPEED", formatSpeed(flight.speedMph, displaySettings.unitSystem), Modifier.weight(1f))
                StatCard("DISTANCE", formatDistance(flight.distanceKm, displaySettings.unitSystem), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("WHERE TO LOOK", humanDirection(flight.bearing, flight.altitudeFt, flight.distanceKm), Modifier.weight(1f))
                StatCard(
                    "NEAREST IN",
                    if (flight.overheadMin < 999) "${flight.overheadMin} min" else "Moving away",
                    Modifier.weight(1f),
                    highlighted = flight.overheadMin < 999
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile(
                    icon = if (isTracked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label = if (isTracked) "Tracking" else "Track",
                    onClick = onTrackToggle,
                    modifier = Modifier.weight(1f)
                )
                ActionTile(Icons.Default.LocationOn, "On map", onClick = onMapOpen, modifier = Modifier.weight(1f))
                ActionTile(
                    if (isNotifyEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                    if (isNotifyEnabled) "Armed" else "Notify",
                    onClick = onNotifyToggle,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RouteCard(flight: Flight, color: Color) {
    val sky = LocalSkyPalette.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = sky.accent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.widthIn(max = 112.dp)) {
                    Text("FROM", color = sky.secondaryText.copy(alpha = 0.75f), fontWeight = FontWeight.W800, fontSize = 11.sp, letterSpacing = 1.6.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(flight.fromCode, color = sky.primaryText, fontWeight = FontWeight.W800, fontSize = 26.sp, maxLines = 1)
                    Text(flight.fromCity, color = sky.secondaryText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.weight(1f).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(1.dp).background(sky.divider))
                    Text("✈", color = color, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    Box(Modifier.weight(1f).height(1.dp).background(sky.divider))
                }
                Column(Modifier.widthIn(max = 112.dp), horizontalAlignment = Alignment.End) {
                    Text("TO", color = sky.secondaryText.copy(alpha = 0.75f), fontWeight = FontWeight.W800, fontSize = 11.sp, letterSpacing = 1.6.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(flight.toCode, color = sky.primaryText, fontWeight = FontWeight.W800, fontSize = 26.sp, maxLines = 1)
                    Text(flight.toCity, color = sky.secondaryText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, highlighted: Boolean = false) {
    val sky = LocalSkyPalette.current
    Column(
        modifier = modifier
            .background(if (highlighted) sky.blue.copy(alpha = 0.10f) else sky.accent, RoundedCornerShape(16.dp))
            .heightIn(min = 88.dp)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(
            label,
            color = if (highlighted) sky.blue else sky.secondaryText.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontWeight = FontWeight.W800,
            letterSpacing = 1.1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            color = sky.primaryText,
            fontSize = 19.sp,
            fontWeight = FontWeight.W800,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun ActionTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sky = LocalSkyPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(sky.accent)
            .clickable(onClick = onClick)
            .height(82.dp)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = sky.primaryText, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = sky.primaryText, fontSize = 13.sp, fontWeight = FontWeight.W500, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
