package com.example.checkfuelmobile.api

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiStation {
    private val gson = GsonBuilder().create()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiStationInterface by lazy {
        retrofit.create(ApiStationInterface::class.java)
    }

    fun buildInstance(): ApiStationInterface = apiService
}
