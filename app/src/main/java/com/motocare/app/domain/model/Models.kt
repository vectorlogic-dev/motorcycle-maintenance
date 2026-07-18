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

data class FuelEconomySegment(
    val fromOdometerKm: Long,
    val toOdometerKm: Long,
    val litres: Double,
    val kilometresPerLitre: Double,
    val costPerKmCentavos: Double,
)

data class FuelSummary(
    val segments: List<FuelEconomySegment> = emptyList(),
    val averageKmPerLitre: Double? = null,
    val bestKmPerLitre: Double? = null,
    val worstKmPerLitre: Double? = null,
    val fuelCostPerKmCentavos: Double? = null,
    val monthlySpendingCentavos: Map<String, Long> = emptyMap(),
)

data class LoanSummary(
    val paymentsMade: Int,
    val remainingPayments: Int,
    val rebatesEarnedCentavos: Long,
    val rebatesLostCentavos: Long,
    val totalPaidCentavos: Long,
    val projectedFinancingCostCentavos: Long,
    val nextPaymentDate: LocalDate?,
    val daysUntilDue: Long?,
    val estimatedPayoffDate: LocalDate?,
)

data class CostSummary(
    val todayCentavos: Long,
    val monthCentavos: Long,
    val yearCentavos: Long,
    val totalCentavos: Long,
    val costPerKmCentavos: Double?,
    val fuelCostPerKmCentavos: Double?,
    val maintenanceCostPerKmCentavos: Double?,
)
