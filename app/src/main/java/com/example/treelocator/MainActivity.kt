package com.example.treelocator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.treelocator.ui.theme.TreeLocatorTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TreeLocatorTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        "gps" to Icons.Filled.GpsFixed,
        "categories" to Icons.Filled.Category
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(context.applicationContext as android.app.Application)
    )
    val LightGreen = Color(0xFF81C784)
    val TransparentLightGreen = LightGreen.copy(alpha = 0.0f)

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { (route, icon) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = {
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(imageVector = icon, contentDescription = route)},
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LightGreen,
                            selectedTextColor = LightGreen,
                            indicatorColor = TransparentLightGreen
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "gps",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("gps") { GpsPage(viewModel) }
            composable("categories") { CategoriesPage(viewModel) }
        }
    }
}