package com.taskfinisher.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.taskfinisher.ui.components.AddTaskDialog
import com.taskfinisher.ui.screens.*
import com.taskfinisher.ui.theme.*

sealed class Screen(val route: String, val label: String,
                    val icon: ImageVector, val iconSelected: ImageVector) {
    data object Today   : Screen("today",   "Today",
        Icons.Outlined.Today,       Icons.Filled.Today)
    data object AllTasks: Screen("all",     "All Tasks",
        Icons.Outlined.List,        Icons.Filled.List)
    data object Calendar: Screen("calendar","Calendar",
        Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth)
    data object Archive : Screen("archive", "Archive",
        Icons.Outlined.Archive,     Icons.Filled.Archive)
}

private val bottomNavItems = listOf(Screen.Today, Screen.AllTasks, Screen.Calendar, Screen.Archive)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTaskId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = Surface, tonalElevation = 0.dp) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDest = navBackStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    val selected = currentDest?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (selected) screen.iconSelected else screen.icon,
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Accent,
                            selectedTextColor = Accent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Accent.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editTaskId = null; showAddDialog = true },
                containerColor = Accent,
                contentColor = OnAccent
            ) {
                Icon(Icons.Filled.Add, "Add task")
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn() + slideInHorizontally() },
            exitTransition = { fadeOut() + slideOutHorizontally() },
            popEnterTransition = { fadeIn() + slideInHorizontally { -it } },
            popExitTransition = { fadeOut() + slideOutHorizontally { it } }
        ) {
            composable(Screen.Today.route) {
                TodayScreen(
                    onEditTask = { id -> editTaskId = id; showAddDialog = true },
                    onAddTask = { showAddDialog = true }
                )
            }
            composable(Screen.AllTasks.route) {
                AllTasksScreen(
                    onEditTask = { id -> editTaskId = id; showAddDialog = true }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    onEditTask = { id -> editTaskId = id; showAddDialog = true }
                )
            }
            composable(Screen.Archive.route) {
                ArchiveScreen()
            }
        }
    }

    // Global Add/Edit dialog overlay
    if (showAddDialog) {
        AddTaskDialog(
            editTaskId = editTaskId,
            onDismiss = { showAddDialog = false; editTaskId = null }
        )
    }
}
