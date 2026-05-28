package com.example.planespotter.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.planespotter.data.Flight
import com.example.planespotter.data.AircraftLabelMode
import com.example.planespotter.data.RadarPlaneScale
import com.example.planespotter.data.UnitSystem
import com.example.planespotter.data.bearingColor
import com.example.planespotter.data.formatDistanceShort
import com.example.planespotter.data.primaryLabel
import com.example.planespotter.data.relDeg
import com.example.planespotter.ui.theme.*
import kotlin.math.*

@Composable
fun CompassView(
    flights: List<Flight>,
    heading: Float,
    radiusKm: Float,
    focusedId: String?,
    trackedIds: Set<String>,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    labelMode: AircraftLabelMode = AircraftLabelMode.CALLSIGN,
    planeScale: RadarPlaneScale = RadarPlaneScale.NORMAL,
    onFlightTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sky = LocalSkyPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "compass")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.0f, label = "pulse",
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 2.4f, label = "ring",
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearOutSlowInEasing), RepeatMode.Restart)
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f, targetValue = 0f, label = "ringAlpha",
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Restart)
    )

    val textMeasurer = rememberTextMeasurer()
    // Plain mutable list — not Compose state, so updating it inside the Canvas draw
    // doesn't trigger recomposition. The tap handler reads it at gesture time, which
    // is always after the draw that populated it.
    val planePositions = remember { mutableListOf<Pair<Flight, Offset>>() }

    Canvas(
        modifier = modifier
            .pointerInput(flights, radiusKm) {
                detectTapGestures { tap ->
                    planePositions.firstOrNull { (_, pos) ->
                        (tap - pos).getDistance() < 72f
                    }?.let { onFlightTap(it.first.id) }
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = minOf(cx, cy) - 4f
        val ringW  = 16f
        val innerR = outerR - ringW

        // ── Conic gradient ring ───────────────────────────────────
        val ringBrush = Brush.sweepGradient(
            colorStops = arrayOf(
                0.00f to SkyBlue,
                0.25f to SkyPurple,
                0.50f to SkyRed,
                0.75f to SkyPurple,
                1.00f to SkyBlue,
            ),
            center = Offset(cx, cy)
        )
        rotate(-90f - heading, pivot = Offset(cx, cy)) {
            drawArc(
                brush = ringBrush,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = ringW, cap = StrokeCap.Butt),
                topLeft = Offset(cx - outerR + ringW / 2, cy - outerR + ringW / 2),
                size = Size((outerR - ringW / 2) * 2, (outerR - ringW / 2) * 2)
            )
        }

        // ── Outer glow ────────────────────────────────────────────
        drawCircle(
            color = SkyBlue.copy(alpha = 0.06f),
            radius = outerR + 8f, center = Offset(cx, cy),
            style = Stroke(width = 14f)
        )

        // ── Tick marks ────────────────────────────────────────────
        for (i in 0 until 36) {
            val deg = i * 10f
            val a = Math.toRadians((deg - heading - 90).toDouble())
            val major = (i % 9 == 0)
            val r1 = if (major) innerR - 10f else innerR - 5f
            drawLine(
                color = Color.White.copy(alpha = if (major) 0.5f else 0.2f),
                start = Offset(cx + cos(a).toFloat() * r1, cy + sin(a).toFloat() * r1),
                end   = Offset(cx + cos(a).toFloat() * (innerR - 1f), cy + sin(a).toFloat() * (innerR - 1f)),
                strokeWidth = if (major) 1.5f else 0.8f
            )
        }

        // ── Cardinal labels ───────────────────────────────────────
        listOf("N" to SkyBlue, "E" to SkyPurple, "S" to SkyRed, "W" to SkyPurple)
            .forEachIndexed { i, (label, color) ->
                val a = Math.toRadians((i * 90f - heading - 90).toDouble())
                val r = innerR - 24f
                val m = textMeasurer.measure(label, style = TextStyle(color = color, fontSize = 13.sp, fontWeight = FontWeight.W700))
                drawText(m, topLeft = Offset(cx + cos(a).toFloat() * r - m.size.width / 2f,
                                             cy + sin(a).toFloat() * r - m.size.height / 2f))
            }

        // ── Centre dot ────────────────────────────────────────────
        drawCircle(color = Color.White.copy(alpha = 0.12f), radius = 18f, center = Offset(cx, cy))
        drawCircle(color = Color.White, radius = 4f, center = Offset(cx, cy))

        // ── Aircraft ──────────────────────────────────────────────
        // flights already filtered to: within radius + approaching (CPA within 15 min)
        val positions = mutableListOf<Pair<Flight, Offset>>()

        // Clip all aircraft drawing to the inner circle so nothing bleeds into or past the ring
        clipPath(Path().apply {
            addOval(Rect(cx - innerR, cy - innerR, cx + innerR, cy + innerR))
        }) {

        flights.forEach { flight ->
            val rel          = relDeg(flight.bearing, heading)
            val ang          = Math.toRadians((rel - 90.0))
            val isFocused    = flight.id == focusedId
            val isTracked    = trackedIds.contains(flight.id)
            val isApproaching = flight.distanceKm > radiusKm
            val color        = Color(bearingColor(rel).toInt())

            val glyphSize = flight.radarGlyphSize(isFocused, planeScale)

            // Approaching planes pin to the ring edge; in-radius planes map proportionally
            val margin   = glyphSize / 2f + 6f
            val maxR     = innerR - margin
            val fraction = (flight.distanceKm / radiusKm).coerceIn(0.04f, 1f)
            val mappedR  = fraction * maxR

            val px  = cx + cos(ang).toFloat() * mappedR
            val py  = cy + sin(ang).toFloat() * mappedR
            val pos = Offset(px, py)
            positions.add(flight to pos)

            // Tracked radiating ring
            if (isTracked) {
                drawCircle(
                    color = color.copy(alpha = ringAlpha * 0.65f),
                    radius = 18f * ringScale, center = pos,
                    style = Stroke(width = 1.5f)
                )
            }

            // Notable (interesting) static ring
            if (flight.interesting && !isTracked) {
                drawCircle(color = SkyPurple.copy(alpha = 0.55f), radius = 20f, center = pos, style = Stroke(width = 1.5f))
            }

            // Focus glow
            if (isFocused) {
                drawCircle(color = color.copy(alpha = 0.25f * pulse), radius = 28f * pulse, center = pos)
            }

            // Approaching planes are dimmed; in-radius planes are solid
            val glyphAlpha = when {
                isFocused     -> 1f
                isApproaching -> 0.45f
                else          -> 0.92f
            }
            val glyphColor = if (isFocused) Color.White else color.copy(alpha = glyphAlpha)
            if (flight.aircraftCategory == "Helicopter") {
                drawHelicopterGlyph(pos, glyphSize, flight.track - heading, glyphColor)
            } else {
                drawAirplaneGlyph(pos, glyphSize, flight.track - heading, glyphColor, flight)
            }

            // Label: callsign when focused, "~Xmin" for approaching, distance for in-range
            val label = when {
                isFocused                              -> flight.primaryLabel(labelMode)
                isApproaching && flight.overheadMin < 999 -> "~${flight.overheadMin}min"
                isApproaching                          -> formatDistanceShort(flight.distanceKm, unitSystem)
                else                                   -> formatDistanceShort(flight.distanceKm, unitSystem)
            }
            val labelAlpha = if (isApproaching) 0.4f else 0.65f
            val mStyle = TextStyle(
                color = if (isFocused) sky.primaryText else sky.primaryText.copy(alpha = labelAlpha),
                fontSize = 10.sp,
                fontWeight = if (isFocused) FontWeight.W600 else FontWeight.W400
            )
            val m = textMeasurer.measure(label, style = mStyle)
            drawText(m, topLeft = Offset(px - m.size.width / 2f, py + glyphSize / 2f + 6f))
        }

        } // end clipPath

        planePositions.clear()
        planePositions.addAll(positions)
    }
}

private fun Flight.radarGlyphSize(isFocused: Boolean, scale: RadarPlaneScale): Float {
    val type = typeCode.uppercase()
    val modelName = model.uppercase()
    val base = when {
        aircraftCategory == "Helicopter" -> 20f
        type in setOf("A388", "B748", "B744") || modelName.contains("A380") || modelName.contains("747") -> 34f
        type.startsWith("A35") || type.startsWith("B77") || type.startsWith("B78") || type.startsWith("A33") || type.startsWith("A34") -> 30f
        aircraftCategory == "Cargo" -> 29f
        type.startsWith("B7") || type.startsWith("A3") -> 25f
        type.startsWith("E") || type.startsWith("CRJ") || type.startsWith("AT") || type.startsWith("DH8") -> 22f
        aircraftCategory == "Private" -> 20f
        else -> 24f
    }
    val focusBoost = if (isFocused) 1.18f else 1f
    return (base * scale.multiplier * focusBoost).coerceIn(16f, 85f)
}

private fun DrawScope.drawAirplaneGlyph(center: Offset, size: Float, trackDeg: Float, color: Color, flight: Flight) {
    val s   = size / 2f
    val rad = Math.toRadians(trackDeg.toDouble())
    val cosT = cos(rad).toFloat()
    val sinT = sin(rad).toFloat()
    // Pre-rotate each point into canvas space — avoids withTransform inside clipPath,
    // which can misplace glyphs relative to the clip region in some Compose versions.
    fun r(lx: Float, ly: Float) = Offset(
        center.x + lx * cosT - ly * sinT,
        center.y + lx * sinT + ly * cosT
    )

    val isHeavy = size >= 30f
    val wingSpan = if (isHeavy) 1.16f else 1.0f
    val bodyWidth = if (isHeavy) 0.28f else 0.22f
    val tailSpan = if (flight.aircraftCategory == "Private") 0.36f else 0.48f

    val fuselage = Path().apply {
        val p0 = r(0f, -s); val p1 = r(0f, s * 0.55f)
        val c1 = r(s * bodyWidth, -s * 0.4f); val c2 = r(s * bodyWidth,  s * 0.2f)
        val c3 = r(-s * bodyWidth, s * 0.2f); val c4 = r(-s * bodyWidth, -s * 0.4f)
        moveTo(p0.x, p0.y)
        cubicTo(c1.x, c1.y, c2.x, c2.y, p1.x, p1.y)
        cubicTo(c3.x, c3.y, c4.x, c4.y, p0.x, p0.y)
        close()
    }
    drawPath(fuselage, color)

    val wings = Path().apply {
        val pts = arrayOf(
            r(-s * 0.18f, -s * 0.05f), r(-s * wingSpan, s * 0.22f),
            r(-s * 0.65f,  s * 0.32f), r(0f,            s * 0.18f),
            r( s * 0.65f,  s * 0.32f), r(s * wingSpan,  s * 0.22f),
            r( s * 0.18f, -s * 0.05f)
        )
        moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close()
    }
    drawPath(wings, color)

    val tail = Path().apply {
        val pts = arrayOf(
            r(-s * 0.12f, s * 0.35f), r(-s * tailSpan, s * 0.72f),
            r(-s * 0.28f, s * 0.72f), r(0f,         s * 0.5f),
            r( s * 0.28f, s * 0.72f), r( s * tailSpan, s * 0.72f),
            r( s * 0.12f, s * 0.35f)
        )
        moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close()
    }
    drawPath(tail, color)
}

private fun DrawScope.drawHelicopterGlyph(center: Offset, size: Float, trackDeg: Float, color: Color) {
    val s = size / 2f
    val rad = Math.toRadians(trackDeg.toDouble())
    val cosT = cos(rad).toFloat()
    val sinT = sin(rad).toFloat()
    fun r(lx: Float, ly: Float) = Offset(
        center.x + lx * cosT - ly * sinT,
        center.y + lx * sinT + ly * cosT
    )

    val body = Path().apply {
        val nose = r(0f, -s * 0.82f)
        val right = r(s * 0.42f, -s * 0.1f)
        val tail = r(0f, s * 0.62f)
        val left = r(-s * 0.42f, -s * 0.1f)
        moveTo(nose.x, nose.y)
        cubicTo(r(s * 0.36f, -s * 0.68f).x, r(s * 0.36f, -s * 0.68f).y, right.x, right.y, right.x, right.y)
        lineTo(tail.x, tail.y)
        lineTo(left.x, left.y)
        cubicTo(r(-s * 0.36f, -s * 0.68f).x, r(-s * 0.36f, -s * 0.68f).y, nose.x, nose.y, nose.x, nose.y)
        close()
    }
    drawPath(body, color)
    drawLine(color, start = r(-s * 1.05f, -s * 0.18f), end = r(s * 1.05f, -s * 0.18f), strokeWidth = maxOf(2f, size * 0.08f), cap = StrokeCap.Round)
    drawLine(color, start = r(0f, s * 0.55f), end = r(0f, s * 1.15f), strokeWidth = maxOf(1.5f, size * 0.06f), cap = StrokeCap.Round)
    drawLine(color, start = r(-s * 0.28f, s * 1.15f), end = r(s * 0.28f, s * 1.15f), strokeWidth = maxOf(1.5f, size * 0.06f), cap = StrokeCap.Round)
}
