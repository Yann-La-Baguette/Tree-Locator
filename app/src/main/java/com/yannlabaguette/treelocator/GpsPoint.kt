// DataModels.kt
package com.yannlabaguette.treelocator
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class GpsPoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
    val date: String? = null
)