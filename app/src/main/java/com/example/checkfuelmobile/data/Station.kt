package com.example.checkfuelmobile.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "stations")
data class Station(
    @SerializedName(value = "id", alternate = ["IDESTACION", "idEstacion", "id_estacion"])
    @PrimaryKey val id: String,

    @SerializedName(value = "postalCode", alternate = ["postal_code", "cp", "C.P.", "codigoPostal"])
    val postalCode: String? = null,

    @SerializedName(value = "address", alternate = ["direccion", "Dirección", "streetAddress"])
    val address: String? = null,

    @SerializedName(value = "schedule", alternate = ["horario", "Horario"])
    val schedule: String? = null,

    @SerializedName(value = "latitude", alternate = ["lat", "latitud", "Latitud", "latitudWGS84"])
    val latitude: Double? = null,

    @SerializedName(value = "longitude", alternate = ["lng", "lon", "longitud", "Longitud (WGS84)", "longitudWGS84"])
    val longitude: Double? = null,

    @SerializedName(value = "locality", alternate = ["localidad", "Localidad"])
    val locality: String? = null,

    @SerializedName(value = "municipality", alternate = ["municipio", "Municipio"])
    val municipality: String? = null,

    @SerializedName(value = "province", alternate = ["provincia", "Provincia"])
    val province: String? = null,

    @SerializedName(value = "brand", alternate = ["rotulo", "Rótulo", "marca"])
    val brand: String? = null,

    @SerializedName(value = "gasoline95E5", alternate = ["gasoline_95_e5", "gasolina95E5", "precioGasolina95E5", "precio_gasolina_95_e5", "Precio Gasolina 95 E5"])
    val gasoline95E5: Double? = null,

    @SerializedName(value = "dieselA", alternate = ["diesel_a", "dieselA", "precioDieselA", "precio_diesel_a", "Precio Gasoleo A"])
    val dieselA: Double? = null,

    @Ignore var distanceKm: Double? = null
) {
    // Constructor secundario para desambiguación con Room
    constructor(
        id: String,
        postalCode: String?,
        address: String?,
        schedule: String?,
        latitude: Double?,
        longitude: Double?,
        locality: String?,
        municipality: String?,
        province: String?,
        brand: String?,
        gasoline95E5: Double?,
        dieselA: Double?
    ) : this(
        id, postalCode, address, schedule, latitude, longitude, locality, municipality, province, brand, gasoline95E5, dieselA, null
    )
}
