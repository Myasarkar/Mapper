package com.example.bandmapper

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bandmapper.ui.theme.BandMapperTheme
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var networkMonitor: NetworkMonitor
    private val markers = mutableStateListOf<MapMarkerData>()

    // İzin isteme yapısı
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // İzinler verildi mi kontrolü yapılabilir
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        networkMonitor = NetworkMonitor(this)

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

        setContent {
            BandMapperTheme {
                MainLayout()
            }
        }
    }

    @Composable
    fun MainLayout() {
        val bandInfo by networkMonitor.currentBand.collectAsState()
        var currentLocation by remember { mutableStateOf(GeoPoint(41.0082, 28.9784)) } // Varsayılan İstanbul

        // Her 5 saniyede bir veri kaydı simülasyonu (Gerçek konum servisi ile bağlanmalı)
        LaunchedEffect(Unit) {
            while (true) {
                delay(5000)
                networkMonitor.updateNetworkInfo()
                
                val color = when (bandInfo) {
                    is NetworkMonitor.BandInfo.NR -> {
                        val nr = bandInfo as NetworkMonitor.BandInfo.NR
                        if (nr.bandIndex == 78) Color.Green else Color.Blue
                    }
                    else -> Color.Gray
                }
                
                markers.add(MapMarkerData(currentLocation, color, "Band"))
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("5G n78 Mapper") })
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Üst Gösterge Paneli
                BandIndicator(bandInfo)
                
                // Harita Alanı
                Box(modifier = Modifier.weight(1f)) {
                    MapScreen(currentLocation, markers)
                }
            }
        }
    }

    @Composable
    fun BandIndicator(bandInfo: NetworkMonitor.BandInfo) {
        val (text, color, subText) = when (bandInfo) {
            is NetworkMonitor.BandInfo.NR -> {
                val nr = bandInfo
                if (nr.bandIndex == 78) {
                    Triple("n78 (3500MHz)", Color.Green, if (nr.isSA) "5G Standalone" else "5G NSA")
                } else {
                    Triple("n${nr.bandIndex}", Color.Blue, "5G NR")
                }
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
