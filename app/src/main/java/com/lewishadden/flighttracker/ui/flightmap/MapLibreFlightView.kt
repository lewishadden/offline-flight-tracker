package com.lewishadden.flighttracker.ui.flightmap

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.lewishadden.flighttracker.di.MapStyleUrl
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
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
                    map.setStyle(styleUrl) {
                        renderState.styleLoaded = true
                        applyState(map, state, renderState)
                    }
                } else {
                    applyState(map, state, renderState)
                }
            }
        }
    )
}

private fun applyState(map: MapLibreMap, state: FlightMapUiState, rs: MapRenderState) {
    val path = state.densifiedPath
    val pathKey = path.size

    if (path.size >= 2 && rs.renderedPathKey != pathKey) {
        map.clear()
        rs.liveMarker = null

        val line = PolylineOptions()
            .color(0xFF1E88E5.toInt())
            .width(3f)
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
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        rs.boundsFitForKey = pathKey
    }

    val loc = state.location
    if (loc != null) {
        val here = LatLng(loc.latitude, loc.longitude)
        val existing = rs.liveMarker
        if (existing == null) {
            rs.liveMarker = map.addMarker(MarkerOptions().position(here).title("You"))
        } else {
            existing.position = here
        }
    } else {
        rs.liveMarker?.let { map.removeMarker(it) }
        rs.liveMarker = null
    }
}
