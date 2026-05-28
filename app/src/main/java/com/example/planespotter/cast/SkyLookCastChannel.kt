package com.example.planespotter.cast

import com.example.planespotter.data.Flight
import com.example.planespotter.ui.cast.CastState
import com.google.android.gms.cast.framework.CastSession
import org.json.JSONArray
import org.json.JSONObject

object SkyLookCastChannel {

    fun send(session: CastSession, state: CastState) {
        if (!session.isConnected) return
        try {
            session.sendMessage(CAST_NAMESPACE, buildJson(state))
        } catch (_: Exception) { }
    }

    private fun buildJson(state: CastState): String {
        val flightsArr = JSONArray()
        state.flights.forEach { f ->
            flightsArr.put(JSONObject().apply {
                put("id", f.id)
                put("callsign", f.callsign)
                put("airline", f.airline)
                put("model", f.model)
                put("bearing", f.bearing.toDouble())
                put("distanceKm", f.distanceKm.toDouble())
                put("altitudeFt", f.altitudeFt)
                put("speedMph", f.speedMph)
                put("overheadMin", f.overheadMin)
                put("fromCode", f.fromCode)
                put("fromCity", f.fromCity)
                put("toCode", f.toCode)
                put("toCity", f.toCity)
                put("progress", f.progress.toDouble())
                put("interesting", f.interesting)
                put("status", f.status)
                put("airlineIataCode", f.airlineIataCode)
            })
        }
        val trackedArr = JSONArray().also { arr -> state.trackedIds.forEach { arr.put(it) } }

        return JSONObject().apply {
            put("flights", flightsArr)
            put("trackedIds", trackedArr)
            put("focusedFlightId", state.focusedFlight?.id ?: JSONObject.NULL)
            put("unitSystem", state.displaySettings.unitSystem.name)
            put("lastUpdated", state.lastUpdated ?: JSONObject.NULL)
        }.toString()
    }
}
