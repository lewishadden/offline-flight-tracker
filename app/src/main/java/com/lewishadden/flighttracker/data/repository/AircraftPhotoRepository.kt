package com.lewishadden.flighttracker.data.repository

import com.lewishadden.flighttracker.data.api.PlanespottersApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class AircraftPhoto(
    val thumbUrl: String?,
    val largeUrl: String?,
    val sourceLink: String?,
    val photographer: String?,
)

/**
 * Pulls aircraft photos from planespotters.net by registration. Caches in
 * memory for the process lifetime so re-opening a flight doesn't re-hit the
 * (free, but rate-limited) public endpoint.
 */
@Singleton
class AircraftPhotoRepository @Inject constructor(
    private val api: PlanespottersApi,
) {
    private val cache = mutableMapOf<String, AircraftPhoto?>()
    private val mutex = Mutex()

    /**
     * Returns null if the registration is unknown to Planespotters or the
     * lookup failed. Caches both hits and misses.
     */
    suspend fun fetchByRegistration(registration: String?): AircraftPhoto? {
        val key = registration?.trim()?.uppercase()?.takeIf { it.isNotBlank() } ?: return null
        mutex.withLock {
            if (cache.containsKey(key)) return cache[key]
        }
        val result = runCatching { api.byRegistration(key) }.getOrNull()
        val first = result?.photos?.firstOrNull()
        val photo = if (first != null) {
            AircraftPhoto(
                thumbUrl = first.thumbnail?.src,
                largeUrl = first.thumbnailLarge?.src ?: first.thumbnail?.src,
                sourceLink = first.link,
                photographer = first.photographer,
            )
        } else null
        mutex.withLock { cache[key] = photo }
        return photo
    }
}
