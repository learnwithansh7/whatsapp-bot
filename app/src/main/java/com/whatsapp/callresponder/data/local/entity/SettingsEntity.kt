package com.whatsapp.callresponder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isMasterEnabled: Boolean = true,
    val minReactionDelaySec: Int = 8,
    val maxReactionDelaySec: Int = 12,
    val isSimulateTypingEnabled: Boolean = true,
    val wordsPerMinute: Int = 40,
    val cooldownMinutes: Int = 30,
    val isAbortOnManualActivityEnabled: Boolean = true
)
