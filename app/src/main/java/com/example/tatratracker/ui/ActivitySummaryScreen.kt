package com.example.tatratracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import com.example.tatratracker.ui.components.TatraPrimaryButton
import com.example.tatratracker.ui.components.TatraProgressBar
import com.example.tatratracker.ui.format.TatraFormat
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraDarkMuted
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraWhite
import com.example.tatratracker.viewmodel.ActivitySummaryElevation
import com.example.tatratracker.viewmodel.ActivitySummaryPlaces
import com.example.tatratracker.viewmodel.ActivitySummaryTrails
import com.example.tatratracker.viewmodel.ActivitySummaryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private const val SUMMARY_PAGE_COUNT = 4

// Najwyżej tyle nowych miejsc mieści się na planszy z nazwy.
private const val MAX_LISTED_PLACES = 6

// Poniżej tego przyrostu (ułamek sieci szlaków) pokazujemy „bez nowych odcinków”.
private const val MIN_SHOWN_TRAIL_GROWTH = 0.00005

private val SummaryTextMuted = TatraWhite.copy(alpha = 0.68f)
private val SummaryDivider = TatraWhite.copy(alpha = 0.14f)

/**
 * Pełnoekranowe plansze podsumowania jednej wędrówki, przesuwane w bok:
 * wędrówka, wysokość, nowe miejsca, szlaki.
 */
@Composable
fun ActivitySummaryScreen(
    onClose: () -> Unit,
    viewModel: ActivitySummaryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val pagerState = rememberPagerState(pageCount = { SUMMARY_PAGE_COUNT })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(TatraDark, TatraDarkMuted)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = TatraSpacing.lg, end = TatraSpacing.xs, top = TatraSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Podsumowanie",
                color = SummaryTextMuted,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Zamknij",
                    tint = TatraWhite
                )
            }
        }

        val activity = state.activity

        when {
            state.isLoading ->
                CenteredMessage {
                    CircularProgressIndicator(color = TatraGreen)
                }

            activity == null ->
                CenteredMessage {
                    Text(
                        text = state.errorMessage ?: "Nie znaleziono tej aktywności.",
                        color = TatraWhite,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }

            else -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    when (page) {
                        0 -> HikePage(activity)
                        1 -> ElevationPage(state.elevation)
                        2 -> PlacesPage(state.places)
                        else -> TrailsPage(state.trails)
                    }
                }

                PageDots(
                    pageCount = SUMMARY_PAGE_COUNT,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = TatraSpacing.sm)
                )

                val isLastPage = pagerState.currentPage == SUMMARY_PAGE_COUNT - 1

                TatraPrimaryButton(
                    onClick = {
                        if (isLastPage) {
                            onClose()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = TatraSpacing.lg,
                            end = TatraSpacing.lg,
                            bottom = TatraSpacing.lg
                        )
                ) {
                    Text(if (isLastPage) "Zamknij" else "Dalej")
                }
            }
        }
    }
}

@Composable
private fun HikePage(activity: ActivityEntity) {
    SummaryPage(icon = Icons.Outlined.Hiking, label = "Wędrówka") {
        Text(
            text = activity.name,
            color = TatraWhite,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = formatSummaryDate(activity.timestamp),
            color = SummaryTextMuted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = TatraSpacing.xxs)
        )

        BigValue(
            value = TatraFormat.distanceExact(activity.distanceMeters),
            caption = "dystans"
        )

        SummaryRows(
            listOf(
                "Czas całkowity" to TatraFormat.durationMillis(activity.durationMillis),
                "Czas w ruchu" to TatraFormat.durationMillis(activity.movingDurationMillis)
            )
        )
    }
}

@Composable
private fun ElevationPage(elevation: ActivitySummaryElevation?) {
    SummaryPage(icon = Icons.Outlined.Landscape, label = "Wysokość") {
        val highest = elevation?.highestMeters

        if (highest == null && elevation?.ascentMeters == null) {
            SummaryNote("Ten ślad nie ma danych o wysokości.")
            return@SummaryPage
        }

        BigValue(
            value = TatraFormat.altitude(highest?.toDouble()),
            caption = "najwyższy punkt"
        )

        SummaryRows(
            listOf(
                "Podejścia" to (elevation?.ascentMeters?.let { "+${TatraFormat.elevation(it)}" } ?: "—"),
                "Zejścia" to (elevation?.descentMeters?.let { "−${TatraFormat.elevation(it)}" } ?: "—")
            )
        )
    }
}

@Composable
private fun PlacesPage(places: ActivitySummaryPlaces?) {
    SummaryPage(icon = Icons.Outlined.Place, label = "Nowe miejsca") {
        if (places == null) {
            CircularProgressIndicator(color = TatraGreen)
            return@SummaryPage
        }

        val newPlaces = places.newPlaces

        if (newPlaces.isEmpty()) {
            SummaryNote("Tym razem bez nowych szczytów i schronisk – same znajome miejsca.")
        } else {
            BigValue(
                value = newPlaces.size.toString(),
                caption = newPlacesCaption(newPlaces.size)
            )

            Column(
                modifier = Modifier.padding(top = TatraSpacing.md),
                verticalArrangement = Arrangement.spacedBy(TatraSpacing.xs)
            ) {
                newPlaces.take(MAX_LISTED_PLACES).forEach { place ->
                    NewPlaceRow(place)
                }

                if (newPlaces.size > MAX_LISTED_PLACES) {
                    Text(
                        text = "i jeszcze ${newPlaces.size - MAX_LISTED_PLACES}",
                        color = SummaryTextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        val collected = places.collectedCount
        val total = places.totalCount

        if (collected != null && total != null && total > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TatraSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Kolekcja: $collected z $total",
                    color = TatraWhite,
                    style = MaterialTheme.typography.titleMedium
                )

                TatraProgressBar(
                    fraction = collected.toFloat() / total,
                    modifier = Modifier.padding(top = TatraSpacing.xs)
                )
            }
        }
    }
}

@Composable
private fun TrailsPage(trails: ActivitySummaryTrails) {
    SummaryPage(icon = Icons.Outlined.Route, label = "Szlaki") {
        val fraction = trails.fraction

        when {
            trails.isLoading -> {
                CircularProgressIndicator(color = TatraGreen)

                SummaryNote("Liczę postęp…")
            }

            fraction == null ->
                SummaryNote("Nie udało się policzyć postępu na szlakach.")

            else -> {
                BigValue(
                    value = formatPercent(fraction),
                    caption = "sieci szlaków Tatr za Tobą"
                )

                TatraProgressBar(
                    fraction = fraction.toFloat(),
                    modifier = Modifier.padding(top = TatraSpacing.md)
                )

                val completed = trails.completedLengthMeters
                val total = trails.totalLengthMeters

                if (completed != null && total != null) {
                    Text(
                        text = "${TatraFormat.distance(completed)} z ${TatraFormat.distance(total)}",
                        color = SummaryTextMuted,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = TatraSpacing.xs)
                    )
                }

                trails.growth?.let { growth ->
                    Text(
                        text = trailGrowthText(growth, total),
                        color = TatraGreen,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = TatraSpacing.lg)
                    )
                }

                if (trails.failed) {
                    SummaryNote("Ostatnie przeliczenie się nie udało – wynik może być nieaktualny.")
                }
            }
        }
    }
}

// Wspólny układ planszy: ikona, etykieta i treść wyśrodkowane w pionie.
@Composable
private fun SummaryPage(
    icon: ImageVector,
    label: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TatraSpacing.xl, vertical = TatraSpacing.md),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(TatraGreen.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TatraGreen,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = label.uppercase(Locale.getDefault()),
            color = TatraGreen,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = TatraSpacing.sm, bottom = TatraSpacing.md)
        )

        content()
    }
}

@Composable
private fun BigValue(value: String, caption: String) {
    Text(
        text = value,
        color = TatraWhite,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = TatraSpacing.md)
    )

    Text(
        text = caption,
        color = SummaryTextMuted,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SummaryRows(rows: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TatraSpacing.xl)
    ) {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                HorizontalDivider(color = SummaryDivider)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TatraSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = SummaryTextMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = value,
                    color = TatraWhite,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun SummaryNote(text: String) {
    Text(
        text = text,
        color = SummaryTextMuted,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = TatraSpacing.sm)
    )
}

@Composable
private fun NewPlaceRow(place: SavedPlaceEntity) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector =
                if (place.category == SavedPlaceCategory.HUT) {
                    Icons.Outlined.Cottage
                } else {
                    Icons.Outlined.Landscape
                },
            contentDescription = null,
            tint = TatraGreen,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = place.name,
            color = TatraWhite,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = TatraSpacing.xs)
        )
    }
}

@Composable
private fun PageDots(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { page ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (page == currentPage) TatraGreen else SummaryDivider)
            )
        }
    }
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(TatraSpacing.xl),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// „1 nowe miejsce”, „3 nowe miejsca”, „5 nowych miejsc”, „12 nowych miejsc”.
private fun newPlacesCaption(count: Int): String {
    val lastTwoDigits = count % 100
    val lastDigit = count % 10

    return when {
        count == 1 -> "nowe miejsce"
        lastDigit in 2..4 && lastTwoDigits !in 12..14 -> "nowe miejsca"
        else -> "nowych miejsc"
    }
}

// „+2,1 km nowych szlaków (+0,35 pkt proc.)”; bez długości sieci – same punkty procentowe.
private fun trailGrowthText(growth: Double, totalLengthMeters: Double?): String {
    if (growth < MIN_SHOWN_TRAIL_GROWTH) {
        return "Bez nowych odcinków – ta trasa wiodła po znanych już szlakach."
    }

    val points = String.format(Locale.getDefault(), "+%.2f pkt proc.", growth * 100.0)

    return if (totalLengthMeters != null && totalLengthMeters > 0.0) {
        "+${TatraFormat.distance(growth * totalLengthMeters)} nowych szlaków ($points)"
    } else {
        points
    }
}

private fun formatSummaryDate(timestamp: Long): String {
    return SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
}
