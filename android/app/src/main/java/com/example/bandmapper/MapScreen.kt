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
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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
