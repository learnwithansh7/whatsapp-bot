# Anti-Ban Guidelines & Human Simulation Architecture

## Why Instant Replies Cause WhatsApp Bans

Automated WhatsApp tools that fire instant 1-second auto-replies trigger algorithmic flags on WhatsApp telemetry servers:
1. **Unrealistic Reaction Time**: Humans take at least 5 to 15 seconds to notice a missed call, unlock their phone, and initiate a response.
2. **Instant Keystroke Velocity**: Transmitting a 100-character message in 0 milliseconds proves mechanical dispatch.
3. **Burst Frequency**: Replying to every ringing attempt when a caller dials 5 times in 2 minutes creates spam reports.

## How This App Mitigates Detection

### 1. Randomized Reaction Jitter (8–12 seconds)
Upon receiving a missed call notification, the engine enters an idle sleep state for a randomized duration between 8 and 12 seconds.

### 2. Typing Pace Emulation (~40 Words Per Minute)
The system calculates a realistic typing duration proportional to the message character length:
```kotlin
val estimatedMs = (messageText.length * 40L)
val typingDurationMs = estimatedMs.coerceIn(2500L, 6500L)
```
This ensures WhatsApp observes a legitimate conversational timeline.

### 3. Anti-Spam Caller Cooldown
The app enforces a persistent SQLite cooldown timer (default: 30 minutes). If the same caller dials multiple times, only one automated response is dispatched within that window.

### 4. Cancellation on Manual Activity
If the user unlocks their phone and calls the person back or sends a message manually during the 8–10 second delay, the scheduled auto-reply is immediately aborted.
