package com.example.tatratracker.ui.map

import org.maplibre.android.geometry.LatLng
import java.util.Locale

data class RoutePlanPoint(
    val name: String,
    val position: LatLng
)


data class RoutePointInput(
    val query: String = "",
    val point: RoutePlanPoint? = null
)


enum class RouteTargetType {

    START,

    WAYPOINT,

    END
}


data class RouteTarget(
    val type: RouteTargetType,
    val waypointIndex: Int? = null
) {

    companion object {

        val Start =
            RouteTarget(
                RouteTargetType.START
            )

        val End =
            RouteTarget(
                RouteTargetType.END
            )

        fun waypoint(
            index: Int
        ): RouteTarget {

            return RouteTarget(
                type =
                    RouteTargetType.WAYPOINT,

                waypointIndex =
                    index
            )
        }
    }
}


data class RoutePlanningState(

    val start:
    RoutePointInput =
        RoutePointInput(),

    val waypoints:
    List<RoutePointInput> =
        emptyList(),

    val end:
    RoutePointInput =
        RoutePointInput(),

    /*
     * Pole, dla którego aktualnie
     * pokazujemy autocomplete.
     */
    val activeSearchTarget:
    RouteTarget? =
        null,

    /*
     * Punkt, który użytkownik
     * aktualnie wskazuje na mapie.
     */
    val selectingOnMap:
    RouteTarget? =
        null
)


internal fun RoutePlanningState.inputFor(
    target: RouteTarget
): RoutePointInput {

    return when (
        target.type
    ) {

        RouteTargetType.START -> {

            start
        }

        RouteTargetType.END -> {

            end
        }

        RouteTargetType.WAYPOINT -> {

            val index =
                target.waypointIndex
                    ?: return RoutePointInput()

            waypoints.getOrElse(
                index
            ) {
                RoutePointInput()
            }
        }
    }
}


internal fun RoutePlanningState.updateQuery(
    target: RouteTarget,
    query: String
): RoutePlanningState {

    val input =
        RoutePointInput(
            query =
                query,

            /*
             * Jeśli użytkownik zaczyna
             * zmieniać tekst, wcześniejszy
             * wybrany punkt przestaje być
             * aktualny.
             */
            point =
                null
        )

    val updated =
        when (
            target.type
        ) {

            RouteTargetType.START -> {

                copy(
                    start =
                        input
                )
            }

            RouteTargetType.END -> {

                copy(
                    end =
                        input
                )
            }

            RouteTargetType.WAYPOINT -> {

                val index =
                    target.waypointIndex
                        ?: return this

                copy(
                    waypoints =
                        waypoints.mapIndexed {
                                waypointIndex,
                                waypoint ->

                            if (
                                waypointIndex ==
                                index
                            ) {

                                input

                            } else {

                                waypoint
                            }
                        }
                )
            }
        }

    return updated.copy(
        activeSearchTarget =
            target,

        selectingOnMap =
            null
    )
}


internal fun RoutePlanningState.setPoint(
    target: RouteTarget,
    point: RoutePlanPoint
): RoutePlanningState {

    val input =
        RoutePointInput(
            query =
                point.name,

            point =
                point
        )

    val updated =
        when (
            target.type
        ) {

            RouteTargetType.START -> {

                copy(
                    start =
                        input
                )
            }

            RouteTargetType.END -> {

                copy(
                    end =
                        input
                )
            }

            RouteTargetType.WAYPOINT -> {

                val index =
                    target.waypointIndex
                        ?: return this

                copy(
                    waypoints =
                        waypoints.mapIndexed {
                                waypointIndex,
                                waypoint ->

                            if (
                                waypointIndex ==
                                index
                            ) {

                                input

                            } else {

                                waypoint
                            }
                        }
                )
            }
        }

    return updated.copy(
        activeSearchTarget =
            null,

        selectingOnMap =
            null
    )
}


internal fun RoutePlanningState.clearPoint(
    target: RouteTarget
): RoutePlanningState {

    val emptyInput =
        RoutePointInput()

    val updated =
        when (
            target.type
        ) {

            RouteTargetType.START -> {

                copy(
                    start =
                        emptyInput
                )
            }

            RouteTargetType.END -> {

                copy(
                    end =
                        emptyInput
                )
            }

            RouteTargetType.WAYPOINT -> {

                val index =
                    target.waypointIndex
                        ?: return this

                copy(
                    waypoints =
                        waypoints.mapIndexed {
                                waypointIndex,
                                waypoint ->

                            if (
                                waypointIndex ==
                                index
                            ) {

                                emptyInput

                            } else {

                                waypoint
                            }
                        }
                )
            }
        }

    return updated.copy(
        activeSearchTarget =
            null,

        selectingOnMap =
            null
    )
}


internal fun RoutePlanningState.addWaypoint():
        RoutePlanningState {

    return copy(
        waypoints =
            waypoints +
                    RoutePointInput(),

        activeSearchTarget =
            null,

        selectingOnMap =
            null
    )
}


internal fun RoutePlanningState.removeWaypoint(
    index: Int
): RoutePlanningState {

    if (
        index !in waypoints.indices
    ) {
        return this
    }

    return copy(
        waypoints =
            waypoints.filterIndexed {
                    waypointIndex,
                    _ ->

                waypointIndex !=
                        index
            },

        activeSearchTarget =
            adjustTargetAfterWaypointRemoval(
                activeSearchTarget,
                index
            ),

        selectingOnMap =
            adjustTargetAfterWaypointRemoval(
                selectingOnMap,
                index
            )
    )
}


private fun adjustTargetAfterWaypointRemoval(
    target: RouteTarget?,
    removedIndex: Int
): RouteTarget? {

    if (
        target == null ||
        target.type !=
        RouteTargetType.WAYPOINT
    ) {

        return target
    }

    val index =
        target.waypointIndex
            ?: return null

    return when {

        index ==
                removedIndex -> {

            null
        }

        index >
                removedIndex -> {

            RouteTarget.waypoint(
                index - 1
            )
        }

        else -> {

            target
        }
    }
}


internal fun routePointFromMap(
    point: LatLng
): RoutePlanPoint {

    val name =
        String.format(
            Locale.US,
            "%.5f, %.5f",
            point.latitude,
            point.longitude
        )

    return RoutePlanPoint(
        name =
            name,

        position =
            point
    )
}