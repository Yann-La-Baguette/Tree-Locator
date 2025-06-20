package com.yannlabaguette.treelocator

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.res.painterResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsPage(viewModel: MainViewModel) {
    val categories by viewModel.categories.collectAsState()
    var name by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val activity = LocalContext.current as Activity
    val LightGreen = Color(0xFF81C784)
    val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    var showSettings by remember { mutableStateOf(false) }

    // Gestion de la permission
    val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    val hasPermission = ActivityCompat.checkSelfPermission(
        activity, locationPermission
    ) == PackageManager.PERMISSION_GRANTED

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> /* rien à faire ici, on gère dans le bouton */ }

    fun getCategoryPath(cat: Category): String {
        val parent = categories.find { it.name == cat.parent }
        return if (parent != null) getCategoryPath(parent) + "/" + cat.name else cat.name
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        IconButton(onClick = { showSettings = true }) {
            Icon(Icons.Filled.Settings, contentDescription = "Réglages", tint = Color.Gray)
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Image(
                painter = painterResource(id = R.drawable.treeappicon),
                contentDescription = "Icône de l'application",
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tree Locator",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily(Font(R.font.boldonse_regular)),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(40.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du point GPS") },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .align(Alignment.CenterHorizontally),
                //textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = LightGreen,
                    unfocusedIndicatorColor = LightGreen,
                    focusedLabelColor = LightGreen,
                    //unfocusedLabelColor = LightGreen
                )
            )
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .align(Alignment.CenterHorizontally)
            ) {
                OutlinedTextField(
                    value = selectedCategory?.let { name ->
                        categories.find { it.name == name }?.let { getCategoryPath(it) }
                    } ?: "Aucune catégorie",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Catégorie") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(1.0f),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = LightGreen,
                        unfocusedIndicatorColor = LightGreen,
                        focusedLabelColor = LightGreen,
                        unfocusedLabelColor = LightGreen
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Aucune catégorie") },
                        onClick = {
                            selectedCategory = null
                            expanded = false
                        }
                    )
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(getCategoryPath(cat)) },
                            onClick = {
                                selectedCategory = cat.name
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (!hasPermission) {
                        launcher.launch(locationPermission)
                    } else {
                        val pointName = name
                        val fusedLocationClient =
                            LocationServices.getFusedLocationProviderClient(activity)
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            val lat = location?.latitude ?: 0.0
                            val lon = location?.longitude ?: 0.0

                            viewModel.addGpsPoint(
                                //GpsPoint(pointName, lat, lon, selectedCategory, currentDate)
                                GpsPoint(
                                    name = pointName,
                                    latitude = lat,
                                    longitude = lon,
                                    category = selectedCategory,
                                    date = currentDate
                                )
                            )
                            Log.d(
                                "GpsPage",
                                "Point ajouté: $pointName ($lat, $lon) dans la catégorie ${selectedCategory ?: "Aucune"}"
                            )
                        }
                        name = ""
                    }
                },
                enabled = selectedCategory != null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(containerColor = LightGreen)
            ) {
                Text("Enregistrer la position", textAlign = TextAlign.Center)
            }
        }
    }
    if (showSettings) {
        // Ajoute un fond semi-opaque
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
        ) {
            SettingsPage(onClose = { showSettings = false }, viewModel = viewModel)
        }
    }
    if (showSettings) {
        SettingsPage(onClose = { showSettings = false }, viewModel = viewModel)
    }
}

@Composable
fun SettingsPage(
    onClose: () -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val LightGreen = Color(0xFF81C784)
    val Orange = Color(0xFFFFA726)

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.importFromExcelUri(it) }
        }
    )

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .fillMaxWidth(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Réglages", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.exportToExcel() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Orange)
            ) {
                Text("Exporter en Excel")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LightGreen)
            ) {
                Text("Importer depuis Excel")
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onClose,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = LightGreen)
            ) {
                Text("Fermer")
            }
        }
    }
}