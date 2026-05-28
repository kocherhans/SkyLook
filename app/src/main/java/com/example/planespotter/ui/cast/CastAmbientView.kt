package com.example.planespotter.ui.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.planespotter.data.*
import com.example.planespotter.ui.components.AirlineLogo
import com.example.planespotter.ui.components.CompassView
import com.example.planespotter.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class CastState(
    val flights: List<Flight> = emptyList(),
    val trackedIds: Set<String> = emptySet(),
    val focusedFlight: Flight? = null,
    val displaySettings: DisplaySettings = DisplaySettings(),
    val lastUpdated: Long? = null
)

@Composable
fun CastAmbientView(state: CastState, modifier: Modifier = Modifier) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); nowMs = System.currentTimeMillis() } }

    val trackedFlights    = remember(state.flights, state.trackedIds) { state.flights.filter { it.id in state.trackedIds } }
    val nonTrackedFlights = remember(state.flights, state.trackedIds) { state.flights.filter { it.id !in state.trackedIds } }

    Box(modifier.background(SkyBackground)) {
        Row(Modifier.fillMaxSize()) {

            // ── Left panel ─────────────────────────────────────────────
            Column(
                Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                CastClockWidget()

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CastStatPill("${state.flights.size}", "aircraft", SkyBlue)
                    if (state.trackedIds.isNotEmpty()) {
                        CastStatPill("${trackedFlights.size}", "tracked", SkyPurple)
                    }
                }

                if (state.lastUpdated == null) {
                    Text(
                        "DEMO DATA",
                        color = SkyPurple.copy(alpha = 0.8f),
                        fontSize = 9.sp, fontWeight = FontWeight.W700, letterSpacing = 0.8.sp,
                        modifier = Modifier
                            .background(SkyPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                } else {
                    val ageMin = ((nowMs - state.lastUpdated) / 60_000).toInt()
                    Text(
                        if (ageMin <= 1) "Live" else "${ageMin}m ago",
                        color = if (ageMin <= 1) SkyBlue.copy(alpha = 0.6f) else SkyRed.copy(alpha = 0.6f),
                        fontSize = 10.sp, fontWeight = FontWeight.W600, letterSpacing = 0.5.sp
                    )
                }

                HorizontalDivider(color = SkyDivider)

                if (trackedFlights.isNotEmpty()) {
                    Text("TRACKED", color = SkyBlue.copy(alpha = 0.7f),
                        fontSize = 10.sp, fontWeight = FontWeight.W700, letterSpacing = 0.8.sp)
                    trackedFlights.forEach { CastFlightRow(flight = it, dimmed = false) }
                    HorizontalDivider(color = SkyDivider)
                }

                Text("NEARBY", color = Color.White.copy(alpha = 0.3f),
                    fontSize = 10.sp, fontWeight = FontWeight.W700, letterSpacing = 0.8.sp)

                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(nonTrackedFlights) { flight ->
                        CastFlightRow(
                            flight = flight,
                            dimmed = state.focusedFlight != null && flight.id != state.focusedFlight.id
                        )
                    }
                }
            }

            // ── Centre compass ─────────────────────────────────────────
            Box(
                Modifier.weight(1f).fillMaxHeight().padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CompassView(
                    flights = state.flights,
                    heading = 0f,   // north-up — the external display has no orientation
                    radiusKm = 75f,
                    focusedId = state.focusedFlight?.id,
                    trackedIds = state.trackedIds,
                    unitSystem = state.displaySettings.unitSystem,
                    labelMode = state.displaySettings.labelMode,
                    onFlightTap = {},   // read-only
                    modifier = Modifier.fillMaxSize()
                )
                // Subtle watermark so viewers know what app this is
                Text(
                    "SkyLook",
                    color = Color.White.copy(alpha = 0.07f),
                    fontSize = 12.sp, fontWeight = FontWeight.W300, letterSpacing = 3.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                )
            }

            // ── Right panel ────────────────────────────────────────────
            Column(
                Modifier
                    .width(350.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(28.dp)
            ) {
                val focused = state.focusedFlight
                if (focused != null) {
                    CastDetailPanel(flight = focused, displaySettings = state.displaySettings)
                } else {
                    CastIdlePanel()
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────

@Composable
private fun CastClockWidget() {
    var timeStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) { timeStr = fmt.format(Date()); delay(10_000) }
    }
    Text(timeStr, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.W200, letterSpacing = (-1).sp)
}

@Composable
private fun CastStatPill(value: String, label: String, color: Color) {
    Column(
        Modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.W700)
        Text(label, color = color.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

@Composable
private fun CastFlightRow(flight: Flight, dimmed: Boolean) {
    val rel = relDeg(flight.bearing, 0f)
    val color = Color(bearingColor(rel))
    val alpha = if (dimmed) 0.35f else 1f
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(6.dp).background(color.copy(alpha = alpha), CircleShape))
        Column(Modifier.weight(1f)) {
            Text(flight.callsign, color = Color.White.copy(alpha = alpha), fontSize = 13.sp, fontWeight = FontWeight.W600)
            if (flight.fromCode.isNotEmpty()) {
                Text("${flight.fromCode} → ${flight.toCode}", color = Color.White.copy(alpha = 0.35f * alpha), fontSize = 10.sp)
            }
        }
        Text("${flight.distanceKm.toInt()}km", color = Color.White.copy(alpha = 0.4f * alpha), fontSize = 10.sp)
    }
}

@Composable
private fun CastDetailPanel(flight: Flight, displaySettings: DisplaySettings) {
    val rel = relDeg(flight.bearing, 0f)
    val color = Color(bearingColor(rel))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AirlineLogo(iataCode = flight.airlineIataCode, airlineName = flight.airline,
                size = 52.dp, cornerRadius = 12.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(flight.callsign, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.W700)
                    if (flight.interesting) {
                        Text("★", color = SkyPurple, fontSize = 11.sp,
                            modifier = Modifier.background(SkyPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
                Text(flight.airline, color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
            }
        }

        // Where to look
        Column(
            Modifier.fillMaxWidth()
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text("WHERE TO LOOK", color = color, fontSize = 9.sp, fontWeight = FontWeight.W800, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(humanDirection(flight.bearing, flight.altitudeFt),
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.W600)
        }

        // Route card + progress bar
        if (flight.fromCode.isNotEmpty() && flight.toCode.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth()
                    .background(SkyAccent, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(flight.fromCode, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.W700)
                        Text(flight.fromCity, color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                    }
                    Text("✈", color = color.copy(alpha = 0.7f), fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.CenterVertically))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(flight.toCode, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.W700)
                        Text(flight.toCity, color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                    }
                }
                if (flight.progress > 0f) {
                    Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))) {
                        Box(Modifier.fillMaxWidth(flight.progress).height(3.dp).background(color, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }

        // Stats grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CastStatRow("Altitude", formatAltitude(flight.altitudeFt, displaySettings.unitSystem))
            CastStatRow("Speed",    formatSpeed(flight.speedMph, displaySettings.unitSystem))
            CastStatRow("Distance", formatDistance(flight.distanceKm, displaySettings.unitSystem))
            if (flight.model.isNotEmpty()) CastStatRow("Aircraft", flight.model)
            CastStatRow("Status",   flight.status)
            if (flight.overheadMin < 999) CastStatRow("Nearest in", "${flight.overheadMin} min")
        }
    }
}

@Composable
private fun CastIdlePanel() {
    Column(
        Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✈", fontSize = 52.sp)
        Spacer(Modifier.height(20.dp))
        Text("SkyLook",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 30.sp, fontWeight = FontWeight.W200, letterSpacing = 2.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Tap any aircraft on your phone\nto see it here.",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun CastStatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.W500)
    }
}
