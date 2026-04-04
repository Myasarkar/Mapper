/*
 * Copyright 2026 Mustafa Yasar Kar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bandmapper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bandmapper.ui.theme.BandMapperTheme
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val markers = mutableStateListOf<MapMarkerData>()
    private var currentLocationState = mutableStateOf(GeoPoint(41.0082, 28.9784))
    private var isMappingActive = mutableStateOf(false)
    private var hasPermissions = mutableStateOf(false)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                currentLocationState.value = GeoPoint(location.latitude, location.longitude)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        hasPermissions.value = allGranted
        if (allGranted) {
            startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Configuration.getInstance().userAgentValue = packageName
        networkMonitor = NetworkMonitor(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkAndRequestPermissions()

        setContent {
            BandMapperTheme {
                if (hasPermissions.value) {
                    MainLayout()
                } else {
                    PermissionRequiredScreen { checkAndRequestPermissions() }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        
        val allGranted = permissions.all { 
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
        }
        
        if (allGranted) {
            hasPermissions.value = true
            startLocationUpdates()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // İzin yok
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onStart() {
        super.onStart()
        networkMonitor.startMonitoring()
    }

    override fun onStop() {
        super.onStop()
        networkMonitor.stopMonitoring()
    }

    @Composable
    fun PermissionRequiredScreen(onRetry: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Uygulamanın çalışması için izinler gereklidir.", modifier = Modifier.padding(16.dp))
                Button(onClick = onRetry) {
                    Text("İzinleri Tekrar İste")
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainLayout() {
        val bandInfo by networkMonitor.currentBand.collectAsState()
        val currentLocation by currentLocationState
        val isMapping by isMappingActive
        var centerTrigger by remember { mutableStateOf(0) }

        // Haritalama Mantığı (Sadece Aktifken)
        LaunchedEffect(currentLocation, bandInfo, isMapping) {
            if (isMapping) {
                val color = when (bandInfo) {
                    is NetworkMonitor.BandInfo.NR -> {
                        val nr = bandInfo as NetworkMonitor.BandInfo.NR
                        when (nr.bandIndex) {
                            78 -> Color.Green
                            28 -> Color(0xFFFFA500) // Orange
                            else -> Color.Blue
                        }
                    }
                    is NetworkMonitor.BandInfo.LTE -> Color.Gray
                    else -> Color.Red
                }
                
                val bandName = when (bandInfo) {
                    is NetworkMonitor.BandInfo.NR -> {
                        val band = (bandInfo as NetworkMonitor.BandInfo.NR).bandIndex
                        if (band > 0) "n$band" else "5G"
                    }
                    is NetworkMonitor.BandInfo.LTE -> "LTE"
                    else -> "Sinyal Yok"
                }
                
                if (markers.isEmpty() || markers.last().position != currentLocation) {
                    markers.add(MapMarkerData(currentLocation, color, bandName))
                }
            }
        }

        // Şebeke güncellemesi
        LaunchedEffect(isMapping) {
            while(isMapping) {
                networkMonitor.updateNetworkInfo()
                delay(2000)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("5G Band Mapper", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { isMappingActive.value = !isMappingActive.value }) {
                            Icon(
                                if (isMapping) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isMapping) "Durdur" else "Başlat",
                                tint = if (isMapping) Color.Red else Color.Green
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    centerTrigger++
                }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Konumum")
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Harita (En Altta)
                MapScreen(currentLocation, markers, centerTrigger)
                
                // Üst Gösterge Paneli (Haritanın Üstünde)
                Column(modifier = Modifier.align(Alignment.TopCenter)) {
                    BandIndicator(bandInfo)
                    if (!isMapping) {
                        Card(
                            modifier = Modifier.padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Yellow.copy(alpha = 0.8f))
                        ) {
                            Text("Haritalama Durduruldu. Başlatmak için sağ üstteki butona basın.", 
                                 modifier = Modifier.padding(8.dp), 
                                 fontSize = 12.sp, 
                                 fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BandIndicator(bandInfo: NetworkMonitor.BandInfo) {
        val (text, color, subText) = when (bandInfo) {
            is NetworkMonitor.BandInfo.NR -> {
                val nr = bandInfo
                val bandColor = when (nr.bandIndex) {
                    78 -> Color.Green
                    28 -> Color(0xFFFFA500) // Orange
                    else -> Color.Blue
                }
                val bandName = if (nr.bandIndex > 0) "n${nr.bandIndex}" else "5G"
                val freq = when(nr.bandIndex) {
                    78 -> "3500MHz"
                    28 -> "700MHz"
                    else -> "Bilinmiyor"
                }
                Triple("$bandName ($freq)", bandColor, if (nr.isSA) "5G Standalone" else "5G NSA")
            }
            is NetworkMonitor.BandInfo.LTE -> Triple("4G LTE", Color.Gray, "LTE")
            else -> Triple("Sinyal Yok", Color.Red, "Bilinmiyor")
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp
                    )
                    Text(
                        text = subText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
