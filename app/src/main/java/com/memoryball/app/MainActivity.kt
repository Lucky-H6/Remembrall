package com.memoryball.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.memoryball.app.engine.LocationMonitorService
import com.memoryball.app.ui.edit.EditScreen
import com.memoryball.app.ui.home.HomeScreen
import com.memoryball.app.ui.map.MapScreen
import com.memoryball.app.ui.places.PlacesScreen
import com.memoryball.app.ui.settings.SettingsScreen
import com.memoryball.app.ui.theme.MemoryBallTheme

object Routes {
    const val HOME = "home"
    const val EDIT = "edit?reminderId={reminderId}"
    const val PLACES = "places"
    const val MAP = "map?placeId={placeId}"
    const val SETTINGS = "settings"

    fun edit(reminderId: Long = -1) = "edit?reminderId=$reminderId"
    fun map(placeId: Long = -1) = "map?placeId=$placeId"
}

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemoryBallTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Request essential permissions up front.
                    val permissions = rememberMultiplePermissionsState(
                        permissions = buildList {
                            add(Manifest.permission.ACCESS_FINE_LOCATION)
                            add(Manifest.permission.ACCESS_COARSE_LOCATION)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onPermissionsResult = {
                            LocationMonitorService.startIfNeeded(this@MainActivity)
                        }
                    )

                    LaunchedEffect(Unit) {
                        if (!permissions.allPermissionsGranted) {
                            permissions.launchMultiplePermissionRequest()
                        } else {
                            LocationMonitorService.startIfNeeded(this@MainActivity)
                        }
                    }

                    AppNavHost()
                }
            }
        }
    }
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewReminder = { nav.navigate(Routes.edit()) },
                onEditReminder = { id -> nav.navigate(Routes.edit(id)) },
                onPlaces = { nav.navigate(Routes.PLACES) },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("reminderId") { defaultValue = -1L; type = NavType.LongType })
        ) { entry ->
            val reminderId = entry.arguments?.getLong("reminderId") ?: -1L
            EditScreen(
                reminderId = reminderId,
                onBack = { nav.popBackStack() },
                onPickPlace = { nav.navigate(Routes.PLACES) }
            )
        }
        composable(Routes.PLACES) {
            PlacesScreen(
                onBack = { nav.popBackStack() },
                onNewPlace = { nav.navigate(Routes.map()) },
                onEditPlace = { id -> nav.navigate(Routes.map(id)) }
            )
        }
        composable(
            Routes.MAP,
            arguments = listOf(navArgument("placeId") { defaultValue = -1L; type = NavType.LongType })
        ) { entry ->
            val placeId = entry.arguments?.getLong("placeId") ?: -1L
            MapScreen(
                placeId = placeId,
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
