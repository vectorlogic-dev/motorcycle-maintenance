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
import com.motocare.app.domain.model.MaintenanceStatus
import com.motocare.app.domain.usecase.MaintenanceCalculator
import com.motocare.app.notification.NotificationChannels
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val maintenance: MaintenanceRepository,
    private val motorcycles: MotorcycleRepository,
    private val preferences: PreferencesRepository,
    private val calculator: MaintenanceCalculator,
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
}
