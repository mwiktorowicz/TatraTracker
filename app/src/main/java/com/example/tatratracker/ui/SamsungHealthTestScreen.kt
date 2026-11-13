package com.example.tatratracker.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraPrimaryButton
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDanger
import com.example.tatratracker.ui.theme.TatraDangerSoft
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.DataSource
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.entries.ExerciseSession
import com.samsung.android.sdk.health.data.error.ErrorCode
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.Ordering
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

/*
 * Ekran testowy (spike): czy treningi nagrane telefonem + Galaxy Fit3 da się
 * odczytać z Samsung Health przez Samsung Health Data SDK w zakresie pliku
 * FIT/TCX (trasa GPS, czas, wysokość, tętno, prędkość).
 * Kod do wyrzucenia albo przepisania po teście – patrz docs/PLAN.md.
 */

private const val LOG_TAG = "SamsungHealthTest"
private const val DAYS_BACK = 90L
private const val MAX_PAGES = 50

private val polishLocale: Locale = Locale.forLanguageTag("pl-PL")
private val startFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

private data class SessionSummary(
    val startTime: Instant,
    val zone: ZoneId,
    val exerciseType: String,
    val duration: Duration?,
    val distanceMeters: Float?,
    val routePoints: Int,
    val routePointsWithAltitude: Int,
    val minRouteAltitude: Float?,
    val maxRouteAltitude: Float?,
    val altitudeGain: Float?,
    val logEntries: Int,
    val logEntriesWithHeartRate: Int,
    val logEntriesWithSpeed: Int,
    val meanHeartRate: Float?,
    val maxHeartRate: Float?,
    val source: String,
)

private data class ReadResult(
    val grantedPermissions: List<String>,
    val missingPermissions: List<String>,
    val dataPointCount: Int,
    val pageCount: Int,
    val sessions: List<SessionSummary>,
)

private sealed interface TestState {
    data object Idle : TestState
    data class Running(val step: String) : TestState
    data class Done(val result: ReadResult) : TestState
    data class Failed(val report: String) : TestState
}

@Composable
fun SamsungHealthTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<TestState>(TestState.Idle) }

    fun runTest() {
        val activity = context.findActivity()
        if (activity == null) {
            state = TestState.Failed("Krok: start\nNie znaleziono Activity w kontekście ekranu.")
            return
        }
        scope.launch {
            var step = "start"
            state = TestState.Running(step)
            state = try {
                val result = readSamsungHealthExercises(activity) { newStep ->
                    step = newStep
                    state = TestState.Running(newStep)
                }
                TestState.Done(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(LOG_TAG, "Test nie powiódł się (krok: $step)", e)
                TestState.Failed(describeFailure(step, e, tryResolve(e, activity)))
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TatraBackground),
        contentPadding = PaddingValues(bottom = TatraSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(TatraSpacing.sm),
    ) {
        item {
            TatraSubScreenHeader(
                title = "Test Samsung Health",
                subtitle = "Treningi z ostatnich $DAYS_BACK dni przez Samsung Health Data SDK",
                onBack = onBack,
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = TatraSpacing.lg)) {
                TatraCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = TatraGreenVerySoft,
                ) {
                    Text(
                        text = "Ekran testowy. Wymaga Samsung Health 6.30.2+ z włączonym " +
                            "„Developer Mode for Data Read”. Nie działa na emulatorze.",
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(modifier = Modifier.size(TatraSpacing.md))

                TatraPrimaryButton(
                    onClick = { runTest() },
                    enabled = state !is TestState.Running,
                ) {
                    Text("Połącz i pobierz treningi")
                }
            }
        }

        when (val current = state) {
            TestState.Idle -> Unit

            is TestState.Running -> item {
                RunningCard(step = current.step)
            }

            is TestState.Failed -> item {
                FailureCard(report = current.report)
            }

            is TestState.Done -> {
                item {
                    SummaryCard(result = current.result)
                }
                items(current.result.sessions) { session ->
                    SessionCard(session = session)
                }
            }
        }
    }
}

@Composable
private fun RunningCard(step: String) {
    TatraCard(
        modifier = Modifier
            .padding(horizontal = TatraSpacing.lg)
            .fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = TatraGreen,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(TatraSpacing.sm))
            Text(
                text = "$step…",
                color = TatraDark,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FailureCard(report: String) {
    TatraCard(
        modifier = Modifier
            .padding(horizontal = TatraSpacing.lg)
            .fillMaxWidth(),
        backgroundColor = TatraDangerSoft,
    ) {
        Column {
            Text(
                text = "Błąd",
                color = TatraDanger,
                style = MaterialTheme.typography.titleMedium,
            )
            SelectionContainer {
                Text(
                    text = report,
                    color = TatraDark,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = TatraSpacing.xs),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(result: ReadResult) {
    val lines = buildList {
        add("Przyznane uprawnienia: ${result.grantedPermissions.joinToString().ifEmpty { "brak" }}")
        add("Brakujące uprawnienia: ${result.missingPermissions.joinToString().ifEmpty { "brak" }}")
        add("Rekordy ćwiczeń: ${result.dataPointCount} (stron odpowiedzi: ${result.pageCount})")
        add("Sesje: ${result.sessions.size}")
        if (result.sessions.isEmpty()) {
            add("Brak treningów w ostatnich $DAYS_BACK dniach.")
        }
    }

    TatraCard(
        modifier = Modifier
            .padding(horizontal = TatraSpacing.lg)
            .fillMaxWidth(),
        backgroundColor = TatraGreenVerySoft,
    ) {
        Column {
            Text(
                text = "Wynik",
                color = TatraDark,
                style = MaterialTheme.typography.titleMedium,
            )
            SelectionContainer {
                Text(
                    text = lines.joinToString("\n"),
                    color = TatraDark,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = TatraSpacing.xs),
                )
            }
        }
    }
}

@Composable
private fun SessionCard(session: SessionSummary) {
    val title = "${startFormatter.format(session.startTime.atZone(session.zone))} · ${session.exerciseType}"
    val lines = listOf(
        "Czas: ${durationText(session.duration)} · dystans: ${kmText(session.distanceMeters)}",
        "Trasa: ${session.routePoints} pkt, z wysokością: ${session.routePointsWithAltitude}",
        "Wysokość z punktów: min ${metersText(session.minRouteAltitude)}, " +
            "maks. ${metersText(session.maxRouteAltitude)}",
        "altitudeGain: ${metersText(session.altitudeGain)}",
        "Log: ${session.logEntries} wpisów, z tętnem: ${session.logEntriesWithHeartRate}, " +
            "z prędkością: ${session.logEntriesWithSpeed}",
        "Tętno: śr. ${bpmText(session.meanHeartRate)}, maks. ${bpmText(session.maxHeartRate)}",
        "Źródło: ${session.source}",
    )

    TatraCard(
        modifier = Modifier
            .padding(horizontal = TatraSpacing.lg)
            .fillMaxWidth(),
    ) {
        SelectionContainer {
            Column {
                Text(
                    text = title,
                    color = TatraDark,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = lines.joinToString("\n"),
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = TatraSpacing.xxs),
                )
            }
        }
    }
}

private suspend fun readSamsungHealthExercises(
    activity: Activity,
    onStep: (String) -> Unit,
): ReadResult {
    val exerciseRead = Permission.of(DataTypes.EXERCISE, AccessType.READ)
    val permissions = setOf(
        exerciseRead,
        Permission.of(DataTypes.EXERCISE_LOCATION, AccessType.READ),
    )

    onStep("Łączenie z Samsung Health")
    val store = HealthDataService.getStore(activity.applicationContext)

    onStep("Sprawdzanie uprawnień")
    var granted = store.getGrantedPermissions(permissions)
    if (!granted.containsAll(permissions)) {
        onStep("Prośba o uprawnienia")
        granted = store.requestPermissions(permissions, activity)
    }
    val missing = permissions - granted
    if (exerciseRead in missing) {
        throw IllegalStateException(
            "Brak zgody na odczyt ćwiczeń. Brakujące: ${missing.joinToString { it.describe() }}"
        )
    }

    onStep("Odczyt treningów z ostatnich $DAYS_BACK dni")
    val end = LocalDateTime.now()
    val timeFilter = LocalTimeFilter.of(end.minusDays(DAYS_BACK), end)
    val dataPoints = mutableListOf<HealthDataPoint>()
    var pageToken: String? = null
    var pageCount = 0
    do {
        val builder = DataTypes.EXERCISE.readDataRequestBuilder
            .setLocalTimeFilter(timeFilter)
            .setOrdering(Ordering.DESC)
        pageToken?.let { builder.setPageToken(it) }
        val response = store.readData(builder.build())
        dataPoints += response.dataList
        pageToken = response.pageToken?.takeIf { it.isNotEmpty() }
        pageCount++
    } while (pageToken != null && pageCount < MAX_PAGES)

    onStep("Przetwarzanie sesji")
    val deviceNames = mutableMapOf<String, String>()
    val sessions = mutableListOf<SessionSummary>()
    for (point in dataPoints) {
        val source = describeSource(store, point.dataSource, deviceNames)
        val zone: ZoneId = point.zoneOffset ?: ZoneId.systemDefault()
        val pointType = point.getValue(DataType.ExerciseType.EXERCISE_TYPE)?.name
        val pointSessions = point.getValue(DataType.ExerciseType.SESSIONS).orEmpty()
        for (session in pointSessions) {
            sessions += summarize(session, pointType, zone, source)
        }
    }
    sessions.sortByDescending { it.startTime }

    return ReadResult(
        grantedPermissions = granted.map { it.describe() }.sorted(),
        missingPermissions = missing.map { it.describe() }.sorted(),
        dataPointCount = dataPoints.size,
        pageCount = pageCount,
        sessions = sessions,
    )
}

private fun summarize(
    session: ExerciseSession,
    fallbackType: String?,
    zone: ZoneId,
    source: String,
): SessionSummary {
    val route = session.route.orEmpty()
    val routeAltitudes = route.mapNotNull { it.altitude }
    val log = session.log.orEmpty()

    return SessionSummary(
        startTime = session.startTime,
        zone = zone,
        exerciseType = session.exerciseType?.name ?: fallbackType ?: "?",
        duration = session.duration,
        distanceMeters = session.distance,
        routePoints = route.size,
        routePointsWithAltitude = routeAltitudes.size,
        minRouteAltitude = routeAltitudes.minOrNull(),
        maxRouteAltitude = routeAltitudes.maxOrNull(),
        altitudeGain = session.altitudeGain,
        logEntries = log.size,
        logEntriesWithHeartRate = log.count { it.heartRate != null },
        logEntriesWithSpeed = log.count { it.speed != null },
        meanHeartRate = session.meanHeartRate,
        maxHeartRate = session.maxHeartRate,
        source = source,
    )
}

private suspend fun describeSource(
    store: HealthDataStore,
    dataSource: DataSource?,
    cache: MutableMap<String, String>,
): String {
    val appId = dataSource?.appId ?: "brak"
    val deviceId = dataSource?.deviceId
    if (deviceId.isNullOrEmpty()) {
        return "brak deviceId, aplikacja: $appId"
    }

    val deviceName = cache.getOrPut(deviceId) {
        try {
            val device = store.getDeviceManager().getDevice(deviceId)
            listOfNotNull(device?.manufacturer, device?.model, device?.name)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" ")
                .ifEmpty { "nieznane urządzenie" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "nazwa niedostępna (${e.javaClass.simpleName})"
        }
    }
    return "$deviceName, id: $deviceId, aplikacja: $appId"
}

private fun Permission.describe(): String = "${dataType.name}/${accessType.name}"

private fun tryResolve(error: Throwable, activity: Activity): String? {
    if (error !is ResolvablePlatformException) {
        return null
    }
    if (!error.hasResolution) {
        return "Rozwiązanie: brak (hasResolution = false)."
    }
    return try {
        error.resolve(activity)
        "Rozwiązanie: uruchomiono e.resolve(activity). Po jego zakończeniu spróbuj ponownie."
    } catch (e: Exception) {
        "Rozwiązanie: nie udało się uruchomić (${e.javaClass.name}: ${e.message})."
    }
}

private fun describeFailure(step: String, error: Throwable, resolution: String?): String =
    buildString {
        appendLine("Krok: $step")
        appendLine("Wyjątek: ${error.javaClass.name}")
        if (error is HealthDataException) {
            val code = error.errorCode
            val codeName = code?.let { errorCodeName(it) }
            appendLine("Kod błędu: ${code ?: "brak"}${codeName?.let { " ($it)" } ?: ""}")
            appendLine("Komunikat SDK: ${error.errorMessage ?: "brak"}")
        }
        appendLine("Komunikat: ${error.message ?: "brak"}")
        error.cause?.let { appendLine("Przyczyna: ${it.javaClass.name}: ${it.message}") }
        resolution?.let { appendLine(it) }
    }.trimEnd()

private fun errorCodeName(code: Int): String? =
    runCatching {
        ErrorCode::class.java.fields
            .firstOrNull { field ->
                field.type == Int::class.javaPrimitiveType && field.getInt(null) == code
            }
            ?.name
    }.getOrNull()

private fun durationText(duration: Duration?): String {
    if (duration == null) {
        return "brak"
    }
    val totalSeconds = duration.seconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%d:%02d:%02d".format(hours, minutes, seconds)
}

private fun kmText(meters: Float?): String =
    meters?.let { "%.2f km".format(polishLocale, it / 1000f) } ?: "brak"

private fun metersText(meters: Float?): String =
    meters?.let { "%.0f m".format(polishLocale, it) } ?: "brak"

private fun bpmText(bpm: Float?): String =
    bpm?.let { "%.0f bpm".format(polishLocale, it) } ?: "brak"

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
