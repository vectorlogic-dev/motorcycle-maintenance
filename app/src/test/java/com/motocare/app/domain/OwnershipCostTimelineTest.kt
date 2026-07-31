package com.motocare.app.domain

import com.motocare.app.data.local.entity.LoanEntity
import com.motocare.app.data.local.entity.LoanPaymentEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.domain.usecase.OwnershipCostTimeline
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class OwnershipCostTimelineTest {
    private val timeline = OwnershipCostTimeline()

    @Test
    fun `cash purchase is included once when no financing plan exists`() {
        val purchaseDate = LocalDate.of(2026, 7, 16)
        val motorcycle = MotorcycleEntity(
            name = "Daily bike",
            manufacturer = "Honda",
            model = "Click",
            purchaseDateEpochDay = purchaseDate.toEpochDay(),
            purchaseType = "CASH",
            purchasePriceCentavos = 8_000_000,
            initialOdometerKm = 1,
            currentOdometerKm = 1,
        )

        val result = timeline.build(motorcycle, null, emptyList())

        assertEquals(1, result.size)
        assertEquals(8_000_000L, result.single().amountCentavos)
        assertEquals(purchaseDate.toEpochDay(), result.single().epochDay)
    }

    @Test
    fun `financing uses down payment and paid installments instead of purchase price`() {
        val start = LocalDate.of(2026, 7, 16)
        val motorcycle = MotorcycleEntity(
            name = "Daily bike",
            manufacturer = "Yamaha",
            model = "NMAX",
            purchaseDateEpochDay = start.toEpochDay(),
            purchaseType = "FINANCED",
            purchasePriceCentavos = 15_000_000,
            initialOdometerKm = 1,
            currentOdometerKm = 1,
            isFinanced = true,
        )
        val loan = LoanEntity(
            id = 2,
            motorcycleId = 1,
            downPaymentCentavos = 2_000_000,
            monthlyPaymentCentavos = 800_000,
            termMonths = 12,
            startEpochDay = start.toEpochDay(),
        )
        val payments = listOf(
            LoanPaymentEntity(
                loanId = 2,
                installmentNumber = 1,
                dueEpochDay = start.plusMonths(1).toEpochDay(),
                paidEpochDay = start.plusMonths(1).toEpochDay(),
                amountCentavos = 800_000,
                status = "PAID_LATE",
            ),
        )

        val result = timeline.build(motorcycle, loan, payments)

        assertEquals(2, result.size)
        assertEquals(2_800_000L, result.sumOf { it.amountCentavos })
    }
}
