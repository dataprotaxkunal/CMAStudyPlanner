package com.cma.studyplanner.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cma.studyplanner.data.PrefsManager
import com.cma.studyplanner.data.Schedule
import com.cma.studyplanner.receiver.ReminderReceiver
import java.util.Calendar
import kotlin.math.abs

object AlarmScheduler {

    private const val MORNING_REQUEST_CODE = 999_999

    /**
     * Generate a unique, stable request code from a schedule ID.
     * Uses abs() and masks to stay positive and avoid collisions
     * between the warning alarm (offset +100_000) and the main alarm.
     */
    private fun requestCodeFor(scheduleId: String): Int {
        // Use the full string's hashCode, then mask to a safe positive range
        return abs(scheduleId.hashCode()) % 100_000
    }

    /**
     * Schedule both the 5-minute warning and the exact-time alarm for a study session.
     */
    fun scheduleSessionAlarm(context: Context, schedule: Schedule) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val dateParts = schedule.date.split("-").map { it.toInt() }
            val timeParts = schedule.startTime.split(":").map { it.toInt() }

            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, dateParts[0])
                set(Calendar.MONTH, dateParts[1] - 1)
                set(Calendar.DAY_OF_MONTH, dateParts[2])
                set(Calendar.HOUR_OF_DAY, timeParts[0])
                set(Calendar.MINUTE, timeParts[1])
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val now = System.currentTimeMillis()
            val scheduleTime = calendar.timeInMillis
            val baseCode = requestCodeFor(schedule.id)

            // ── 5-minute warning notification ──
            val warningTime = scheduleTime - (5 * 60 * 1000)
            if (warningTime > now) {
                val warningIntent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("title", "⏰ Starting in 5 minutes!")
                    putExtra("body", "${schedule.subject} - ${schedule.chapter}")
                    putExtra("is_alarm", false)
                }
                val warningPending = PendingIntent.getBroadcast(
                    context, baseCode + 100_000, warningIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, warningTime, warningPending
                )
            }

            // ── Exact-time full-screen alarm ──
            if (scheduleTime > now) {
                val endTimeParts = schedule.endTime.split(":").map { it.toInt() }
                val startFormatted = formatTime12(timeParts[0], timeParts[1])
                val endFormatted = formatTime12(endTimeParts[0], endTimeParts[1])

                val alarmIntent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("title", "🔔 Study NOW!")
                    putExtra("body", "${schedule.subject} - ${schedule.chapter}")
                    putExtra("is_alarm", true)
                    putExtra("subject", schedule.subject)
                    putExtra("chapter", schedule.chapter)
                    putExtra("time", "$startFormatted - $endFormatted")
                }
                val alarmPending = PendingIntent.getBroadcast(
                    context, baseCode, alarmIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, scheduleTime, alarmPending
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Schedule the morning motivational alarm using setExactAndAllowWhileIdle.
     * Must be re-called each day (from ReminderReceiver after it fires, and from BootReceiver).
     */
    fun scheduleMorningAlarm(context: Context) {
        try {
            val prefs = PrefsManager(context)
            if (!prefs.onboarded || prefs.examName.isBlank()) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("is_morning", true)
                putExtra("title", "Good Morning! \uD83C\uDF1E")
                putExtra("subject", prefs.examName)
                putExtra("exam_date", prefs.examDate)
            }
            val pending = PendingIntent.getBroadcast(
                context, MORNING_REQUEST_CODE, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Next 7:00 AM
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            // Exact alarm — will be re-scheduled for the next day inside ReminderReceiver
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pending
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatTime12(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return String.format("%d:%02d %s", h, minute, amPm)
    }
}
