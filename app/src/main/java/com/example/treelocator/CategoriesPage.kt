package com.example.treelocator

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesPage(viewModel: MainViewModel) {
    val categories by viewModel.categories.collectAsState()
    val gpsPoints by viewModel.gpsPoints.collectAsState()
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    var showCategoryManager by remember { mutableStateOf(false) }
    var categorySearchQuery by remember { mutableStateOf("") }
    var pointSearchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val LightGreen = Color(0xFF81C784)
    val lazyListState = rememberLazyListState()
    var fabVisible by remember { mutableStateOf(true) }
    var lastScrollOffset by remember { mutableStateOf(0) }
    var pointToDelete by remember { mutableStateOf<GpsPoint?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    // --- Ajout pour le filtre par mois ---
    val moisList = listOf(
        "Tous", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
        "Juillet", "Aout", "Septembre", "Octobre", "Novembre", "Décembre"
    )
    var selectedMois by remember { mutableStateOf("Tous") }
    var moisDropdownExpanded by remember { mutableStateOf(false) }
    // -------------------------------------

    @Composable
    fun CategoryTree(
        parent: String? = null,
        indent: Int = 0,
        cats: List<Category> = categories,
        points: List<GpsPoint> = gpsPoints
    ) {
        cats.filter { it.parent == parent }
            .filter {
                selectedMois == "Tous" ||
                        categoryOrDescendantsHasPointInMonth(it, categories, gpsPoints, moisList.indexOf(selectedMois))
            }
            .filter {
                categorySearchQuery.isBlank() ||
                        categoryOrDescendantsMatchCategoryQuery(it, categories, categorySearchQuery)
            }
            .filter {
                pointSearchQuery.isBlank() ||
                        categoryContainsMatchingPoint(it, categories, gpsPoints, pointSearchQuery)
            }
            .forEach { cat ->
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
                            onClick = { categoryToDelete = cat },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer la catégorie", tint = Color.Red)
                        }
                        IconButton(onClick = {
                            expandedStates[cat.name] = !(expandedStates[cat.name] ?: false)
                        }) {
                            Icon(
                                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (expanded) "Réduire" else "Dérouler",
                                tint = LightGreen
                            )
                        }
                    }
                    if (expanded) {
                        val pointsInCat = points.filter { it.category == cat.name }
                        val filteredPointsInCat = pointsInCat
                            .filter { pointSearchQuery.isBlank() || (it.name?.contains(pointSearchQuery, ignoreCase = true) == true) }
                            .filter {
                                selectedMois == "Tous" ||
                                        getMonthFromDate(it.date) == moisList.indexOf(selectedMois)
                            }
                        val hasSubCategories = cats.any { it.parent == cat.name }
                        if (filteredPointsInCat.isEmpty() && !hasSubCategories) {
                            Text("Aucun point", Modifier.padding(start = (indent * 16 + 16).dp))
                        } else {
                            filteredPointsInCat.forEach { point ->
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
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0x4F81C784))
                                ) {
                                    Row(
                                        Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(point.name ?: "Sans nom", style = MaterialTheme.typography.bodyMedium)
                                            Text(formatDate(point.date), style = MaterialTheme.typography.bodySmall)
                                        }
                                        IconButton(onClick = { pointToDelete = point }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer le point", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                        CategoryTree(parent = cat.name, indent = indent + 1, cats = cats, points = points)
                    }
                }
            }
    }

    // Détection du scroll
    LaunchedEffect(lazyListState.firstVisibleItemScrollOffset, lazyListState.firstVisibleItemIndex) {
        val currentOffset = lazyListState.firstVisibleItemIndex * 10000 + lazyListState.firstVisibleItemScrollOffset
        fabVisible = currentOffset <= lastScrollOffset
        lastScrollOffset = currentOffset
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            // Barre de recherche catégories
            OutlinedTextField(
                value = categorySearchQuery,
                onValueChange = { categorySearchQuery = it },
                label = { Text("Rechercher une catégorie") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = LightGreen,
                    unfocusedIndicatorColor = LightGreen,
                    focusedLabelColor = LightGreen,
                    //unfocusedLabelColor = LightGreen
                )
            )
            // Barre de recherche points
            OutlinedTextField(
                value = pointSearchQuery,
                onValueChange = { pointSearchQuery = it },
                label = { Text("Rechercher un point") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = LightGreen,
                    unfocusedIndicatorColor = LightGreen,
                    focusedLabelColor = LightGreen,
                    //unfocusedLabelColor = LightGreen
                )
            )
            // --- Ajout du menu déroulant pour le mois ---
            ExposedDropdownMenuBox(
                expanded = moisDropdownExpanded,
                onExpandedChange = { moisDropdownExpanded = !moisDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedMois,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filtrer par mois") },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = LightGreen,
                        unfocusedIndicatorColor = LightGreen,
                        focusedLabelColor = LightGreen,
                        unfocusedLabelColor = LightGreen
                    )
                )
                ExposedDropdownMenu(
                    expanded = moisDropdownExpanded,
                    onDismissRequest = { moisDropdownExpanded = false }
                ) {
                    moisList.forEach { mois ->
                        DropdownMenuItem(
                            text = { Text(mois) },
                            onClick = {
                                selectedMois = mois
                                moisDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            // -------------------------------------------
            LazyColumn(state = lazyListState) {
                item {
                    CategoryTree()
                }
            }
        }
        if (fabVisible) {
            FloatingActionButton(
                onClick = { showCategoryManager = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = LightGreen
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Gérer les catégories")
            }
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
        if (pointToDelete != null) {
            AlertDialog(
                onDismissRequest = { pointToDelete = null },
                title = { Text("Confirmation") },
                text = { Text("Êtes-vous sûr de vouloir supprimer ${pointToDelete?.name ?: "ce point"} ?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteGpsPoint(pointToDelete!!)
                        pointToDelete = null
                    }) { Text("Oui", color = LightGreen) }
                },
                dismissButton = {
                    TextButton(onClick = { pointToDelete = null }) { Text("Annuler", color = LightGreen) }
                }
            )
        }
        if (categoryToDelete != null) {
            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                title = { Text("Confirmation") },
                text = { Text("Êtes-vous sûr de vouloir supprimer ${categoryToDelete?.name ?: "cette catégorie"} ?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteCategory(categoryToDelete!!)
                        categoryToDelete = null
                    }) { Text("Oui", color = LightGreen) }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToDelete = null }) { Text("Annuler", color = LightGreen) }
                }
            )
        }
    }
}

fun getMonthFromDate(date: String?): Int? {
    return try {
        date?.split("-")?.getOrNull(1)?.toIntOrNull()
    } catch (e: Exception) { null }
}

fun categoryContainsMatchingPoint(
    cat: Category,
    cats: List<Category>,
    points: List<GpsPoint>,
    pointQuery: String
): Boolean {
    // Points de cette catégorie qui correspondent
    if (points.any { it.category == cat.name && it.name?.contains(pointQuery, ignoreCase = true) == true }) return true
    // Vérifie récursivement les sous-catégories
    return cats.filter { it.parent == cat.name }
        .any { categoryContainsMatchingPoint(it, cats, points, pointQuery) }
}

fun categoryOrDescendantsHasPointInMonth(
    cat: Category,
    cats: List<Category>,
    points: List<GpsPoint>,
    selectedMonth: Int
): Boolean {
    // Vérifie si cette catégorie a un point dans le mois sélectionné
    if (points.any { it.category == cat.name && getMonthFromDate(it.date) == selectedMonth }) return true
    // Vérifie récursivement les sous-catégories
    return cats.filter { it.parent == cat.name }
        .any { categoryOrDescendantsHasPointInMonth(it, cats, points, selectedMonth) }
}

fun categoryOrDescendantsMatchCategoryQuery(
    cat: Category,
    cats: List<Category>,
    query: String
): Boolean {
    if (cat.name.contains(query, ignoreCase = true)) return true
    return cats.filter { it.parent == cat.name }
        .any { categoryOrDescendantsMatchCategoryQuery(it, cats, query) }
}

fun formatDate(date: String?): String {
    return try {
        val parts = date?.split("-")
        if (parts != null && parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}"
        else "Date inconnue"
    } catch (e: Exception) {
        "Date inconnue"
    }
}