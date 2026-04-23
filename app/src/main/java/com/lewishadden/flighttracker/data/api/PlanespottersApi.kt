package com.lewishadden.flighttracker.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Free, no-key public photos endpoint from planespotters.net.
 * Used to surface a hero image of the actual aircraft assigned to a flight.
 *
 * Docs: https://www.planespotters.net/photo/api
 */
interface PlanespottersApi {

    @GET("pub/photos/reg/{registration}")
    suspend fun byRegistration(@Path("registration") registration: String): PlanespottersResponse

    @GET("pub/photos/hex/{hex}")
    suspend fun byHex(@Path("hex") hex: String): PlanespottersResponse
}

@Serializable
data class PlanespottersResponse(
    val photos: List<PlanespottersPhoto> = emptyList(),
)

@Serializable
data class PlanespottersPhoto(
    val id: String? = null,
    val thumbnail: PlanespottersImage? = null,
    @SerialName("thumbnail_large") val thumbnailLarge: PlanespottersImage? = null,
    val link: String? = null,
    val photographer: String? = null,
)

@Serializable
data class PlanespottersImage(
    val src: String? = null,
    val size: PlanespottersSize? = null,
)

@Serializable
data class PlanespottersSize(
    val width: Int? = null,
    val height: Int? = null,
)
