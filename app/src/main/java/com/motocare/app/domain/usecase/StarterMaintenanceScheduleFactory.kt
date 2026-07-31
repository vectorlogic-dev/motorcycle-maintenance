package com.motocare.app.domain.usecase

import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import java.time.LocalDate
import javax.inject.Inject

class StarterMaintenanceScheduleFactory @Inject constructor() {
    fun create(
        motorcycleId: Long,
        currentOdometerKm: Long,
        driveType: String = "UNKNOWN",
        coolingType: String = "UNKNOWN",
        startDate: LocalDate = LocalDate.now(),
    ): List<MaintenanceScheduleEntity> {
        val applicableTemplates = buildList {
            addAll(commonTemplates)
            when (driveType) {
                "CHAIN" -> add(chainTemplate)
                "BELT" -> add(beltTemplate)
            }
            if (coolingType == "LIQUID") add(coolantTemplate)
        }
        return applicableTemplates.map { template ->
            MaintenanceScheduleEntity(
                motorcycleId = motorcycleId,
                name = template.name,
                description = template.description,
                intervalKm = template.intervalKm,
                intervalDays = template.intervalDays,
                nextDueOdometerKm = template.intervalKm?.let(currentOdometerKm::plus),
                nextDueEpochDay = template.intervalDays?.let { startDate.plusDays(it.toLong()).toEpochDay() },
                reminderLeadKm = template.reminderLeadKm,
                reminderLeadDays = template.reminderLeadDays,
                source = SOURCE,
                isEditableTemplate = true,
            )
        }
    }

    private data class Template(
        val name: String,
        val description: String,
        val intervalKm: Long?,
        val intervalDays: Int?,
        val reminderLeadKm: Long = 500,
        val reminderLeadDays: Int = 30,
    )

    private companion object {
        const val SOURCE = "RESEARCH_STARTER_V1"

        val commonTemplates = listOf(
            Template(
                name = "Engine oil change",
                description = "General road-use starting point. Confirm the oil specification and interval for this motorcycle.",
                intervalKm = 3_000,
                intervalDays = 365,
                reminderLeadKm = 300,
            ),
            Template(
                name = "Air filter inspection",
                description = "Inspect sooner in dusty, wet, or flooded-road conditions; replace as specified by the manufacturer.",
                intervalKm = 4_000,
                intervalDays = 365,
            ),
            Template(
                name = "Brake system inspection",
                description = "Inspect operation and wear. Include pads or shoes, hoses, and fluid level where fitted.",
                intervalKm = 4_000,
                intervalDays = 365,
            ),
            Template(
                name = "Spark plug inspection",
                description = "Inspect or replace only as specified for this motorcycle and spark plug type.",
                intervalKm = 6_000,
                intervalDays = 365,
            ),
            Template(
                name = "Brake fluid replacement (if fitted)",
                description = "For hydraulic brakes only. Replacement requires the correct fluid and safe service procedure.",
                intervalKm = null,
                intervalDays = 730,
                reminderLeadDays = 30,
            ),
        )

        val chainTemplate = Template(
            name = "Drive chain service",
            description = "Inspect, adjust, clean, and lubricate as specified for this motorcycle and riding conditions.",
            intervalKm = 1_000,
            intervalDays = null,
            reminderLeadKm = 100,
            reminderLeadDays = 14,
        )

        val beltTemplate = Template(
            name = "Drive belt inspection",
            description = "Inspect the drive belt or CVT at the manufacturer interval; replacement mileage varies substantially by model.",
            intervalKm = 4_000,
            intervalDays = 365,
        )

        val coolantTemplate = Template(
            name = "Coolant replacement",
            description = "Use only the coolant type and replacement procedure specified for this liquid-cooled motorcycle.",
            intervalKm = null,
            intervalDays = 1_095,
            reminderLeadDays = 30,
        )
    }
}
