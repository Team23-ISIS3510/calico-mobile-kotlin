# 🚀 Hot Slots Feature - Integration Steps

## Prerequisites

- ✅ All files created in their correct packages
- ✅ API endpoint available: `GET /tutors/{tutorId}/hot-slots`
- ✅ Android API level 24+
- ✅ Dependencies: Retrofit, Coroutines, Compose already in project

---

## Step-by-Step Integration

### Phase 1: Build & Compile

```bash
# 1. Open the project in Android Studio
# 2. Let Gradle sync all dependencies
./gradlew clean build

# If build fails, check:
# - Kotlin version compatibility
# - Compose dependency versions
# - Import statements in new files
```

### Phase 2: Create ViewModel Factory

Create file: `app/src/main/java/com/calico/tutor/ui/viewmodel/HotSlotsViewModelFactory.kt`

```kotlin
package com.calico.tutor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.calico.tutor.domain.usecase.GetHotSlotsAnalysisUseCase

class HotSlotsViewModelFactory(
    private val getHotSlotsAnalysisUseCase: GetHotSlotsAnalysisUseCase
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HotSlotsViewModel::class.java)) {
            return HotSlotsViewModel(getHotSlotsAnalysisUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
```

### Phase 3: Add Navigation Route

In your `MainScreen.kt` or navigation setup:

```kotlin
import androidx.navigation.compose.composable
import androidx.compose.ui.platform.LocalContext

// Inside your NavHost setup
composable(
    route = "hotSlots/{tutorId}",
    arguments = listOf(navArgument("tutorId") { type = NavType.StringType })
) { backStackEntry ->
    val tutorId = backStackEntry.arguments?.getString("tutorId") ?: ""
    val context = LocalContext.current
    
    // Get use case from ServiceLocator
    val useCase = ServiceLocator.getHotSlotsAnalysisUseCase(context)
    
    // Create ViewModel with factory
    val viewModel: HotSlotsViewModel = viewModel(
        factory = HotSlotsViewModelFactory(useCase)
    )
    
    // Show screen
    HotSlotsRecommendationScreen(
        viewModel = viewModel,
        tutorId = tutorId,
        onDismiss = {
            navController.popBackStack()
        }
    )
}
```

### Phase 4: Integration Point 1 - Availability Screen

In `AvailabilityScreen.kt`:

```kotlin
@Composable
fun AvailabilityScreen(tutorId: String, navController: NavController) {
    var showHotSlotsButton by remember { mutableStateOf(false) }
    
    // Check if no availability is set
    LaunchedEffect(tutorId) {
        // You might check from your ViewModel
        showHotSlotsButton = true // Set based on actual data
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Existing availability list...
        
        // Add recommendation banner
        if (showHotSlotsButton) {
            RecommendationBanner(
                onClick = {
                    navController.navigate("hotSlots/$tutorId")
                }
            )
        }
    }
}

@Composable
fun RecommendationBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        backgroundColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "💡 Boost Your Bookings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "View recommended time slots from last week",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text("→", fontSize = 20.sp)
        }
    }
}
```

### Phase 5: Integration Point 2 - Home Screen

In `HomeScreen.kt`:

```kotlin
@Composable
fun HomeScreen(tutorId: String, navController: NavController) {
    var hotSlotsData by remember { mutableStateOf<HotSlotsAnalysis?>(null) }
    
    LaunchedEffect(tutorId) {
        // Load hot slots on home screen
        val useCase = ServiceLocator.getHotSlotsAnalysisUseCase(LocalContext.current)
        when (val result = useCase(tutorId)) {
            is Result.Success -> hotSlotsData = result.data
            is Result.Error -> Log.e("HomeScreen", result.message)
        }
    }
    
    Column {
        // ... existing home content ...
        
        // Show hot slots widget
        hotSlotsData?.let { data ->
            HotSlotsWidget(
                analysis = data,
                onClick = { navController.navigate("hotSlots/$tutorId") }
            )
        }
    }
}

@Composable
fun HotSlotsWidget(analysis: HotSlotsAnalysis, onClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🔥 Hot Slots This Week", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                analysis.hotSlots.take(3).forEach { slot ->
                    Badge(
                        modifier = Modifier.padding(end = 8.dp),
                        backgroundColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(slot.slotStart.takeLast(5), fontSize = 10.sp)
                    }
                }
            }
            Text("${analysis.totalSessionsLastWeek} bookings last week", fontSize = 12.sp)
        }
    }
}
```

### Phase 6: Integration Point 3 - Profile Screen

In `ProfileScreen.kt`:

```kotlin
// Add a menu item to view recommendations
Button(
    onClick = { navController.navigate("hotSlots/$tutorId") },
    modifier = Modifier.fillMaxWidth()
) {
    Text("🔥 View Hot Slots Recommendations")
}
```

### Phase 7: Testing

```bash
# Run unit tests
./gradlew test

# Run specific test
./gradlew test --tests "*HotSlots*"

# Run instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest

# Check for build warnings
./gradlew build --warn
```

### Phase 8: Manual Testing Checklist

- [ ] App builds without errors
- [ ] Navigate to hot-slots screen
- [ ] Data loads from API
- [ ] Loading spinner shows
- [ ] Slots display with correct times
- [ ] Availability badges show colors
- [ ] Summary card shows period correctly
- [ ] Error state displays if API fails
- [ ] No memory leaks (check with Profiler)
- [ ] Works offline/online transitions

---

## Debugging Tips

### If API Call Fails

Check Logcat for tag `"AvailabilityRepo"`:

```
D/AvailabilityRepo: Cargando análisis de hot slots para tutor: tutor_123
E/AvailabilityRepo: HTTP 404 cargando hot slots: Not Found
```

**Solutions:**
- Verify tutorId is correct
- Check API endpoint URL in RetrofitClient
- Verify authentication token is sent (check Interceptors)

### If Data Not Showing

Check Logcat for tag `"HotSlotsViewModel"`:

```
D/HotSlotsViewModel: Hot slots cargados exitosamente
```

**Solutions:**
- Verify JSON response matches DTO fields
- Check @SerializedName annotations
- Enable JSON logging: `HttpLoggingInterceptor` with BODY level

### If UI Not Rendering

**Solutions:**
- Check if `LaunchedEffect` is being called
- Verify StateFlow is emitting values
- Check Material 3 theme compatibility
- Review Compose version in build.gradle

### Enable Verbose Logging

In `RetrofitClient.kt`, enable logging:

```kotlin
val httpClient = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY  // See all requests/responses
    })
    // ... other interceptors
    .build()
```

---

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| Compile error: `HotSlotsAnalysis not found` | Verify file created in `domain/model/` |
| Runtime error: `ServiceLocator.getHotSlotsAnalysisUseCase` not found | Verify ServiceLocator updated |
| API returns 401 | Check token authentication |
| Empty hot slots list | Check if bookings exist in last 7 days |
| UI not updating after API call | Verify StateFlow subscription in composable |
| Memory leak | Verify viewModelScope is used, not GlobalScope |

---

## Optional Enhancements

### 1. Add Caching

```kotlin
// In AvailabilityRepositoryImpl
private var cachedHotSlots: Pair<String, HotSlotsAnalysis>? = null

override suspend fun getHotSlotsAnalysis(tutorId: String): Result<HotSlotsAnalysis> {
    cachedHotSlots?.let { (id, data) ->
        if (id == tutorId) return Result.Success(data)
    }
    // ... fetch from API
}
```

### 2. Add Pull-to-Refresh

```kotlin
var isRefreshing by remember { mutableStateOf(false) }

PullRefreshIndicator(
    isRefreshing,
    modifier = Modifier.align(Alignment.TopCenter)
)

Button(onClick = {
    isRefreshing = true
    viewModel.loadHotSlots(tutorId)
})
```

### 3. Add Slot Comparison

```kotlin
// Show previous week's slots for comparison
fun loadHistoricalComparison()
```

### 4. Add Notifications

```kotlin
// Notify tutor when hot slot appears
NotificationManager.notify(
    "Hot slot ${slot.slotStart} has 5 bookings!"
)
```

---

## Deployment Checklist

- [ ] All files created with correct packages
- [ ] API endpoint tested and working
- [ ] Navigation routes added
- [ ] ViewModel factory created
- [ ] Error handling working
- [ ] Unit tests written and passing
- [ ] UI tested on multiple devices
- [ ] Performance tested (no lag)
- [ ] Memory leaks checked
- [ ] Logging configured
- [ ] Documentation updated
- [ ] Team notified of new feature

---

## Support

**Issues or Questions?**
- Review `HOT_SLOTS_FEATURE.md` for detailed docs
- Check `QUICK_REFERENCE.md` for shortcuts
- Review `.github/copilot-instructions.md` for project conventions
- Run test suite: `./gradlew test`

---

**Next:** Start with Phase 1 - Build & Compile, then proceed sequentially.
