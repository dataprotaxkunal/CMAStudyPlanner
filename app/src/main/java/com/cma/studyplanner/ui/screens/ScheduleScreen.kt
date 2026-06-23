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

@Composable
fun ScheduleScreen(vm: MainViewModel) {
    val subjects by vm.subjectsWithChapters.collectAsState()
    val schedules by vm.schedules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var filterMode by remember { mutableStateOf("all") } // all, today, upcoming

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

        // Filter chips
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
            // Group by date
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
                        time = "${vm.formatTime12(sch.startTime)} – ${vm.formatTime12(sch.endTime)}",
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

    // Add Schedule Dialog
    if (showAddDialog) {
        AddScheduleDialog(
            subjects = subjects.map { it.subject.name to it.chapters.map { c -> c.name } },
            onConfirm = { subj, ch, date, st, et ->
                vm.addSchedule(subj, ch, date, st, et)
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
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var subjectExpanded by remember { mutableStateOf(false) }
    var chapterExpanded by remember { mutableStateOf(false) }

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

                // Date
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Time row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSubject.isNotBlank() && selectedChapter.isNotBlank()) {
                        onConfirm(selectedSubject, selectedChapter, date, startTime, endTime)
                    }
                }
            ) { Text("Schedule") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
