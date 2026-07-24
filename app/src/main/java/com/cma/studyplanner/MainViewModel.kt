package com.cma.studyplanner.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cma.studyplanner.CMAApp
import com.cma.studyplanner.data.*
import com.cma.studyplanner.service.FocusService
import com.cma.studyplanner.util.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CMAApp
    private val db = app.database
    private val prefs = app.prefs

    // CMA preset subjects
    val suggestedSubjects: List<String>
        get() {
            val exam = prefs.examName
            return when {
                exam.contains("CMA", true) -> listOf("Financial Planning", "Strategic Financial Mgmt", "Cost Management", "Internal Controls", "Financial Statement Analysis", "Corporate Finance", "Decision Analysis", "Risk Management", "Investment Decisions", "Professional Ethics")
                exam.contains("CA", true) -> listOf("Accounting", "Auditing", "Taxation", "Corporate Law", "Cost & Mgmt Accounting", "Financial Management", "Strategic Management")
                exam.contains("UPSC", true) -> listOf("History", "Geography", "Polity", "Economy", "Science", "Current Affairs", "Ethics", "Essay")
                exam.contains("NEET", true) || exam.contains("Medical", true) -> listOf("Physics", "Chemistry", "Biology", "Zoology", "Botany")
                exam.contains("JEE", true) || exam.contains("GATE", true) -> listOf("Physics", "Chemistry", "Mathematics")
                exam.contains("CAT", true) || exam.contains("GMAT", true) -> listOf("Quantitative Aptitude", "Verbal Ability", "Data Interpretation", "Logical Reasoning")
                else -> listOf("Subject 1", "Subject 2", "Subject 3")
            }
        }

    val colors = listOf(
        0xFF6366F1, 0xFFF59E0B, 0xFF10B981, 0xFFEF4444, 0xFF8B5CF6,
        0xFFEC4899, 0xFF14B8A6, 0xFFF97316, 0xFF3B82F6, 0xFF84CC16
    )
    val lightColors = listOf(
        0xFFEEF0FF, 0xFFFEF3C7, 0xFFD1FAE5, 0xFFFEE2E2, 0xFFEDE9FE,
        0xFFFCE7F3, 0xFFCCFBF1, 0xFFFFEDD5, 0xFFDBEAFE, 0xFFECFCCB
    )

    // Data flows
    val subjectsWithChapters: StateFlow<List<SubjectWithChapters>> =
        db.subjectDao().getAllWithChapters()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schedules: StateFlow<List<Schedule>> =
        db.scheduleDao().getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val revisions: StateFlow<List<Revision>> =
        db.revisionDao().getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    private val _expandedSubjectId = MutableStateFlow<String?>(null)
    val expandedSubjectId = _expandedSubjectId.asStateFlow()

    // Focus state
    private val _focusActive = MutableStateFlow(false)
    val focusActive = _focusActive.asStateFlow()

    private val _focusPaused = MutableStateFlow(true)
    val focusPaused = _focusPaused.asStateFlow()

    private val _focusSeconds = MutableStateFlow(0)
    val focusSeconds = _focusSeconds.asStateFlow()

    private val _focusMode = MutableStateFlow("study") // "study" or "break"
    val focusMode = _focusMode.asStateFlow()

    private val _focusSessions = MutableStateFlow(0)
    val focusSessions = _focusSessions.asStateFlow()

    private val _focusTotalSeconds = MutableStateFlow(prefs.focusStudyMinutes * 60)
    val focusTotalSeconds = _focusTotalSeconds.asStateFlow()

    // Calendar state
    private val _calendarWeekStart = MutableStateFlow(getWeekStart(LocalDate.now()))
    val calendarWeekStart = _calendarWeekStart.asStateFlow()

    private val _calendarMonth = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val calendarMonth = _calendarMonth.asStateFlow()

    private val _calendarView = MutableStateFlow("week")
    val calendarView = _calendarView.asStateFlow()

    // Settings accessors
    val whatsappNumber: String get() = prefs.whatsappNumber
    val focusStudyMinutes: Int get() = prefs.focusStudyMinutes
    val focusBreakMinutes: Int get() = prefs.focusBreakMinutes
    val dndEnabled: Boolean get() = prefs.dndEnabled
    val appBlockingEnabled: Boolean get() = prefs.appBlockingEnabled
    val calendarSyncEnabled: Boolean get() = prefs.calendarSyncEnabled

    // Profile
    fun getUserName(): String = prefs.userName
    fun getExamName(): String = prefs.examName
    fun getExamDate(): String = prefs.examDate
    fun isOnboarded(): Boolean = prefs.onboarded

    fun saveProfile(name: String, dob: String, email: String, exam: String, examDate: String) {
        prefs.userName = name
        prefs.userDob = dob
        prefs.userEmail = email
        prefs.examName = exam
        prefs.examDate = examDate
        prefs.onboarded = true
        scheduleMorningAlarm()
    }

    fun scheduleMorningAlarm() {
        AlarmScheduler.scheduleMorningAlarm(getApplication())
    }

    private fun getDaysLeft(): Int {
        return try {
            val ed = java.time.LocalDate.parse(prefs.examDate)
            java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), ed).toInt().coerceAtLeast(0)
        } catch (e: Exception) { 0 }
    }


    // ─── Profile ───
    // ─── Stats ───
    fun getTotalChapters(): Int = subjectsWithChapters.value.sumOf { it.chapters.size }
    fun getDoneChapters(): Int = subjectsWithChapters.value.sumOf { it.chapters.count { c -> c.done } }
    fun getProgressPercent(): Int {
        val total = getTotalChapters()
        return if (total > 0) (getDoneChapters() * 100 / total) else 0
    }

    fun getTodaySchedules(): List<Schedule> {
        val today = LocalDate.now().toString()
        return schedules.value.filter { it.date == today }
    }

    fun getSubjectStudyHours(subjectName: String): Double {
        return schedules.value
            .filter { it.subject == subjectName && it.completed }
            .sumOf { timeDiffMinutes(it.startTime, it.endTime).coerceAtLeast(0) } / 60.0
    }

    // ─── Subjects ───
    fun addSubject(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val existing = db.subjectDao().getAllOnce()
            if (existing.any { it.name.equals(name, true) }) {
                _toastMessage.emit("Subject already exists")
                return@launch
            }
            val colorIdx = existing.size % colors.size
            db.subjectDao().insert(Subject(name = name.trim(), colorIndex = colorIdx))
            _toastMessage.emit("✅ Added: $name")
        }
    }

    fun deleteSubject(id: String) {
        viewModelScope.launch {
            db.subjectDao().deleteById(id)
            _toastMessage.emit("🗑 Subject deleted")
        }
    }

    fun toggleExpandSubject(id: String) {
        _expandedSubjectId.value = if (_expandedSubjectId.value == id) null else id
    }

    // ─── Chapters ───
    fun addChapter(subjectId: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            db.chapterDao().insert(Chapter(subjectId = subjectId, name = name.trim()))
            _toastMessage.emit("✅ Chapter added")
        }
    }

    fun toggleChapter(chapter: Chapter) {
        viewModelScope.launch {
            db.chapterDao().update(chapter.copy(done = !chapter.done))
        }
    }

    fun deleteChapter(id: String) {
        viewModelScope.launch {
            db.chapterDao().deleteById(id)
        }
    }

    fun bulkAddChapters(subjectId: String, names: List<String>) {
        viewModelScope.launch {
            val chapters = names.filter { it.isNotBlank() }.map {
                Chapter(subjectId = subjectId, name = it.trim())
            }
            db.chapterDao().insertAll(chapters)
            _toastMessage.emit("✅ Added ${chapters.size} chapters")
        }
    }

    // ─── Schedules ───
    fun addSchedule(subject: String, chapter: String, date: String, startTime: String, endTime: String) {
        if (subject.isBlank() || chapter.isBlank() || date.isBlank()) return
        viewModelScope.launch {
            db.scheduleDao().insert(
                Schedule(subject = subject, chapter = chapter, date = date,
                    startTime = startTime, endTime = endTime)
            )
            _toastMessage.emit("✅ Session scheduled")
        }
    }

    fun addScheduleWithAlarm(subject: String, chapter: String, date: String, startTime: String, endTime: String) {
        if (subject.isBlank() || chapter.isBlank() || date.isBlank()) return
        viewModelScope.launch {
            val schedule = Schedule(subject = subject, chapter = chapter, date = date,
                startTime = startTime, endTime = endTime)
            db.scheduleDao().insert(schedule)
            scheduleAlarm(schedule)

            // Sync to Google Calendar
            if (prefs.calendarSyncEnabled) {
                try {
                    val eventId = com.cma.studyplanner.data.CalendarHelper.addEventToCalendar(
                        getApplication(), subject, chapter, date, startTime, endTime
                    )
                    if (eventId > 0) {
                        _toastMessage.emit("✅ Scheduled + added to Google Calendar")
                    } else {
                        _toastMessage.emit("✅ Scheduled (Calendar sync failed — check permissions)")
                    }
                } catch (e: Exception) {
                    _toastMessage.emit("✅ Scheduled (Calendar permission needed)")
                }
            } else {
                _toastMessage.emit("✅ Session scheduled with alarm")
            }
        }
    }

    private fun scheduleAlarm(schedule: Schedule) {
        AlarmScheduler.scheduleSessionAlarm(getApplication(), schedule)
    }

    fun toggleScheduleComplete(schedule: Schedule) {
        viewModelScope.launch {
            db.scheduleDao().update(schedule.copy(completed = !schedule.completed))
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            db.scheduleDao().deleteById(id)
        }
    }

    fun bulkAddSchedules(list: List<Schedule>) {
        viewModelScope.launch {
            db.scheduleDao().insertAll(list)
            _toastMessage.emit("✅ Added ${list.size} schedules")
        }
    }

    suspend fun getSchedulesForRange(start: String, end: String): List<Schedule> {
        return db.scheduleDao().getByDateRange(start, end)
    }

    // ─── Revisions ───
    fun addRevision(subjectId: String, chapterId: String, date: String, startTime: String, endTime: String, notes: String) {
        viewModelScope.launch {
            val swc = db.subjectDao().getAllWithChaptersOnce()
            val subj = swc.find { it.subject.id == subjectId }
            val chap = subj?.chapters?.find { it.id == chapterId }
            if (subj == null || chap == null) {
                _toastMessage.emit("❌ Invalid subject/chapter")
                return@launch
            }
            db.revisionDao().insert(
                Revision(
                    subjectId = subjectId, subjectName = subj.subject.name,
                    chapterId = chapterId, chapterName = chap.name,
                    date = date, startTime = startTime, endTime = endTime, notes = notes
                )
            )
            _toastMessage.emit("✅ Revision added")
        }
    }

    // ─── Edit Schedule ───
    fun editSchedule(id: String, subject: String, chapter: String, date: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            val existing = db.scheduleDao().getAllOnce().find { it.id == id } ?: return@launch
            db.scheduleDao().update(existing.copy(
                subject = subject, chapter = chapter, date = date,
                startTime = startTime, endTime = endTime
            ))
            _toastMessage.emit("✅ Schedule updated")
        }
    }

    // ─── Edit Revision ───
    fun editRevision(id: String, date: String, startTime: String, endTime: String, notes: String) {
        viewModelScope.launch {
            val existing = db.revisionDao().getAllOnce().find { it.id == id } ?: return@launch
            db.revisionDao().update(existing.copy(
                date = date, startTime = startTime, endTime = endTime, notes = notes
            ))
            _toastMessage.emit("✅ Revision updated")
        }
    }

    // ─── Weekly Study Plan ───
    fun getDayHours(day: Int): Float = prefs.getDayHours(day)
    fun setDayHours(day: Int, hours: Float) = prefs.setDayHours(day, hours)
    fun getWeeklyPlannedHours(): Float = prefs.getWeeklyPlannedHours()

    fun getThisWeekActualHours(): Float {
        val now = java.time.LocalDate.now()
        val weekStart = now.minusDays(now.dayOfWeek.value.toLong() % 7)
        val weekEnd = weekStart.plusDays(6)
        return schedules.value
            .filter { it.completed && it.date >= weekStart.toString() && it.date <= weekEnd.toString() }
            .sumOf { timeDiffMinutes(it.startTime, it.endTime).coerceAtLeast(0) } / 60f
    }

    fun getWeeklyDeficit(): Float {
        val planned = getWeeklyPlannedHours() + prefs.weeklyDeficitCarryOver
        val actual = getThisWeekActualHours()
        return (planned - actual).coerceAtLeast(0f)
    }

    fun getNextWeekTarget(): Float {
        return getWeeklyPlannedHours() + getWeeklyDeficit()
    }

    fun getExamTotalHours(): Float {
        val daysLeft = try {
            java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(),
                java.time.LocalDate.parse(prefs.examDate)
            ).toInt().coerceAtLeast(0)
        } catch (e: Exception) { 0 }
        val weeksLeft = daysLeft / 7f
        return weeksLeft * getWeeklyPlannedHours()
    }

    fun toggleRevisionComplete(revision: Revision) {
        viewModelScope.launch {
            db.revisionDao().update(revision.copy(completed = !revision.completed))
        }
    }

    fun deleteRevision(id: String) {
        viewModelScope.launch {
            db.revisionDao().deleteById(id)
        }
    }

    // ─── Focus Mode ───
    fun startFocus() {
        _focusActive.value = true
        _focusPaused.value = false
        _focusMode.value = "study"
        _focusSeconds.value = 0
        _focusTotalSeconds.value = prefs.focusStudyMinutes * 60

        // Start foreground service with DND + app blocking
        val intent = Intent(getApplication(), FocusService::class.java).apply {
            action = FocusService.ACTION_START
        }
        getApplication<Application>().startForegroundService(intent)
    }

    fun pauseFocus() {
        _focusPaused.value = !_focusPaused.value
    }

    fun stopFocus() {
        _focusActive.value = false
        _focusPaused.value = true
        _focusSeconds.value = 0

        val intent = Intent(getApplication(), FocusService::class.java).apply {
            action = FocusService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }

    fun tickFocus() {
        if (!_focusPaused.value && _focusActive.value) {
            _focusSeconds.value++
            if (_focusSeconds.value >= _focusTotalSeconds.value) {
                if (_focusMode.value == "study") {
                    _focusSessions.value++
                    _focusMode.value = "break"
                    _focusTotalSeconds.value = prefs.focusBreakMinutes * 60
                } else {
                    _focusMode.value = "study"
                    _focusTotalSeconds.value = prefs.focusStudyMinutes * 60
                }
                _focusSeconds.value = 0
            }
        }
    }

    fun skipFocusPhase() {
        if (_focusMode.value == "study") {
            _focusSessions.value++
            _focusMode.value = "break"
            _focusTotalSeconds.value = prefs.focusBreakMinutes * 60
        } else {
            _focusMode.value = "study"
            _focusTotalSeconds.value = prefs.focusStudyMinutes * 60
        }
        _focusSeconds.value = 0
    }

    fun setFocusModeType(mode: String) {
        _focusMode.value = mode
        _focusTotalSeconds.value = if (mode == "study") prefs.focusStudyMinutes * 60 else prefs.focusBreakMinutes * 60
        _focusSeconds.value = 0
    }

    // ─── Settings ───
    fun updateWhatsapp(number: String) {
        prefs.whatsappNumber = number
    }

    fun updateFocusStudy(minutes: Int) {
        prefs.focusStudyMinutes = minutes
    }

    fun updateFocusBreak(minutes: Int) {
        prefs.focusBreakMinutes = minutes
    }

    fun updateDndEnabled(enabled: Boolean) {
        prefs.dndEnabled = enabled
    }

    fun updateAppBlocking(enabled: Boolean) {
        prefs.appBlockingEnabled = enabled
    }

    fun updateCalendarSync(enabled: Boolean) {
        prefs.calendarSyncEnabled = enabled
    }

    // ─── Calendar Navigation ───
    fun setCalendarView(view: String) { _calendarView.value = view }

    fun nextWeek() {
        _calendarWeekStart.value = _calendarWeekStart.value.plusWeeks(1)
    }

    fun prevWeek() {
        _calendarWeekStart.value = _calendarWeekStart.value.minusWeeks(1)
    }

    fun todayWeek() {
        _calendarWeekStart.value = getWeekStart(LocalDate.now())
    }

    fun nextMonth() {
        _calendarMonth.value = _calendarMonth.value.plusMonths(1)
    }

    fun prevMonth() {
        _calendarMonth.value = _calendarMonth.value.minusMonths(1)
    }

    // ─── WhatsApp ───
    fun getWhatsappIntent(message: String): Intent? {
        val number = prefs.whatsappNumber.replace(Regex("[^0-9]"), "")
        if (number.isBlank()) return null
        return Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("https://wa.me/$number?text=${android.net.Uri.encode(message)}")
        }
    }

    // ─── Utility ───
    private fun getWeekStart(date: LocalDate): LocalDate {
        return date.minusDays(date.dayOfWeek.value.toLong() % 7)
    }

    fun timeDiffMinutes(start: String, end: String): Int {
        return try {
            val s = LocalTime.parse(start)
            val e = LocalTime.parse(end)
            ChronoUnit.MINUTES.between(s, e).toInt()
        } catch (ex: Exception) { 0 }
    }

    fun formatTime12(time: String): String {
        return try {
            val t = LocalTime.parse(time)
            t.format(DateTimeFormatter.ofPattern("h:mm a"))
        } catch (ex: Exception) { time }
    }

    fun formatDateShort(date: String): String {
        return try {
            val d = LocalDate.parse(date)
            d.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
        } catch (ex: Exception) { date }
    }
}
