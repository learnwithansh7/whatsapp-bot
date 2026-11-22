package com.whatsapp.callresponder.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.whatsapp.callresponder.WhatsAppResponderApp
import com.whatsapp.callresponder.ui.dashboard.DashboardScreen
import com.whatsapp.callresponder.ui.history.HistoryScreen
import com.whatsapp.callresponder.ui.settings.SettingsScreen
import com.whatsapp.callresponder.ui.templates.TemplateEditorScreen
import com.whatsapp.callresponder.ui.theme.WhatsAppResponderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as WhatsAppResponderApp
        val database = app.database

        setContent {
            WhatsAppResponderTheme {
                MainAppScaffold(database)
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Templates : Screen("templates", "Messages", Icons.Default.ChatBubble)
    object Settings : Screen("settings", "Anti-Ban", Icons.Default.Security)
    object History : Screen("history", "History", Icons.Default.History)
}

@Composable
fun MainAppScaffold(database: com.whatsapp.callresponder.data.local.AppDatabase) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = listOf(
        Screen.Dashboard,
        Screen.Templates,
        Screen.Settings,
        Screen.History
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    db = database,
                    onNavigateToTemplates = {
                        navController.navigate(Screen.Templates.route)
                    }
                )
            }
            composable(Screen.Templates.route) {
                TemplateEditorScreen(db = database)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(db = database)
            }
            composable(Screen.History.route) {
                HistoryScreen(db = database)
            }
        }
    }
}
