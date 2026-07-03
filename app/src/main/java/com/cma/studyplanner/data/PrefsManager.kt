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

    var calendarSyncEnabled: Boolean
        get() = prefs.getBoolean("calendar_sync", true)
        set(value) = prefs.edit().putBoolean("calendar_sync", value).apply()

    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(value) = prefs.edit().putBoolean("onboarded", value).apply()

    var userName: String
        get() = prefs.getString("user_name", "") ?: ""
        set(value) = prefs.edit().putString("user_name", value).apply()

    var userDob: String
        get() = prefs.getString("user_dob", "") ?: ""
        set(value) = prefs.edit().putString("user_dob", value).apply()

    var userEmail: String
        get() = prefs.getString("user_email", "") ?: ""
        set(value) = prefs.edit().putString("user_email", value).apply()

    var examName: String
        get() = prefs.getString("exam_name", "") ?: ""
        set(value) = prefs.edit().putString("exam_name", value).apply()

    var examDate: String
        get() = prefs.getString("exam_date", "") ?: ""
        set(value) = prefs.edit().putString("exam_date", value).apply()

    val whitelistedPackages: Set<String>
        get() = setOf(
            "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer",
            "com.android.messaging", "com.google.android.apps.messaging", "com.samsung.android.messaging",
            "com.android.settings", "com.cma.studyplanner", "com.android.systemui",
            "com.android.launcher", "com.android.launcher3",
            "com.google.android.apps.nexuslauncher", "com.sec.android.app.launcher"
        )
}
