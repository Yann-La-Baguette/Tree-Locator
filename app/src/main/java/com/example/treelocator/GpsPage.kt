package com.example.treelocator

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
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsPage(viewModel: MainViewModel) {
    val categories by viewModel.categories.collectAsState()
    var name by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val activity = LocalContext.current as Activity
    val LightGreen = Color(0xFF81C784)

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
                    unfocusedLabelColor = LightGreen
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
                                GpsPoint(pointName, lat, lon, selectedCategory)
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
}