package com.lewishadden.flighttracker.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.lewishadden.flighttracker.BuildConfig
import com.lewishadden.flighttracker.data.api.AeroApiService
import com.lewishadden.flighttracker.data.api.PlanespottersApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class AeroRetrofit
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class PlanespottersRetrofit
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class AeroOkHttp
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class PlainOkHttp

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
        else HttpLoggingInterceptor.Level.NONE
    }

    @Provides
    @Singleton
    @AeroOkHttp
    fun aeroOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("x-apikey", BuildConfig.AERO_API_KEY)
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(loggingInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    /** No auth, no extra headers — used for public endpoints like Planespotters. */
    @Provides
    @Singleton
    @PlainOkHttp
    fun plainOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(loggingInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @AeroRetrofit
    fun aeroRetrofit(@AeroOkHttp client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.AERO_API_BASE)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @PlanespottersRetrofit
    fun planespottersRetrofit(@PlainOkHttp client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.planespotters.net/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun aeroApi(@AeroRetrofit retrofit: Retrofit): AeroApiService =
        retrofit.create(AeroApiService::class.java)

    @Provides
    @Singleton
    fun planespottersApi(@PlanespottersRetrofit retrofit: Retrofit): PlanespottersApi =
        retrofit.create(PlanespottersApi::class.java)
}
