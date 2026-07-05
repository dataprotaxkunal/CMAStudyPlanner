@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.cma.studyplanner.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cma.studyplanner.ui.components.*
import com.cma.studyplanner.ui.theme.*
import com.cma.studyplanner.viewmodel.MainViewModel

@Composable
fun SubjectsScreen(vm: MainViewModel) {
    val subjects by vm.subjectsWithChapters.collectAsState()
    val expandedId by vm.expandedSubjectId.collectAsState()
    var showAddSubject by remember { mutableStateOf(false) }
    var showAddChapter by remember { mutableStateOf<String?>(null) }
    var viewMode by remember { mutableStateOf("table") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Subjects", fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("Manage your subjects", color = Muted, fontSize = 14.sp)
            }
            Button(onClick = { showAddSubject = true }, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // View Toggle
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = viewMode == "table", onClick = { viewMode = "table" }, label = { Text("Table", fontSize = 12.sp) }, shape = RoundedCornerShape(8.dp))
            FilterChip(selected = viewMode == "cards", onClick = { viewMode = "cards" }, label = { Text("Cards", fontSize = 12.sp) }, shape = RoundedCornerShape(8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (subjects.isEmpty()) {
            EmptyState("📚", "No subjects yet", "Add your first subject to get started")
        }

        // ═══ TABLE VIEW ═══
        if (viewMode == "table" && subjects.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(Color(0xFF1E1E2E))
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Text("#", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                        Text("Subject", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Chapter", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                        Text("Status", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
                    }

                    // Rows
                    var sn = 0
                    subjects.forEach { swc ->
                        val color = Color(vm.colors[swc.subject.colorIndex % vm.colors.size])
                        swc.chapters.forEach { chapter ->
                            sn++
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .background(if (sn % 2 == 0) Color(0xFFFAFBFC) else Color.White)
                                    .clickable { vm.toggleChapter(chapter) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$sn", fontSize = 13.sp, color = Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(32.dp))
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(color))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(swc.subject.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }
                                Text(chapter.name, fontSize = 13.sp, modifier = Modifier.weight(1.5f), maxLines = 1, color = Color(0xFF444444))
                                Box(
                                    modifier = Modifier.width(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (chapter.done) Color(0xFFD1FAE5) else Color(0xFFFEF3C7))
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (chapter.done) "Done" else "Pending",
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        color = if (chapter.done) Color(0xFF059669) else Color(0xFFD97706)
                                    )
                                }
                            }
                            Divider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        // ═══ CARD VIEW ═══
        if (viewMode == "cards" && subjects.isNotEmpty()) {
            subjects.forEach { swc ->
                val color = Color(vm.colors[swc.subject.colorIndex % vm.colors.size])
                val lightColor = Color(vm.lightColors[swc.subject.colorIndex % vm.lightColors.size])
                val isExpanded = expandedId == swc.subject.id
                val totalCh = swc.chapters.size
                val doneCh = swc.chapters.count { it.done }
                val hours = vm.getSubjectStudyHours(swc.subject.name)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).animateContentSize()
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { vm.toggleExpandSubject(swc.subject.id) }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(lightColor), contentAlignment = Alignment.Center) { Text("📘", fontSize = 20.sp) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(swc.subject.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                                Text("$doneCh/$totalCh chapters", fontSize = 12.sp, color = Muted)
                            }
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Muted, modifier = Modifier.rotate(if (isExpanded) 180f else 0f))
                        }

                        if (totalCh > 0) {
                            LinearProgressIndicator(
                                progress = doneCh.toFloat() / totalCh,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = color, trackColor = color.copy(alpha = 0.12f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (isExpanded) {
                            Divider(color = Border, thickness = 0.5.dp)
                            if (swc.chapters.isEmpty()) {
                                Text("No chapters yet", modifier = Modifier.padding(16.dp), color = Muted, fontSize = 13.sp)
                            } else {
                                swc.chapters.forEachIndexed { index, chapter ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(if (chapter.done) color else color.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (chapter.done) Color.White else color)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CheckButton(checked = chapter.done, onToggle = { vm.toggleChapter(chapter) })
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            chapter.name, modifier = Modifier.weight(1f), fontSize = 14.sp,
                                            color = if (chapter.done) Muted else TextDark,
                                            textDecoration = if (chapter.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                                        )
                                        IconButton(onClick = { vm.deleteChapter(chapter.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = Muted, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { showAddChapter = swc.subject.id }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Chapter", fontSize = 12.sp)
                                }
                                OutlinedButton(onClick = { vm.deleteSubject(swc.subject.id) }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Delete", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showAddSubject) {
        AddSubjectDialog(
            availableSubjects = listOf("CMA","CA","CS","CPA","ACCA","CFA","GATE","JEE","NEET","UPSC","CAT","GMAT","SSC","Banking","Board Exams").filter { name -> subjects.none { it.subject.name == name } },
            onConfirm = { name -> vm.addSubject(name); showAddSubject = false },
            onDismiss = { showAddSubject = false }
        )
    }

    showAddChapter?.let { subjectId ->
        InputDialog(title = "Add Chapter", placeholder = "Chapter name", confirmText = "Add",
            onConfirm = { name -> vm.addChapter(subjectId, name); showAddChapter = null },
            onDismiss = { showAddChapter = null })
    }
}

@Composable
fun AddSubjectDialog(availableSubjects: List<String>, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subject", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("Subject name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                if (availableSubjects.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Quick add:", fontSize = 12.sp, color = Muted)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableSubjects.take(8).forEach { name ->
                            AssistChip(onClick = { text = name }, label = { Text(name, fontSize = 11.sp) }, shape = RoundedCornerShape(8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
