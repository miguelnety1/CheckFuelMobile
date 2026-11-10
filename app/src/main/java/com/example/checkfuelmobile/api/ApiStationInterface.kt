package com.example.checkfuelmobile.api

import com.example.checkfuelmobile.data.Station
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiStationInterface {
    @GET("stations")
    fun getStations(): Call<List<Station>>

    // Petición estándar limpia sin parámetros URL para obtener todas las gasolineras
    @GET("stations")
    suspend fun getStationsList(): List<Station>

    // Petición opcional por cercanía si el servidor soporta parámetros espaciales
    @GET("stations")
    suspend fun getStationsNearby(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radiusKm: Double
    ): List<Station>
}
