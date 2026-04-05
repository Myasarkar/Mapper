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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
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
import com.example.bandmapper.data.BandData
import com.example.bandmapper.data.BandDatabase
import kotlinx.coroutines.launch
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.app.Activity

class MainActivity : ComponentActivity() {

    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: BandDatabase
    private val markers = mutableStateListOf<MapMarkerData>()
    private var currentLocationState = mutableStateOf(GeoPoint(41.0082, 28.9784))
    private var isMappingActive = mutableStateOf(false)
    private var hasPermissions = mutableStateOf(false)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                // Sadece yeterli doğruluktaki konumları al (Örn: 30 metreden iyi)
                // Bu, "orada olmadığın yerlere işaretleme yapma" sorununu çözer.
                LogManager.log("Location Update: Acc ${location.accuracy}m, Lat ${location.latitude}")
                if (location.accuracy < 30f) {
                    currentLocationState.value = GeoPoint(location.latitude, location.longitude)
                }
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
        database = BandDatabase.getDatabase(this)

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
        val scope = rememberCoroutineScope()
        
        // Debug Modülü
        var debugClickCount by remember { mutableStateOf(0) }
        var showDebugDialog by remember { mutableStateOf(false) }
        var lastClickTime by remember { mutableStateOf(0L) }

        if (showDebugDialog) {
            AlertDialog(
                onDismissRequest = { showDebugDialog = false },
                title = { Text("Hata Ayıklama Logları") },
                text = {
                    Box(modifier = Modifier.height(400.dp).fillMaxWidth()) {
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(LogManager.logs.size) { index ->
                                Text(LogManager.logs[LogManager.logs.size - 1 - index], fontSize = 10.sp)
                                HorizontalDivider()
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { LogManager.clear() }) { Text("Temizle") }
                },
                dismissButton = {
                    Button(onClick = { showDebugDialog = false }) { Text("Kapat") }
                }
            )
        }

        // Ekranı Açık Tutma (Mapping Aktifken)
        val context = LocalContext.current
        DisposableEffect(isMapping) {
            val activity = context as? Activity
            if (isMapping) {
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        // Geçmiş Verileri Yükle
        LaunchedEffect(Unit) {
            database.bandDataDao().getAllBandData().collect { history ->
                if (markers.isEmpty()) { // Sadece başlangıçta yükle
                    history.forEach { data ->
                        val color = when (data.technology) {
                            "5G" -> {
                                if (data.bandName.contains("78")) Color.Green
                                else if (data.bandName.contains("28")) Color(0xFFFFA500)
                                else Color.Blue
                            }
                            "LTE" -> Color.Gray
                            else -> Color.Red
                        }
                        markers.add(MapMarkerData(id = data.id, position = GeoPoint(data.latitude, data.longitude), color = color, bandName = data.bandName))
                    }
                }
            }
        }

        // Haritalama Mantığı (Sadece Aktifken)
        LaunchedEffect(currentLocation, bandInfo, isMapping) {
            if (isMapping) {
                LogManager.log("Mapping: Lat ${currentLocation.latitude}, Lon ${currentLocation.longitude}, Band: $bandInfo")
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
                
                val tech = when (bandInfo) {
                    is NetworkMonitor.BandInfo.NR -> "5G"
                    is NetworkMonitor.BandInfo.LTE -> "LTE"
                    else -> "NONE"
                }
                
                // 20 metre yakınında başka bir nokta var mı kontrol et
                val existingMarkerIndex = markers.indexOfFirst { 
                    it.position.distanceToAsDouble(currentLocation) < 20.0 
                }

                if (existingMarkerIndex != -1) {
                    // Mevcut noktayı güncelle
                    val existingMarker = markers[existingMarkerIndex]
                    val updatedMarker = existingMarker.copy(color = color, bandName = bandName)
                    markers[existingMarkerIndex] = updatedMarker
                    LogManager.log("Marker Updated: ID ${existingMarker.id}, Band $bandName")
                    
                    // Veritabanında güncelle
                    scope.launch {
                        database.bandDataDao().insert(
                            BandData(
                                id = existingMarker.id ?: 0,
                                latitude = existingMarker.position.latitude,
                                longitude = existingMarker.position.longitude,
                                bandName = bandName,
                                technology = tech
                            )
                        )
                    }
                } else {
                    // Yeni nokta ekle
                    scope.launch {
                        val newId = database.bandDataDao().insert(
                            BandData(
                                latitude = currentLocation.latitude,
                                longitude = currentLocation.longitude,
                                bandName = bandName,
                                technology = tech
                            )
                        ).toInt()
                        markers.add(MapMarkerData(id = newId, position = currentLocation, color = color, bandName = bandName))
                        LogManager.log("New Marker Added: ID $newId, Band $bandName")
                    }
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
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = { 
                        Text(
                            "5G Band Mapper", 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                val now = System.currentTimeMillis()
                                if (now - lastClickTime < 1000) {
                                    debugClickCount++
                                    if (debugClickCount >= 5) {
                                        showDebugDialog = true
                                        debugClickCount = 0
                                    }
                                } else {
                                    debugClickCount = 1
                                }
                                lastClickTime = now
                            }
                        ) 
                    },
                    actions = {
                        IconButton(onClick = { 
                            isMappingActive.value = !isMappingActive.value 
                            LogManager.log("Mapping Button Clicked: New State = ${isMappingActive.value}")
                        }) {
                            Icon(
                                if (isMapping) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = if (isMapping) "Durdur" else "Başlat",
                                tint = if (isMapping) Color.Red else Color.Green
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    centerTrigger++
                    LogManager.log("Center Button Clicked")
                }) {
                    Icon(Icons.Filled.LocationOn, contentDescription = "Konumum")
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Harita (En Altta)
                MapScreen(currentLocation, markers, centerTrigger)
                
                // Üst Gösterge Paneli (Haritanın Üstünde)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(1f) // Haritanın üstünde kalmasını garanti et
                ) {
                    BandIndicator(bandInfo)
                    if (!isMapping) {
                        Card(
                            modifier = Modifier.padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Yellow.copy(alpha = 0.9f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Text("Haritalama Durduruldu. Başlatmak için sağ üstteki butona basın.", 
                                 modifier = Modifier.padding(12.dp), 
                                 fontSize = 14.sp, 
                                 fontWeight = FontWeight.Bold,
                                 color = Color.Black)
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
                Triple("$bandName ($freq)", bandColor, "5G Aktif") // SA/NSA bilgisini kaldırdık
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 24.sp,
                        maxLines = 1
                    )
                    Text(
                        text = subText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
