@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cma.studyplanner.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.biometric.BiometricManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cma.studyplanner.ui.theme.*
import com.cma.studyplanner.viewmodel.MainViewModel

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var studyMin by remember { mutableStateOf(vm.focusStudyMinutes.toString()) }
    var breakMin by remember { mutableStateOf(vm.focusBreakMinutes.toString()) }
    var dndOn by remember { mutableStateOf(vm.dndEnabled) }
    var blockOn by remember { mutableStateOf(vm.appBlockingEnabled) }
    var calSync by remember { mutableStateOf(vm.calendarSyncEnabled) }

    // Lock settings
    var pinEnabled by remember { mutableStateOf(vm.isPinEnabled()) }
    var biometricEnabled by remember { mutableStateOf(vm.isBiometricEnabled()) }
    var showSetPin by remember { mutableStateOf(false) }
    val canUseBiometric = BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text("Configure your study planner", color = Muted, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        // ── App Lock ──
        SectionLabel("🔒 App Lock")
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                // PIN toggle
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("PIN Lock", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("4-digit PIN to open the app", fontSize = 12.sp, color = Muted)
                    }
                    Switch(checked = pinEnabled, onCheckedChange = {
                        if (it) { showSetPin = true }
                        else { pinEnabled = false; vm.setPin(""); vm.setPinEnabled(false) }
                    })
                }
                if (pinEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showSetPin = true }) { Text("Change PIN", color = Indigo, fontSize = 12.sp) }
                }
                if (canUseBiometric) {
                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = Border)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Fingerprint / Face ID", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Use biometric to unlock app", fontSize = 12.sp, color = Muted)
                        }
                        Switch(checked = biometricEnabled, onCheckedChange = {
                            biometricEnabled = it
                            vm.setBiometricEnabled(it)
                            if (it && !pinEnabled) { showSetPin = true }
                        })
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Cloud backup is not configured in this build.
        SectionLabel("☁️ Cloud Sync")
        Text("Cloud backup is not available yet. Your study data is saved on this device.", fontSize = 12.sp, color = Muted)
        Spacer(modifier = Modifier.height(24.dp))

        // ── Permissions ──
        SectionLabel("Permissions")
        PermItem("Do Not Disturb", "Required for DND", isDndOk(context)) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
        Spacer(modifier = Modifier.height(6.dp))
        PermItem("Usage Access", "Required to block apps", isUsageOk(context)) { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        Spacer(modifier = Modifier.height(6.dp))
        PermItem("Overlay", "Required for blocker screen", Settings.canDrawOverlays(context)) { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))) }
        Spacer(modifier = Modifier.height(24.dp))

        // ── Battery ──
        SectionLabel("Battery Optimization")
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Disable Battery Optimization", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Required for alarms on Samsung/Xiaomi/Oppo", fontSize = 11.sp, color = Muted)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { try { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (_: Exception) {} }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Text("Open Battery Settings") }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // ── Focus ──
        SectionLabel("Focus Mode")
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                TogRow("Enable DND", "Silence during focus", dndOn) { dndOn = it; vm.updateDndEnabled(it) }
                Divider(modifier = Modifier.padding(vertical = 10.dp), color = Border)
                TogRow("Block Apps", "Block all except essentials", blockOn) { blockOn = it; vm.updateAppBlocking(it) }
                Divider(modifier = Modifier.padding(vertical = 10.dp), color = Border)
                NumRow("Study", studyMin) { studyMin = it; it.toIntOrNull()?.let { m -> vm.updateFocusStudy(m) } }
                Spacer(modifier = Modifier.height(8.dp))
                NumRow("Break", breakMin) { breakMin = it; it.toIntOrNull()?.let { m -> vm.updateFocusBreak(m) } }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // ── Weekly ──
        SectionLabel("Weekly Study Plan (hours per day)")
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday").forEachIndexed { index, dayName ->
                    var hrs by remember { mutableStateOf(vm.getDayHours(index).toString()) }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(dayName, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = hrs, onValueChange = { hrs = it; it.toFloatOrNull()?.let { f -> vm.setDayHours(index, f) } }, modifier = Modifier.width(80.dp).height(48.dp), shape = RoundedCornerShape(8.dp), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center), suffix = { Text("hrs", fontSize = 11.sp, color = Muted) })
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Border)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total per week", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${String.format("%.1f", vm.getWeeklyPlannedHours())} hrs", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Indigo)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // ── Calendar ──
        SectionLabel("Google Calendar")
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                TogRow("Sync to Calendar", "Auto-add sessions with reminders", calSync) { calSync = it; vm.updateCalendarSync(it) }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }

    // Set PIN dialog
    if (showSetPin) {
        SetPinDialog(
            onSet = { pin ->
                vm.setPin(pin)
                vm.setPinEnabled(true)
                pinEnabled = true
                showSetPin = false
            },
            onDismiss = {
                showSetPin = false
                if (!pinEnabled) { biometricEnabled = false; vm.setBiometricEnabled(false) }
            }
        )
    }
}

@Composable
fun SetPinDialog(onSet: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 1) "Set 4-digit PIN" else "Confirm PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (step == 1) "Enter a 4-digit PIN to lock your app" else "Re-enter your PIN to confirm", fontSize = 13.sp, color = Muted, modifier = Modifier.padding(bottom = 16.dp))
                OutlinedTextField(
                    value = if (step == 1) pin else confirmPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            if (step == 1) pin = it else confirmPin = it
                            error = ""
                        }
                    },
                    label = { Text("PIN") },
                    placeholder = { Text("••••") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                if (error.isNotEmpty()) { Text(error, fontSize = 12.sp, color = Red, modifier = Modifier.padding(top = 6.dp)) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (step == 1) {
                    if (pin.length == 4) { step = 2; error = "" }
                    else error = "PIN must be 4 digits"
                } else {
                    if (confirmPin == pin) onSet(pin)
                    else { error = "PINs don't match"; confirmPin = "" }
                }
            }) { Text(if (step == 1) "Next" else "Set PIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable fun SectionLabel(t: String) { Text(t, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(bottom = 10.dp)) }

@Composable fun PermItem(t: String, s: String, ok: Boolean, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (ok) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)), modifier = Modifier.fillMaxWidth().clickable { if (!ok) onClick() }) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (ok) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (ok) Green else Red, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text(t, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text(s, fontSize = 11.sp, color = Muted) }
            if (!ok) Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo)
        }
    }
}
@Composable fun TogRow(t: String, s: String, v: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text(t, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(s, fontSize = 12.sp, color = Muted) }; Switch(checked = v, onCheckedChange = onChange) } }
@Composable fun NumRow(label: String, value: String, onChange: (String) -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("$label Duration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp); OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.width(80.dp), shape = RoundedCornerShape(8.dp), singleLine = true, suffix = { Text("min", fontSize = 11.sp, color = Muted) }) } }
fun isDndOk(c: Context) = (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).isNotificationPolicyAccessGranted
fun isUsageOk(c: Context) = try { val ao = c.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager; ao.unsafeCheckOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), c.packageName) == android.app.AppOpsManager.MODE_ALLOWED } catch (_: Exception) { false }
