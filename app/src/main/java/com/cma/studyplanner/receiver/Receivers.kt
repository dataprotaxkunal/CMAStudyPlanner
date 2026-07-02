package com.cma.studyplanner.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.cma.studyplanner.CMAApp
import com.cma.studyplanner.MainActivity
import com.cma.studyplanner.R
import com.cma.studyplanner.service.AlarmActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Study Reminder"
        val body = intent.getStringExtra("body") ?: "Time to study!"
        val isAlarm = intent.getBooleanExtra("is_alarm", false)
        val subject = intent.getStringExtra("subject") ?: ""
        val chapter = intent.getStringExtra("chapter") ?: ""
        val time = intent.getStringExtra("time") ?: ""

        if (isAlarm) {
            // Full-screen alarm — like an incoming call
            val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("subject", subject)
                putExtra("chapter", chapter)
                putExtra("time", time)
            }
            context.startActivity(alarmIntent)
        } else {
            // Regular 5-minute warning notification
            val openIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, CMAApp.CHANNEL_REMINDER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openIntent)
                .setVibrate(longArrayOf(0, 300, 200, 300))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // TODO: Re-schedule alarms after reboot
        }
    }
}
