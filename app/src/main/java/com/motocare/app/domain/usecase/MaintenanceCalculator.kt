package com.motocare.app.domain.usecase

import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.domain.model.MaintenanceAssessment
import com.motocare.app.domain.model.MaintenanceStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class MaintenanceCalculator @Inject constructor() {
    fun assess(
        schedule: MaintenanceScheduleEntity,
        currentOdometerKm: Long,
        today: LocalDate = LocalDate.now(),
    ): MaintenanceAssessment {
        val remainingKm = schedule.nextDueOdometerKm?.minus(currentOdometerKm)
        val dueDate = schedule.nextDueEpochDay?.let(LocalDate::ofEpochDay)
        val remainingDays = dueDate?.let { ChronoUnit.DAYS.between(today, it) }
        val overdueByDistance = remainingKm?.let { it < 0 } == true
        val overdueByTime = remainingDays?.let { it < 0 } == true
        val dueNow = remainingKm == 0L || remainingDays == 0L
        val dueSoon = remainingKm?.let { it in 1..schedule.reminderLeadKm } == true ||
            remainingDays?.let { it in 1..schedule.reminderLeadDays.toLong() } == true

        val status = when {
            overdueByDistance || overdueByTime -> MaintenanceStatus.OVERDUE
            dueNow -> MaintenanceStatus.DUE
            dueSoon -> MaintenanceStatus.DUE_SOON
            else -> MaintenanceStatus.GOOD
        }
        return MaintenanceAssessment(status, remainingKm, remainingDays, overdueByDistance, overdueByTime)
    }
}
