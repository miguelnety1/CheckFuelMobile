package com.example.checkfuelmobile

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.checkfuelmobile.ui.screens.HomeScreen
import org.osmdroid.config.Configuration

/**
 * Actividad Principal.
 * Inicializa la configuración de baldosas (tiles) de OpenStreetMap antes del renderizado de Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración crítica de osmdroid antes de cargar el MapView:
        // Evita el error 403 Forbidden y permite el almacenamiento en caché de tiles
        Configuration.getInstance().apply {
            userAgentValue = "CheckFuelMobileApp/1.0 (Android)"
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir
            load(this@MainActivity, PreferenceManager.getDefaultSharedPreferences(this@MainActivity))
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
}
