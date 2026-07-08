@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cma.studyplanner.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.cma.studyplanner.ui.theme.*

@Composable
fun LockScreen(
    pinEnabled: Boolean,
    savedPin: String,
    biometricEnabled: Boolean,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var showPinEntry by remember { mutableStateOf(!biometricEnabled) }

    // Auto-trigger biometric on launch
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            triggerBiometric(context, onSuccess = onUnlock, onFallback = { showPinEntry = true })
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1E1E2E), Color(0xFF6366F1)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("🔒", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("StudyPlanner Pro", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Enter your PIN to continue", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(32.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier.size(16.dp).clip(CircleShape).background(
                            if (i < enteredPin.length) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (error.isNotEmpty()) {
                Text(error, fontSize = 12.sp, color = Color(0xFFEF4444), textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Number pad
            val keys = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","⌫"))
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                                .background(if (key.isEmpty()) Color.Transparent else Color.White.copy(alpha = 0.1f))
                                .clickable(enabled = key.isNotEmpty()) {
                                    when (key) {
                                        "⌫" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                                error = ""
                                                if (enteredPin.length == 4) {
                                                    if (enteredPin == savedPin) onUnlock()
                                                    else { error = "Wrong PIN. Try again."; enteredPin = "" }
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key.isNotEmpty()) {
                                Text(key, fontSize = if (key == "⌫") 20.sp else 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Biometric button
            if (biometricEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(onClick = { triggerBiometric(context, onSuccess = onUnlock, onFallback = { showPinEntry = true }) }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Fingerprint", tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Text("Use fingerprint", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

fun triggerBiometric(context: android.content.Context, onSuccess: () -> Unit, onFallback: () -> Unit) {
    val activity = context as? FragmentActivity ?: run { onFallback(); return }
    val biometricManager = BiometricManager.from(context)
    if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
        onFallback(); return
    }
    val executor = ContextCompat.getMainExecutor(context)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onFallback() }
        override fun onAuthenticationFailed() {}
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("StudyPlanner Pro")
        .setSubtitle("Verify your identity")
        .setNegativeButtonText("Use PIN")
        .build()
    prompt.authenticate(info)
}
