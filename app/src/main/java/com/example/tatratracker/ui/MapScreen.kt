package com.example.tatratracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.BuildConfig
import com.example.tatratracker.data.PlannedRouteDraft
import com.example.tatratracker.data.PlannedRouteDraftPoint
import com.example.tatratracker.data.PlannedRoutePointEntity
import com.example.tatratracker.data.PlannedRoutePointType
import com.example.tatratracker.ui.components.TatraFeatureDialog
import com.example.tatratracker.ui.map.AddOwnPlaceDialog
import com.example.tatratracker.ui.map.MapLayerSettings
import com.example.tatratracker.ui.map.MapLayersPanel
import com.example.tatratracker.ui.map.MapPoiToolsPanel
import com.example.tatratracker.ui.map.MapRoundButton
import com.example.tatratracker.ui.map.MapRouteQuickActions
import com.example.tatratracker.ui.map.MapSearchRepository
import com.example.tatratracker.ui.map.MapSearchResult
import com.example.tatratracker.ui.map.MapTrailToolsPanel
import com.example.tatratracker.ui.map.MapViewLifecycle
import com.example.tatratracker.ui.map.PlannedRoute
import com.example.tatratracker.ui.map.PlannedRouteActionsDialog
import com.example.tatratracker.ui.map.RouteElevationProfile
import com.example.tatratracker.ui.map.RouteElevationRepository
import com.example.tatratracker.ui.map.RouteElevationSample
import com.example.tatratracker.ui.map.RoutePlanPoint
import com.example.tatratracker.ui.map.RoutePlannerBottomSheet
import com.example.tatratracker.ui.map.RoutePlannerSheetLevel
import com.example.tatratracker.ui.map.RoutePlanningPanel
import com.example.tatratracker.ui.map.RoutePlanningState
import com.example.tatratracker.ui.map.RoutePointInput
import com.example.tatratracker.ui.map.RouteRoutingRepository
import com.example.tatratracker.ui.map.SavePlannedRouteDialog
import com.example.tatratracker.ui.map.TATRA_CENTER
import com.example.tatratracker.ui.map.addElevationMarkerLayers
import com.example.tatratracker.ui.map.addPlannedRouteLayers
import com.example.tatratracker.ui.map.addRouteSelectionLayers
import com.example.tatratracker.ui.map.addTatraMapLayers
import com.example.tatratracker.ui.map.addWaypoint
import com.example.tatratracker.ui.map.applyMapLayerVisibility
import com.example.tatratracker.ui.map.clearPoint
import com.example.tatratracker.ui.map.inputFor
import com.example.tatratracker.ui.map.landscapeStyleUrl
import com.example.tatratracker.ui.map.removeWaypoint
import com.example.tatratracker.ui.map.routePointFromMap
import com.example.tatratracker.ui.map.setPoint
import com.example.tatratracker.ui.map.updateElevationMarker
import com.example.tatratracker.ui.map.updatePlannedRoute
import com.example.tatratracker.ui.map.updateQuery
import com.example.tatratracker.ui.map.updateRouteSelection
import com.example.tatratracker.ui.map.updateUserTracks
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.viewmodel.MapViewModel
import com.example.tatratracker.viewmodel.PlannedRouteEditData
import com.example.tatratracker.viewmodel.PlannedRoutesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import kotlin.math.roundToInt

@Composable
fun MapScreen(
    mapViewModel:
    MapViewModel =
        viewModel(),
    plannedRoutesViewModel:
    PlannedRoutesViewModel =
        viewModel(),
    editRouteId: Long? = null,
    onEditSaved: (() -> Unit)? = null
) {

    val context =
        LocalContext.current

    val coroutineScope =
        rememberCoroutineScope()

    val tracks by
    mapViewModel
        .tracks
        .collectAsState()

    val apiKey =
        BuildConfig.MAPTILER_API_KEY

    val isEditMode =
        editRouteId != null

    var editingRouteName by
    remember(
        editRouteId
    ) {
        mutableStateOf<String?>(
            null
        )
    }

    var editLoading by
    remember(
        editRouteId
    ) {
        mutableStateOf(
            editRouteId != null
        )
    }

    var mapLibreMap by
    remember {
        mutableStateOf<MapLibreMap?>(
            null
        )
    }

    var loadedStyle by
    remember {
        mutableStateOf<Style?>(
            null
        )
    }

    var mapLoading by
    remember {
        mutableStateOf(
            true
        )
    }

    var showLayersPanel by
    remember {
        mutableStateOf(
            false
        )
    }

    var layerSettings by
    remember {
        mutableStateOf(
            MapLayerSettings()
        )
    }

    var showPoiPanel by
    remember {
        mutableStateOf(
            false
        )
    }

    var showTrailToolsPanel by
    remember {
        mutableStateOf(
            false
        )
    }

    var showSaveRouteDialog by
    remember {
        mutableStateOf(
            false
        )
    }

    var showRouteActionsDialog by
    remember {
        mutableStateOf(
            false
        )
    }

    var showAddOwnPlaceDialog by
    remember {
        mutableStateOf(
            false
        )
    }

    var featureDialog by
    remember {
        mutableStateOf<
                Pair<String, String>?
                >(
            null
        )
    }

    var showRoutePlanner by
    remember {
        mutableStateOf(
            false
        )
    }

    var routePlanningState by
    remember {
        mutableStateOf(
            RoutePlanningState()
        )
    }

    var routeSheetLevel by
    remember {
        mutableStateOf(
            RoutePlannerSheetLevel.MEDIUM
        )
    }

    val routingRepository =
        remember {
            RouteRoutingRepository()
        }

    var plannedRoute by
    remember {
        mutableStateOf<PlannedRoute?>(
            null
        )
    }

    var routeLoading by
    remember {
        mutableStateOf(
            false
        )
    }

    var routeError by
    remember {
        mutableStateOf<String?>(
            null
        )
    }

    val elevationRepository =
        remember {
            RouteElevationRepository()
        }

    var elevationProfile by
    remember {
        mutableStateOf<RouteElevationProfile?>(
            null
        )
    }

    var elevationLoading by
    remember {
        mutableStateOf(
            false
        )
    }

    var elevationError by
    remember {
        mutableStateOf<String?>(
            null
        )
    }

    var selectedElevationSample by
    remember {
        mutableStateOf<RouteElevationSample?>(
            null
        )
    }

    var routeCalculationVersion by
    remember {
        mutableStateOf(
            0
        )
    }

    fun invalidateCalculatedRoute() {

        routeCalculationVersion++

        plannedRoute =
            null

        routeError =
            null

        routeLoading =
            false

        elevationProfile =
            null

        elevationError =
            null

        elevationLoading =
            false

        selectedElevationSample =
            null
    }

    LaunchedEffect(
        editRouteId
    ) {

        val routeId =
            editRouteId
                ?: return@LaunchedEffect

        editLoading =
            true

        plannedRoutesViewModel
            .loadRouteForEditing(
                routeId
            )
            .onSuccess {
                    editData ->

                editingRouteName =
                    editData.route.name

                routePlanningState =
                    editData
                        .toRoutePlanningState()

                plannedRoute =
                    editData
                        .toPlannedRoute()

                elevationProfile =
                    editData
                        .toElevationProfile()

                routeError =
                    if (
                        plannedRoute == null
                    ) {
                        "Zapisana trasa nie zawiera poprawnej geometrii."
                    } else {
                        null
                    }

                showRoutePlanner =
                    true

                routeSheetLevel =
                    RoutePlannerSheetLevel.COMPACT
            }
            .onFailure {
                    error ->

                featureDialog =
                    "Nie udało się otworzyć trasy" to
                            (
                                    error.message
                                        ?: "Nieznany błąd odczytu."
                                    )
            }

        editLoading =
            false
    }

    val searchRepository =
        remember(
            apiKey
        ) {
            MapSearchRepository(
                apiKey
            )
        }

    var searchResults by
    remember {
        mutableStateOf<
                List<MapSearchResult>
                >(
            emptyList()
        )
    }

    var searchLoading by
    remember {
        mutableStateOf(
            false
        )
    }

    var searchError by
    remember {
        mutableStateOf<String?>(
            null
        )
    }

    val activeSearchTarget =
        routePlanningState
            .activeSearchTarget

    val activeSearchQuery =
        activeSearchTarget
            ?.let {
                routePlanningState
                    .inputFor(
                        it
                    )
                    .query
            }
            .orEmpty()

    LaunchedEffect(
        activeSearchTarget,
        activeSearchQuery
    ) {

        if (
            activeSearchTarget == null ||
            activeSearchQuery
                .trim()
                .length < 2
        ) {

            searchResults =
                emptyList()

            searchLoading =
                false

            searchError =
                null

            return@LaunchedEffect
        }

        delay(
            350
        )

        searchLoading =
            true

        searchError =
            null

        val proximity =
            mapLibreMap
                ?.cameraPosition
                ?.target
                ?: TATRA_CENTER

        val response =
            searchRepository.search(
                query =
                    activeSearchQuery,
                proximity =
                    proximity
            )

        searchResults =
            response.results

        searchError =
            response.error

        searchLoading =
            false
    }

    val mapStyleUrl =
        remember(
            apiKey
        ) {
            landscapeStyleUrl(
                apiKey
            )
        }

    val mapView =
        remember(
            context
        ) {

            MapLibre.getInstance(
                context.applicationContext
            )

            MapView(
                context
            ).apply {
                onCreate(
                    null
                )
            }
        }

    MapViewLifecycle(
        context =
            context,
        mapView =
            mapView
    )

    LaunchedEffect(
        mapView
    ) {

        if (
            apiKey.isBlank()
        ) {

            mapLoading =
                false

            return@LaunchedEffect
        }

        mapView
            .addOnDidFailLoadingMapListener {

                mapLoading =
                    false
            }

        mapView.getMapAsync {
                map ->

            mapLibreMap =
                map

            map.cameraPosition =
                CameraPosition
                    .Builder()
                    .target(
                        TATRA_CENTER
                    )
                    .zoom(
                        10.3
                    )
                    .build()

            map.uiSettings
                .setCompassEnabled(
                    false
                )

            map.setStyle(
                Style
                    .Builder()
                    .fromUri(
                        mapStyleUrl
                    )
            ) {
                    style ->

                addTatraMapLayers(
                    context =
                        context,
                    style =
                        style,
                    apiKey =
                        apiKey,
                    tracks =
                        tracks
                )

                addRouteSelectionLayers(
                    style
                )

                addPlannedRouteLayers(
                    style
                )

                addElevationMarkerLayers(
                    style
                )

                updateRouteSelection(
                    style =
                        style,
                    state =
                        routePlanningState
                )

                updatePlannedRoute(
                    style =
                        style,
                    route =
                        plannedRoute
                )

                updateElevationMarker(
                    style =
                        style,
                    sample =
                        selectedElevationSample
                )

                applyMapLayerVisibility(
                    style =
                        style,
                    settings =
                        layerSettings
                )

                loadedStyle =
                    style

                mapLoading =
                    false
            }
        }
    }

    DisposableEffect(
        mapLibreMap,
        routePlanningState
            .selectingOnMap
    ) {

        val map =
            mapLibreMap

        val target =
            routePlanningState
                .selectingOnMap

        if (
            map == null ||
            target == null
        ) {

            onDispose {}

        } else {

            val listener =
                MapLibreMap
                    .OnMapClickListener {
                            point ->

                        invalidateCalculatedRoute()

                        routePlanningState =
                            routePlanningState
                                .setPoint(
                                    target =
                                        target,
                                    point =
                                        routePointFromMap(
                                            point
                                        )
                                )

                        true
                    }

            map.addOnMapClickListener(
                listener
            )

            onDispose {

                map.removeOnMapClickListener(
                    listener
                )
            }
        }
    }

    LaunchedEffect(
        routePlanningState.start,
        routePlanningState.waypoints,
        routePlanningState.end,
        loadedStyle
    ) {

        val style =
            loadedStyle
                ?: return@LaunchedEffect

        updateRouteSelection(
            style =
                style,
            state =
                routePlanningState
        )
    }

    LaunchedEffect(
        plannedRoute,
        loadedStyle
    ) {

        val style =
            loadedStyle
                ?: return@LaunchedEffect

        updatePlannedRoute(
            style =
                style,
            route =
                plannedRoute
        )
    }

    LaunchedEffect(
        selectedElevationSample,
        loadedStyle
    ) {

        val style =
            loadedStyle
                ?: return@LaunchedEffect

        updateElevationMarker(
            style =
                style,
            sample =
                selectedElevationSample
        )
    }

    LaunchedEffect(
        tracks,
        loadedStyle
    ) {

        val style =
            loadedStyle
                ?: return@LaunchedEffect

        updateUserTracks(
            style =
                style,
            tracks =
                tracks
        )
    }

    LaunchedEffect(
        layerSettings,
        loadedStyle
    ) {

        val style =
            loadedStyle
                ?: return@LaunchedEffect

        applyMapLayerVisibility(
            style =
                style,
            settings =
                layerSettings
        )
    }

    LaunchedEffect(
        editRouteId,
        plannedRoute,
        mapLibreMap
    ) {

        if (
            editRouteId == null
        ) {
            return@LaunchedEffect
        }

        val route =
            plannedRoute
                ?: return@LaunchedEffect

        val map =
            mapLibreMap
                ?: return@LaunchedEffect

        if (
            route.points.size >=
            2
        ) {

            val bounds =
                LatLngBounds
                    .fromLatLngs(
                        route.points
                    )

            map.animateCamera(
                CameraUpdateFactory
                    .newLatLngBounds(
                        bounds,
                        90
                    )
            )
        }
    }

    val suggestedRouteName =
        remember(
            routePlanningState
                .start
                .point,
            routePlanningState
                .end
                .point
        ) {

            val startName =
                routePlanningState
                    .start
                    .point
                    ?.name
                    ?.trim()
                    .orEmpty()

            val endName =
                routePlanningState
                    .end
                    .point
                    ?.name
                    ?.trim()
                    .orEmpty()

            when {

                startName.isNotEmpty() &&
                        endName.isNotEmpty() ->

                    "$startName – $endName"

                endName.isNotEmpty() ->

                    "Trasa do $endName"

                startName.isNotEmpty() ->

                    "Trasa z $startName"

                else ->

                    "Nowa trasa"
            }
        }

    if (
        showSaveRouteDialog
    ) {

        val route =
            plannedRoute

        if (
            route != null
        ) {

            SavePlannedRouteDialog(
                route =
                    route,
                elevationProfile =
                    elevationProfile,
                suggestedName =
                    editingRouteName
                        ?: suggestedRouteName,
                isEditing =
                    isEditMode,
                onDismiss = {

                    showSaveRouteDialog =
                        false
                },
                onSave = {
                        routeName ->

                    val routeSnapshot =
                        plannedRoute

                    if (
                        routeSnapshot != null
                    ) {

                        val draft =
                            buildPlannedRouteDraft(
                                name =
                                    routeName,
                                route =
                                    routeSnapshot,
                                elevationProfile =
                                    elevationProfile,
                                planningState =
                                    routePlanningState
                            )

                        coroutineScope.launch {

                            val result =
                                if (
                                    editRouteId != null
                                ) {

                                    plannedRoutesViewModel
                                        .updateRoute(
                                            routeId =
                                                editRouteId,
                                            draft =
                                                draft
                                        )

                                } else {

                                    plannedRoutesViewModel
                                        .saveRoute(
                                            draft
                                        )
                                        .map {
                                            Unit
                                        }
                                }

                            showSaveRouteDialog =
                                false

                            result
                                .onSuccess {

                                    editingRouteName =
                                        routeName

                                    if (
                                        editRouteId != null &&
                                        onEditSaved != null
                                    ) {

                                        onEditSaved()

                                    } else {

                                        featureDialog =
                                            "Trasa zapisana" to
                                                    "„$routeName” została zapisana lokalnie."
                                    }
                                }
                                .onFailure {
                                        error ->

                                    featureDialog =
                                        "Nie udało się zapisać trasy" to
                                                (
                                                        error.message
                                                            ?: "Wystąpił nieznany błąd zapisu."
                                                        )
                                }
                        }
                    }
                }
            )
        }
    }

    if (
        showRouteActionsDialog
    ) {

        PlannedRouteActionsDialog(
            onDismiss = {

                showRouteActionsDialog =
                    false
            },
            onEdit = {

                showRouteActionsDialog =
                    false

                showRoutePlanner =
                    true

                routeSheetLevel =
                    RoutePlannerSheetLevel.MEDIUM
            },
            onExportGpx = {

                showRouteActionsDialog =
                    false

                featureDialog =
                    "Eksport GPX" to
                            "Eksport gotowej trasy do GPX podepniemy w kolejnym etapie."
            },
            onDuplicate = {

                showRouteActionsDialog =
                    false

                featureDialog =
                    "Duplikowanie trasy" to
                            "Duplikowanie zapisanej trasy jest dostępne z jej ekranu szczegółów."
            },
            onDelete = {

                showRouteActionsDialog =
                    false

                invalidateCalculatedRoute()

                routeSheetLevel =
                    RoutePlannerSheetLevel.MEDIUM
            }
        )
    }

    if (
        showAddOwnPlaceDialog
    ) {

        AddOwnPlaceDialog(
            onDismiss = {

                showAddOwnPlaceDialog =
                    false
            },
            onSave = {
                    placeName ->

                showAddOwnPlaceDialog =
                    false

                featureDialog =
                    "Zapisany punkt" to
                            "Punkt „$placeName” jest gotowy do podłączenia do warstwy danych."
            }
        )
    }

    featureDialog
        ?.let {
                dialog ->

            TatraFeatureDialog(
                title =
                    dialog.first,
                message =
                    dialog.second,
                onDismiss = {

                    featureDialog =
                        null
                }
            )
        }

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        AndroidView(
            modifier =
                Modifier.fillMaxSize(),
            factory = {
                mapView
            }
        )

        MapRoundButton(
            modifier =
                Modifier
                    .align(
                        Alignment.TopStart
                    )
                    .padding(
                        start = 14.dp,
                        top = 14.dp
                    ),
            onClick = {

                val opening =
                    !showRoutePlanner

                showRoutePlanner =
                    opening

                showLayersPanel =
                    false

                showPoiPanel =
                    false

                showTrailToolsPanel =
                    false

                if (
                    opening
                ) {

                    routeSheetLevel =
                        RoutePlannerSheetLevel.MEDIUM

                } else {

                    routePlanningState =
                        routePlanningState.copy(
                            activeSearchTarget =
                                null,
                            selectingOnMap =
                                null
                        )

                    searchResults =
                        emptyList()

                    selectedElevationSample =
                        null
                }
            }
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Menu,
                contentDescription =
                    "Planowanie trasy",
                tint =
                    TatraDark
            )
        }

        if (
            plannedRoute != null
        ) {

            MapRouteQuickActions(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopCenter
                        )
                        .padding(
                            top = 14.dp
                        ),
                saveText =
                    if (
                        isEditMode
                    ) {
                        "Zapisz zmiany"
                    } else {
                        "Zapisz"
                    },
                onSaveClick = {

                    showSaveRouteDialog =
                        true
                },
                onMoreClick = {

                    showRouteActionsDialog =
                        true
                }
            )
        }

        Column(
            modifier =
                Modifier
                    .align(
                        Alignment.TopEnd
                    )
                    .padding(
                        end = 14.dp,
                        top = 14.dp
                    )
        ) {

            MapRoundButton(
                onClick = {

                    showPoiPanel =
                        !showPoiPanel

                    showLayersPanel =
                        false

                    showTrailToolsPanel =
                        false

                    showRoutePlanner =
                        false

                    routePlanningState =
                        routePlanningState.copy(
                            activeSearchTarget =
                                null,
                            selectingOnMap =
                                null
                        )

                    selectedElevationSample =
                        null
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.LocationOn,
                    contentDescription =
                        "Punkty",
                    tint =
                        TatraDark
                )
            }

            MapRoundButton(
                modifier =
                    Modifier.padding(
                        top = 8.dp
                    ),
                onClick = {

                    showLayersPanel =
                        !showLayersPanel

                    showPoiPanel =
                        false

                    showTrailToolsPanel =
                        false

                    showRoutePlanner =
                        false

                    routePlanningState =
                        routePlanningState.copy(
                            activeSearchTarget =
                                null,
                            selectingOnMap =
                                null
                        )

                    selectedElevationSample =
                        null
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Layers,
                    contentDescription =
                        "Warstwy",
                    tint =
                        TatraDark
                )
            }

            MapRoundButton(
                modifier =
                    Modifier.padding(
                        top = 8.dp
                    ),
                onClick = {

                    showTrailToolsPanel =
                        !showTrailToolsPanel

                    showLayersPanel =
                        false

                    showPoiPanel =
                        false

                    showRoutePlanner =
                        false

                    routePlanningState =
                        routePlanningState.copy(
                            activeSearchTarget =
                                null,
                            selectingOnMap =
                                null
                        )

                    selectedElevationSample =
                        null
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Terrain,
                    contentDescription =
                        "Szlaki",
                    tint =
                        TatraDark
                )
            }
        }

        if (
            showRoutePlanner
        ) {

            RoutePlannerBottomSheet(
                modifier =
                    Modifier.align(
                        Alignment.BottomCenter
                    ),
                level =
                    routeSheetLevel,
                onLevelChange = {

                    routeSheetLevel =
                        it
                }
            ) {

                RoutePlanningPanel(
                    modifier =
                        Modifier.fillMaxSize(),
                    state =
                        routePlanningState,
                    searchResults =
                        searchResults,
                    searchLoading =
                        searchLoading,
                    searchError =
                        searchError,
                    plannedRoute =
                        plannedRoute,
                    routeLoading =
                        routeLoading,
                    routeError =
                        routeError,
                    elevationProfile =
                        elevationProfile,
                    elevationLoading =
                        elevationLoading,
                    elevationError =
                        elevationError,
                    selectedElevationSample =
                        selectedElevationSample,
                    onElevationSampleSelected = {
                            sample ->

                        selectedElevationSample =
                            sample
                    },
                    onQueryChange = {
                            target,
                            query ->

                        invalidateCalculatedRoute()

                        routePlanningState =
                            routePlanningState
                                .updateQuery(
                                    target =
                                        target,
                                    query =
                                        query
                                )
                    },
                    onSearchResultSelected = {
                            target,
                            result ->

                        invalidateCalculatedRoute()

                        val routePoint =
                            RoutePlanPoint(
                                name =
                                    result.name,
                                position =
                                    result.position
                            )

                        routePlanningState =
                            routePlanningState
                                .setPoint(
                                    target =
                                        target,
                                    point =
                                        routePoint
                                )

                        searchResults =
                            emptyList()

                        mapLibreMap
                            ?.animateCamera(
                                CameraUpdateFactory
                                    .newLatLngZoom(
                                        result.position,
                                        13.5
                                    )
                            )
                    },
                    onSelectOnMap = {
                            target ->

                        routePlanningState =
                            routePlanningState.copy(
                                activeSearchTarget =
                                    null,
                                selectingOnMap =
                                    target
                            )

                        routeSheetLevel =
                            RoutePlannerSheetLevel.COMPACT

                        searchResults =
                            emptyList()
                    },
                    onClear = {
                            target ->

                        invalidateCalculatedRoute()

                        routePlanningState =
                            routePlanningState
                                .clearPoint(
                                    target
                                )

                        searchResults =
                            emptyList()
                    },
                    onAddWaypoint = {

                        invalidateCalculatedRoute()

                        routePlanningState =
                            routePlanningState
                                .addWaypoint()

                        if (
                            routeSheetLevel ==
                            RoutePlannerSheetLevel.COMPACT
                        ) {

                            routeSheetLevel =
                                RoutePlannerSheetLevel.MEDIUM
                        }
                    },
                    onRemoveWaypoint = {
                            index ->

                        invalidateCalculatedRoute()

                        routePlanningState =
                            routePlanningState
                                .removeWaypoint(
                                    index
                                )
                    },
                    onCalculateRoute = {

                        if (
                            !routeLoading
                        ) {

                            routeCalculationVersion++

                            val calculationVersion =
                                routeCalculationVersion

                            coroutineScope.launch {

                                routeLoading =
                                    true

                                routeError =
                                    null

                                plannedRoute =
                                    null

                                elevationProfile =
                                    null

                                elevationError =
                                    null

                                elevationLoading =
                                    false

                                selectedElevationSample =
                                    null

                                val response =
                                    routingRepository
                                        .calculateRoute(
                                            routePlanningState
                                        )

                                if (
                                    calculationVersion !=
                                    routeCalculationVersion
                                ) {

                                    return@launch
                                }

                                routeLoading =
                                    false

                                plannedRoute =
                                    response.route

                                routeError =
                                    response.error

                                val route =
                                    response.route

                                if (
                                    route != null
                                ) {

                                    routeSheetLevel =
                                        RoutePlannerSheetLevel.COMPACT

                                    if (
                                        route.points.size >=
                                        2
                                    ) {

                                        val bounds =
                                            LatLngBounds
                                                .fromLatLngs(
                                                    route.points
                                                )

                                        mapLibreMap
                                            ?.animateCamera(
                                                CameraUpdateFactory
                                                    .newLatLngBounds(
                                                        bounds,
                                                        90
                                                    )
                                            )
                                    }

                                    elevationLoading =
                                        true

                                    val elevationResponse =
                                        elevationRepository
                                            .calculateProfile(
                                                route
                                            )

                                    if (
                                        calculationVersion !=
                                        routeCalculationVersion
                                    ) {

                                        return@launch
                                    }

                                    elevationLoading =
                                        false

                                    elevationProfile =
                                        elevationResponse.profile

                                    elevationError =
                                        elevationResponse.error
                                }
                            }
                        }
                    },
                    onClearRoute = {

                        invalidateCalculatedRoute()

                        routeSheetLevel =
                            RoutePlannerSheetLevel.MEDIUM
                    },
                    onClose = {

                        showRoutePlanner =
                            false

                        routePlanningState =
                            routePlanningState.copy(
                                activeSearchTarget =
                                    null,
                                selectingOnMap =
                                    null
                            )

                        searchResults =
                            emptyList()

                        selectedElevationSample =
                            null
                    }
                )
            }
        }

        if (
            showPoiPanel
        ) {

            MapPoiToolsPanel(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopEnd
                        )
                        .padding(
                            top = 66.dp,
                            end = 70.dp
                        ),
                settings =
                    layerSettings,
                onSettingsChange = {

                    layerSettings =
                        it
                },
                onAddOwnPlace = {

                    showAddOwnPlaceDialog =
                        true
                },
                onClose = {

                    showPoiPanel =
                        false
                }
            )
        }

        if (
            showTrailToolsPanel
        ) {

            MapTrailToolsPanel(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopEnd
                        )
                        .padding(
                            top = 66.dp,
                            end = 70.dp
                        ),
                settings =
                    layerSettings,
                onSettingsChange = {

                    layerSettings =
                        it
                },
                onCompletedTrailsClick = {

                    featureDialog =
                        "Ukończone szlaki" to
                                "Podświetlanie zaliczonych odcinków podepniemy po uruchomieniu TrailMatcher."
                },
                onImportGpxClick = {

                    featureDialog =
                        "Import GPX" to
                                "Import GPX podepniemy w kolejnym etapie."
                },
                onClose = {

                    showTrailToolsPanel =
                        false
                }
            )
        }

        if (
            showLayersPanel
        ) {

            MapLayersPanel(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopEnd
                        )
                        .padding(
                            top = 66.dp,
                            end = 70.dp
                        ),
                settings =
                    layerSettings,
                onSettingsChange = {

                    layerSettings =
                        it
                },
                onClose = {

                    showLayersPanel =
                        false
                }
            )
        }

        if (
            !showRoutePlanner
        ) {

            MapRoundButton(
                modifier =
                    Modifier
                        .align(
                            Alignment.BottomEnd
                        )
                        .padding(
                            end = 14.dp,
                            bottom = 18.dp
                        ),
                onClick = {

                    mapLibreMap
                        ?.animateCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(
                                    TATRA_CENTER,
                                    11.0
                                )
                        )
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.MyLocation,
                    contentDescription =
                        "Tatry",
                    tint =
                        TatraDark
                )
            }
        }

        if (
            mapLoading ||
            editLoading
        ) {

            CircularProgressIndicator(
                modifier =
                    Modifier.align(
                        Alignment.Center
                    ),
                color =
                    TatraGreen
            )
        }
    }
}

private fun buildPlannedRouteDraft(
    name: String,
    route: PlannedRoute,
    elevationProfile:
    RouteElevationProfile?,
    planningState:
    RoutePlanningState
): PlannedRouteDraft {

    val points =
        mutableListOf<
                PlannedRouteDraftPoint
                >()

    route.points
        .forEachIndexed {
                index,
                point ->

            points +=
                PlannedRouteDraftPoint(
                    pointType =
                        PlannedRoutePointType.GEOMETRY,
                    orderIndex =
                        index,
                    latitude =
                        point.latitude,
                    longitude =
                        point.longitude
                )
        }

    planningState
        .start
        .point
        ?.let {
                start ->

            points +=
                PlannedRouteDraftPoint(
                    pointType =
                        PlannedRoutePointType.START,
                    orderIndex =
                        0,
                    latitude =
                        start.position.latitude,
                    longitude =
                        start.position.longitude,
                    name =
                        start.name
                )
        }

    planningState
        .waypoints
        .mapNotNull {
            it.point
        }
        .forEachIndexed {
                index,
                waypoint ->

            points +=
                PlannedRouteDraftPoint(
                    pointType =
                        PlannedRoutePointType.WAYPOINT,
                    orderIndex =
                        index,
                    latitude =
                        waypoint.position.latitude,
                    longitude =
                        waypoint.position.longitude,
                    name =
                        waypoint.name
                )
        }

    planningState
        .end
        .point
        ?.let {
                end ->

            points +=
                PlannedRouteDraftPoint(
                    pointType =
                        PlannedRoutePointType.END,
                    orderIndex =
                        0,
                    latitude =
                        end.position.latitude,
                    longitude =
                        end.position.longitude,
                    name =
                        end.name
                )
        }

    elevationProfile
        ?.samples
        ?.forEachIndexed {
                index,
                sample ->

            points +=
                PlannedRouteDraftPoint(
                    pointType =
                        PlannedRoutePointType.ELEVATION,
                    orderIndex =
                        index,
                    latitude =
                        sample.position.latitude,
                    longitude =
                        sample.position.longitude,
                    altitudeMeters =
                        sample.elevationMeters,
                    distanceMeters =
                        sample.distanceMeters
                )
        }

    return PlannedRouteDraft(
        name =
            name.trim(),
        distanceMeters =
            route.distanceKm *
                    1000.0,
        durationSeconds =
            route.durationSeconds,
        ascentMeters =
            elevationProfile
                ?.totalAscentMeters,
        descentMeters =
            elevationProfile
                ?.totalDescentMeters,
        minElevationMeters =
            elevationProfile
                ?.lowestElevationMeters,
        maxElevationMeters =
            elevationProfile
                ?.highestElevationMeters,
        startName =
            planningState
                .start
                .point
                ?.name,
        endName =
            planningState
                .end
                .point
                ?.name,
        points =
            points
    )
}

private fun PlannedRouteEditData
        .toRoutePlanningState():
        RoutePlanningState {

    val startPoint =
        points
            .firstOrNull {
                it.pointType ==
                        PlannedRoutePointType.START
            }

    val endPoint =
        points
            .firstOrNull {
                it.pointType ==
                        PlannedRoutePointType.END
            }

    val waypointPoints =
        points
            .filter {
                it.pointType ==
                        PlannedRoutePointType.WAYPOINT
            }
            .sortedBy {
                it.orderIndex
            }

    return RoutePlanningState(
        start =
            startPoint
                ?.toRoutePointInput(
                    fallbackName =
                        route.startName
                            ?: "Start"
                )
                ?: RoutePointInput(),
        waypoints =
            waypointPoints
                .mapIndexed {
                        index,
                        point ->

                    point.toRoutePointInput(
                        fallbackName =
                            "Przystanek ${index + 1}"
                    )
                },
        end =
            endPoint
                ?.toRoutePointInput(
                    fallbackName =
                        route.endName
                            ?: "Cel"
                )
                ?: RoutePointInput()
    )
}

private fun PlannedRouteEditData
        .toPlannedRoute():
        PlannedRoute? {

    val geometry =
        points
            .filter {
                it.pointType ==
                        PlannedRoutePointType.GEOMETRY
            }
            .sortedBy {
                it.orderIndex
            }

    if (
        geometry.size < 2
    ) {
        return null
    }

    val waypointCount =
        points.count {
            it.pointType ==
                    PlannedRoutePointType.WAYPOINT
        }

    return PlannedRoute(
        points =
            geometry.map {
                    point ->

                LatLng(
                    point.latitude,
                    point.longitude
                )
            },
        distanceKm =
            route.distanceMeters /
                    1000.0,
        durationSeconds =
            route.durationSeconds,
        legCount =
            waypointCount + 1
    )
}

private fun PlannedRouteEditData
        .toElevationProfile():
        RouteElevationProfile? {

    val samples =
        points
            .filter {
                it.pointType ==
                        PlannedRoutePointType.ELEVATION
            }
            .sortedBy {
                it.orderIndex
            }
            .mapNotNull {
                    point ->

                val elevation =
                    point.altitudeMeters
                        ?: return@mapNotNull null

                RouteElevationSample(
                    distanceMeters =
                        point.distanceMeters
                            ?: 0.0,
                    position =
                        LatLng(
                            point.latitude,
                            point.longitude
                        ),
                    elevationMeters =
                        elevation
                )
            }

    if (
        samples.size < 2
    ) {
        return null
    }

    val highest =
        route.maxElevationMeters
            ?: samples
                .maxOf {
                    it.elevationMeters
                }
                .roundToInt()

    val lowest =
        route.minElevationMeters
            ?: samples
                .minOf {
                    it.elevationMeters
                }
                .roundToInt()

    return RouteElevationProfile(
        samples =
            samples,
        totalAscentMeters =
            route.ascentMeters
                ?: 0,
        totalDescentMeters =
            route.descentMeters
                ?: 0,
        highestElevationMeters =
            highest,
        lowestElevationMeters =
            lowest
    )
}

private fun PlannedRoutePointEntity
        .toRoutePointInput(
    fallbackName: String
): RoutePointInput {

    val displayName =
        name
            ?.takeIf {
                it.isNotBlank()
            }
            ?: fallbackName

    return RoutePointInput(
        query =
            displayName,
        point =
            RoutePlanPoint(
                name =
                    displayName,
                position =
                    LatLng(
                        latitude,
                        longitude
                    )
            )
    )
}