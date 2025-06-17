package com.example.treelocator
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val name: String,
    val parent: String? = null
)