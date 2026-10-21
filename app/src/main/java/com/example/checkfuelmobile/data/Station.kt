package com.example.checkfuelmobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class Station(
    @PrimaryKey val id: String,
    val postalCode: String? = null,
    val address: String? = null,
    val schedule: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locality: String? = null,
    val municipality: String? = null,
    val province: String? = null,
    val brand: String? = null,
    val gasoline95E5: Double? = null,
    val dieselA: Double? = null
)
