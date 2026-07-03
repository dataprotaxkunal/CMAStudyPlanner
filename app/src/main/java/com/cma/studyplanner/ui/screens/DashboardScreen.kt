@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cma.studyplanner.ui.components.*
import com.cma.studyplanner.ui.theme.*
import com.cma.studyplanner.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun DashboardScreen(vm: MainViewModel) {
    val subjects by vm.subjectsWithChapters.collectAsState()
    val schedules by vm.schedules.collectAsState()
    val todaySchedules = vm.getTodaySchedules()
    val total = vm.getTotalChapters()
    val done = vm.getDoneChapters()
    val pct = vm.getProgressPercent()

    val userName = vm.getUserName()
    val examName = vm.getExamName()
    val examDate = vm.getExamDate()

    val hour = java.time.LocalTime.now().hour
    val greeting = when { hour < 12 -> "Good Morning"; hour < 17 -> "Good Afternoon"; else -> "Good Evening" }

    val daysLeft = try {
        val ed = LocalDate.parse(examDate)
        ChronoUnit.DAYS.between(LocalDate.now(), ed).toInt().coerceAtLeast(0)
    } catch (e: Exception) { 0 }
    val weeksLeft = daysLeft / 7
    val remaining = total - done
    val hoursPerDay = if (daysLeft > 0 && remaining > 0) String.format("%.1f", remaining * 2.0 / daysLeft) else "0"

    val studyHours = schedules.filter { it.completed }
        .sumOf { vm.timeDiffMinutes(it.startTime, it.endTime).coerceAtLeast(0) } / 60.0

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        // Greeting banner
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))))
                .padding(18.dp)
        ) {
            Column {
                Text(greeting, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                Text("$userName \uD83D\uDC4B", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("\"Success is the sum of small efforts repeated daily.\"", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Exam countdown
        if (examName.isNotBlank() && daysLeft > 0) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Countdown ring
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                        val ringColor = when { daysLeft < 30 -> Red; daysLeft < 90 -> Amber; else -> Indigo }
                        CircularProgressIndicator(
                            progress = 1f, modifier = Modifier.fillMaxSize(),
                            color = ringColor.copy(alpha = 0.12f), strokeWidth = 5.dp
                        )
                        CircularProgressIndicator(
                            progress = ((365f - daysLeft) / 365f).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxSize(),
                            color = ringColor, strokeWidth = 5.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$daysLeft", fontSize = 18.sp, fontWeight = FontWeight.Black, color = ringColor)
                            Text("DAYS", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Muted)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("\uD83C\uDFAF $examName", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val examDateFormatted = remember(examDate) {
                            try {
                                LocalDate.parse(examDate).format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMM yyyy"))
                            } catch (_: Exception) { examDate }
                        }
                        Text(examDateFormatted, fontSize = 11.sp, color = Muted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                Text("$weeksLeft", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Indigo)
                                Text("weeks", fontSize = 9.sp, color = Muted)
                            }
                            Column {
                                Text(hoursPerDay, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Amber)
                                Text("hrs/day", fontSize = 9.sp, color = Muted)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Compact stats row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(title = "Progress", value = "$pct%", icon = "\uD83D\uDCCA", modifier = Modifier.weight(1f))
            StatCard(title = "Today", value = "${todaySchedules.size}", icon = "\uD83D\uDCC5", modifier = Modifier.weight(1f))
            StatCard(title = "Hours", value = String.format("%.1f", studyHours), icon = "\u23F1\uFE0F", modifier = Modifier.weight(1f))
            StatCard(title = "Subjects", value = "${subjects.size}", icon = "\uD83D\uDCDA", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Overall progress bar
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Overall Progress", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("$pct%", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Indigo)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = pct / 100f, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                    color = Indigo, trackColor = IndigoLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$done done", fontSize = 9.sp, color = Muted)
                    Text("$remaining left", fontSize = 9.sp, color = Muted)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Today's sessions
        Text("Today's Sessions", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (todaySchedules.isEmpty()) {
            EmptyState("\uD83D\uDCED", "No sessions today")
        } else {
            todaySchedules.forEach { sch ->
                val subjIdx = subjects.indexOfFirst { it.subject.name == sch.subject }
                val colorIdx = if (subjIdx >= 0) subjects[subjIdx].subject.colorIndex else 0
                val color = Color(vm.colors[colorIdx % vm.colors.size])
                val lightColor = Color(vm.lightColors[colorIdx % vm.lightColors.size])
                ScheduleItem(
                    subject = sch.subject, chapter = sch.chapter,
                    time = "${vm.formatTime12(sch.startTime)} - ${vm.formatTime12(sch.endTime)}",
                    completed = sch.completed, color = color, lightColor = lightColor,
                    onToggle = { vm.toggleScheduleComplete(sch) },
                    onDelete = { vm.deleteSchedule(sch.id) }
                )
                Spacer(modifier = Modifier.height(5.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Subject progress
        Text("Subject Progress", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        subjects.forEach { swc ->
            val d = swc.chapters.count { it.done }; val t = swc.chapters.size
            val p = if (t > 0) d.toFloat() / t else 0f
            val color = Color(vm.colors[swc.subject.colorIndex % vm.colors.size])
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(4.dp).height(28.dp).clip(RoundedCornerShape(2.dp)).background(color))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(swc.subject.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = p, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = color, trackColor = color.copy(alpha = 0.12f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("$d/$t", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
