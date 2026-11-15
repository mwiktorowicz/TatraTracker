package com.example.tatratracker.navigation

sealed class Screen(
    val route: String
) {

    object Home : Screen(
        "home_screen"
    )

    object Map : Screen(
        "map_screen"
    )

    object History : Screen(
        "history_screen"
    )

    object Statistics : Screen(
        "statistics_screen"
    )

    object More : Screen(
        "more_screen"
    )

    object Import : Screen(
        "import_screen"
    )

    object MyRoutes : Screen(
        "my_routes_screen"
    )

    object CompletedTrails : Screen(
        "completed_trails_screen"
    )

    object SavedPlaces : Screen(
        "saved_places_screen"
    )

    object OfflineMaps : Screen(
        "offline_maps_screen"
    )

    object Settings : Screen(
        "settings_screen"
    )

    object DataExport : Screen(
        "data_export_screen"
    )

    object Backup : Screen(
        "backup_screen"
    )

    object About : Screen(
        "about_screen"
    )

    object ActivityDetails : Screen(
        "activity_details/{activityId}"
    ) {

        fun createRoute(
            activityId: Int
        ): String {
            return "activity_details/$activityId"
        }
    }

    // Pełnoekranowe odtwarzanie wędrówki (bez dolnego paska).
    object ActivityReplay : Screen("activity_replay/{activityId}") {

        fun createRoute(activityId: Int): String {
            return "activity_replay/$activityId"
        }
    }

    object PlannedRouteDetails : Screen(
        "planned_route_details/{routeId}"
    ) {

        fun createRoute(
            routeId: Long
        ): String {
            return "planned_route_details/$routeId"
        }
    }

    object EditPlannedRoute : Screen(
        "edit_planned_route/{routeId}"
    ) {

        fun createRoute(
            routeId: Long
        ): String {
            return "edit_planned_route/$routeId"
        }
    }
}