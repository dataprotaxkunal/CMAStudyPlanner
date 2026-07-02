package com.cma.studyplanner.service

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.delay

class AlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

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
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        val subject = intent.getStringExtra("subject") ?: "Study Time"
        val chapter = intent.getStringExtra("chapter") ?: ""
        val time = intent.getStringExtra("time") ?: ""

        startAlarmSound()
        startVibration()

        setContent {
            AlarmScreen(
                subject = subject,
                chapter = chapter,
                time = time,
                onDismiss = { finishAlarm() },
                onOpenApp = {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    })
                    finishAlarm()
                }
            )
        }
    }

    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmActivity, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 800, 400, 800, 400)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun finishAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.release()
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    override fun onBackPressed() {
        // Prevent back button
    }
}

@Composable
fun AlarmScreen(
    subject: String,
    chapter: String,
    time: String,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit
) {
    var pulse by remember { mutableStateOf(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            pulse = 1.15f
            delay(600)
            pulse = 1f
            delay(600)
        }
    }
    val pulseAnimated by animateFloatAsState(
        targetValue = pulse,
        animationSpec = tween(600),
        label = "pulse"
    )

    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1E2E), Color(0xFF6366F1))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp, horizontal = 32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(currentTime, fontSize = 24.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("STUDY ALARM", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier.size((140 * pulseAnimated).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔔", fontSize = 72.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text("Time to Study!", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.15f)).padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(subject, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        if (chapter.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(chapter, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                        }
                        if (time.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("⏰ $time", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onOpenApp,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF6366F1))
                ) {
                    Text("📚 Start Studying", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Dismiss", fontSize = 15.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

fun getCurrentTime(): String {
    val cal = java.util.Calendar.getInstance()
    val h = cal.get(java.util.Calendar.HOUR)
    val m = cal.get(java.util.Calendar.MINUTE)
    val ampm = if (cal.get(java.util.Calendar.AM_PM) == 0) "AM" else "PM"
    val hr = if (h == 0) 12 else h
    return String.format("%d:%02d %s", hr, m, ampm)
}
