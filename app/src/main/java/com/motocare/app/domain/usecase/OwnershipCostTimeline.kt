package com.motocare.app.domain.usecase

import com.motocare.app.data.local.entity.LoanEntity
import com.motocare.app.data.local.entity.LoanPaymentEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.domain.model.DatedCost
import javax.inject.Inject

class OwnershipCostTimeline @Inject constructor() {
    fun build(
        motorcycle: MotorcycleEntity?,
        loan: LoanEntity?,
        payments: List<LoanPaymentEntity>,
    ): List<DatedCost> {
        if (motorcycle == null) return emptyList()
        if (loan != null) {
            return buildList {
                loan.startEpochDay?.let { start ->
                    if (loan.downPaymentCentavos > 0) add(DatedCost(start, loan.downPaymentCentavos))
                }
                payments.filter { it.status == "PAID_ON_TIME" || it.status == "PAID_LATE" }
                    .forEach { payment ->
                        payment.paidEpochDay?.let { paidDate ->
                            if (payment.amountCentavos > 0) add(DatedCost(paidDate, payment.amountCentavos))
                        }
                    }
            }
        }
        val isCashPurchase = motorcycle.purchaseType == "CASH" || !motorcycle.isFinanced
        return if (isCashPurchase && motorcycle.purchasePriceCentavos != null && motorcycle.purchaseDateEpochDay != null) {
            listOf(DatedCost(motorcycle.purchaseDateEpochDay, motorcycle.purchasePriceCentavos))
        } else {
            emptyList()
        }
    }
}
