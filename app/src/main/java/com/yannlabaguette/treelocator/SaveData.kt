package com.yannlabaguette.treelocator

@kotlinx.serialization.Serializable
data class SaveData(
    val categories: List<Category>,
    val gpsPoints: List<GpsPoint>
)