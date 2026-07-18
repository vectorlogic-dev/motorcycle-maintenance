package com.motocare.app.domain

import com.motocare.app.data.local.entity.ExpenseEntity
import com.motocare.app.data.local.entity.FuelEntryEntity
import com.motocare.app.data.local.entity.ServiceRecordEntity
import com.motocare.app.domain.usecase.CostSummaryCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CostSummaryCalculatorTest {
    @Test fun `cost per kilometre includes expenses fuel services and ownership costs`() {
        val today = LocalDate.of(2026, 7, 19)
        val expense = ExpenseEntity(motorcycleId = 1, dateEpochDay = today.toEpochDay(), category = "PARKING", amountCentavos = 3_500)
        val fuel = FuelEntryEntity(motorcycleId = 1, dateEpochDay = today.toEpochDay(), odometerKm = 100, litres = 2.0, pricePerLitreCentavos = 7_000, totalCostCentavos = 14_000, fullTank = true)
        val service = ServiceRecordEntity(motorcycleId = 1, serviceEpochDay = today.toEpochDay(), odometerKm = 100, labourCostCentavos = 10_000, partsCostCentavos = 20_000)
        val result = CostSummaryCalculator().calculate(listOf(expense), listOf(fuel), listOf(service), 100, 100_000, today)
        assertEquals(147_500L, result.totalCentavos)
        assertEquals(1_475.0, result.costPerKmCentavos!!, 0.001)
    }
}
