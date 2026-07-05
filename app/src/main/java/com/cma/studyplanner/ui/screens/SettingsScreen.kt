@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cma.studyplanner.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text("Configure your study planner", color = Muted, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        // ── Permissions ──
        SectionLabel("Permissions")
        PermissionItem("Do Not Disturb Access", "Required for DND during focus", isDndPermissionGranted(context)) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        Spacer(modifier = Modifier.height(8.dp))
        PermissionItem("Usage Access", "Required to block apps", isUsagePermissionGranted(context)) {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        Spacer(modifier = Modifier.height(8.dp))
        PermissionItem("Display Over Other Apps", "Required for blocker screen", Settings.canDrawOverlays(context)) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
        }
        Spacer(modifier = Modifier.height(24.dp))

        // ── Battery ──
        SectionLabel("Battery Optimization")
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Disable Battery Optimization", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Required for alarms on Samsung, Xiaomi, Oppo, Vivo", fontSize = 11.sp, color = Muted)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { try { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (e: Exception) {} }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Text("Open Battery Settings", fontSize = 12.sp) }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Find StudyPlanner Pro and set to Unrestricted", fontSize = 10.sp, color = Muted)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // ── Focus Mode Settings ──
        SectionLabel("Focus Mode")
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Enable DND", fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text("Silence notifications during focus", fontSize = 12.sp, color = Muted) }
                    Switch(checked = dndOn, onCheckedChange = { dndOn = it; vm.updateDndEnabled(it) })
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Border)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Block Apps", fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text("Block all apps except essentials", fontSize = 12.sp, color = Muted) }
                    Switch(checked = blockOn, onCheckedChange = { blockOn = it; vm.updateAppBlocking(it) })
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Border)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Study Duration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedTextField(value = studyMin, onValueChange = { studyMin = it; it.toIntOrNull()?.let { m -> vm.updateFocusStudy(m) } }, modifier = Modifier.width(80.dp), shape = RoundedCornerShape(8.dp), singleLine = true, suffix = { Text("min", fontSize = 12.sp, color = Muted) })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Break Duration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedTextField(value = breakMin, onValueChange = { breakMin = it; it.toIntOrNull()?.let { m -> vm.updateFocusBreak(m) } }, modifier = Modifier.width(80.dp), shape = RoundedCornerShape(8.dp), singleLine = true, suffix = { Text("min", fontSize = 12.sp, color = Muted) })
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // ── Weekly Study Plan ──
        SectionLabel("Weekly Study Plan (hours per day)")
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                dayNames.forEachIndexed { index, dayName ->
                    var hrs by remember { mutableStateOf(vm.getDayHours(index).toString()) }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(dayName, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = hrs, onValueChange = { hrs = it; it.toFloatOrNull()?.let { f -> vm.setDayHours(index, f) } }, modifier = Modifier.width(70.dp).height(44.dp), shape = RoundedCornerShape(8.dp), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center), suffix = { Text("h", fontSize = 10.sp, color = Muted) })
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Border)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total per week", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("${String.format("%.1f", vm.getWeeklyPlannedHours())} hrs", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Indigo)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // ── Google Calendar Sync ──
        SectionLabel("Google Calendar Sync")
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Sync to Google Calendar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text("Auto-add sessions with reminders", fontSize = 12.sp, color = Muted) }
                    Switch(checked = calSync, onCheckedChange = { calSync = it; vm.updateCalendarSync(it) })
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(bottom = 10.dp))
}

@Composable
fun PermissionItem(title: String, subtitle: String, isGranted: Boolean, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (isGranted) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)), modifier = Modifier.fillMaxWidth().clickable { if (!isGranted) onClick() }) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = if (isGranted) Green else Red, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(subtitle, fontSize = 11.sp, color = Muted) }
            if (!isGranted) { Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo) }
        }
    }
}

fun isDndPermissionGranted(context: Context): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return nm.isNotificationPolicyAccessGranted
}

fun isUsagePermissionGranted(context: Context): Boolean {
    return try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.packageName)
        mode == android.app.AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) { false }
}
