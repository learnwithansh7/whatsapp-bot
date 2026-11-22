package com.whatsapp.callresponder.ui.dashboard

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whatsapp.callresponder.data.local.AppDatabase
import com.whatsapp.callresponder.data.local.entity.SettingsEntity
import com.whatsapp.callresponder.ui.theme.WhatsAppGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    db: AppDatabase,
    onNavigateToTemplates: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsState by db.settingsDao().getSettingsFlow().collectAsState(initial = SettingsEntity())
    val logsState by db.logDao().getAllLogs().collectAsState(initial = emptyList())

    val settings = settingsState ?: SettingsEntity()

    val isNotificationAccessGranted = remember {
        mutableStateOf(checkNotificationAccessGranted(context))
    }

    val totalCalls = logsState.size
    val totalSent = logsState.count { it.status == "SENT" }
    val totalCooldown = logsState.count { it.status == "SKIPPED_COOLDOWN" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WhatsApp Call Auto-Responder", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Alert Banner
            if (!isNotificationAccessGranted.value) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Permission Required",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Android requires 'Notification Access' permission so the app can detect WhatsApp missed calls.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                isNotificationAccessGranted.value = checkNotificationAccessGranted(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Grant Notification Access")
                        }
                    }
                }
            }

            // Master Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Auto-Reply Service",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (settings.isMasterEnabled) "Active • Watching missed calls" else "Paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (settings.isMasterEnabled) WhatsAppGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isMasterEnabled,
                        onCheckedChange = { isEnabled ->
                            coroutineScope.launch {
                                db.settingsDao().upsertSettings(settings.copy(isMasterEnabled = isEnabled))
                            }
                        }
                    )
                }
            }

            // Human Delay & Anti-Ban Indicator Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Human Simulation Active",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Delays reply by ${settings.minReactionDelaySec}-${settings.maxReactionDelaySec}s with realistic typing simulation (~${settings.wordsPerMinute} WPM) so WhatsApp recognizes you as human.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Quick Stats
            Text("Activity Overview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Total Calls", totalCalls.toString(), Icons.Default.PhoneMissed, Modifier.weight(1f))
                StatCard("Replies Sent", totalSent.toString(), Icons.Default.Send, Modifier.weight(1f))
                StatCard("In Cooldown", totalCooldown.toString(), Icons.Default.Timer, Modifier.weight(1f))
            }

            // Active Template Preview Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Auto-Reply Message", fontWeight = FontWeight.Bold)
                        TextButton(onClick = onNavigateToTemplates) {
                            Text("Change")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val activeTemplate by produceState<String?>(initialValue = "Loading...") {
                        val t = db.templateDao().getActiveTemplate()
                        value = t?.content ?: "No template active"
                    }
                    Text(
                        activeTemplate ?: "No template active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun checkNotificationAccessGranted(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(pkgName)
}
