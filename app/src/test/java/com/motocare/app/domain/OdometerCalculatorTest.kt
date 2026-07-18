package com.motocare.app.domain

import com.motocare.app.data.local.entity.OdometerEntryEntity
import com.motocare.app.domain.model.OdometerValidation
import com.motocare.app.domain.usecase.OdometerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class OdometerCalculatorTest {
    private val calculator = OdometerCalculator()

    @Test fun `lower reading requires explicit correction`() {
        assertEquals(OdometerValidation.CorrectionRequired(100), calculator.validate(90, 100, false))
        assertEquals(OdometerValidation.Valid, calculator.validate(90, 100, true))
    }

    @Test fun `stats calculate daily and monthly travel`() {
        val zone = ZoneId.of("UTC")
        fun entry(km: Long, day: Int) = OdometerEntryEntity(
            motorcycleId = 1,
            readingKm = km,
            recordedAtEpochMillis = LocalDate.of(2026, 7, day).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
        val stats = calculator.stats(listOf(entry(100, 1), entry(280, 10)), zone)
        assertEquals(180, stats.travelledKm)
        assertEquals(20.0, stats.averageKmPerDay, 0.001)
        assertTrue(stats.travelledByMonth["2026-07"] == 180L)
    }
}
