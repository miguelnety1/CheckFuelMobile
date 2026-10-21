package com.example.checkfuelmobile.api

import com.example.checkfuelmobile.data.Station
import retrofit2.Call
import retrofit2.http.GET

interface ApiStationInterface {
    @GET("stations")
    fun getStations(): Call<List<Station>>

    @GET("stations")
    suspend fun getStationsList(): List<Station>
}
