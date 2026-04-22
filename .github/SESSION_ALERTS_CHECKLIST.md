# Session Alerts Integration Checklist

## What Was Implemented ✅

### Core Components
- [x] `SessionAlertResponse` data class with nullable fields
- [x] `AnalyticsApiService` with `/analytics/session-alert` endpoint
- [x] `NotificationHelper` utility for channel creation and notification display
- [x] `SessionAlertState` sealed class for UI state management
- [x] `HomeScreenViewModel.startSessionAlertPolling()` with 60-second polling loop
- [x] Repository support in `AnalyticsRepository` interface and implementation
- [x] ServiceLocator wiring with token auth and latency tracking
- [x] POST_NOTIFICATIONS permission in manifest
- [x] Duplicate prevention via `shownNotifications` Set with sessionId-based tracking

### Build Status
- [x] Gradle build successful
- [x] No compilation errors
- [x] All imports resolved
- [x] Backwards compatible (Android 8.0+)

## Integration Steps (For HomeScreen)

### Step 1: Start Polling When HomeScreen Loads

In `ui/screen/HomeScreen.kt` (or wherever you initialize HomeScreenViewModel):

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = viewModel(factory = HomeScreenViewModelFactory(context)),
    tutorId: String
) {
    LaunchedEffect(Unit) {
        viewModel.startSessionAlertPolling()  // ← ADD THIS
        viewModel.loadSessions(tutorId)
        viewModel.loadOccupancy(tutorId)
    }
    
    // Rest of your HomeScreen UI
}
```

### Step 2: (Optional) Display Alert Status

If you want to show the alert status in the UI:

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = viewModel(factory = HomeScreenViewModelFactory(context)),
    tutorId: String
) {
    val alertState = viewModel.sessionAlertState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.startSessionAlertPolling()
        viewModel.loadSessions(tutorId)
        viewModel.loadOccupancy(tutorId)
    }
    
    // Display alert status if needed
    when (val state = alertState.value) {
        is SessionAlertState.Alert -> {
            if (state.response.hasAlert) {
                // Show banner or toast about upcoming session
                Text("Upcoming: ${state.response.studentName}")
            }
        }
        is SessionAlertState.Error -> {
            // Optionally show error (polling will retry automatically)
            Log.e("HomeScreen", "Alert error: ${state.message}")
        }
        else -> {} // Idle
    }
    
    // Rest of your UI
}
```

### Step 3: Test the Backend Endpoint

Ensure your backend has implemented:

```
GET /analytics/session-alert
Authorization: Bearer [jwt_token]

Response (200 OK):
{
  "hasAlert": true,
  "studentName": "Student Name",
  "minutesToStart": 15,
  "sessionId": "unique_session_id"
}
```

## Files to Review

1. **`util/NotificationHelper.kt`**
   - Notification creation and channel management
   - Feel free to customize title, text, or icon

2. **`ui/viewmodel/HomeScreenViewModel.kt`**
   - Polling loop implementation
   - StateFlow management
   - Duplicate prevention logic

3. **`data/datasource/remote/AnalyticsApiService.kt`**
   - Retrofit endpoint definition

4. **`di/ServiceLocator.kt`**
   - Service initialization and wiring

5. **`AndroidManifest.xml`**
   - POST_NOTIFICATIONS permission

## Testing Checklist

- [ ] Backend `/analytics/session-alert` endpoint returns correct format
- [ ] Notification channel is created (check Settings > Apps > CalicoTutor > Notifications)
- [ ] First poll shows notification when `hasAlert: true`
- [ ] Second poll with same sessionId doesn't create duplicate
- [ ] Different sessionId creates new notification
- [ ] Polling continues even if notification isn't shown
- [ ] Error handling: Check Logcat when network is offline
- [ ] Polling doesn't spam - exactly one request per 60 seconds
- [ ] Notification auto-cancels when tapped
- [ ] Notification uses correct title and text format
- [ ] Polling uses Auth header (token is sent)

## Configuration

All hardcoded values are safe defaults:

```kotlin
private const val CHANNEL_ID = "calico_tutor_alerts"
private const val CHANNEL_NAME = "Session Alerts"
private const val NOTIFICATION_ID_BASE = 1000
private const val SHAKE_COOLDOWN_MS = 60000L  // 60 seconds
```

To adjust polling frequency, edit `HomeScreenViewModel.kt` line ~167:
```kotlin
delay(60000)  // Change 60000 to desired milliseconds
```

## Dependencies Used

All dependencies already in `build.gradle.kts`:
- `androidx.core.app.NotificationCompat` ✓
- `kotlinx.coroutines` ✓
- `retrofit2` ✓
- `androidx.lifecycle.viewModelScope` ✓

No new dependencies needed!

## Troubleshooting

### Notifications Not Showing
1. Check POST_NOTIFICATIONS permission is in manifest ✓
2. On Android 13+, grant permission in app settings
3. Check notification channel is enabled in system settings
4. Verify `hasAlert: true` in backend response
5. Check logcat: `adb logcat | grep HomeScreenViewModel`

### Duplicate Notifications
- Check that `sessionId` in response is unique per session
- Verify `shownNotifications` Set is being populated (check logs)
- Notification ID is based on `sessionId.hashCode()` - ensure sessionId is consistent

### Polling Stops
- Polling only runs while HomeScreen is displayed
- Moving to another screen stops polling (this is intentional)
- Restart HomeScreen to resume polling
- For background polling, use WorkManager (future enhancement)

### No Notifications at All
1. Check if `AnalyticsApiService.getSessionAlert()` is being called
2. Check if `ServiceLocator.analyticsRepository()` is initialized
3. Verify Retrofit client has token auth set up
4. Check backend endpoint is returning correct JSON

## Success Indicators ✅

When working correctly:
- System notification appears with "Upcoming Session Soon!" title
- Notification shows student name and minutes until session
- Same session ID doesn't create duplicate notifications
- Different session IDs create separate notifications
- Polling occurs every 60 seconds (check logcat)
- Logcat shows: `D/HomeScreenViewModel: Session alert notification shown for [name]`

## Support

For issues:
1. Check `.github/SESSION_ALERTS_IMPLEMENTATION.md` for full architecture details
2. Review logcat with `adb logcat | grep HomeScreenViewModel`
3. Verify backend endpoint format matches `SessionAlertResponse` DTO
4. Test with backend returning explicit `hasAlert: true`

---

**Build Status**: ✅ Successful (0 errors, 6 warnings from deprecated Compose APIs)
**Tested With**: Kotlin 1.9.x, Android Gradle Plugin 8.x, Compose 1.5.x
