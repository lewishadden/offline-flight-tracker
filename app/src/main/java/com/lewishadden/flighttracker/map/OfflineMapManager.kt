package com.lewishadden.flighttracker.map

import android.content.Context
import com.lewishadden.flighttracker.data.db.FlightDao
import com.lewishadden.flighttracker.data.db.entities.OfflineRegionEntity
import com.lewishadden.flighttracker.domain.model.RouteFix
import com.lewishadden.flighttracker.util.BBox
import com.lewishadden.flighttracker.util.Geo
import com.lewishadden.flighttracker.util.LatLon
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Wraps MapLibre [OfflineManager] for per-flight map caching.
 *
 * For a given flight, we densify the AeroAPI route along great-circle arcs,
 * compute a padded corridor bbox around the path, and register a MapLibre
 * offline region at zoom 3..9. Tiles are stored in MapLibre's internal SQLite
 * DB under app storage — fully available offline with no network.
 */
@Singleton
class OfflineMapManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: FlightDao,
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    data class Progress(
        val completedTiles: Long,
        val requiredTiles: Long,
        val completedBytes: Long,
        val percent: Float,
        val done: Boolean,
        val error: String? = null,
    )

    fun downloadForFlight(
        faFlightId: String,
        styleUrl: String,
        routeFixes: List<RouteFix>,
        originLat: Double?,
        originLon: Double?,
        destLat: Double?,
        destLon: Double?,
        pixelRatio: Float,
    ): Flow<Progress> = callbackFlow {
        val pathPoints = buildList {
            if (originLat != null && originLon != null) add(LatLon(originLat, originLon))
            addAll(routeFixes.map { LatLon(it.lat, it.lon) })
            if (destLat != null && destLon != null) add(LatLon(destLat, destLon))
        }
        if (pathPoints.size < 2) {
            val reason = buildString {
                append("Not enough waypoints for offline region. ")
                append("origin=${if (originLat != null && originLon != null) "ok" else "missing"}, ")
                append("dest=${if (destLat != null && destLon != null) "ok" else "missing"}, ")
                append("routeFixes=${routeFixes.size}. ")
                append("Try refreshing the flight once it has a filed route, ")
                append("or check that AeroAPI returned ICAO codes for origin/destination.")
            }
            trySend(Progress(0, 0, 0, 0f, true, reason))
            close()
            return@callbackFlow
        }

        val densified = Geo.densifyGreatCircle(pathPoints, maxSegmentKm = 50.0)
        val corridor = Geo.bboxAround(densified, paddingKm = 80.0)
        val minZoom = 3.0
        val maxZoom = 9.0

        val manager = OfflineManager.getInstance(context)
        manager.setMaximumAmbientCacheSize(250L * 1024 * 1024, null)

        val definition = corridor.toDefinition(styleUrl, pixelRatio, minZoom, maxZoom)
        val metadata = "flight:$faFlightId".toByteArray()

        manager.createOfflineRegion(definition, metadata, object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(region: OfflineRegion) {
                region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                region.setObserver(object : OfflineRegion.OfflineRegionObserver {
                    override fun onStatusChanged(status: OfflineRegionStatus) {
                        val req = status.requiredResourceCount
                        val done = status.completedResourceCount
                        val pct = if (req > 0) (done.toFloat() / req.toFloat()).coerceIn(0f, 1f) else 0f
                        val complete = status.isComplete
                        trySend(
                            Progress(
                                completedTiles = done,
                                requiredTiles = req,
                                completedBytes = status.completedResourceSize,
                                percent = pct,
                                done = complete,
                                error = null,
                            )
                        )
                        if (complete) {
                            region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                            scope.launch {
                                dao.upsertOfflineRegion(
                                    OfflineRegionEntity(
                                        faFlightId = faFlightId,
                                        maplibreRegionId = region.id,
                                        minLat = corridor.minLat,
                                        minLon = corridor.minLon,
                                        maxLat = corridor.maxLat,
                                        maxLon = corridor.maxLon,
                                        minZoom = minZoom,
                                        maxZoom = maxZoom,
                                        sizeBytes = status.completedResourceSize,
                                        completedEpochMs = System.currentTimeMillis(),
                                    )
                                )
                            }
                            close()
                        }
                    }

                    override fun onError(error: OfflineRegionError) {
                        trySend(Progress(0, 0, 0, 0f, true, "${error.reason}: ${error.message}"))
                        close()
                    }

                    override fun mapboxTileCountLimitExceeded(limit: Long) {
                        trySend(Progress(0, 0, 0, 0f, true, "Tile count limit exceeded: $limit"))
                        close()
                    }
                })
            }

            override fun onError(error: String) {
                trySend(Progress(0, 0, 0, 0f, true, error))
                close()
            }
        })

        awaitClose { }
    }

    suspend fun deleteFlightRegion(faFlightId: String) {
        val entity = dao.getOfflineRegion(faFlightId) ?: return
        val manager = OfflineManager.getInstance(context)
        val regions = listOfflineRegions(manager)
        val target = regions.firstOrNull { it.id == entity.maplibreRegionId }
        if (target != null) {
            suspendCancellableCoroutine<Unit> { cont ->
                target.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                    override fun onDelete() = cont.resume(Unit)
                    override fun onError(error: String) = cont.resume(Unit)
                })
            }
        }
        dao.deleteOfflineRegion(faFlightId)
    }

    private suspend fun listOfflineRegions(manager: OfflineManager): Array<OfflineRegion> =
        suspendCancellableCoroutine { cont ->
            manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    cont.resume(offlineRegions ?: emptyArray())
                }
                override fun onError(error: String) = cont.resume(emptyArray())
            })
        }

    private fun BBox.toDefinition(
        styleUrl: String,
        pixelRatio: Float,
        minZoom: Double,
        maxZoom: Double,
    ): OfflineTilePyramidRegionDefinition {
        val bounds = LatLngBounds.Builder()
            .include(LatLng(minLat, minLon))
            .include(LatLng(maxLat, maxLon))
            .build()
        return OfflineTilePyramidRegionDefinition(styleUrl, bounds, minZoom, maxZoom, pixelRatio)
    }
}
