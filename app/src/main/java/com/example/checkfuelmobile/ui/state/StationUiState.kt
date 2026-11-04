package com.example.checkfuelmobile.ui.state

import com.example.checkfuelmobile.data.Station

/** Opciones de tipo de combustible para filtrado */
enum class FuelFilter(val label: String) {
    ALL("Todos"),
    GASOLINE_95("Gasolina 95"),
    DIESEL("Diésel A")
}

/** Opciones de ordenación para el listado */
enum class SortOption(val label: String) {
    DISTANCE_ASC("Más cercanas GPS"),
    PRICE_G95_ASC("Precio G95 (Menor a Mayor)"),
    PRICE_DIESEL_ASC("Precio Diésel (Menor a Mayor)"),
    BRAND_ASC("Nombre / Marca (A-Z)")
}

/** Estado de la UI para la pantalla principal */
sealed interface StationUiState {
    object Loading : StationUiState
    data class Success(val stations: List<Station>) : StationUiState
    data class Error(val message: String) : StationUiState
}
