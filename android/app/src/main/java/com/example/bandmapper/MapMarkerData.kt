package com.example.bandmapper

import androidx.compose.ui.graphics.Color
import org.osmdroid.util.GeoPoint

data class MapMarkerData(
    val position: GeoPoint,
    val color: Color,
    val bandName: String
)
