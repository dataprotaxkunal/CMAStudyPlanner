package com.cma.studyplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.cma.studyplanner.ui.theme.*
import com.cma.studyplanner.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(vm: MainViewModel, onClose: () -> Unit) {
    val active by vm.focusActive.collectAsState()
    val paused by vm.focusPaused.collectAsState()
    val seconds by vm.focusSeconds.collectAsState()
    val totalSeconds by vm.focusTotalSeconds.collectAsState()
    val mode by vm.focusMode.collectAsState()
    val sessions by vm.focusSessions.collectAsState()

    // Timer tick
    LaunchedEffect(active, paused) {
        while (active && !paused) {
            delay(1000)
            vm.tickFocus()
        }
    }

    val remaining = totalSeconds - seconds
    val minutes = remaining / 60
    val secs = remaining % 60
    val progress = if (totalSeconds > 0) seconds.toFloat() / totalSeconds else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0F1A), Color(0xFF1A1A2E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = {
                    if (active) vm.stopFocus()
                    onClose()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mode toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(3.dp)
            ) {
                listOf("study" to "Study", "break" to "Break").forEach { (key, label) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (mode == key) Modifier.background(Indigo)
                                else Modifier
                            )
                            .clickable { vm.setFocusModeType(key) }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            label,
                            color = if (mode == key) Color.White else Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Timer circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(250.dp)
            ) {
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White.copy(alpha = 0.08f),
                    strokeWidth = 8.dp,
                )
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxSize(),
                    color = if (mode == "study") Indigo else Green,
                    strokeWidth = 8.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        String.format("%02d:%02d", minutes, secs),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        if (mode == "study") "FOCUS" else "BREAK",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 4.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Session count
            Text(
                "Session ${sessions + 1}",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp
            )

            // Status badges
            if (active) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (vm.dndEnabled) {
                        Badge(containerColor = Indigo.copy(alpha = 0.3f)) {
                            Text("🔇 DND On", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    if (vm.appBlockingEnabled) {
                        Badge(containerColor = Red.copy(alpha = 0.3f)) {
                            Text("🚫 Apps Blocked", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop
                if (active) {
                    IconButton(
                        onClick = { vm.stopFocus() },
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                // Play/Pause
                IconButton(
                    onClick = {
                        if (!active) vm.startFocus()
                        else vm.pauseFocus()
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Indigo)
                ) {
                    Icon(
                        if (!active || paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Skip
                if (active) {
                    IconButton(
                        onClick = { vm.skipFocusPhase() },
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Skip", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Info text
            if (!active) {
                Text(
                    "Tap play to start focus mode.\nDND will activate and apps will be blocked.",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
