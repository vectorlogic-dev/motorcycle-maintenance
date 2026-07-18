package com.motocare.app.domain.usecase

import com.motocare.app.data.local.entity.LoanEntity
import com.motocare.app.data.local.entity.LoanPaymentEntity
import com.motocare.app.domain.model.LoanSummary
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class LoanCalculator @Inject constructor() {
    fun calculate(loan: LoanEntity, payments: List<LoanPaymentEntity>, today: LocalDate = LocalDate.now()): LoanSummary {
        val paid = payments.filter { it.status == "PAID_ON_TIME" || it.status == "PAID_LATE" }
        val onTime = payments.count { it.status == "PAID_ON_TIME" }
        val lost = payments.count { it.status == "PAID_LATE" || it.status == "MISSED" }
        val remaining = payments.count { it.status == "PENDING" || it.status == "MISSED" }
        val pending = payments.count { it.status == "PENDING" }
        val next = payments.filter { it.status == "PENDING" || it.status == "MISSED" }.minByOrNull { it.dueEpochDay }
        val earnedCentavos = onTime * loan.rebateCentavos
        val projected = loan.downPaymentCentavos + loan.termMonths * loan.monthlyPaymentCentavos -
            earnedCentavos - pending * loan.rebateCentavos
        return LoanSummary(
            paymentsMade = paid.size,
            remainingPayments = remaining,
            rebatesEarnedCentavos = earnedCentavos,
            rebatesLostCentavos = lost * loan.rebateCentavos,
            totalPaidCentavos = loan.downPaymentCentavos + paid.sumOf { it.amountCentavos },
            projectedFinancingCostCentavos = projected,
            nextPaymentDate = next?.let { LocalDate.ofEpochDay(it.dueEpochDay) },
            daysUntilDue = next?.let { ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(it.dueEpochDay)) },
            estimatedPayoffDate = payments.maxByOrNull { it.dueEpochDay }?.let { LocalDate.ofEpochDay(it.dueEpochDay) },
        )
    }
}
