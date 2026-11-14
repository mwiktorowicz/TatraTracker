package com.example.tatratracker.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tatratracker.ui.format.TatraFormat
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen

private val RouteEndColor =
    Color(
        0xFFD65148
    )

private val WaypointColor =
    Color(
        0xFFE09D31
    )

private val RouteSummaryBackground =
    Color(
        0xFFF0F5E8
    )


@Composable
internal fun RoutePlanningPanel(
    state: RoutePlanningState,
    searchResults: List<MapSearchResult>,
    searchLoading: Boolean,
    searchError: String?,
    plannedRoute: PlannedRoute?,
    routeLoading: Boolean,
    routeError: String?,
    elevationProfile: RouteElevationProfile?,
    elevationLoading: Boolean,
    elevationError: String?,
    selectedElevationSample: RouteElevationSample?,
    onElevationSampleSelected: (
        RouteElevationSample?
    ) -> Unit,
    onQueryChange:
        (
        RouteTarget,
        String
    ) -> Unit,
    onSearchResultSelected:
        (
        RouteTarget,
        MapSearchResult
    ) -> Unit,
    onSelectOnMap:
        (RouteTarget) -> Unit,
    onClear:
        (RouteTarget) -> Unit,
    onAddWaypoint:
        () -> Unit,
    onRemoveWaypoint:
        (Int) -> Unit,
    onCalculateRoute:
        () -> Unit,
    onClearRoute:
        () -> Unit,
    onClose:
        () -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier =
            modifier,

        shape =
            RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            ),

        color =
            Color.White.copy(
                alpha = 0.99f
            ),

        shadowElevation =
            16.dp
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(
                        rememberScrollState()
                    )
        ) {

            /*
             * =================================================
             * UCHWYT
             * =================================================
             */

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 9.dp,
                            bottom = 2.dp
                        ),

                horizontalArrangement =
                    Arrangement.Center
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                width = 42.dp,
                                height = 4.dp
                            )
                            .background(
                                color =
                                    Color(
                                        0xFFD2D7D3
                                    ),

                                shape =
                                    RoundedCornerShape(
                                        50
                                    )
                            )
                )
            }

            /*
             * =================================================
             * NAGŁÓWEK
             * =================================================
             */

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 18.dp,
                            top = 2.dp,
                            end = 6.dp,
                            bottom = 6.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(
                        text =
                            "Zaplanuj trasę",

                        color =
                            TatraDark,

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            18.sp
                    )

                    Text(
                        text =
                            "Wyszukaj miejsce lub wskaż je na mapie",

                        color =
                            Color(
                                0xFF687276
                            ),

                        fontSize =
                            11.sp
                    )
                }

                IconButton(
                    onClick =
                        onClose
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Close,

                        contentDescription =
                            "Zamknij",

                        tint =
                            TatraDark
                    )
                }
            }

            HorizontalDivider()

            RoutePointEditor(
                title = "Start",
                color = TatraGreen,
                target = RouteTarget.Start,
                input = state.start,
                isSearching =
                    state.activeSearchTarget ==
                            RouteTarget.Start,
                selectingOnMap =
                    state.selectingOnMap ==
                            RouteTarget.Start,
                searchResults = searchResults,
                searchLoading = searchLoading,
                searchError = searchError,
                onQueryChange = onQueryChange,
                onSearchResultSelected =
                    onSearchResultSelected,
                onSelectOnMap = onSelectOnMap,
                onClear = onClear
            )

            state.waypoints
                .forEachIndexed {
                        index,
                        waypoint ->

                    HorizontalDivider(
                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp
                            )
                    )

                    val target =
                        RouteTarget.waypoint(
                            index
                        )

                    RoutePointEditor(
                        title =
                            "Przystanek ${index + 1}",
                        color =
                            WaypointColor,
                        target =
                            target,
                        input =
                            waypoint,
                        isSearching =
                            state.activeSearchTarget ==
                                    target,
                        selectingOnMap =
                            state.selectingOnMap ==
                                    target,
                        searchResults =
                            searchResults,
                        searchLoading =
                            searchLoading,
                        searchError =
                            searchError,
                        onQueryChange =
                            onQueryChange,
                        onSearchResultSelected =
                            onSearchResultSelected,
                        onSelectOnMap =
                            onSelectOnMap,
                        onClear =
                            onClear,
                        onRemove = {

                            onRemoveWaypoint(
                                index
                            )
                        }
                    )
                }

            OutlinedButton(
                onClick =
                    onAddWaypoint,

                modifier =
                    Modifier
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                        .fillMaxWidth()
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Add,

                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )

                Text(
                    text =
                        "Dodaj przystanek"
                )
            }

            HorizontalDivider(
                modifier =
                    Modifier.padding(
                        horizontal = 16.dp
                    )
            )

            RoutePointEditor(
                title = "Cel",
                color = RouteEndColor,
                target = RouteTarget.End,
                input = state.end,
                isSearching =
                    state.activeSearchTarget ==
                            RouteTarget.End,
                selectingOnMap =
                    state.selectingOnMap ==
                            RouteTarget.End,
                searchResults = searchResults,
                searchLoading = searchLoading,
                searchError = searchError,
                onQueryChange = onQueryChange,
                onSearchResultSelected =
                    onSearchResultSelected,
                onSelectOnMap = onSelectOnMap,
                onClear = onClear
            )

            if (
                plannedRoute != null
            ) {

                RouteSummary(
                    route =
                        plannedRoute
                )
            }

            /*
             * =================================================
             * ELEVATION
             * =================================================
             */

            if (
                elevationLoading
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                16.dp
                            ),

                    horizontalArrangement =
                        Arrangement.Center,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                18.dp
                            ),

                        strokeWidth =
                            2.dp,

                        color =
                            TatraGreen
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                9.dp
                            )
                    )

                    Text(
                        text =
                            "Obliczam profil wysokościowy...",

                        color =
                            Color(
                                0xFF687276
                            ),

                        fontSize =
                            12.sp
                    )
                }
            }

            if (
                elevationProfile != null
            ) {

                ElevationSummary(
                    profile =
                        elevationProfile,

                    selectedSample =
                        selectedElevationSample,

                    onSampleSelected =
                        onElevationSampleSelected
                )
            }

            if (
                elevationError != null
            ) {

                Text(
                    text =
                        elevationError,

                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 6.dp
                        ),

                    color =
                        RouteEndColor,

                    fontSize =
                        11.sp
                )
            }

            if (
                routeError != null
            ) {

                Text(
                    text =
                        routeError,

                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 6.dp
                        ),

                    color =
                        RouteEndColor,

                    fontSize =
                        12.sp
                )
            }

            val routeReady =
                state.start.point != null &&
                        state.end.point != null

            Button(
                onClick =
                    onCalculateRoute,

                enabled =
                    routeReady &&
                            !routeLoading,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp
                        ),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            TatraGreen
                    )
            ) {

                if (
                    routeLoading
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                19.dp
                            ),

                        strokeWidth =
                            2.dp,

                        color =
                            Color.White
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                9.dp
                            )
                    )

                    Text(
                        text =
                            "Wyznaczam..."
                    )

                } else {

                    Text(
                        text =
                            if (
                                plannedRoute == null
                            ) {

                                "Wyznacz trasę"

                            } else {

                                "Przelicz trasę"
                            }
                    )
                }
            }

            if (
                plannedRoute != null
            ) {

                TextButton(
                    onClick =
                        onClearRoute,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                bottom = 12.dp
                            )
                ) {

                    Text(
                        text =
                            "Usuń wyznaczoną trasę"
                    )
                }

            } else {

                Spacer(
                    modifier =
                        Modifier.padding(
                            bottom = 16.dp
                        )
                )
            }
        }
    }
}


@Composable
private fun ElevationSummary(
    profile: RouteElevationProfile,
    selectedSample: RouteElevationSample?,
    onSampleSelected: (
        RouteElevationSample?
    ) -> Unit
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 6.dp
                ),

        shape =
            RoundedCornerShape(
                14.dp
            ),

        color =
            Color(
                0xFFF7F8F3
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    14.dp
                )
        ) {

            Text(
                text =
                    "Profil wysokościowy",

                color =
                    TatraDark,

                fontWeight =
                    FontWeight.Bold,

                fontSize =
                    14.sp
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp
                        ),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                ElevationStat(
                    title =
                        "Podejście",

                    value =
                        "+${TatraFormat.elevation(profile.totalAscentMeters)}"
                )

                ElevationStat(
                    title =
                        "Zejście",

                    value =
                        "-${TatraFormat.elevation(profile.totalDescentMeters)}"
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp
                        ),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                ElevationStat(
                    title =
                        "Najwyżej",

                    value =
                        TatraFormat.elevation(profile.highestElevationMeters)
                )

                ElevationStat(
                    title =
                        "Najniżej",

                    value =
                        TatraFormat.elevation(profile.lowestElevationMeters)
                )
            }

            /*
             * =================================================
             * WYBRANY PUNKT
             * =================================================
             */

            if (
                selectedSample != null
            ) {

                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 12.dp
                            ),

                    shape =
                        RoundedCornerShape(
                            10.dp
                        ),

                    color =
                        Color(
                            0xFFEAF2DE
                        )
                ) {

                    Text(
                        text =
                            TatraFormat.distance(selectedSample.distanceMeters) +
                                "  •  " +
                                TatraFormat.altitude(selectedSample.elevationMeters),

                        modifier =
                            Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            ),

                        color =
                            TatraDark,

                        fontWeight =
                            FontWeight.SemiBold,

                        fontSize =
                            13.sp
                    )
                }
            }

            ElevationProfileChart(
                profile =
                    profile,

                selectedSample =
                    selectedSample,

                onSampleSelected =
                    onSampleSelected,

                modifier =
                    Modifier.padding(
                        top = 6.dp
                    )
            )

            val totalDistanceMeters =
                profile.samples
                    .lastOrNull()
                    ?.distanceMeters
                    ?: 0.0

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text =
                        "0 km",

                    color =
                        Color(
                            0xFF7A8284
                        ),

                    fontSize =
                        9.sp
                )

                Text(
                    text =
                        TatraFormat.distance(totalDistanceMeters),

                    color =
                        Color(
                            0xFF7A8284
                        ),

                    fontSize =
                        9.sp
                )
            }

            Text(
                text =
                    "Wysokość: Copernicus DEM / Open-Meteo",

                modifier =
                    Modifier.padding(
                        top = 8.dp
                    ),

                color =
                    Color(
                        0xFF92999A
                    ),

                fontSize =
                    8.sp
            )
        }
    }
}


@Composable
private fun ElevationStat(
    title: String,
    value: String
) {

    Column(
        modifier =
            Modifier.width(
                118.dp
            )
    ) {

        Text(
            text =
                title,

            color =
                Color(
                    0xFF747C7E
                ),

            fontSize =
                10.sp
        )

        Text(
            text =
                value,

            modifier =
                Modifier.padding(
                    top = 2.dp
                ),

            color =
                TatraDark,

            fontWeight =
                FontWeight.Bold,

            fontSize =
                15.sp
        )
    }
}


@Composable
private fun RoutePointEditor(
    title: String,
    color: Color,
    target: RouteTarget,
    input: RoutePointInput,
    isSearching: Boolean,
    selectingOnMap: Boolean,
    searchResults: List<MapSearchResult>,
    searchLoading: Boolean,
    searchError: String?,
    onQueryChange:
        (
        RouteTarget,
        String
    ) -> Unit,
    onSearchResultSelected:
        (
        RouteTarget,
        MapSearchResult
    ) -> Unit,
    onSelectOnMap:
        (RouteTarget) -> Unit,
    onClear:
        (RouteTarget) -> Unit,
    onRemove:
    (() -> Unit)? = null
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                )
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            11.dp
                        )
                        .background(
                            color =
                                color,

                            shape =
                                CircleShape
                        )
            )

            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )

            Text(
                text =
                    title,

                modifier =
                    Modifier.weight(
                        1f
                    ),

                color =
                    TatraDark,

                fontWeight =
                    FontWeight.SemiBold,

                fontSize =
                    14.sp
            )

            if (
                onRemove != null
            ) {

                IconButton(
                    onClick =
                        onRemove
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Delete,

                        contentDescription =
                            "Usuń przystanek",

                        tint =
                            Color(
                                0xFF8A8F91
                            )
                    )
                }
            }
        }

        OutlinedTextField(
            value =
                input.query,

            onValueChange = {

                onQueryChange(
                    target,
                    it
                )
            },

            modifier =
                Modifier.fillMaxWidth(),

            singleLine =
                true,

            placeholder = {

                Text(
                    text =
                        "np. Świnica"
                )
            },

            trailingIcon = {

                if (
                    searchLoading &&
                    isSearching
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                18.dp
                            ),

                        strokeWidth =
                            2.dp
                    )

                } else {

                    Icon(
                        imageVector =
                            Icons.Outlined.Search,

                        contentDescription =
                            null
                    )
                }
            }
        )

        if (
            isSearching &&
            input.query
                .trim()
                .length >= 2
        ) {

            if (
                searchError != null
            ) {

                Text(
                    text =
                        searchError,

                    modifier =
                        Modifier.padding(
                            top = 6.dp
                        ),

                    color =
                        RouteEndColor,

                    fontSize =
                        11.sp
                )
            }

            if (
                searchResults.isNotEmpty()
            ) {

                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 4.dp
                            ),

                    shape =
                        RoundedCornerShape(
                            12.dp
                        ),

                    color =
                        Color(
                            0xFFF6F7F4
                        ),

                    tonalElevation =
                        2.dp
                ) {

                    Column {

                        searchResults
                            .forEachIndexed {
                                    index,
                                    result ->

                                SearchResultRow(
                                    result =
                                        result,

                                    onClick = {

                                        onSearchResultSelected(
                                            target,
                                            result
                                        )
                                    }
                                )

                                if (
                                    index <
                                    searchResults.lastIndex
                                ) {

                                    HorizontalDivider(
                                        modifier =
                                            Modifier.padding(
                                                horizontal = 12.dp
                                            )
                                    )
                                }
                            }
                    }
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 4.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            TextButton(
                onClick = {

                    onSelectOnMap(
                        target
                    )
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Map,

                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            5.dp
                        )
                )

                Text(
                    text =
                        if (
                            selectingOnMap
                        ) {

                            "Dotknij miejsca na mapie"

                        } else {

                            "Wybierz na mapie"
                        }
                )
            }

            Spacer(
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            if (
                input.point != null ||
                input.query.isNotBlank()
            ) {

                TextButton(
                    onClick = {

                        onClear(
                            target
                        )
                    }
                ) {

                    Text(
                        text =
                            "Wyczyść"
                    )
                }
            }
        }
    }
}


@Composable
private fun SearchResultRow(
    result: MapSearchResult,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onClick
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                Icons.Outlined.Map,

            contentDescription =
                null,

            tint =
                TatraGreen
        )

        Spacer(
            modifier =
                Modifier.width(
                    10.dp
                )
        )

        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                text =
                    result.name,

                color =
                    TatraDark,

                fontWeight =
                    FontWeight.SemiBold,

                fontSize =
                    13.sp
            )

            if (
                result.fullName !=
                result.name
            ) {

                Text(
                    text =
                        result.fullName,

                    color =
                        Color(
                            0xFF687276
                        ),

                    fontSize =
                        10.5.sp,

                    maxLines =
                        2
                )
            }

            if (
                result.type.isNotBlank()
            ) {

                Text(
                    text =
                        result.type,

                    color =
                        TatraGreen,

                    fontSize =
                        9.5.sp
                )
            }
        }
    }
}


@Composable
private fun RouteSummary(
    route: PlannedRoute
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),

        shape =
            RoundedCornerShape(
                14.dp
            ),

        color =
            RouteSummaryBackground
    ) {

        Column(
            modifier =
                Modifier.padding(
                    14.dp
                )
        ) {

            Text(
                text =
                    "Trasa gotowa",

                color =
                    TatraDark,

                fontWeight =
                    FontWeight.Bold,

                fontSize =
                    14.sp
            )

            Text(
                text =
                    TatraFormat.distance(route.distanceKm * 1000.0),

                modifier =
                    Modifier.padding(
                        top = 5.dp
                    ),

                color =
                    TatraGreen,

                fontWeight =
                    FontWeight.Bold,

                fontSize =
                    21.sp
            )

            Text(
                text =
                    "Szacowany czas: " +
                            TatraFormat.duration(route.durationSeconds),

                color =
                    Color(
                        0xFF687276
                    ),

                fontSize =
                    12.sp
            )

            if (
                route.legCount > 1
            ) {

                Text(
                    text =
                        "Odcinki trasy: ${route.legCount}",

                    color =
                        Color(
                            0xFF687276
                        ),

                    fontSize =
                        11.sp
                )
            }
        }
    }
}