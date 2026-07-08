@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cma.studyplanner.ui.screens

import androidx.compose.foundation.clickable
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
    var addToRevision by remember { mutableStateOf<Schedule?>(null) }

    val today = LocalDate.now().toString()
    val filtered = when (filterMode) {
        "today" -> schedules.filter { it.date == today }
        "upcoming" -> schedules.filter { it.date >= today && !it.completed }
        else -> schedules
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
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
                Text(vm.formatDateShort(date), fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = if (date == today) Indigo else if (date < today) Red else TextDark,
                    modifier = Modifier.padding(vertical = 8.dp))
                items.forEach { sch ->
                    val subjIdx = subjects.indexOfFirst { it.subject.name == sch.subject }
                    val colorIdx = if (subjIdx >= 0) subjects[subjIdx].subject.colorIndex else 0
                    val color = Color(vm.colors[colorIdx % vm.colors.size])
                    val lightColor = Color(vm.lightColors[colorIdx % vm.lightColors.size])

                    Column {
                        ScheduleItem(
                            subject = sch.subject, chapter = sch.chapter,
                            time = "${vm.formatTime12(sch.startTime)} - ${vm.formatTime12(sch.endTime)}",
                            completed = sch.completed, color = color, lightColor = lightColor,
                            onToggle = {
                                vm.toggleScheduleComplete(sch)
                                // If marking as complete, offer to add to revision
                                if (!sch.completed) addToRevision = sch
                            },
                            onDelete = { vm.deleteSchedule(sch.id) },
                            onEdit = { editingSchedule = sch; showAddDialog = true }
                        )
                        // Show "Add to Revision" button when completed
                        if (sch.completed) {
                            TextButton(
                                onClick = { addToRevision = sch },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("📖 Add to Revision", fontSize = 11.sp, color = Indigo)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }

    // Add/Edit dialog
    if (showAddDialog) {
        AddEditScheduleDialog(
            subjects = subjects.map { it.subject.name to it.chapters.map { c -> c.name } },
            existingSchedule = editingSchedule,
            onConfirm = { subj, ch, date, st, et ->
                if (editingSchedule != null) vm.editSchedule(editingSchedule!!.id, subj, ch, date, st, et)
                else vm.addScheduleWithAlarm(subj, ch, date, st, et)
                showAddDialog = false; editingSchedule = null
            },
            onDismiss = { showAddDialog = false; editingSchedule = null }
        )
    }

    // Add to Revision dialog
    addToRevision?.let { sch ->
        AddToRevisionDialog(
            schedule = sch,
            subjects = subjects,
            onConfirm = { subjectId, chapterId, date, startTime, endTime, notes ->
                vm.addRevision(subjectId, chapterId, date, startTime, endTime, notes)
                addToRevision = null
            },
            onDismiss = { addToRevision = null }
        )
    }
}

@Composable
fun AddToRevisionDialog(
    schedule: Schedule,
    subjects: List<com.cma.studyplanner.data.SubjectWithChapters>,
    onConfirm: (String, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val swc = subjects.find { it.subject.name == schedule.subject }
    val chapter = swc?.chapters?.find { it.name == schedule.chapter }
    var notes by remember { mutableStateOf("") }
    var revDate by remember { mutableStateOf(schedule.date) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📖 Add to Revision", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Subject: ${schedule.subject}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Chapter: ${schedule.chapter}", fontSize = 12.sp, color = Muted)
                OutlinedTextField(value = revDate, onValueChange = { revDate = it }, label = { Text("Revision Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, placeholder = { Text("What to focus on...") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (swc != null && chapter != null) {
                    onConfirm(swc.subject.id, chapter.id, revDate, "09:00", "10:00", notes)
                }
                onDismiss()
            }) { Text("Add to Revision") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Skip") } }
    )
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

    var selectedDateMillis by remember {
        mutableStateOf(if (existingSchedule != null) {
            try { val p = existingSchedule.date.split("-").map { it.toInt() }; Calendar.getInstance().apply { set(p[0], p[1]-1, p[2]) }.timeInMillis } catch (_: Exception) { System.currentTimeMillis() }
        } else System.currentTimeMillis())
    }
    val selectedDate = remember(selectedDateMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH))
    }
    val displayDate = remember(selectedDate) { try { LocalDate.parse(selectedDate).format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy")) } catch (_: Exception) { selectedDate } }
    var showDatePicker by remember { mutableStateOf(false) }

    var startHour by remember { mutableStateOf(existingSchedule?.startTime?.substringBefore(":")?.toIntOrNull() ?: 9) }
    var startMinute by remember { mutableStateOf(existingSchedule?.startTime?.substringAfter(":")?.toIntOrNull() ?: 0) }
    var endHour by remember { mutableStateOf(existingSchedule?.endTime?.substringBefore(":")?.toIntOrNull() ?: 10) }
    var endMinute by remember { mutableStateOf(existingSchedule?.endTime?.substringAfter(":")?.toIntOrNull() ?: 0) }
    var showStartTime by remember { mutableStateOf(false) }
    var showEndTime by remember { mutableStateOf(false) }

    val chapters = subjects.find { it.first == selectedSubject }?.second ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Schedule" else "Schedule Session", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = subjectExpanded, onExpandedChange = { subjectExpanded = it }) {
                    OutlinedTextField(value = selectedSubject, onValueChange = {}, readOnly = true, label = { Text("Subject") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    ExposedDropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                        subjects.forEach { (name, _) -> DropdownMenuItem(text = { Text(name) }, onClick = { selectedSubject = name; selectedChapter = ""; subjectExpanded = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = chapterExpanded, onExpandedChange = { chapterExpanded = it }) {
                    OutlinedTextField(value = selectedChapter, onValueChange = {}, readOnly = true, label = { Text("Chapter") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    ExposedDropdownMenu(expanded = chapterExpanded, onDismissRequest = { chapterExpanded = false }) {
                        chapters.forEach { ch -> DropdownMenuItem(text = { Text(ch) }, onClick = { selectedChapter = ch; chapterExpanded = false }) }
                    }
                }
                OutlinedTextField(value = displayDate, onValueChange = {}, readOnly = true, label = { Text("Date") }, modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, shape = RoundedCornerShape(10.dp), enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant), leadingIcon = { Text("\uD83D\uDCC5", fontSize = 16.sp) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = formatAmPm(startHour, startMinute), onValueChange = {}, readOnly = true, label = { Text("Start") }, modifier = Modifier.weight(1f).clickable { showStartTime = true }, shape = RoundedCornerShape(10.dp), enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant))
                    OutlinedTextField(value = formatAmPm(endHour, endMinute), onValueChange = {}, readOnly = true, label = { Text("End") }, modifier = Modifier.weight(1f).clickable { showEndTime = true }, shape = RoundedCornerShape(10.dp), enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        },
        confirmButton = { Button(onClick = { if (selectedSubject.isNotBlank() && selectedChapter.isNotBlank()) onConfirm(selectedSubject, selectedChapter, selectedDate, String.format("%02d:%02d", startHour, startMinute), String.format("%02d:%02d", endHour, endMinute)) }) { Text(if (isEdit) "Update" else "Schedule") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { selectedDateMillis = it }; showDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }) { DatePicker(state = dps) }
    }
    if (showStartTime) {
        val tps = rememberTimePickerState(initialHour = startHour, initialMinute = startMinute, is24Hour = false)
        AlertDialog(onDismissRequest = { showStartTime = false }, title = { Text("Start Time", fontWeight = FontWeight.Bold) }, text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = tps) } }, confirmButton = { Button(onClick = { startHour = tps.hour; startMinute = tps.minute; showStartTime = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showStartTime = false }) { Text("Cancel") } })
    }
    if (showEndTime) {
        val tps = rememberTimePickerState(initialHour = endHour, initialMinute = endMinute, is24Hour = false)
        AlertDialog(onDismissRequest = { showEndTime = false }, title = { Text("End Time", fontWeight = FontWeight.Bold) }, text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = tps) } }, confirmButton = { Button(onClick = { endHour = tps.hour; endMinute = tps.minute; showEndTime = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showEndTime = false }) { Text("Cancel") } })
    }
}

fun formatAmPm(hour: Int, minute: Int): String {
    val h = if (hour % 12 == 0) 12 else hour % 12
    return String.format("%d:%02d %s", h, minute, if (hour < 12) "AM" else "PM")
}
