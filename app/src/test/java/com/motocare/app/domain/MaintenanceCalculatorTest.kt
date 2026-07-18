package com.motocare.app.domain

import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.domain.model.MaintenanceStatus
import com.motocare.app.domain.usecase.MaintenanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MaintenanceCalculatorTest {
    private val calculator = MaintenanceCalculator()
    private val today = LocalDate.of(2026, 7, 19)

    @Test fun `whichever trigger is reached first makes schedule overdue`() {
        val schedule = schedule(nextKm = 5_000, nextDate = today.minusDays(1))
        val result = calculator.assess(schedule, currentOdometerKm = 4_000, today)
        assertEquals(MaintenanceStatus.OVERDUE, result.status)
        assertTrue(result.overdueByTime)
    }

    @Test fun `distance lead marks due soon`() {
        val result = calculator.assess(schedule(nextKm = 5_000, nextDate = today.plusDays(100)), 4_600, today)
        assertEquals(MaintenanceStatus.DUE_SOON, result.status)
        assertEquals(400L, result.remainingKm)
    }

    @Test fun `exact due date is due`() {
        val result = calculator.assess(schedule(nextKm = null, nextDate = today), 1, today)
        assertEquals(MaintenanceStatus.DUE, result.status)
    }

    private fun schedule(nextKm: Long?, nextDate: LocalDate?) = MaintenanceScheduleEntity(
        motorcycleId = 1,
        name = "Oil",
        nextDueOdometerKm = nextKm,
        nextDueEpochDay = nextDate?.toEpochDay(),
        reminderLeadKm = 500,
        reminderLeadDays = 14,
    )
}
