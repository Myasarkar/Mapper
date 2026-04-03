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
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val markers = mutableStateListOf<MapMarkerData>()
    private var currentLocationState = mutableStateOf(GeoPoint(41.0082, 28.9784))

    // İzin isteme yapısı
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        networkMonitor = NetworkMonitor(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // İzinleri iste
        requestPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.POST_NOTIFICATIONS
        ))

        // Servisi başlat
        val serviceIntent = Intent(this, ForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }

        setContent {
            BandMapperTheme {
                MainLayout()
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(3000)
            .setMaxUpdateDelayMillis(10000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    currentLocationState.value = GeoPoint(location.latitude, location.longitude)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // İzin yok
        }
    }

    override fun onStart() {
        super.onStart()
        networkMonitor.startMonitoring()
    }

    override fun onStop() {
        super.onStop()
        networkMonitor.stopMonitoring()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainLayout() {
        val bandInfo by networkMonitor.currentBand.collectAsState()
        val currentLocation by currentLocationState
        var centerTrigger by remember { mutableStateOf(0) }

        // nPerf Tarzı Haritalama Mantığı
        // Her konum veya bant değiştiğinde iz bırak
        LaunchedEffect(currentLocation, bandInfo) {
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
                is NetworkMonitor.BandInfo.NR -> "n${(bandInfo as NetworkMonitor.BandInfo.NR).bandIndex}"
                is NetworkMonitor.BandInfo.LTE -> "LTE"
                else -> "Sinyal Yok"
            }
            
            // Eğer son eklenen nokta ile aynı konumdaysak ekleme (gereksiz kalabalığı önlemek için)
            if (markers.isEmpty() || markers.last().position != currentLocation) {
                markers.add(MapMarkerData(currentLocation, color, bandName))
            }
        }

        // Periyodik şebeke güncellemesi (subscribe çalışmazsa yedek olarak)
        LaunchedEffect(Unit) {
            while(true) {
                networkMonitor.updateNetworkInfo()
                delay(2000)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("5G n78 Mapper") })
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    centerTrigger++
                }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Konumum")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Üst Gösterge Paneli
                BandIndicator(bandInfo)
                
                // Harita Alanı
                Box(modifier = Modifier.weight(1f)) {
                    MapScreen(currentLocation, markers, centerTrigger)
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
                Triple("n${nr.bandIndex} (${if (nr.bandIndex == 78) "3500MHz" else "700MHz"})", bandColor, if (nr.isSA) "5G Standalone" else "5G NSA")
            }
            is NetworkMonitor.BandInfo.LTE -> Triple("4G LTE", Color.Gray, "LTE")
            else -> Triple("Sinyal Yok", Color.Red, "Bilinmiyor")
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(subText, fontSize = 14.sp, color = Color.Gray)
                }
            }
        }
    }
}
