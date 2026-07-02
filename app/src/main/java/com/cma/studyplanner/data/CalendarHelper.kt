package com.cma.studyplanner.data

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.util.Calendar
import java.util.TimeZone

object CalendarHelper {

    /**
     * Inserts a study session into Google Calendar with reminders.
     * Returns the event ID or -1 if failed.
     */
    fun addEventToCalendar(
        context: Context,
        subject: String,
        chapter: String,
        date: String,        // YYYY-MM-DD
        startTime: String,   // HH:mm
        endTime: String      // HH:mm
    ): Long {
        return try {
            val calendarId = getPrimaryCalendarId(context) ?: return -1

            val dateParts = date.split("-").map { it.toInt() }
            val startParts = startTime.split(":").map { it.toInt() }
            val endParts = endTime.split(":").map { it.toInt() }

            val startCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, dateParts[0])
                set(Calendar.MONTH, dateParts[1] - 1)
                set(Calendar.DAY_OF_MONTH, dateParts[2])
                set(Calendar.HOUR_OF_DAY, startParts[0])
                set(Calendar.MINUTE, startParts[1])
                set(Calendar.SECOND, 0)
            }

            val endCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, dateParts[0])
                set(Calendar.MONTH, dateParts[1] - 1)
                set(Calendar.DAY_OF_MONTH, dateParts[2])
                set(Calendar.HOUR_OF_DAY, endParts[0])
                set(Calendar.MINUTE, endParts[1])
                set(Calendar.SECOND, 0)
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, "📚 $subject")
                put(CalendarContract.Events.DESCRIPTION, "Chapter: $chapter\n\nCMA Study Session - Added by CMA Study Planner")
                put(CalendarContract.Events.DTSTART, startCal.timeInMillis)
                put(CalendarContract.Events.DTEND, endCal.timeInMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.EVENT_COLOR, 0xFF6366F1.toInt())
                put(CalendarContract.Events.HAS_ALARM, 1)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment?.toLongOrNull() ?: -1

            if (eventId > 0) {
                // Add 5-minute reminder
                val reminder5 = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, 5)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminder5)

                // Add 15-minute reminder
                val reminder15 = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, 15)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminder15)
            }

            eventId
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * Gets the primary Google Calendar ID on the device.
     */
    private fun getPrimaryCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )

        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null, null, null
        ) ?: return null

        var calendarId: Long? = null
        var googleCalendarId: Long? = null

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val isPrimary = it.getInt(1)
                val accountType = it.getString(2)

                if (isPrimary == 1) {
                    calendarId = id
                    break
                }
                // Fallback: use first Google calendar found
                if (accountType == "com.google" && googleCalendarId == null) {
                    googleCalendarId = id
                }
            }
        }

        return calendarId ?: googleCalendarId
    }
}
