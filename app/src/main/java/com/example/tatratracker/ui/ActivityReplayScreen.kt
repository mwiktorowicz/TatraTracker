package com.example.tatratracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.replay.ActivityReplayTrack
import com.example.tatratracker.replay.ReplayReadings
import com.example.tatratracker.replay.ReplayTempo
import com.example.tatratracker.ui.components.TatraErrorState
import com.example.tatratracker.ui.components.TatraLoadingState
import com.example.tatratracker.ui.components.TatraPrimaryButton
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.format.TatraFormat
import com.example.tatratracker.ui.map.ActivityReplayMap
import com.example.tatratracker.ui.map.ElevationProfileChart
import com.example.tatratracker.ui.map.MapRoundButton
import com.example.tatratracker.ui.map.RouteElevationProfile
import com.example.tatratracker.ui.map.RouteElevationSample
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraSurface
import com.example.tatratracker.ui.theme.TatraSurfaceStrong
import com.example.tatratracker.ui.theme.TatraText
import com.example.tatratracker.ui.theme.TatraTextMuted
import com.example.tatratracker.viewmodel.ActivityReplayLoadState
import com.example.tatratracker.viewmodel.ActivityReplayViewModel
import org.maplibre.android.geometry.LatLng
import kotlin.math.roundToInt

/**
 * Odtwarzanie wędrówki: znacznik jedzie po trasie na mapie, razem z nim kursor na profilu
 * wysokości i odczyty w bieżącym miejscu.
 */
@Composable
fun ActivityReplayScreen(
    onBack: () -> Unit,
    viewModel: ActivityReplayViewModel = viewModel()
) {
    val loadState by viewModel.loadState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TatraBackground)
    ) {
        when (val state = loadState) {
            ActivityReplayLoadState.Loading -> {
                TatraLoadingState(
                    message = "Wczytuję wędrówkę...",
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is ActivityReplayLoadState.Error -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    TatraSubScreenHeader(
                        title = "Odtwarzanie wędrówki",
                        onBack = onBack
                    )

                    TatraErrorState(
                        message = state.message,
                        modifier = Modifier.padding(horizontal = TatraSpacing.lg)
                    )
                }
            }

            is ActivityReplayLoadState.Ready -> {
                ReplayContent(
                    state = state,
                    viewModel = viewModel,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun ReplayContent(
    state: ActivityReplayLoadState.Ready,
    viewModel: ActivityReplayViewModel,
    onBack: () -> Unit
) {
    val track = state.track
    val progressState = viewModel.progressMeters.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val tempo by viewModel.tempo.collectAsState()
    val following by viewModel.following.collectAsState()

    // Pętla klatek tylko w trakcie odtwarzania; stan i postęp trzyma ViewModel.
    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            return@LaunchedEffect
        }

        var previousFrame = withFrameNanos { it }
        while (true) {
            withFrameNanos { frame ->
                viewModel.advance(frame - previousFrame)
                previousFrame = frame
            }
        }
    }

    // Wyjście z ekranu, aplikacja w tle i obrót ekranu: pauza w bieżącym miejscu.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.pause()
        }
    }

    // Ekran nie gaśnie w trakcie odtwarzania.
    val view = LocalView.current
    DisposableEffect(view, isPlaying) {
        view.keepScreenOn = isPlaying

        onDispose {
            view.keepScreenOn = false
        }
    }

    val mapArea: @Composable (Modifier) -> Unit = { modifier ->
        Box(modifier = modifier) {
            ActivityReplayMap(
                track = track,
                layerSettings = state.layerSettings,
                progressMeters = { progressState.value },
                isPlaying = isPlaying,
                following = following,
                onUserMovedMap = { viewModel.setFollowing(false) },
                modifier = Modifier.fillMaxSize()
            )

            MapRoundButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 14.dp),
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Wstecz",
                    tint = TatraDark
                )
            }

            FollowButton(
                following = following,
                onClick = { viewModel.setFollowing(!following) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp, top = 14.dp)
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // W poziomie panel obok mapy – pod mapą zostałby z niej wąski pasek.
        if (maxWidth > maxHeight) {
            Row(modifier = Modifier.fillMaxSize()) {
                mapArea(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                ReplayPanel(
                    track = track,
                    progressState = progressState,
                    isPlaying = isPlaying,
                    tempo = tempo,
                    onTogglePlay = viewModel::togglePlay,
                    onTempoSelected = viewModel::setTempo,
                    onSeek = viewModel::seekTo,
                    shape = RectangleShape,
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                mapArea(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                ReplayPanel(
                    track = track,
                    progressState = progressState,
                    isPlaying = isPlaying,
                    tempo = tempo,
                    onTogglePlay = viewModel::togglePlay,
                    onTempoSelected = viewModel::setTempo,
                    onSeek = viewModel::seekTo,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FollowButton(
    following: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (following) Color.White else TatraDark

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (following) TatraGreen else Color.White.copy(alpha = 0.96f),
        shadowElevation = 5.dp
    ) {
        Row(
            modifier = Modifier
                .selectable(
                    selected = following,
                    onClick = onClick,
                    role = Role.Switch
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (following) Icons.Outlined.GpsFixed else Icons.Outlined.GpsNotFixed,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Śledź",
                color = contentColor,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ReplayPanel(
    track: ActivityReplayTrack,
    progressState: State<Double>,
    isPlaying: Boolean,
    tempo: ReplayTempo,
    onTogglePlay: () -> Unit,
    onTempoSelected: (ReplayTempo) -> Unit,
    onSeek: (Double) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val progress = progressState.value
    val readings = remember(track, progress) { track.readingsAt(progress) }

    // Profil dla wspólnego wykresu: oś pozioma to postęp odtwarzania, jak u znacznika.
    val chartProfile = remember(track) { buildChartProfile(track) }

    val cursor = chartProfile?.let { profile ->
        val elevation = track.profileElevationAt(progress) ?: return@let null
        val position = track.positionAt(progress)

        RouteElevationSample(
            distanceMeters = progress.coerceIn(
                profile.samples.first().distanceMeters,
                profile.samples.last().distanceMeters
            ),
            position = LatLng(position.lat, position.lon),
            elevationMeters = elevation
        )
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = TatraSurface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TatraSpacing.md, vertical = TatraSpacing.sm)
        ) {
            ReplayReadingsGrid(readings = readings)

            Spacer(modifier = Modifier.height(TatraSpacing.xxs))

            if (chartProfile != null) {
                ElevationProfileChart(
                    profile = chartProfile,
                    selectedSample = cursor,
                    onSampleSelected = { sample ->
                        if (sample != null) {
                            onSeek(sample.distanceMeters)
                        }
                    }
                )
            } else {
                Text(
                    text = "Ślad nie ma zapisanej wysokości – profil niedostępny.",
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = TatraSpacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xxs))

            ReplayControls(
                canPlay = track.totalProgressMeters > 0.0,
                isPlaying = isPlaying,
                atEnd = track.totalProgressMeters > 0.0 && progress >= track.totalProgressMeters,
                tempo = tempo,
                onTogglePlay = onTogglePlay,
                onTempoSelected = onTempoSelected
            )
        }
    }
}

@Composable
private fun ReplayReadingsGrid(readings: ReplayReadings) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TatraSpacing.xs)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            ReplayReading(
                label = "Godzina",
                value = TatraFormat.timeOfDay(readings.timestampSeconds),
                modifier = Modifier.weight(1f)
            )
            ReplayReading(
                label = "Dystans",
                value = TatraFormat.distance(readings.distanceMeters),
                modifier = Modifier.weight(1f)
            )
            ReplayReading(
                label = "Wysokość",
                value = TatraFormat.elevation(readings.elevationMeters),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            ReplayReading(
                label = "Tętno",
                value = TatraFormat.heartRate(readings.heartRate),
                modifier = Modifier.weight(1f)
            )
            ReplayReading(
                label = "Prędkość",
                value = TatraFormat.speed(readings.speedMs),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReplayReading(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = TatraTextMuted,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = value,
            color = TatraDark,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ReplayControls(
    canPlay: Boolean,
    isPlaying: Boolean,
    atEnd: Boolean,
    tempo: ReplayTempo,
    onTogglePlay: () -> Unit,
    onTempoSelected: (ReplayTempo) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TatraPrimaryButton(
            onClick = onTogglePlay,
            enabled = canPlay,
            modifier = Modifier.weight(1f)
        ) {
            val (icon, label) = when {
                isPlaying -> Icons.Filled.Pause to "Pauza"
                atEnd -> Icons.Filled.Replay to "Od początku"
                else -> Icons.Filled.PlayArrow to "Odtwórz"
            }

            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, maxLines = 1)
        }

        Spacer(modifier = Modifier.width(TatraSpacing.sm))

        TempoSelector(
            tempo = tempo,
            onTempoSelected = onTempoSelected
        )
    }
}

@Composable
private fun TempoSelector(
    tempo: ReplayTempo,
    onTempoSelected: (ReplayTempo) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TatraSurfaceStrong)
            .padding(3.dp)
    ) {
        ReplayTempo.entries.forEach { option ->
            val selected = option == tempo

            Box(
                modifier = Modifier
                    .height(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) TatraGreen else Color.Transparent)
                    .selectable(
                        selected = selected,
                        onClick = { onTempoSelected(option) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tempoLabel(option),
                    color = if (selected) Color.White else TatraText,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}

private fun tempoLabel(tempo: ReplayTempo): String {
    return when (tempo) {
        ReplayTempo.SLOW -> "2 min"
        ReplayTempo.NORMAL -> "1 min"
        ReplayTempo.FAST -> "30 s"
    }
}

private fun buildChartProfile(track: ActivityReplayTrack): RouteElevationProfile? {
    if (track.profile.size < 2) {
        return null
    }

    val samples = track.profile.map { sample ->
        val position = track.positionAt(sample.progressMeters)

        RouteElevationSample(
            distanceMeters = sample.progressMeters,
            position = LatLng(position.lat, position.lon),
            elevationMeters = sample.elevationMeters
        )
    }

    // Wykres korzysta tylko z próbek; sumy podejść nie są tu pokazywane.
    return RouteElevationProfile(
        samples = samples,
        totalAscentMeters = 0,
        totalDescentMeters = 0,
        highestElevationMeters = track.profile.maxOf { it.elevationMeters }.roundToInt(),
        lowestElevationMeters = track.profile.minOf { it.elevationMeters }.roundToInt()
    )
}
