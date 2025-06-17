// MainViewModel.kt
package com.example.treelocator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val file = File(application.filesDir, "data.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _gpsPoints = MutableStateFlow<List<GpsPoint>>(emptyList())
    val gpsPoints: StateFlow<List<GpsPoint>> = _gpsPoints

    init { loadData() }

    fun addCategory(category: Category) {
        _categories.value = _categories.value + category
        saveData()
    }

    fun addGpsPoint(point: GpsPoint) {
        _gpsPoints.value = _gpsPoints.value + point
        saveData()
    }

    private fun saveData() {
        try {
            val data = SaveData(_categories.value, _gpsPoints.value)
            file.writeText(json.encodeToString(data))
            Log.d("MainViewModel", "Données sauvegardées: $data")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Erreur de sauvegarde", e)
        }
    }

    private fun loadData() {
        try {
            if (file.exists()) {
                val data = json.decodeFromString<SaveData>(file.readText())
                _categories.value = data.categories
                _gpsPoints.value = data.gpsPoints
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Erreur de chargement", e)
        }
    }

    fun deleteCategory(category: Category) {
        val toDelete = mutableListOf(category)
        var i = 0
        while (i < toDelete.size) {
            val current = toDelete[i]
            toDelete += _categories.value.filter { it.parent == current.name }
            i++
        }
        _categories.value = _categories.value.filter { it !in toDelete }
        _gpsPoints.value = _gpsPoints.value.filter { it.category !in toDelete.map { c -> c.name } }
        saveData()
    }

    fun deleteGpsPoint(point: GpsPoint) {
        _gpsPoints.value = _gpsPoints.value - point
        saveData()
    }

    override fun onCleared() {
        super.onCleared()
        saveData()
    }
}