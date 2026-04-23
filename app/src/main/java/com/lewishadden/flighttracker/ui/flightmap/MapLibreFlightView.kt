package com.lewishadden.flighttracker.ui.flightmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.lewishadden.flighttracker.R
import com.lewishadden.flighttracker.di.MapStyleUrl
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MapStyleEntryPoint {
    @MapStyleUrl fun styleUrl(): String
}

private class MapRenderState {
    var styleLoaded: Boolean = false
    var renderedPathKey: Int = 0
    var boundsFitForKey: Int = -1
    var liveMarker: Marker? = null
    var liveMarkerMode: MarkerMode = MarkerMode.NONE
}

private enum class MarkerMode { NONE, GPS, PLANE }

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
    val planeIcon = remember { vectorIcon(context, R.drawable.ic_plane_marker) }
    val gpsIcon = remember { vectorIcon(context, R.drawable.ic_gps_dot) }

    DisposableEffect(Unit) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView },
        update = { view ->
            view.getMapAsync { map ->
                if (!renderState.styleLoaded) {
                    // Suppress the red north-pointer compass that fades in when the
                    // map is rotated — it reads as a visual "flash" on the dark theme.
                    map.uiSettings.isCompassEnabled = false
                    map.uiSettings.isAttributionEnabled = false
                    map.uiSettings.isLogoEnabled = false
                    map.setStyle(styleUrl) {
                        renderState.styleLoaded = true
                        applyState(map, state, renderState, planeIcon, gpsIcon)
                    }
                } else {
                    applyState(map, state, renderState, planeIcon, gpsIcon)
                }
            }
        }
    )
}

private fun applyState(
    map: MapLibreMap,
    state: FlightMapUiState,
    rs: MapRenderState,
    planeIcon: org.maplibre.android.annotations.Icon,
    gpsIcon: org.maplibre.android.annotations.Icon,
) {
    val path = state.densifiedPath
    val pathKey = path.size

    if (path.size >= 2 && rs.renderedPathKey != pathKey) {
        map.clear()
        rs.liveMarker = null
        rs.liveMarkerMode = MarkerMode.NONE

        val line = PolylineOptions()
            .color(0xFF55B9F7.toInt())
            .width(4f)
        path.forEach { line.add(LatLng(it.lat, it.lon)) }
        map.addPolyline(line)

        val first = path.first()
        val last = path.last()
        map.addMarker(MarkerOptions().position(LatLng(first.lat, first.lon)).title("Origin"))
        map.addMarker(MarkerOptions().position(LatLng(last.lat, last.lon)).title("Destination"))

        rs.renderedPathKey = pathKey
    }

    if (path.size >= 2 && rs.boundsFitForKey != pathKey) {
        val bounds = LatLngBounds.Builder().apply {
            path.forEach { include(LatLng(it.lat, it.lon)) }
        }.build()
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        rs.boundsFitForKey = pathKey
    }

    val loc = state.location
    if (loc != null) {
        val here = LatLng(loc.latitude, loc.longitude)
        val airborne = isAirborne(state)
        val desiredMode = if (airborne) MarkerMode.PLANE else MarkerMode.GPS
        val existing = rs.liveMarker
        if (existing == null || rs.liveMarkerMode != desiredMode) {
            existing?.let { map.removeMarker(it) }
            val options = MarkerOptions()
                .position(here)
                .title(if (airborne) "In flight" else "You")
                .icon(if (airborne) planeIcon else gpsIcon)
            rs.liveMarker = map.addMarker(options)
            rs.liveMarkerMode = desiredMode
        } else {
            existing.position = here
        }
    } else {
        rs.liveMarker?.let { map.removeMarker(it) }
        rs.liveMarker = null
        rs.liveMarkerMode = MarkerMode.NONE
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

private fun vectorIcon(context: Context, @DrawableRes resId: Int): org.maplibre.android.annotations.Icon {
    val drawable = checkNotNull(ContextCompat.getDrawable(context, resId))
    val w = drawable.intrinsicWidth.coerceAtLeast(1)
    val h = drawable.intrinsicHeight.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return IconFactory.getInstance(context).fromBitmap(bmp)
}
