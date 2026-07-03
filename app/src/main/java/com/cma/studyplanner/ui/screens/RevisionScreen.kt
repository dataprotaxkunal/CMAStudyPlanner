@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cma.studyplanner.ui.screens

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
    val completed = revisions.filter { it.completed }
    val overdue = revisions.filter { it.date < today && !it.completed }

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
                Text("Revisions", fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("Track your revision schedule", color = Muted, fontSize = 14.sp)
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

        Spacer(modifier = Modifier.height(12.dp))

        // Stats row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Upcoming",
                value = "${upcoming.size}",
                icon = "📅",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Overdue",
                value = "${overdue.size}",
                icon = "⚠️",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Done",
                value = "${completed.size}",
                icon = "✅",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Overdue
        if (overdue.isNotEmpty()) {
            SectionHeader(title = "⚠️ Overdue")
            Spacer(modifier = Modifier.height(8.dp))
            overdue.forEach { rev ->
                RevisionItem(rev, vm, Color(0xFFFEE2E2))
                Spacer(modifier = Modifier.height(6.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Upcoming
        SectionHeader(title = "📅 Upcoming")
        Spacer(modifier = Modifier.height(8.dp))
        if (upcoming.isEmpty()) {
            EmptyState("📖", "No upcoming revisions")
        } else {
            upcoming.forEach { rev ->
                RevisionItem(rev, vm, Color(0xFFEEF0FF))
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Completed
        if (completed.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "✅ Completed")
            Spacer(modifier = Modifier.height(8.dp))
            completed.take(10).forEach { rev ->
                RevisionItem(rev, vm, Color(0xFFF0FDF4))
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showAddDialog) {
        AddRevisionDialog(
            subjects = subjects,
            onConfirm = { subjectId, chapterId, date, startTime, endTime, notes ->
                vm.addRevision(subjectId, chapterId, date, startTime, endTime, notes)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun RevisionItem(
    rev: com.cma.studyplanner.data.Revision,
    vm: MainViewModel,
    bgColor: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckButton(
                checked = rev.completed,
                onToggle = { vm.toggleRevisionComplete(rev) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rev.subjectName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(rev.chapterName, fontSize = 12.sp, color = Muted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        vm.formatDateShort(rev.date),
                        fontSize = 11.sp,
                        color = Indigo,
                        fontWeight = FontWeight.Medium
                    )
                    if (rev.notes.isNotBlank()) {
                        Text(
                            " • ${rev.notes}",
                            fontSize = 11.sp,
                            color = Muted,
                            maxLines = 1
                        )
                    }
                }
            }
            IconButton(
                onClick = { vm.deleteRevision(rev.id) },
                modifier = Modifier.size(32.dp)
            ) {
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
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var notes by remember { mutableStateOf("") }
    var subjExpanded by remember { mutableStateOf(false) }
    var chapExpanded by remember { mutableStateOf(false) }

    val selectedSubject = subjects.find { it.subject.id == selectedSubjectId }
    val chapters = selectedSubject?.chapters ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Revision", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = subjExpanded,
                    onExpandedChange = { subjExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSubject?.subject?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = subjExpanded,
                        onDismissRequest = { subjExpanded = false }
                    ) {
                        subjects.forEach { swc ->
                            DropdownMenuItem(
                                text = { Text(swc.subject.name) },
                                onClick = {
                                    selectedSubjectId = swc.subject.id
                                    selectedChapterId = ""
                                    subjExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = chapExpanded,
                    onExpandedChange = { chapExpanded = it }
                ) {
                    OutlinedTextField(
                        value = chapters.find { it.id == selectedChapterId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Chapter") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = chapExpanded,
                        onDismissRequest = { chapExpanded = false }
                    ) {
                        chapters.forEach { ch ->
                            DropdownMenuItem(
                                text = { Text(ch.name) },
                                onClick = {
                                    selectedChapterId = ch.id
                                    chapExpanded = false
                                }
                            )
                        }
                    }
                }

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

                OutlinedTextField(
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("e.g. Focus on formulas") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSubjectId.isNotBlank() && selectedChapterId.isNotBlank()) {
                        onConfirm(selectedSubjectId, selectedChapterId, date, startTime, endTime, notes)
                    }
                }
            ) { Text("Add Revision") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
