package com.motocare.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val REMINDERS = "motocare_reminders"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(REMINDERS, "MotoCare reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Maintenance and ownership reminders"
            },
        )
    }
}
