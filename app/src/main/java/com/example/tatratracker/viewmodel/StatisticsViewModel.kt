package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.TrackPointEntity
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class StatisticsPeriod(
    val label: String,
    val helper: String
) {
    LAST_7_DAYS(
        label = "7 dni",
        helper = "Ostatnie 7 dni"
    ),
    LAST_MONTH(
        label = "Miesiąc",
        helper = "Ostatni miesiąc"
    ),
    LAST_YEAR(
        label = "Rok",
        helper = "Ostatnie 12 miesięcy"
    ),
    ALL(
        label = "Wszystko",
        helper = "Cała historia"
    )
}

data class StatisticsChartPoint(
    val label: String,
    val distanceKm: Double,
    val activityCount: Int
)

data class StatisticsUiState(
    val period: StatisticsPeriod = StatisticsPeriod.ALL,
    val activityCount: Int = 0,
    val distanceMeters: Double = 0.0,
    val durationMillis: Long = 0L,
    val calories: Double = 0.0,
    val totalAscentMeters: Double = 0.0,
    val totalDescentMeters: Double = 0.0,
    val activeDays: Int = 0,
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val highestElevationMeters: Double? = null,
    val averageActivityDistanceMeters: Double = 0.0,
    val longestActivityName: String? = null,
    val longestActivityDistanceMeters: Double = 0.0,
    val longestActivityDurationMillis: Long = 0L,
    val highestActivityName: String? = null,
    val chartPoints: List<StatisticsChartPoint> = emptyList()
) {
    val hasActivities: Boolean
        get() = activityCount > 0
}

class StatisticsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val activityDao =
        AppDatabase
            .getDatabase(application)
            .activityDao()

    private val selectedPeriod =
        MutableStateFlow(StatisticsPeriod.ALL)

    private val _uiState =
        MutableStateFlow(StatisticsUiState())

    val uiState: StateFlow<StatisticsUiState> =
        _uiState.asStateFlow()

    init {
        observeStatistics()
    }

    fun setPeriod(
        period: StatisticsPeriod
    ) {
        selectedPeriod.value = period
    }

    private fun observeStatistics() {
        viewModelScope.launch {
            combine(
                activityDao.getAllActivities(),
                activityDao.getAllTrackPoints(),
                selectedPeriod
            ) { activities, trackPoints, period ->
                buildStatistics(
                    allActivities = activities,
                    allTrackPoints = trackPoints,
                    period = period
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun buildStatistics(
        allActivities: List<ActivityEntity>,
        allTrackPoints: List<TrackPointEntity>,
        period: StatisticsPeriod
    ): StatisticsUiState {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val startDate = periodStartDate(period, today)

        val activities =
            allActivities.filter { activity ->
                val activityDate =
                    Instant
                        .ofEpochMilli(activity.timestamp)
                        .atZone(zoneId)
                        .toLocalDate()

                startDate == null || !activityDate.isBefore(startDate)
            }

        if (activities.isEmpty()) {
            return StatisticsUiState(
                period = period,
                chartPoints =
                    buildChartPoints(
                        activities = emptyList(),
                        period = period,
                        today = today,
                        zoneId = zoneId
                    )
            )
        }

        val activityIds =
            activities
                .asSequence()
                .map { it.id }
                .toHashSet()

        val trackPoints =
            allTrackPoints.filter { point ->
                point.activityId in activityIds
            }

        val elevation =
            calculateElevationTotals(trackPoints)

        val heartRateActivities =
            activities.filter { it.avgHeartRate > 0 }

        val averageHeartRate =
            if (heartRateActivities.isNotEmpty()) {
                heartRateActivities
                    .map { it.avgHeartRate }
                    .average()
                    .toInt()
            } else {
                null
            }

        val maxHeartRate =
            activities
                .mapNotNull { activity ->
                    activity.maxHeartRate
                        .takeIf { it > 0 }
                }
                .maxOrNull()

        val highestActivity =
            activities
                .filter { it.maxAltitudeMeters > 0.0 }
                .maxByOrNull { it.maxAltitudeMeters }

        val longestDistanceActivity =
            activities.maxByOrNull {
                it.distanceMeters
            }

        val longestDurationActivity =
            activities.maxByOrNull {
                it.durationMillis
            }

        val activeDays =
            activities
                .map { activity ->
                    Instant
                        .ofEpochMilli(activity.timestamp)
                        .atZone(zoneId)
                        .toLocalDate()
                }
                .distinct()
                .size

        val totalDistance =
            activities.sumOf {
                it.distanceMeters
            }

        return StatisticsUiState(
            period = period,
            activityCount = activities.size,
            distanceMeters = totalDistance,
            durationMillis = activities.sumOf { it.durationMillis },
            calories = activities.sumOf { it.calories.coerceAtLeast(0.0) },
            totalAscentMeters = elevation.ascentMeters,
            totalDescentMeters = elevation.descentMeters,
            activeDays = activeDays,
            averageHeartRate = averageHeartRate,
            maxHeartRate = maxHeartRate,
            highestElevationMeters = highestActivity?.maxAltitudeMeters,
            averageActivityDistanceMeters = totalDistance / activities.size,
            longestActivityName = longestDistanceActivity?.name,
            longestActivityDistanceMeters =
                longestDistanceActivity?.distanceMeters ?: 0.0,
            longestActivityDurationMillis =
                longestDurationActivity?.durationMillis ?: 0L,
            highestActivityName = highestActivity?.name,
            chartPoints =
                buildChartPoints(
                    activities = activities,
                    period = period,
                    today = today,
                    zoneId = zoneId
                )
        )
    }

    private fun periodStartDate(
        period: StatisticsPeriod,
        today: LocalDate
    ): LocalDate? =
        when (period) {
            StatisticsPeriod.LAST_7_DAYS ->
                today.minusDays(6)

            StatisticsPeriod.LAST_MONTH ->
                today.minusMonths(1)

            StatisticsPeriod.LAST_YEAR ->
                today.minusYears(1)

            StatisticsPeriod.ALL ->
                null
        }

    private fun calculateElevationTotals(
        points: List<TrackPointEntity>
    ): ElevationTotals {
        var ascent = 0.0
        var descent = 0.0

        points
            .groupBy { it.activityId }
            .values
            .forEach { activityPoints ->
                val elevations =
                    activityPoints
                        .mapNotNull { it.altitudeMeters }

                if (elevations.size < 2) {
                    return@forEach
                }

                for (index in 1 until elevations.size) {
                    val difference =
                        elevations[index] - elevations[index - 1]

                    // Pomijamy drobny szum wysokości GPS.
                    if (abs(difference) < 2.0) {
                        continue
                    }

                    if (difference > 0.0) {
                        ascent += difference
                    } else {
                        descent += -difference
                    }
                }
            }

        return ElevationTotals(
            ascentMeters = ascent,
            descentMeters = descent
        )
    }

    private fun buildChartPoints(
        activities: List<ActivityEntity>,
        period: StatisticsPeriod,
        today: LocalDate,
        zoneId: ZoneId
    ): List<StatisticsChartPoint> {
        return when (period) {
            StatisticsPeriod.LAST_7_DAYS ->
                buildSevenDayChart(
                    activities = activities,
                    today = today,
                    zoneId = zoneId
                )

            StatisticsPeriod.LAST_MONTH ->
                buildDailyChart(
                    activities = activities,
                    zoneId = zoneId
                )

            StatisticsPeriod.LAST_YEAR ->
                buildMonthlyChart(
                    activities = activities,
                    zoneId = zoneId
                )

            StatisticsPeriod.ALL ->
                buildAllHistoryChart(
                    activities = activities,
                    zoneId = zoneId
                )
        }
    }

    private fun buildSevenDayChart(
        activities: List<ActivityEntity>,
        today: LocalDate,
        zoneId: ZoneId
    ): List<StatisticsChartPoint> {
        val grouped =
            activities.groupBy { activity ->
                Instant
                    .ofEpochMilli(activity.timestamp)
                    .atZone(zoneId)
                    .toLocalDate()
            }

        val formatter =
            DateTimeFormatter.ofPattern(
                "EEE",
                Locale("pl", "PL")
            )

        return (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dayActivities = grouped[date].orEmpty()

            StatisticsChartPoint(
                label =
                    date
                        .format(formatter)
                        .replaceFirstChar { it.uppercase() },
                distanceKm =
                    dayActivities.sumOf { it.distanceMeters } / 1000.0,
                activityCount = dayActivities.size
            )
        }
    }

    private fun buildDailyChart(
        activities: List<ActivityEntity>,
        zoneId: ZoneId
    ): List<StatisticsChartPoint> {
        val formatter =
            DateTimeFormatter.ofPattern("d.MM")

        return activities
            .groupBy { activity ->
                Instant
                    .ofEpochMilli(activity.timestamp)
                    .atZone(zoneId)
                    .toLocalDate()
            }
            .toSortedMap()
            .map { (date, dayActivities) ->
                StatisticsChartPoint(
                    label = date.format(formatter),
                    distanceKm =
                        dayActivities.sumOf { it.distanceMeters } / 1000.0,
                    activityCount = dayActivities.size
                )
            }
    }

    private fun buildMonthlyChart(
        activities: List<ActivityEntity>,
        zoneId: ZoneId
    ): List<StatisticsChartPoint> {
        val formatter =
            DateTimeFormatter.ofPattern(
                "MMM",
                Locale("pl", "PL")
            )

        return activities
            .groupBy { activity ->
                YearMonth.from(
                    Instant
                        .ofEpochMilli(activity.timestamp)
                        .atZone(zoneId)
                        .toLocalDate()
                )
            }
            .toSortedMap()
            .map { (month, monthActivities) ->
                StatisticsChartPoint(
                    label =
                        month
                            .format(formatter)
                            .replaceFirstChar { it.uppercase() },
                    distanceKm =
                        monthActivities.sumOf { it.distanceMeters } / 1000.0,
                    activityCount = monthActivities.size
                )
            }
    }

    private fun buildAllHistoryChart(
        activities: List<ActivityEntity>,
        zoneId: ZoneId
    ): List<StatisticsChartPoint> {
        if (activities.isEmpty()) {
            return emptyList()
        }

        val dates =
            activities.map { activity ->
                Instant
                    .ofEpochMilli(activity.timestamp)
                    .atZone(zoneId)
                    .toLocalDate()
            }

        val firstDate = dates.minOrNull() ?: return emptyList()
        val lastDate = dates.maxOrNull() ?: return emptyList()

        return if (YearMonth.from(firstDate) == YearMonth.from(lastDate)) {
            buildDailyChart(
                activities = activities,
                zoneId = zoneId
            )
        } else if (Year.from(firstDate) == Year.from(lastDate)) {
            buildMonthlyChart(
                activities = activities,
                zoneId = zoneId
            )
        } else {
            activities
                .groupBy { activity ->
                    Year.from(
                        Instant
                            .ofEpochMilli(activity.timestamp)
                            .atZone(zoneId)
                            .toLocalDate()
                    )
                }
                .toSortedMap()
                .map { (year, yearActivities) ->
                    StatisticsChartPoint(
                        label = year.value.toString(),
                        distanceKm =
                            yearActivities.sumOf { it.distanceMeters } / 1000.0,
                        activityCount = yearActivities.size
                    )
                }
        }
    }

    private data class ElevationTotals(
        val ascentMeters: Double,
        val descentMeters: Double
    )
}