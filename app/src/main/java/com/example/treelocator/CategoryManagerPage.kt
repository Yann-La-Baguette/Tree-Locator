package com.example.treelocator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerPage(viewModel: MainViewModel, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val categories by viewModel.categories.collectAsState(initial = emptyList<Category>())
    var parent by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val LightGreen = Color(0xFF81C784)

    // Fonction pour obtenir le chemin complet d'une catégorie
    fun getCategoryPath(cat: Category): String {
        val parentCat = categories.find { it.name == cat.parent }
        return if (parentCat != null) getCategoryPath(parentCat) + "/" + cat.name else cat.name
    }

    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nom de la catégorie") }
        )
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = parent?.let { pname ->
                    categories.find { it.name == pname }?.let { getCategoryPath(it) }
                } ?: "Aucune (racine)",
                onValueChange = {},
                readOnly = true,
                label = { Text("Catégorie parente") },
                trailingIcon = {
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null
                    )
                },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Aucune (racine)") },
                    onClick = {
                        parent = null
                        expanded = false
                    }
                )
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(getCategoryPath(cat)) },
                        onClick = {
                            parent = cat.name
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            viewModel.addCategory(Category(name, parent))
            name = ""
            parent = null
        },
            colors = ButtonDefaults.buttonColors(containerColor = LightGreen)
        ) {
            Text("Créer la catégorie")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LightGreen)
        ) {
            Text("Fermer")
        }
    }
}