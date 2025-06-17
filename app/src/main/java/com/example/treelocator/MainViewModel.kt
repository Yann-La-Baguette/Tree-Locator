// MainViewModel.kt
package com.example.treelocator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.FileOutputStream
import java.io.FileInputStream

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

    fun exportToExcel() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val workbook = XSSFWorkbook()
                // Feuille Catégories
                val catSheet = workbook.createSheet("Catégories")
                val catHeader = catSheet.createRow(0)
                val context = getApplication<Application>()
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                catHeader.createCell(0).setCellValue("Nom")
                catHeader.createCell(1).setCellValue("Parent")
                _categories.value.forEachIndexed { i, cat ->
                    val row = catSheet.createRow(i + 1)
                    row.createCell(0).setCellValue(cat.name)
                    row.createCell(1).setCellValue(cat.parent ?: "")
                }
                // Feuille Points GPS
                val pointSheet = workbook.createSheet("Points GPS")
                val pointHeader = pointSheet.createRow(0)
                pointHeader.createCell(0).setCellValue("Nom")
                pointHeader.createCell(1).setCellValue("Catégorie")
                pointHeader.createCell(2).setCellValue("Latitude")
                pointHeader.createCell(3).setCellValue("Longitude")
                pointHeader.createCell(4).setCellValue("Date")
                _gpsPoints.value.forEachIndexed { i, pt ->
                    val row = pointSheet.createRow(i + 1)
                    row.createCell(0).setCellValue(pt.name)
                    row.createCell(1).setCellValue(pt.category ?: "")
                    row.createCell(2).setCellValue(pt.latitude)
                    row.createCell(3).setCellValue(pt.longitude)
                    row.createCell(4).setCellValue(pt.date ?: "")
                }
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val excelFile = File(downloadsDir, "export.xlsx")
                FileOutputStream(excelFile).use { workbook.write(it) }
                workbook.close()
                Log.d("MainViewModel", "Export Excel réussi: ${excelFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Erreur export Excel", e)
            }
        }
    }

    fun importFromExcel() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val excelFile = File(getApplication<Application>().filesDir, "export.xlsx")
                if (!excelFile.exists()) return@launch
                FileInputStream(excelFile).use { fis ->
                    val workbook = WorkbookFactory.create(fis)
                    // Catégories
                    val catSheet = workbook.getSheet("Catégories")
                    val newCats = mutableListOf<Category>()
                    for (i in 1..catSheet.lastRowNum) {
                        val row = catSheet.getRow(i)
                        val name = row.getCell(0)?.stringCellValue ?: continue
                        val parent = row.getCell(1)?.stringCellValue?.takeIf { it.isNotBlank() }
                        newCats.add(Category(name, parent))
                    }
                    // Points GPS
                    val pointSheet = workbook.getSheet("Points GPS")
                    val newPoints = mutableListOf<GpsPoint>()
                    for (i in 1..pointSheet.lastRowNum) {
                        val row = pointSheet.getRow(i)
                        val name = row.getCell(0)?.stringCellValue ?: continue
                        val category = row.getCell(1)?.stringCellValue
                        val lat = row.getCell(2)?.numericCellValue ?: 0.0
                        val lon = row.getCell(3)?.numericCellValue ?: 0.0
                        val date = row.getCell(4)?.stringCellValue
                        newPoints.add(GpsPoint(name, lat, lon, category, date))
                    }
                    workbook.close()
                    _categories.value = newCats
                    _gpsPoints.value = newPoints
                    saveData()
                    Log.d("MainViewModel", "Import Excel réussi")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Erreur import Excel", e)
            }
        }
    }

    fun importFromExcelUri(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openInputStream(uri)?.use { fis ->
                    val workbook = WorkbookFactory.create(fis)
                    // Catégories
                    val catSheet = workbook.getSheet("Catégories")
                    val newCats = mutableListOf<Category>()
                    for (i in 1..catSheet.lastRowNum) {
                        val row = catSheet.getRow(i)
                        val name = row.getCell(0)?.stringCellValue ?: continue
                        val parent = row.getCell(1)?.stringCellValue?.takeIf { it.isNotBlank() }
                        newCats.add(Category(name, parent))
                    }
                    // Points GPS
                    val pointSheet = workbook.getSheet("Points GPS")
                    val newPoints = mutableListOf<GpsPoint>()
                    for (i in 1..pointSheet.lastRowNum) {
                        val row = pointSheet.getRow(i)
                        val name = row.getCell(0)?.stringCellValue ?: continue
                        val category = row.getCell(1)?.stringCellValue
                        val lat = row.getCell(2)?.numericCellValue ?: 0.0
                        val lon = row.getCell(3)?.numericCellValue ?: 0.0
                        val date = row.getCell(4)?.stringCellValue
                        newPoints.add(GpsPoint(name, lat, lon, category, date))
                    }
                    workbook.close()
                    _categories.value = newCats
                    _gpsPoints.value = newPoints
                    saveData()
                    Log.d("MainViewModel", "Import Excel réussi")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Erreur import Excel", e)
            }
        }
    }
}