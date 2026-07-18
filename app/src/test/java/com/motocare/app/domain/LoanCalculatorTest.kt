package com.motocare.app.domain

import com.motocare.app.data.local.entity.LoanEntity
import com.motocare.app.data.local.entity.LoanPaymentEntity
import com.motocare.app.domain.usecase.LoanCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LoanCalculatorTest {
    @Test fun `on-time and late payments apply rebate correctly`() {
        val today = LocalDate.of(2026, 7, 1)
        val loan = LoanEntity(
            id = 1, motorcycleId = 1, downPaymentCentavos = 1_000_000,
            monthlyPaymentCentavos = 840_000, termMonths = 3, rebateCentavos = 20_000,
        )
        val payments = listOf(
            payment(1, "PAID_ON_TIME", 820_000, today.plusDays(1)),
            payment(2, "PAID_LATE", 840_000, today.plusDays(31)),
            payment(3, "PENDING", 0, today.plusDays(61)),
        )
        val result = LoanCalculator().calculate(loan, payments, today)
        assertEquals(2, result.paymentsMade)
        assertEquals(1, result.remainingPayments)
        assertEquals(20_000L, result.rebatesEarnedCentavos)
        assertEquals(20_000L, result.rebatesLostCentavos)
        assertEquals(2_660_000L, result.totalPaidCentavos)
        assertEquals(3_480_000L, result.projectedFinancingCostCentavos)
    }

    private fun payment(number: Int, status: String, amount: Long, due: LocalDate) = LoanPaymentEntity(
        id = number.toLong(), loanId = 1, installmentNumber = number, dueEpochDay = due.toEpochDay(),
        status = status, amountCentavos = amount,
    )
}
