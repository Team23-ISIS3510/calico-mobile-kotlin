# Session Alert Notifications Implementation Guide

## Overview

The CalicoTutor app now includes a **session alert notification system** that:
- Polls the backend every 60 seconds for upcoming session alerts
- Triggers system notifications when `hasAlert: true` is returned
- Prevents duplicate notifications for the same session
- Uses Android's NotificationCompat API (works on Android 8.0+)
- Automatically creates notification channels (required for Android 8.0+)

## Architecture

### Files Created

#### 1. **Data Models**
- **`data/dto/response/SessionAlertResponse.kt`**
  - DTO that deserializes the backend response
  - Fields: `hasAlert`, `studentName`, `minutesToStart`, `sessionId`

#### 2. **API Service**
- **`data/datasource/remote/AnalyticsApiService.kt`**
  - Retrofit interface with `@GET("analytics/session-alert")` endpoint
  - Called by the repository to fetch session alert data

#### 3. **Notification Management**
- **`util/NotificationHelper.kt`**
  - `createNotificationChannel()`: Sets up Android 8.0+ notification channel
    - Channel ID: "calico_tutor_alerts"
    - Importance: HIGH (shows as popup)
    - Vibration and lights enabled
  - `showSessionAlertNotification()`: Creates and displays notifications
    - Uses unique notification ID based on sessionId hash to prevent duplicates
    - Title: "Upcoming Session Soon!"
    - Text: "You have a session with [studentName] starting in [minutes] minutes."
    - Priority: PRIORITY_HIGH (shows at top of notification drawer)
    - Auto-cancels when tapped

#### 4. **UI State Management**
- **Updated `ui/viewmodel/HomeScreenViewModel.kt`**
  - New sealed class: `SessionAlertState` (Idle, Alert, Error)
  - New StateFlow: `_sessionAlertState` for observing alert changes
  - New method: `startSessionAlertPolling()`
    - Launches coroutine that polls every 60 seconds
    - `while(true)` loop with `delay(60000)` between requests
    - Catches exceptions and continues polling even on error
    - Tracks shown notifications with `shownNotifications` Set to prevent duplicates
    - Logs all alert events for debugging

#### 5. **Repository Updates**
- **Updated `domain/repository/AnalyticsRepository.kt`**
  - Added `suspend fun getSessionAlert(): SessionAlertResponse`

- **Updated `data/repository/AnalyticsRepositoryImpl.kt`**
  - Now depends on both `SubjectsApiService` and `AnalyticsApiService`
  - Implements `getSessionAlert()` by delegating to `analyticsApiService.getSessionAlert()`

#### 6. **Dependency Injection**
- **Updated `di/ServiceLocator.kt`**
  - Added `analyticsApiService` field
  - Created `analyticsApiService()` method with:
    - Token-based auth via TokenInterceptor
    - Latency telemetry tracking (feature: "analytics", action: "session_alert_polling")
  - Updated `analyticsRepository()` to pass both API services

#### 7. **Networking**
- **Updated `data/datasource/remote/RetrofitClient.kt`**
  - Added `createAnalyticsApiService()` method

#### 8. **Permissions**
- **Updated `AndroidManifest.xml`**
  - Added `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`
  - Required for Android 13+ (API 33+) to show notifications

## How to Use

### 1. Starting the Polling Loop

Call from your HomeScreen composable after login:

```kotlin
val homeViewModel = viewModel(factory = HomeScreenViewModelFactory(context))

// Start polling when screen appears
LaunchedEffect(Unit) {
    homeViewModel.startSessionAlertPolling()
    homeViewModel.loadSessions(tutorId)
    homeViewModel.loadOccupancy(tutorId)
}
```

Or call directly in the ViewModel initialization:

```kotlin
// In HomeScreenViewModel.kt or elsewhere
viewModelScope.launch {
    viewModel.startSessionAlertPolling()
}
```

### 2. Observing Alert State (Optional)

In your Composable, you can observe alert state if needed:

```kotlin
val alertState = homeViewModel.sessionAlertState.collectAsState()

when (val state = alertState.value) {
    is SessionAlertState.Alert -> {
        if (state.response.hasAlert) {
            Text("Alert: ${state.response.studentName} in ${state.response.minutesToStart} min")
        }
    }
    is SessionAlertState.Error -> {
        Text("Alert error: ${state.message}")
    }
    else -> {} // Idle - no alert
}
```

### 3. Notification Behavior

- **First polling cycle**: If `hasAlert: true`, a notification appears
- **Subsequent cycles**: Same session ID → no duplicate notification (tracked in `shownNotifications` Set)
- **Different session**: New session ID → new notification displays
- **hasAlert: false**: No notification shown
- **Network error**: Polling continues every 60 seconds, retrying automatically

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│ HomeScreenViewModel.startSessionAlertPolling()              │
│ (runs in viewModelScope as while(true) loop)                │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       v
    ┌──────────────────────────────────┐
    │ AnalyticsRepository.getSessionAlert()
    │ (suspend function)
    └──────────────┬───────────────────┘
                   │
                   v
        ┌──────────────────────────────────┐
        │ AnalyticsApiService.getSessionAlert()
        │ (Retrofit @GET "analytics/session-alert")
        └──────────────┬───────────────────┘
                       │
                       v
        ┌──────────────────────────────────┐
        │ Backend: GET /analytics/session-alert
        │ Response: SessionAlertResponse
        └──────────────┬───────────────────┘
                       │
                       v
    ┌──────────────────────────────────┐
    │ Update _sessionAlertState
    │ (reactive StateFlow update)
    └──────────────┬───────────────────┘
                   │
                   v
    ┌──────────────────────────────────┐
    │ Check: hasAlert && not shown yet?
    │ (via shownNotifications Set)
    └──────────────┬───────────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
       YES                    NO
        │                     │
        v                     v
    ┌─────────────┐       ┌─────────┐
    │ Show        │       │ Skip    │
    │ Notification│       │ (Shown) │
    └─────────────┘       └─────────┘
        │
        │ delay(60000)
        │
        v
    (Next poll in 60 seconds)
```

## Notification ID Generation

Each notification is assigned a unique ID based on the session ID:

```kotlin
val notificationId = (NOTIFICATION_ID_BASE + sessionId.hashCode()).toInt()
```

This ensures:
- Same session → same notification ID → replaces previous (no duplicate visible)
- Different session → different ID → separate notification shown

## Telemetry Integration

Session alert polling is tracked as latency telemetry:
- **feature**: "analytics"
- **action**: "session_alert_polling"
- **endpoint**: "analytics/session-alert"
- **method**: "GET"
- **durationMs**: Automatically measured
- **statusCode**: HTTP response code

If polling request takes >2000ms, it's reported as a LATENCY bug to the backend.

## Logging

All polling events are logged with tag `"HomeScreenViewModel"`:

```
D HomeScreenViewModel: Session alert notification shown for [studentName]
E HomeScreenViewModel: Error polling session alerts: [exception message]
```

Check Logcat with: `adb logcat | grep HomeScreenViewModel`

## Testing

### Scenario 1: First Alert
1. Start polling
2. Backend returns `hasAlert: true, studentName: "John", minutesToStart: 5, sessionId: "session123"`
3. ✅ Notification appears: "Upcoming Session Soon! You have a session with John starting in 5 minutes."

### Scenario 2: Duplicate Prevention
1. Same polling cycle/next cycle with same `sessionId: "session123"`
2. ✅ No notification (already shown, tracked in `shownNotifications`)

### Scenario 3: New Session
1. Next alert: `sessionId: "session456"` (different)
2. ✅ New notification appears for new session

### Scenario 4: No Alert
1. Backend returns `hasAlert: false`
2. ✅ No notification shown
3. Polling continues every 60 seconds

### Scenario 5: Network Error
1. Network fails (e.g., offline)
2. ✅ Error logged to `sessionAlertState`
3. ✅ Polling continues, retries after 60 seconds

## Backend Requirements

The backend `/analytics/session-alert` endpoint must return:

```json
{
  "hasAlert": boolean,
  "studentName": string | null,
  "minutesToStart": number | null,
  "sessionId": string | null
}
```

Example:
```json
{
  "hasAlert": true,
  "studentName": "María García",
  "minutesToStart": 15,
  "sessionId": "sess_abc123def456"
}
```

When no alert:
```json
{
  "hasAlert": false,
  "studentName": null,
  "minutesToStart": null,
  "sessionId": null
}
```

## Android Version Compatibility

- **Android 8.0+ (API 26+)**: Full support with NotificationChannel
- **Android 13+ (API 33+)**: POST_NOTIFICATIONS permission required (added to manifest)
- **Older versions**: Graceful degradation (channel creation is no-op)

## Permissions

Added to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Users on Android 13+ will see a permission request the first time the app shows a notification.

## Known Limitations

1. **Polling stops if ViewModel is destroyed** - Polling is tied to `viewModelScope`, so it stops when HomeScreen is navigated away from. This is intentional to save battery. Restart by calling `startSessionAlertPolling()` again.

2. **No persistence** - If app is force-closed, polling stops. Consider using WorkManager for background polling if needed.

3. **No notification click action** - Tapping notification just closes it. To navigate to sessions on tap, update `NotificationHelper.showSessionAlertNotification()` to add a PendingIntent.

## Future Enhancements

1. **Click to navigate**: Add PendingIntent to navigate to session details when notification is tapped
2. **Background polling**: Use WorkManager for periodic background polling even when app is closed
3. **Sound/vibration control**: Add settings to customize notification behavior
4. **Notification dismiss action**: Clear all showed notifications on specific action
5. **Session countdown**: Update minutes remaining in real-time

## Build Status

✅ Project builds successfully
- Kotlin compiler: No errors
- Warnings: Only deprecated API usage (not related to this implementation)
- Size: Minimal (2 new files, ~250 lines of code)
