package com.motocare.app.domain

import com.motocare.app.data.local.entity.FuelEntryEntity
import com.motocare.app.domain.usecase.FuelEconomyCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FuelEconomyCalculatorTest {
    private val calculator = FuelEconomyCalculator()

    @Test fun `economy uses fuel accumulated between full tanks`() {
        val entries = listOf(
            fuel(1, 1_000, 2.0, true),
            fuel(2, 1_100, 1.0, false),
            fuel(3, 1_300, 5.0, true),
        )
        val result = calculator.calculate(entries)
        assertEquals(1, result.segments.size)
        assertEquals(50.0, result.averageKmPerLitre!!, 0.001)
        assertEquals(6.0, result.segments.single().litres, 0.001)
    }

    @Test fun `single full tank does not produce economy`() {
        assertNull(calculator.calculate(listOf(fuel(1, 100, 2.0, true))).averageKmPerLitre)
    }

    private fun fuel(day: Int, km: Long, litres: Double, full: Boolean) = FuelEntryEntity(
        id = day.toLong(), motorcycleId = 1, dateEpochDay = LocalDate.of(2026, 7, day).toEpochDay(),
        odometerKm = km, litres = litres, pricePerLitreCentavos = 7_000,
        totalCostCentavos = (litres * 7_000).toLong(), fullTank = full,
    )
}
