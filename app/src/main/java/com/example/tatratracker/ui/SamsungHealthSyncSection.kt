package com.example.tatratracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.tatratracker.health.SamsungHealthSyncService
import com.example.tatratracker.health.SyncSummary
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraErrorState
import com.example.tatratracker.ui.components.TatraLoadingState
import com.example.tatratracker.ui.components.TatraPrimaryButton
import com.example.tatratracker.ui.components.TatraSecondaryButton
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted
import com.example.tatratracker.ui.theme.TatraWarningSoft
import com.example.tatratracker.viewmodel.SamsungHealthSyncState

/** Sekcja „Samsung Health” na ekranie importu. */
@Composable
fun SamsungHealthSyncSection(
    state: SamsungHealthSyncState,
    onSync: (fullHistory: Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val isRunning = state is SamsungHealthSyncState.Running

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TatraSpacing.md),
    ) {
        TatraCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = TatraGreenVerySoft,
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        tint = TatraGreen,
                    )
                    Text(
                        text = "Samsung Health",
                        color = TatraDark,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = TatraSpacing.sm),
                    )
                }
                Text(
                    text = "Wędrówki nagrane w Samsung Health (np. z opaską Galaxy) trafią tu bez pobierania " +
                        "plików. Synchronizacja obejmuje ostatnie ${SamsungHealthSyncService.RECENT_DAYS} dni " +
                        "i odbywa się też automatycznie przy starcie aplikacji.",
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = TatraSpacing.sm),
                )
            }
        }

        TatraPrimaryButton(
            onClick = { onSync(false) },
            enabled = enabled && !isRunning,
        ) {
            Icon(imageVector = Icons.Outlined.Sync, contentDescription = null)
            Text(
                text = "Synchronizuj z Samsung Health",
                modifier = Modifier.padding(start = TatraSpacing.xs),
            )
        }

        TatraSecondaryButton(
            onClick = { onSync(true) },
            enabled = enabled && !isRunning,
        ) {
            Icon(imageVector = Icons.Outlined.History, contentDescription = null)
            Text(
                text = "Pobierz całą historię",
                modifier = Modifier.padding(start = TatraSpacing.xs),
            )
        }

        when (state) {
            SamsungHealthSyncState.Idle -> Unit

            is SamsungHealthSyncState.Running -> TatraLoadingState(message = runningMessage(state))

            is SamsungHealthSyncState.Finished -> Text(
                text = "Synchronizacja zakończona: ${summaryText(state.summary)}.",
                color = TatraTextMuted,
                style = MaterialTheme.typography.bodyMedium,
            )

            SamsungHealthSyncState.PermissionsMissing -> PermissionsMissingCard(
                onRetry = { onSync(false) },
                enabled = enabled,
            )

            is SamsungHealthSyncState.Failed -> TatraErrorState(message = state.message)
        }
    }
}

@Composable
private fun PermissionsMissingCard(onRetry: () -> Unit, enabled: Boolean) {
    TatraCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = TatraWarningSoft,
    ) {
        Column {
            Text(
                text = "Brak zgody Samsung Health",
                color = TatraDark,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Do pobrania wędrówek potrzebne są dwie zgody: na odczyt ćwiczeń i na lokalizację " +
                    "ćwiczeń (trasa GPS). Nadaj je w oknie Samsung Health.",
                color = TatraTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = TatraSpacing.xs, bottom = TatraSpacing.md),
            )
            TatraSecondaryButton(onClick = onRetry, enabled = enabled) {
                Text(text = "Spróbuj ponownie")
            }
        }
    }
}

private fun runningMessage(state: SamsungHealthSyncState.Running): String {
    val title = when {
        state.automatic -> "Automatyczna synchronizacja z Samsung Health…"
        state.fullHistory -> "Pobieranie całej historii z Samsung Health…"
        else -> "Synchronizacja z Samsung Health…"
    }
    return "$title\n${summaryText(state.progress).replaceFirstChar { it.uppercase() }}"
}

private fun summaryText(summary: SyncSummary): String =
    "zaimportowano ${summary.imported}, pominięto ${summary.skipped}, błędy ${summary.failed}"
