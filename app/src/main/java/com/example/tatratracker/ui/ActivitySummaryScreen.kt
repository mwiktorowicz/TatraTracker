package com.example.tatratracker.ui

import androidx.compose.foundation.background
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.achievements.NewAchievements
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import com.example.tatratracker.share.SummaryImageSharer
import com.example.tatratracker.ui.components.TatraPrimaryButton
import com.example.tatratracker.ui.components.TatraProgressBar
import com.example.tatratracker.ui.components.TatraSecondaryButton
import com.example.tatratracker.ui.format.TatraFormat
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraDarkMuted
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraWhite
import com.example.tatratracker.viewmodel.ActivitySummaryElevation
import com.example.tatratracker.viewmodel.ActivitySummaryPlaces
import com.example.tatratracker.viewmodel.ActivitySummaryTrails
import com.example.tatratracker.viewmodel.ActivitySummaryUiState
import com.example.tatratracker.viewmodel.ActivitySummaryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

private enum class SummaryPageKind {
    HIKE,
    ELEVATION,
    PLACES,
    TRAILS,
    ACHIEVEMENTS,
    SHARE
}

// Plansza „Nowe osiągnięcia” jest tylko wtedy, gdy wędrówka coś przyniosła.
private val PAGES_WITH_ACHIEVEMENTS = SummaryPageKind.entries.toList()
private val PAGES_WITHOUT_ACHIEVEMENTS = PAGES_WITH_ACHIEVEMENTS - SummaryPageKind.ACHIEVEMENTS

// Najwyżej tyle nowych miejsc mieści się na planszy z nazwy.
private const val MAX_LISTED_PLACES = 6

// Poniżej tego przyrostu (ułamek sieci szlaków) pokazujemy „bez nowych odcinków”.
private const val MIN_SHOWN_TRAIL_GROWTH = 0.00005

private val SummaryTextMuted = TatraWhite.copy(alpha = 0.68f)
private val SummaryDivider = TatraWhite.copy(alpha = 0.14f)

/**
 * Pełnoekranowe plansze podsumowania jednej wędrówki, przesuwane w bok:
 * wędrówka, wysokość, nowe miejsca, szlaki, nowe osiągnięcia (jeśli są) i karta do udostępnienia.
 */
@Composable
fun ActivitySummaryScreen(
    onClose: () -> Unit,
    viewModel: ActivitySummaryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val pages =
        if (state.newAchievements.isEmpty) PAGES_WITHOUT_ACHIEVEMENTS else PAGES_WITH_ACHIEVEMENTS

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    // Warstwa, do której rysuje się karta z ostatniej planszy – z niej powstaje obrazek.
    val shareCardLayer = rememberGraphicsLayer()
    var isSharing by remember { mutableStateOf(false) }

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
                    when (pages.getOrNull(page)) {
                        SummaryPageKind.HIKE -> HikePage(activity)
                        SummaryPageKind.ELEVATION -> ElevationPage(state.elevation)
                        SummaryPageKind.PLACES -> PlacesPage(state.places)
                        SummaryPageKind.TRAILS -> TrailsPage(state.trails)

                        SummaryPageKind.ACHIEVEMENTS ->
                            AchievementsPage(
                                newAchievements = state.newAchievements,
                                isCurrent = pagerState.currentPage == page
                            )

                        null -> Unit

                        SummaryPageKind.SHARE ->
                            SharePage(
                                activity = activity,
                                state = state,
                                cardLayer = shareCardLayer,
                                isSharing = isSharing,
                                onShare = {
                                    if (!isSharing) {
                                        isSharing = true

                                        scope.launch {
                                            try {
                                                val bitmap = shareCardLayer.toImageBitmap().asAndroidBitmap()
                                                val imageUri = SummaryImageSharer.save(context, bitmap)

                                                context.startActivity(
                                                    SummaryImageSharer.chooserIntent(imageUri, activity.name)
                                                )
                                            } catch (error: CancellationException) {
                                                throw error
                                            } catch (error: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Nie udało się przygotować obrazka.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } finally {
                                                isSharing = false
                                            }
                                        }
                                    }
                                }
                            )
                    }
                }

                PageDots(
                    pageCount = pages.size,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = TatraSpacing.sm)
                )

                val isLastPage = pagerState.currentPage >= pages.size - 1

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

/**
 * „Nowe osiągnięcia”: przy pierwszym wejściu na planszę puchar „wskakuje”, a wiersze
 * pojawiają się jeden po drugim. Po obrocie ekranu animacja się nie powtarza.
 */
@Composable
private fun AchievementsPage(newAchievements: NewAchievements, isCurrent: Boolean) {
    var revealed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isCurrent) {
        if (isCurrent) {
            revealed = true
        }
    }

    val iconScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow),
        label = "achievementIcon"
    )

    SummaryPage(
        icon = Icons.Outlined.EmojiEvents,
        label = "Nowe osiągnięcia",
        iconScale = iconScale
    ) {
        val rows =
            newAchievements.achievements.map { achievement ->
                Triple(achievementIcon(achievement.kind), achievement.kind.title, achievement.kind.description)
            } +
                newAchievements.records.map { record ->
                    Triple(recordIcon(record.kind), "Rekord: " + recordValue(record), recordTitle(record.kind))
                }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TatraSpacing.md)
        ) {
            rows.forEachIndexed { index, (icon, title, subtitle) ->
                val delayMillis = 250 + index * 160

                AnimatedVisibility(
                    visible = revealed,
                    enter = fadeIn(tween(durationMillis = 350, delayMillis = delayMillis)) +
                        slideInVertically(tween(durationMillis = 350, delayMillis = delayMillis)) { it / 2 }
                ) {
                    AchievementRow(icon = icon, title = title, subtitle = subtitle)
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(TatraGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TatraWhite,
                modifier = Modifier.size(26.dp)
            )
        }

        Column(modifier = Modifier.padding(start = TatraSpacing.sm)) {
            Text(
                text = title,
                color = TatraWhite,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = subtitle,
                color = SummaryTextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SharePage(
    activity: ActivityEntity,
    state: ActivitySummaryUiState,
    cardLayer: GraphicsLayer,
    isSharing: Boolean,
    onShare: () -> Unit
) {
    SummaryPage(icon = Icons.Outlined.Share, label = "Na pamiątkę") {
        // Karta rysuje się do warstwy i z niej na ekran – obrazek jest dokładnie tym, co widać.
        ActivitySummaryShareCard(
            activity = activity,
            state = state,
            modifier = Modifier.drawWithContent {
                cardLayer.record {
                    this@drawWithContent.drawContent()
                }

                drawLayer(cardLayer)
            }
        )

        TatraSecondaryButton(
            onClick = onShare,
            enabled = !isSharing,
            modifier = Modifier.padding(top = TatraSpacing.md)
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Udostępnij")
        }
    }
}

// Wspólny układ planszy: ikona, etykieta i treść wyśrodkowane w pionie.
@Composable
private fun SummaryPage(
    icon: ImageVector,
    label: String,
    iconScale: Float = 1f,
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
                .scale(iconScale)
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
