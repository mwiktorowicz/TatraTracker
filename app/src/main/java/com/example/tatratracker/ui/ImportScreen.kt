package com.example.tatratracker.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.tatratracker.fit.ActivityImportManager
import com.example.tatratracker.fit.ActivityImportResult
import com.example.tatratracker.poi.ActivityPoiDetectionResult
import com.example.tatratracker.poi.ActivityPoiDetector
import com.example.tatratracker.poi.PoiHistoryRebuildResult
import com.example.tatratracker.poi.PoiHistoryRebuilder
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraLoadingState
import com.example.tatratracker.ui.components.TatraPrimaryButton
import com.example.tatratracker.ui.components.TatraScreenHeader
import com.example.tatratracker.ui.components.TatraSecondaryButton
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImportScreen() {

    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    val importManager =
        remember {

            ActivityImportManager(
                context.applicationContext
            )
        }

    val poiDetector =
        remember {

            ActivityPoiDetector(
                context.applicationContext
            )
        }

    val poiHistoryRebuilder =
        remember {

            PoiHistoryRebuilder(
                context.applicationContext
            )
        }

    var statusMessage by
    remember {

        mutableStateOf(
            "Wybierz plik aktywności z pamięci telefonu."
        )
    }

    var isLoading by
    remember {

        mutableStateOf(
            false
        )
    }

    var importedActivity by
    remember {

        mutableStateOf<
                ActivityImportResult?
                >(
            null
        )
    }

    var poiDetectionMessage by
    remember {

        mutableStateOf<String?>(
            null
        )
    }

    /*
     * =====================================================
     * PRZELICZANIE STAREJ HISTORII
     * =====================================================
     */

    var isRebuildingHistory by
    remember {

        mutableStateOf(
            false
        )
    }

    var rebuildHistoryMessage by
    remember {

        mutableStateOf<String?>(
            null
        )
    }

    val activityPicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .OpenDocument()
        ) {
                uri: Uri? ->

            if (
                uri == null
            ) {

                return@rememberLauncherForActivityResult
            }

            scope.launch {

                isLoading =
                    true

                importedActivity =
                    null

                poiDetectionMessage =
                    null

                statusMessage =
                    "Analizowanie i zapisywanie aktywności..."

                val importResult =
                    importManager
                        .importActivity(
                            uri
                        )

                importResult
                    .onSuccess {
                            imported ->

                        importedActivity =
                            imported

                        statusMessage =
                            "Aktywność zapisana. Sprawdzam odwiedzone miejsca..."

                        /*
                         * Automatyczna analiza POI
                         * dla właśnie importowanej
                         * aktywności.
                         */
                        val poiResult =
                            poiDetector
                                .detectAndRegister(
                                    activityId =
                                        imported.activityId,
                                    visitedAt =
                                        imported.startTimeMillis
                                            ?: System
                                                .currentTimeMillis(),
                                    trackPoints =
                                        imported.trackPoints
                                )

                        poiResult
                            .onSuccess {
                                    detection ->

                                poiDetectionMessage =
                                    formatPoiDetectionResult(
                                        detection
                                    )

                                statusMessage =
                                    "Aktywność została zapisana."
                            }
                            .onFailure {
                                    error ->

                                poiDetectionMessage =
                                    buildString {

                                        append(
                                            "Aktywność została zapisana poprawnie, "
                                        )

                                        append(
                                            "ale nie udało się teraz sprawdzić odwiedzonych miejsc."
                                        )

                                        error.message
                                            ?.takeIf {
                                                it.isNotBlank()
                                            }
                                            ?.let {
                                                    message ->

                                                append(
                                                    "\n\n"
                                                )

                                                append(
                                                    message
                                                )
                                            }
                                    }

                                statusMessage =
                                    "Aktywność została zapisana."
                            }
                    }
                    .onFailure {
                            error ->

                        statusMessage =
                            error.message
                                ?: "Nie udało się zaimportować aktywności."
                    }

                isLoading =
                    false
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    TatraBackground
                )
                .verticalScroll(
                    rememberScrollState()
                )
    ) {

        TatraScreenHeader(
            title =
                "Dodaj aktywność",
            subtitle =
                "Importuj zapis przejścia z pliku FIT lub TCX"
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            TatraSpacing.lg
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    TatraSpacing.md
                )
        ) {

            /*
             * =================================================
             * IMPORT
             * =================================================
             */

            TatraCard(
                modifier =
                    Modifier.fillMaxWidth(),
                backgroundColor =
                    TatraGreenVerySoft
            ) {

                Column {

                    Text(
                        text =
                            "Import pliku",
                        color =
                            TatraDark,
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )

                    Text(
                        text =
                            "Plik może pochodzić ze Stravy, zegarka lub innej aplikacji. TatraTracker zapisze aktywność i ślad GPS lokalnie.",
                        color =
                            TatraTextMuted,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        modifier =
                            Modifier.padding(
                                top =
                                    6.dp
                            )
                    )

                    Row(
                        modifier =
                            Modifier.padding(
                                top =
                                    12.dp
                            ),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        TatraStatusBadge(
                            text =
                                "FIT",
                            style =
                                TatraBadgeStyle.Success
                        )

                        TatraStatusBadge(
                            text =
                                "TCX",
                            style =
                                TatraBadgeStyle.Success
                        )

                        TatraStatusBadge(
                            text =
                                "AUTO POI",
                            style =
                                TatraBadgeStyle.Success
                        )
                    }
                }
            }

            TatraPrimaryButton(
                onClick = {

                    activityPicker.launch(
                        arrayOf(
                            "*/*"
                        )
                    )
                },
                enabled =
                    !isLoading &&
                            !isRebuildingHistory
            ) {

                Icon(
                    imageVector =
                        Icons
                            .Outlined
                            .FileOpen,
                    contentDescription =
                        null
                )

                Text(
                    text =
                        "Wybierz plik",
                    modifier =
                        Modifier.padding(
                            start =
                                8.dp
                        )
                )
            }

            TatraSecondaryButton(
                onClick = {

                    val url =
                        Uri.parse(
                            "https://www.strava.com/athlete/training"
                        )

                    val intent =
                        Intent
                            .makeMainSelectorActivity(
                                Intent.ACTION_MAIN,
                                Intent.CATEGORY_APP_BROWSER
                            )
                            .apply {

                                data =
                                    url
                            }

                    context.startActivity(
                        intent
                    )
                },
                enabled =
                    !isLoading &&
                            !isRebuildingHistory
            ) {

                Icon(
                    imageVector =
                        Icons
                            .Outlined
                            .OpenInBrowser,
                    contentDescription =
                        null
                )

                Text(
                    text =
                        "Otwórz Stravę",
                    modifier =
                        Modifier.padding(
                            start =
                                8.dp
                        )
                )
            }

            if (
                isLoading
            ) {

                TatraLoadingState(
                    message =
                        statusMessage
                )

            } else {

                Text(
                    text =
                        statusMessage,
                    color =
                        TatraTextMuted,
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )
            }

            importedActivity
                ?.let {
                        imported ->

                    ImportSummary(
                        imported =
                            imported,
                        poiDetectionMessage =
                            poiDetectionMessage
                    )
                }

            /*
             * =================================================
             * STARSZE AKTYWNOŚCI
             * =================================================
             */

            Spacer(
                modifier =
                    Modifier.height(
                        TatraSpacing.xs
                    )
            )

            TatraCard(
                modifier =
                    Modifier.fillMaxWidth(),
                backgroundColor =
                    TatraGreenVerySoft
            ) {

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons
                                    .Outlined
                                    .Refresh,
                            contentDescription =
                                null,
                            tint =
                                TatraGreen
                        )

                        Text(
                            text =
                                "Starsze aktywności",
                            color =
                                TatraDark,
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            modifier =
                                Modifier.padding(
                                    start =
                                        TatraSpacing.sm
                                )
                        )
                    }

                    Text(
                        text =
                            "Jeśli masz aktywności zapisane przed dodaniem automatycznego wykrywania miejsc, możesz przeanalizować całą historię. Ponowne uruchomienie nie doda tych samych wizyt drugi raz.",
                        color =
                            TatraTextMuted,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        modifier =
                            Modifier.padding(
                                top =
                                    TatraSpacing.sm
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                TatraSpacing.md
                            )
                    )

                    TatraSecondaryButton(
                        onClick = {

                            if (
                                !isRebuildingHistory
                            ) {

                                scope.launch {

                                    isRebuildingHistory =
                                        true

                                    rebuildHistoryMessage =
                                        null

                                    val result =
                                        poiHistoryRebuilder
                                            .rebuild()

                                    result
                                        .onSuccess {
                                                rebuild ->

                                            rebuildHistoryMessage =
                                                formatHistoryRebuildResult(
                                                    rebuild
                                                )
                                        }
                                        .onFailure {
                                                error ->

                                            rebuildHistoryMessage =
                                                error.message
                                                    ?: "Nie udało się przeliczyć historii aktywności."
                                        }

                                    isRebuildingHistory =
                                        false
                                }
                            }
                        },
                        enabled =
                            !isLoading &&
                                    !isRebuildingHistory
                    ) {

                        Icon(
                            imageVector =
                                Icons
                                    .Outlined
                                    .Refresh,
                            contentDescription =
                                null
                        )

                        Text(
                            text =
                                "Przelicz moje aktywności",
                            modifier =
                                Modifier.padding(
                                    start =
                                        TatraSpacing.xs
                                )
                        )
                    }

                    if (
                        isRebuildingHistory
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    TatraSpacing.md
                                )
                        )

                        TatraLoadingState(
                            message =
                                "Analizuję wcześniejsze aktywności i odwiedzone miejsca..."
                        )
                    }

                    rebuildHistoryMessage
                        ?.let {
                                message ->

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        TatraSpacing.md
                                    )
                            )

                            Text(
                                text =
                                    message,
                                color =
                                    TatraTextMuted,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )
                        }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        TatraSpacing.md
                    )
            )
        }
    }
}

@Composable
private fun ImportSummary(
    imported: ActivityImportResult,
    poiDetectionMessage: String?
) {

    val totalSeconds =
        imported
            .durationSeconds
            .toLong()

    val hours =
        totalSeconds /
                3600

    val minutes =
        (
                totalSeconds %
                        3600
                ) /
                60

    val seconds =
        totalSeconds %
                60

    val duration =
        String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        )

    val distanceKm =
        imported.distanceMeters /
                1000.0

    TatraCard(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons
                            .Outlined
                            .CheckCircle,
                    contentDescription =
                        null,
                    tint =
                        TatraGreen
                )

                Text(
                    text =
                        "Zaimportowano",
                    color =
                        TatraDark,
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    modifier =
                        Modifier.padding(
                            start =
                                10.dp
                        )
                )
            }

            Text(
                text =
                    imported.fileName,
                color =
                    TatraTextMuted,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                modifier =
                    Modifier.padding(
                        top =
                            4.dp
                    )
            )

            imported.startTimeMillis
                ?.let {
                        timestamp ->

                    SummaryRow(
                        label =
                            "Data",
                        value =
                            formatDate(
                                timestamp
                            )
                    )
                }

            SummaryRow(
                label =
                    "Dystans",
                value =
                    String.format(
                        Locale.getDefault(),
                        "%.2f km",
                        distanceKm
                    )
            )

            SummaryRow(
                label =
                    "Czas",
                value =
                    duration
            )

            SummaryRow(
                label =
                    "Kalorie",
                value =
                    if (
                        imported.calories > 0
                    ) {

                        "${imported.calories} kcal"

                    } else {

                        "Brak danych"
                    }
            )

            SummaryRow(
                label =
                    "Śr. tętno",
                value =
                    if (
                        imported.averageHeartRate > 0
                    ) {

                        "${imported.averageHeartRate} bpm"

                    } else {

                        "Brak danych"
                    }
            )

            SummaryRow(
                label =
                    "Punkty GPS",
                value =
                    imported
                        .trackPoints
                        .size
                        .toString()
            )

            poiDetectionMessage
                ?.let {
                        message ->

                    Spacer(
                        modifier =
                            Modifier.height(
                                TatraSpacing.md
                            )
                    )

                    TatraCard(
                        modifier =
                            Modifier.fillMaxWidth(),
                        backgroundColor =
                            TatraGreenVerySoft
                    ) {

                        Column {

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector =
                                        Icons
                                            .Outlined
                                            .Landscape,
                                    contentDescription =
                                        null,
                                    tint =
                                        TatraGreen
                                )

                                Text(
                                    text =
                                        "Odwiedzone miejsca",
                                    color =
                                        TatraDark,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,
                                    modifier =
                                        Modifier.padding(
                                            start =
                                                TatraSpacing.xs
                                        )
                                )
                            }

                            Text(
                                text =
                                    message,
                                color =
                                    TatraTextMuted,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,
                                modifier =
                                    Modifier.padding(
                                        top =
                                            TatraSpacing.sm
                                    )
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top =
                        10.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text =
                label,
            color =
                TatraTextMuted,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            modifier =
                Modifier.weight(
                    1f
                )
        )

        Text(
            text =
                value,
            color =
                TatraDark,
            style =
                MaterialTheme
                    .typography
                    .titleSmall
        )
    }
}

private fun formatPoiDetectionResult(
    result: ActivityPoiDetectionResult
): String {

    if (
        result.skippedNoTrack
    ) {

        return "Aktywność nie zawiera wystarczającego śladu GPS do automatycznego wykrywania miejsc."
    }

    if (
        result.skippedOutsideTatras
    ) {

        return "Ślad nie przebiega przez obszar Tatr, dlatego analiza górskich POI została pominięta."
    }

    if (
        result.matchedPlaces == 0
    ) {

        return "Nie wykryto przy tej trasie schronisk, szczytów ani przełęczy spełniających kryteria zaliczenia."
    }

    val names =
        result
            .detectedNames
            .take(
                6
            )

    val remaining =
        result
            .detectedNames
            .size -
                names.size

    return buildString {

        append(
            "Wykryto ${result.matchedPlaces} "
        )

        append(
            placeWord(
                result.matchedPlaces
            )
        )

        append(
            " przy trasie."
        )

        if (
            result.newVisits > 0
        ) {

            append(
                "\n"
            )

            append(
                "Dodano ${result.newVisits} "
            )

            append(
                visitWord(
                    result.newVisits
                )
            )

            append(
                "."
            )
        }

        if (
            result.newPlaces > 0
        ) {

            append(
                "\n"
            )

            append(
                "Nowe unikalne miejsca: ${result.newPlaces}."
            )
        }

        if (
            names.isNotEmpty()
        ) {

            append(
                "\n\n"
            )

            append(
                names.joinToString(
                    separator =
                        " • "
                )
            )

            if (
                remaining > 0
            ) {

                append(
                    " • +$remaining"
                )
            }
        }
    }
}

private fun formatHistoryRebuildResult(
    result: PoiHistoryRebuildResult
): String {

    if (
        result.totalActivities == 0
    ) {

        return "Nie masz jeszcze zapisanych aktywności do przeanalizowania."
    }

    return buildString {

        append(
            "Sprawdzono ${result.totalActivities} "
        )

        append(
            activityWord(
                result.totalActivities
            )
        )

        append(
            "."
        )

        if (
            result.analyzedActivities > 0
        ) {

            append(
                "\nAktywności przeanalizowane w Tatrach: ${result.analyzedActivities}."
            )
        }

        if (
            result.matchedPlaces > 0
        ) {

            append(
                "\nWykrycia miejsc na trasach: ${result.matchedPlaces}."
            )
        }

        if (
            result.newVisits > 0
        ) {

            append(
                "\nDodano nowych wizyt: ${result.newVisits}."
            )

        } else if (
            result.matchedPlaces > 0
        ) {

            append(
                "\nNie dodano nowych wizyt — wykryte przejścia były już zapisane."
            )
        }

        if (
            result.newPlaces > 0
        ) {

            append(
                "\nNowe unikalne miejsca: ${result.newPlaces}."
            )
        }

        if (
            result.activitiesOutsideTatras > 0
        ) {

            append(
                "\nPoza Tatrami: ${result.activitiesOutsideTatras}."
            )
        }

        if (
            result.activitiesWithoutTrack > 0
        ) {

            append(
                "\nBez wystarczającego śladu GPS: ${result.activitiesWithoutTrack}."
            )
        }

        if (
            result.failedActivities > 0
        ) {

            append(
                "\nNie udało się przeanalizować: ${result.failedActivities}."
            )
        }

        if (
            result.detectedNames.isNotEmpty()
        ) {

            val names =
                result
                    .detectedNames
                    .take(
                        8
                    )

            val remaining =
                result
                    .detectedNames
                    .size -
                        names.size

            append(
                "\n\n"
            )

            append(
                names.joinToString(
                    separator =
                        " • "
                )
            )

            if (
                remaining > 0
            ) {

                append(
                    " • +$remaining"
                )
            }
        }
    }
}

private fun activityWord(
    count: Int
): String {

    return when {

        count == 1 ->
            "aktywność"

        count %
                10 in 2..4 &&
                count %
                100 !in 12..14 ->
            "aktywności"

        else ->
            "aktywności"
    }
}

private fun placeWord(
    count: Int
): String {

    return when {

        count == 1 ->
            "miejsce"

        count %
                10 in 2..4 &&
                count %
                100 !in 12..14 ->
            "miejsca"

        else ->
            "miejsc"
    }
}

private fun visitWord(
    count: Int
): String {

    return when {

        count == 1 ->
            "wizytę"

        count %
                10 in 2..4 &&
                count %
                100 !in 12..14 ->
            "wizyty"

        else ->
            "wizyt"
    }
}

private fun formatDate(
    timestamp: Long
): String {

    val formatter =
        SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale.getDefault()
        )

    return formatter.format(
        Date(
            timestamp
        )
    )
}