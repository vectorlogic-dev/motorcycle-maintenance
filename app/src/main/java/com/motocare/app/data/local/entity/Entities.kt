package com.motocare.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "motorcycles")
data class MotorcycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val manufacturer: String,
    val model: String,
    val variant: String = "",
    val year: Int? = null,
    val purchaseDateEpochDay: Long? = null,
    val purchaseType: String = "UNKNOWN",
    val purchasePriceCentavos: Long? = null,
    val seller: String = "",
    val secondHand: Boolean = false,
    val initialOdometerKm: Long,
    val currentOdometerKm: Long,
    val plateNumber: String = "",
    val engineNumber: String = "",
    val chassisNumber: String = "",
    val registrationExpiryEpochDay: Long? = null,
    val insuranceExpiryEpochDay: Long? = null,
    val isFinanced: Boolean = false,
    val notes: String = "",
    val photoUri: String? = null,
    val archived: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "odometer_entries",
    foreignKeys = [ForeignKey(
        entity = MotorcycleEntity::class,
        parentColumns = ["id"],
        childColumns = ["motorcycleId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("motorcycleId"), Index(value = ["motorcycleId", "recordedAtEpochMillis"])],
)
data class OdometerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val readingKm: Long,
    val recordedAtEpochMillis: Long,
    val note: String = "",
    val isCorrection: Boolean = false,
)

@Entity(
    tableName = "maintenance_schedules",
    foreignKeys = [ForeignKey(
        entity = MotorcycleEntity::class,
        parentColumns = ["id"], childColumns = ["motorcycleId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("motorcycleId")],
)
data class MaintenanceScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val name: String,
    val description: String = "",
    val intervalKm: Long? = null,
    val intervalDays: Int? = null,
    val lastServiceEpochDay: Long? = null,
    val lastServiceOdometerKm: Long? = null,
    val nextDueEpochDay: Long? = null,
    val nextDueOdometerKm: Long? = null,
    val reminderLeadDays: Int = 14,
    val reminderLeadKm: Long = 500,
    val active: Boolean = true,
    val source: String = "OWNER_TEMPLATE",
    val isEditableTemplate: Boolean = false,
)

@Entity(
    tableName = "service_records",
    foreignKeys = [ForeignKey(entity = MotorcycleEntity::class, parentColumns = ["id"], childColumns = ["motorcycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("motorcycleId")],
)
data class ServiceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val serviceEpochDay: Long,
    val odometerKm: Long,
    val dealerOrMechanic: String = "",
    val labourCostCentavos: Long = 0,
    val partsCostCentavos: Long = 0,
    val partsReplaced: String = "",
    val notes: String = "",
    val nextRecommendedEpochDay: Long? = null,
    val nextRecommendedOdometerKm: Long? = null,
)

@Entity(
    tableName = "service_record_items",
    primaryKeys = ["serviceRecordId", "maintenanceScheduleId"],
    foreignKeys = [
        ForeignKey(entity = ServiceRecordEntity::class, parentColumns = ["id"], childColumns = ["serviceRecordId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MaintenanceScheduleEntity::class, parentColumns = ["id"], childColumns = ["maintenanceScheduleId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("serviceRecordId"), Index("maintenanceScheduleId")],
)
data class ServiceRecordItemEntity(val serviceRecordId: Long, val maintenanceScheduleId: Long)

@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(entity = MotorcycleEntity::class, parentColumns = ["id"], childColumns = ["motorcycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("motorcycleId")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val dateEpochDay: Long,
    val category: String,
    val amountCentavos: Long,
    val odometerKm: Long? = null,
    val description: String = "",
    val receiptUri: String? = null,
    val paymentMethod: String = "",
    val vendor: String = "",
)

@Entity(
    tableName = "fuel_entries",
    foreignKeys = [ForeignKey(entity = MotorcycleEntity::class, parentColumns = ["id"], childColumns = ["motorcycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("motorcycleId")],
)
data class FuelEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val dateEpochDay: Long,
    val odometerKm: Long,
    val litres: Double,
    val pricePerLitreCentavos: Long,
    val totalCostCentavos: Long,
    val fullTank: Boolean,
    val station: String = "",
    val notes: String = "",
)

@Entity(
    tableName = "loans",
    foreignKeys = [ForeignKey(entity = MotorcycleEntity::class, parentColumns = ["id"], childColumns = ["motorcycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["motorcycleId"], unique = true)],
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val cashPriceCentavos: Long? = null,
    val downPaymentCentavos: Long,
    val monthlyPaymentCentavos: Long,
    val termMonths: Int,
    val paymentDueDay: Int? = null,
    val rebateCentavos: Long = 0,
    val rebateCondition: String = "Paid on time",
    val startEpochDay: Long? = null,
    val notes: String = "",
)

@Entity(
    tableName = "loan_payments",
    foreignKeys = [ForeignKey(entity = LoanEntity::class, parentColumns = ["id"], childColumns = ["loanId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("loanId")],
)
data class LoanPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val installmentNumber: Int,
    val dueEpochDay: Long,
    val paidEpochDay: Long? = null,
    val amountCentavos: Long = 0,
    val status: String = "PENDING",
)

@Entity(
    tableName = "registration_records",
    foreignKeys = [ForeignKey(entity = MotorcycleEntity::class, parentColumns = ["id"], childColumns = ["motorcycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("motorcycleId")],
)
data class RegistrationRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val orDateEpochDay: Long? = null,
    val crDateEpochDay: Long? = null,
    val expiryEpochDay: Long? = null,
    val plateNumber: String = "",
    val temporaryPlate: Boolean = false,
    val dealerSubmissionEpochDay: Long? = null,
    val ltoTransactionReference: String = "",
    val notes: String = "",
)

@Entity(
    tableName = "insurance_records",
    foreignKeys = [ForeignKey(entity = MotorcycleEntity::class, parentColumns = ["id"], childColumns = ["motorcycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("motorcycleId")],
)
data class InsuranceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val provider: String = "",
    val policyNumber: String = "",
    val startEpochDay: Long? = null,
    val expiryEpochDay: Long? = null,
    val notes: String = "",
)

@Entity(
    tableName = "problem_logs",
    foreignKeys = [ForeignKey(entity = MotorcycleEntity::class, parentColumns = ["id"], childColumns = ["motorcycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("motorcycleId"), Index("serviceRecordId")],
)
data class ProblemLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val dateEpochDay: Long,
    val odometerKm: Long? = null,
    val severity: String,
    val symptom: String,
    val description: String = "",
    val mediaUri: String? = null,
    val resolved: Boolean = false,
    val resolution: String = "",
    val serviceRecordId: Long? = null,
)

@Entity(
    tableName = "coverage_plans",
    foreignKeys = [ForeignKey(entity = MotorcycleEntity::class, parentColumns = ["id"], childColumns = ["motorcycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("motorcycleId")],
)
data class CoveragePlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val startOdometerKm: Long,
    val limitOdometerKm: Long,
    val coveredServices: String = "",
    val coveredLabour: Boolean = false,
    val coveredParts: Boolean = false,
    val notes: String = "",
    val dealerName: String = "",
)

@Entity(tableName = "attachment_references", indices = [Index(value = ["ownerType", "ownerId"])])
data class AttachmentReferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerType: String,
    val ownerId: Long,
    val uri: String,
    val mediaType: String,
    val displayName: String = "",
)
