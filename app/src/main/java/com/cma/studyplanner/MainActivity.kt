package com.cma.studyplanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cma.studyplanner.ui.screens.*
import com.cma.studyplanner.ui.theme.CMATheme
import com.cma.studyplanner.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            CMATheme {
                val vm: MainViewModel = viewModel()

                // Toast observer
                LaunchedEffect(Unit) {
                    vm.toastMessage.collectLatest { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                MainApp(vm)
            }
        }
    }
}

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(vm: MainViewModel) {
    var currentRoute by remember { mutableStateOf("dashboard") }
    var showFocus by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavItem("dashboard", "Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem("subjects", "Subjects", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
        NavItem("calendar", "Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        NavItem("schedule", "Schedule", Icons.Filled.Schedule, Icons.Outlined.Schedule),
        NavItem("revision", "Revision", Icons.Filled.Replay, Icons.Outlined.Replay),
        NavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    )

    // Full-screen focus mode
    if (showFocus) {
        FocusScreen(vm = vm, onClose = { showFocus = false })
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 2.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                // Main nav items (first 4)
                navItems.take(4).forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = { currentRoute = item.route },
                        icon = {
                            Icon(
                                if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(item.label, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        },
                        alwaysShowLabel = true
                    )
                }
                // Focus mode button (center-ish)
                NavigationBarItem(
                    selected = false,
                    onClick = { showFocus = true },
                    icon = {
                        Icon(
                            Icons.Filled.FitnessCenter,
                            contentDescription = "Focus",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text("Focus", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                )
                // More items
                navItems.drop(4).forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = { currentRoute = item.route },
                        icon = {
                            Icon(
                                if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(item.label, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentRoute) {
                "dashboard" -> DashboardScreen(vm)
                "subjects" -> SubjectsScreen(vm)
                "calendar" -> CalendarScreen(vm)
                "schedule" -> ScheduleScreen(vm)
                "revision" -> RevisionScreen(vm)
                "settings" -> SettingsScreen(vm)
            }
        }
    }
}
