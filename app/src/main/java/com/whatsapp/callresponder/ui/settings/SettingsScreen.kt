package com.whatsapp.callresponder.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whatsapp.callresponder.data.local.AppDatabase
import com.whatsapp.callresponder.data.local.entity.SettingsEntity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(db: AppDatabase) {
    val coroutineScope = rememberCoroutineScope()
    val settingsState by db.settingsDao().getSettingsFlow().collectAsState(initial = SettingsEntity())
    val settings = settingsState ?: SettingsEntity()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Human Simulation & Anti-Ban", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Configure natural human delays and typing simulation so WhatsApp recognizes your automated replies as genuine human interaction.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Reaction Delay Section (8-12 seconds default)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Reaction Delay (Notice Time)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "How long the app waits after a missed call before beginning to respond (mimics looking at your phone).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Min Delay: ${settings.minReactionDelaySec} seconds", fontWeight = FontWeight.Medium)
                    Slider(
                        value = settings.minReactionDelaySec.toFloat(),
                        onValueChange = { newVal ->
                            coroutineScope.launch {
                                val min = newVal.roundToInt()
                                val max = maxOf(min, settings.maxReactionDelaySec)
                                db.settingsDao().upsertSettings(settings.copy(minReactionDelaySec = min, maxReactionDelaySec = max))
                            }
                        },
                        valueRange = 5f..15f,
                        steps = 9
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Max Delay: ${settings.maxReactionDelaySec} seconds", fontWeight = FontWeight.Medium)
                    Slider(
                        value = settings.maxReactionDelaySec.toFloat(),
                        onValueChange = { newVal ->
                            coroutineScope.launch {
                                val max = newVal.roundToInt()
                                val min = minOf(max, settings.minReactionDelaySec)
                                db.settingsDao().upsertSettings(settings.copy(minReactionDelaySec = min, maxReactionDelaySec = max))
                            }
                        },
                        valueRange = 8f..20f,
                        steps = 11
                    )
                }
            }

            // Typing Simulation Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Simulate Keyboard Typing",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Calculates natural typing duration (~${settings.wordsPerMinute} WPM) based on message length before sending.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.isSimulateTypingEnabled,
                            onCheckedChange = { isEnabled ->
                                coroutineScope.launch {
                                    db.settingsDao().upsertSettings(settings.copy(isSimulateTypingEnabled = isEnabled))
                                }
                            }
                        )
                    }
                }
            }

            // Anti-Spam Cooldown Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Caller Anti-Spam Cooldown",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "If the same contact calls multiple times in a row, only reply once within this interval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Cooldown: ${settings.cooldownMinutes} minutes", fontWeight = FontWeight.Medium)
                    Slider(
                        value = settings.cooldownMinutes.toFloat(),
                        onValueChange = { newVal ->
                            coroutineScope.launch {
                                db.settingsDao().upsertSettings(settings.copy(cooldownMinutes = newVal.roundToInt()))
                            }
                        },
                        valueRange = 5f..120f,
                        steps = 22
                    )
                }
            }

            // Abort on Manual Action
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Cancel on Manual Activity",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Automatically abort auto-reply if you pick up your phone and call or message the contact first during the delay.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.isAbortOnManualActivityEnabled,
                            onCheckedChange = { isEnabled ->
                                coroutineScope.launch {
                                    db.settingsDao().upsertSettings(settings.copy(isAbortOnManualActivityEnabled = isEnabled))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
