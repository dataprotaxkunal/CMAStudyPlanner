@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.cma.studyplanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cma.studyplanner.ui.screens.*
import com.cma.studyplanner.ui.theme.CMATheme
import com.cma.studyplanner.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Request calendar permissions
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

                MainApp(vm)
            }
        }
    }
}

data class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainApp(vm: MainViewModel) {
    var showFocus by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem("Subjects", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
        NavItem("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        NavItem("Schedule", Icons.Filled.Schedule, Icons.Outlined.Schedule),
        NavItem("Revision", Icons.Filled.Replay, Icons.Outlined.Replay),
        NavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    )

    // Focus mode is separate full-screen
    if (showFocus) {
        FocusScreen(vm = vm, onClose = { showFocus = false })
        return
    }

    // Pager state for swipe navigation
    val pagerState = rememberPagerState(pageCount = { navItems.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 2.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                navItems.take(3).forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        icon = {
                            Icon(
                                if (pagerState.currentPage == index) item.selectedIcon else item.unselectedIcon,
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
                // Focus button (center)
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
                        Text(
                            "Focus", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
                navItems.drop(3).forEachIndexed { index, item ->
                    val actualIndex = index + 3
                    NavigationBarItem(
                        selected = pagerState.currentPage == actualIndex,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(actualIndex) }
                        },
                        icon = {
                            Icon(
                                if (pagerState.currentPage == actualIndex) item.selectedIcon else item.unselectedIcon,
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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            when (page) {
                0 -> DashboardScreen(vm)
                1 -> SubjectsScreen(vm)
                2 -> CalendarScreen(vm)
                3 -> ScheduleScreen(vm)
                4 -> RevisionScreen(vm)
                5 -> SettingsScreen(vm)
            }
        }
    }
}
