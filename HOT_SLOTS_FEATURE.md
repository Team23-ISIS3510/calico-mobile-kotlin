# Hot Slots Feature - Implementation Guide

## Overview

The **Hot Slots Recommendation** feature helps tutors discover the most-booked time slots from the past week and intelligently suggests availability set times. It analyzes booking patterns to maximize potential sessions.

## Architecture

### 1. DTOs (Data Transfer Objects)
- **File:** `data/dto/response/HotSlotsAnalysisResponse.kt`
- Contains:
  - `HotSlotsAnalysisResponseDto` - API response wrapper
  - `HotSlotDto` - Individual slot data

### 2. Domain Models
- **File:** `domain/model/HotSlotsModels.kt`
- Contains:
  - `HotSlotsAnalysis` - Domain-layer model
  - `HotSlot` - Individual slot in domain layer

### 3. Data Mapper
- **File:** `data/mapper/HotSlotsMapper.kt`
- Converts DTOs → Domain Models
- Extensions: `HotSlotDto.toDomain()` and `HotSlotsAnalysisResponseDto.toDomain()`

### 4. API Service
- **File:** `data/datasource/remote/AvailabilityApiService.kt`
- New method: `getHotSlotsAnalysis(tutorId: String): HotSlotsAnalysisResponseDto`
- Endpoint: `GET /tutors/{tutorId}/hot-slots`

### 5. Repository
- **Interface:** `domain/repository/AvailabilityRepository.kt`
- New method: `getHotSlotsAnalysis(tutorId: String): Result<HotSlotsAnalysis>`
- **Implementation:** `data/repository/AvailabilityRepositoryImpl.kt`
- Handles error mapping and logging

### 6. Use Case
- **File:** `domain/usecase/GetHotSlotsAnalysisUseCase.kt`
- Single responsibility: fetch hot slots data
- Callable via `invoke(tutorId: String)`

### 7. ViewModel
- **File:** `ui/viewmodel/HotSlotsViewModel.kt`
- State: `HotSlotsState` (sealed class with Loading, Success, Error, Idle)
- Exposes: `hotSlotsState: StateFlow<HotSlotsState>`
- Method: `loadHotSlots(tutorId: String)`

### 8. Compose Screen
- **File:** `ui/screen/HotSlotsRecommendationScreen.kt`
- Components:
  - `HotSlotsRecommendationScreen()` - Main container
  - `HotSlotsContent()` - Content display
  - `SummaryCard()` - Period & session count
  - `HotSlotCard()` - Individual slot card
  - `StatusBadge()` - Availability indicator

### 9. Dependency Injection
- **File:** `di/ServiceLocator.kt`
- New: `getHotSlotsAnalysisUseCase(context: Context)`
- Wires: Use Case → Repository → API Service

## Integration Steps

### Step 1: ViewModel Factory
Create a factory if using ViewModels with parameters:

```kotlin
class HotSlotsViewModelFactory(
    private val getHotSlotsAnalysisUseCase: GetHotSlotsAnalysisUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HotSlotsViewModel(getHotSlotsAnalysisUseCase) as T
    }
}
```

### Step 2: Navigation Integration
Add to your navigation graph (MainScreen.kt or wherever routing is managed):

```kotlin
// In your NavController setup
composable("hotSlots/{tutorId}") { backStackEntry ->
    val tutorId = backStackEntry.arguments?.getString("tutorId") ?: ""
    val context = LocalContext.current
    val useCase = ServiceLocator.getHotSlotsAnalysisUseCase(context)
    val viewModel: HotSlotsViewModel = viewModel(
        factory = HotSlotsViewModelFactory(useCase)
    )
    
    HotSlotsRecommendationScreen(
        viewModel = viewModel,
        tutorId = tutorId,
        onDismiss = { navController.popBackStack() }
    )
}
```

### Step 3: Show as Modal/Screen When No Availability
In `AvailabilityViewModel` or relevant screen:

```kotlin
// When checking if availability is empty
if (availabilities.isEmpty() && isFirstLoad) {
    // Navigate to hot slots
    navController.navigate("hotSlots/$tutorId")
}
```

### Step 4: Alternative: Show as Sheet
Use `ModalBottomSheet` if you want a dismissible recommendation:

```kotlin
var showHotSlots by remember { mutableStateOf(false) }

if (showHotSlots) {
    ModalBottomSheet(onDismissRequest = { showHotSlots = false }) {
        val context = LocalContext.current
        val useCase = ServiceLocator.getHotSlotsAnalysisUseCase(context)
        val viewModel: HotSlotsViewModel = viewModel(
            factory = HotSlotsViewModelFactory(useCase)
        )
        HotSlotsRecommendationScreen(viewModel, tutorId) { showHotSlots = false }
    }
}
```

## Usage Example

```kotlin
@Composable
fun AvailabilityScreenWithHotSlots(tutorId: String) {
    val context = LocalContext.current
    val useCase = ServiceLocator.getHotSlotsAnalysisUseCase(context)
    val hotSlotsViewModel: HotSlotsViewModel = viewModel(
        factory = HotSlotsViewModelFactory(useCase)
    )
    
    LaunchedEffect(tutorId) {
        hotSlotsViewModel.loadHotSlots(tutorId)
    }
    
    val hotSlotsState = hotSlotsViewModel.hotSlotsState.collectAsState()
    
    Column {
        // Your availability list...
        
        // Recommendation section
        when (val state = hotSlotsState.value) {
            is HotSlotsState.Success -> {
                HotSlotsRecommendationSection(state.analysis)
            }
            is HotSlotsState.Error -> {
                // Silent fail or show error banner
                Log.w("HotSlots", state.message)
            }
            else -> {} // Loading or Idle
        }
    }
}
```

## API Response Format

```json
{
  "tutorId": "tutor_123",
  "analysisStartDate": "2024-01-08T00:00:00Z",
  "analysisEndDate": "2024-01-15T00:00:00Z",
  "totalSessionsLastWeek": 12,
  "hotSlots": [
    {
      "slotStart": "2024-01-15T14:00:00Z",
      "slotEnd": "2024-01-15T15:00:00Z",
      "bookingCount": 5,
      "tutorAvailability": "not_available"
    },
    {
      "slotStart": "2024-01-15T10:00:00Z",
      "slotEnd": "2024-01-15T11:00:00Z",
      "bookingCount": 4,
      "tutorAvailability": "available",
      "availabilityStart": "2024-01-15T10:00:00Z",
      "availabilityEnd": "2024-01-15T11:00:00Z"
    }
  ]
}
```

## Data Flow

```
UI (HotSlotsRecommendationScreen)
    ↓ calls loadHotSlots(tutorId)
ViewModel (HotSlotsViewModel)
    ↓ invokes
Use Case (GetHotSlotsAnalysisUseCase)
    ↓ calls
Repository (AvailabilityRepository)
    ↓ calls
API Service (AvailabilityApiService)
    ↓ HTTP GET /tutors/{tutorId}/hot-slots
Backend API
    ↓ returns HotSlotsAnalysisResponseDto
Mapper (HotSlotsMapper)
    ↓ converts to domain model
ViewModel (updates state)
    ↓ emits HotSlotsState.Success
UI (observes StateFlow, recomposes)
```

## Error Handling

The implementation handles:
- **Network errors:** Caught and wrapped in `Result.Error`
- **HTTP errors:** Specific HTTP codes logged
- **Parsing errors:** JSON deserialization failures
- **State management:** All states (Loading, Success, Error) propagated to UI

Example error state:
```kotlin
HotSlotsState.Error("Error del servidor (500)")
// or
HotSlotsState.Error("Error cargando hot slots")
```

## Customization

### Change Analysis Period
Modify the backend request (add query parameters if API supports):
```kotlin
suspend fun getHotSlotsAnalysis(
    @Path("tutorId") tutorId: String,
    @Query("days") days: Int = 7
): HotSlotsAnalysisResponseDto
```

### Customize Slot Display
Edit `HotSlotCard()` composable:
- Change colors in `StatusBadge()`
- Add more details (date, day of week)
- Add action buttons (e.g., "Set Availability")

### Add Filtering
In ViewModel, filter slots before updating state:
```kotlin
val filtered = analysis.hotSlots.filter { it.bookingCount > threshold }
_hotSlotsState.value = HotSlotsState.Success(
    analysis.copy(hotSlots = filtered)
)
```

## Testing

Example unit test:
```kotlin
@Test
fun `loadHotSlots updates state with success`() = runTest {
    val mockUseCase = mockk<GetHotSlotsAnalysisUseCase>()
    val expectedAnalysis = HotSlotsAnalysis(...)
    
    coEvery { mockUseCase.invoke(any()) } returns Result.Success(expectedAnalysis)
    
    val viewModel = HotSlotsViewModel(mockUseCase)
    viewModel.loadHotSlots("tutor_123")
    
    advanceUntilIdle()
    
    val state = viewModel.hotSlotsState.value
    assertTrue(state is HotSlotsState.Success)
    assertEquals(expectedAnalysis, (state as HotSlotsState.Success).analysis)
}
```

## Troubleshooting

### Feature Not Showing
- Verify API endpoint returns 200 with valid JSON
- Check `tutorId` parameter is correctly passed
- Review Logcat for error logs tagged "HotSlotsViewModel"

### Empty Hot Slots
- Backend returns empty array if no bookings in analysis period
- UI gracefully handles: show empty state or informational message

### Mapping Issues
- Ensure DTO field names match API response (`@SerializedName` annotations)
- Verify date formats are ISO 8601 (handled by Gson)

### Performance
- Hot slots fetch is lightweight (single API call)
- Compose recomposition optimized via `StateFlow`
- No caching by default; add if frequent access

---

**Files Created/Modified:**
- ✅ Created: `HotSlotsAnalysisResponse.kt` (DTOs)
- ✅ Created: `HotSlotsModels.kt` (Domain models)
- ✅ Created: `HotSlotsMapper.kt` (Mapper)
- ✅ Created: `GetHotSlotsAnalysisUseCase.kt` (Use case)
- ✅ Created: `HotSlotsViewModel.kt` (ViewModel)
- ✅ Created: `HotSlotsRecommendationScreen.kt` (UI)
- ✅ Modified: `AvailabilityApiService.kt` (Added endpoint)
- ✅ Modified: `AvailabilityRepository.kt` (Added interface method)
- ✅ Modified: `AvailabilityRepositoryImpl.kt` (Added implementation)
- ✅ Modified: `ServiceLocator.kt` (Wired dependencies)
