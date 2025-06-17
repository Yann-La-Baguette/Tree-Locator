package com.example.treelocator

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.Color

@Composable
fun CategoriesPage(viewModel: MainViewModel) {
    val categories by viewModel.categories.collectAsState()
    val gpsPoints by viewModel.gpsPoints.collectAsState()
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    var showCategoryManager by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val LightGreen = Color(0xFF81C784)

    @Composable
    fun CategoryTree(
        parent: String? = null,
        indent: Int = 0
    ) {
        categories.filter { it.parent == parent }.forEach { cat ->
            val expanded = expandedStates[cat.name] ?: false
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = (indent * 16).dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(cat.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { viewModel.deleteCategory(cat) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer la catégorie")
                    }
                    IconButton(onClick = {
                        expandedStates[cat.name] = !(expandedStates[cat.name] ?: false)
                    }) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Réduire" else "Dérouler"
                        )
                    }
                }
                if (expanded) {
                    val points = gpsPoints.filter { it.category == cat.name }
                    val hasSubCategories = categories.any { it.parent == cat.name }
                    if (points.isEmpty() && !hasSubCategories) {
                        Text("Aucun point", Modifier.padding(start = (indent * 16 + 16).dp))
                    } else {
                        points.forEach { point ->
                            Card(
                                modifier = Modifier
                                    .padding(start = (indent * 16 + 16).dp, top = 4.dp, bottom = 4.dp, end = 16.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        val gmmIntentUri = Uri.parse("geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}(${point.name ?: "Point"})")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        context.startActivity(mapIntent)
                                    },
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(point.name ?: "Sans nom", style = MaterialTheme.typography.bodyMedium)
                                        Text("(${point.latitude}, ${point.longitude})", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { viewModel.deleteGpsPoint(point) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer le point")
                                    }
                                }
                            }
                        }
                    }
                    CategoryTree(parent = cat.name, indent = indent + 1)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            LazyColumn {
                item {
                    CategoryTree()
                }
            }
        }
        FloatingActionButton(
            onClick = { showCategoryManager = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = LightGreen
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Gérer les catégories")
        }

        if (showCategoryManager) {
            Dialog(onDismissRequest = { showCategoryManager = false }) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 8.dp
                ) {
                    CategoryManagerPage(
                        viewModel = viewModel,
                        onClose = { showCategoryManager = false }
                    )
                }
            }
        }
    }
}