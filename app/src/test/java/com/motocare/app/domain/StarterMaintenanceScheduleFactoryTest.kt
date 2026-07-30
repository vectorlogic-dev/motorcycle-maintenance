package com.motocare.app.domain

import com.motocare.app.domain.usecase.StarterMaintenanceScheduleFactory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterMaintenanceScheduleFactoryTest {
    private val factory = StarterMaintenanceScheduleFactory()
    private val startDate = LocalDate.of(2026, 7, 31)

    @Test
    fun `creates researched editable starter schedules from current motorcycle state`() {
        val schedules = factory.create(motorcycleId = 42, currentOdometerKm = 8_500, startDate)

        assertEquals(6, schedules.size)
        assertTrue(schedules.all { it.motorcycleId == 42L })
        assertTrue(schedules.all { it.isEditableTemplate })
        assertTrue(schedules.all { it.source == "RESEARCH_STARTER_V1" })

        val oil = schedules.single { it.name == "Engine oil change" }
        assertEquals(3_000L, oil.intervalKm)
        assertEquals(365, oil.intervalDays)
        assertEquals(11_500L, oil.nextDueOdometerKm)
        assertEquals(startDate.plusDays(365).toEpochDay(), oil.nextDueEpochDay)
    }

    @Test
    fun `keeps drivetrain and fluid templates limited to applicable trigger types`() {
        val schedules = factory.create(motorcycleId = 1, currentOdometerKm = 0, startDate)

        val chain = schedules.single { it.name == "Drive chain service (if fitted)" }
        assertEquals(1_000L, chain.intervalKm)
        assertNull(chain.intervalDays)
        assertEquals(1_000L, chain.nextDueOdometerKm)
        assertNull(chain.nextDueEpochDay)

        val brakeFluid = schedules.single { it.name == "Brake fluid replacement (if fitted)" }
        assertNull(brakeFluid.intervalKm)
        assertEquals(730, brakeFluid.intervalDays)
        assertNull(brakeFluid.nextDueOdometerKm)
        assertEquals(startDate.plusDays(730).toEpochDay(), brakeFluid.nextDueEpochDay)
    }
}
