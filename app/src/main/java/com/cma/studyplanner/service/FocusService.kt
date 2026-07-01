package com.cma.studyplanner.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.cma.studyplanner.CMAApp
import com.cma.studyplanner.MainActivity
import com.cma.studyplanner.R
import com.cma.studyplanner.data.PrefsManager

class FocusService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var monitoring = false
    private lateinit var prefs: PrefsManager

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (monitoring) {
                checkForegroundApp()
                handler.postDelayed(this, 500) // Check every 500ms
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
        // Start foreground notification
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Enable DND if setting is on
        if (prefs.dndEnabled) {
            enableDND()
        }

        // Start app monitoring if setting is on
        if (prefs.appBlockingEnabled) {
            monitoring = true
            handler.post(monitorRunnable)
        }
    }

    private fun stopFocusMode() {
        monitoring = false
        handler.removeCallbacks(monitorRunnable)

        // Disable DND
        if (prefs.dndEnabled) {
            disableDND()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun enableDND() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun disableDND() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkForegroundApp() {
        try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                now - 2000, now
            )

            if (stats.isNullOrEmpty()) return

            val foregroundApp = stats
                .sortedByDescending { it.lastTimeUsed }
                .firstOrNull()
                ?.packageName ?: return

            val whitelist = prefs.whitelistedPackages

            if (foregroundApp !in whitelist && !foregroundApp.startsWith("com.android.")) {
                // Block: launch blocker overlay
                val blockerIntent = Intent(this, BlockerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("blocked_app", foregroundApp)
                }
                startActivity(blockerIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CMAApp.CHANNEL_FOCUS)
            .setContentTitle("📚 Focus Mode Active")
            .setContentText("DND on · Apps blocked · Stay focused!")
            .setSmallIcon(R.drawable.ic_focus)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_stop, "Stop",
                PendingIntent.getService(
                    this, 1,
                    Intent(this, FocusService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.cma.studyplanner.START_FOCUS"
        const val ACTION_STOP = "com.cma.studyplanner.STOP_FOCUS"
        const val NOTIFICATION_ID = 1001
    }
}
