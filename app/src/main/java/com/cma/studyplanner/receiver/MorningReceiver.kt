package com.cma.studyplanner.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cma.studyplanner.service.MorningAlarmActivity

class MorningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmIntent = Intent(context, MorningAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(alarmIntent)
    }
}
