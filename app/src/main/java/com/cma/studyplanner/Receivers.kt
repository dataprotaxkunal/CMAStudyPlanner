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
import com.cma.studyplanner.data.AppDatabase
import com.cma.studyplanner.service.AlarmActivity
import com.cma.studyplanner.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Study Reminder"
        val body = intent.getStringExtra("body") ?: "Time to study!"
        val isMorning = intent.getBooleanExtra("is_morning", false)

        if (isMorning) {
            // Re-schedule tomorrow's morning alarm (since we no longer use setRepeating)
            AlarmScheduler.scheduleMorningAlarm(context)

            // Launch full-screen morning countdown
            val morningIntent = Intent(context, com.cma.studyplanner.service.MorningAlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("exam_name", intent.getStringExtra("subject") ?: "")
                putExtra("exam_date", intent.getStringExtra("exam_date") ?: "")
            }
            context.startActivity(morningIntent)
            return
        }

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
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Re-schedule the morning alarm
        AlarmScheduler.scheduleMorningAlarm(context)

        // Re-schedule all future study session alarms from the database
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val today = java.time.LocalDate.now().toString() // yyyy-MM-dd
                val allSchedules = db.scheduleDao().getAllOnce()

                for (schedule in allSchedules) {
                    if (schedule.completed) continue
                    // Only re-schedule future sessions
                    if (schedule.date < today) continue
                    AlarmScheduler.scheduleSessionAlarm(context, schedule)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
