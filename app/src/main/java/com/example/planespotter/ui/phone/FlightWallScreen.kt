package com.example.planespotter.ui.phone

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.planespotter.data.*
import com.example.planespotter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FlightWallScreen(
    flights: List<Flight>,
    heading: Float,
    trackedIds: Set<String>,
    lastUpdated: Long?,
    isLoading: Boolean,
    locationError: String?,
    displaySettings: DisplaySettings,
    onFlightTap: (String) -> Unit,
    onTrackToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sky = LocalSkyPalette.current
    Column(modifier.background(sky.background)) {
        // Header
        WallHeader(count = flights.size, lastUpdated = lastUpdated, isLoading = isLoading)
        HorizontalDivider(color = sky.divider, thickness = 0.5.dp)

        // Column headers
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("FLIGHT", color = sky.secondaryText.copy(alpha = 0.75f), fontSize = 9.sp,
                fontWeight = FontWeight.W700, letterSpacing = 1.sp, modifier = Modifier.width(96.dp))
            Text("ALT", color = sky.secondaryText.copy(alpha = 0.75f), fontSize = 9.sp,
                fontWeight = FontWeight.W700, letterSpacing = 1.sp, modifier = Modifier.width(68.dp))
            Text("SPD", color = sky.secondaryText.copy(alpha = 0.75f), fontSize = 9.sp,
                fontWeight = FontWeight.W700, letterSpacing = 1.sp, modifier = Modifier.width(62.dp))
            Text("DIST", color = sky.secondaryText.copy(alpha = 0.75f), fontSize = 9.sp,
                fontWeight = FontWeight.W700, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            Text("DIR", color = sky.secondaryText.copy(alpha = 0.75f), fontSize = 9.sp,
                fontWeight = FontWeight.W700, letterSpacing = 1.sp,
                modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
        HorizontalDivider(color = sky.divider, thickness = 0.5.dp)

        if (locationError != null) {
            ErrorBanner(locationError)
        }

        if (flights.isEmpty() && !isLoading) {
            EmptyState()
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                itemsIndexed(flights, key = { _, f -> f.id }) { index, flight ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    ) {
                        WallRow(
                            flight = flight,
                            heading = heading,
                            index = index,
                            isTracked = trackedIds.contains(flight.id),
                            displaySettings = displaySettings,
                            onTap = { onFlightTap(flight.id) },
                            onTrackToggle = { onTrackToggle(flight.id) }
                        )
                    }
                    if (index < flights.lastIndex) {
                        HorizontalDivider(
                            color = sky.divider,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WallHeader(count: Int, lastUpdated: Long?, isLoading: Boolean) {
    val sky = LocalSkyPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("FLIGHT WALL", color = sky.primaryText,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W700, fontSize = 16.sp,
                letterSpacing = 2.sp)
            Text("$count aircraft overhead", color = sky.secondaryText, fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = SkyBlue, strokeWidth = 2.dp
                )
            }
            if (lastUpdated != null) {
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastUpdated))
                Box(
                    Modifier
                        .border(0.5.dp, SkyBlue.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(time, color = SkyBlue, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                Box(
                    Modifier
                        .background(SkyBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("LIVE", color = SkyBlue, fontSize = 10.sp,
                        fontWeight = FontWeight.W700, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
private fun WallRow(
    flight: Flight,
    heading: Float,
    index: Int,
    isTracked: Boolean,
    displaySettings: DisplaySettings,
    onTap: () -> Unit,
    onTrackToggle: () -> Unit
) {
    val sky = LocalSkyPalette.current
    val rel   = relDeg(flight.bearing, heading)
    val color = Color(bearingColor(rel))

    // Fade distant aircraft
    val alpha = when {
        flight.distanceKm < 10f  -> 1.0f
        flight.distanceKm < 25f  -> 0.85f
        flight.distanceKm < 50f  -> 0.65f
        else                     -> 0.45f
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Flight identity ──────────────────────────────────────
        Column(Modifier.width(96.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (flight.interesting) {
                    Text("★", color = SkyPurple, fontSize = 10.sp)
                }
                Text(
                    flight.primaryLabel(displaySettings.labelMode),
                    color = sky.primaryText.copy(alpha = alpha),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.W700,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Text(
                flight.secondaryLabel(displaySettings.labelMode),
                color = sky.secondaryText.copy(alpha = alpha),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ── Altitude ──────────────────────────────────────────────
        Column(Modifier.width(68.dp)) {
            Text(
                formatAltitudeValue(flight.altitudeFt, displaySettings.unitSystem),
                color = altitudeColor(flight.altitudeFt).copy(alpha = alpha),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.W600,
                fontSize = 13.sp
            )
            Text(altitudeUnit(displaySettings.unitSystem), color = sky.secondaryText.copy(alpha = alpha), fontSize = 9.sp)
        }

        // ── Speed ─────────────────────────────────────────────────
        Column(Modifier.width(62.dp)) {
            Text(
                formatSpeedValue(flight.speedMph, displaySettings.unitSystem),
                color = sky.primaryText.copy(alpha = alpha),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.W500,
                fontSize = 12.sp,
                maxLines = 1
            )
            Text(speedUnit(displaySettings.unitSystem), color = sky.secondaryText.copy(alpha = alpha), fontSize = 8.sp, maxLines = 1)
        }

        // ── Distance ──────────────────────────────────────────────
        Column(Modifier.weight(1f)) {
            Text(
                formatDistance(flight.distanceKm, displaySettings.unitSystem),
                color = sky.primaryText.copy(alpha = alpha),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(flight.status, color = statusColor(flight.status, sky).copy(alpha = alpha * 0.8f), fontSize = 9.sp)
        }

        // ── Direction arrow ────────────────────────────────────────
        Row(
            Modifier.width(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                bearingArrow(rel),
                color = color.copy(alpha = alpha),
                fontSize = 18.sp
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onTrackToggle, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (isTracked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Track aircraft",
                    tint = if (isTracked) sky.blue else sky.secondaryText.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val sky = LocalSkyPalette.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("✈", fontSize = 48.sp, color = sky.secondaryText.copy(alpha = 0.5f))
            Text("No aircraft detected", color = sky.secondaryText, fontSize = 14.sp)
            Text("Try increasing the radius in Filter", color = sky.secondaryText.copy(alpha = 0.75f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(SkyRed.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("⚠", color = SkyRed, fontSize = 14.sp)
        Text(message, color = SkyRed.copy(alpha = 0.9f), fontSize = 12.sp)
    }
}

private fun bearingArrow(relDeg: Float): String {
    val norm = ((relDeg + 180) % 360).toInt()
    return when ((norm / 45) % 8) {
        0 -> "↑"; 1 -> "↗"; 2 -> "→"; 3 -> "↘"
        4 -> "↓"; 5 -> "↙"; 6 -> "←"; 7 -> "↖"
        else -> "·"
    }
}

private fun altitudeColor(altFt: Int): Color = when {
    altFt > 38000 -> SkyPurple
    altFt > 25000 -> SkyBlue
    altFt > 12000 -> Color(0xFF7ED8A4)
    else          -> Color(0xFFFFCC66)
}

private fun statusColor(status: String, sky: SkyPalette): Color = when (status) {
    "Descending" -> SkyRed
    "Climbing"   -> Color(0xFF7ED8A4)
    else         -> sky.primaryText
}
