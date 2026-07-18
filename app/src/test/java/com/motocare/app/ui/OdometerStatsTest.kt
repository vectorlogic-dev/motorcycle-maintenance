package com.motocare.app.ui

import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.local.entity.OdometerEntryEntity
import com.motocare.app.domain.usecase.OdometerCalculator
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class OdometerStatsTest {
    @Test
    fun `all UI consumers include the purchase baseline`() {
        val purchaseDate = LocalDate.of(2026, 7, 16)
        val currentDate = purchaseDate.plusDays(3)
        val motorcycle = MotorcycleEntity(
            name = "Daily bike",
            manufacturer = "Honda",
            model = "Click",
            purchaseDateEpochDay = purchaseDate.toEpochDay(),
            initialOdometerKm = 1,
            currentOdometerKm = 31,
        )
        val latest = OdometerEntryEntity(
            motorcycleId = 1,
            readingKm = 31,
            recordedAtEpochMillis = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )

        val stats = OdometerCalculator().statsFor(motorcycle, listOf(latest))

        assertEquals(30L, stats.travelledKm)
        assertEquals(10.0, stats.averageKmPerDay, 0.001)
    }
}
