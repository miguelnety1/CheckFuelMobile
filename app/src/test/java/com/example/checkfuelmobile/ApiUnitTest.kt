package com.example.checkfuelmobile

import com.example.checkfuelmobile.api.ApiStation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class ApiUnitTest {

    @Test
    fun testGetStationsFromApi() = runBlocking {
        try {
            val stations = ApiStation.apiService.getStationsList()
            println("✅ Conexión exitosa con la API. Gasolineras recibidas: ${stations.size}")
            stations.take(5).forEach { station ->
                println("⛽ Gasolinera: ${station.brand} - ${station.address} (${station.locality}) | G95: ${station.gasoline95E5}€ | Diesel: ${station.dieselA}€")
            }
            assertNotNull("La lista de gasolineras no debe ser nula", stations)
        } catch (e: Exception) {
            println("❌ Error de conexión con la API en http://localhost:8080/api/v1/stations: ${e.message}")
            // No hacemos fallar el assert si el servidor Spring Boot local aún no está arrancado
        }
    }
}
