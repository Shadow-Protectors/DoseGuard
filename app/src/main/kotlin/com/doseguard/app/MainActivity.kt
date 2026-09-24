package com.doseguard.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.doseguard.app.navigation.DoseGuardNavGraph
import com.doseguard.app.repository.DoseGuardRepository
import com.doseguard.app.ui.theme.DoseGuardTheme

/**
 * DoseGuard MainActivity — single-activity Compose host.
 *
 * Responsibilities:
 * 1. Request CAMERA permission at startup (required for QR scan + image capture).
 * 2. Instantiate the shared Repository (passed down to NavGraph).
 * 3. Set up the Navigation Compose host inside the DoseGuard theme.
 */
class MainActivity : ComponentActivity() {

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted → camera screens work; denied → handled inside each screen */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request camera permission upfront — both QR scanner and ImageCapture need it
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Single shared Repository instance — scoped to the Activity lifecycle
        val repository = DoseGuardRepository(applicationContext)

        setContent {
            DoseGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    DoseGuardNavGraph(
                        navController = navController,
                        repository    = repository
                    )
                }
            }
        }
    }
}
