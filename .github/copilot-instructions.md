# Copilot Instructions for Calico Mobile (Kotlin)

This document guides Copilot and other AI assistants working on the **Calico Tutor Mobile** project—a tutoring platform Android app built with Kotlin, Jetpack Compose, and a multi-layered architecture supporting offline-first functionality.

## Quick Reference

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose  
- **Architecture:** MVVM + Domain/Data layers
- **Network:** Retrofit + OkHttp
- **Storage:** SQLite (Room & custom), DataStore, File API
- **Async:** Kotlin Coroutines
- **Minimum SDK:** 24 | **Target SDK:** 36

---

## Build, Test, and Lint

### Build the Project

```bash
# Full build (APK)
./gradlew build

# Debug build only
./gradlew assembleDebug

# Faster development build (skips tests)
./gradlew assembleDebug --offline
```

### Run Tests

```bash
# All unit tests
./gradlew test

# Unit tests in specific module
./gradlew app:test

# Single test class
./gradlew app:test --tests com.calico.tutor.domain.usecase.*

# Run with more output
./gradlew test --info

# Instrumentation tests (Android device/emulator required)
./gradlew connectedAndroidTest
```

### Lint and Format

```bash
# Kotlin linting (Ktlint via Gradle if configured, otherwise manual check)
./gradlew check

# Build with strict checks
./gradlew build --strict
```

**Note:** This project currently uses Kotest and JUnit. Check `app/build.gradle.kts` for test configuration details. No pre-configured linter like ktlint or detekt is currently in the build—static analysis should be manual or added via CI if needed.

---

## High-Level Architecture

### Layered MVVM Architecture

```
┌─────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)             │
│  - Screens (LoginScreen, CoursesScreen) │
│  - Components (reusable UI elements)    │
│  - ViewModels (UI state & logic)        │
└──────────────────┬──────────────────────┘
                   │ (viewModelScope.launch)
┌──────────────────▼──────────────────────┐
│  Domain Layer                           │
│  - Use Cases (LoginUseCase, etc.)       │
│  - Domain Models (AuthToken, Course)    │
│  - Repository Interfaces (abstract)     │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│  Data Layer                             │
│  - Repository Implementations           │
│  - Data Sources (Remote, Local)         │
│  - Mappers (DTO ↔ Domain Model)         │
│  - Cache (InMemory LRU, DB)             │
└──────────────────┬──────────────────────┘
                   │
         ┌─────────┴──────────┐
         │                    │
    ┌────▼─────┐      ┌──────▼────┐
    │ Remote   │      │  Local     │
    │ (API)    │      │ (Storage)  │
    └──────────┘      └────────────┘
```

### Key Directories

| Path | Purpose |
|------|---------|
| `app/src/main/java/com/calico/tutor/ui/` | Compose screens, components, ViewModels |
| `app/src/main/java/com/calico/tutor/domain/` | Use cases, models, repository interfaces |
| `app/src/main/java/com/calico/tutor/data/` | Repository impls, data sources, cache, mappers |
| `app/src/main/java/com/calico/tutor/di/` | ServiceLocator (simple DI) |

### Data Persistence Strategy

The app uses **3 complementary storage mechanisms**:

1. **SQLite (Custom SQLiteOpenHelper):** Structured data (courses, applications, tutor profiles)
   - File: `app/src/main/java/com/calico/tutor/ui/screen/DatabaseHelper.kt`
   - Tables: `courses`, `approved_courses`, `applications`, `pending_applications`, `tutor_profile`
   - Used for: Offline caching and syncing pending actions

2. **EncryptedSharedPreferences (TokenManager):** Secure token storage
   - File: `app/src/main/java/com/calico/tutor/data/datasource/local/TokenManager.kt`
   - Stores: JWT tokens, encrypted at rest

3. **DataStore (UserPreferencesDataStore):** Key-value preferences
   - File: `app/src/main/java/com/calico/tutor/data/local/UserPreferencesDataStore.kt`
   - Stores: Cache TTL, last sync time, notification preferences

4. **File API (FileManager):** Backups and logs
   - File: `app/src/main/java/com/calico/tutor/data/local/FileManager.kt`
   - Stores: JSON backups of pending actions, application logs

### Caching Architecture

**Two-Level Caching:**
- **Memory (LRU):** `InMemoryCache.kt` — fast, limited by heap
- **Disk (SQLite):** DatabaseHelper — persistent, survives app restart

**Cache TTL:** Configurable via DataStore (default: 5 minutes, 300,000ms)

**Invalidation Strategy:** Manual invalidation on successful API sync; automatic expiry based on TTL

### Offline-First Network Handling

- **Network Detection:** `isNetworkAvailable()` checks ConnectivityManager
- **Auto-Retry:** Failed requests queued in `RetryQueue` and retried on network recovery
- **Pending Sync Queue:** Offline actions (e.g., applications) stored in `pending_applications` table
- **Connectivity Monitoring:** Network callbacks auto-refresh data when connectivity returns

---

## Key Conventions

### StateFlow Pattern for UI State

All ViewModels expose state as `StateFlow<UiState>` using sealed classes:

```kotlin
sealed class CoursesState {
    object Loading : CoursesState()
    data class Success(val courses: List<Course>, val isOffline: Boolean = false) : CoursesState()
    data class Error(val message: String) : CoursesState()
}

class CoursesViewModel(...) : ViewModel() {
    private val _coursesState = MutableStateFlow<CoursesState>(CoursesState.Loading)
    val coursesState: StateFlow<CoursesState> = _coursesState.asStateFlow()
}
```

**UI consumes via:** `.collectAsState()` in Composables

### Coroutine Patterns

1. **Suspend + Dispatchers.IO** — Database/File operations:
   ```kotlin
   suspend fun getCourses(): List<Course> = withContext(Dispatchers.IO) { ... }
   ```

2. **viewModelScope.launch** — Main-thread UI updates:
   ```kotlin
   viewModelScope.launch(Dispatchers.Main) {
       val data = withContext(Dispatchers.IO) { fetchData() }
       _state.value = Success(data)
   }
   ```

3. **async/await for parallel loading** — Multiple concurrent API calls:
   ```kotlin
   val result1 = async(Dispatchers.IO) { fetch1() }
   val result2 = async(Dispatchers.IO) { fetch2() }
   val combined = result1.await() + result2.await()
   ```

### DTO to Domain Model Mapping

Keep DTOs (from API) separate from domain models (used in UI/domain layer):

- **DTOs:** Located in `data/dto/` — mirror API response structure
- **Mappers:** Located in `data/mapper/` — convert DTO → Domain Model
- **Example:**
  ```kotlin
  // DTO
  data class CourseDto(val id: String, val title: String)
  
  // Domain Model
  data class Course(val id: String, val title: String, val category: String)
  
  // Mapper
  fun CourseDto.toDomain(): Course = Course(id, title, "default")
  ```

**Gson Serialization:** RetrofitClient configures `GsonBuilder().serializeNulls()` so null fields are included in JSON. Account for this when parsing optional API fields.

### Repository Pattern

Repositories are the **single source of truth** for data:

1. **Define interface in domain layer:**
   ```kotlin
   interface CourseRepository {
       suspend fun getCourses(tutorId: String): Result<List<Course>>
   }
   ```

2. **Implement in data layer** with caching logic:
   ```kotlin
   class CourseRepositoryImpl(...) : CourseRepository {
       override suspend fun getCourses(tutorId: String): Result<List<Course>> {
           return try {
               val cached = cache.get("courses_$tutorId")
               if (cached != null && !isExpired(cached)) return Result.Success(cached)
               val remote = apiService.getCourses(tutorId)
               cache.put("courses_$tutorId", remote)
               Result.Success(remote)
           } catch (e: Exception) {
               Result.Error(e)
           }
       }
   }
   ```

### Use Cases

Each use case is a **single responsibility** callable:

```kotlin
class GetCoursesUseCase(private val repository: CourseRepository) {
    suspend operator fun invoke(tutorId: String): Result<List<Course>> {
        return repository.getCourses(tutorId)
    }
}
```

**Why:** Decouples UI from repository implementation; enables easy testing and swapping logic.

### Result Type for Error Handling

Use the custom `Result` sealed class for success/error:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val exception: Exception) : Result<T>()
}
```

ViewModels check `when` on result to update UI state accordingly.

### Dependency Injection (ServiceLocator)

Simple DI via `ServiceLocator` object—no framework (e.g., Hilt) currently used:

```kotlin
object ServiceLocator {
    fun authRepository(context: Context): AuthRepository =
        _authRepository ?: synchronized(this) {
            _authRepository ?: AuthRepositoryImpl(
                apiService = RetrofitClient.authService(),
                tokenManager = TokenManager(context)
            ).also { _authRepository = it }
        }
}
```

**Pattern:** Lazy initialization with synchronization (thread-safe singleton). Retrieve instances in ViewModels via factory or constructor injection.

### Naming Conventions

- **ViewModels:** `<Screen>ViewModel` (e.g., `LoginViewModel`, `CoursesViewModel`)
- **States:** `<Feature>State` (e.g., `AuthState`, `CoursesState`)
- **Use Cases:** `<Action>UseCase` (e.g., `LoginUseCase`, `GetCoursesUseCase`)
- **Repositories:** `<Entity>Repository` interface + `<Entity>RepositoryImpl` implementation
- **Screens:** `<Screen>Screen` (e.g., `LoginScreen`, `CoursesScreen`)
- **API Services:** `<Feature>ApiService` (e.g., `AuthApiService`, `CoursesApiService`)

### Jetpack Compose Best Practices

1. **State hoisting:** Lift state to parent Composable or ViewModel
2. **Preview annotations:** Use `@Preview` for rapid UI iteration
3. **Immutable data:** Pass immutable snapshots to Composables
4. **Recomposition minimization:** Avoid passing lambdas; use `onValueChange` callbacks

Example:
```kotlin
@Composable
fun CourseCard(
    course: Course,
    onApply: (courseId: String) -> Unit
) {
    // Minimal recomposition triggers
}

@Preview
@Composable
fun PreviewCourseCard() {
    CourseCard(course = Course(...), onApply = {})
}
```

### Logging

Use Android's `Log` class with descriptive tags:

```kotlin
Log.d("CoursesViewModel", "Loading courses for tutor: $tutorId")
Log.e("ApiService", "Request failed: ${exception.message}")
```

All errors are also appended to the app's log file via `FileManager.appendLog()` for debugging.

---

## Testing

### Test Structure

- **Unit tests:** `app/src/test/java/` — business logic (Use Cases, Repositories, Mappers)
- **Instrumentation tests:** `app/src/androidTest/java/` — UI and Android components

### Running Tests

```bash
# All unit tests
./gradlew test

# Specific test class
./gradlew test --tests "com.calico.tutor.domain.usecase.*"

# Instrumentation tests (device/emulator required)
./gradlew connectedAndroidTest

# Watch mode (if Gradle daemon is running)
./gradlew build --continuous
```

### Testing Patterns

- Use Kotest for BDD-style assertions
- Mock repositories in ViewModel tests
- Avoid testing framework internals; test behavior
- Example:
  ```kotlin
  class LoginUseCaseTest {
      @Test
      fun `login with valid credentials returns success`() {
          // Given
          val useCase = LoginUseCase(mockRepository)
          
          // When
          val result = useCase("user@test.com", "password123")
          
          // Then
          assertTrue(result is Result.Success)
      }
  }
  ```

---

## Common Tasks

### Adding a New Feature (e.g., a new screen)

1. **Create domain layer:**
   - Add use case in `domain/usecase/`
   - Add models in `domain/model/` if needed

2. **Create data layer:**
   - Add repository interface in `domain/repository/`
   - Add implementation in `data/repository/`
   - Add API service if needed in `data/datasource/remote/`
   - Add DTOs in `data/dto/`
   - Add mappers in `data/mapper/`

3. **Create UI layer:**
   - Add ViewModel in `ui/viewmodel/`
   - Add Composable screen in `ui/screen/`
   - Add components in `ui/component/` if reusable

4. **Wire dependencies:**
   - Add to `ServiceLocator` DI container

### Adding an API Endpoint

1. Add method to relevant `*ApiService` interface (e.g., `AuthApiService`)
2. Create DTO classes in `data/dto/`
3. Create mapper in `data/mapper/` to convert DTO → Domain Model
4. Update repository to call the API service
5. Update use case if needed
6. Handle `Result<T>` in ViewModel

### Handling Network Errors

All API errors should flow through the `Result` type:

```kotlin
repository.fetchData().let {
    when (it) {
        is Result.Success -> _uiState.value = Success(it.data)
        is Result.Error -> _uiState.value = Error(it.exception.message ?: "Unknown error")
    }
}
```

Network failures are automatically retried via `RetryQueue` if offline; pending actions are queued for sync.

### Caching Data

1. Query cache first (check TTL)
2. If miss or expired, fetch from API
3. Store in both memory and disk cache
4. Return to UI

Example in Repository:
```kotlin
suspend fun getCourses(): Result<List<Course>> {
    val cached = inMemoryCache.get("courses")
    if (cached != null && !isExpired(cached)) return Result.Success(cached)
    
    return try {
        val data = apiService.getCourses()
        inMemoryCache.put("courses", data)
        dbHelper.saveCourses(data)
        Result.Success(data)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
```

---

## Troubleshooting

### Build Fails: `GOOGLE_WEB_CLIENT_ID` not found

Add to `local.properties`:
```properties
GOOGLE_WEB_CLIENT_ID=your_google_web_client_id
FIREBASE_API_KEY=your_firebase_api_key
```

### Tests Fail: `ServiceLocator` not initialized

Ensure test setup initializes ServiceLocator with mocks or a test Context.

### Offline Mode Not Working

Check:
1. `isNetworkAvailable()` correctly detects network (review ConnectivityManager call)
2. Data is being cached before going offline
3. Network recovery broadcasts are being monitored

### Memory Leaks in Long-Running Operations

Always use `viewModelScope` in ViewModels—it's cancelled with the ViewModel lifecycle. Avoid `GlobalScope.launch`.

---

## Additional Resources

- **TECHNICAL_SUMMARY.md:** Deep dive into storage, caching, eventual connectivity, multi-threading
- **Firebase Integration:** See `CalicoApp.kt` for Firebase initialization
- **WorkManager:** Background sync tasks in `data/worker/` (e.g., `PendingAvailabilitiesWorker`)

---

## Notes for Future Development

- **DI Framework:** Consider migrating from ServiceLocator to Hilt for better testability
- **Logging:** Integrate a structured logging library (e.g., Timber) for production
- **CI/CD:** Set up GitHub Actions or similar for automated testing and builds
- **Linting:** Add Detekt or ktlint to the build pipeline

---

**Last Updated:** 2026-05-05  
**Maintained By:** Calico Team
