package com.example.checkfuelmobile.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.checkfuelmobile.api.ApiConfig
import com.example.checkfuelmobile.api.ApiStation
import com.example.checkfuelmobile.data.Station
import com.example.checkfuelmobile.ui.state.FuelFilter
import com.example.checkfuelmobile.ui.state.SortOption
import com.example.checkfuelmobile.ui.state.StationUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * ViewModel optimizado con ejecuciones asíncronas en Dispatchers.IO para evitar congelamientos en el hilo principal.
 */
class StationViewModel : ViewModel() {

    // Lista original obtenida desde la API
    private val rawStations = MutableStateFlow<List<Station>>(emptyList())

    // Ubicación del usuario (Latitud, Longitud)
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    // Estado principal de la petición de red
    private val _uiState = MutableStateFlow<StationUiState>(StationUiState.Loading)
    val uiState: StateFlow<StationUiState> = _uiState.asStateFlow()

    // Búsqueda y Filtros
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _fuelFilter = MutableStateFlow(FuelFilter.ALL)
    val fuelFilter: StateFlow<FuelFilter> = _fuelFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DISTANCE_ASC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Gasolinera actualmente seleccionada
    private val _selectedStation = MutableStateFlow<Station?>(null)
    val selectedStation: StateFlow<Station?> = _selectedStation.asStateFlow()

    init {
        fetchStations()
    }

    /** Realiza la petición HTTP a la API. Si hay coordenadas y radio válidos usa la consulta cercana, de lo contrario la estándar */
    fun fetchStations(
        lat: Double? = _userLocation.value?.first,
        lng: Double? = _userLocation.value?.second,
        radiusKm: Double? = null
    ) {
        _uiState.value = StationUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Si disponemos de lat, lng y radio llamamos a getStationsNearby; si no, hacemos la llamada limpia getStationsList()
                val list = if (lat != null && lng != null && radiusKm != null) {
                    try {
                        ApiStation.apiService.getStationsNearby(lat, lng, radiusKm)
                    } catch (e: Exception) {
                        // Fallback a lista completa si el backend no soporta getStationsNearby
                        ApiStation.apiService.getStationsList()
                    }
                } else {
                    ApiStation.apiService.getStationsList()
                }
                rawStations.value = list
                _uiState.value = StationUiState.Success(list)
            } catch (e: Exception) {
                val errorMsg = when {
                    e is ConnectException || e is SocketTimeoutException || e.message?.contains("Failed to connect") == true ->
                        "No se pudo conectar con la API (${ApiConfig.BASE_URL}stations). Asegúrese de que el servidor esté activo y ejecutable."
                    else -> e.message ?: "Error al conectar con el servidor de gasolineras."
                }
                _uiState.value = StationUiState.Error(errorMsg)
            }
        }
    }

    /** Actualiza las coordenadas GPS del usuario y recalcula distancias */
    fun updateUserLocation(lat: Double, lng: Double) {
        val isFirstLocation = _userLocation.value == null
        if (_userLocation.value?.first != lat || _userLocation.value?.second != lng) {
            _userLocation.value = Pair(lat, lng)
            if (isFirstLocation) {
                fetchStations(lat = lat, lng = lng, radiusKm = 30.0)
            }
        }
    }

    /**
     * Calcula la distancia en kilómetros entre el usuario y una gasolinera.
     */
    private fun computeDistanceKm(userLat: Double, userLng: Double, stationLat: Double, stationLng: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLng, stationLat, stationLng, results)
        return (results[0] / 1000.0)
    }

    /** Filtra, calcula distancia y limita a un número manejable en hilo secundario */
    val filteredStations: StateFlow<List<Station>> = combine(
        rawStations,
        _userLocation,
        _searchQuery,
        _fuelFilter,
        _sortOption
    ) { stations, location, query, fuel, sort ->
        withContext(Dispatchers.Default) {
            val currentLat = location?.first ?: 40.416775
            val currentLng = location?.second ?: -3.703790

            // 1. Calcular distancia para cada gasolinera
            val listWithDistances = stations.map { station ->
                if (station.latitude != null && station.longitude != null) {
                    val dist = computeDistanceKm(currentLat, currentLng, station.latitude, station.longitude)
                    station.copy(distanceKm = dist)
                } else {
                    station
                }
            }

            // 2. Aplicar filtros
            val filtered = listWithDistances
                .filter { station ->
                    if (query.isBlank()) true
                    else {
                        val q = query.lowercase().trim()
                        (station.brand?.lowercase()?.contains(q) == true) ||
                                (station.address?.lowercase()?.contains(q) == true) ||
                                (station.municipality?.lowercase()?.contains(q) == true) ||
                                (station.province?.lowercase()?.contains(q) == true)
                    }
                }
                .filter { station ->
                    when (fuel) {
                        FuelFilter.ALL -> true
                        FuelFilter.GASOLINE_95 -> (station.gasoline95E5 != null && station.gasoline95E5 > 0)
                        FuelFilter.DIESEL -> (station.dieselA != null && station.dieselA > 0)
                    }
                }

            // 3. Aplicar ordenación
            val sorted = filtered.sortedWith { a, b ->
                when (sort) {
                    SortOption.DISTANCE_ASC -> {
                        val distA = a.distanceKm ?: Double.MAX_VALUE
                        val distB = b.distanceKm ?: Double.MAX_VALUE
                        distA.compareTo(distB)
                    }
                    SortOption.PRICE_G95_ASC -> (a.gasoline95E5 ?: Double.MAX_VALUE).compareTo(b.gasoline95E5 ?: Double.MAX_VALUE)
                    SortOption.PRICE_DIESEL_ASC -> (a.dieselA ?: Double.MAX_VALUE).compareTo(b.dieselA ?: Double.MAX_VALUE)
                    SortOption.BRAND_ASC -> (a.brand ?: "").compareTo(b.brand ?: "", ignoreCase = true)
                }
            }

            // Límite de marcadores para mantener 60 fps
            if (query.isBlank()) {
                sorted.take(50)
            } else {
                sorted.take(120)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFuelFilterSelected(filter: FuelFilter) {
        _fuelFilter.value = filter
    }

    fun onSortOptionSelected(option: SortOption) {
        _sortOption.value = option
    }

    fun selectStation(station: Station?) {
        _selectedStation.value = station
    }
}
