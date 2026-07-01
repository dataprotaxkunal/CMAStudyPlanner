@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cma.studyplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cma.studyplanner.data.Schedule
import com.cma.studyplanner.ui.theme.*
import com.cma.studyplanner.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(vm: MainViewModel) {
    val calView by vm.calendarView.collectAsState()
    val weekStart by vm.calendarWeekStart.collectAsState()
    val monthStart by vm.calendarMonth.collectAsState()
    val schedules by vm.schedules.collectAsState()
    val revisions by vm.revisions.collectAsState()
    val subjects by vm.subjectsWithChapters.collectAsState()
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Calendar", fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(
                    if (calView == "week") weekStart.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    else monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    color = Muted, fontSize = 14.sp
                )
            }
            // View toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgLight)
                    .padding(2.dp)
            ) {
                listOf("week" to "Week", "month" to "Month").forEach { (key, label) ->
                    Text(
                        label,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (calView == key) Modifier.background(CardLight) else Modifier
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = if (calView == key) FontWeight.Bold else FontWeight.Medium,
                        color = if (calView == key) TextDark else Muted
                    ).also {
                        // Make clickable through a wrapper — simplified approach
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { if (calView == "week") vm.prevWeek() else vm.prevMonth() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                }
                TextButton(onClick = {
                    vm.todayWeek()
                    vm.setCalendarView("week")
                }) {
                    Text("Today", fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = { if (calView == "week") vm.nextWeek() else vm.nextMonth() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = calView == "week",
                    onClick = { vm.setCalendarView("week") },
                    label = { Text("Week", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = calView == "month",
                    onClick = { vm.setCalendarView("month") },
                    label = { Text("Month", fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (calView == "week") {
            WeekView(vm, weekStart, schedules, subjects, today)
        } else {
            MonthView(vm, monthStart, schedules, revisions, subjects, today)
        }
    }
}

@Composable
fun WeekView(
    vm: MainViewModel,
    weekStart: LocalDate,
    schedules: List<Schedule>,
    subjects: List<com.cma.studyplanner.data.SubjectWithChapters>,
    today: LocalDate
) {
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }

    // Day headers
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(40.dp))
        days.forEach { day ->
            val isToday = day == today
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isToday) Indigo else Muted
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .then(
                            if (isToday) Modifier
                                .clip(CircleShape)
                                .background(Indigo)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${day.dayOfMonth}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) Color.White else TextDark
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Divider(color = Border)

    // Time slots
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        (6..22).forEach { hour ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                // Time label
                Text(
                    "${if (hour % 12 == 0) 12 else hour % 12} ${if (hour < 12) "AM" else "PM"}",
                    modifier = Modifier.width(40.dp),
                    fontSize = 9.sp,
                    color = Muted,
                    textAlign = TextAlign.End
                )

                // Day columns
                days.forEach { day ->
                    val dateStr = day.toString()
                    val daySchedules = schedules.filter {
                        it.date == dateStr && it.startTime.substringBefore(":").toIntOrNull() == hour
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.5.dp, Border.copy(alpha = 0.3f))
                            .padding(1.dp)
                    ) {
                        daySchedules.forEach { sch ->
                            val subjIdx = subjects.indexOfFirst { it.subject.name == sch.subject }
                            val colorIdx = if (subjIdx >= 0) subjects[subjIdx].subject.colorIndex else 0
                            val color = Color(vm.colors[colorIdx % vm.colors.size])

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color.copy(alpha = 0.15f))
                                    .padding(2.dp)
                            ) {
                                Text(
                                    sch.subject.take(8),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = color,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun MonthView(
    vm: MainViewModel,
    monthStart: LocalDate,
    schedules: List<Schedule>,
    revisions: List<com.cma.studyplanner.data.Revision>,
    subjects: List<com.cma.studyplanner.data.SubjectWithChapters>,
    today: LocalDate
) {
    val daysInMonth = monthStart.lengthOfMonth()
    val firstDayOfWeek = monthStart.dayOfWeek.value % 7 // 0=Sun
    val totalCells = firstDayOfWeek + daysInMonth
    val rows = (totalCells + 6) / 7

    // Headers
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach {
            Text(
                it,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Muted
            )
        }
    }

    Divider(color = Border, modifier = Modifier.padding(vertical = 4.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIdx = row * 7 + col
                    val dayNum = cellIdx - firstDayOfWeek + 1

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 72.dp)
                            .border(0.5.dp, Border.copy(alpha = 0.2f))
                            .padding(2.dp)
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val date = monthStart.withDayOfMonth(dayNum)
                            val dateStr = date.toString()
                            val isToday = date == today
                            val dayScheds = schedules.filter { it.date == dateStr }
                            val dayRevs = revisions.filter { it.date == dateStr }

                            Column {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .then(
                                            if (isToday) Modifier
                                                .clip(CircleShape)
                                                .background(Indigo)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$dayNum",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isToday) Color.White else TextDark
                                    )
                                }

                                dayScheds.take(2).forEach { sch ->
                                    val sIdx = subjects.indexOfFirst { it.subject.name == sch.subject }
                                    val cIdx = if (sIdx >= 0) subjects[sIdx].subject.colorIndex else 0
                                    val color = Color(vm.colors[cIdx % vm.colors.size])

                                    Text(
                                        sch.subject.take(6),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = color,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(color.copy(alpha = 0.1f))
                                            .padding(horizontal = 2.dp)
                                    )
                                }

                                dayRevs.take(1).forEach {
                                    Text(
                                        "📖 Rev",
                                        fontSize = 8.sp,
                                        color = Amber,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Amber.copy(alpha = 0.1f))
                                            .padding(horizontal = 2.dp)
                                    )
                                }

                                val moreCount = (dayScheds.size - 2).coerceAtLeast(0) + (dayRevs.size - 1).coerceAtLeast(0)
                                if (moreCount > 0) {
                                    Text("+$moreCount more", fontSize = 7.sp, color = Muted)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
