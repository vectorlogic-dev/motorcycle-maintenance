package com.motocare.app.domain.model

import java.time.LocalDate

enum class MaintenanceStatus { GOOD, DUE_SOON, DUE, OVERDUE }

data class MaintenanceAssessment(
    val status: MaintenanceStatus,
    val remainingKm: Long?,
    val remainingDays: Long?,
    val overdueByDistance: Boolean,
    val overdueByTime: Boolean,
)

data class OdometerStats(
    val travelledKm: Long = 0,
    val averageKmPerDay: Double = 0.0,
    val averageKmPerMonth: Double = 0.0,
    val travelledByMonth: Map<String, Long> = emptyMap(),
)

sealed interface OdometerValidation {
    data object Valid : OdometerValidation
    data class CorrectionRequired(val previousKm: Long) : OdometerValidation
    data object NegativeReading : OdometerValidation
}

data class CoverageAssessment(
    val remainingDays: Long,
    val remainingKm: Long,
    val expiredByDate: Boolean,
    val expiredByDistance: Boolean,
    val estimatedEndDate: LocalDate?,
)
