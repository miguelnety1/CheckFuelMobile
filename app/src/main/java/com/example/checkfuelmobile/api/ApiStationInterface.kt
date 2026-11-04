package com.example.checkfuelmobile.api

import com.example.checkfuelmobile.data.Station
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiStationInterface {
    @GET("stations")
    fun getStations(): Call<List<Station>>

    @GET("stations")
    suspend fun getStationsList(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radius") radiusKm: Double? = null
    ): List<Station>
}
