package com.lewishadden.flighttracker.util

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLon(val lat: Double, val lon: Double)

data class BBox(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
) {
    val spansAntimeridian: Boolean get() = minLon > maxLon
    val centerLat: Double get() = (minLat + maxLat) / 2.0
    val centerLon: Double
        get() = if (spansAntimeridian) {
            val avg = (minLon + maxLon + 360.0) / 2.0
            if (avg > 180.0) avg - 360.0 else avg
        } else (minLon + maxLon) / 2.0
}

object Geo {

    private const val EARTH_RADIUS_KM = 6371.0088

    /**
     * Initial great-circle bearing from [a] to [b], in degrees clockwise from
     * true north, normalized to [0, 360). Used to orient the in-flight plane
     * marker along the current route segment.
     */
    fun bearingDeg(a: LatLon, b: LatLon): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val brg = Math.toDegrees(atan2(y, x))
        return (brg + 360.0) % 360.0
    }

    fun haversineKm(a: LatLon, b: LatLon): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val h = sin(dLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
        val c = 2 * atan2(sqrt(h), sqrt(1 - h))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Densifies a polyline by interpolating additional points along great-circle
     * arcs between each pair so that no segment is longer than [maxSegmentKm].
     * Used to avoid straight-line bounds in Mercator that would cut off
     * high-latitude route curvature.
     */
    fun densifyGreatCircle(points: List<LatLon>, maxSegmentKm: Double = 50.0): List<LatLon> {
        if (points.size < 2) return points
        val out = mutableListOf<LatLon>()
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            out += p1
            val dist = haversineKm(p1, p2)
            if (dist <= maxSegmentKm) continue
            val steps = (dist / maxSegmentKm).toInt().coerceAtLeast(1)
            val lat1 = Math.toRadians(p1.lat); val lon1 = Math.toRadians(p1.lon)
            val lat2 = Math.toRadians(p2.lat); val lon2 = Math.toRadians(p2.lon)
            val d = 2 * Math.asin(
                sqrt(
                    sin((lat2 - lat1) / 2).let { it * it } +
                        cos(lat1) * cos(lat2) * sin((lon2 - lon1) / 2).let { it * it }
                )
            )
            if (d == 0.0) continue
            for (k in 1 until steps) {
                val f = k.toDouble() / steps
                val a = sin((1 - f) * d) / sin(d)
                val b = sin(f * d) / sin(d)
                val x = a * cos(lat1) * cos(lon1) + b * cos(lat2) * cos(lon2)
                val y = a * cos(lat1) * sin(lon1) + b * cos(lat2) * sin(lon2)
                val z = a * sin(lat1) + b * sin(lat2)
                val iLat = atan2(z, sqrt(x * x + y * y))
                val iLon = atan2(y, x)
                out += LatLon(Math.toDegrees(iLat), Math.toDegrees(iLon))
            }
        }
        out += points.last()
        return out
    }

    /**
     * Bounding box around [points] padded by [paddingKm] in each direction.
     * Does not handle antimeridian crossings specially — flights that cross it
     * will get clamped to world bounds, which is acceptable for offline tile
     * download (tiles covering the Pacific are cheap).
     */
    fun bboxAround(points: List<LatLon>, paddingKm: Double = 80.0): BBox {
        require(points.isNotEmpty())
        var minLat = 90.0; var maxLat = -90.0
        var minLon = 180.0; var maxLon = -180.0
        for (p in points) {
            minLat = min(minLat, p.lat); maxLat = max(maxLat, p.lat)
            minLon = min(minLon, p.lon); maxLon = max(maxLon, p.lon)
        }
        val latPad = paddingKm / 111.0
        val meanLat = Math.toRadians((minLat + maxLat) / 2.0)
        val lonPad = paddingKm / (111.0 * max(0.05, cos(meanLat)))
        return BBox(
            minLat = (minLat - latPad).coerceAtLeast(-85.0),
            minLon = (minLon - lonPad).coerceAtLeast(-180.0),
            maxLat = (maxLat + latPad).coerceAtMost(85.0),
            maxLon = (maxLon + lonPad).coerceAtMost(180.0),
        )
    }

    /**
     * Closest point on a polyline (index + param t along segment, plus distance km).
     * Used to project the user's GPS position onto the cached route for
     * "progress along route" displays when offline.
     */
    fun nearestPointOnPolyline(polyline: List<LatLon>, p: LatLon): NearestResult? {
        if (polyline.size < 2) return null
        var bestIdx = 0
        var bestT = 0.0
        var bestDistKm = Double.MAX_VALUE
        var bestProj = polyline.first()
        for (i in 0 until polyline.size - 1) {
            val a = polyline[i]; val b = polyline[i + 1]
            val res = projectOnSegment(a, b, p)
            if (res.distKm < bestDistKm) {
                bestDistKm = res.distKm
                bestIdx = i
                bestT = res.t
                bestProj = res.projected
            }
        }
        return NearestResult(bestIdx, bestT, bestDistKm, bestProj)
    }

    private fun projectOnSegment(a: LatLon, b: LatLon, p: LatLon): SegProj {
        val ax = a.lon; val ay = a.lat
        val bx = b.lon; val by = b.lat
        val px = p.lon; val py = p.lat
        val dx = bx - ax; val dy = by - ay
        val lenSq = dx * dx + dy * dy
        val t = if (lenSq == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / lenSq).coerceIn(0.0, 1.0)
        val proj = LatLon(ay + t * dy, ax + t * dx)
        return SegProj(t, haversineKm(proj, p), proj)
    }

    private data class SegProj(val t: Double, val distKm: Double, val projected: LatLon)

    data class NearestResult(
        val segmentIndex: Int,
        val segmentT: Double,
        val crossTrackKm: Double,
        val projected: LatLon,
    )
}
