// DataModels.kt
package com.example.treelocator
import kotlinx.serialization.Serializable

@Serializable
data class GpsPoint(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null, // null = sans catégorie
    val date: String?
)