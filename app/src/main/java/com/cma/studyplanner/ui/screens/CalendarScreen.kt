@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cma.studyplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cma.studyplanner.data.Schedule
import com.cma.studyplanner.ui.components.*
import com.cma.studyplanner.ui.theme.*
import com.cma.studyplanner.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(vm: MainViewModel) {
    val monthStart by vm.calendarMonth.collectAsState()
    val schedules by vm.schedules.collectAsState()
    val subjects by vm.subjectsWithChapters.collectAsState()
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Header
        Text("Calendar", fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text("Tap any date to see sessions", color = Muted, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(20.dp))

        // Month navigator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { vm.prevMonth() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BgLight)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
            }
            Text(
                monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { vm.nextMonth() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BgLight)
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Day-of-week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Muted
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid
        val daysInMonth = monthStart.lengthOfMonth()
        val firstDayOfWeek = monthStart.dayOfWeek.value % 7 // Sunday = 0
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIdx = row * 7 + col
                    val dayNum = cellIdx - firstDayOfWeek + 1

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val date = monthStart.withDayOfMonth(dayNum)
                            val dateStr = date.toString()
                            val isToday = date == today
                            val isSelected = date == selectedDate
                            val hasEvents = schedules.any { it.date == dateStr }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> Indigo
                                            isToday -> IndigoLight
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { selectedDate = date },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "$dayNum",
                                        fontSize = 15.sp,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            isSelected -> Color.White
                                            isToday -> Indigo
                                            else -> TextDark
                                        }
                                    )
                                    if (hasEvents) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) Color.White else Indigo
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Selected date sessions
        Divider(color = Border)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        val selectedDateSchedules = schedules.filter { it.date == selectedDate.toString() }
            .sortedBy { it.startTime }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (selectedDateSchedules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No sessions", color = Muted, fontSize = 13.sp)
                    }
                }
            } else {
                selectedDateSchedules.forEach { sch ->
                    val subjIdx = subjects.indexOfFirst { it.subject.name == sch.subject }
                    val colorIdx = if (subjIdx >= 0) subjects[subjIdx].subject.colorIndex else 0
                    val color = Color(vm.colors[colorIdx % vm.colors.size])
                    val lightColor = Color(vm.lightColors[colorIdx % vm.lightColors.size])

                    ScheduleItem(
                        subject = sch.subject,
                        chapter = sch.chapter,
                        time = "${vm.formatTime12(sch.startTime)} - ${vm.formatTime12(sch.endTime)}",
                        completed = sch.completed,
                        color = color,
                        lightColor = lightColor,
                        onToggle = { vm.toggleScheduleComplete(sch) },
                        onDelete = { vm.deleteSchedule(sch.id) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
