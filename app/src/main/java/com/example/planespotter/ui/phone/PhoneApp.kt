package com.example.planespotter.ui.phone

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.mediarouter.app.MediaRouteChooserDialog
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.example.planespotter.cast.CAST_APP_ID
import com.example.planespotter.cast.SkyLookCastChannel
import com.example.planespotter.data.*
import com.example.planespotter.ui.cast.CastState
import com.example.planespotter.ui.cast.SkyLookPresentation
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.CastMediaControlIntent
import com.example.planespotter.notifications.AlertSettings
import kotlinx.coroutines.flow.first
import com.example.planespotter.ui.components.AirlineLogo
import com.example.planespotter.notifications.PlaneNotifications
import com.example.planespotter.notifications.alertDescription
import com.example.planespotter.ui.components.CompassView
import com.example.planespotter.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AppView { RADAR, WALL }

@Composable
fun PlaneSpotterPhoneApp(modifier: Modifier = Modifier, deepLinkFlightId: String? = null) {
    val context = LocalContext.current

    // ── Permissions ───────────────────────────────────────────────
    var hasLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocation = granted }

    var hasNotificationPermission by remember { mutableStateOf(PlaneNotifications.hasPermission(context)) }
    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasNotificationPermission = PlaneNotifications.hasPermission(context) }
    val requestNotificationPermission = {
        hasNotificationPermission = PlaneNotifications.hasPermission(context)
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermLauncher.launch(PlaneNotifications.PERMISSION)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocation) permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        PlaneNotifications.ensureChannel(context)
    }

    if (!hasLocation) {
        LocationPermissionScreen(onRequest = { permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) })
        return
    }

    // ── State ─────────────────────────────────────────────────────
    var manualHeading  by remember { mutableFloatStateOf(0f) }
    var focusedId      by remember { mutableStateOf<String?>(null) }
    var trackedIds     by remember { mutableStateOf(setOf<String>()) }
    var radiusKm       by remember { mutableFloatStateOf(25f) }
    var showDetail     by remember { mutableStateOf(false) }
    var showFilter     by remember { mutableStateOf(false) }
    var showCompass    by remember { mutableStateOf(false) }
    var forceManual    by remember { mutableStateOf(false) }
    var activeView     by remember { mutableStateOf(AppView.RADAR) }
    var minAltitudeFt  by remember { mutableFloatStateOf(0f) }
    var aircraftTypes  by remember { mutableStateOf(setOf("Commercial", "Private", "Cargo")) }
    var alertSettings  by remember { mutableStateOf(AlertSettings()) }
    var displaySettings by remember { mutableStateOf(DisplaySettings()) }
    var oneShotAlertIds by remember { mutableStateOf(setOf<String>()) }
    var oneShotAlertLabels by remember { mutableStateOf(mapOf<String, String>()) }
    val notificationHistory = remember { mutableMapOf<String, Long>() }

    // ── Persistence ───────────────────────────────────────────────
    var settingsLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        skyLookSettingsFlow(context).first().let { s ->
            trackedIds      = s.trackedIds
            alertSettings   = s.alertSettings
            displaySettings = s.displaySettings
            settingsLoaded  = true
        }
    }
    LaunchedEffect(trackedIds)      { if (settingsLoaded) saveTrackedIds(context, trackedIds) }
    LaunchedEffect(alertSettings)   { if (settingsLoaded) saveAlertSettings(context, alertSettings) }
    LaunchedEffect(displaySettings) { if (settingsLoaded) saveDisplaySettings(context, displaySettings) }

    val skyPalette = if (displaySettings.themeMode == AppThemeMode.LIGHT) LightSkyPalette else DarkSkyPalette

    // ── Compass heading ───────────────────────────────────────────
    // Pause the accelerometer + magnetometer while on the Wall view —
    // there's no compass there and running them continuously drains battery unnecessarily.
    val headingState = rememberHeading(manualHeading, forceManual, active = activeView == AppView.RADAR)
    val isManual        = headingState.mode == HeadingMode.MANUAL
    val compassAccurate = headingState.accuracy >= 2  // SENSOR_STATUS_ACCURACY_MEDIUM

    // Animate the heading so the compass ring glides rather than snaps.
    // We track an unbounded target (can exceed 360) so the animation always
    // takes the short arc — avoids spinning 350° when crossing the 0/360 boundary.
    var headingTarget by remember { mutableFloatStateOf(headingState.degrees) }
    LaunchedEffect(headingState.degrees) {
        val raw  = headingState.degrees
        val diff = ((raw - headingTarget % 360f + 540f) % 360f) - 180f
        headingTarget += diff
    }
    val heading by animateFloatAsState(
        targetValue = headingTarget,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 80f),
        label = "heading"
    )

    // ── GPS ───────────────────────────────────────────────────────
    // Coarsen the GPS interval when nothing is nearby to save battery; tighten when
    // aircraft are close. Starts at 60 s and is adjusted after the first flight fetch.
    var gpsIntervalMs by remember { mutableLongStateOf(60_000L) }
    val userLocation = rememberUserLocation(intervalMs = gpsIntervalMs)
    // rememberUpdatedState lets coroutines always read the latest location
    // without restarting the LaunchedEffect on every GPS tick
    val latestLocation by rememberUpdatedState(userLocation)

    // ── Live flight data ──────────────────────────────────────────
    var flights      by remember { mutableStateOf(emptyList<Flight>()) }
    var lastUpdated  by remember { mutableStateOf<Long?>(null) }
    var isLoading    by remember { mutableStateOf(false) }
    var fetchError   by remember { mutableStateOf<String?>(null) }

    // ── Route cache ───────────────────────────────────────────────
    val routeCache       = remember { mutableStateMapOf<String, RouteInfo?>() }
    val fetchedCallsigns = remember { mutableSetOf<String>() }
    val routeScope       = rememberCoroutineScope()

    // ── Aircraft info cache (registration, year built) ────────────
    val aircraftInfoCache  = remember { mutableStateMapOf<String, AircraftInfo?>() }
    val fetchedAircraftIds = remember { mutableSetOf<String>() }

    LaunchedEffect(radiusKm) {
        delay(300)  // debounce: slider changes cancel this before reaching the fetch
        var pollMs = 15_000L
        while (true) {
            val loc = latestLocation
            if (loc != null) {
                isLoading = true
                fetchError = null
                // Fetch 1.5× the display radius so we have data on planes about to enter
                val result = fetchNearbyFlights(loc, (radiusKm * 1.5).toDouble())
                result.fold(
                    onSuccess = { list ->
                        if (list.isNotEmpty()) {
                            flights = list
                            lastUpdated = System.currentTimeMillis()
                        }
                        fetchError = null
                        pollMs = 15_000L                          // reset on success
                    },
                    onFailure = {
                        fetchError = "Network error — showing cached data"
                        pollMs = (pollMs * 2).coerceAtMost(120_000L) // double, cap at 2 min
                    }
                )
                isLoading = false
                delay(pollMs)
            } else {
                delay(1_000)  // fast loop until GPS is ready, then switch to normal cadence
            }
        }
    }

    // Adjust GPS polling rate based on aircraft proximity
    LaunchedEffect(flights) {
        gpsIntervalMs = when {
            flights.any { it.distanceKm <= 5f } -> 30_000L   // aircraft very close — tighten updates
            flights.isNotEmpty()                -> 60_000L   // normal
            else                               -> 300_000L  // nothing around — coarsen to 5 min
        }
    }

    // Auto-select closest when no explicit selection, or when selected flight disappears
    LaunchedEffect(flights) {
        if (focusedId == null || flights.none { it.id == focusedId }) {
            focusedId = flights.minByOrNull { it.distanceKm }?.id
        }
        // Background-fetch route info for any callsign we haven't seen before.
        // Evict stale entries (not in current flight list) when the cache grows large.
        if (routeCache.size >= 100) {
            val active = flights.map { it.callsign }.toSet()
            routeCache.keys.filter { it !in active }.take(25).forEach {
                routeCache.remove(it); fetchedCallsigns.remove(it)
            }
        }
        flights.forEach { flight ->
            if (flight.callsign !in fetchedCallsigns) {
                fetchedCallsigns.add(flight.callsign)
                routeScope.launch {
                    routeCache[flight.callsign] = fetchRouteInfo(flight.callsign)
                }
            }
            if (flight.id !in fetchedAircraftIds) {
                fetchedAircraftIds.add(flight.id)
                routeScope.launch {
                    aircraftInfoCache[flight.id] = fetchAircraftInfo(flight.id)
                }
            }
        }
    }

    // Dead-reckoning tick: extrapolate positions every second, then include any plane
    // currently outside the radius whose CPA trajectory will bring it inside within 15 min.
    var displayFlights by remember { mutableStateOf(emptyList<Flight>()) }
    LaunchedEffect(Unit) {
        while (true) {
            val loc = latestLocation
            val now = System.currentTimeMillis()
            displayFlights = if (loc != null && flights.isNotEmpty()) {
                flights
                    .map { it.extrapolate(loc, now) }
                    .filter { it.willEnterRadius(loc, radiusKm) }
                    .filter { it.altitudeFt >= minAltitudeFt.toInt() }
                    .filter { it.matchesAircraftTypes(aircraftTypes) }
            } else {
                flights
                    .filter { it.altitudeFt >= minAltitudeFt.toInt() }
                    .filter { it.matchesAircraftTypes(aircraftTypes) }
            }
            delay(1_000)
        }
    }

    // Merge route + aircraft info into display lists
    val enrichedFlights    by remember { derivedStateOf { displayFlights.enrich(routeCache).enrichWithAircraftInfo(aircraftInfoCache) } }
    val enrichedRawFlights by remember {
        derivedStateOf {
            flights
                .filter { it.altitudeFt >= minAltitudeFt.toInt() }
                .filter { it.matchesAircraftTypes(aircraftTypes) }
                .enrich(routeCache)
                .enrichWithAircraftInfo(aircraftInfoCache)
        }
    }

    val focusedFlight = enrichedFlights.find { it.id == focusedId } ?: enrichedFlights.minByOrNull { it.distanceKm }

    // Deep-link: open detail for a specific flight when launched from a notification.
    // deepLinkHandled resets if a new notification arrives (new deepLinkFlightId).
    var deepLinkHandled by remember(deepLinkFlightId) { mutableStateOf(false) }
    LaunchedEffect(deepLinkFlightId, enrichedFlights) {
        if (!deepLinkHandled && deepLinkFlightId != null) {
            if (enrichedFlights.any { it.id == deepLinkFlightId }) {
                focusedId = deepLinkFlightId
                showDetail = true
                deepLinkHandled = true
            }
        }
    }

    // ── Cast / secondary display ──────────────────────────────────
    val activity = LocalContext.current as ComponentActivity
    var castPresentation by remember { mutableStateOf<SkyLookPresentation?>(null) }
    var castSession by remember { mutableStateOf<CastSession?>(null) }
    val castSelector = remember {
        MediaRouteSelector.Builder()
            .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)         // Miracast
            .addControlCategory(CastMediaControlIntent.categoryForCast(CAST_APP_ID)) // Chromecast
            .build()
    }

    // Miracast path — renders Presentation on the secondary display
    DisposableEffect(Unit) {
        val router = MediaRouter.getInstance(context)
        fun launch(display: android.view.Display) {
            castPresentation?.dismiss()
            SkyLookPresentation(activity, display).also { it.show(); castPresentation = it }
        }
        val callback = object : MediaRouter.Callback() {
            override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
                route.presentationDisplay?.let { launch(it) }
            }
            override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
                castPresentation?.dismiss(); castPresentation = null
            }
            override fun onRoutePresentationDisplayChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                val d = route.presentationDisplay
                if (d != null) launch(d) else { castPresentation?.dismiss(); castPresentation = null }
            }
        }
        router.addCallback(castSelector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        onDispose { router.removeCallback(callback); castPresentation?.dismiss() }
    }

    // Chromecast path — sends flight data to the web receiver via a Cast channel
    DisposableEffect(Unit) {
        val castContext = try { CastContext.getSharedInstance(context) } catch (_: Exception) { null }
        val listener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(session: CastSession, sessionId: String) { castSession = session }
            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) { castSession = session }
            override fun onSessionEnded(session: CastSession, error: Int) { castSession = null }
            override fun onSessionSuspended(session: CastSession, reason: Int) { castSession = null }
            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionStartFailed(session: CastSession, error: Int) {}
            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        }
        castContext?.sessionManager?.addSessionManagerListener(listener, CastSession::class.java)
        onDispose { castContext?.sessionManager?.removeSessionManagerListener(listener, CastSession::class.java) }
    }

    // Push state to whichever cast target is active.
    // Heading excluded — the cast display is always north-up.
    LaunchedEffect(enrichedFlights, trackedIds, focusedFlight?.id, displaySettings, lastUpdated, castPresentation, castSession) {
        val state = CastState(
            flights         = enrichedFlights,
            trackedIds      = trackedIds,
            focusedFlight   = focusedFlight,
            displaySettings = displaySettings,
            lastUpdated     = lastUpdated
        )
        castPresentation?.updateState(state)
        castSession?.let { SkyLookCastChannel.send(it, state) }
    }

    LaunchedEffect(enrichedFlights, trackedIds, oneShotAlertIds, alertSettings, radiusKm, hasNotificationPermission) {
        if (!hasNotificationPermission || enrichedFlights.isEmpty()) return@LaunchedEffect

        val now = System.currentTimeMillis()
        val frequency = alertSettings.frequency
        val cooldownMs = frequency.cooldownMs
        fun postOnce(key: String, title: String, message: String, flightId: String = "") {
            val lastPosted = notificationHistory[key] ?: 0L
            if (now - lastPosted < cooldownMs) return
            // Evict oldest entry so the map never grows unbounded in long sessions
            if (notificationHistory.size >= 100) {
                notificationHistory.minByOrNull { it.value }?.key?.let { notificationHistory.remove(it) }
            }
            notificationHistory[key] = now
            PlaneNotifications.notifyFlight(context, key.hashCode(), title, message, flightId)
        }

        val firedOneShotIds = mutableSetOf<String>()
        enrichedFlights.forEach { flight ->
            val isSoonOverhead = flight.overheadMin in 1..frequency.overheadMinutes || flight.distanceKm <= 2f
            val isTrackedSoon = flight.id in trackedIds && flight.overheadMin in 1..frequency.trackedMinutes
            val isOneShotReady = flight.id in oneShotAlertIds &&
                (flight.overheadMin in 1..frequency.oneShotMinutes || flight.distanceKm <= frequency.oneShotDistanceKm)

            if (alertSettings.overhead && isSoonOverhead) {
                postOnce(
                    key = "overhead:${flight.id}",
                    title = "Aircraft overhead",
                    message = flight.alertDescription(),
                    flightId = flight.id
                )
            }

            if (alertSettings.overhead && isTrackedSoon) {
                postOnce(
                    key = "tracked:${flight.id}",
                    title = "Tracked aircraft approaching",
                    message = flight.alertDescription(),
                    flightId = flight.id
                )
            }

            if (isOneShotReady) {
                postOnce(
                    key = "oneshot:${flight.id}",
                    title = "Flight alert",
                    message = flight.alertDescription(),
                    flightId = flight.id
                )
                firedOneShotIds.add(flight.id)
            }

            if (alertSettings.rare && flight.interesting && flight.distanceKm <= radiusKm) {
                postOnce(
                    key = "rare:${flight.id}",
                    title = "Rare aircraft nearby",
                    message = "${flight.callsign} is %.1f km away".format(flight.distanceKm),
                    flightId = flight.id
                )
            }
        }

        val nearbyCountForTraffic = enrichedFlights.count { it.distanceKm <= radiusKm }
        if (alertSettings.traffic && nearbyCountForTraffic >= frequency.trafficCount) {
            postOnce(
                key = "traffic",
                title = "Heavy traffic nearby",
                message = "$nearbyCountForTraffic aircraft are within ${radiusKm.toInt()} km"
            )
        }

        if (firedOneShotIds.isNotEmpty()) {
            oneShotAlertIds = oneShotAlertIds - firedOneShotIds
            oneShotAlertLabels = oneShotAlertLabels - firedOneShotIds
        }
    }

    // Minute ticker so the "X min ago" staleness label stays current
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(60_000); nowMs = System.currentTimeMillis() }
    }

    val nearbyCount = enrichedFlights.count { it.distanceKm <= radiusKm }
    val statusText = when {
        !compassAccurate                        -> "Compass needs calibration — wave phone in a figure-8"
        userLocation == null                    -> "Waiting for GPS…"
        isLoading && flights.isEmpty()          -> "Scanning for aircraft…"
        fetchError != null && flights.isEmpty() -> "Can't reach server — retrying…"
        // Offline with cached data — keep showing aircraft but surface the staleness
        fetchError != null -> {
            val ageMin = lastUpdated?.let { ((nowMs - it) / 60_000).toInt() } ?: 0
            "$nearbyCount aircraft · offline · ${ageMin}m ago"
        }
        nearbyCount == 0                        -> "No aircraft detected nearby"
        else                                    -> "$nearbyCount aircraft nearby"
    }

    CompositionLocalProvider(LocalSkyPalette provides skyPalette) {
    val sky = LocalSkyPalette.current
    Box(modifier = modifier.background(sky.background)) {

        AnimatedContent(targetState = activeView, label = "view") { view ->
            when (view) {
                AppView.RADAR -> RadarView(
                    flights = enrichedFlights,
                    heading = heading,
                    isManual = isManual,
                    manualHeading = manualHeading,
                    onManualChange = { manualHeading = it },
                    radiusKm = radiusKm,
                    focusedId = focusedId,
                    trackedIds = trackedIds,
                    focusedFlight = focusedFlight,
                    displaySettings = displaySettings,
                    isLoading = isLoading,
                    lastUpdated = lastUpdated,
                    statusText = statusText,
                    compassAccuracy = headingState.accuracy,
                    castSelector = castSelector,
                    isCasting = castPresentation != null || castSession != null,
                    onAircraftTap = { id -> focusedId = id; showDetail = true },
                    onExpandDetail = { showDetail = true },
                    onOpenFilter = { showFilter = true },
                    onOpenCompass = { showCompass = true },
                )
                AppView.WALL -> FlightWallScreen(
                    flights = enrichedRawFlights,
                    heading = heading,
                    trackedIds = trackedIds,
                    lastUpdated = lastUpdated,
                    isLoading = isLoading,
                    locationError = fetchError,
                    displaySettings = displaySettings,
                    onFlightTap = { id ->
                        focusedId = id
                        showDetail = true
                        activeView = AppView.RADAR
                    },
                    onTrackToggle = { id -> trackedIds = if (trackedIds.contains(id)) trackedIds - id else trackedIds + id },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ── Bottom view toggle bar ─────────────────────────────────
        ViewToggleBar(
            active = activeView,
            onSwitch = { activeView = it },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 40.dp, vertical = 12.dp)
        )

        // ── Overlays ──────────────────────────────────────────────
        if (showDetail && focusedFlight != null) {
            FlightDetailSheet(
                flight = focusedFlight,
                heading = heading,
                isTracked = trackedIds.contains(focusedFlight.id),
                isNotifyEnabled = focusedFlight.id in oneShotAlertIds,
                displaySettings = displaySettings,
                onTrackToggle = { trackedIds = if (trackedIds.contains(focusedFlight.id)) trackedIds - focusedFlight.id else trackedIds + focusedFlight.id },
                onNotifyToggle = {
                    requestNotificationPermission()
                    oneShotAlertIds = if (focusedFlight.id in oneShotAlertIds) {
                        oneShotAlertLabels = oneShotAlertLabels - focusedFlight.id
                        oneShotAlertIds - focusedFlight.id
                    } else {
                        oneShotAlertLabels = oneShotAlertLabels + (focusedFlight.id to focusedFlight.callsign)
                        oneShotAlertIds + focusedFlight.id
                    }
                },
                onMapOpen = {
                    if (focusedFlight.lat != 0.0 || focusedFlight.lon != 0.0) {
                        val uri = Uri.parse("geo:${focusedFlight.lat},${focusedFlight.lon}?q=${focusedFlight.lat},${focusedFlight.lon}(${focusedFlight.callsign})")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                        }
                    }
                },
                onDismiss = { showDetail = false }
            )
        }
        if (showFilter) {
            FilterSheet(
                radiusKm = radiusKm,
                minAltitudeFt = minAltitudeFt,
                aircraftTypes = aircraftTypes,
                unitSystem = displaySettings.unitSystem,
                onRadiusChange = { radiusKm = it },
                onMinAltitudeChange = { minAltitudeFt = it },
                onAircraftTypeToggle = { type ->
                    aircraftTypes = if (type in aircraftTypes) aircraftTypes - type else aircraftTypes + type
                },
                onClose = { showFilter = false }
            )
        }
        if (showCompass) {
            CompassSheet(
                headingState = headingState,
                forceManual = forceManual,
                manualHeading = manualHeading,
                alertSettings = alertSettings,
                displaySettings = displaySettings,
                activeFlightAlerts = oneShotAlertLabels,
                notificationsEnabled = hasNotificationPermission,
                onForceManualChange = { forceManual = it },
                onManualHeadingChange = { manualHeading = it },
                onAlertSettingsChange = { next ->
                    if ((!alertSettings.overhead && next.overhead) ||
                        (!alertSettings.rare && next.rare) ||
                        (!alertSettings.traffic && next.traffic)
                    ) {
                        requestNotificationPermission()
                    }
                    alertSettings = next
                },
                onDisplaySettingsChange = { displaySettings = it },
                onFlightAlertCancel = { id ->
                    oneShotAlertIds = oneShotAlertIds - id
                    oneShotAlertLabels = oneShotAlertLabels - id
                },
                onRequestNotifications = requestNotificationPermission,
                onClose = { showCompass = false }
            )
        }
    }
    }
}

// ── Radar sub-view ────────────────────────────────────────────────
@Composable
private fun RadarView(
    flights: List<Flight>,
    heading: Float,
    isManual: Boolean,
    manualHeading: Float,
    onManualChange: (Float) -> Unit,
    radiusKm: Float,
    focusedId: String?,
    trackedIds: Set<String>,
    focusedFlight: Flight?,
    displaySettings: DisplaySettings,
    isLoading: Boolean,
    lastUpdated: Long?,
    statusText: String,
    compassAccuracy: Int,
    castSelector: MediaRouteSelector,
    isCasting: Boolean,
    onAircraftTap: (String) -> Unit,
    onExpandDetail: () -> Unit,
    onOpenFilter: () -> Unit,
    onOpenCompass: () -> Unit,
) {
    val sky = LocalSkyPalette.current
    val context = LocalContext.current
    Box(Modifier.fillMaxSize()) {
        CompassView(
            flights = flights, heading = heading, radiusKm = radiusKm,
            focusedId = focusedId, trackedIds = trackedIds,
            unitSystem = displaySettings.unitSystem,
            labelMode = displaySettings.labelMode,
            planeScale = displaySettings.radarPlaneScale,
            onFlightTap = onAircraftTap,
            modifier = Modifier.fillMaxSize()
        )

        // Top gradient fade
        Box(
            Modifier.fillMaxWidth().height(150.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(sky.background, Color.Transparent)))
        )

        // Top bar
        Column(
            Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SkyLook", style = MaterialTheme.typography.titleLarge, color = sky.primaryText)
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = SkyBlue, strokeWidth = 2.dp)
                        }
                    }
                    Text(statusText, style = MaterialTheme.typography.bodyMedium, color = sky.secondaryText)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Cast button — MediaRouteChooserDialog discovers both Miracast and Chromecast
                    IconButton(
                        onClick = {
                            MediaRouteChooserDialog(context).apply {
                                setRouteSelector(castSelector)
                                show()
                            }
                        },
                        modifier = Modifier.size(40.dp).background(
                            if (isCasting) SkyBlue.copy(alpha = 0.18f) else sky.accent, CircleShape
                        )
                    ) {
                        Icon(
                            Icons.Default.Cast,
                            contentDescription = if (isCasting) "Casting" else "Cast to screen",
                            tint = if (isCasting) SkyBlue else sky.primaryText
                        )
                    }
                    IconButton(onClick = onOpenCompass,
                        modifier = Modifier.size(40.dp).background(
                            if (compassAccuracy < 2) sky.red.copy(alpha = 0.18f) else sky.accent, CircleShape)) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings",
                            tint = if (compassAccuracy < 2) sky.red else sky.primaryText)
                    }
                    IconButton(onClick = onOpenFilter,
                        modifier = Modifier.size(40.dp).background(sky.accent, CircleShape)) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = sky.primaryText)
                    }
                }
            }

            if (isManual) {
                Spacer(Modifier.height(8.dp))
                ManualHeadingBar(heading = manualHeading, onChange = onManualChange, modifier = Modifier.fillMaxWidth())
            }
        }

        // Bottom gradient
        Box(
            Modifier.fillMaxWidth().height(200.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, sky.background.copy(alpha = 0.95f))))
        )

        // Focused flight bar
        if (focusedFlight != null) {
            FocusedBar(
                flight = focusedFlight, heading = heading,
                displaySettings = displaySettings,
                onExpand = onExpandDetail,
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .navigationBarsPadding().padding(bottom = 64.dp)
                    .padding(horizontal = 16.dp)
            )
        }
    }

}

@Composable
private fun ViewToggleBar(active: AppView, onSwitch: (AppView) -> Unit, modifier: Modifier = Modifier) {
    val sky = LocalSkyPalette.current
    Row(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(sky.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ToggleTab(label = "Radar", icon = "⊙", selected = active == AppView.RADAR, onClick = { onSwitch(AppView.RADAR) }, modifier = Modifier.weight(1f))
        ToggleTab(label = "Wall", icon = "≡", selected = active == AppView.WALL, onClick = { onSwitch(AppView.WALL) }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ToggleTab(label: String, icon: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sky = LocalSkyPalette.current
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) sky.blue.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(icon, color = if (selected) sky.blue else sky.secondaryText, fontSize = 14.sp)
            Text(label, color = if (selected) sky.blue else sky.secondaryText,
                fontSize = 13.sp, fontWeight = if (selected) FontWeight.W600 else FontWeight.W400)
        }
    }
}

@Composable
private fun LocationPermissionScreen(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize().background(SkyBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(40.dp)) {
            Text("✈", fontSize = 64.sp)
            Text("SkyLook", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Text("Location access is needed to find aircraft flying near you.",
                color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Allow Location", fontSize = 15.sp, fontWeight = FontWeight.W600)
            }
        }
    }
}

@Composable
private fun FocusedBar(
    flight: Flight,
    heading: Float,
    displaySettings: DisplaySettings,
    onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val sky = LocalSkyPalette.current
    val rel = relDeg(flight.bearing, heading)
    val color = Color(bearingColor(rel).toInt())
    val whereToLook = humanDirection(flight.bearing, flight.altitudeFt, flight.distanceKm)

    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(sky.surface)
            .clickable(onClick = onExpand)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top row: logo + callsign/route + expand arrow
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AirlineLogo(iataCode = flight.airlineIataCode, airlineName = flight.airline, size = 44.dp, cornerRadius = 11.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        flight.primaryLabel(displaySettings.labelMode),
                        style = MaterialTheme.typography.titleMedium,
                        color = sky.primaryText,
                        fontWeight = FontWeight.W700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (flight.interesting) {
                        Text("★", color = SkyPurple, fontSize = 10.sp,
                            modifier = Modifier.background(SkyPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                }
                if (flight.fromCode.isNotEmpty() && flight.toCode.isNotEmpty()) {
                    Text("${flight.fromCode} → ${flight.toCode} · ${flight.secondaryLabel(displaySettings.labelMode)}",
                        style = MaterialTheme.typography.bodySmall, color = sky.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    Text(flight.secondaryLabel(displaySettings.labelMode),
                        style = MaterialTheme.typography.bodySmall, color = sky.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null,
                tint = sky.secondaryText.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }

        // Where to look — the most useful thing when you're trying to spot the plane
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("LOOK", color = color, fontSize = 9.sp, fontWeight = FontWeight.W800, letterSpacing = 1.sp)
            Text(whereToLook, color = color, fontSize = 14.sp, fontWeight = FontWeight.W700, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // Key stats
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FocusMetric("ALT", formatAltitude(flight.altitudeFt, displaySettings.unitSystem), Modifier.weight(1f))
            FocusMetric("SPEED", formatSpeed(flight.speedMph, displaySettings.unitSystem), Modifier.weight(1f))
            FocusMetric("NEAREST", if (flight.overheadMin < 999) "${flight.overheadMin} min" else "away", Modifier.weight(1f))
        }
    }
}

@Composable
private fun FocusMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val sky = LocalSkyPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(sky.accent)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = sky.secondaryText.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.W700, letterSpacing = 0.5.sp, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(value, color = sky.primaryText, fontSize = 14.sp, fontWeight = FontWeight.W700, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun focusedRouteText(flight: Flight, displaySettings: DisplaySettings): String {
    val route = if (flight.fromCode.isNotEmpty() && flight.toCode.isNotEmpty()) {
        "${flight.fromCode} → ${flight.toCode}"
    } else {
        flight.secondaryLabel(displaySettings.labelMode)
    }
    return "$route · ${humanDirection(flight.bearing, flight.altitudeFt, flight.distanceKm)}"
}

@Composable
private fun ManualHeadingBar(heading: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(SkySurface.copy(alpha = 0.92f)).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Explore, contentDescription = null, tint = SkyPurple, modifier = Modifier.size(16.dp))
                Text("Manual heading", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            Text("${heading.toInt()}°", color = SkyBlue, fontWeight = FontWeight.W600, fontSize = 13.sp)
        }
        Slider(value = heading, onValueChange = onChange, valueRange = 0f..359f,
            colors = SliderDefaults.colors(
                thumbColor = LocalSkyPalette.current.blue,
                activeTrackColor = LocalSkyPalette.current.blue,
                inactiveTrackColor = LocalSkyPalette.current.divider
            ),
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FilterSheet(
    radiusKm: Float,
    minAltitudeFt: Float,
    aircraftTypes: Set<String>,
    unitSystem: UnitSystem,
    onRadiusChange: (Float) -> Unit,
    onMinAltitudeChange: (Float) -> Unit,
    onAircraftTypeToggle: (String) -> Unit,
    onClose: () -> Unit
) {
    val sky = LocalSkyPalette.current
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onClose), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .heightIn(max = 720.dp)
                .background(sky.surface)
                .clickable(onClick = {})
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp)
                .navigationBarsPadding()
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Filter", style = MaterialTheme.typography.headlineMedium, color = sky.primaryText)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SEARCH RADIUS", color = sky.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.W700, letterSpacing = 1.3.sp)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (unitSystem == UnitSystem.METRIC) "${radiusKm.toInt()}" else "${(radiusKm * 0.621371f).toInt()}",
                        color = sky.primaryText,
                        fontWeight = FontWeight.W800,
                        fontSize = 28.sp
                    )
                    Text(if (unitSystem == UnitSystem.METRIC) "km" else "mi", color = sky.secondaryText, fontSize = 16.sp, modifier = Modifier.padding(bottom = 5.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Slider(value = radiusKm, onValueChange = onRadiusChange, valueRange = 5f..200f,
                colors = SliderDefaults.colors(thumbColor = sky.blue, activeTrackColor = sky.blue, inactiveTrackColor = sky.divider), modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (unitSystem == UnitSystem.METRIC) {
                    Text("5 km", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
                    Text("50", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
                    Text("100", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
                    Text("200 km", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
                } else {
                    Text("3 mi", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
                    Text("31", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
                    Text("62", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
                    Text("124 mi", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SkyBlue.copy(alpha = 0.09f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(20.dp))
                Text("Balanced — covers a typical metro flight corridor.", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 2)
            }

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("MIN ALTITUDE", color = sky.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.W700, letterSpacing = 1.3.sp)
                Text(formatAltitude(minAltitudeFt.toInt(), unitSystem), color = sky.primaryText, fontWeight = FontWeight.W700, fontSize = 16.sp)
            }
            Slider(value = minAltitudeFt, onValueChange = onMinAltitudeChange, valueRange = 0f..45000f,
                colors = SliderDefaults.colors(thumbColor = sky.blue, activeTrackColor = sky.blue, inactiveTrackColor = sky.divider), modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(20.dp))
            Text("AIRCRAFT TYPES", color = sky.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.W700, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AIRCRAFT_FILTER_TYPES.forEach { type ->
                    val selected = type in aircraftTypes
                    FilterPill(label = type, selected = selected, onClick = { onAircraftTypeToggle(type) })
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = sky.blue, contentColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("Apply", fontSize = 16.sp, fontWeight = FontWeight.W700)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) SkyBlue.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = if (selected) SkyBlue else Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            fontWeight = FontWeight.W700
        )
    }
}
