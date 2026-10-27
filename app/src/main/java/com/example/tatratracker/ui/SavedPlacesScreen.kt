package com.example.tatratracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraEmptyState
import com.example.tatratracker.ui.components.TatraPrimaryButton
import com.example.tatratracker.ui.components.TatraSecondaryButton
import com.example.tatratracker.ui.components.TatraSectionHeader
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.map.SavedPlaceFocusMap
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDanger
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraOutline
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted
import com.example.tatratracker.viewmodel.SavedPlaceSummary
import com.example.tatratracker.viewmodel.SavedPlacesViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedPlacesScreen(
    onBack: () -> Unit,
    onOpenMap: () -> Unit,
    viewModel:
    SavedPlacesViewModel =
        viewModel()
) {

    val scope =
        rememberCoroutineScope()

    val placeSummaries by
    viewModel
        .placeSummaries
        .collectAsState()

    var selectedPlace by
    remember {
        mutableStateOf<
                SavedPlaceSummary?
                >(
            null
        )
    }

    var showAddDialog by
    remember {
        mutableStateOf(
            false
        )
    }

    var placeToDelete by
    remember {
        mutableStateOf<
                SavedPlaceEntity?
                >(
            null
        )
    }

    var statusDialog by
    remember {
        mutableStateOf<
                Pair<String, String>?
                >(
            null
        )
    }

    /*
     * Jeżeli użytkownik kliknął miejsce,
     * pokazujemy pełnoekranową mapę.
     *
     * Nie zmieniamy route Navigation Compose,
     * więc powrót wraca dokładnie do tej listy.
     */
    selectedPlace
        ?.let {
                summary ->

            SavedPlaceFocusMap(
                summary =
                    summary,
                onBack = {

                    selectedPlace =
                        null
                }
            )

            return
        }

    val hutCount =
        placeSummaries.count {
            it.place.category ==
                    SavedPlaceCategory.HUT
        }

    val mountainCount =
        placeSummaries.count {
            it.place.category ==
                    SavedPlaceCategory.MOUNTAIN
        }

    val otherCount =
        placeSummaries.count {
            it.place.category ==
                    SavedPlaceCategory.OTHER
        }

    val totalVisits =
        placeSummaries.sumOf {
            it.visitCount
        }

    if (
        showAddDialog
    ) {

        AddSavedPlaceDialog(
            onDismiss = {

                showAddDialog =
                    false
            },
            onSave = {
                    name,
                    category,
                    latitude,
                    longitude,
                    note ->

                scope.launch {

                    viewModel
                        .savePlace(
                            name =
                                name,
                            category =
                                category,
                            latitude =
                                latitude,
                            longitude =
                                longitude,
                            note =
                                note
                        )
                        .onSuccess {

                            showAddDialog =
                                false

                            statusDialog =
                                "Punkt zapisany" to
                                        "„$name” został zapisany jako odwiedzone miejsce."
                        }
                        .onFailure {
                                error ->

                            statusDialog =
                                "Nie udało się zapisać punktu" to
                                        (
                                                error.message
                                                    ?: "Nieznany błąd zapisu."
                                                )
                        }
                }
            }
        )
    }

    placeToDelete
        ?.let {
                place ->

            AlertDialog(
                onDismissRequest = {

                    placeToDelete =
                        null
                },
                title = {

                    Text(
                        "Usunąć miejsce?"
                    )
                },
                text = {

                    Text(
                        "„${place.name}” zostanie usunięty razem z całą historią jego wizyt."
                    )
                },
                confirmButton = {

                    TextButton(
                        onClick = {

                            viewModel
                                .deletePlace(
                                    place.id
                                )

                            placeToDelete =
                                null
                        }
                    ) {

                        Text(
                            text =
                                "Usuń",
                            color =
                                TatraDanger
                        )
                    }
                },
                dismissButton = {

                    TextButton(
                        onClick = {

                            placeToDelete =
                                null
                        }
                    ) {

                        Text(
                            "Anuluj"
                        )
                    }
                }
            )
        }

    statusDialog
        ?.let {
                dialog ->

            AlertDialog(
                onDismissRequest = {

                    statusDialog =
                        null
                },
                title = {

                    Text(
                        dialog.first
                    )
                },
                text = {

                    Text(
                        dialog.second
                    )
                },
                confirmButton = {

                    TextButton(
                        onClick = {

                            statusDialog =
                                null
                        }
                    ) {

                        Text(
                            "OK"
                        )
                    }
                }
            )
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

        TatraSubScreenHeader(
            title =
                "Odwiedzone miejsca",
            subtitle =
                if (
                    placeSummaries.isEmpty()
                ) {

                    "Schroniska, szczyty i inne punkty"

                } else {

                    "${placeSummaries.size} miejsc • $totalVisits wizyt"
                },
            onBack =
                onBack
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

            TatraSectionHeader(
                title =
                    "Unikalne miejsca"
            )

            TatraCard(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column {

                    CategorySummaryRow(
                        title =
                            "Schroniska",
                        subtitle =
                            "Unikalne odwiedzone schroniska",
                        icon =
                            Icons.Outlined.Place,
                        count =
                            hutCount
                    )

                    HorizontalDivider(
                        color =
                            TatraOutline
                    )

                    CategorySummaryRow(
                        title =
                            "Szczyty i przełęcze",
                        subtitle =
                            "Unikalne odwiedzone miejsca górskie",
                        icon =
                            Icons.Outlined.Landscape,
                        count =
                            mountainCount
                    )

                    HorizontalDivider(
                        color =
                            TatraOutline
                    )

                    CategorySummaryRow(
                        title =
                            "Pozostałe",
                        subtitle =
                            "Inne odwiedzone miejsca",
                        icon =
                            Icons.Outlined.Route,
                        count =
                            otherCount
                    )
                }
            }

            TatraSectionHeader(
                title =
                    "Dodaj ręcznie"
            )

            TatraPrimaryButton(
                onClick = {

                    showAddDialog =
                        true
                }
            ) {

                Icon(
                    imageVector =
                        Icons
                            .Outlined
                            .AddLocationAlt,
                    contentDescription =
                        null
                )

                Text(
                    text =
                        "Dodaj odwiedzone miejsce",
                    modifier =
                        Modifier.padding(
                            start =
                                TatraSpacing.xs
                        )
                )
            }

            TatraSecondaryButton(
                onClick =
                    onOpenMap
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Map,
                    contentDescription =
                        null
                )

                Text(
                    text =
                        "Otwórz główną mapę",
                    modifier =
                        Modifier.padding(
                            start =
                                TatraSpacing.xs
                        )
                )
            }

            TatraSectionHeader(
                title =
                    "Historia miejsc"
            )

            if (
                placeSummaries.isEmpty()
            ) {

                TatraEmptyState(
                    title =
                        "Brak odwiedzonych miejsc",
                    message =
                        "Po analizie Twoich aktywności będą pojawiały się tutaj schroniska, szczyty, przełęcze i inne punkty.",
                    icon =
                        Icons.Outlined.Place
                )

            } else {

                Text(
                    text =
                        "Dotknij miejsca, aby zobaczyć je na mapie.",
                    color =
                        TatraTextMuted,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                placeSummaries
                    .forEach {
                            summary ->

                        SavedPlaceCard(
                            summary =
                                summary,
                            onOpen = {

                                selectedPlace =
                                    summary
                            },
                            onDelete = {

                                placeToDelete =
                                    summary.place
                            }
                        )
                    }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        TatraSpacing.xl
                    )
            )
        }
    }
}

@Composable
private fun CategorySummaryRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    count: Int
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        TatraSpacing.sm
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                icon,
            contentDescription =
                null,
            tint =
                TatraGreen
        )

        Column(
            modifier =
                Modifier
                    .weight(
                        1f
                    )
                    .padding(
                        start =
                            TatraSpacing.sm
                    )
        ) {

            Text(
                text =
                    title,
                color =
                    TatraDark,
                style =
                    MaterialTheme
                        .typography
                        .titleSmall
            )

            Text(
                text =
                    subtitle,
                color =
                    TatraTextMuted,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                modifier =
                    Modifier.padding(
                        top =
                            2.dp
                    )
            )
        }

        TatraStatusBadge(
            text =
                count.toString(),
            style =
                if (
                    count > 0
                ) {

                    TatraBadgeStyle.Success

                } else {

                    TatraBadgeStyle.Neutral
                }
        )
    }
}

@Composable
private fun SavedPlaceCard(
    summary: SavedPlaceSummary,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {

    val place =
        summary.place

    TatraCard(
        modifier =
            Modifier.fillMaxWidth(),
        onClick =
            onOpen
    ) {

        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        iconForCategory(
                            place.category
                        ),
                    contentDescription =
                        null,
                    tint =
                        TatraGreen
                )

                Column(
                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .padding(
                                start =
                                    TatraSpacing.sm
                            )
                ) {

                    Text(
                        text =
                            place.name,
                        color =
                            TatraDark,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    Text(
                        text =
                            SavedPlaceCategory
                                .label(
                                    place.category
                                ),
                        color =
                            TatraTextMuted,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        modifier =
                            Modifier.padding(
                                top =
                                    2.dp
                            )
                    )
                }

                TatraStatusBadge(
                    text =
                        if (
                            summary.visitCount ==
                            1
                        ) {

                            "1 WIZYTA"

                        } else {

                            "${summary.visitCount} WIZYT"
                        },
                    style =
                        TatraBadgeStyle.Success
                )

                Icon(
                    imageVector =
                        Icons
                            .Outlined
                            .KeyboardArrowRight,
                    contentDescription =
                        "Pokaż na mapie",
                    tint =
                        TatraTextMuted,
                    modifier =
                        Modifier.padding(
                            start =
                                TatraSpacing.xs
                        )
                )

                IconButton(
                    onClick =
                        onDelete
                ) {

                    Icon(
                        imageVector =
                            Icons
                                .Outlined
                                .DeleteOutline,
                        contentDescription =
                            "Usuń miejsce",
                        tint =
                            TatraTextMuted
                    )
                }
            }

            TatraCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top =
                                TatraSpacing.sm
                        ),
                backgroundColor =
                    TatraGreenVerySoft
            ) {

                Column {

                    Text(
                        text =
                            "Odwiedzono",
                        color =
                            TatraTextMuted,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    Text(
                        text =
                            visitCountText(
                                summary.visitCount
                            ),
                        color =
                            TatraDark,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        modifier =
                            Modifier.padding(
                                top =
                                    2.dp
                            )
                    )

                    summary.lastVisitedAt
                        ?.let {
                                timestamp ->

                            Text(
                                text =
                                    "Ostatnio: ${
                                        formatVisitDate(
                                            timestamp
                                        )
                                    }",
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
                        }
                }
            }

            if (
                summary.visitCount >
                1
            ) {

                summary.firstVisitedAt
                    ?.let {
                            timestamp ->

                        Text(
                            text =
                                "Pierwsza wizyta: ${
                                    formatVisitDate(
                                        timestamp
                                    )
                                }",
                            color =
                                TatraTextMuted,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            modifier =
                                Modifier.padding(
                                    top =
                                        TatraSpacing.sm
                                )
                        )
                    }
            }

            Text(
                text =
                    String.format(
                        Locale.getDefault(),
                        "%.5f, %.5f",
                        place.latitude,
                        place.longitude
                    ),
                color =
                    TatraTextMuted,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                modifier =
                    Modifier.padding(
                        top =
                            TatraSpacing.sm
                    )
            )

            place.note
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                        note ->

                    Text(
                        text =
                            note,
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

@Composable
private fun AddSavedPlaceDialog(
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        Double,
        Double,
        String?
    ) -> Unit
) {

    var name by
    remember {
        mutableStateOf(
            ""
        )
    }

    var category by
    remember {
        mutableStateOf(
            SavedPlaceCategory.OTHER
        )
    }

    var latitudeText by
    remember {
        mutableStateOf(
            ""
        )
    }

    var longitudeText by
    remember {
        mutableStateOf(
            ""
        )
    }

    var note by
    remember {
        mutableStateOf(
            ""
        )
    }

    var validationError by
    remember {
        mutableStateOf<String?>(
            null
        )
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {

            Text(
                "Dodaj odwiedzone miejsce"
            )
        },
        text = {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        )
            ) {

                OutlinedTextField(
                    value =
                        name,
                    onValueChange = {

                        name =
                            it

                        validationError =
                            null
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine =
                        true,
                    label = {

                        Text(
                            "Nazwa"
                        )
                    },
                    placeholder = {

                        Text(
                            "Np. Murowaniec"
                        )
                    }
                )

                Text(
                    text =
                        "Kategoria",
                    color =
                        TatraDark,
                    style =
                        MaterialTheme
                            .typography
                            .titleSmall,
                    modifier =
                        Modifier.padding(
                            top =
                                TatraSpacing.md
                        )
                )

                CategoryRadioRow(
                    title =
                        "Schronisko",
                    selected =
                        category ==
                                SavedPlaceCategory.HUT,
                    onClick = {

                        category =
                            SavedPlaceCategory.HUT
                    }
                )

                CategoryRadioRow(
                    title =
                        "Szczyt / przełęcz",
                    selected =
                        category ==
                                SavedPlaceCategory.MOUNTAIN,
                    onClick = {

                        category =
                            SavedPlaceCategory.MOUNTAIN
                    }
                )

                CategoryRadioRow(
                    title =
                        "Inne miejsce",
                    selected =
                        category ==
                                SavedPlaceCategory.OTHER,
                    onClick = {

                        category =
                            SavedPlaceCategory.OTHER
                    }
                )

                OutlinedTextField(
                    value =
                        latitudeText,
                    onValueChange = {

                        latitudeText =
                            it

                        validationError =
                            null
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top =
                                    TatraSpacing.sm
                            ),
                    singleLine =
                        true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),
                    label = {

                        Text(
                            "Szerokość geograficzna"
                        )
                    }
                )

                OutlinedTextField(
                    value =
                        longitudeText,
                    onValueChange = {

                        longitudeText =
                            it

                        validationError =
                            null
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top =
                                    TatraSpacing.sm
                            ),
                    singleLine =
                        true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),
                    label = {

                        Text(
                            "Długość geograficzna"
                        )
                    }
                )

                OutlinedTextField(
                    value =
                        note,
                    onValueChange = {

                        note =
                            it
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top =
                                    TatraSpacing.sm
                            ),
                    minLines =
                        2,
                    maxLines =
                        4,
                    label = {

                        Text(
                            "Notatka"
                        )
                    }
                )

                validationError
                    ?.let {
                            error ->

                        Text(
                            text =
                                error,
                            color =
                                TatraDanger,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            modifier =
                                Modifier.padding(
                                    top =
                                        TatraSpacing.sm
                                )
                        )
                    }
            }
        },
        confirmButton = {

            TextButton(
                onClick = {

                    val cleanName =
                        name.trim()

                    val latitude =
                        parseCoordinate(
                            latitudeText
                        )

                    val longitude =
                        parseCoordinate(
                            longitudeText
                        )

                    when {

                        cleanName.isBlank() -> {

                            validationError =
                                "Podaj nazwę miejsca."
                        }

                        latitude ==
                                null -> {

                            validationError =
                                "Podaj poprawną szerokość geograficzną."
                        }

                        latitude !in
                                -90.0..90.0 -> {

                            validationError =
                                "Szerokość musi być w zakresie -90 do 90."
                        }

                        longitude ==
                                null -> {

                            validationError =
                                "Podaj poprawną długość geograficzną."
                        }

                        longitude !in
                                -180.0..180.0 -> {

                            validationError =
                                "Długość musi być w zakresie -180 do 180."
                        }

                        else -> {

                            onSave(
                                cleanName,
                                category,
                                latitude,
                                longitude,
                                note
                            )
                        }
                    }
                }
            ) {

                Text(
                    "Zapisz"
                )
            }
        },
        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Anuluj"
                )
            }
        }
    )
}

@Composable
private fun CategoryRadioRow(
    title: String,
    selected: Boolean,
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
                    vertical =
                        4.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        RadioButton(
            selected =
                selected,
            onClick =
                onClick
        )

        Text(
            text =
                title,
            color =
                TatraDark,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            modifier =
                Modifier.padding(
                    start =
                        TatraSpacing.xs
                )
        )
    }
}

private fun parseCoordinate(
    value: String
): Double? {

    return value
        .trim()
        .replace(
            ',',
            '.'
        )
        .toDoubleOrNull()
}

private fun iconForCategory(
    category: String
): ImageVector {

    return when (
        category
    ) {

        SavedPlaceCategory.HUT ->
            Icons.Outlined.Place

        SavedPlaceCategory.MOUNTAIN ->
            Icons.Outlined.Landscape

        else ->
            Icons.Outlined.Route
    }
}

private fun visitCountText(
    count: Int
): String {

    return when (
        count
    ) {

        1 ->
            "1 raz"

        else ->
            "$count razy"
    }
}

private fun formatVisitDate(
    timestamp: Long
): String {

    val formatter =
        SimpleDateFormat(
            "dd MMM yyyy • HH:mm",
            Locale.getDefault()
        )

    return formatter.format(
        Date(
            timestamp
        )
    )
}