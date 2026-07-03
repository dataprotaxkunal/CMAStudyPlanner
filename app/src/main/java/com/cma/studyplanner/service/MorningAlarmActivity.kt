package com.cma.studyplanner.service

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cma.studyplanner.MainActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MorningAlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val examName = intent.getStringExtra("exam_name") ?: "Exam"
        val examDateStr = intent.getStringExtra("exam_date") ?: ""

        setContent {
            MorningCountdownScreen(
                examName = examName,
                examDate = examDateStr,
                onDismiss = { finish() },
                onOpenApp = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
fun MorningCountdownScreen(examName: String, examDate: String, onDismiss: () -> Unit, onOpenApp: () -> Unit) {
    val daysLeft = try {
        ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(examDate)).toInt().coerceAtLeast(0)
    } catch (e: Exception) { 0 }
    val weeksLeft = daysLeft / 7
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when { hour < 12 -> "Good Morning"; hour < 17 -> "Good Afternoon"; else -> "Good Evening" }
    val dateToday = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0F0F1A), Color(0xFF1A1A2E), Color(0xFF6366F1)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(vertical = 48.dp, horizontal = 32.dp)
        ) {
            // Top
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\u2600\uFE0F", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(greeting, fontSize = 16.sp, color = Color.White.copy(alpha = 0.6f))
                Text(dateToday, fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f))
            }

            // Center - Countdown
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83C\uDFAF", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(examName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(20.dp))

                // Big countdown number
                Box(
                    modifier = Modifier.size(180.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(150.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$daysLeft",
                                fontSize = 56.sp, fontWeight = FontWeight.Black,
                                color = if (daysLeft < 30) Color(0xFFEF4444) else Color.White
                            )
                            Text("DAYS LEFT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 3.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Weeks + months
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$weeksLeft", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFFBBF24))
                        Text("weeks", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${daysLeft / 30}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                        Text("months", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${daysLeft * 14}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B5CF6))
                        Text("hours", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (daysLeft > 90) "You have time. Use it wisely!" 
                    else if (daysLeft > 30) "Every day counts. Stay consistent!"
                    else if (daysLeft > 7) "Final stretch! Give it your all!"
                    else "Almost there! You've got this!",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center
                )
            }

            // Bottom buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onOpenApp,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF6366F1))
                ) {
                    Text("\uD83D\uDCDA Start Studying", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Dismiss", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }
        }
    }
}
