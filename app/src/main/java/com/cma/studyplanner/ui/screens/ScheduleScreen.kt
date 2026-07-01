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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun ScheduleScreen(vm: MainViewModel) {
    val subjects by vm.subjectsWithChapters.collectAsState()
    val schedules by vm.schedules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var filterMode by remember { mutableStateOf("all") }

    val today = LocalDate.now().toString()
    val filtered = when (filterMode) {
        "today" -> schedules.filter { it.date == today }
        "upcoming" -> schedules.filter { it.date >= today && !it.completed }
        else -> schedules
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Schedule", fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("Plan your study sessions", color = Muted, fontSize = 14.sp)
            }
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "All", "today" to "Today", "upcoming" to "Upcoming").forEach { (key, label) ->
                FilterChip(
                    selected = filterMode == key,
                    onClick = { filterMode = key },
                    label = { Text(label, fontSize = 13.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filtered.isEmpty()) {
            EmptyState("📅", "No sessions found", "Create a study session to get started")
        } else {
            val grouped = filtered.groupBy { it.date }.toSortedMap()
            grouped.forEach { (date, items) ->
                Text(
                    vm.formatDateShort(date),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (date == today) Indigo else TextDark,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                items.forEach { sch ->
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
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showAddDialog) {
        AddScheduleDialog(
            subjects = subjects.map { it.subject.name to it.chapters.map { c -> c.name } },
            onConfirm = { subj, ch, date, st, et ->
                vm.addScheduleWithAlarm(subj, ch, date, st, et)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun AddScheduleDialog(
    subjects: List<Pair<String, List<String>>>,
    onConfirm: (String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSubject by remember { mutableStateOf("") }
    var selectedChapter by remember { mutableStateOf("") }
    var subjectExpanded by remember { mutableStateOf(false) }
    var chapterExpanded by remember { mutableStateOf(false) }

    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val selectedDate = remember(selectedDateMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }
    val displayDate = remember(selectedDate) {
        try {
            LocalDate.parse(selectedDate).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
        } catch (e: Exception) { selectedDate }
    }

    // Time picker states
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var startHour by remember { mutableIntStateOf(9) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(10) }
    var endMinute by remember { mutableIntStateOf(0) }

    val startTimeStr = String.format("%02d:%02d", startHour, startMinute)
    val endTimeStr = String.format("%02d:%02d", endHour, endMinute)
    val startDisplay = formatTime12h(startHour, startMinute)
    val endDisplay = formatTime12h(endHour, endMinute)

    val chapters = subjects.find { it.first == selectedSubject }?.second ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Session", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Subject Dropdown
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSubject,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        subjects.forEach { (name, _) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedSubject = name
                                    selectedChapter = ""
                                    subjectExpanded = false
                                }
                            )
                        }
                    }
                }

                // Chapter Dropdown
                ExposedDropdownMenuBox(
                    expanded = chapterExpanded,
                    onExpandedChange = { chapterExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedChapter,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Chapter") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = chapterExpanded,
                        onDismissRequest = { chapterExpanded = false }
                    ) {
                        chapters.forEach { ch ->
                            DropdownMenuItem(
                                text = { Text(ch) },
                                onClick = {
                                    selectedChapter = ch
                                    chapterExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Picker Button
                OutlinedTextField(
                    value = displayDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = { Text("📅", fontSize = 16.sp) },
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also {
                        LaunchedEffect(it) {
                            it.interactions.collect { interaction ->
                                if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                    showDatePicker = true
                                }
                            }
                        }
                    }
                )

                // Time Row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Text("🕐", fontSize = 14.sp) },
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also {
                            LaunchedEffect(it) {
                                it.interactions.collect { interaction ->
                                    if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                        showStartTimePicker = true
                                    }
                                }
                            }
                        }
                    )
                    OutlinedTextField(
                        value = endDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Text("🕐", fontSize = 14.sp) },
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also {
                            LaunchedEffect(it) {
                                it.interactions.collect { interaction ->
                                    if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                        showEndTimePicker = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSubject.isNotBlank() && selectedChapter.isNotBlank()) {
                        onConfirm(selectedSubject, selectedChapter, selectedDate, startTimeStr, endTimeStr)
                    }
                }
            ) { Text("Schedule") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Start Time Picker Dialog
    if (showStartTimePicker) {
        TimePickerDialog(
            initialHour = startHour,
            initialMinute = startMinute,
            onConfirm = { h, m -> startHour = h; startMinute = m; showStartTimePicker = false },
            onDismiss = { showStartTimePicker = false }
        )
    }

    // End Time Picker Dialog
    if (showEndTimePicker) {
        TimePickerDialog(
            initialHour = endHour,
            initialMinute = endMinute,
            onConfirm = { h, m -> endHour = h; endMinute = m; showEndTimePicker = false },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time", fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun formatTime12h(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h = if (hour % 12 == 0) 12 else hour % 12
    return String.format("%d:%02d %s", h, minute, amPm)
}
