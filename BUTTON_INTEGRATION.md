# 🔌 Hot Slots Button Integration Guide

## What Was Added

✅ **Button added to AvailabilityScreen** - "🔥 View Hot Slots Recommendations"  
✅ **HotSlotsViewModelFactory created** - For ViewModel instantiation  
✅ **Integration point ready** - Just need to wire navigation

---

## How to Complete Integration (2 Steps)

### Step 1: Add Navigation Route

In your **MainScreen.kt** or wherever you manage navigation routes:

```kotlin
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext

// Inside your NavHost { ... }

composable(
    route = "hotSlots/{tutorId}",
    arguments = listOf(navArgument("tutorId") { type = NavType.StringType })
) { backStackEntry ->
    val tutorId = backStackEntry.arguments?.getString("tutorId") ?: ""
    val context = LocalContext.current
    
    // Get the use case from ServiceLocator
    val useCase = ServiceLocator.getHotSlotsAnalysisUseCase(context)
    
    // Create ViewModel with factory
    val viewModel: HotSlotsViewModel = viewModel(
        factory = HotSlotsViewModelFactory(useCase)
    )
    
    // Show the hot slots screen
    HotSlotsRecommendationScreen(
        viewModel = viewModel,
        tutorId = tutorId,
        onDismiss = {
            navController.popBackStack()
        }
    )
}
```

### Step 2: Update AvailabilityScreen Call

Wherever you call `AvailabilityScreen()` composable:

**Before:**
```kotlin
AvailabilityScreen(
    context = context,
    tutorId = tutorId,
    onNavigateToEdit = { item -> /* ... */ }
)
```

**After:**
```kotlin
AvailabilityScreen(
    context = context,
    tutorId = tutorId,
    onNavigateToEdit = { item -> /* ... */ },
    onNavigateToHotSlots = {
        navController.navigate("hotSlots/$tutorId")
    }
)
```

---

## What The Button Does

### When Availabilities Are Empty
Shows a centered button:
```
No upcoming availabilities
[🔥 View Hot Slots]
```

### When Availabilities Exist
Shows a button at the bottom of the list:
```
[List of availabilities...]
[🔥 View Hot Slots Recommendations]
```

---

## Files Modified/Created

✅ **Modified:** `AvailabilityScreen.kt`
- Added `onNavigateToHotSlots` parameter
- Added button UI in SeeAvailabilitiesTab
- Shows button in both empty and populated states

✅ **Created:** `HotSlotsViewModelFactory.kt`
- Factory for HotSlotsViewModel instantiation

---

## Testing

Build and run:
```bash
./gradlew clean build
```

Then:
1. Navigate to Availability screen
2. Click "🔥 View Hot Slots Recommendations" button
3. Should navigate to hot-slots screen and load data

---

## If Navigation Fails

If you get "Navigation to hotSlots/$tutorId failed", ensure:
1. ✅ Route added to NavHost
2. ✅ Import `NavType.StringType` for String argument
3. ✅ `navController` is available in scope
4. ✅ Spelling matches exactly: `"hotSlots/{tutorId}"`

---

## Next Steps

1. Add the navigation route
2. Update the AvailabilityScreen call
3. Build and test
4. Done! ✅

**The button is ready - just wire the navigation!**
