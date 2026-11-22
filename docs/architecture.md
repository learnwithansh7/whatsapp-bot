# System Architecture

## Component Overview

The application follows Android MVVM architecture powered by Jetpack Compose and Room Persistence:

```mermaid
flowchart TD
    subgraph SystemLayer [Android OS]
        WA["WhatsApp / WhatsApp Business"] --> Notif["Missed Call Notification"]
        Notif --> NLS["WhatsAppNotificationListenerService"]
    end

    subgraph CoreEngine [Core Services]
        NLS --> Engine["HumanSimulationEngine"]
        Engine --> Cooldown{"Cooldown Check"}
        Cooldown -->|Safe| Delay["8-12s Reaction Jitter"]
        Delay --> Typing["Keystroke Duration Pause"]
        Typing --> Dispatcher["AutoReplyDispatcher"]
    end

    subgraph DataLayer [Storage]
        DB[(Room SQLite DB)]
        Engine <--> DB
        Dispatcher --> DB
    end

    subgraph UILayer [User Interface]
        UI["Jetpack Compose UI (Dashboard, Templates, Settings, History)"]
        UI <--> DB
    end
```

## Core Modules

1. **`WhatsAppNotificationListenerService`**:
   Binds to Android's notification subsystem and filters for `com.whatsapp` and `com.whatsapp.w4b` missed call notifications.

2. **`HumanSimulationEngine`**:
   Orchestrates delays, typing pauses, and manual cancellation safeguards.

3. **`AutoReplyDispatcher`**:
   Extracts `RemoteInput` from notification actions to deliver responses back into WhatsApp.

4. **`AppDatabase`**:
   Stores active templates, system settings, and an audit trail of all intercepted calls.
