package com.example.planespotter.ui.tv

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.planespotter.data.*
import com.example.planespotter.ui.components.AirlineLogo
import com.example.planespotter.ui.components.CompassView
import com.example.planespotter.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TVAmbientScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // ── Location permission ───────────────────────────────────────
    var hasLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocation = granted }

    LaunchedEffect(Unit) {
        if (!hasLocation) permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (!hasLocation) {
        TVPermissionScreen(onRequest = { permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) })
        return
    }

    // TV has no compass sensor — show North-up fixed orientation
    val heading = 0f
    var selectedId by remember { mutableStateOf<String?>(null) }
    var trackedIds by remember { mutableStateOf(setOf<String>()) }    // fix 1: was setOf("BA123")
    var keyboardFocusId by remember { mutableStateOf<String?>(null) } // fix 8: ID-based, not index
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var displaySettings by remember { mutableStateOf(DisplaySettings()) }
    val routeScope = rememberCoroutineScope()

    // ── Persistence ───────────────────────────────────────────────
    var settingsLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        skyLookSettingsFlow(context).first().let { s ->
            trackedIds      = s.trackedIds
            displaySettings = s.displaySettings
            settingsLoaded  = true
        }
    }
    LaunchedEffect(trackedIds)      { if (settingsLoaded) saveTrackedIds(context, trackedIds) }
    LaunchedEffect(displaySettings) { if (settingsLoaded) saveDisplaySettings(context, displaySettings) }

    // ── Live flight data ──────────────────────────────────────────
    var flights by remember { mutableStateOf(MOCK_FLIGHTS) }
    var lastUpdated by remember { mutableStateOf<Long?>(null) }
    val routeCache = remember { mutableStateMapOf<String, RouteInfo?>() }
    val fetchedCallsigns = remember { mutableSetOf<String>() }

    val userLocation = rememberUserLocation()
    val latestLocation by rememberUpdatedState(userLocation)

    LaunchedEffect(Unit) {
        var pollMs = 15_000L
        while (true) {
            val loc = latestLocation
            if (loc != null) {
                val result = fetchNearbyFlights(loc, 75.0)
                result.fold(
                    onSuccess = { list ->
                        if (list.isNotEmpty()) { flights = list; lastUpdated = System.currentTimeMillis() }
                        pollMs = 15_000L
                    },
                    onFailure = { pollMs = (pollMs * 2).coerceAtMost(120_000L) }
                )
                delay(pollMs)
            } else {
                delay(1_000)
            }
        }
    }

    // Background-fetch route info for new callsigns
    LaunchedEffect(flights) {
        // fix 8: initialize keyboard cursor to first non-tracked flight if unset or stale
        if (keyboardFocusId == null || flights.none { it.id == keyboardFocusId }) {
            keyboardFocusId = flights.filter { !trackedIds.contains(it.id) }.firstOrNull()?.id
        }

        flights.forEach { flight ->
            if (flight.callsign !in fetchedCallsigns) {
                fetchedCallsigns.add(flight.callsign)
                routeScope.launch { routeCache[flight.callsign] = fetchRouteInfo(flight.callsign) }
            }
        }
    }

    val enrichedFlights by remember { derivedStateOf { flights.enrich(routeCache) } }
    val nonTrackedFlights by remember { derivedStateOf { enrichedFlights.filter { !trackedIds.contains(it.id) } } }
    val selectedFlight = enrichedFlights.find { it.id == selectedId }
    val focusRequester = remember { FocusRequester() }

    // ── Staleness ticker (fix 5) ──────────────────────────────────
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(60_000); nowMs = System.currentTimeMillis() }
    }

    // ── Ambient idle: auto-dismiss detail panel after 30 s ────────
    LaunchedEffect(lastInteractionMs, selectedId) {
        if (selectedId != null) {
            delay(30_000)
            selectedId = null
        }
    }

    // ── Sound cue: ping when a tracked flight enters 25 km ────────
    val pingedIds = remember { mutableSetOf<String>() }
    LaunchedEffect(enrichedFlights) {
        enrichedFlights
            .filter { it.id in trackedIds && it.distanceKm <= 25f && it.id !in pingedIds }
            .forEach { flight ->
                pingedIds.add(flight.id)
                try {
                    val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 35)
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    delay(500)
                    tg.release()
                } catch (_: Exception) {}
            }
        pingedIds.retainAll(enrichedFlights.filter { it.distanceKm <= 25f }.map { it.id }.toSet())
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = modifier
            .background(SkyBackground)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionUp -> {
                            val idx = nonTrackedFlights.indexOfFirst { it.id == keyboardFocusId }
                            keyboardFocusId = nonTrackedFlights.getOrNull(
                                (idx - 1 + nonTrackedFlights.size) % nonTrackedFlights.size.coerceAtLeast(1)
                            )?.id
                            lastInteractionMs = System.currentTimeMillis()
                            true
                        }
                        Key.DirectionDown -> {
                            val idx = nonTrackedFlights.indexOfFirst { it.id == keyboardFocusId }
                            keyboardFocusId = nonTrackedFlights.getOrNull(
                                (idx + 1) % nonTrackedFlights.size.coerceAtLeast(1)
                            )?.id
                            lastInteractionMs = System.currentTimeMillis()
                            true
                        }
                        Key.Enter, Key.DirectionCenter -> {
                            selectedId = if (selectedId == keyboardFocusId) null else keyboardFocusId
                            lastInteractionMs = System.currentTimeMillis()
                            true
                        }
                        Key.Escape, Key.Back -> {
                            selectedId = null
                            lastInteractionMs = System.currentTimeMillis()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
    ) {
        Row(Modifier.fillMaxSize()) {
            // ── Left panel ─────────────────────────────────────────────
            Column(
                Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ClockWidget()

                // Stats row
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TVStatPill("${enrichedFlights.size}", "aircraft", SkyBlue)
                    TVStatPill("${trackedIds.size}", "tracked", SkyPurple)
                }

                // fix 4: demo badge when showing mock data; fix 5: live staleness ticker
                if (lastUpdated == null) {
                    Text(
                        "DEMO DATA",
                        color = SkyPurple.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.W700,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier
                            .background(SkyPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                } else {
                    val ageMin = ((nowMs - lastUpdated!!) / 60_000).toInt()
                    val label = if (ageMin <= 1) "Live" else "${ageMin}m ago"
                    Text(
                        label,
                        color = if (ageMin <= 1) SkyBlue.copy(alpha = 0.5f) else SkyRed.copy(alpha = 0.6f),
                        fontSize = 10.sp, fontWeight = FontWeight.W600, letterSpacing = 0.5.sp
                    )
                }

                HorizontalDivider(color = SkyDivider)

                if (trackedIds.isNotEmpty()) {
                    Text("TRACKED", color = SkyBlue.copy(alpha = 0.7f),
                        fontSize = 10.sp, fontWeight = FontWeight.W700, letterSpacing = 0.8.sp)
                    enrichedFlights.filter { trackedIds.contains(it.id) }.forEach { flight ->
                        TVFlightRow(
                            flight = flight,
                            heading = heading,
                            isSelected = flight.id == selectedId,
                            isFocused = false,
                            onClick = {
                                selectedId = if (selectedId == flight.id) null else flight.id
                                lastInteractionMs = System.currentTimeMillis()
                            }
                        )
                    }
                    HorizontalDivider(color = SkyDivider)
                }

                Text("NEARBY", color = Color.White.copy(alpha = 0.3f),
                    fontSize = 10.sp, fontWeight = FontWeight.W700, letterSpacing = 0.8.sp)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(nonTrackedFlights) { _, flight ->
                        TVFlightRow(
                            flight = flight,
                            heading = heading,
                            isSelected = flight.id == selectedId,
                            isFocused = flight.id == keyboardFocusId, // fix 8: compare by ID
                            onClick = {
                                selectedId = if (selectedId == flight.id) null else flight.id
                                lastInteractionMs = System.currentTimeMillis()
                            }
                        )
                    }
                }
            }

            // ── Compass center ─────────────────────────────────────────
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CompassView(
                    flights = enrichedFlights,
                    heading = heading,
                    radiusKm = 50f,
                    focusedId = selectedId,
                    trackedIds = trackedIds,
                    onFlightTap = { id ->
                        selectedId = if (selectedId == id) null else id
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ── Right panel ────────────────────────────────────────────
            Column(
                Modifier
                    .width(340.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(24.dp)
            ) {
                AnimatedVisibility(
                    visible = selectedFlight != null,
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    selectedFlight?.let { flight ->
                        TVDetailPanel(
                            flight = flight,
                            heading = heading,
                            displaySettings = displaySettings,  // fix 10
                            isTracked = trackedIds.contains(flight.id),
                            onTrackToggle = {
                                trackedIds = if (trackedIds.contains(flight.id))
                                    trackedIds - flight.id
                                else
                                    trackedIds + flight.id
                            }
                        )
                    }
                }
                if (selectedFlight == null) {
                    TVDirectionGuide()
                }
            }
        }
    }
}

@Composable
private fun TVPermissionScreen(onRequest: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Box(Modifier.fillMaxSize().background(SkyBackground), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(60.dp)
        ) {
            Text("✈", fontSize = 80.sp)
            Text("SkyLook", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.W200, letterSpacing = (-1).sp)
            Text(
                "Location access is needed to find aircraft nearby.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                modifier = Modifier.focusRequester(focusRequester)
            ) {
                Text("Allow Location", fontSize = 16.sp, fontWeight = FontWeight.W600)
            }
        }
    }
}

@Composable
private fun ClockWidget() {
    var timeStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            timeStr = fmt.format(Date())
            delay(10_000)
        }
    }
    Text(timeStr, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.W200, letterSpacing = (-1).sp)
}

@Composable
private fun TVStatPill(value: String, label: String, color: Color) {
    Column(
        Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.W700)
        Text(label, color = color.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

@Composable
private fun TVFlightRow(
    flight: Flight,
    heading: Float,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val rel = relDeg(flight.bearing, heading)
    val color = Color(bearingColor(rel))
    val bg = when {
        isSelected -> color.copy(alpha = 0.2f)
        isFocused  -> Color.White.copy(alpha = 0.08f)
        else       -> Color.Transparent
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(flight.callsign, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.W600)
            Text("${flight.fromCode}→${flight.toCode}",
                color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
        }
        Text("${flight.distanceKm.toInt()}km",
            color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
    }
}

@Composable
private fun TVDetailPanel(
    flight: Flight,
    heading: Float,
    displaySettings: DisplaySettings,  // fix 10
    isTracked: Boolean,
    onTrackToggle: () -> Unit
) {
    val rel = relDeg(flight.bearing, heading)
    val color = Color(bearingColor(rel))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            AirlineLogo(
                iataCode = flight.airlineIataCode,
                airlineName = flight.airline,
                size = 48.dp,
                cornerRadius = 10.dp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(flight.callsign, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.W700)
                Text(flight.airline, color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
            }
            Box(
                Modifier
                    .size(36.dp)
                    .background(
                        if (isTracked) SkyBlue.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onTrackToggle),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isTracked) "♥" else "♡",
                    color = if (isTracked) SkyBlue else Color.White.copy(alpha = 0.5f), fontSize = 18.sp)
            }
        }

        // Where to look
        Column(
            Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text("WHERE TO LOOK", color = color, fontSize = 10.sp, fontWeight = FontWeight.W700, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(4.dp))
            Text(humanDirection(flight.bearing, flight.altitudeFt),
                color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.W600)
        }

        // Route card
        if (flight.fromCode.isNotEmpty() && flight.toCode.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(SkyAccent, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(flight.fromCode, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.W700)
                    Text(flight.fromCity, color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(flight.toCode, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.W700)
                    Text(flight.toCity, color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                }
            }
            if (flight.progress > 0f) {
                Box(
                    Modifier.fillMaxWidth().height(3.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        Modifier.fillMaxWidth(flight.progress).height(3.dp)
                            .background(color, RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // Stats — fix 10: use display settings for units
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TVStatRow("Altitude", formatAltitude(flight.altitudeFt, displaySettings.unitSystem))
            TVStatRow("Speed",    formatSpeed(flight.speedMph, displaySettings.unitSystem))
            TVStatRow("Distance", formatDistance(flight.distanceKm, displaySettings.unitSystem))
            TVStatRow("Aircraft", flight.model)
            TVStatRow("Status",   flight.status)
        }
    }
}

@Composable
private fun TVStatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.W500)
    }
}

@Composable
private fun TVDirectionGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("COLOUR GUIDE", color = Color.White.copy(alpha = 0.3f),
            fontSize = 10.sp, fontWeight = FontWeight.W700, letterSpacing = 0.8.sp)
        TVColorRow(SkyBlue, "North (ahead)")
        TVColorRow(SkyPurple, "East / West (sides)")
        TVColorRow(SkyRed, "South (behind)")
        Spacer(Modifier.height(8.dp))
        Text("Select any aircraft to see details",
            color = Color.White.copy(alpha = 0.25f), fontSize = 12.sp)
        Text("↑↓ Navigate  ·  OK Select  ·  Back Dismiss",
            color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp)
    }
}

@Composable
private fun TVColorRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
    }
}
