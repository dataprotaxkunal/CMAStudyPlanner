package com.cma.studyplanner.data

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cma_prefs", Context.MODE_PRIVATE)

    var whatsappNumber: String
        get() = prefs.getString("whatsapp", "") ?: ""
        set(value) = prefs.edit().putString("whatsapp", value).apply()

    var focusStudyMinutes: Int
        get() = prefs.getInt("focus_study", 25)
        set(value) = prefs.edit().putInt("focus_study", value).apply()

    var focusBreakMinutes: Int
        get() = prefs.getInt("focus_break", 5)
        set(value) = prefs.edit().putInt("focus_break", value).apply()

    var dndEnabled: Boolean
        get() = prefs.getBoolean("dnd_enabled", true)
        set(value) = prefs.edit().putBoolean("dnd_enabled", value).apply()

    var appBlockingEnabled: Boolean
        get() = prefs.getBoolean("app_blocking", true)
        set(value) = prefs.edit().putBoolean("app_blocking", value).apply()

    // Whitelist: phone, messages, settings, this app
    val whitelistedPackages: Set<String>
        get() = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.messaging",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.settings",
            "com.cma.studyplanner",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher"
        )
}
