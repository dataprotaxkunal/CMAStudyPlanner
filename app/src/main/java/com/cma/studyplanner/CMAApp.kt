package com.cma.studyplanner

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.cma.studyplanner.data.AppDatabase
import com.cma.studyplanner.data.PrefsManager

class CMAApp : Application() {
    lateinit var database: AppDatabase
    lateinit var prefs: PrefsManager

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        prefs = PrefsManager(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        val focusChannel = NotificationChannel(
            CHANNEL_FOCUS, "Focus Mode",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows when focus mode is active" }

        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDER, "Study Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders and alarms for study sessions"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            setSound(
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }

        nm.createNotificationChannel(focusChannel)
        nm.createNotificationChannel(reminderChannel)
    }

    companion object {
        const val CHANNEL_FOCUS = "focus_channel"
        const val CHANNEL_REMINDER = "reminder_channel"
    }
}
