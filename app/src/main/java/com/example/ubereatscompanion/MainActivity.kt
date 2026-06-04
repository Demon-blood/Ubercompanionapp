package com.example.ubereatscompanion

import android.Manifest
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import com.example.ubereatscompanion.data.CompanionRepository
import com.example.ubereatscompanion.data.DeliveryCsvImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.ubereatscompanion.services.LocationTrackingService
import com.example.ubereatscompanion.services.OcrMonitorService
import com.example.ubereatscompanion.features.OfferOverlayService
import com.example.ubereatscompanion.ui.CompanionApp

class MainActivity : ComponentActivity() {
    private lateinit var repository: CompanionRepository

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val hasFine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val hasCoarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasFine || hasCoarse) startLocationService()
    }


    private val csvImportLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                contentResolver.openInputStream(uri)?.use { input ->
                    repository.importDeliveries(DeliveryCsvImporter.parse(input))
                }
            }
        }
    }

    private val screenCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        val intent = Intent(this, OcrMonitorService::class.java).apply {
            putExtra(OcrMonitorService.EXTRA_RESULT_CODE, result.resultCode)
            putExtra(OcrMonitorService.EXTRA_DATA, data)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRuntimePermissions()

        repository = CompanionRepository((application as UberCompanionApp).database)
        setContent {
            MaterialTheme {
                CompanionApp(
                    repository = repository,
                    onStartLocation = { startLocationService() },
                    onStartOcr = { requestScreenCapture() },
                    onStopOcr = { stopService(Intent(this, OcrMonitorService::class.java)) },
                    onImportCsv = { csvImportLauncher.launch(arrayOf("text/*", "text/comma-separated-values", "application/csv", "application/vnd.ms-excel")) },
                    onOpenAccessibilitySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    onOpenAppSettings = {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        })
                    },
                    onOpenOverlaySettings = { openOverlaySettings() },
                    onOpenNotificationSettings = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    onStartOverlay = { startOfferOverlay() }
                )
            }
        }
    }

    private fun requestRuntimePermissions() {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        permissionLauncher.launch(permissions)
    }

    private fun startLocationService() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            ContextCompat.startForegroundService(this, Intent(this, LocationTrackingService::class.java))
        } else {
            requestRuntimePermissions()
        }
    }

    private fun requestScreenCapture() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}


private fun MainActivity.openOverlaySettings() {
    if (!Settings.canDrawOverlays(this)) {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        })
    }
}

private fun MainActivity.startOfferOverlay() {
    if (!Settings.canDrawOverlays(this)) {
        openOverlaySettings()
    } else {
        ContextCompat.startForegroundService(this, Intent(this, OfferOverlayService::class.java))
    }
}
