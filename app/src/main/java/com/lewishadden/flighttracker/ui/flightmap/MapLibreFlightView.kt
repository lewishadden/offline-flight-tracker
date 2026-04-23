package com.lewishadden.flighttracker.ui.flightmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.lewishadden.flighttracker.R
import com.lewishadden.flighttracker.di.MapStyleUrl
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.Circle
import org.maplibre.android.plugins.annotation.CircleManager
import org.maplibre.android.plugins.annotation.CircleOptions
import org.maplibre.android.plugins.annotation.Line
import org.maplibre.android.plugins.annotation.LineManager
import org.maplibre.android.plugins.annotation.LineOptions
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MapStyleEntryPoint {
    @MapStyleUrl fun styleUrl(): String
}

private const val ICON_GPS = "flighttracker.gps"

private class MapRenderState {
    var styleLoaded: Boolean = false
    var lineManager: LineManager? = null
    var circleManager: CircleManager? = null
    var symbolManager: SymbolManager? = null

    var pathLine: Line? = null
    var originDot: Circle? = null
    var destDot: Circle? = null
    var renderedPathKey: Int = 0
    var boundsFitForKey: Int = -1

    var gpsSymbol: Symbol? = null
    var lastGpsLat: Double = Double.NaN
    var lastGpsLon: Double = Double.NaN
}

@Composable
fun MapLibreFlightView(state: FlightMapUiState) {
    val context = LocalContext.current
    val styleUrl = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext, MapStyleEntryPoint::class.java
        ).styleUrl()
    }

    val mapView = remember { MapView(context) }
    val renderState = remember { MapRenderState() }
    val gpsBitmap = remember { vectorBitmap(context, R.drawable.ic_gps_dot) }

    // Map ref + camera tick — used by the plane overlay below to re-project
    // the world position to screen pixels on every camera frame.
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    val cameraTick = remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            renderState.lineManager?.onDestroy()
            renderState.circleManager?.onDestroy()
            renderState.symbolManager?.onDestroy()
            renderState.lineManager = null
            renderState.circleManager = null
            renderState.symbolManager = null
            mapRef = null
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { view ->
                view.getMapAsync { map ->
                    if (!renderState.styleLoaded) {
                        map.uiSettings.isCompassEnabled = true
                        map.uiSettings.isAttributionEnabled = false
                        map.uiSettings.isLogoEnabled = false
                        map.setStyle(styleUrl) { style ->
                            style.addImage(ICON_GPS, gpsBitmap)
                            renderState.lineManager = LineManager(view, map, style)
                            renderState.circleManager = CircleManager(view, map, style)
                            renderState.symbolManager = SymbolManager(view, map, style).apply {
                                iconAllowOverlap = true
                                iconIgnorePlacement = true
                            }
                            renderState.styleLoaded = true
                            // Tick the camera state on every frame of any move
                            // (pan/zoom/rotate). The plane overlay re-projects
                            // its lat/lng to screen px each composition, so it
                            // tracks the map perfectly during gestures.
                            map.addOnCameraMoveListener {
                                cameraTick.intValue = cameraTick.intValue + 1
                            }
                            mapRef = map
                            applyState(map, state, renderState)
                        }
                    } else {
                        applyState(map, state, renderState)
                    }
                }
            }
        )

        // Plane overlay — rendered as a Compose Image, NOT a MapLibre symbol.
        // This bypasses the SymbolManager entirely for the airborne marker so
        // there's no GL feature re-upload during rotation gestures and no
        // symbol-layer rotation/clipping artifacts to flash through.
        val map = mapRef
        val loc = state.location
        val heading = state.headingDeg
        if (map != null && loc != null && isAirborne(state) && heading != null) {
            // Read the tick to register a recomposition dependency on each
            // camera move — the value itself is unused.
            @Suppress("UNUSED_VARIABLE")
            val tick = cameraTick.intValue
            val screen = remember(loc.latitude, loc.longitude, tick) {
                map.projection.toScreenLocation(LatLng(loc.latitude, loc.longitude))
            }
            val mapBearing = remember(tick) { map.cameraPosition.bearing.toFloat() }
            val planeRotation = heading - mapBearing
            val density = LocalDensity.current
            val sizeDp = 56.dp
            val sizePx = with(density) { sizeDp.toPx() }
            val halfPx = (sizePx / 2f).toInt()

            Box(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(
                                IntOffset(
                                    screen.x.toInt() - halfPx,
                                    screen.y.toInt() - halfPx,
                                )
                            )
                        }
                    }
                    .size(sizeDp)
                    .rotate(planeRotation)
            ) {
                Image(
                    bitmap = remember { vectorBitmap(context, R.drawable.ic_plane_marker).asImageBitmap() },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun applyState(
    map: MapLibreMap,
    state: FlightMapUiState,
    rs: MapRenderState,
) {
    val lineManager = rs.lineManager ?: return
    val circleManager = rs.circleManager ?: return
    val symbolManager = rs.symbolManager ?: return

    val path = state.densifiedPath
    val pathKey = path.size

    if (path.size >= 2 && rs.renderedPathKey != pathKey) {
        rs.pathLine?.let { lineManager.delete(it) }
        rs.originDot?.let { circleManager.delete(it) }
        rs.destDot?.let { circleManager.delete(it) }

        val latLngs = path.map { LatLng(it.lat, it.lon) }
        rs.pathLine = lineManager.create(
            LineOptions()
                .withLatLngs(latLngs)
                .withLineColor("#55B9F7")
                .withLineWidth(3.5f)
                .withLineOpacity(0.95f)
        )

        val first = latLngs.first()
        val last = latLngs.last()
        rs.originDot = circleManager.create(
            CircleOptions()
                .withLatLng(first)
                .withCircleRadius(6f)
                .withCircleColor("#FFB86B")
                .withCircleStrokeColor("#0B1328")
                .withCircleStrokeWidth(2f)
        )
        // Diversion-aware: paint the destination dot rose-red and beef up the
        // stroke so it visually stands out as the divert-to airport.
        val diverted = state.flight?.diverted == true
        val destColor = if (diverted) "#FF6B7A" else "#55B9F7"
        val destStrokeWidth = if (diverted) 3f else 2f
        rs.destDot = circleManager.create(
            CircleOptions()
                .withLatLng(last)
                .withCircleRadius(if (diverted) 7f else 6f)
                .withCircleColor(destColor)
                .withCircleStrokeColor("#0B1328")
                .withCircleStrokeWidth(destStrokeWidth)
        )

        rs.renderedPathKey = pathKey
    }

    if (path.size >= 2 && rs.boundsFitForKey != pathKey) {
        val bounds = org.maplibre.android.geometry.LatLngBounds.Builder().apply {
            path.forEach { include(LatLng(it.lat, it.lon)) }
        }.build()
        map.moveCamera(
            org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(bounds, 120)
        )
        rs.boundsFitForKey = pathKey
    }

    val loc = state.location
    val airborne = isAirborne(state)
    // GPS dot is only used for ground/landed states. The airborne plane is
    // rendered above as a Compose overlay.
    if (loc != null && !airborne) {
        val here = LatLng(loc.latitude, loc.longitude)
        val existing = rs.gpsSymbol
        if (existing == null) {
            rs.gpsSymbol = symbolManager.create(
                SymbolOptions()
                    .withLatLng(here)
                    .withIconImage(ICON_GPS)
                    .withIconSize(1f)
                    .withIconAnchor("center")
            )
            rs.lastGpsLat = loc.latitude
            rs.lastGpsLon = loc.longitude
        } else {
            val moved = rs.lastGpsLat != loc.latitude || rs.lastGpsLon != loc.longitude
            if (moved) {
                existing.latLng = here
                symbolManager.update(existing)
                rs.lastGpsLat = loc.latitude
                rs.lastGpsLon = loc.longitude
            }
        }
    } else {
        rs.gpsSymbol?.let { symbolManager.delete(it) }
        rs.gpsSymbol = null
        rs.lastGpsLat = Double.NaN
        rs.lastGpsLon = Double.NaN
    }
}

/**
 * Airborne = we've left the origin gate but haven't arrived.
 * Prefer wheels-up/wheels-down for accuracy; fall back to out/in for taxiing edge cases.
 */
private fun isAirborne(state: FlightMapUiState): Boolean {
    val f = state.flight ?: return false
    if (f.cancelled) return false
    val departed = f.actualOff != null || f.actualOut != null
    val arrived = f.actualOn != null || f.actualIn != null
    return departed && !arrived
}

private fun vectorBitmap(context: Context, @DrawableRes resId: Int): Bitmap {
    val drawable = checkNotNull(ContextCompat.getDrawable(context, resId))
    val w = drawable.intrinsicWidth.coerceAtLeast(1)
    val h = drawable.intrinsicHeight.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bmp
}
