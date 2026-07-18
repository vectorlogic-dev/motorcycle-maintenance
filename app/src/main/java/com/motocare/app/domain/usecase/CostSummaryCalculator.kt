package com.motocare.app.domain.usecase

import com.motocare.app.data.local.entity.ExpenseEntity
import com.motocare.app.data.local.entity.FuelEntryEntity
import com.motocare.app.data.local.entity.ServiceRecordEntity
import com.motocare.app.domain.model.CostSummary
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class CostSummaryCalculator @Inject constructor() {
    fun calculate(
        expenses: List<ExpenseEntity>,
        fuel: List<FuelEntryEntity>,
        services: List<ServiceRecordEntity>,
        travelledKm: Long,
        additionalOwnershipCostCentavos: Long = 0,
        today: LocalDate = LocalDate.now(),
    ): CostSummary {
        fun ExpenseEntity.amount() = amountCentavos
        fun ServiceRecordEntity.amount() = labourCostCentavos + partsCostCentavos
        val todayCost = expenses.filter { it.dateEpochDay == today.toEpochDay() }.sumOf { it.amount() } +
            fuel.filter { it.dateEpochDay == today.toEpochDay() }.sumOf { it.totalCostCentavos } +
            services.filter { it.serviceEpochDay == today.toEpochDay() }.sumOf { it.amount() }
        val month = YearMonth.from(today)
        val monthCost = expenses.filter { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == month }.sumOf { it.amount() } +
            fuel.filter { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == month }.sumOf { it.totalCostCentavos } +
            services.filter { YearMonth.from(LocalDate.ofEpochDay(it.serviceEpochDay)) == month }.sumOf { it.amount() }
        val yearCost = expenses.filter { LocalDate.ofEpochDay(it.dateEpochDay).year == today.year }.sumOf { it.amount() } +
            fuel.filter { LocalDate.ofEpochDay(it.dateEpochDay).year == today.year }.sumOf { it.totalCostCentavos } +
            services.filter { LocalDate.ofEpochDay(it.serviceEpochDay).year == today.year }.sumOf { it.amount() }
        val fuelCost = fuel.sumOf { it.totalCostCentavos }
        val maintenanceCost = services.sumOf { it.amount() } + expenses
            .filter { it.category == "MAINTENANCE" || it.category == "REPAIRS" }
            .sumOf { it.amountCentavos }
        val total = expenses.sumOf { it.amount() } + fuelCost + services.sumOf { it.amount() } + additionalOwnershipCostCentavos
        return CostSummary(
            todayCentavos = todayCost,
            monthCentavos = monthCost,
            yearCentavos = yearCost,
            totalCentavos = total,
            costPerKmCentavos = if (travelledKm > 0) total.toDouble() / travelledKm else null,
            fuelCostPerKmCentavos = if (travelledKm > 0) fuelCost.toDouble() / travelledKm else null,
            maintenanceCostPerKmCentavos = if (travelledKm > 0) maintenanceCost.toDouble() / travelledKm else null,
        )
    }
}
