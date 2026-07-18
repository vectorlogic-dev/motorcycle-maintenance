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

    fun stats(entries: List<OdometerEntryEntity>, zoneId: ZoneId = ZoneId.systemDefault()): OdometerStats {
        if (entries.size < 2) return OdometerStats()
        val sorted = entries.sortedBy { it.recordedAtEpochMillis }
        val travelled = max(0, sorted.last().readingKm - sorted.first().readingKm)
        val firstDate = Instant.ofEpochMilli(sorted.first().recordedAtEpochMillis).atZone(zoneId).toLocalDate()
        val lastDate = Instant.ofEpochMilli(sorted.last().recordedAtEpochMillis).atZone(zoneId).toLocalDate()
        val days = max(1, ChronoUnit.DAYS.between(firstDate, lastDate).toInt())
        val monthly = sorted.zipWithNext().groupBy { pair ->
            YearMonth.from(Instant.ofEpochMilli(pair.second.recordedAtEpochMillis).atZone(zoneId))
        }.mapValues { (_, pairs) -> pairs.sumOf { max(0, it.second.readingKm - it.first.readingKm) } }
            .mapKeys { it.key.toString() }
        return OdometerStats(
            travelledKm = travelled,
            averageKmPerDay = travelled.toDouble() / days,
            averageKmPerMonth = travelled.toDouble() / days * 30.4375,
            travelledByMonth = monthly,
        )
    }
}
