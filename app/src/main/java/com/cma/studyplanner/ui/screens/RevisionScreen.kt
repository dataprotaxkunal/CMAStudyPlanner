@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cma.studyplanner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cma.studyplanner.ui.components.*
import com.cma.studyplanner.ui.theme.*
import com.cma.studyplanner.viewmodel.MainViewModel
import java.time.LocalDate

@Composable
fun RevisionScreen(vm: MainViewModel) {
    val subjects by vm.subjectsWithChapters.collectAsState()
    val revisions by vm.revisions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val today = LocalDate.now().toString()
    val upcoming = revisions.filter { it.date >= today && !it.completed }
    val overdue = revisions.filter { it.date < today && !it.completed }
    val completed = revisions.filter { it.completed }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Revisions", fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("Track your revision schedule", color = Muted, fontSize = 14.sp)
            }
            Button(onClick = { showAddDialog = true }, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(title = "Upcoming", value = "${upcoming.size}", icon = "\uD83D\uDCC5", modifier = Modifier.weight(1f))
            StatCard(title = "Overdue", value = "${overdue.size}", icon = "⚠️", modifier = Modifier.weight(1f))
            StatCard(title = "Done", value = "${completed.size}", icon = "✅", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(20.dp))

        if (overdue.isNotEmpty()) {
            Text("⚠️ Overdue", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Red)
            Spacer(modifier = Modifier.height(8.dp))
            overdue.forEach { rev ->
                RevCard(rev, vm, Color(0xFFFEE2E2))
                Spacer(modifier = Modifier.height(6.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("📅 Upcoming", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        if (upcoming.isEmpty()) EmptyState("\uD83D\uDCD6", "No upcoming revisions")
        else upcoming.forEach { rev ->
            RevCard(rev, vm, Color(0xFFEEF0FF))
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (completed.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("✅ Completed", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            completed.take(10).forEach { rev ->
                RevCard(rev, vm, Color(0xFFF0FDF4))
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showAddDialog) {
        AddRevisionDialog(subjects = subjects,
            onConfirm = { subjectId, chapterId, date, st, et, notes ->
                vm.addRevision(subjectId, chapterId, date, st, et, notes)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false })
    }
}

@Composable
fun RevCard(rev: com.cma.studyplanner.data.Revision, vm: MainViewModel, bgColor: Color) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = bgColor)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CheckButton(checked = rev.completed, onToggle = { vm.toggleRevisionComplete(rev) })
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rev.subjectName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(rev.chapterName, fontSize = 12.sp, color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(vm.formatDateShort(rev.date), fontSize = 11.sp, color = Indigo, fontWeight = FontWeight.Medium)
                    // AM/PM time display
                    Text("${formatAmPm(rev.startTime.substringBefore(":").toIntOrNull() ?: 9, rev.startTime.substringAfter(":").toIntOrNull() ?: 0)} - ${formatAmPm(rev.endTime.substringBefore(":").toIntOrNull() ?: 10, rev.endTime.substringAfter(":").toIntOrNull() ?: 0)}", fontSize = 10.sp, color = Muted)
                }
                if (rev.notes.isNotBlank()) Text(rev.notes, fontSize = 11.sp, color = Muted)
            }
            IconButton(onClick = { vm.deleteRevision(rev.id) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Muted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AddRevisionDialog(
    subjects: List<com.cma.studyplanner.data.SubjectWithChapters>,
    onConfirm: (String, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf("") }
    var selectedChapterId by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var notes by remember { mutableStateOf("") }
    var subjExpanded by remember { mutableStateOf(false) }
    var chapExpanded by remember { mutableStateOf(false) }
    var startHour by remember { mutableStateOf(9) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(10) }
    var endMinute by remember { mutableStateOf(0) }
    var showStartTime by remember { mutableStateOf(false) }
    var showEndTime by remember { mutableStateOf(false) }

    val selectedSubject = subjects.find { it.subject.id == selectedSubjectId }
    val chapters = selectedSubject?.chapters ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Revision", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = subjExpanded, onExpandedChange = { subjExpanded = it }) {
                    OutlinedTextField(value = selectedSubject?.subject?.name ?: "", onValueChange = {}, readOnly = true, label = { Text("Subject") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    ExposedDropdownMenu(expanded = subjExpanded, onDismissRequest = { subjExpanded = false }) {
                        subjects.forEach { swc -> DropdownMenuItem(text = { Text(swc.subject.name) }, onClick = { selectedSubjectId = swc.subject.id; selectedChapterId = ""; subjExpanded = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = chapExpanded, onExpandedChange = { chapExpanded = it }) {
                    OutlinedTextField(value = chapters.find { it.id == selectedChapterId }?.name ?: "", onValueChange = {}, readOnly = true, label = { Text("Chapter") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    ExposedDropdownMenu(expanded = chapExpanded, onDismissRequest = { chapExpanded = false }) {
                        chapters.forEach { ch -> DropdownMenuItem(text = { Text(ch.name) }, onClick = { selectedChapterId = ch.id; chapExpanded = false }) }
                    }
                }
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)

                // Time with AM/PM pickers
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formatAmPm(startHour, startMinute), onValueChange = {}, readOnly = true,
                        label = { Text("Start Time") }, modifier = Modifier.weight(1f).clickable { showStartTime = true },
                        shape = RoundedCornerShape(10.dp), enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    OutlinedTextField(
                        value = formatAmPm(endHour, endMinute), onValueChange = {}, readOnly = true,
                        label = { Text("End Time") }, modifier = Modifier.weight(1f).clickable { showEndTime = true },
                        shape = RoundedCornerShape(10.dp), enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedSubjectId.isNotBlank() && selectedChapterId.isNotBlank()) {
                    onConfirm(selectedSubjectId, selectedChapterId, date, String.format("%02d:%02d", startHour, startMinute), String.format("%02d:%02d", endHour, endMinute), notes)
                }
            }) { Text("Add Revision") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showStartTime) {
        val tps = rememberTimePickerState(initialHour = startHour, initialMinute = startMinute, is24Hour = false)
        AlertDialog(onDismissRequest = { showStartTime = false }, title = { Text("Start Time") }, text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = tps) } }, confirmButton = { Button(onClick = { startHour = tps.hour; startMinute = tps.minute; showStartTime = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showStartTime = false }) { Text("Cancel") } })
    }
    if (showEndTime) {
        val tps = rememberTimePickerState(initialHour = endHour, initialMinute = endMinute, is24Hour = false)
        AlertDialog(onDismissRequest = { showEndTime = false }, title = { Text("End Time") }, text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = tps) } }, confirmButton = { Button(onClick = { endHour = tps.hour; endMinute = tps.minute; showEndTime = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showEndTime = false }) { Text("Cancel") } })
    }
}
