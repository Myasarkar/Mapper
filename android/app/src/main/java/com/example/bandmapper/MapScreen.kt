package com.example.bandmapper

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.config.Configuration
import androidx.preference.PreferenceManager

@Composable
fun MapScreen(
    currentLocation: GeoPoint,
    markers: List<MapMarkerData>
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))

            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(currentLocation)
            }
        },
        update = { mapView ->
            // Sadece konum değiştiğinde merkeze al (Kullanıcı haritayı kaydırıyorsa bozmamak için)
            // mapView.controller.animateTo(currentLocation) 
            
            // Mevcut overlay sayısını kontrol et, eğer liste boyutu farklıysa güncelle
            if (mapView.overlays.size != markers.size) {
                mapView.overlays.clear()
                markers.forEach { data ->
                    val marker = Marker(mapView)
                    marker.position = data.position
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = data.bandName
                    // Marker rengini burada data.color'a göre özelleştirebilirsin (İkon değiştirerek)
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        }
    )
}
