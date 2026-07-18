package com.motocare.app.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.motocare.app.MainActivity
import com.motocare.app.R
import com.motocare.app.data.repository.MaintenanceRepository
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.data.local.dao.LoanDao
import com.motocare.app.data.local.dao.OdometerDao
import com.motocare.app.data.local.dao.PhaseThreeDao
import com.motocare.app.domain.usecase.CoverageCalculator
import com.motocare.app.domain.model.MaintenanceStatus
import com.motocare.app.domain.usecase.MaintenanceCalculator
import com.motocare.app.notification.NotificationChannels
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val maintenance: MaintenanceRepository,
    private val motorcycles: MotorcycleRepository,
    private val preferences: PreferencesRepository,
    private val calculator: MaintenanceCalculator,
    private val coverageCalculator: CoverageCalculator,
    private val phaseThreeDao: PhaseThreeDao,
    private val loanDao: LoanDao,
    private val odometerDao: OdometerDao,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        if (!preferences.notificationsEnabled.first()) return@runCatching Result.success()
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return@runCatching Result.success()

        maintenance.getAllActive().forEach { schedule ->
            val bike = motorcycles.get(schedule.motorcycleId) ?: return@forEach
            val assessment = calculator.assess(schedule, bike.currentOdometerKm)
            if (assessment.status == MaintenanceStatus.DUE_SOON || assessment.status == MaintenanceStatus.DUE || assessment.status == MaintenanceStatus.OVERDUE) {
                notify(
                    id = schedule.id.toInt(),
                    title = "${schedule.name}: ${assessment.status.name.replace('_', ' ').lowercase()}",
                    body = "${bike.name} • open MotoCare to review the schedule",
                )
            }
        }
        val today = LocalDate.now()
        val activeBikes = motorcycles.activeMotorcycles.first()
        val registrations = phaseThreeDao.getAllRegistrations()
        registrations.forEach { record ->
            record.expiryEpochDay?.let { notifyDate(record.motorcycleId, 100_000, "Registration", it, today) }
        }
        activeBikes.filter { bike -> registrations.none { it.motorcycleId == bike.id } }.forEach { bike ->
            bike.registrationExpiryEpochDay?.let { notifyDate(bike.id, 100_000, "Registration", it, today) }
        }
        val insuranceRecords = phaseThreeDao.getAllInsurance()
        insuranceRecords.forEach { record ->
            record.expiryEpochDay?.let { notifyDate(record.motorcycleId, 200_000, "Insurance", it, today) }
        }
        activeBikes.filter { bike -> insuranceRecords.none { it.motorcycleId == bike.id } }.forEach { bike ->
            bike.insuranceExpiryEpochDay?.let { notifyDate(bike.id, 200_000, "Insurance", it, today) }
        }
        phaseThreeDao.getAllCoverage().forEach { plan ->
            val bike = motorcycles.get(plan.motorcycleId) ?: return@forEach
            val start = LocalDate.ofEpochDay(plan.startEpochDay)
            val days = ChronoUnit.DAYS.between(start, today).coerceAtLeast(1)
            val average = (bike.currentOdometerKm - bike.initialOdometerKm).coerceAtLeast(0).toDouble() / days
            val assessment = coverageCalculator.assess(plan, bike.currentOdometerKm, average, today)
            if (assessment.remainingDays <= 14 || assessment.remainingKm <= 500) {
                notify(300_000 + plan.id.toInt(), "Maintenance coverage ending", "${bike.name} • ${assessment.remainingDays} days or ${assessment.remainingKm} km remaining")
            }
        }
        loanDao.getAllLoans().forEach { loan ->
            val next = loanDao.getPayments(loan.id).filter { it.status == "PENDING" || it.status == "MISSED" }.minByOrNull { it.dueEpochDay }
            if (next != null) {
                val days = ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(next.dueEpochDay))
                if (days <= 3) motorcycles.get(loan.motorcycleId)?.let { bike -> notify(400_000 + loan.id.toInt(), "Loan payment due", "${bike.name} • payment ${next.installmentNumber} is due in $days days") }
            }
        }
        val staleDays = preferences.staleOdometerDays.first()
        activeBikes.forEach { bike ->
            val latest = odometerDao.latest(bike.id) ?: return@forEach
            val lastDate = Instant.ofEpochMilli(latest.recordedAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            if (ChronoUnit.DAYS.between(lastDate, today) >= staleDays) {
                notify(500_000 + bike.id.toInt(), "Update ${bike.name} odometer", "No odometer update for $staleDays days")
            }
        }
        Result.success()
    }.getOrElse { Result.retry() }

    private fun notify(id: Int, title: String, body: String) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    private suspend fun notifyDate(motorcycleId: Long, offset: Int, label: String, expiryEpochDay: Long, today: LocalDate) {
        val days = ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(expiryEpochDay))
        if (days <= 30) motorcycles.get(motorcycleId)?.let { bike ->
            notify(offset + motorcycleId.toInt(), "$label ${if (days < 0) "overdue" else "expiring"}", "${bike.name} • $days days remaining")
        }
    }
}
