package com.motocare.app.data.repository

import androidx.room.withTransaction
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.entity.CoveragePlanEntity
import com.motocare.app.data.local.entity.LoanEntity
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.local.entity.OdometerEntryEntity
import com.motocare.app.data.local.entity.LoanPaymentEntity
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SampleDataRepository @Inject constructor(private val database: MotoCareDatabase) {
    suspend fun createHondaClickSample(startDate: LocalDate = LocalDate.of(2026, 7, 16)): Long = database.withTransaction {
        val id = database.motorcycleDao().insert(
            MotorcycleEntity(
                name = "My Click125",
                manufacturer = "Honda",
                model = "Click125",
                variant = "Smart Edition",
                purchaseDateEpochDay = startDate.toEpochDay(),
                purchaseType = "FINANCED",
                initialOdometerKm = 1,
                currentOdometerKm = 1,
                registrationExpiryEpochDay = startDate.plusYears(3).toEpochDay(),
                isFinanced = true,
                notes = "Sample profile — review dates and identifiers before relying on reminders.",
            ),
        )
        database.odometerDao().insert(
            OdometerEntryEntity(
                motorcycleId = id,
                readingKm = 1,
                recordedAtEpochMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                note = "Starting odometer",
            ),
        )
        database.maintenanceDao().insertAll(
            listOf(
                "Engine oil", "Gear oil", "Coolant", "CVT cleaning", "Drive belt inspection",
                "Air filter", "Spark plug", "Brake pads", "Brake fluid", "Tires", "Battery",
                "Valve clearance", "General inspection",
            ).map { name ->
                MaintenanceScheduleEntity(
                    motorcycleId = id,
                    name = name,
                    description = "Editable starter template. Confirm the interval with your owner’s manual or dealer booklet.",
                    active = true,
                    source = "EDITABLE_STARTER_TEMPLATE",
                    isEditableTemplate = true,
                )
            },
        )
        val loanId = database.phaseTwoDao().insertLoan(
            LoanEntity(
                motorcycleId = id,
                downPaymentCentavos = 1_000_000,
                monthlyPaymentCentavos = 840_000,
                termMonths = 12,
                rebateCentavos = 20_000,
                rebateCondition = "PHP 200 rebate when paid on time; effective on-time payment PHP 8,200",
                startEpochDay = startDate.toEpochDay(),
            ),
        )
        database.loanDao().insertPayments(
            (1..12).map { number ->
                LoanPaymentEntity(
                    loanId = loanId,
                    installmentNumber = number,
                    dueEpochDay = startDate.plusMonths(number.toLong()).toEpochDay(),
                )
            },
        )
        database.phaseTwoDao().insertCoverage(
            CoveragePlanEntity(
                motorcycleId = id,
                startEpochDay = startDate.toEpochDay(),
                endEpochDay = startDate.plusYears(1).toEpochDay(),
                startOdometerKm = 1,
                limitOdometerKm = 12_000,
                coveredServices = "Confirm covered services with the dealer booklet.",
                notes = "Coverage ends after one year or at 12,000 km, whichever comes first.",
            ),
        )
        id
    }
}
