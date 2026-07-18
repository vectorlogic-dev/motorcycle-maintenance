package com.motocare.app.domain.usecase

import com.motocare.app.data.local.entity.CoveragePlanEntity
import com.motocare.app.domain.model.CoverageAssessment
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.max

class CoverageCalculator @Inject constructor() {
    fun assess(
        plan: CoveragePlanEntity,
        currentOdometerKm: Long,
        averageKmPerDay: Double,
        today: LocalDate = LocalDate.now(),
    ): CoverageAssessment {
        val end = LocalDate.ofEpochDay(plan.endEpochDay)
        val remainingDays = ChronoUnit.DAYS.between(today, end)
        val remainingKm = plan.limitOdometerKm - currentOdometerKm
        val distanceEnd = if (averageKmPerDay > 0 && remainingKm > 0) {
            today.plusDays(max(1, (remainingKm / averageKmPerDay).toLong()))
        } else null
        return CoverageAssessment(
            remainingDays = remainingDays,
            remainingKm = remainingKm,
            expiredByDate = remainingDays < 0,
            expiredByDistance = remainingKm < 0,
            estimatedEndDate = listOfNotNull(end, distanceEnd).minOrNull(),
        )
    }
}
