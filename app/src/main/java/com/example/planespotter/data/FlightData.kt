package com.example.planespotter.data

import kotlin.math.*

data class Flight(
    val id: String,
    val callsign: String,
    val airline: String,
    val model: String,
    val typeCode: String = "",
    val aircraftCategory: String = "Private",
    val isMilitary: Boolean = false,
    val bearing: Float,         // absolute bearing from user 0–360
    val distanceKm: Float,
    val altitudeFt: Int,
    val speedMph: Int,
    val track: Float,           // aircraft nose direction
    val overheadMin: Int,
    val fromCode: String,
    val fromCity: String,
    val toCode: String,
    val toCity: String,
    val progress: Float,        // 0.0–1.0
    val interesting: Boolean = false,
    val status: String = "En route",
    val airlineIataCode: String = "",   // 2-letter IATA code for logo lookups
    val registration: String = "",      // tail number e.g. G-STBH
    val yearBuilt: Int = 0,             // manufacture year from hexdb.io
    val lat: Double = 0.0,      // last known position for dead reckoning
    val lon: Double = 0.0,
    val lastSeenMs: Long = 0L
)

val AIRCRAFT_FILTER_TYPES = listOf("Commercial", "Private", "Cargo", "Military", "Helicopter")

fun Flight.matchesAircraftTypes(selectedTypes: Set<String>): Boolean =
    selectedTypes.isNotEmpty() && aircraftCategory in selectedTypes

enum class UnitSystem { METRIC, IMPERIAL }
enum class AircraftLabelMode { CALLSIGN, AIRLINE }
enum class AppThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light")
}
enum class RadarPlaneScale(val label: String, val multiplier: Float) {
    SMALL("Small", 1.55f),
    NORMAL("Normal", 1.90f),
    LARGE("Large", 2.45f)
}

data class DisplaySettings(
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val labelMode: AircraftLabelMode = AircraftLabelMode.CALLSIGN,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val radarPlaneScale: RadarPlaneScale = RadarPlaneScale.NORMAL
)

fun Flight.primaryLabel(mode: AircraftLabelMode): String =
    when (mode) {
        AircraftLabelMode.CALLSIGN -> callsign
        AircraftLabelMode.AIRLINE -> airline.ifBlank { callsign }
    }

fun Flight.secondaryLabel(mode: AircraftLabelMode): String =
    when (mode) {
        AircraftLabelMode.CALLSIGN -> airline.ifBlank { aircraftCategory }
        AircraftLabelMode.AIRLINE -> callsign
    }

fun formatDistance(km: Float, unitSystem: UnitSystem, decimals: Int = 1): String =
    when (unitSystem) {
        UnitSystem.METRIC -> "%.${decimals}f km".format(km)
        UnitSystem.IMPERIAL -> "%.${decimals}f mi".format(km * 0.621371f)
    }

fun formatDistanceShort(km: Float, unitSystem: UnitSystem): String =
    when (unitSystem) {
        UnitSystem.METRIC -> "%.0fkm".format(km)
        UnitSystem.IMPERIAL -> "%.0fmi".format(km * 0.621371f)
    }

fun formatAltitude(feet: Int, unitSystem: UnitSystem): String =
    when (unitSystem) {
        UnitSystem.METRIC -> "%,d m".format((feet * 0.3048).toInt())
        UnitSystem.IMPERIAL -> "%,d ft".format(feet)
    }

fun formatAltitudeValue(feet: Int, unitSystem: UnitSystem): String =
    when (unitSystem) {
        UnitSystem.METRIC -> "%,d".format((feet * 0.3048).toInt())
        UnitSystem.IMPERIAL -> "%,d".format(feet)
    }

fun altitudeUnit(unitSystem: UnitSystem): String =
    if (unitSystem == UnitSystem.METRIC) "m" else "ft"

fun formatSpeed(mph: Int, unitSystem: UnitSystem): String =
    when (unitSystem) {
        UnitSystem.METRIC -> "${(mph * 1.60934).toInt()} km/h"
        UnitSystem.IMPERIAL -> "$mph mph"
    }

fun formatSpeedValue(mph: Int, unitSystem: UnitSystem): String =
    when (unitSystem) {
        UnitSystem.METRIC -> "${(mph * 1.60934).toInt()}"
        UnitSystem.IMPERIAL -> "$mph"
    }

fun speedUnit(unitSystem: UnitSystem): String =
    if (unitSystem == UnitSystem.METRIC) "km/h" else "mph"

val MOCK_FLIGHTS = listOf(
    Flight(
        id = "BA123", callsign = "BA123", airline = "British Airways", model = "Boeing 777-300ER",
        bearing = 5f, distanceKm = 3.2f, altitudeFt = 32500, speedMph = 515, track = 90f,
        overheadMin = 2, fromCode = "JFK", fromCity = "New York", toCode = "LHR", toCity = "London",
        progress = 0.58f, interesting = false, status = "En route", airlineIataCode = "BA"
    ),
    Flight(
        id = "UA88", callsign = "UA88", airline = "United Airlines", model = "Airbus A350-900",
        bearing = 78f, distanceKm = 5.6f, altitudeFt = 38000, speedMph = 540, track = 110f,
        overheadMin = 5, fromCode = "SFO", fromCity = "San Francisco", toCode = "FRA", toCity = "Frankfurt",
        progress = 0.42f, interesting = false, status = "En route", airlineIataCode = "UA"
    ),
    Flight(
        id = "AF22", callsign = "AF22", airline = "Air France", model = "Boeing 787-9",
        bearing = 188f, distanceKm = 8.1f, altitudeFt = 28000, speedMph = 480, track = 270f,
        overheadMin = 12, fromCode = "CDG", fromCity = "Paris", toCode = "BOS", toCity = "Boston",
        progress = 0.70f, interesting = false, status = "En route", airlineIataCode = "AF"
    ),
    Flight(
        id = "DL451", callsign = "DL451", airline = "Delta Air Lines", model = "Airbus A321neo",
        bearing = 240f, distanceKm = 22f, altitudeFt = 18000, speedMph = 410, track = 60f,
        overheadMin = 18, fromCode = "ATL", fromCity = "Atlanta", toCode = "SEA", toCity = "Seattle",
        progress = 0.30f, interesting = false, status = "Descending", airlineIataCode = "DL"
    ),
    Flight(
        id = "LH401", callsign = "LH401", airline = "Lufthansa", model = "Airbus A340-600",
        bearing = 305f, distanceKm = 42f, altitudeFt = 36000, speedMph = 525, track = 200f,
        overheadMin = 30, fromCode = "MUC", fromCity = "Munich", toCode = "IAD", toCity = "Washington",
        progress = 0.85f, interesting = false, status = "En route", airlineIataCode = "LH"
    ),
    Flight(
        id = "EK7", callsign = "EK7", airline = "Emirates", model = "Airbus A380-800",
        bearing = 140f, distanceKm = 15f, altitudeFt = 41000, speedMph = 575, track = 320f,
        overheadMin = 8, fromCode = "DXB", fromCity = "Dubai", toCode = "JFK", toCity = "New York",
        progress = 0.52f, interesting = true, status = "En route", airlineIataCode = "EK"
    ),
)

fun humanDirection(bearing: Float, altitudeFt: Int, distanceKm: Float = 0f): String {
    val b = ((bearing % 360) + 360) % 360
    val dirs = listOf("North", "North-East", "East", "South-East", "South", "South-West", "West", "North-West")
    val dir = dirs[(Math.round(b / 45f) % 8).toInt()]
    if (distanceKm > 0.5f && altitudeFt > 0) {
        if (distanceKm < 2f) return "Almost overhead"
        val elevDeg = Math.toDegrees(atan2(altitudeFt * 0.3048, distanceKm * 1000.0)).toInt()
        return when {
            elevDeg < 5  -> "$dir — beyond horizon"
            elevDeg < 10 -> "$dir — near horizon"
            else         -> "$dir, ${elevDeg}° up"
        }
    }
    return when {
        altitudeFt > 38000 -> "High in the $dir"
        altitudeFt > 25000 -> "Look $dir"
        altitudeFt > 12000 -> "$dir — mid sky"
        else               -> "Low in the $dir"
    }
}

// Dead reckoning: extrapolate aircraft position from last known fix using speed + track.
// Capped at 2 minutes to avoid wildly wrong positions on stale data.
fun Flight.extrapolate(userLoc: UserLocation, nowMs: Long): Flight {
    if (lastSeenMs == 0L || lat == 0.0 && lon == 0.0) return this
    val elapsedMs = (nowMs - lastSeenMs).coerceIn(0L, 120_000L)
    if (elapsedMs < 500L) return this

    val elapsedHrs  = elapsedMs / 3_600_000.0
    val speedKmh    = speedMph * 1.60934
    val distKm      = speedKmh * elapsedHrs
    val trackRad    = Math.toRadians(track.toDouble())
    val earthR      = 6371.0

    val newLat = lat + Math.toDegrees(distKm / earthR * cos(trackRad))
    val newLon = lon + Math.toDegrees(distKm / earthR * sin(trackRad) / cos(Math.toRadians(lat)))

    val newBearing = haversineBearing(userLoc.lat, userLoc.lon, newLat, newLon)
    val newDist    = haversineDistance(userLoc.lat, userLoc.lon, newLat, newLon)

    // CPA-based overhead time — 999 means the plane is moving away (tCpa < 0)
    val newOverhead = cpaMinutes(userLoc.lat, userLoc.lon, newLat, newLon, speedMph, track)

    return copy(
        lat         = newLat,
        lon         = newLon,
        bearing     = newBearing.toFloat(),
        distanceKm  = newDist.toFloat(),
        overheadMin = newOverhead
    )
}

// Returns minutes to closest point of approach, or 999 if the plane is moving away.
internal fun cpaMinutes(
    userLat: Double, userLon: Double,
    planeLat: Double, planeLon: Double,
    speedMph: Int, trackDeg: Float
): Int {
    if (speedMph <= 0) return 999
    val speedKmMin = speedMph * 1.60934 / 60.0
    val trackRad   = Math.toRadians(trackDeg.toDouble())
    val vx         = speedKmMin * sin(trackRad)
    val vy         = speedKmMin * cos(trackRad)
    val cosLat     = cos(Math.toRadians(planeLat))
    val dx         = (planeLon - userLon) * cosLat * 111.32
    val dy         = (planeLat - userLat) * 111.32
    val dotVV      = vx * vx + vy * vy
    if (dotVV < 1e-10) return 999
    val tCpa = -(dx * vx + dy * vy) / dotVV
    return if (tCpa > 0) tCpa.toInt().coerceIn(1, 998) else 999
}

// Closest Point of Approach: returns true if this plane's straight-line trajectory
// will bring it within radiusKm of the user within withinMinutes, even if it's outside now.
fun Flight.willEnterRadius(userLoc: UserLocation, radiusKm: Float, withinMinutes: Int = 15): Boolean {
    if (distanceKm <= radiusKm) return true
    if (speedMph <= 0 || (lat == 0.0 && lon == 0.0)) return false

    val speedKmMin = speedMph * 1.60934 / 60.0
    val trackRad   = Math.toRadians(track.toDouble())
    // Velocity components in flat-earth km/min (good enough for <300 km)
    val vx = speedKmMin * sin(trackRad)
    val vy = speedKmMin * cos(trackRad)

    val cosLat = cos(Math.toRadians(userLoc.lat))
    val dx = (lon - userLoc.lon) * cosLat * 111.32   // km east
    val dy = (lat - userLoc.lat) * 111.32             // km north

    val dotVV = vx * vx + vy * vy
    if (dotVV < 1e-10) return false
    val tCpa = -(dx * vx + dy * vy) / dotVV          // minutes to CPA
    if (tCpa < 0 || tCpa > withinMinutes) return false

    val cpaX = dx + vx * tCpa
    val cpaY = dy + vy * tCpa
    return sqrt(cpaX * cpaX + cpaY * cpaY) <= radiusKm
}

fun relDeg(absoluteBearing: Float, heading: Float): Float =
    ((absoluteBearing - heading + 540f) % 360f) - 180f

fun bearingColor(relDeg: Float): Long {
    val a = Math.abs(relDeg)
    return when {
        a < 60f  -> 0xFF5BB3FF
        a < 130f -> 0xFF9B7DFF
        else     -> 0xFFFF5B7A
    }
}
