@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.cma.studyplanner

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cma.studyplanner.ui.screens.*
import com.cma.studyplanner.ui.theme.CMATheme
import com.cma.studyplanner.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { requestExactAlarmPermissionIfNeeded() }

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    })
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    })
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestExactAlarmPermissionIfNeeded()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            calendarPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            )
        }

        setContent {
            CMATheme {
                val vm: MainViewModel = viewModel()

                LaunchedEffect(Unit) {
                    vm.toastMessage.collectLatest { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                MainAppWithOnboarding(vm)
            }
        }
    }
}

@Composable
fun MainAppWithOnboarding(vm: MainViewModel) {
    var onboarded by remember { mutableStateOf(vm.isOnboarded()) }

    if (!onboarded) {
        OnboardingScreen(
            onComplete = { name, dob, email, exam, examDate ->
                vm.saveProfile(name, dob, email, exam, examDate)
                onboarded = true
            }
        )
    } else {
        MainApp(vm)
    }
}

data class NavItem(val id: String, val icon: String, val label: String)

@Composable
fun MainApp(vm: MainViewModel) {
    var showFocus by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val navItems = listOf(
        NavItem("home", "\uD83C\uDFE0", "Home"),
        NavItem("study", "\uD83D\uDCDA", "Study"),
        NavItem("focus", "\uD83C\uDFAF", "Focus"),
        NavItem("plan", "\uD83D\uDCC5", "Plan"),
        NavItem("settings", "\u2699\uFE0F", "Settings"),
    )

    // Full-screen focus mode
    if (showFocus) {
        FocusScreen(vm = vm, onClose = { showFocus = false })
        return
    }

    val pagerState = rememberPagerState(pageCount = { 4 }) // 4 swipeable pages (focus is separate)

    // Map pager index to nav index (skip focus at index 2)
    val currentNavIndex = when (pagerState.currentPage) {
        0 -> 0 // home
        1 -> 1 // study
        2 -> 3 // plan
        3 -> 4 // settings
        else -> 0
    }

    Scaffold(
        bottomBar = {
            // Design B: Dark navbar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFF1E1E2E))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEachIndexed { index, item ->
                        val isActive = index == currentNavIndex

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isActive) Color(0xFF6366F1).copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    if (item.id == "focus") {
                                        showFocus = true
                                    } else {
                                        val pageIndex = when (index) {
                                            0 -> 0
                                            1 -> 1
                                            3 -> 2
                                            4 -> 3
                                            else -> 0
                                        }
                                        scope.launch { pagerState.animateScrollToPage(pageIndex) }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    item.icon,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    item.label,
                                    fontSize = 9.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) Color.White else Color(0xFFA5A7C4)
                                )
                                if (isActive) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF6366F1))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            when (page) {
                0 -> DashboardScreen(vm)
                1 -> SubjectsScreen(vm)
                2 -> PlanScreen(vm)
                3 -> SettingsScreen(vm)
            }
        }
    }
}

// Plan screen with inner tabs: Calendar | Schedule | Revision
@Composable
fun PlanScreen(vm: MainViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Calendar", "Schedule", "Revision")

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFF6366F1)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Tab content
        when (selectedTab) {
            0 -> CalendarScreen(vm)
            1 -> ScheduleScreen(vm)
            2 -> RevisionScreen(vm)
        }
    }
}
