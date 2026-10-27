package com.example.tatratracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tatratracker.navigation.Screen
import com.example.tatratracker.ui.AboutScreen
import com.example.tatratracker.ui.ActivityDetailsScreen
import com.example.tatratracker.ui.BackupScreen
import com.example.tatratracker.ui.CompletedTrailsScreen
import com.example.tatratracker.ui.DataExportScreen
import com.example.tatratracker.ui.HistoryScreen
import com.example.tatratracker.ui.HomeScreen
import com.example.tatratracker.ui.ImportScreen
import com.example.tatratracker.ui.MapScreen
import com.example.tatratracker.ui.MoreScreen
import com.example.tatratracker.ui.MyRoutesScreen
import com.example.tatratracker.ui.OfflineMapsScreen
import com.example.tatratracker.ui.PlannedRouteDetailsScreen
import com.example.tatratracker.ui.SavedPlacesScreen
import com.example.tatratracker.ui.SettingsScreen
import com.example.tatratracker.ui.StatisticsScreen
import com.example.tatratracker.ui.components.TatraBottomBar
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraTrackerTheme

class MainActivity :
    ComponentActivity() {

    override fun onCreate(
        savedInstanceState:
        Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContent {

            TatraTrackerTheme {

                TatraTrackerApp()
            }
        }
    }
}

@Composable
private fun TatraTrackerApp() {

    val navController =
        rememberNavController()

    val navBackStackEntry by
    navController
        .currentBackStackEntryAsState()

    val currentRoute =
        navBackStackEntry
            ?.destination
            ?.route

    val secondaryRoutes =
        setOf(
            Screen.MyRoutes.route,
            Screen.CompletedTrails.route,
            Screen.SavedPlaces.route,
            Screen.OfflineMaps.route,
            Screen.Settings.route,
            Screen.DataExport.route,
            Screen.Backup.route,
            Screen.About.route,
            Screen.Import.route,
            Screen.ActivityDetails.route,
            Screen.PlannedRouteDetails.route,
            Screen.EditPlannedRoute.route
        )

    Scaffold(
        containerColor =
            TatraBackground,
        bottomBar = {

            if (
                currentRoute !in
                secondaryRoutes
            ) {

                TatraBottomBar(
                    currentRoute =
                        currentRoute,
                    onNavigate = {
                            route ->

                        navigateBottomBar(
                            navController =
                                navController,
                            route =
                                route
                        )
                    }
                )
            }
        }
    ) {
            innerPadding ->

        NavHost(
            navController =
                navController,
            startDestination =
                Screen.Home.route,
            modifier =
                Modifier.padding(
                    innerPadding
                )
        ) {

            composable(
                route =
                    Screen.Home.route
            ) {

                HomeScreen(
                    onNavigateToImport = {
                        navController.navigate(
                            Screen.Import.route
                        )
                    },
                    onNavigateToMap = {

                        navigateBottomBar(
                            navController =
                                navController,
                            route =
                                Screen.Map.route
                        )
                    },
                    onNavigateToHistory = {

                        navigateBottomBar(
                            navController =
                                navController,
                            route =
                                Screen.History.route
                        )
                    }
                )
            }

            composable(
                route =
                    Screen.Map.route
            ) {

                MapScreen()
            }

            composable(
                route =
                    Screen.History.route
            ) {

                HistoryScreen(
                    onActivityClick = {
                            activityId ->

                        navController.navigate(
                            Screen
                                .ActivityDetails
                                .createRoute(
                                    activityId
                                )
                        )
                    },
                    onPlannedRouteClick = {
                            routeId ->

                        navController.navigate(
                            Screen
                                .PlannedRouteDetails
                                .createRoute(
                                    routeId
                                )
                        )
                    },
                    onImportClick = {

                        navController.navigate(
                            Screen.Import.route
                        )
                    },
                    onOpenMap = {

                        navigateBottomBar(
                            navController =
                                navController,
                            route =
                                Screen.Map.route
                        )
                    }
                )
            }

            composable(
                route =
                    Screen.Statistics.route
            ) {

                StatisticsScreen(
                    onCompletedTrailsClick = {

                        navController.navigate(
                            Screen
                                .CompletedTrails
                                .route
                        )
                    }
                )
            }

            composable(
                route =
                    Screen.More.route
            ) {

                MoreScreen(
                    onMyRoutesClick = {

                        navController.navigate(
                            Screen.MyRoutes.route
                        )
                    },
                    onCompletedTrailsClick = {

                        navController.navigate(
                            Screen
                                .CompletedTrails
                                .route
                        )
                    },
                    onSavedPlacesClick = {

                        navController.navigate(
                            Screen
                                .SavedPlaces
                                .route
                        )
                    },
                    onOfflineMapsClick = {

                        navController.navigate(
                            Screen
                                .OfflineMaps
                                .route
                        )
                    },
                    onSettingsClick = {

                        navController.navigate(
                            Screen.Settings.route
                        )
                    },
                    onDataExportClick = {

                        navController.navigate(
                            Screen
                                .DataExport
                                .route
                        )
                    },
                    onBackupClick = {

                        navController.navigate(
                            Screen.Backup.route
                        )
                    },
                    onAboutClick = {

                        navController.navigate(
                            Screen.About.route
                        )
                    }
                )
            }

            composable(
                route =
                    Screen.Import.route
            ) {

                ImportScreen()
            }

            composable(
                route =
                    Screen.MyRoutes.route
            ) {

                MyRoutesScreen(
                    onBack = {
                        navController
                            .popBackStack()
                    },
                    onPlanRoute = {

                        navigateBottomBar(
                            navController =
                                navController,
                            route =
                                Screen.Map.route
                        )
                    },
                    onRouteClick = {
                            routeId ->

                        navController.navigate(
                            Screen
                                .PlannedRouteDetails
                                .createRoute(
                                    routeId
                                )
                        )
                    }
                )
            }

            composable(
                route =
                    Screen.CompletedTrails.route
            ) {

                CompletedTrailsScreen(
                    onBack = {
                        navController
                            .popBackStack()
                    },
                    onOpenMap = {

                        navigateBottomBar(
                            navController =
                                navController,
                            route =
                                Screen.Map.route
                        )
                    }
                )
            }

            composable(
                route =
                    Screen.SavedPlaces.route
            ) {

                SavedPlacesScreen(
                    onBack = {
                        navController
                            .popBackStack()
                    },
                    onOpenMap = {

                        navigateBottomBar(
                            navController =
                                navController,
                            route =
                                Screen.Map.route
                        )
                    }
                )
            }

            composable(
                route =
                    Screen.OfflineMaps.route
            ) {

                OfflineMapsScreen(
                    onBack = {
                        navController
                            .popBackStack()
                    }
                )
            }

            composable(
                route =
                    Screen.Settings.route
            ) {

                SettingsScreen(
                    onBack = {
                        navController
                            .popBackStack()
                    }
                )
            }

            composable(
                route =
                    Screen.DataExport.route
            ) {

                DataExportScreen(
                    onBack = {
                        navController
                            .popBackStack()
                    }
                )
            }

            composable(
                route =
                    Screen.Backup.route
            ) {

                BackupScreen(
                    onBack = {
                        navController
                            .popBackStack()
                    }
                )
            }

            composable(
                route =
                    Screen.About.route
            ) {

                AboutScreen(
                    onBack = {
                        navController
                            .popBackStack()
                    }
                )
            }

            composable(
                route =
                    Screen.ActivityDetails.route,
                arguments =
                    listOf(
                        navArgument(
                            "activityId"
                        ) {
                            type =
                                NavType.IntType
                        }
                    )
            ) {

                ActivityDetailsScreen(
                    onBack = {
                        navController
                            .popBackStack()
                    },
                    onDeleted = {

                        navController
                            .popBackStack(
                                Screen
                                    .History
                                    .route,
                                inclusive =
                                    false
                            )
                    }
                )
            }

            composable(
                route =
                    Screen
                        .PlannedRouteDetails
                        .route,
                arguments =
                    listOf(
                        navArgument(
                            "routeId"
                        ) {
                            type =
                                NavType.LongType
                        }
                    )
            ) {
                    backStackEntry ->

                val routeId =
                    backStackEntry
                        .arguments
                        ?.getLong(
                            "routeId"
                        )
                        ?: return@composable

                PlannedRouteDetailsScreen(
                    onBack = {
                        navController
                            .popBackStack()
                    },
                    onEdit = {

                        navController.navigate(
                            Screen
                                .EditPlannedRoute
                                .createRoute(
                                    routeId
                                )
                        )
                    },
                    onDeleted = {
                        navController
                            .popBackStack()
                    }
                )
            }

            composable(
                route =
                    Screen
                        .EditPlannedRoute
                        .route,
                arguments =
                    listOf(
                        navArgument(
                            "routeId"
                        ) {
                            type =
                                NavType.LongType
                        }
                    )
            ) {
                    backStackEntry ->

                val routeId =
                    backStackEntry
                        .arguments
                        ?.getLong(
                            "routeId"
                        )
                        ?: return@composable

                MapScreen(
                    editRouteId =
                        routeId,
                    onEditSaved = {
                        navController
                            .popBackStack()
                    }
                )
            }
        }
    }
}

private fun navigateBottomBar(
    navController:
    NavHostController,
    route: String
) {

    navController.navigate(
        route
    ) {

        popUpTo(
            Screen.Home.route
        ) {
            saveState =
                true
        }

        launchSingleTop =
            true

        restoreState =
            true
    }
}