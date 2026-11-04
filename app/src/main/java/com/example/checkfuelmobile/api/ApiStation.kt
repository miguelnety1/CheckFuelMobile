package com.example.checkfuelmobile.api

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Safe Gson TypeAdapter for Double that handles numbers, strings with commas ("1,659"), and nulls */
private class SafeDoubleTypeAdapter : TypeAdapter<Double?>() {
    override fun write(out: JsonWriter, value: Double?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value)
        }
    }

    override fun read(reader: JsonReader): Double? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return try {
            if (reader.peek() == JsonToken.STRING) {
                val str = reader.nextString().trim()
                    .replace(",", ".")
                    .replace("[^0-9.]".toRegex(), "")
                if (str.isEmpty()) null else str.toDoubleOrNull()
            } else if (reader.peek() == JsonToken.NUMBER) {
                reader.nextDouble()
            } else {
                reader.skipValue()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

object ApiStation {
    private val gson = GsonBuilder()
        .registerTypeAdapter(Double::class.java, SafeDoubleTypeAdapter())
        .registerTypeAdapter(Double::class.javaObjectType, SafeDoubleTypeAdapter())
        .create()

    // Configuración de OkHttpClient: readTimeout ampliado a 120s para descargar payloads pesados (~12.000 gasolineras / ~15MB JSON)
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiStationInterface by lazy {
        retrofit.create(ApiStationInterface::class.java)
    }

    fun buildInstance(): ApiStationInterface = apiService
}
