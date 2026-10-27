package com.example.tatratracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraErrorState
import com.example.tatratracker.ui.components.TatraLoadingState
import com.example.tatratracker.ui.components.TatraPrimaryButton
import com.example.tatratracker.ui.components.TatraSecondaryButton
import com.example.tatratracker.ui.components.TatraSectionHeader
import com.example.tatratracker.ui.components.TatraStatCard
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted
import com.example.tatratracker.viewmodel.COMPLETED_TRAIL_THRESHOLD
import com.example.tatratracker.viewmodel.CompletedTrailsUiState
import com.example.tatratracker.viewmodel.CompletedTrailsViewModel
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CompletedTrailsScreen(
    onBack: () -> Unit,
    onOpenMap: () -> Unit,
    viewModel: CompletedTrailsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val result = state.result

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(TatraBackground)
                .verticalScroll(rememberScrollState())
    ) {
        TatraSubScreenHeader(
            title = "Ukończone szlaki",
            subtitle = "Twój postęp w Tatrach",
            onBack = onBack
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TatraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TatraSpacing.md)
        ) {
            TatraCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = TatraGreenVerySoft
            ) {
                Column {
                    Icon(
                        imageVector = Icons.Outlined.Route,
                        contentDescription = null,
                        tint = TatraGreen
                    )

                    Text(
                        text = summaryTitle(state),
                        color = TatraDark,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = TatraSpacing.sm)
                    )

                    Text(
                        text = summaryMessage(state),
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = TatraSpacing.xxs)
                    )
                }
            }

            if (state.isLoading) {
                TatraLoadingState(
                    message = "Dopasowuję ślady GPS do szlaków..."
                )
            }

            state.errorMessage?.let { message ->
                TatraErrorState(
                    message = message
                )
            }

            TatraSectionHeader(
                title = "Postęp"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TatraSpacing.sm)
            ) {
                TatraStatCard(
                    label = "Ukończono",
                    value = result?.let { formatPercent(it.completedFraction) } ?: "—%",
                    helper = "Sieci szlaków",
                    modifier = Modifier.weight(1f)
                )

                TatraStatCard(
                    label = "Przebyto",
                    value = result?.let { formatKilometers(it.matchedLengthMeters) } ?: "— km",
                    helper = "Po szlakach",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TatraSpacing.sm)
            ) {
                TatraStatCard(
                    label = "Szlaki",
                    value =
                        if (result != null) {
                            "${state.completedTrailCount} / ${state.totalTrailCount}"
                        } else {
                            "—"
                        },
                    helper = "Ukończone ≥ ${(COMPLETED_TRAIL_THRESHOLD * 100).roundToInt()}%",
                    modifier = Modifier.weight(1f)
                )

                TatraStatCard(
                    label = "Pozostało",
                    value =
                        result?.let {
                            formatKilometers(
                                (it.totalLengthMeters - it.matchedLengthMeters)
                                    .coerceAtLeast(0.0)
                            )
                        } ?: "— km",
                    helper = "Do przejścia",
                    modifier = Modifier.weight(1f)
                )
            }

            TatraPrimaryButton(
                onClick = onOpenMap
            ) {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = null
                )

                Text(
                    text = "Pokaż szlaki na mapie",
                    modifier = Modifier.padding(start = TatraSpacing.xs)
                )
            }

            TatraSecondaryButton(
                onClick = viewModel::recalculate,
                enabled = !state.isLoading
            ) {
                Text(
                    if (state.isLoading) {
                        "Liczę postęp..."
                    } else {
                        "Przelicz postęp"
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(TatraSpacing.md)
            )
        }
    }
}

private fun summaryTitle(
    state: CompletedTrailsUiState
): String {
    val result = state.result

    return when {
        result == null && state.isLoading ->
            "Liczę postęp"

        result == null ->
            "Nie udało się policzyć postępu"

        state.activityCount == 0 ->
            "Brak aktywności"

        result.matchedTrackCount == 0 ->
            "Brak przebytych szlaków"

        else ->
            "Przebyto ${formatKilometers(result.matchedLengthMeters)} szlaków"
    }
}

private fun summaryMessage(
    state: CompletedTrailsUiState
): String {
    val result = state.result

    return when {
        result == null && state.isLoading ->
            "Dopasowuję zapisane ślady GPS do znakowanych szlaków TPN i TANAP."

        result == null ->
            "Spróbuj przeliczyć postęp jeszcze raz."

        state.activityCount == 0 ->
            "Zaimportuj aktywność, a policzę, które szlaki już przeszedłeś."

        result.matchedTrackCount == 0 ->
            "Żadna z Twoich aktywności (${state.activityCount}) nie przebiega znakowanymi szlakami TPN i TANAP."

        else ->
            "Na podstawie ${result.matchedTrackCount} z ${state.activityCount} aktywności. " +
                    "Sieć szlaków TPN i TANAP liczy ${formatKilometers(result.totalLengthMeters)}."
    }
}

private fun formatKilometers(
    meters: Double
): String {
    return String.format(
        Locale.getDefault(),
        "%.1f km",
        meters / 1000.0
    )
}

private fun formatPercent(
    fraction: Double
): String {
    return String.format(
        Locale.getDefault(),
        "%.1f%%",
        fraction * 100.0
    )
}
