package com.cma.studyplanner.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.cma.studyplanner.CMAApp
import com.cma.studyplanner.MainActivity
import com.cma.studyplanner.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Study Reminder"
        val body = intent.getStringExtra("body") ?: "Time to study!"
        val isAlarm = intent.getBooleanExtra("is_alarm", false)

        // Open app when notification is tapped
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmSound = RingtoneManager.getDefaultUri(
            if (isAlarm) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION
        )

        val builder = NotificationCompat.Builder(context, CMAApp.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // For alarm-type notifications, make it a full-screen intent
        if (isAlarm) {
            builder.setFullScreenIntent(openIntent, true)
            builder.setOngoing(true) // Can't swipe away easily
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), builder.build())

        // Also vibrate the phone
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 200, 500, 200, 500), -1
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Play alarm sound for 10 seconds if it's an alarm
        if (isAlarm) {
            try {
                val ringtone = RingtoneManager.getRingtone(context, alarmSound)
                ringtone?.play()
                // Stop after 15 seconds
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try { ringtone?.stop() } catch (_: Exception) {}
                }, 15000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // TODO: Re-schedule alarms after reboot by reading from database
        }
    }
}
