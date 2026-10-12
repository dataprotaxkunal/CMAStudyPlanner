package com.cma.studyplanner.service

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.cma.studyplanner.CMAApp
import com.cma.studyplanner.MainActivity
import com.cma.studyplanner.R
import com.cma.studyplanner.data.PrefsManager

/**
 * Strict focus mode.
 *
 * DND allows starred contacts and repeat callers. Android/phone-carrier emergency-call
 * handling remains system-controlled and cannot be guaranteed by a third-party app.
 * App blocking uses Usage Access + the blocker activity; it is not device-owner kiosk mode.
 */
class FocusService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var monitoring = false
    private var lastBlockedPackage: String? = null
    private var lastBlockLaunchAt = 0L
    private lateinit var prefs: PrefsManager
    private val sessionPrefs by lazy {
        getSharedPreferences("focus_session_state", Context.MODE_PRIVATE)
    }

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (monitoring) {
                checkForegroundApp()
                handler.postDelayed(this, 700)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = (application as CMAApp).prefs
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startFocusMode()
            ACTION_STOP -> stopFocusMode()
        }
        return START_STICKY
    }

    private fun startFocusMode() {
        startForeground(NOTIFICATION_ID, buildNotification())

        if (prefs.dndEnabled) enableDndAllowingPriorityCalls()

        if (prefs.appBlockingEnabled) {
            monitoring = true
            handler.removeCallbacks(monitorRunnable)
            handler.post(monitorRunnable)
        }
    }

    private fun stopFocusMode() {
        monitoring = false
        handler.removeCallbacks(monitorRunnable)
        restorePreviousDndSettings()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun enableDndAllowingPriorityCalls() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.isNotificationPolicyAccessGranted) return

            // Save the user's existing DND policy only once for this focus session.
            if (!sessionPrefs.getBoolean(KEY_DND_SAVED, false)) {
                val previous = nm.notificationPolicy
                sessionPrefs.edit()
                    .putInt(KEY_PREVIOUS_FILTER, nm.currentInterruptionFilter)
                    .putInt(KEY_PREVIOUS_CATEGORIES, previous.priorityCategories)
                    .putInt(KEY_PREVIOUS_CALL_SENDERS, previous.priorityCallSenders)
                    .putInt(KEY_PREVIOUS_MESSAGE_SENDERS, previous.priorityMessageSenders)
                    .putInt(KEY_PREVIOUS_SUPPRESSED_EFFECTS, previous.suppressedVisualEffects)
                    .putBoolean(KEY_DND_SAVED, true)
                    .apply()
            }

            // Calls from starred contacts and repeat callers are allowed by Priority DND.
            // Users select trusted contacts by starring them in their phone's Contacts app.
            val policy = NotificationManager.Policy(
                NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or
                    NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS,
                NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                nm.notificationPolicy.suppressedVisualEffects
            )
            nm.setNotificationPolicy(policy)
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        } catch (_: SecurityException) {
            // Settings screen explains how to grant Notification Policy access.
        } catch (_: Exception) {
            // Keep focus mode running even if this Android skin rejects a DND policy update.
        }
    }

    private fun restorePreviousDndSettings() {
        try {
            if (!sessionPrefs.getBoolean(KEY_DND_SAVED, false)) return
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.isNotificationPolicyAccessGranted) return

            val policy = NotificationManager.Policy(
                sessionPrefs.getInt(KEY_PREVIOUS_CATEGORIES, 0),
                sessionPrefs.getInt(KEY_PREVIOUS_CALL_SENDERS, NotificationManager.Policy.PRIORITY_SENDERS_ANY),
                sessionPrefs.getInt(KEY_PREVIOUS_MESSAGE_SENDERS, NotificationManager.Policy.PRIORITY_SENDERS_ANY),
                sessionPrefs.getInt(KEY_PREVIOUS_SUPPRESSED_EFFECTS, 0)
            )
            nm.setNotificationPolicy(policy)
            nm.setInterruptionFilter(
                sessionPrefs.getInt(KEY_PREVIOUS_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
            )
            sessionPrefs.edit().putBoolean(KEY_DND_SAVED, false).apply()
        } catch (_: Exception) {
            // Do not crash while ending a study session.
        }
    }

    private fun checkForegroundApp() {
        try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - 10_000, now)
            val event = UsageEvents.Event()
            var foregroundPackage: String? = null
            var latestForegroundTime = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND &&
                    event.timeStamp >= latestForegroundTime
                ) {
                    latestForegroundTime = event.timeStamp
                    foregroundPackage = event.packageName
                }
            }

            val pkg = foregroundPackage ?: return
            if (pkg == packageName || pkg in prefs.whitelistedPackages) {
                lastBlockedPackage = null
                return
            }

            // Avoid repeatedly launching the blocker over itself for the same app.
            if (pkg == lastBlockedPackage && now - lastBlockLaunchAt < 2_000) return
            lastBlockedPackage = pkg
            lastBlockLaunchAt = now

            val blockerIntent = Intent(this, BlockerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("blocked_app", pkg)
            }
            startActivity(blockerIntent)
        } catch (_: SecurityException) {
            // Usage Access must be enabled in Settings for app blocking to work.
        } catch (_: Exception) {
            // Keep the foreground service alive if usage data is temporarily unavailable.
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CMAApp.CHANNEL_FOCUS)
            .setContentTitle("📚 Strict Focus Mode Active")
            .setContentText("Starred contacts and repeat callers can reach you")
            .setSmallIcon(R.drawable.ic_focus)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.cma.studyplanner.START_FOCUS"
        const val ACTION_STOP = "com.cma.studyplanner.STOP_FOCUS"
        const val NOTIFICATION_ID = 1001
        private const val KEY_DND_SAVED = "dnd_saved"
        private const val KEY_PREVIOUS_FILTER = "previous_filter"
        private const val KEY_PREVIOUS_CATEGORIES = "previous_categories"
        private const val KEY_PREVIOUS_CALL_SENDERS = "previous_call_senders"
        private const val KEY_PREVIOUS_MESSAGE_SENDERS = "previous_message_senders"
        private const val KEY_PREVIOUS_SUPPRESSED_EFFECTS = "previous_suppressed_effects"
    }
}
