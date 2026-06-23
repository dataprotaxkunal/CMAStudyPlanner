package com.cma.studyplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cma.studyplanner.ui.components.*
import com.cma.studyplanner.ui.theme.*
import com.cma.studyplanner.viewmodel.MainViewModel

@Composable
fun DashboardScreen(vm: MainViewModel) {
    val subjects by vm.subjectsWithChapters.collectAsState()
    val schedules by vm.schedules.collectAsState()
    val todaySchedules = vm.getTodaySchedules()
    val total = vm.getTotalChapters()
    val done = vm.getDoneChapters()
    val pct = vm.getProgressPercent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header
        Text("Dashboard", fontSize = 26.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Your CMA study overview",
            color = Muted,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Stats Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Completion",
                value = "$pct%",
                subtitle = "$done / $total chapters",
                icon = "📊",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Today",
                value = "${todaySchedules.size}",
                subtitle = "sessions",
                icon = "📅",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val studyHours = schedules.filter { it.completed }
                .sumOf { vm.timeDiffMinutes(it.startTime, it.endTime).coerceAtLeast(0) } / 60.0
            StatCard(
                title = "Study Hours",
                value = String.format("%.1f", studyHours),
                subtitle = "completed",
                icon = "⏱️",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Subjects",
                value = "${subjects.size}",
                subtitle = "active",
                icon = "📚",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Today's Schedule
        SectionHeader(title = "Today's Sessions")
        Spacer(modifier = Modifier.height(12.dp))

        if (todaySchedules.isEmpty()) {
            EmptyState("📅", "No sessions today", "Schedule a study session")
        } else {
            todaySchedules.forEach { sch ->
                val subjIdx = subjects.indexOfFirst { it.subject.name == sch.subject }
                val colorIdx = if (subjIdx >= 0) subjects[subjIdx].subject.colorIndex else 0
                val color = Color(vm.colors[colorIdx % vm.colors.size])
                val lightColor = Color(vm.lightColors[colorIdx % vm.lightColors.size])

                ScheduleItem(
                    subject = sch.subject,
                    chapter = sch.chapter,
                    time = "${vm.formatTime12(sch.startTime)} – ${vm.formatTime12(sch.endTime)}",
                    completed = sch.completed,
                    color = color,
                    lightColor = lightColor,
                    onToggle = { vm.toggleScheduleComplete(sch) },
                    onDelete = { vm.deleteSchedule(sch.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Subject Progress
        SectionHeader(title = "Subject Progress")
        Spacer(modifier = Modifier.height(12.dp))

        if (subjects.isEmpty()) {
            EmptyState("📚", "No subjects yet", "Add your first subject")
        } else {
            subjects.forEach { swc ->
                val totalCh = swc.chapters.size
                val doneCh = swc.chapters.count { it.done }
                val progress = if (totalCh > 0) doneCh.toFloat() / totalCh else 0f
                val color = Color(vm.colors[swc.subject.colorIndex % vm.colors.size])

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BgLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProgressRing(progress = progress, color = color)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                swc.subject.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$doneCh / $totalCh chapters",
                                fontSize = 12.sp,
                                color = Muted
                            )
                        }
                        // Progress bar
                        Box(modifier = Modifier.width(80.dp)) {
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = color,
                                trackColor = color.copy(alpha = 0.15f),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Bottom padding for nav bar
    }
}
