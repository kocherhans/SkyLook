package com.example.planespotter.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.planespotter.data.*
import com.example.planespotter.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TrackFlightScreen(
    flights: List<Flight>,
    userLocation: UserLocation?,
    trackedIds: Set<String>,
    onToggle: (String) -> Unit,
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    val sky = LocalSkyPalette.current
    var query by remember { mutableStateOf("") }
    var globalResults by remember { mutableStateOf(listOf<Flight>()) }
    var isSearching by remember { mutableStateOf(false) }

    // Debounced global callsign search
    LaunchedEffect(query) {
        if (query.length < 3) { globalResults = emptyList(); return@LaunchedEffect }
        delay(400)
        isSearching = true
        searchFlightGlobal(query, userLocation).onSuccess { results ->
            // Only show flights not already in the local list
            val localIds = flights.map { it.id }.toSet()
            globalResults = results.filter { it.id !in localIds }
        }
        isSearching = false
    }

    val localFiltered = if (query.isBlank()) flights
    else flights.filter {
        it.callsign.contains(query, ignoreCase = true) ||
        it.airline.contains(query, ignoreCase = true) ||
        it.fromCode.contains(query, ignoreCase = true) ||
        it.toCode.contains(query, ignoreCase = true)
    }

    val tracked = localFiltered.filter { trackedIds.contains(it.id) }
    val special = localFiltered.filter { it.interesting && !trackedIds.contains(it.id) }
    val regular = localFiltered.filter { !it.interesting && !trackedIds.contains(it.id) }

    Column(Modifier.fillMaxSize().background(sky.background)) {
        // Header
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = sky.primaryText)
            }
            Text(
                "Track a Flight", style = MaterialTheme.typography.titleLarge, color = sky.primaryText,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }

        // Search field
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                .background(sky.accent, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SkyBlue, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null,
                        tint = sky.secondaryText, modifier = Modifier.size(18.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search any flight number or airline…",
                            color = sky.secondaryText, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = query, onValueChange = { query = it },
                        textStyle = TextStyle(color = sky.primaryText, fontSize = 14.sp),
                        cursorBrush = SolidColor(SkyBlue),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear",
                        tint = sky.secondaryText,
                        modifier = Modifier.size(16.dp).clickable { query = "" })
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {

            // Global search results (flights not in local area)
            if (globalResults.isNotEmpty()) {
                item { SectionHeader("GLOBAL SEARCH", SkyBlue) }
                items(globalResults) { flight ->
                    FlightRow(
                        flight = flight, isTracked = trackedIds.contains(flight.id),
                        onToggle = { onToggle(flight.id) },
                        onSelect = { onSelect(flight.id) }
                    )
                }
            }

            if (tracked.isNotEmpty()) {
                item { SectionHeader("TRACKED", SkyBlue) }
                items(tracked) { flight ->
                    FlightRow(flight = flight, isTracked = true,
                        onToggle = { onToggle(flight.id) }, onSelect = { onSelect(flight.id) })
                }
            }
            if (special.isNotEmpty()) {
                item { SectionHeader("NOTABLE", SkyPurple) }
                items(special) { flight ->
                    FlightRow(flight = flight, isTracked = false,
                        onToggle = { onToggle(flight.id) }, onSelect = { onSelect(flight.id) })
                }
            }
            if (regular.isNotEmpty()) {
                item { SectionHeader("NEARBY", sky.secondaryText) }
                items(regular) { flight ->
                    FlightRow(flight = flight, isTracked = false,
                        onToggle = { onToggle(flight.id) }, onSelect = { onSelect(flight.id) })
                }
            }

            if (localFiltered.isEmpty() && globalResults.isEmpty() && !isSearching && query.isNotEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✈", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("No flights match \"$query\"",
                                color = sky.secondaryText, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, color: Color) {
    Text(
        label, color = color.copy(alpha = 0.8f), fontSize = 10.sp,
        fontWeight = FontWeight.W700, letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
fun FlightRow(flight: Flight, isTracked: Boolean, onToggle: () -> Unit, onSelect: () -> Unit) {
    val sky = LocalSkyPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(42.dp).background(sky.accent, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("✈", color = sky.primaryText.copy(alpha = 0.8f), fontSize = 18.sp)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(flight.callsign, color = sky.primaryText, fontWeight = FontWeight.W600, fontSize = 15.sp)
                if (flight.interesting) {
                    Text("★", color = SkyPurple, fontSize = 9.sp,
                        modifier = Modifier.background(SkyPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp))
                }
            }
            val subtitle = buildString {
                append(flight.airline)
                if (flight.model.isNotEmpty()) append("  ·  ${flight.model}")
                if (flight.fromCode.isNotEmpty() || flight.toCode.isNotEmpty()) {
                    append("  ·  ${flight.fromCode} → ${flight.toCode}")
                }
            }
            Text(subtitle, color = sky.secondaryText, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (flight.distanceKm > 0f) {
                Text("${flight.distanceKm.toInt()} km",
                    color = sky.secondaryText, fontSize = 12.sp)
            }
            Text("${flight.altitudeFt / 100 * 100} ft",
                color = sky.secondaryText.copy(alpha = 0.75f), fontSize = 11.sp)
        }
        IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
            Icon(
                if (isTracked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Track",
                tint = if (isTracked) sky.blue else sky.secondaryText.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
