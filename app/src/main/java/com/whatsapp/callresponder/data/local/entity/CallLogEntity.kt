package com.whatsapp.callresponder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callerName: String,
    val callerNumber: String? = null,
    val callTimestamp: Long = System.currentTimeMillis(),
    val replyMessage: String? = null,
    val status: String, // SENT, SKIPPED_COOLDOWN, CANCELLED_MANUAL, FAILED
    val delaySeconds: Int = 0,
    val typingSeconds: Int = 0,
    val errorDetails: String? = null
)
