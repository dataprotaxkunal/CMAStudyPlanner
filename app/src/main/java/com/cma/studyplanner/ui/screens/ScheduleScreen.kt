@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cma.studyplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cma.studyplanner.data.Schedule
import com.cma.studyplanner.ui.components.*
import com.cma.studyplanner.ui.theme.*
import com.cma.studyplanner.viewmodel.MainViewModel
import java.time.LocalDate
import java.util.Calendar

@Composable
fun ScheduleScreen(vm: MainViewModel) {
    val subjects by vm.subjectsWithChapters.collectAsState()
    val schedules by vm.schedules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }
    var filterMode by remember { mutableStateOf("all") }

    val today = LocalDate.now().toString()
    val filtered = when (filterMode) {
        "today" -> schedules.filter { it.date == today }
        "upcoming" -> schedules.filter { it.date >= today && !it.completed }
        else -> schedules
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Schedule", fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("Plan your study sessions", color = Muted, fontSize = 14.sp)
            }
            Button(onClick = { editingSchedule = null; showAddDialog = true }, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "All", "today" to "Today", "upcoming" to "Upcoming").forEach { (key, label) ->
                FilterChip(selected = filterMode == key, onClick = { filterMode = key }, label = { Text(label, fontSize = 13.sp) }, shape = RoundedCornerShape(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filtered.isEmpty()) {
            EmptyState("\uD83D\uDCC5", "No sessions found", "Create a study session to get started")
        } else {
            val grouped = filtered.groupBy { it.date }.toSortedMap()
            grouped.forEach { (date, items) ->
                Text(vm.formatDateShort(date), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (date == today) Indigo else TextDark, modifier = Modifier.padding(vertical = 8.dp))
                items.forEach { sch ->
                    val subjIdx = subjects.indexOfFirst { it.subject.name == sch.subject }
                    val colorIdx = if (subjIdx >= 0) subjects[subjIdx].subject.colorIndex else 0
                    val color = Color(vm.colors[colorIdx % vm.colors.size])
                    val lightColor = Color(vm.lightColors[colorIdx % vm.lightColors.size])
                    ScheduleItem(
                        subject = sch.subject, chapter = sch.chapter,
                        time = "${vm.formatTime12(sch.startTime)} - ${vm.formatTime12(sch.endTime)}",
                        completed = sch.completed, color = color, lightColor = lightColor,
                        onToggle = { vm.toggleScheduleComplete(sch) },
                        onDelete = { vm.deleteSchedule(sch.id) },
                        onEdit = { editingSchedule = sch; showAddDialog = true }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }

    // Add OR Edit dialog
    if (showAddDialog) {
        AddEditScheduleDialog(
            subjects = subjects.map { it.subject.name to it.chapters.map { c -> c.name } },
            existingSchedule = editingSchedule,
            onConfirm = { subj, ch, date, st, et ->
                if (editingSchedule != null) {
                    vm.editSchedule(editingSchedule!!.id, subj, ch, date, st, et)
                } else {
                    vm.addScheduleWithAlarm(subj, ch, date, st, et)
                }
                showAddDialog = false
                editingSchedule = null
            },
            onDismiss = { showAddDialog = false; editingSchedule = null }
        )
    }
}

@Composable
fun AddEditScheduleDialog(
    subjects: List<Pair<String, List<String>>>,
    existingSchedule: Schedule?,
    onConfirm: (String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val isEdit = existingSchedule != null

    var selectedSubject by remember { mutableStateOf(existingSchedule?.subject ?: "") }
    var selectedChapter by remember { mutableStateOf(existingSchedule?.chapter ?: "") }
    var subjectExpanded by remember { mutableStateOf(false) }
    var chapterExpanded by remember { mutableStateOf(false) }

    // Date
    var selectedDateMillis by remember {
        mutableStateOf(
            if (existingSchedule != null) {
                try {
                    val parts = existingSchedule.date.split("-").map { it.toInt() }
                    Calendar.getInstance().apply { set(parts[0], parts[1] - 1, parts[2]) }.timeInMillis
                } catch (e: Exception) { System.currentTimeMillis() }
            } else System.currentTimeMillis()
        )
    }
    val selectedDate = remember(selectedDateMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }
    val displayDate = remember(selectedDate) {
        try { LocalDate.parse(selectedDate).format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy")) } catch (e: Exception) { selectedDate }
    }
    var showDatePicker by remember { mutableStateOf(false) }

    // Times - parse from existing or default
    var startHour by remember { mutableStateOf(existingSchedule?.startTime?.substringBefore(":")?.toIntOrNull() ?: 9) }
    var startMinute by remember { mutableStateOf(existingSchedule?.startTime?.substringAfter(":")?.toIntOrNull() ?: 0) }
    var endHour by remember { mutableStateOf(existingSchedule?.endTime?.substringBefore(":")?.toIntOrNull() ?: 10) }
    var endMinute by remember { mutableStateOf(existingSchedule?.endTime?.substringAfter(":")?.toIntOrNull() ?: 0) }
    var showStartTime by remember { mutableStateOf(false) }
    var showEndTime by remember { mutableStateOf(false) }

    val startTimeStr = String.format("%02d:%02d", startHour, startMinute)
    val endTimeStr = String.format("%02d:%02d", endHour, endMinute)
    val startDisplay = formatAmPm(startHour, startMinute)
    val endDisplay = formatAmPm(endHour, endMinute)

    val chapters = subjects.find { it.first == selectedSubject }?.second ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Schedule" else "Schedule Session", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Subject
                ExposedDropdownMenuBox(expanded = subjectExpanded, onExpandedChange = { subjectExpanded = it }) {
                    OutlinedTextField(value = selectedSubject, onValueChange = {}, readOnly = true, label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    ExposedDropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                        subjects.forEach { (name, _) -> DropdownMenuItem(text = { Text(name) }, onClick = { selectedSubject = name; selectedChapter = ""; subjectExpanded = false }) }
                    }
                }
                // Chapter
                ExposedDropdownMenuBox(expanded = chapterExpanded, onExpandedChange = { chapterExpanded = it }) {
                    OutlinedTextField(value = selectedChapter, onValueChange = {}, readOnly = true, label = { Text("Chapter") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    ExposedDropdownMenu(expanded = chapterExpanded, onDismissRequest = { chapterExpanded = false }) {
                        chapters.forEach { ch -> DropdownMenuItem(text = { Text(ch) }, onClick = { selectedChapter = ch; chapterExpanded = false }) }
                    }
                }
                // Date
                OutlinedTextField(value = displayDate, onValueChange = {}, readOnly = true, label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth().clickableNoRipple { showDatePicker = true },
                    shape = RoundedCornerShape(10.dp), leadingIcon = { Text("\uD83D\uDCC5", fontSize = 16.sp) })
                // Start time
                OutlinedTextField(value = startDisplay, onValueChange = {}, readOnly = true, label = { Text("Start Time") },
                    modifier = Modifier.fillMaxWidth().clickableNoRipple { showStartTime = true },
                    shape = RoundedCornerShape(10.dp), leadingIcon = { Text("\uD83D\uDD50", fontSize = 16.sp) })
                // End time
                OutlinedTextField(value = endDisplay, onValueChange = {}, readOnly = true, label = { Text("End Time") },
                    modifier = Modifier.fillMaxWidth().clickableNoRipple { showEndTime = true },
                    shape = RoundedCornerShape(10.dp), leadingIcon = { Text("\uD83D\uDD50", fontSize = 16.sp) })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedSubject.isNotBlank() && selectedChapter.isNotBlank()) {
                    onConfirm(selectedSubject, selectedChapter, selectedDate, startTimeStr, endTimeStr)
                }
            }) { Text(if (isEdit) "Update" else "Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    // Date Picker
    if (showDatePicker) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { selectedDateMillis = it }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dps) }
    }
    // Start Time Picker
    if (showStartTime) {
        val tps = rememberTimePickerState(initialHour = startHour, initialMinute = startMinute, is24Hour = false)
        AlertDialog(onDismissRequest = { showStartTime = false },
            title = { Text("Start Time", fontWeight = FontWeight.Bold) },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = tps) } },
            confirmButton = { Button(onClick = { startHour = tps.hour; startMinute = tps.minute; showStartTime = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showStartTime = false }) { Text("Cancel") } })
    }
    // End Time Picker
    if (showEndTime) {
        val tps = rememberTimePickerState(initialHour = endHour, initialMinute = endMinute, is24Hour = false)
        AlertDialog(onDismissRequest = { showEndTime = false },
            title = { Text("End Time", fontWeight = FontWeight.Bold) },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = tps) } },
            confirmButton = { Button(onClick = { endHour = tps.hour; endMinute = tps.minute; showEndTime = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showEndTime = false }) { Text("Cancel") } })
    }
}

fun formatAmPm(hour: Int, minute: Int): String {
    val h = if (hour % 12 == 0) 12 else hour % 12
    val ampm = if (hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", h, minute, ampm)
}

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(), indication = null, onClick = onClick))
