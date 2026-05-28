// Aircraft data sourced from api.airplanes.live (Baada.io ADS-B aggregator), not OpenSky Network.
package com.example.planespotter.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.*

private val AIRLINES = mapOf(
    "AAL" to "American Airlines",   "AFR" to "Air France",
    "AIC" to "Air India",           "ANZ" to "Air New Zealand",
    "AUA" to "Austrian Airlines",   "BAW" to "British Airways",
    "CAL" to "China Airlines",      "CCA" to "Air China",
    "CSN" to "China Southern",      "DAL" to "Delta Air Lines",
    "DLH" to "Lufthansa",           "EIN" to "Aer Lingus",
    "ETD" to "Etihad Airways",      "ETH" to "Ethiopian Airlines",
    "EVA" to "EVA Air",             "EWG" to "Eurowings",
    "EZY" to "easyJet",             "FDX" to "FedEx",
    "FIN" to "Finnair",             "IBE" to "Iberia",
    "ICE" to "Icelandair",          "JAL" to "Japan Airlines",
    "JBU" to "JetBlue",             "KAL" to "Korean Air",
    "KLM" to "KLM",                 "LOT" to "LOT Polish Airlines",
    "MAS" to "Malaysia Airlines",   "QFA" to "Qantas",
    "QTR" to "Qatar Airways",       "RYR" to "Ryanair",
    "SAS" to "SAS",                 "SIA" to "Singapore Airlines",
    "SWA" to "Southwest Airlines",  "SWR" to "Swiss",
    "TAP" to "TAP Air Portugal",    "THA" to "Thai Airways",
    "THY" to "Turkish Airlines",    "TOM" to "TUI Airways",
    "UAE" to "Emirates",            "UAL" to "United Airlines",
    "UPS" to "UPS Airlines",        "VIR" to "Virgin Atlantic",
    "WJA" to "WestJet",             "GEC" to "Lufthansa Cargo",
)

// IATA (2-letter) codes mapped from the ICAO (3-letter) callsign prefix, used for logo URLs
private val IATA_CODES = mapOf(
    "AAL" to "AA", "AFR" to "AF", "AIC" to "AI", "ANZ" to "NZ", "AUA" to "OS",
    "BAW" to "BA", "CAL" to "CI", "CCA" to "CA", "CSN" to "CZ", "DAL" to "DL",
    "DLH" to "LH", "EIN" to "EI", "ETD" to "EY", "ETH" to "ET", "EVA" to "BR",
    "EWG" to "EW", "EZY" to "U2", "FDX" to "FX", "FIN" to "AY", "IBE" to "IB",
    "ICE" to "FI", "JAL" to "JL", "JBU" to "B6", "KAL" to "KE", "KLM" to "KL",
    "LOT" to "LO", "MAS" to "MH", "QFA" to "QF", "QTR" to "QR", "RYR" to "FR",
    "SAS" to "SK", "SIA" to "SQ", "SWA" to "WN", "SWR" to "LX", "TAP" to "TP",
    "THA" to "TG", "THY" to "TK", "TOM" to "BY", "UAE" to "EK", "UAL" to "UA",
    "UPS" to "5X", "VIR" to "VS", "WJA" to "WS", "GEC" to "LH",
)

fun airlineIata(callsign: String): String = IATA_CODES[callsign.take(3).uppercase()] ?: ""

private val PASSENGER_AIRLINE_PREFIXES = AIRLINES.keys - setOf("FDX", "UPS", "GEC")

private val CARGO_PREFIXES = setOf(
    "ABX", "BOX", "CKS", "FDX", "GTI", "GEC", "NCA", "PAC", "SNC", "TAY", "UPS"
)

// "C" and "F" were too broad — they matched CRJ (regional commercial) and Fokker F50/F100.
// Use more specific prefixes so those commercial jets classify correctly.
private val PRIVATE_TYPE_PREFIXES = setOf(
    "C17", "C18", "C20", "C25", "C35", "C50", "C56",   // Cessna (not CRJ)
    "CL",                                                 // Bombardier Challenger
    "E50", "E55",                                         // Embraer Phenom
    "F2T", "F7X", "F8X", "F90", "FA7", "FA8",           // Dassault Falcon (not Fokker F100/F50)
    "GL",                                                 // Gulfstream / Bombardier Global
    "LJ",                                                 // Learjet
    "PC",                                                 // Pilatus
    "PRM",                                                // Piaggio Avanti
    "TBM"                                                 // TBM
)

private val HELICOPTER_TYPE_PREFIXES = setOf(
    "A109", "A119", "AS3", "AS5", "B06", "B47", "B407", "B429", "EC", "H1", "H2", "H3", "H4",
    "H5", "H6", "H7", "H8", "H9", "MD", "R22", "R44", "R66", "S61", "S76", "S92"
)

private val AIRCRAFT_TYPES = mapOf(
    "B736" to "Boeing 737-600",          "B737" to "Boeing 737-700",
    "B738" to "Boeing 737-800",          "B739" to "Boeing 737-900",
    "B37M" to "Boeing 737 MAX 7",        "B38M" to "Boeing 737 MAX 8",
    "B39M" to "Boeing 737 MAX 9",
    "B744" to "Boeing 747-400",          "B748" to "Boeing 747-8",
    "B752" to "Boeing 757-200",          "B753" to "Boeing 757-300",
    "B762" to "Boeing 767-200",          "B763" to "Boeing 767-300",
    "B764" to "Boeing 767-400",
    "B772" to "Boeing 777-200",          "B773" to "Boeing 777-300",
    "B77W" to "Boeing 777-300ER",
    "B788" to "Boeing 787-8 Dreamliner", "B789" to "Boeing 787-9 Dreamliner",
    "B78X" to "Boeing 787-10 Dreamliner",
    "A318" to "Airbus A318",             "A319" to "Airbus A319",
    "A320" to "Airbus A320",             "A321" to "Airbus A321",
    "A19N" to "Airbus A319neo",          "A20N" to "Airbus A320neo",
    "A21N" to "Airbus A321neo",
    "A332" to "Airbus A330-200",         "A333" to "Airbus A330-300",
    "A338" to "Airbus A330-800neo",      "A339" to "Airbus A330-900neo",
    "A343" to "Airbus A340-300",         "A345" to "Airbus A340-500",
    "A346" to "Airbus A340-600",
    "A359" to "Airbus A350-900",         "A35K" to "Airbus A350-1000",
    "A388" to "Airbus A380-800",
    "E170" to "Embraer 170",             "E175" to "Embraer 175",
    "E190" to "Embraer 190",             "E195" to "Embraer 195",
    "E75L" to "Embraer 175",             "E7W"  to "Embraer 190-E2",
    "CRJ2" to "Bombardier CRJ-200",      "CRJ7" to "Bombardier CRJ-700",
    "CRJ9" to "Bombardier CRJ-900",      "CRJX" to "Bombardier CRJ-1000",
    "DH8D" to "Bombardier Q400",         "DH8A" to "Bombardier Q200",
    "AT72" to "ATR 72",                  "AT75" to "ATR 72-500",
    "C172" to "Cessna 172",              "C208" to "Cessna 208 Caravan",
    "PC12" to "Pilatus PC-12",
    "TBM9" to "TBM 930",                 "TBM7" to "TBM 700",
    "GLF5" to "Gulfstream G550",         "GL7T" to "Gulfstream G700",
    "GLEX" to "Bombardier Global Express",
    "CL35" to "Bombardier Challenger 350", "CL60" to "Bombardier Challenger 600",
    "F900" to "Dassault Falcon 900",     "F2TH" to "Dassault Falcon 2000",
    "LJ35" to "Learjet 35",              "LJ45" to "Learjet 45",
    "C25A" to "Citation CJ2",           "C25B" to "Citation CJ3",
    "C56X" to "Citation XLS",
)

fun airlineName(callsign: String): String =
    AIRLINES[callsign.take(3).uppercase()] ?: callsign.take(3).uppercase()

fun aircraftModel(typeCode: String): String =
    AIRCRAFT_TYPES[typeCode.uppercase()] ?: typeCode

private fun classifyAircraft(
    callsign: String,
    airline: String,
    typeCode: String,
    isMilitary: Boolean
): String {
    if (isMilitary) return "Military"

    val prefix = callsign.take(3).uppercase()
    val type = typeCode.uppercase()
    val airlineName = airline.uppercase()

    if (HELICOPTER_TYPE_PREFIXES.any { type.startsWith(it) } ||
        airlineName.contains("HELICOPTER") ||
        airlineName.contains("AIR AMBULANCE")
    ) {
        return "Helicopter"
    }

    if (prefix in CARGO_PREFIXES ||
        airlineName.contains("CARGO") ||
        airlineName.contains("FEDEX") ||
        airlineName.contains("UPS") ||
        airlineName.contains("DHL")
    ) {
        return "Cargo"
    }

    if (prefix in PASSENGER_AIRLINE_PREFIXES) return "Commercial"

    if (PRIVATE_TYPE_PREFIXES.any { type.startsWith(it) } ||
        type in setOf("BE20", "BE40", "BE9L", "P28A", "PA31", "PA46", "SR20", "SR22")
    ) {
        return "Private"
    }

    return "Private"
}

private fun parseAircraftObject(s: org.json.JSONObject, loc: UserLocation?): Flight? {
    if (!s.has("lat") || !s.has("lon")) return null

    val altObj = if (s.has("alt_baro")) s.opt("alt_baro") else null
    val altFt = when (altObj) {
        is Int    -> altObj
        is Double -> altObj.toInt()
        is Long   -> altObj.toInt()
        else      -> 0
    }
    if (altFt < 200) return null

    val icao     = s.optString("hex", "").uppercase()
    val callsign = s.optString("flight", icao).trim().ifEmpty { icao }
    val acLat    = s.getDouble("lat")
    val acLon    = s.getDouble("lon")
    val gsKnots  = s.optDouble("gs", 0.0)
    val track    = s.optDouble("track", 0.0)
    val baroRate = s.optDouble("baro_rate", 0.0)
    val typeCode = s.optString("t", "")
    val dbFlags  = s.optInt("dbFlags", 0)
    // Baada.io also returns a top-level `mil` boolean; check both for coverage
    val isMilitary = dbFlags and 1 != 0 || s.optBoolean("mil", false)
    val isInteresting = dbFlags and 2 != 0
    val airline = airlineName(callsign)
    val model = aircraftModel(typeCode)

    val bearing     = if (loc != null) haversineBearing(loc.lat, loc.lon, acLat, acLon) else 0.0
    val distance    = if (loc != null) haversineDistance(loc.lat, loc.lon, acLat, acLon) else 0.0
    val speedMph    = (gsKnots * 1.15078).toInt()
    val overheadMin = if (speedMph > 0) ((distance / speedMph) * 60).toInt().coerceAtMost(999) else 999

    return Flight(
        id           = icao,
        callsign     = callsign,
        airline      = airline,
        model        = model,
        typeCode     = typeCode.uppercase(),
        aircraftCategory = classifyAircraft(callsign, airline, typeCode, isMilitary),
        isMilitary   = isMilitary,
        bearing      = bearing.toFloat(),
        distanceKm   = distance.toFloat(),
        altitudeFt   = altFt,
        speedMph     = speedMph,
        track        = track.toFloat(),
        overheadMin  = overheadMin,
        fromCode     = "", fromCity = "",
        toCode       = "", toCity   = "",
        progress     = 0f,
        airlineIataCode = airlineIata(callsign),
        interesting  = isInteresting || isMilitary || altFt > 38000 || speedMph > 560,
        status       = when {
            baroRate < -200 -> "Descending"
            baroRate >  200 -> "Climbing"
            else            -> "En route"
        },
        lat        = acLat,
        lon        = acLon,
        lastSeenMs = System.currentTimeMillis()
    )
}

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

private fun fetchJson(url: String): String {
    Log.d("AdsbFi", "GET $url")
    val request = Request.Builder()
        .url(url)
        .header("User-Agent", "SkyLook/1.0")
        .build()
    httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            Log.w("AdsbFi", "HTTP ${response.code}")
            throw Exception("HTTP ${response.code}")
        }
        val body = response.body?.string() ?: throw Exception("Empty body")
        Log.d("AdsbFi", "Response ${body.length} chars — ${body.take(300)}")
        return body
    }
}

private fun parseAcArray(json: String, loc: UserLocation?): List<Flight> {
    val root = JSONObject(json)
    // Log the top-level keys so we can see the actual response shape
    Log.d("AdsbFi", "Top-level keys: ${root.keys().asSequence().toList()}")
    val ac = root.optJSONArray("ac")
        ?: root.optJSONArray("aircraft")
        ?: root.optJSONArray("states")  // OpenSky fallback
        ?: run { Log.w("AdsbFi", "No aircraft array found in response"); return emptyList() }
    Log.d("AdsbFi", "Aircraft array length: ${ac.length()}")
    val flights = mutableListOf<Flight>()
    for (i in 0 until ac.length()) {
        parseAircraftObject(ac.getJSONObject(i), loc)?.let { flights += it }
    }
    Log.d("AdsbFi", "Parsed ${flights.size} airborne flights")
    return flights
}

suspend fun fetchNearbyFlights(loc: UserLocation, radiusKm: Double = 50.0): Result<List<Flight>> =
    withContext(Dispatchers.IO) {
        try {
            val radiusNm = (radiusKm / 1.852).toInt().coerceAtLeast(1)
            val url = "https://api.airplanes.live/v2/point/${loc.lat}/${loc.lon}/$radiusNm"
            val flights = parseAcArray(fetchJson(url), loc).sortedBy { it.distanceKm }
            Result.success(flights)
        } catch (e: Exception) {
            Log.w("AdsbFi", "fetch failed: ${e.message}")
            Result.failure(e)
        }
    }

suspend fun searchFlightGlobal(callsign: String, loc: UserLocation?): Result<List<Flight>> =
    withContext(Dispatchers.IO) {
        try {
            val cs = callsign.trim().uppercase()
            val url = "https://api.airplanes.live/v2/callsign/$cs"
            val flights = parseAcArray(fetchJson(url), loc)
            Result.success(flights)
        } catch (e: Exception) {
            Log.w("AdsbFi", "search failed: ${e.message}")
            Result.failure(e)
        }
    }

internal fun haversineBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLon = Math.toRadians(lon2 - lon1)
    val y = sin(dLon) * cos(Math.toRadians(lat2))
    val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
            sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}

internal fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 6371.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
}

data class RouteInfo(
    val fromCode: String,
    val fromCity: String,
    val fromLat: Double,
    val fromLon: Double,
    val toCode: String,
    val toCity: String,
    val toLat: Double,
    val toLon: Double,
    val airline: String
)

suspend fun fetchRouteInfo(callsign: String): RouteInfo? = withContext(Dispatchers.IO) {
    fetchHexDbRouteInfo(callsign) ?: fetchAdsbDbRouteInfo(callsign)
}

private fun fetchHexDbRouteInfo(callsign: String): RouteInfo? {
    try {
        val cs = callsign.trim().uppercase()
        val routeBody = fetchJson("https://hexdb.io/api/v1/route/icao/$cs")
        val route = JSONObject(routeBody)
        val routeCodes = route.optString("route", "")
            .split("-")
            .map { it.trim().uppercase() }
        if (routeCodes.size != 2 || routeCodes.any { it.length != 4 }) return null

        val origin = fetchHexDbAirport(routeCodes[0]) ?: return null
        val dest = fetchHexDbAirport(routeCodes[1]) ?: return null

        return RouteInfo(
            fromCode = origin.iata.ifEmpty { origin.icao },
            fromCity = origin.city,
            fromLat = origin.lat,
            fromLon = origin.lon,
            toCode = dest.iata.ifEmpty { dest.icao },
            toCity = dest.city,
            toLat = dest.lat,
            toLon = dest.lon,
            airline = airlineName(cs)
        )
    } catch (e: Exception) {
        Log.w("AdsbFi", "HexDB route lookup failed for $callsign: ${e.message}")
        return null
    }
}

private data class HexDbAirport(
    val icao: String,
    val iata: String,
    val city: String,
    val lat: Double,
    val lon: Double
)

private fun fetchHexDbAirport(icao: String): HexDbAirport? {
    val body = fetchJson("https://hexdb.io/api/v1/airport/icao/$icao")
    val json = JSONObject(body)
    if (json.optString("status") == "404") return null
    val airportName = json.optString("airport", "")
    val city = airportName
        .replace(" International Airport", "")
        .replace(" Airport", "")
        .replace(" Guglielmo Marconi", "")
        .ifBlank { json.optString("region_name", "") }

    return HexDbAirport(
        icao = json.optString("icao", icao),
        iata = json.optString("iata", ""),
        city = city,
        lat = json.optDouble("latitude", 0.0),
        lon = json.optDouble("longitude", 0.0)
    )
}

private fun fetchAdsbDbRouteInfo(callsign: String): RouteInfo? {
    try {
        val cs = callsign.trim().uppercase()
        val body = fetchJson("https://api.adsbdb.com/v0/callsign/$cs")
        val route = JSONObject(body).getJSONObject("response").getJSONObject("flightroute")
        val origin     = route.optJSONObject("origin")
        val dest       = route.optJSONObject("destination")
        val airlineObj = route.optJSONObject("airline")
        return RouteInfo(
            fromCode = origin?.optString("iata_code") ?: "",
            fromCity = origin?.optString("municipality") ?: "",
            fromLat  = origin?.optString("latitude")?.toDoubleOrNull() ?: 0.0,
            fromLon  = origin?.optString("longitude")?.toDoubleOrNull() ?: 0.0,
            toCode   = dest?.optString("iata_code") ?: "",
            toCity   = dest?.optString("municipality") ?: "",
            toLat    = dest?.optString("latitude")?.toDoubleOrNull() ?: 0.0,
            toLon    = dest?.optString("longitude")?.toDoubleOrNull() ?: 0.0,
            airline  = airlineObj?.optString("name") ?: ""
        )
    } catch (e: Exception) {
        Log.w("AdsbFi", "ADSBDB route lookup failed for $callsign: ${e.message}")
        return null
    }
}

data class AircraftInfo(
    val registration: String,
    val yearBuilt: Int
)

suspend fun fetchAircraftInfo(icao24: String): AircraftInfo? = withContext(Dispatchers.IO) {
    try {
        val body = fetchJson("https://hexdb.io/api/v1/aircraft/${icao24.lowercase()}")
        val json = JSONObject(body)
        val reg = json.optString("Registration", "")
        val yearStr = json.optString("YearBuilt", "")
        val year = yearStr.trim().toIntOrNull() ?: 0
        if (reg.isEmpty() && year == 0) null
        else AircraftInfo(registration = reg, yearBuilt = year)
    } catch (e: Exception) {
        Log.w("AdsbFi", "Aircraft info lookup failed for $icao24: ${e.message}")
        null
    }
}

fun List<Flight>.enrichWithAircraftInfo(cache: Map<String, AircraftInfo?>): List<Flight> = map { flight ->
    val info = cache[flight.id] ?: return@map flight
    flight.copy(
        registration = info.registration.ifEmpty { flight.registration },
        yearBuilt = if (info.yearBuilt > 0) info.yearBuilt else flight.yearBuilt
    )
}

fun List<Flight>.enrich(cache: Map<String, RouteInfo?>): List<Flight> = map { flight ->
    val route = cache[flight.callsign] ?: return@map flight
    val progress = if (route.fromLat != 0.0 && route.toLat != 0.0) {
        val total = haversineDistance(route.fromLat, route.fromLon, route.toLat, route.toLon)
        val done  = haversineDistance(route.fromLat, route.fromLon, flight.lat, flight.lon)
        if (total > 1.0) (done / total).toFloat().coerceIn(0f, 1f) else 0f
    } else 0f
    flight.copy(
        fromCode = route.fromCode,
        fromCity = route.fromCity,
        toCode   = route.toCode,
        toCity   = route.toCity,
        airline  = route.airline.ifEmpty { flight.airline },
        progress = progress
    )
}
