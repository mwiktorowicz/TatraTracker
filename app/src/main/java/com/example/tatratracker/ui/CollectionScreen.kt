package com.example.tatratracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.collection.CollectionItemProgress
import com.example.tatratracker.collection.CollectionProgress
import com.example.tatratracker.collection.CollectionSetProgress
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraErrorState
import com.example.tatratracker.ui.components.TatraFeatureDialog
import com.example.tatratracker.ui.components.TatraLoadingState
import com.example.tatratracker.ui.components.TatraProgressBar
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.format.TatraFormat
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenDark
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraSurfaceStrong
import com.example.tatratracker.ui.theme.TatraTextMuted
import com.example.tatratracker.ui.theme.TatraTextSoft
import com.example.tatratracker.ui.theme.TatraWhite
import com.example.tatratracker.viewmodel.CollectionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val BADGES_PER_ROW = 3

/**
 * „Kolekcja”: gotowe zestawy szczytów i schronisk jak książeczka odznak –
 * zdobyte w kolorze z datą pierwszego wejścia, niezdobyte szare.
 */
@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    viewModel: CollectionViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var openedItem by remember { mutableStateOf<CollectionItemProgress?>(null) }

    openedItem?.let { item ->
        TatraFeatureDialog(
            title = item.item.name,
            message = itemDetails(item),
            onDismiss = { openedItem = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TatraBackground)
            .verticalScroll(rememberScrollState())
    ) {
        TatraSubScreenHeader(
            title = "Kolekcja",
            subtitle = "Szczyty i schroniska Tatr",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TatraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TatraSpacing.md)
        ) {
            if (state.isLoading) {
                TatraLoadingState(message = "Wczytuję kolekcję...")
            }

            state.errorMessage?.let { message ->
                TatraErrorState(message = message)
            }

            state.progress?.let { progress ->
                CollectionSummaryCard(progress)

                progress.sets.forEach { set ->
                    CollectionSetCard(
                        set = set,
                        onItemClick = { item -> openedItem = item }
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.md))
        }
    }
}

@Composable
private fun CollectionSummaryCard(progress: CollectionProgress) {
    TatraCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = TatraGreenVerySoft
    ) {
        Column {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = TatraGreen
            )

            Text(
                text = "Zdobyto ${progress.collectedCount} z ${progress.totalCount}",
                color = TatraDark,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = TatraSpacing.sm)
            )

            TatraProgressBar(
                fraction = fractionOf(progress.collectedCount, progress.totalCount),
                modifier = Modifier.padding(top = TatraSpacing.xs)
            )

            Text(
                text =
                    "Miejsce jest zdobyte, gdy przeszła przez nie któraś z Twoich wędrówek " +
                        "albo gdy dodasz je w „Schroniska i punkty”.",
                color = TatraTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = TatraSpacing.xs)
            )
        }
    }
}

@Composable
private fun CollectionSetCard(
    set: CollectionSetProgress,
    onItemClick: (CollectionItemProgress) -> Unit
) {
    TatraCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(TatraSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = set.set.title,
                    color = TatraDark,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${set.collectedCount} z ${set.totalCount}",
                    color = if (set.collectedCount > 0) TatraGreenDark else TatraTextMuted,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = TatraSpacing.sm)
                )
            }

            if (set.set.description.isNotBlank()) {
                Text(
                    text = set.set.description,
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            TatraProgressBar(fraction = set.fraction.toFloat())

            set.items.chunked(BADGES_PER_ROW).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TatraSpacing.xs)
                ) {
                    rowItems.forEach { item ->
                        CollectionBadge(
                            item = item,
                            onClick = { onItemClick(item) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Niepełny ostatni rząd: puste miejsca trzymają szerokość kolumn.
                    repeat(BADGES_PER_ROW - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionBadge(
    item: CollectionItemProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val collected = item.isCollected

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = TatraSpacing.xs, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (collected) TatraGreen else TatraSurfaceStrong),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector =
                    if (item.item.category == SavedPlaceCategory.HUT) {
                        Icons.Outlined.Cottage
                    } else {
                        Icons.Outlined.Landscape
                    },
                contentDescription = if (collected) "Zdobyte" else "Niezdobyte",
                tint = if (collected) TatraWhite else TatraTextSoft,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = item.item.name,
            color = if (collected) TatraDark else TatraTextMuted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )

        item.item.elevationMeters?.let { elevation ->
            Text(
                text = TatraFormat.elevation(elevation),
                color = TatraTextSoft,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }

        item.firstVisitedAt?.let { timestamp ->
            Text(
                text = formatCollectionDate(timestamp),
                color = TatraGreenDark,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun itemDetails(item: CollectionItemProgress): String {
    val lines = mutableListOf<String>()

    val kind =
        if (item.item.category == SavedPlaceCategory.HUT) {
            "Schronisko"
        } else {
            "Szczyt"
        }

    lines +=
        item.item.elevationMeters
            ?.let { elevation -> "$kind · ${TatraFormat.altitude(elevation.toDouble())}" }
            ?: kind

    if (item.isCollected) {
        lines += "Twoje wizyty: ${item.visitCount}"

        item.firstVisitedAt?.let { lines += "Pierwsza: ${formatCollectionDate(it)}" }

        if (item.visitCount > 1) {
            item.lastVisitedAt?.let { lines += "Ostatnia: ${formatCollectionDate(it)}" }
        }
    } else {
        lines += "Jeszcze Cię tu nie było."
    }

    return lines.joinToString("\n")
}

private fun fractionOf(part: Int, total: Int): Float {
    return if (total > 0) part.toFloat() / total else 0f
}

private fun formatCollectionDate(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
}
