@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cma.studyplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cma.studyplanner.ui.components.*
import com.cma.studyplanner.ui.theme.*
import com.cma.studyplanner.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(vm: MainViewModel) {
    val schedules by vm.schedules.collectAsState()
    val subjects by vm.subjectsWithChapters.collectAsState()
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }

    // Generate 30 days: 7 before today + 23 after
    val dates = remember(today) {
        (-7..22).map { today.plusDays(it.toLong()) }
    }

    val selectedStr = selectedDate.toString()
    val selectedScheds = schedules
        .filter { it.date == selectedStr }
        .sortedBy { it.startTime }

    Column(modifier = Modifier.fillMaxSize()) {
        // Indigo header with slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF6366F1), Color(0xFF5558E6))
                    ),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .padding(top = 20.dp, bottom = 8.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Calendar",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Horizontal date slider
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dates.forEach { date ->
                        val dateStr = date.toString()
                        val isToday = date == today
                        val isSelected = date == selectedDate
                        val hasEvents = schedules.any { it.date == dateStr }
                        val isSunday = date.dayOfWeek.value == 7

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isSelected -> Color.White
                                        isToday -> Color.White.copy(alpha = 0.15f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { selectedDate = date }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Day name
                            Text(
                                date.format(DateTimeFormatter.ofPattern("EEE")).take(3).uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isSelected -> Muted
                                    isSunday -> Color(0xFFFCA5A5)
                                    else -> Color.White.copy(alpha = 0.5f)
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Date number
                            Text(
                                "${date.dayOfMonth}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Indigo else Color.White
                            )
                            // Event dot
                            if (hasEvents) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Indigo else Color(0xFFFBBF24)
                                        )
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // Selected date + sessions below
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Date header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${selectedScheds.size} session${if (selectedScheds.size != 1) "s" else ""}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Indigo,
                    modifier = Modifier
                        .background(IndigoLight, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sessions list
            if (selectedScheds.isEmpty()) {
                EmptyState("📭", "No sessions", "Schedule a study session for this day")
            } else {
                selectedScheds.forEach { sch ->
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

            // Quick stats
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Pending",
                    value = "${selectedScheds.count { !it.completed }}",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Done",
                    value = "${selectedScheds.count { it.completed }}",
                    modifier = Modifier.weight(1f)
                )
                val totalMins = selectedScheds.sumOf {
                    vm.timeDiffMinutes(it.startTime, it.endTime).coerceAtLeast(0)
                }
                StatCard(
                    title = "Total",
                    value = "${String.format("%.1f", totalMins / 60.0)}h",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
