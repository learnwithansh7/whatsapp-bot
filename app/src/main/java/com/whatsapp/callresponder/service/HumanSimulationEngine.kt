package com.whatsapp.callresponder.service

import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import com.whatsapp.callresponder.data.local.AppDatabase
import com.whatsapp.callresponder.data.local.entity.CallLogEntity
import com.whatsapp.callresponder.util.TemplateResolver
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class HumanSimulationEngine(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Tracks pending jobs by caller name to cancel if user acts manually
    private val pendingReplies = ConcurrentHashMap<String, Job>()

    /**
     * Cancel any pending auto-reply if the user calls or replies manually
     */
    fun cancelPendingForCaller(callerName: String) {
        val job = pendingReplies.remove(callerName)
        if (job != null && job.isActive) {
            job.cancel()
            Log.i(TAG, "Cancelled pending auto-reply for $callerName due to user manual action.")
            scope.launch {
                db.logDao().insertLog(
                    CallLogEntity(
                        callerName = callerName,
                        status = "CANCELLED_MANUAL",
                        errorDetails = "User picked up phone or replied manually during delay"
                    )
                )
            }
        }
    }

    /**
     * Process intercepted missed call with Human-Like Reaction & Typing Simulation
     */
    fun processMissedCall(
        sbn: StatusBarNotification,
        callerName: String,
        callerNumber: String?
    ) {
        scope.launch {
            val settings = db.settingsDao().getSettings() ?: return@launch
            if (!settings.isMasterEnabled) {
                Log.i(TAG, "Auto-reply master toggle is OFF. Skipping call from $callerName.")
                return@launch
            }

            // 1. Check Cooldown (Anti-Spam)
            val cooldownMs = settings.cooldownMinutes * 60 * 1000L
            val lastLog = db.logDao().getLastLogForCaller(callerName)
            if (lastLog != null && lastLog.status == "SENT") {
                val timeSinceLast = System.currentTimeMillis() - lastLog.callTimestamp
                if (timeSinceLast < cooldownMs) {
                    val remainingMin = ((cooldownMs - timeSinceLast) / 60000).toInt()
                    Log.i(TAG, "Skipping $callerName: In cooldown ($remainingMin mins remaining).")
                    db.logDao().insertLog(
                        CallLogEntity(
                            callerName = callerName,
                            callerNumber = callerNumber,
                            status = "SKIPPED_COOLDOWN",
                            errorDetails = "Caller is in ${settings.cooldownMinutes}m cooldown"
                        )
                    )
                    return@launch
                }
            }

            // 2. Resolve Active Template
            val activeTemplate = db.templateDao().getActiveTemplate()
            if (activeTemplate == null) {
                Log.w(TAG, "No active template found in database!")
                return@launch
            }
            val resolvedText = TemplateResolver.resolve(activeTemplate.content, callerName)

            // 3. Human Simulation: Step 1 - Reaction Delay (8 to 12 seconds)
            val minDelay = settings.minReactionDelaySec
            val maxDelay = maxOf(minDelay, settings.maxReactionDelaySec)
            val reactionDelaySec = Random.nextInt(minDelay, maxDelay + 1)
            val reactionDelayMs = reactionDelaySec * 1000L

            // 4. Human Simulation: Step 2 - Typing Duration (~40 Words Per Minute)
            val typingDurationMs = if (settings.isSimulateTypingEnabled) {
                // ~40 WPM is ~3.5 chars per second (~280ms per word). Clamped between 2.5s and 6.5s
                val estimatedMs = (resolvedText.length * 40L)
                estimatedMs.coerceIn(2500L, 6500L)
            } else {
                0L
            }
            val typingDurationSec = (typingDurationMs / 1000).toInt()

            Log.i(
                TAG,
                "Scheduling human-like auto-reply to $callerName. Reaction delay: ${reactionDelaySec}s, Typing: ${typingDurationSec}s"
            )

            // Cancel any previously scheduled job for this caller
            pendingReplies[callerName]?.cancel()

            val job = launch {
                try {
                    // Wait the natural human reaction time (8-12 seconds)
                    delay(reactionDelayMs)

                    // Wait the simulated typing duration (so WhatsApp server sees human pace)
                    if (typingDurationMs > 0) {
                        delay(typingDurationMs)
                    }

                    // Dispatch message
                    val success = AutoReplyDispatcher.dispatchReply(
                        context = context,
                        sbn = sbn,
                        replyText = resolvedText,
                        callerNumber = callerNumber
                    )

                    // Record audit log
                    db.logDao().insertLog(
                        CallLogEntity(
                            callerName = callerName,
                            callerNumber = callerNumber,
                            replyMessage = resolvedText,
                            status = if (success) "SENT" else "FAILED",
                            delaySeconds = reactionDelaySec,
                            typingSeconds = typingDurationSec,
                            errorDetails = if (success) null else "Could not execute dispatch intent"
                        )
                    )
                    Log.i(TAG, "Auto-reply completed for $callerName with status: ${if (success) "SENT" else "FAILED"}")
                } catch (e: CancellationException) {
                    Log.i(TAG, "Auto-reply to $callerName was cancelled before sending.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in auto-reply execution for $callerName", e)
                } finally {
                    pendingReplies.remove(callerName)
                }
            }

            pendingReplies[callerName] = job
        }
    }

    companion object {
        private const val TAG = "HumanSimulationEngine"
    }
}
