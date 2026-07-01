@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cma.studyplanner.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
    var whatsapp by remember { mutableStateOf(vm.whatsappNumber) }
    var studyMin by remember { mutableStateOf(vm.focusStudyMinutes.toString()) }
    var breakMin by remember { mutableStateOf(vm.focusBreakMinutes.toString()) }
    var dndOn by remember { mutableStateOf(vm.dndEnabled) }
    var blockOn by remember { mutableStateOf(vm.appBlockingEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text("Configure your study planner", color = Muted, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(24.dp))

        // ── Permissions Section ──
        SectionLabel("🔐 Permissions")

        PermissionItem(
            title = "Do Not Disturb Access",
            subtitle = "Required to toggle DND during focus mode",
            isGranted = isDndPermissionGranted(context),
            onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        PermissionItem(
            title = "Usage Access",
            subtitle = "Required to detect and block apps",
            isGranted = isUsagePermissionGranted(context),
            onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        PermissionItem(
            title = "Display Over Other Apps",
            subtitle = "Required to show blocker screen",
            isGranted = Settings.canDrawOverlays(context),
            onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Focus Mode Settings ──
        SectionLabel("🎯 Focus Mode")

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // DND toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Enable DND", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Silence notifications during focus", fontSize = 12.sp, color = Muted)
                    }
                    Switch(
                        checked = dndOn,
                        onCheckedChange = {
                            dndOn = it
                            vm.updateDndEnabled(it)
                        }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Border)

                // App blocking toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Block Apps", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Block all apps except essentials", fontSize = 12.sp, color = Muted)
                    }
                    Switch(
                        checked = blockOn,
                        onCheckedChange = {
                            blockOn = it
                            vm.updateAppBlocking(it)
                        }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Border)

                // Study duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Study Duration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedTextField(
                        value = studyMin,
                        onValueChange = {
                            studyMin = it
                            it.toIntOrNull()?.let { m -> vm.updateFocusStudy(m) }
                        },
                        modifier = Modifier.width(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        suffix = { Text("min", fontSize = 12.sp, color = Muted) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Break duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Break Duration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedTextField(
                        value = breakMin,
                        onValueChange = {
                            breakMin = it
                            it.toIntOrNull()?.let { m -> vm.updateFocusBreak(m) }
                        },
                        modifier = Modifier.width(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        suffix = { Text("min", fontSize = 12.sp, color = Muted) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── WhatsApp ──
        SectionLabel("💬 WhatsApp Reminders")

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("WhatsApp Number", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Receive study reminders via WhatsApp", fontSize = 12.sp, color = Muted)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = {
                        whatsapp = it
                        vm.updateWhatsapp(it)
                    },
                    placeholder = { Text("+91XXXXXXXXXX") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    leadingIcon = {
                        Text("📱", fontSize = 18.sp)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Whitelisted Apps Info ──
        SectionLabel("📱 Allowed During Focus")

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                listOf(
                    "📞 Phone / Dialer",
                    "💬 Messages",
                    "⚙️ Settings",
                    "📘 CMA Study Planner"
                ).forEach {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Green,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "All other apps will be blocked during focus mode.",
                    fontSize = 12.sp,
                    color = Muted
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Muted,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
fun PermissionItem(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isGranted) onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) Green else Red,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = Muted)
            }
            if (!isGranted) {
                Text(
                    "Grant",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Indigo
                )
            }
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
        val mode = appOps.unsafeCheckOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            context.packageName
        )
        mode == android.app.AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) { false }
}
