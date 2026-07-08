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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cma.studyplanner.ui.theme.*

val allExams = listOf(
    "── Accounting & Finance ──" to true,
    "CMA (Cost & Management Accountant)" to false,
    "CA (Chartered Accountant)" to false,
    "CS (Company Secretary)" to false,
    "CPA (Certified Public Accountant)" to false,
    "ACCA" to false, "CFA" to false, "FRM" to false, "CIA" to false,
    "── Engineering ──" to true,
    "GATE" to false, "JEE Main" to false, "JEE Advanced" to false, "BITSAT" to false,
    "── Medical ──" to true,
    "NEET UG" to false, "NEET PG" to false, "AIIMS" to false,
    "── Civil Services ──" to true,
    "UPSC CSE" to false, "UPSC CDS" to false, "UPSC NDA" to false, "State PSC" to false,
    "── Banking & SSC ──" to true,
    "IBPS PO" to false, "IBPS Clerk" to false, "SBI PO" to false,
    "SSC CGL" to false, "SSC CHSL" to false, "RBI Grade B" to false,
    "── MBA ──" to true,
    "CAT" to false, "GMAT" to false, "XAT" to false, "MAT" to false,
    "── Law ──" to true,
    "CLAT" to false, "AILET" to false, "LSAT" to false,
    "── Abroad ──" to true,
    "GRE" to false, "SAT" to false, "IELTS" to false, "TOEFL" to false, "PTE" to false,
    "── Teaching ──" to true,
    "UGC NET" to false, "CTET" to false, "TET" to false,
    "── School / College ──" to true,
    "Board Exams (10th)" to false, "Board Exams (12th)" to false,
    "University Semester" to false, "University Finals" to false,
    "── Defence ──" to true,
    "NDA" to false, "CDS" to false, "AFCAT" to false,
    "── Other ──" to true,
    "Railway (RRB)" to false, "Insurance (LIC AAO)" to false,
    "Judiciary" to false, "Police" to false, "Other" to false
)

@Composable
fun OnboardingScreen(onComplete: (String, String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var exam by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf("") }
    var customExam by remember { mutableStateOf("") }
    var examExpanded by remember { mutableStateOf(false) }

    val selectedExam = if (exam == "Other") customExam else exam
    val canSubmit = name.isNotBlank() && selectedExam.isNotBlank() && examDate.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("\uD83D\uDCD6", fontSize = 44.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("StudyPlanner Pro", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Set up your profile to get started", fontSize = 13.sp, color = Muted)
        Spacer(modifier = Modifier.height(28.dp))

        FormLabel("Full Name *")
        OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Enter your full name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
        Spacer(modifier = Modifier.height(14.dp))

        FormLabel("Date of Birth")
        OutlinedTextField(value = dob, onValueChange = { dob = it }, placeholder = { Text("DD/MM/YYYY") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
        Spacer(modifier = Modifier.height(14.dp))

        FormLabel("Email")
        OutlinedTextField(value = email, onValueChange = { email = it }, placeholder = { Text("you@example.com") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
        Spacer(modifier = Modifier.height(14.dp))

        FormLabel("Course / Exam *")
        ExposedDropdownMenuBox(expanded = examExpanded, onExpandedChange = { examExpanded = it }) {
            OutlinedTextField(
                value = selectedExam, onValueChange = {}, readOnly = true,
                placeholder = { Text("Select your exam...") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = examExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(expanded = examExpanded, onDismissRequest = { examExpanded = false }) {
                allExams.forEach { (n, isHeader) ->
                    if (isHeader) {
                        Text(n.replace("──", "").trim(), modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Muted)
                    } else {
                        DropdownMenuItem(
                            text = { Text(n, fontSize = 13.sp, color = if (exam == n) Indigo else TextDark, fontWeight = if (exam == n) FontWeight.Bold else FontWeight.Normal) },
                            onClick = { exam = n; examExpanded = false }
                        )
                    }
                }
            }
        }
        if (exam == "Other") {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = customExam, onValueChange = { customExam = it }, placeholder = { Text("Type your exam name...") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
        }
        Spacer(modifier = Modifier.height(14.dp))

        FormLabel("Exam Date *")
        OutlinedTextField(value = examDate, onValueChange = { examDate = it }, placeholder = { Text("YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true, leadingIcon = { Text("\uD83D\uDCC5", fontSize = 16.sp) })
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { if (canSubmit) onComplete(name, dob, email, selectedExam, examDate) },
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
            enabled = canSubmit, colors = ButtonDefaults.buttonColors(containerColor = Indigo)
        ) { Text("\uD83D\uDE80 Start My Journey", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(8.dp))
        Text("You can change these later in Settings", fontSize = 11.sp, color = Muted)
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun FormLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
}
