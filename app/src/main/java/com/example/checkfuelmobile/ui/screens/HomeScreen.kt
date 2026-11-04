package com.example.checkfuelmobile.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checkfuelmobile.data.Station
import com.example.checkfuelmobile.ui.state.FuelFilter
import com.example.checkfuelmobile.ui.state.SortOption
import com.example.checkfuelmobile.ui.state.StationUiState
import com.example.checkfuelmobile.ui.viewmodel.StationViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale

/**
 * Pantalla principal libre de fugas de memoria y bloqueos de interfaz.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: StationViewModel = viewModel()) {
    val context = LocalContext.current

    // Configuración inicial de osmdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Verificación de permisos sin bloquear el hilo principal
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
    }

    // Lanza la solicitud de permisos una única vez al iniciar
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Estados observados del ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val filteredStations by viewModel.filteredStations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFuelFilter by viewModel.fuelFilter.collectAsState()
    val selectedSortOption by viewModel.sortOption.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    // Referencia única al MapView de osmdroid
    val mapView = remember { MapView(context) }

    // Capa de ubicación creada ÚNICAMENTE cuando el permiso es concedido (evita el congelamiento)
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val overlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
                overlay.enableMyLocation()
                locationOverlay = overlay

                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let {
                        viewModel.updateUserLocation(it.latitude, it.longitude)
                    }
                }
            } catch (e: Exception) {
                // Captura de seguridad
            }
        }
    }

    // Gestión limpia del ciclo de vida del MapView
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            locationOverlay?.disableMyLocation()
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Actualización quirúrgica de marcadores aislada fuera del 'update' de AndroidView para evitar recomposiciones infinitas
    LaunchedEffect(filteredStations, mapView, locationOverlay) {
        // Limpiamos únicamente marcadores antiguos
        val existingMarkers = mapView.overlays.filterIsInstance<Marker>()
        mapView.overlays.removeAll(existingMarkers)

        locationOverlay?.let {
            if (!mapView.overlays.contains(it)) {
                mapView.overlays.add(it)
            }
        }

        filteredStations.forEach { station ->
            if (station.latitude != null && station.longitude != null) {
                val marker = Marker(mapView).apply {
                    position = GeoPoint(station.latitude, station.longitude)
                    title = station.brand ?: "Gasolinera"
                    val distStr = station.distanceKm?.let { String.format(Locale.getDefault(), " (%.1f km)", it) } ?: ""
                    snippet = "${station.address ?: ""}$distStr\nG95: ${station.gasoline95E5 ?: "-"}€ | Diésel: ${station.dieselA ?: "-"}€"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ ->
                        viewModel.selectStation(station)
                        true
                    }
                }
                mapView.overlays.add(marker)
            }
        }
        mapView.invalidate()
    }

    // Centrar cámara al seleccionar una gasolinera
    LaunchedEffect(selectedStation) {
        selectedStation?.let { station ->
            if (station.latitude != null && station.longitude != null) {
                val point = GeoPoint(station.latitude, station.longitude)
                mapView.controller.animateTo(point)
                mapView.controller.setZoom(16.0)
            }
        }
    }

    // Centrar automáticamente en la primera gasolinera al cargar
    LaunchedEffect(filteredStations) {
        if (filteredStations.isNotEmpty() && selectedStation == null) {
            val firstWithCoords = filteredStations.firstOrNull { it.latitude != null && it.longitude != null }
            firstWithCoords?.let { station ->
                val point = GeoPoint(station.latitude!!, station.longitude!!)
                mapView.controller.animateTo(point)
                mapView.controller.setZoom(12.0)
            }
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 160.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContent = {
            BottomSheetPanelContent(
                uiState = uiState,
                stations = filteredStations,
                searchQuery = searchQuery,
                selectedFuel = selectedFuelFilter,
                selectedSort = selectedSortOption,
                selectedStation = selectedStation,
                onSearchChange = { viewModel.onSearchQueryChanged(it) },
                onFuelFilterChange = { viewModel.onFuelFilterSelected(it) },
                onSortChange = { viewModel.onSortOptionSelected(it) },
                onStationClick = { station ->
                    viewModel.selectStation(station)
                    coroutineScope.launch { bottomSheetState.partialExpand() }
                },
                onRetry = { viewModel.fetchStations() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // MAPA DE OPENSTREETMAP (osmdroid) - Factory limpia
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        controller.setZoom(12.0)
                        controller.setCenter(GeoPoint(40.416775, -3.703790)) // Madrid por defecto
                    }
                }
            )

            // BOTONES FLOTANTES SUPERIORES (GPS y Recargar)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón GPS
                FloatingActionButton(
                    onClick = {
                        if (hasLocationPermission) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                loc?.let {
                                    viewModel.updateUserLocation(it.latitude, it.longitude)
                                    mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude))
                                    mapView.controller.setZoom(15.0)
                                }
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Mi Ubicación GPS"
                    )
                }

                // Botón Recargar
                FloatingActionButton(
                    onClick = { viewModel.fetchStations() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recargar gasolineras"
                    )
                }
            }
        }
    }
}

/** Contenido del panel flotante desplegable */
@Composable
fun BottomSheetPanelContent(
    uiState: StationUiState,
    stations: List<Station>,
    searchQuery: String,
    selectedFuel: FuelFilter,
    selectedSort: SortOption,
    selectedStation: Station?,
    onSearchChange: (String) -> Unit,
    onFuelFilterChange: (FuelFilter) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onStationClick: (Station) -> Unit,
    onRetry: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        // Indicador visual de arrastre (Drag Handle)
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .width(40.dp)
                .height(4.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {}
        }

        // Barra de Búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por marca, municipio o provincia...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filtros rápidos de combustible (Chips)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(FuelFilter.entries.toTypedArray()) { filter ->
                    FilterChip(
                        selected = (selectedFuel == filter),
                        onClick = { onFuelFilterChange(filter) },
                        label = { Text(filter.label, fontSize = 12.sp) }
                    )
                }
            }

            // Botón Dropdown de Ordenación
            Box {
                OutlinedButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = selectedSort.label,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onSortChange(option)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Encabezado de Resultados
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stations.size} gasolineras mostradas",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Contenido según el Estado de la Red
        when (uiState) {
            is StationUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is StationUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🔴 Error al cargar gasolineras", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(uiState.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = onRetry) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            is StationUiState.Success -> {
                if (stations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No se encontraron gasolineras", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    ) {
                        items(stations) { station ->
                            StationMapListItem(
                                station = station,
                                isSelected = (selectedStation?.id == station.id),
                                onClick = { onStationClick(station) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Tarjeta individual en la lista del panel flotante con badge de distancia */
@Composable
fun StationMapListItem(
    station: Station,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = station.brand ?: "Gasolinera",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )

                station.distanceKm?.let { dist ->
                    Text(
                        text = if (dist < 1.0) "${(dist * 1000).toInt()} m" else String.format(Locale.getDefault(), "%.1f km", dist),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } ?: Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Ver en mapa",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            station.address?.let {
                Text(text = it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            station.locality?.let {
                Text(text = it, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row {
                station.gasoline95E5?.let {
                    Text(text = "G95: $it €", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                station.dieselA?.let {
                    Text(text = "Diésel: $it €", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
