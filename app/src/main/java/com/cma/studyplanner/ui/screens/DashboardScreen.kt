@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cma.studyplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val revisions by vm.revisions.collectAsState()
    val todaySchedules = vm.getTodaySchedules()
    val total = vm.getTotalChapters()
    val done = vm.getDoneChapters()
    val pct = vm.getProgressPercent()
    val userName = vm.getUserName()
    val examName = vm.getExamName()
    val examDate = vm.getExamDate()
    var dashSection by remember { mutableStateOf("overview") }

    val hour = java.time.LocalTime.now().hour
    val greeting = when { hour < 12 -> "Good Morning"; hour < 17 -> "Good Afternoon"; else -> "Good Evening" }
    val daysLeft = try { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(examDate)).toInt().coerceAtLeast(0) } catch (_: Exception) { 0 }
    val weeksLeft = daysLeft / 7
    val remaining = total - done

    // Total study hours = schedule + revision completed hours
    val schedHrs = schedules.filter { it.completed }.sumOf { vm.timeDiffMinutes(it.startTime, it.endTime).coerceAtLeast(0) } / 60.0
    val revHrs = revisions.filter { it.completed }.sumOf { vm.timeDiffMinutes(it.startTime, it.endTime).coerceAtLeast(0) } / 60.0
    val studyHours = schedHrs + revHrs
    val hoursPerDay = if (daysLeft > 0 && remaining > 0) String.format("%.1f", remaining * 2.0 / daysLeft) else "0"

    // Subject-wise hours
    val subjectHours = subjects.map { swc ->
        val sH = schedules.filter { it.completed && it.subject == swc.subject.name }.sumOf { vm.timeDiffMinutes(it.startTime, it.endTime).coerceAtLeast(0) } / 60.0
        val rH = revisions.filter { it.completed && it.subjectName == swc.subject.name }.sumOf { vm.timeDiffMinutes(it.startTime, it.endTime).coerceAtLeast(0) } / 60.0
        swc.subject.name to (sH + rH)
    }

    // This week hours (Mon-Sun)
    val now = LocalDate.now()
    val weekStart = now.minusDays((now.dayOfWeek.value - 1).toLong()) // Monday
    val weekEnd = weekStart.plusDays(6) // Sunday
    val thisWeekHrs = (schedules.filter { it.completed && it.date >= weekStart.toString() && it.date <= weekEnd.toString() }.sumOf { vm.timeDiffMinutes(it.startTime, it.endTime).coerceAtLeast(0) } +
        revisions.filter { it.completed && it.date >= weekStart.toString() && it.date <= weekEnd.toString() }.sumOf { vm.timeDiffMinutes(it.startTime, it.endTime).coerceAtLeast(0) }) / 60.0

    // This month hours
    val monthStart = now.withDayOfMonth(1)
    val monthEnd = now.withDayOfMonth(now.lengthOfMonth())
    val thisMonthHrs = (schedules.filter { it.completed && it.date >= monthStart.toString() && it.date <= monthEnd.toString() }.sumOf { vm.timeDiffMinutes(it.startTime, it.endTime).coerceAtLeast(0) } +
        revisions.filter { it.completed && it.date >= monthStart.toString() && it.date <= monthEnd.toString() }.sumOf { vm.timeDiffMinutes(it.startTime, it.endTime).coerceAtLeast(0) }) / 60.0

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // Greeting
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))).padding(18.dp)) {
            Column {
                Text(greeting, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                Text("$userName \uD83D\uDC4B", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("\"Success is the sum of small efforts repeated daily.\"", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Toggle
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).padding(3.dp)) {
            listOf("overview" to "Overview", "progress" to "Progress").forEach { (key, label) ->
                Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (dashSection == key) Indigo else Color.Transparent).clickable { dashSection = key }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (dashSection == key) Color.White else Muted)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // ═══ OVERVIEW ═══
        if (dashSection == "overview") {
            // Countdown
            if (examName.isNotBlank() && daysLeft > 0) {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                            val rc = when { daysLeft < 30 -> Red; daysLeft < 90 -> Amber; else -> Indigo }
                            CircularProgressIndicator(progress = 1f, Modifier.fillMaxSize(), color = rc.copy(alpha = 0.12f), strokeWidth = 5.dp)
                            CircularProgressIndicator(progress = ((365f - daysLeft) / 365f).coerceIn(0f, 1f), Modifier.fillMaxSize(), color = rc, strokeWidth = 5.dp)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$daysLeft", fontSize = 18.sp, fontWeight = FontWeight.Black, color = rc); Text("DAYS", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Muted) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("\uD83C\uDFAF $examName", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            val ef = remember(examDate) { try { LocalDate.parse(examDate).format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy")) } catch (_: Exception) { examDate } }
                            Text(ef, fontSize = 11.sp, color = Muted)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column { Text("$weeksLeft", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Indigo); Text("weeks", fontSize = 9.sp, color = Muted) }
                                Column { Text(hoursPerDay, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Amber); Text("hrs/day", fontSize = 9.sp, color = Muted) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            // Stats
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(title = "Progress", value = "$pct%", icon = "\uD83D\uDCCA", modifier = Modifier.weight(1f))
                StatCard(title = "Today", value = "${todaySchedules.size}", icon = "\uD83D\uDCC5", modifier = Modifier.weight(1f))
                StatCard(title = "Hours", value = String.format("%.1f", studyHours), icon = "\u23F1\uFE0F", modifier = Modifier.weight(1f))
                StatCard(title = "Subjects", value = "${subjects.size}", icon = "\uD83D\uDCDA", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            // Today's sessions
            Text("Today's Sessions", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (todaySchedules.isEmpty()) { EmptyState("\uD83D\uDCED", "No sessions today") }
            else { todaySchedules.forEach { sch ->
                val si = subjects.indexOfFirst { it.subject.name == sch.subject }
                val ci = if (si >= 0) subjects[si].subject.colorIndex else 0
                ScheduleItem(sch.subject, sch.chapter, "${vm.formatTime12(sch.startTime)} - ${vm.formatTime12(sch.endTime)}", sch.completed, Color(vm.colors[ci % vm.colors.size]), Color(vm.lightColors[ci % vm.lightColors.size]),
                    onToggle = { vm.toggleScheduleComplete(sch) }, onDelete = { vm.deleteSchedule(sch.id) }, onEdit = { })
                Spacer(Modifier.height(5.dp))
            }}
        }

        // ═══ PROGRESS ═══
        if (dashSection == "progress") {
            // Overall
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Overall Progress", fontSize = 13.sp, fontWeight = FontWeight.Bold); Text("$pct%", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Indigo) }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = pct / 100f, Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)), color = Indigo, trackColor = IndigoLight)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("$done done", fontSize = 9.sp, color = Muted); Text("$remaining left", fontSize = 9.sp, color = Muted) }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Study Hours Summary — Week / Month / Total
            Text("Study Hours", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HoursCard("This Week", thisWeekHrs, Indigo, Modifier.weight(1f))
                HoursCard("This Month", thisMonthHrs, Amber, Modifier.weight(1f))
                HoursCard("All Time", studyHours, Green, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))

            // Weekly Plan
            Text("Weekly Plan", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            val weeklyPlanned = vm.getWeeklyPlannedHours()
            val deficit = (weeklyPlanned - thisWeekHrs.toFloat()).coerceAtLeast(0f)
            val nextWeek = weeklyPlanned + deficit
            val examTotalHrs = vm.getExamTotalHours()
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("This Week", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${String.format("%.1f", thisWeekHrs)} / ${String.format("%.1f", weeklyPlanned)} hrs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (thisWeekHrs >= weeklyPlanned) Green else Amber)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = if (weeklyPlanned > 0) (thisWeekHrs.toFloat() / weeklyPlanned).coerceIn(0f, 1f) else 0f, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = if (thisWeekHrs >= weeklyPlanned) Green else Amber, trackColor = Border)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(if (deficit > 0) "-${String.format("%.1f", deficit)}" else "0", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (deficit > 0) Red else Green); Text("deficit", fontSize = 9.sp, color = Muted) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(String.format("%.1f", nextWeek), fontSize = 14.sp, fontWeight = FontWeight.Black, color = Indigo); Text("next week", fontSize = 9.sp, color = Muted) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(String.format("%.0f", examTotalHrs), fontSize = 14.sp, fontWeight = FontWeight.Black, color = Amber); Text("total hrs", fontSize = 9.sp, color = Muted) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Subject-wise Hours
            Text("Subject-wise Hours", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            subjects.forEachIndexed { i, swc ->
                val hrs = subjectHours.find { it.first == swc.subject.name }?.second ?: 0.0
                val d = swc.chapters.count { it.done }; val t = swc.chapters.size
                val p = if (t > 0) d.toFloat() / t else 0f
                val color = Color(vm.colors[swc.subject.colorIndex % vm.colors.size])
                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(color))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(swc.subject.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(progress = p, Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = color, trackColor = color.copy(alpha = 0.12f))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(String.format("%.1fh", hrs), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
                            Text("$d/$t ch", fontSize = 10.sp, color = Muted)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun HoursCard(title: String, hours: Double, color: Color, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(String.format("%.1f", hours), fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
            Text("hours", fontSize = 9.sp, color = Muted)
            Spacer(Modifier.height(2.dp))
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Muted)
        }
    }
}
