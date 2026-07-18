package com.motocare.app.domain

import com.motocare.app.data.local.entity.CoveragePlanEntity
import com.motocare.app.domain.usecase.CoverageCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class CoverageCalculatorTest {
    @Test fun `coverage tracks time and distance independently`() {
        val today = LocalDate.of(2026, 1, 1)
        val plan = CoveragePlanEntity(
            motorcycleId = 1,
            startEpochDay = today.minusDays(10).toEpochDay(),
            endEpochDay = today.plusDays(20).toEpochDay(),
            startOdometerKm = 1,
            limitOdometerKm = 12_000,
        )
        val result = CoverageCalculator().assess(plan, currentOdometerKm = 2_000, averageKmPerDay = 100.0, today)
        assertEquals(20, result.remainingDays)
        assertEquals(10_000, result.remainingKm)
        assertEquals(today.plusDays(20), result.estimatedEndDate)
        assertFalse(result.expiredByDate)
    }
}
