package com.motocare.app.data.repository

import androidx.room.withTransaction
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.entity.LoanEntity
import com.motocare.app.data.local.entity.LoanPaymentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository @Inject constructor(private val database: MotoCareDatabase) {
    fun observeLoan(motorcycleId: Long): Flow<LoanEntity?> = database.loanDao().observeForMotorcycle(motorcycleId)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observePayments(motorcycleId: Long): Flow<List<LoanPaymentEntity>> = observeLoan(motorcycleId).flatMapLatest { loan ->
        loan?.let { database.loanDao().observePayments(it.id) } ?: flowOf(emptyList())
    }

    suspend fun configure(loan: LoanEntity): Long = database.withTransaction {
        val id = database.loanDao().insert(loan)
        createPaymentsIfMissing(id, loan)
        id
    }

    suspend fun ensurePayments(motorcycleId: Long) = database.withTransaction {
        val loan = database.loanDao().getForMotorcycle(motorcycleId) ?: return@withTransaction
        createPaymentsIfMissing(loan.id, loan)
    }

    suspend fun markPayment(payment: LoanPaymentEntity, status: String, loan: LoanEntity) {
        val amount = when (status) {
            "PAID_ON_TIME" -> (loan.monthlyPaymentCentavos - loan.rebateCentavos).coerceAtLeast(0)
            "PAID_LATE" -> loan.monthlyPaymentCentavos
            else -> 0
        }
        database.loanDao().updatePayment(
            payment.copy(
                status = status,
                amountCentavos = amount,
                paidEpochDay = if (status.startsWith("PAID")) LocalDate.now().toEpochDay() else null,
            ),
        )
    }

    private suspend fun createPaymentsIfMissing(loanId: Long, loan: LoanEntity) {
        if (database.loanDao().getPayments(loanId).isNotEmpty()) return
        val start = loan.startEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
        val payments = (1..loan.termMonths).map { number ->
            val month = start.plusMonths(number.toLong())
            val due = loan.paymentDueDay?.let { day -> month.withDayOfMonth(day.coerceAtMost(month.lengthOfMonth())) } ?: month
            LoanPaymentEntity(loanId = loanId, installmentNumber = number, dueEpochDay = due.toEpochDay())
        }
        database.loanDao().insertPayments(payments)
    }
}
