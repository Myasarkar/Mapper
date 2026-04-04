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

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.config.Configuration
import androidx.preference.PreferenceManager
import androidx.compose.ui.graphics.toArgb
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable

@Composable
fun MapScreen(
    currentLocation: GeoPoint,
    markers: List<MapMarkerData>,
    centerTrigger: Int // Bu değer değiştikçe harita merkeze odaklanır
) {
    val mapView = remember { mutableStateOf<MapView?>(null) }
    val locationOverlayState = remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))

            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(currentLocation)
                
                // Mevcut konum katmanı
                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
                locationOverlay.enableMyLocation()
                locationOverlay.enableFollowLocation()
                overlays.add(locationOverlay)
                locationOverlayState.value = locationOverlay
                mapView.value = this
            }
        },
        update = { view ->
            // Marker'ları güncelle
            val currentMarkers = view.overlays.filterIsInstance<Marker>()
            if (currentMarkers.size != markers.size) {
                view.overlays.removeAll(currentMarkers)
                markers.forEach { data ->
                    val marker = Marker(view)
                    marker.position = data.position
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    
                    // nPerf tarzı nokta ikonu oluştur (Boyut büyütüldü)
                    val size = 60
                    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    val paint = android.graphics.Paint()
                    paint.color = data.color.toArgb()
                    paint.isAntiAlias = true
                    // Dış halka (beyaz)
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = android.graphics.Color.WHITE
                    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
                    // İç nokta (renkli)
                    paint.color = data.color.toArgb()
                    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6, paint)
                    
                    marker.icon = android.graphics.drawable.BitmapDrawable(view.context.resources, bitmap)
                    marker.title = "${data.bandName} Band"
                    view.overlays.add(marker)
                }
                view.invalidate()
            }
        }
    )

    // Merkeze odaklanma tetiklendiğinde
    LaunchedEffect(centerTrigger) {
        if (centerTrigger > 0) {
            locationOverlayState.value?.enableFollowLocation()
            mapView.value?.controller?.animateTo(currentLocation)
        }
    }
}
