package com.motocare.app.domain.usecase

import com.motocare.app.data.local.entity.FuelEntryEntity
import com.motocare.app.domain.model.FuelEconomySegment
import com.motocare.app.domain.model.FuelSummary
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class FuelEconomyCalculator @Inject constructor() {
    fun calculate(entries: List<FuelEntryEntity>): FuelSummary {
        val sorted = entries.sortedWith(compareBy<FuelEntryEntity> { it.dateEpochDay }.thenBy { it.id })
        val segments = mutableListOf<FuelEconomySegment>()
        var previousFullIndex: Int? = null
        sorted.forEachIndexed { index, current ->
            if (!current.fullTank) return@forEachIndexed
            previousFullIndex?.let { previousIndex ->
                val previous = sorted[previousIndex]
                val distance = current.odometerKm - previous.odometerKm
                val intervalEntries = sorted.subList(previousIndex + 1, index + 1)
                val litres = intervalEntries.sumOf { it.litres }
                val cost = intervalEntries.sumOf { it.totalCostCentavos }
                if (distance > 0 && litres > 0.0) {
                    segments += FuelEconomySegment(
                        fromOdometerKm = previous.odometerKm,
                        toOdometerKm = current.odometerKm,
                        litres = litres,
                        kilometresPerLitre = distance / litres,
                        costPerKmCentavos = cost.toDouble() / distance,
                    )
                }
            }
            previousFullIndex = index
        }
        val totalDistance = segments.sumOf { it.toOdometerKm - it.fromOdometerKm }
        val totalLitres = segments.sumOf { it.litres }
        val monthly = entries.groupBy { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)).toString() }
            .mapValues { (_, values) -> values.sumOf { it.totalCostCentavos } }
        return FuelSummary(
            segments = segments,
            averageKmPerLitre = if (totalDistance > 0 && totalLitres > 0) totalDistance / totalLitres else null,
            bestKmPerLitre = segments.maxOfOrNull { it.kilometresPerLitre },
            worstKmPerLitre = segments.minOfOrNull { it.kilometresPerLitre },
            fuelCostPerKmCentavos = if (totalDistance > 0) {
                segments.sumOf { it.costPerKmCentavos * (it.toOdometerKm - it.fromOdometerKm) } / totalDistance
            } else null,
            monthlySpendingCentavos = monthly,
        )
    }
}
