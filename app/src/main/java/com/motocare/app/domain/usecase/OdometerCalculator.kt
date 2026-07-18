package com.motocare.app.domain.usecase

import com.motocare.app.data.local.entity.OdometerEntryEntity
import com.motocare.app.domain.model.OdometerStats
import com.motocare.app.domain.model.OdometerValidation
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.max

class OdometerCalculator @Inject constructor() {
    fun validate(newReadingKm: Long, previousReadingKm: Long?, correctionConfirmed: Boolean): OdometerValidation = when {
        newReadingKm < 0 -> OdometerValidation.NegativeReading
        previousReadingKm != null && newReadingKm < previousReadingKm && !correctionConfirmed ->
            OdometerValidation.CorrectionRequired(previousReadingKm)
        else -> OdometerValidation.Valid
    }

    fun stats(
        entries: List<OdometerEntryEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        initialReadingKm: Long? = null,
        initialDate: java.time.LocalDate? = null,
    ): OdometerStats {
        val points = entries.map {
            ReadingPoint(
                readingKm = it.readingKm,
                date = Instant.ofEpochMilli(it.recordedAtEpochMillis).atZone(zoneId).toLocalDate(),
                order = it.recordedAtEpochMillis,
            )
        }.toMutableList()
        if (initialReadingKm != null && initialDate != null &&
            points.none { it.readingKm == initialReadingKm && it.date == initialDate } &&
            (points.minOfOrNull { it.date } == null || initialDate <= points.minOf { it.date })
        ) {
            points += ReadingPoint(initialReadingKm, initialDate, Long.MIN_VALUE)
        }
        if (points.size < 2) return OdometerStats()
        val sorted = points.sortedWith(compareBy<ReadingPoint> { it.date }.thenBy { it.order })
        val travelled = max(0, sorted.last().readingKm - sorted.first().readingKm)
        val firstDate = sorted.first().date
        val lastDate = sorted.last().date
        val days = max(1, ChronoUnit.DAYS.between(firstDate, lastDate).toInt())
        val monthly = sorted.zipWithNext().groupBy { pair ->
            YearMonth.from(pair.second.date)
        }.mapValues { (_, pairs) -> pairs.sumOf { max(0, it.second.readingKm - it.first.readingKm) } }
            .mapKeys { it.key.toString() }
        return OdometerStats(
            travelledKm = travelled,
            averageKmPerDay = travelled.toDouble() / days,
            averageKmPerMonth = travelled.toDouble() / days * 30.4375,
            travelledByMonth = monthly,
        )
    }

    private data class ReadingPoint(val readingKm: Long, val date: java.time.LocalDate, val order: Long)
}
