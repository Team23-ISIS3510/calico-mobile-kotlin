# Super Detailed Technical Summary: LocalStorage, Cache, EventualConnectivity & Multi-Threading

## Table of Contents

1. [Local Storage](#1-local-storage)
   - [1.1 Relational Local DB - SQLiteOpenHelper](#11-relational-local-db---sqliteopenhelper)
   - [1.2 EncryptedSharedPreferences - TokenManager](#12-encryptedsharedpreferences---tokenmanager)
   - [1.3 App-Specific Files - FileManager](#13-app-specific-files---filemanager)
2. [Cache](#2-cache)
   - [2.1 In-Memory LRU Cache](#21-in-memory-lru-cache)
   - [2.2 ApiResponseCache](#22-apiresponsecache)
   - [2.3 Two-Level Caching Strategy](#23-two-level-caching-strategy)
3. [Eventual Connectivity](#3-eventual-connectivity)
   - [3.1 Network Detection](#31-network-detection)
   - [3.2 Network Monitoring with Callbacks](#32-network-monitoring-with-callbacks)
   - [3.3 Offline-First Architecture](#33-offline-first-architecture)
4. [Multi-Threading](#4-multi-threading)
   - [4.1 Coroutines Patterns](#41-coroutines-patterns)
   - [4.2 Parallel Data Loading](#42-parallel-data-loading)
   - [4.3 Thread-Safe Operations](#43-thread-safe-operations)
5. [Focus: Courses Page Implementation](#5-focus-courses-page-implementation)
6. [Focus: Profile Page Implementation](#6-focus-profile-page-implementation)
7. [Architecture Summary](#7-architecture-summary)

---

## 1. Local Storage

The app implements **3 complementary strategies** for local persistence:

| Strategy | Technology | Purpose | Location |
|----------|-----------|---------|----------|
| Relational DB | SQLiteOpenHelper | Structured data (courses, applications, profiles) | `DatabaseHelper.kt` |
| Encrypted Preferences | EncryptedSharedPreferences | Secure token storage | `TokenManager.kt` |
| App-Specific Files | File API | Backups and logs | `FileManager.kt` |

---

### 1.1 Relational Local DB - SQLiteOpenHelper

There are **2 separate SQLite databases** in the app, each with different responsibilities:

#### **A) DatabaseHelper.kt** (`app/src/main/java/com/calico/tutor/ui/screen/DatabaseHelper.kt`)

**Database Name:** `app_database.db`  
**Version:** 5  
**Location:** Lines 10-397

**Purpose:** Primary storage for application data (courses, applications, tutor profiles)

**Tables:**

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `courses` | Available courses cached from API | `id`, `api_id`, `title`, `description`, `category` |
| `approved_courses` | Tutor's approved courses | `id`, `title`, `description`, `category` |
| `applications` | Course applications with status | `id`, `course_id`, `course_name`, `course_code`, `status`, `rejection_reason` |
| `pending_applications` | Offline queue for applications awaiting sync | `id`, `course_id`, `course_name`, `course_code`, `notes`, `created_at` |
| `tutor_profile` | Cached tutor profile for offline | `id`, `name`, `email`, `subject`, `profile_image_url` |

**Schema Definitions:** Lines 31-82

```kotlin
private val CREATE_TABLE_COURSES = (
    "CREATE TABLE $TABLE_COURSES ("
    + "$COLUMN_COURSE_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
    + "api_id TEXT, "
    + "$COLUMN_COURSE_TITLE TEXT NOT NULL, "
    + "$COLUMN_COURSE_DESCRIPTION TEXT, "
    + "$COLUMN_COURSE_CATEGORY TEXT"
    + ")"
)
```

**Key Methods:**

| Method | Lines | Description |
|--------|-------|-------------|
| `saveCourses(courses)` | 102-121 | Batch insert courses with transaction |
| `getCourses()` | 123-152 | Query all courses from DB |
| `saveApprovedCourses(courses)` | 154-172 | Save approved courses |
| `getApprovedCourses()` | 174-200 | Get approved courses |
| `saveApplications(applications)` | 278-298 | Save course applications |
| `getApplications()` | 300-328 | Get applications |
| `savePendingApplication(app)` | 339-349 | Queue application for offline sync |
| `getPendingApplications()` | 351-379 | Get pending applications |
| `deletePendingApplication(id)` | 381-385 | Delete after successful sync |
| `saveTutorProfile(tutor)` | 220-230 | Cache tutor profile |
| `getTutorProfiles()` | 232-259 | Load cached profile |

**Data Classes:** Lines 212-267

```kotlin
data class Course(
    val id: Long = 0,
    val apiId: String? = null,
    val title: String,
    val description: String? = null,
    val category: String? = null
)

data class TutorProfile(
    val id: Long = 0,
    val name: String,
    val email: String,
    val subject: String? = null,
    val profileImageUrl: String? = null
)

data class Application(
    val id: Long = 0,
    val courseId: String,
    val courseName: String,
    val courseCode: String,
    val status: String,
    val rejectionReason: String? = null
)

data class PendingApplication(
    val id: Long = 0,
    val courseId: String,
    val courseName: String,
    val courseCode: String,
    val notes: String? = null,
    val createdAt: String
)
```

**Thread Safety:** All methods accessing the database use `SQLiteDatabase` transactions (`beginTransaction()`, `setTransactionSuccessful()`, `endTransaction()`) to ensure atomicity.

---

#### **B) CacheDatabase.kt** (`app/src/main/java/com/calico/tutor/data/local/CacheDatabase.kt`)

**Database Name:** `calico_cache.db`  
**Version:** 2  
**Location:** Lines 19-146

**Purpose:** Cache storage for Home screen data (sessions, occupancy, subjects) and pending availability actions

**Tables:**

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `cache_home` | JSON cache for Home screen responses | `id`, `json_data`, `timestamp` |
| `pending_availabilities` | Offline queue for availability actions | `id`, `availability_json`, `action_type`, `availability_id`, `created_at` |

**Constants:** Lines 26-33

```kotlin
const val KEY_SESSIONS       = "sessions"
const val KEY_OCCUPANCY      = "occupancy"
const val KEY_SUBJECTS      = "subjects_recommended"
const val KEY_AVAILABILITIES = "availabilities"

const val ACTION_CREATE = "CREATE"
const val ACTION_UPDATE = "UPDATE"
const val ACTION_DELETE = "DELETE"
```

**Key Methods:**

| Method | Lines | Description |
|--------|-------|-------------|
| `saveCache(key, json)` | 71-80 | Save JSON to cache with timestamp |
| `getCache(key)` | 83-92 | Get (json, timestamp) pair |
| `savePending(json, actionType, availabilityId)` | 95-107 | Queue pending action |
| `getAllPending()` | 110-129 | Get all pending actions |
| `deletePending(id)` | 132-134 | Delete after sync |
| `getPendingCount()` | 143-145 | Count pending items |

**Schema:** Lines 44-60

```kotlin
db.execSQL("""CREATE TABLE cache_home (
    id TEXT PRIMARY KEY,
    json_data TEXT NOT NULL,
    timestamp INTEGER NOT NULL
)""")

db.execSQL("""CREATE TABLE pending_availabilities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    availability_json TEXT NOT NULL,
    action_type TEXT NOT NULL DEFAULT 'CREATE',
    availability_id TEXT,
    created_at INTEGER NOT NULL
)""")
```

**All database operations are suspend functions using `withContext(Dispatchers.IO)`:** Lines 71, 83, 95, 110, 132

```kotlin
suspend fun saveCache(key: String, json: String) = withContext(Dispatchers.IO) {
    // database operations
}
```

---

### 1.2 EncryptedSharedPreferences - TokenManager

**File:** `app/src/main/java/com/calico/tutor/data/datasource/local/TokenManager.kt`  
**Location:** Lines 8-130

**Purpose:** Securely store authentication tokens and user email with AES-256 encryption

**Encryption Scheme:**

| Component | Algorithm |
|-----------|-----------|
| Master Key | AES256-GCM |
| Pref Key Encryption | AES256-SIV |
| Pref Value Encryption | AES256-GCM |

**Configuration:** Lines 17-23

```kotlin
EncryptedSharedPreferences.create(
    context,
    PREFS_NAME,
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

**Error Recovery:** Lines 24-40

If the encrypted preferences file becomes corrupted, the implementation automatically:
1. Catches the exception
2. Deletes and recreates the preferences file
3. Creates new encrypted preferences with the same encryption scheme

```kotlin
} catch (e: Exception) {
    Log.e(TAG, "Failed to create encrypted prefs, clearing and recreating: ${e.message}")
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    // recreate with same encryption
}
```

**Stored Data:**

| Key | Type | Description |
|-----|------|-------------|
| `KEY_ID_TOKEN` | String | Firebase ID token |
| `KEY_REFRESH_TOKEN` | String | Refresh token for renewal |
| `KEY_EXPIRES_IN` | Long | Token expiration in seconds |
| `KEY_SAVE_TIME` | Long | Timestamp when token was saved |
| `KEY_EMAIL` | String | User email |

**Key Methods:**

| Method | Lines | Description |
|--------|-------|-------------|
| `saveToken(idToken, refreshToken, expiresIn)` | 43-53 | Save tokens with timestamp |
| `saveEmail(email)` | 55-61 | Save user email |
| `getEmail()` | 63 | Retrieve email |
| `getIdToken()` | 65 | Get ID token |
| `getRefreshToken()` | 67 | Get refresh token |
| `getExpiresIn()` | 69 | Get expiration time |
| `isTokenValid()` | 75-85 | Check token validity |
| `isTokenExpiringSoon()` | 91-104 | Check if expires in < 5 minutes |
| `clearToken()` | 108-118 | Clear all tokens |
| `isTokenAvailable()` | 120 | Check if token exists |

**Token Validation Logic:** Lines 75-85

```kotlin
fun isTokenValid(): Boolean {
    val idToken = getIdToken() ?: return false
    val expiresIn = getExpiresIn()
    val saveTime = getSaveTime()
    
    val elapsedSeconds = (System.currentTimeMillis() - saveTime) / 1000
    val isValid = elapsedSeconds < expiresIn
    
    Log.d(TAG, "✅ Token válido: $isValid (Transcurrido: $elapsedSeconds seg, Expira en: $expiresIn seg)")
    return isValid
}
```

**Early Renewal Detection:** Lines 91-104

```kotlin
fun isTokenExpiringSoon(): Boolean {
    val expiresIn = getExpiresIn()
    val saveTime = getSaveTime()
    val RENEWAL_THRESHOLD_SECONDS = 300 // 5 minutos
    
    val elapsedSeconds = (System.currentTimeMillis() - saveTime) / 1000
    val timeRemainingSeconds = expiresIn - elapsedSeconds
    val expiringSoon = timeRemainingSeconds < RENEWAL_THRESHOLD_SECONDS && timeRemainingSeconds > 0
    
    if (expiringSoon) {
        Log.w(TAG, "⏰ Token expirará en $timeRemainingSeconds segundos")
    }
    return expiringSoon
}
```

---

### 1.3 App-Specific Files - FileManager

**File:** `app/src/main/java/com/calico/tutor/data/local/FileManager.kt`  
**Location:** Lines 22-68

**Purpose:** Manage app-specific file storage for backups and logs

**Storage Locations:**

| Directory | Path | Purpose |
|-----------|------|---------|
| `backups/` | `context.filesDir/backups/` | JSON backups of pending actions |
| `logs.txt` | `context.filesDir/logs.txt` | Exportable application logs |

**Key Methods:**

| Method | Lines | Description |
|--------|-------|-------------|
| `saveBackup(jsonContent)` | 34-41 | Save JSON backup with timestamp |
| `appendLog(message)` | 44-51 | Append timestamped log entry |
| `readLogs()` | 54-56 | Read full log file |
| `cleanOldBackups(maxFiles)` | 62-67 | Delete old backups (keep last 5) |

**Implementation:** Lines 34-41

```kotlin
suspend fun saveBackup(jsonContent: String) = withContext(Dispatchers.IO) {
    try {
        File(backupsDir, "pending_${System.currentTimeMillis()}.json").writeText(jsonContent)
        cleanOldBackups()
    } catch (e: Exception) {
        Log.e("FileManager", "Error guardando backup: ${e.message}")
    }
}
```

**Log Format:** Lines 44-51

```kotlin
suspend fun appendLog(message: String) = withContext(Dispatchers.IO) {
    try {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        logsFile.appendText("[$ts] $message\n")
    } catch (e: Exception) {
        Log.e("FileManager", "Error escribiendo log: ${e.message}")
    }
}
```

**Automatic Cleanup:** Lines 62-67

```kotlin
private fun cleanOldBackups(maxFiles: Int = 5) {
    backupsDir.listFiles()
        ?.sortedByDescending { it.lastModified() }
        ?.drop(maxFiles)
        ?.forEach { it.delete() }
}
```

---

### 1.4 User Preferences DataStore

**File:** `app/src/main/java/com/calico/tutor/data/local/UserPreferencesDataStore.kt`  
**Location:** Lines 32-69

**Purpose:** Key-value storage using Jetpack DataStore for user preferences

**Database Name:** `user_prefs` (via preferencesDataStore)

**Stored Keys:**

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `cache_expiry_ms` | Long | 5 minutes (300,000ms) | Cache TTL |
| `last_sync_time` | Long | 0 | Last successful sync timestamp |
| `notifications_enabled` | Boolean | true | User notification preference |

**Flow-Based Observability:** Lines 44-56

```kotlin
val cacheExpiryMs: Flow<Long> = context.userPrefsStore.data.map { prefs ->
    prefs[KEY_CACHE_EXPIRY_MS] ?: DEFAULT_CACHE_EXPIRY_MS
}

val lastSyncTime: Flow<Long> = context.userPrefsStore.data.map { prefs ->
    prefs[KEY_LAST_SYNC_TIME] ?: 0L
}

val notificationsEnabled: Flow<Boolean> = context.userPrefsStore.data.map { prefs ->
    prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
}
```

**Key Methods:**

| Method | Lines | Description |
|--------|-------|-------------|
| `setCacheExpiryMs(expiryMs)` | 58-60 | Set cache TTL |
| `updateLastSyncTime()` | 62-64 | Update sync timestamp |
| `setNotificationsEnabled(enabled)` | 66-68 | Toggle notifications |

---

## 2. Cache

The app implements a **two-level caching strategy** with in-memory and disk caches:

| Level | Technology | Size Limit | Persistence |
|-------|-----------|----------|------------|
| L1 (Memory) | LinkedHashMap (LRU) | 20 entries |App process only |
| L2 (Disk) | SQLite | Unlimited | Across app restarts |

---

### 2.1 In-Memory LRU Cache

**File:** `app/src/main/java/com/calico/tutor/data/cache/InMemoryCache.kt`  
**Location:** Lines 29-71

**Architecture:**

```kotlin
class InMemoryCache(private val maxSize: Int = 20) {

    data class CacheEntry(
        val value: Any,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val map = object : LinkedHashMap<String, CacheEntry>(
        maxSize + 1,  // initialCapacity: +1 to avoid rehashing
        0.75f,        // loadFactor: optimal memory/performance balance
        true          // accessOrder: true = LRU mode
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean =
            size > maxSize
    }
}
```

**LRU Implementation Details:**

- `accessOrder = true`: Most recently accessed items move to the end
- `removeEldestEntry()`: Automatically triggered when `size > maxSize`
- `@Synchronized`: All methods are thread-safe

**Thread-Safe Methods:**

| Method | lines | Description |
|--------|-------|-------------|
| `put(key, value)` | 52-55 | Insert/update with automatic LRU eviction |
| `get(key)` | 61-62 | Retrieve and update LRU order |
| `clear()` | 65-66 | Clear all entries |
| `size()` | 69-70 | Get current size |

**ServiceLocator Registration:** `ServiceLocator.kt:47-50`

```kotlin
fun inMemoryCache(): InMemoryCache =
    _inMemoryCache ?: synchronized(this) {
        _inMemoryCache ?: InMemoryCache(maxSize = 20).also { _inMemoryCache = it }
    }
```

---

### 2.2 ApiResponseCache

**File:** `app/src/main/java/com/calico/tutor/util/ApiResponseCache.kt`  
**Location:** Lines 14-94

**Purpose:** Specialized LRU cache for API responses in CoursesViewModel

**Implementation:**

```kotlin
class ApiResponseCache<T>(private val maxSize: Int) {
    
    private val cache = LinkedHashMap<String, Any>(16, 0.75f, true)

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun get(key: String): T? = cache[key] as? T

    @Synchronized
    fun put(key: String, value: Any) {
        if (cache.containsKey(key)) {
            cache.remove(key)
        }
        
        cache[key] = value
        
        while (cache.size > maxSize) {
            val oldestKey = cache.keys.first()
            cache.remove(oldestKey)
            Log.d("ApiCache", "🗑️ Evicted: $oldestKey")
        }
        
        Log.d("ApiCache", "💾 Cached: $key (size: ${cache.size}/$maxSize)")
    }
    // ... other methods
}
```

**Singleton Instances:** Lines 54-93

| Cache Instance | Max Size | Purpose |
|-------------|--------|---------|
| `approvedCoursesCache` | 10 | Approved tutor courses |
| `applicationsCache` | 10 | Course applications |
| `allCoursesCache` | 5 | All available courses |

```kotlin
fun approvedCourses(): ApiResponseCache<*> {
    return approvedCoursesCache ?: synchronized(this) {
        approvedCoursesCache ?: ApiResponseCache<Any>(10).also { 
            approvedCoursesCache = it 
        }
    }
}

fun applications(): ApiResponseCache<*> {
    return applicationsCache ?: synchronized(this) {
        applicationsCache ?: ApiResponseCache<Any>(10).also { 
            applicationsCache = it 
        }
    }
}

fun allCourses(): ApiResponseCache<*> {
    return allCoursesCache ?: synchronized(this) {
        allCoursesCache ?: ApiResponseCache<Any>(5).also { 
            allCoursesCache = it 
        }
    }
}
```

---

### 2.3 Two-Level Caching Strategy

The complete caching flow for Home screen data:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       TWO-LEVEL CACHING FLOW                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. Check L1 (InMemoryCache)                                              │
│     ┌─────────────────┐                                                 │
│     │ memoryCache.get │ ─── Hit ──► Return data, update LRU order            │
│     └────────┬�──────┘                                                 │
│              │ Miss                                                      │
│     ┌──────▼────────┐                                                   │
│     │             │                                                   │
│     │  2. Check  │                                                   │
│     │ L2 (SQLite) │                                                   │
│     │  CacheDatabase.getCache(key)                                   │
│     └─────┬───────┘                                                   │
│           │                                                            │
│     ┌────▼────────┐                                                 │
│     │ Has data?  │                                                 │
│     └────┬──────┘                                                 │
│          │                                                            │
│    ┌────┴───┐                                                       │
│    │        │                                                       │
│   Yes      No                                                        │
│    │        │                                                       │
│    ▼        ▼                                                       │
│ ┌────────────────┐  ┌────────────────┐                            │
│ │Check freshness│  │   FETCH      │                            │
│ │ timestamp   │  │   FROM API   │                            │
│ │ vs expiry  │  │ (withContext │                            │
│ └─────┬─────┘  │  Dispatchers.IO)│                            │
│       │        │  └──────┬─────┘                            │
│  ┌────┴───┐         │                                  │
│  │        │         │                                  │
│ Fresher  Stale   │                                  │
│   │       │         │                                  │
│   ▼       │         ▼                                  │
│ ┌────────────────┐  ┌────────────────┐                         │
│ │ Populate L1    │  │ Save to L2    │                         │
│ │memoryCache.put│  │ cacheDb.save │                         │
│ └──────────────┘  │memoryCache.put│                         │
│                    └��─────────────┘                         │
│                                                              │
│  Fallback: Both L1 & L2 miss + API fail = Show error           │
└──────────────────────────────────────────────────────────────┘
```

**Implementation in HomeScreenViewModel:** Lines 245-295 (fetchSessionsWithCache)

```kotlin
private suspend fun fetchSessionsWithCache(tutorId: String): SessionsState {
    val cacheKey  = "${CacheDatabase.KEY_SESSIONS}_$tutorId"
    val expiryMs  = userPrefs.cacheExpiryMs.first()
    
    // 1. Check L1 (InMemoryCache)
    memoryCache.get(cacheKey)?.let { entry ->
        Log.d(TAG, "Sessions: L1 cache hit")
        return SessionsState.Success(...)
    }
    
    // 2. Check L2 (SQLite)
    val (cachedJson, cachedTs) = cacheDb.getCache(cacheKey)
    val isFresh = cachedJson != null && (System.currentTimeMillis() - cachedTs) < expiryMs
    
    if (isFresh && cachedJson != null) {
        Log.d(TAG, "Sessions: L2 cache hit (fresh)")
        val rawList = gson.fromJson<List<TutoringSessionData>>(cachedJson, type)
        memoryCache.put(cacheKey, rawList)  // Populate L1
        return SessionsState.Success(...)
    }
    
    // 3. Fetch from API
    try {
        val response = withContext(Dispatchers.IO) {
            apiService.getSessions(tutorId)
        }
        cacheDb.saveCache(cacheKey, gson.toJson(response.sessions, type))  // Save to L2
        memoryCache.put(cacheKey, response.sessions)                     // Populate L1
        return SessionsState.Success(...)
    } catch (e: Exception) {
        // 4. Fallback to stale L2 cache
        if (cachedJson != null) {
            val rawList = gson.fromJson<List<TutoringSessionData>>(cachedJson, type)
            memoryCache.put(cacheKey, rawList)
            return SessionsState.Success(...)
        }
    }
    
    return SessionsState.Error("No hay datos disponibles")
}
```

---

## 3. Eventual Connectivity

The app implements **graceful degradation** when network is unavailable:

| Scenario | Behavior |
|----------|----------|
| Online + API success | Use fresh API data, update cache |
| Online + API fail | Use stale cache if available |
| Offline + Cache available | Use cache with offline banner |
| Offline + No cache | Show error message |

---

### 3.1 Network Detection

**Method:** `isNetworkAvailable()` (used in both CoursesViewModel and ProfileViewModel)

**CoursesViewModel:** Lines 57-62

```kotlin
private fun isNetworkAvailable(): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
```

**ProfileViewModel:** Lines 43-48 (identical implementation)

```kotlin
private fun isNetworkAvailable(): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
```

**Network Capabilities Check:**
- `NET_CAPABILITY_INTERNET`: Confirms internet access is available
- Uses `activeNetwork`: Current active network
- Returns `false` if no network or no capabilities

---

### 3.2 Network Monitoring with Callbacks

**Location:** HomeScreenViewModel:142-215

**Architecture:** Uses `ConnectivityManager.NetworkCallback` to auto-refresh when network returns

**Registration:** Lines 194-215

```kotlin
fun startConnectivityMonitoring(tutorId: String) {
    if (networkCallback != null) return   // already registered
    monitoredTutorId = tutorId
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val req = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available, auto-refreshing data")
            refreshData(tutorId)
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost")
        }
    }

    cm.registerNetworkCallback(req, networkCallback!!)
}
```

**Unregistration:** Lines 222-226

```kotlin
fun stopConnectivityMonitoring() {
    networkCallback?.let { cb ->
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        try { cm?.unregisterNetworkCallback(cb) } catch (_: Exception) {}
    }
    networkCallback = null
}
```

**Call Sites (AvailabilityViewModel):** Similar pattern at lines 144-187

---

### 3.3 Offline-First Architecture

**Offline Banner State:**

The app displays banners to inform users when viewing cached data:

```kotlin
data class Success(
    // ... other fields
    val isOffline: Boolean = false    // OFFLINE BANNER FLAG
) : CoursesState()
```

**Flow in CoursesViewModel:** Lines 64-78

```kotlin
fun loadData(tutorId: String, preservePending: Boolean = false) {
    viewModelScope.launch {
        if (!preservePending) {
            _coursesState.value = CoursesState.Loading
        }

        val hasNetwork = isNetworkAvailable()
        Log.d("CoursesViewModel", "Network available: $hasNetwork, preservePending: $preservePending")

        // If no network, go directly to offline mode
        if (!hasNetwork) {
            loadFromCache(tutorId, isOffline = true, preservePending = preservePending)
            return@launch
        }
        
        // ... proceed with API calls
    }
}
```

---

## 4. Multi-Threading

The app uses **Kotlin Coroutines** with specific patterns for concurrent operations:

---

### 4.1 Coroutines Patterns

**Pattern 1: Suspend Functions with Dispatchers.IO**

Used in: DatabaseHelper, CacheDatabase, FileManager, all API calls

```kotlin
suspend fun getCourses(): List<Course> = withContext(Dispatchers.IO) {
    // Database operations run on IO thread pool
    val db = this.readableDatabase
    val cursor = db.query(...)
    // ... cursor operations
}
```

**Pattern 2: viewModelScope.launch with Dispatchers**

Used in: All ViewModels

```kotlin
viewModelScope.launch(Dispatchers.Main) {
    // UI operations on Main thread
    val result = withContext(Dispatchers.IO) {
        // Background work on IO thread
        apiService.getData()
    }
    // Back to Main thread
    _state.value = Success(result)
}
```

**Pattern 3: async/await for Parallel Execution**

Used in: HomeScreenViewModel

```kotlin
viewModelScope.launch(Dispatchers.Main) {
    // Parallel execution on IO threads
    val sessionsDeferred  = async(Dispatchers.IO) { fetchSessionsWithCache(tutorId) }
    val occupancyDeferred = async(Dispatchers.IO) { fetchOccupancyWithCache(tutorId) }
    val subjectsDeferred  = async(Dispatchers.IO) { fetchSubjectsWithCache() }

    // Await results back on Main thread
    _sessionsState.value  = sessionsDeferred.await()
    _occupancyState.value = occupancyDeferred.await()
    _subjectsState.value  = subjectsDeferred.await()
}
```

---

### 4.2 Parallel Data Loading

**HomeScreenViewModel Implementation:** Lines 159-181

```kotlin
fun loadAllData(tutorId: String) {
    viewModelScope.launch(Dispatchers.Main) {
        // State initialization on Main thread
        _sessionsState.value  = SessionsState.Loading
        _occupancyState.value = OccupancyState.Loading
        _subjectsState.value  = SubjectsState.Loading

        // Parallel execution (async on IO threads)
        val profileDeferred   = async(Dispatchers.IO) { fetchTutorName(tutorId) }
        val sessionsDeferred  = async(Dispatchers.IO) { fetchSessionsWithCache(tutorId) }
        val occupancyDeferred = async(Dispatchers.IO) { fetchOccupancyWithCache(tutorId) }
        val subjectsDeferred  = async(Dispatchers.IO) { fetchSubjectsWithCache() }

        // Await results back on Main thread (maintain order)
        _tutorName.value      = profileDeferred.await()
        _sessionsState.value  = sessionsDeferred.await()
        _occupancyState.value = occupancyDeferred.await()
        _subjectsState.value  = subjectsDeferred.await()

        if (_sessionsState.value !is SessionsState.Error) {
            userPrefs.updateLastSyncTime()
        }
    }
}
```

**Parallelism Diagram:**

```
ViewModelScope.launch(Dispatchers.Main)
│
├── async(Dispatchers.IO) ── fetchTutorName() ──► await() ──┐
│                                                     ├──► StateFlow updates
├── async(Dispatchers.IO) ── fetchSessionsWithCache() ──┤     (on Main thread)
├── async(Dispatchers.IO) ── fetchOccupancyWithCache() ────┤
└── async(Dispatchers.IO) ── fetchSubjectsWithCache() ─────────┘
```

**Benefits:**
- All 4 API calls execute concurrently
- Total time ≈ max(single call time) instead of sum(single call times)
- Results await() in order, update StateFlows on Main thread

---

### 4.3 Thread-Safe Operations

**Synchronized Methods:**

In `InMemoryCache.kt`: Lines 52-70

```kotlin
@Synchronized
fun put(key: String, value: Any) {
    map[key] = CacheEntry(value)
}

@Synchronized
fun get(key: String): CacheEntry? = map[key]

@Synchronized
fun clear() = map.clear()

@Synchronized
fun size(): Int = map.size
```

In `ApiResponseCache.kt`: Lines 19-52

```kotlin
@Suppress("UNCHECKED_CAST")
@Synchronized
fun get(key: String): T? = cache[key] as? T

@Synchronized
fun put(key: String, value: Any) { ... }

@Synchronized
fun contains(key: String): Boolean = cache.containsKey(key)

@Synchronized
fun remove(key: String) = cache.remove(key)

@Synchronized
fun clear() { cache.clear() }

@Synchronized
fun size(): Int = cache.size
```

**Singleton Thread Safety:**

ServiceLocator uses double-checked locking pattern:

```kotlin
fun cacheDatabase(context: Context): CacheDatabase =
    _cacheDatabase ?: synchronized(this) {
        _cacheDatabase ?: CacheDatabase(context.applicationContext).also { _cacheDatabase = it }
    }
```

---

## 5. Focus: Courses Page Implementation

**ViewModel:** `CoursesViewModel.kt` (530 lines)

**State Definition:** Lines 24-36

```kotlin
sealed class CoursesState {
    object Idle : CoursesState()
    object Loading : CoursesState()
    data class Success(
        val approvedCourses: List<TutorCourseData>,
        val availableCourses: List<AvailableCourseResponse>,
        val applications: List<CourseApplicationResponse>,
        val pendingApplications: List<DatabaseHelper.PendingApplication> = emptyList(),
        val isOffline: Boolean = false,
        val applicationQueued: Boolean = false
    ) : CoursesState()
    data class Error(val message: String) : CoursesState()
}
```

### 5.1 Three-Tier Data Loading

**CoursesPage uses a 3-tier fallback strategy:**

```
┌─────────────────────────────────────────────────────────────┐
│            COURSES PAGE DATA LOADING FLOW                   │
├─────────────────────────────────────────────────────────────┤
│                                                       │
│  Tier 1: Try API first                                 │
│  ┌──────────────────────────────────────────────────┐  │
│  │ subjectsApiService.getTutorCourses(tutorId)     │  │
│  │ subjectsApiService.getTutorApplications(tutorId)  │  │
│  │ subjectsApiService.getAllAvailableCourses() │  │
│  └──────────────────┬───────────────────────┘  │
│                    │                              │
│         ┌─────────┴─────────┐                │
│         │                │                │
│       Success         Failure            │
│         │                │                │
│         ▼                ▼                │
│  ┌─────────────┐  ┌───────────────────┐    │
│  │ Cache to L1 │  │ Try ApiResponseCache│    │
│  │ (LRU)      │  │ (in-memory)       │    │
│  └─────────────┘  └────────┬────────┘    │
│                            │             │
│                     ┌─────┴─────┐       │
│                     │           │       │
│                   Hit         Miss     │
│                     │           │       │
│                     ▼           ▼       │
│              ┌─────────────────────┐       │
│              │ Try SQLite DB     │       │
│              │ (DatabaseHelper)  │       │
│              └────────┬────────┘       │
│                     │                  │
│              ┌─────┴─────┐            │
│              │           │            │
│            Has        Empty      │
│              │           │            │
│              ▼           ▼            │
│         ┌─────────┐ ┌──────────┐    │
│         │ Use DB  │ │ Empty   │    │
│         │ data   │ │ state   │    │
│         └────────┘ └────────┘    │
│                                     │
│  FINAL: Save to SQLite for offline   │
│  ┌────────────────────────────────────┐ │
│  │ dbHelper.saveCourses(...)         │ │
│  │ dbHelper.saveApprovedCourses()  │ │
│  │ dbHelper.saveApplications()     │ │
│  └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Cache Integration (CoursesViewModel)

**In-Memory Caches:** Lines 53-55

```kotlin
private val approvedCoursesCache = ApiResponseCache.approvedCourses()
private val applicationsCache = ApiResponseCache.applications()
private val allCoursesCache = ApiResponseCache.allCourses()
```

**Cache Usage on API Failure:** Lines 114-116

```kotlin
} catch (e: Exception) {
    Log.e("CoursesViewModel", "❌ API failed for approved: ${e.message}", e)
    // Try LRU cache
    val cached = approvedCoursesCache.get("approved_$tutorId")
    approvedCourses = (cached as? List<TutorCourseData>) ?: emptyList()
    // If still empty, try DB
    if (approvedCourses.isEmpty()) {
        approvedCourses = withContext(Dispatchers.IO) {
            dbHelper.getApprovedCourses().map { course -> ... }
        }
    }
}
```

**Cache Population on Success:** Lines 109-110

```kotlin
// Only cache after successful API call
approvedCoursesCache.put("approved_$tutorId", approvedCourses)
```

**SQLite Persistence:** Lines 226-230

```kotlin
withContext(Dispatchers.IO) {
    dbHelper.saveCourses(coursesToCache)
    dbHelper.saveApprovedCourses(approvedToCache)
    dbHelper.saveApplications(applicationsToCache)
}
```

### 5.3 Offline Application Queue

**Queue on Offline:** Lines 461-494

```kotlin
if (!hasNetwork) {
    // Queue the application for later
    val pendingApp = DatabaseHelper.PendingApplication(
        courseId = courseId,
        courseName = courseName,
        courseCode = courseCode,
        notes = notes,
        createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
    )
    withContext(Dispatchers.IO) {
        dbHelper.savePendingApplication(pendingApp)
    }
    Log.d("CoursesViewModel", "Application queued for later: $courseName")
    
    // Update UI immediately
    val currentState = _coursesState.value
    if (currentState is CoursesState.Success) {
        val updatedPendingApps = currentState.pendingApplications + pendingApp.copy(id = System.currentTimeMillis())
        _coursesState.value = currentState.copy(pendingApplications = updatedPendingApps)
    }
    
    // Reload in background to refresh from DB
    loadData(tutorId, preservePending = true)
    
    _applicationQueued.value = true
    return@launch
}
```

**Sync on Reconnect:** Lines 362-396

```kotlin
private suspend fun syncPendingApplications(tutorId: String, pendingApps: List<DatabaseHelper.PendingApplication>) {
    val successfulIds = mutableListOf<Long>()
    
    for (app in pendingApps) {
        try {
            val request = ApplyCourseRequest(tutorId = tutorId, courseId = app.courseId, notes = app.notes)
            withContext(Dispatchers.IO) {
                subjectsApiService.applyForCourse(request)
            }
            // Remove from DB on success
            withContext(Dispatchers.IO) {
                dbHelper.deletePendingApplication(app.id)
            }
            successfulIds.add(app.id)
            Log.d("CoursesViewModel", "✅ Synced pending application: ${app.courseName}")
        } catch (e: Exception) {
            Log.w("CoursesViewModel", "❌ Failed to sync application: ${app.courseName} - ${e.message}")
        }
    }
    
    if (successfulIds.isNotEmpty()) {
        // Clear caches and fetch fresh data
        applicationsCache.remove("apps_$tutorId")
        approvedCoursesCache.remove("approved_$tutorId")
        allCoursesCache.remove("all")
        
        fetchAndUpdateData(tutorId)
    }
}
```

### 5.4 Methods in CoursesViewModel

| Method | lines | Purpose |
|--------|-------|---------|
| `loadData(tutorId, preservePending)` | 64-278 | Main data loading |
| `loadFromCache(tutorId, isOffline, preservePending)` | 280-360 | Load from SQLite |
| `syncPendingApplications(tutorId, pendingApps)` | 362-396 | Sync queued apps |
| `fetchAndUpdateData(tutorId)` | 398-452 | Fetch fresh data after sync |
| `applyForCourse(tutorId, courseId, courseName, courseCode, notes)` | 454-525 | Submit application |
| `resetApplicationQueued()` | 527-529 | Reset queued flag |

---

## 6. Focus: Profile Page Implementation

**ViewModel:** `ProfileViewModel.kt` (197 lines)

### 6.1 State Definition

**Lines 20-32:**

```kotlin
sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(
        val userName: String,
        val userEmail: String,
        val profileImageUrl: String? = null,
        val rating: Double? = null,
        val bio: String? = null,
        val isOffline: Boolean = false
    ) : ProfileState()
    data class Error(val message: String) : ProfileState()
}
```

### 6.2 Data Loading Flow

**Lines 50-151:**

```kotlin
fun loadProfile() {
    viewModelScope.launch {
        _profileState.value = ProfileState.Loading

        val email = tokenManager.getEmail() ?: "User"
        val firebaseUid = tokenManager.getIdToken()?.let { JwtUtils.extractFirebaseUid(it) }
        
        // Check network once at the start
        val hasNetwork = isNetworkAvailable()
        Log.d("ProfileViewModel", "Network available: $hasNetwork")

        // If no network, go directly to offline
        if (!hasNetwork) {
            loadFromCache(email, isOffline = true)
            return@launch
        }

        try {
            // Try online first
            val tutorResponse = withContext(Dispatchers.IO) {
                if (firebaseUid != null) {
                    ServiceLocator.subjectsApiService(context).getTutorProfile(firebaseUid)
                } else {
                    null
                }
            }

            var profileImageUrl: String? = null

            if (tutorResponse != null) {
                profileImageUrl = tutorResponse.profileImage ?: tutorResponse.profilePictureUrl
                
                // Save to SQLite for offline
                withContext(Dispatchers.IO) {
                    try {
                        val profile = DatabaseHelper.TutorProfile(
                            name = tutorResponse.name,
                            email = tutorResponse.email,
                            subject = tutorResponse.courses?.firstOrNull(),
                            profileImageUrl = profileImageUrl
                        )
                        ServiceLocator.provideDatabaseHelper(context).saveTutorProfile(profile)
                        Log.d("ProfileViewModel", "✅ Saved profile to SQLite")
                    } catch (e: Exception) {
                        Log.e("ProfileViewModel", "❌ Failed to save profile: ${e.message}")
                    }
                }
            }

            val userName = tutorResponse?.name 
                ?: email.substringBefore("@").replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase() else it.toString() 
                }

            _profileState.value = ProfileState.Success(
                userName = userName,
                userEmail = tutorResponse?.email ?: email,
                profileImageUrl = profileImageUrl,
                rating = tutorResponse?.rating,
                bio = tutorResponse?.bio,
                isOffline = false
            )

        } catch (e: Exception) {
            Log.w("ProfileViewModel", "❌ Online failed: ${e.message}")
            
            // Use cached data (without offline banner)
            var profileImageUrl: String? = null
            var cachedName = ""
            var cachedEmail = ""
            
            withContext(Dispatchers.IO) {
                try {
                    val dbHelper = ServiceLocator.provideDatabaseHelper(context)
                    val profiles = dbHelper.getTutorProfiles()
                    if (profiles.isNotEmpty()) {
                        val profile = profiles.first()
                        profileImageUrl = profile.profileImageUrl
                        cachedName = profile.name
                        cachedEmail = profile.email
                    }
                } catch (e: Exception) { }
            }

            val userName = cachedName.ifEmpty { 
                email.substringBefore("@").replaceFirstChar { ... }
            }

            // Network existed, just API failed - show as ONLINE
            _profileState.value = ProfileState.Success(
                userName = userName,
                userEmail = cachedEmail.ifEmpty { email },
                profileImageUrl = profileImageUrl,
                isOffline = false
            )
        }
    }
}
```

### 6.3 Offline Cache Loading

**Lines 153-187:**

```kotlin
private suspend fun loadFromCache(email: String, isOffline: Boolean) {
    var profileImageUrl: String? = null
    var cachedName = ""
    var cachedEmail = ""
    
    withContext(Dispatchers.IO) {
        try {
            val dbHelper = ServiceLocator.provideDatabaseHelper(context)
            val profiles = dbHelper.getTutorProfiles()
            if (profiles.isNotEmpty()) {
                val profile = profiles.first()
                profileImageUrl = profile.profileImageUrl
                cachedName = profile.name
                cachedEmail = profile.email
                Log.d("ProfileViewModel", "✅ Loaded from SQLite: $profileImageUrl")
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "❌ Failed to load from SQLite: ${e.message}")
        }
    }

    val userName = cachedName.ifEmpty { 
        email.substringBefore("@").replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase() else it.toString() 
        }
    }

    _profileState.value = ProfileState.Success(
        userName = userName,
        userEmail = cachedEmail.ifEmpty { email },
        profileImageUrl = profileImageUrl,
        isOffline = isOffline
    )
}
```

### 6.4 Profile Data Storage

**DatabaseHelper Methods:**

```kotlin
fun saveTutorProfile(tutor: TutorProfile): Long {
    val db = this.writableDatabase
    db.delete(TABLE_TUTOR_PROFILE, null, null)
    val values = ContentValues().apply {
        put(COLUMN_TUTOR_NAME, tutor.name)
        put(COLUMN_TUTOR_EMAIL, tutor.email)
        put(COLUMN_TUTOR_SUBJECT, tutor.subject)
        put("profile_image_url", tutor.profileImageUrl)
    }
    return db.insert(TABLE_TUTOR_PROFILE, null, values).also { db.close() }
}

fun getTutorProfiles(): List<TutorProfile> {
    val tutors = mutableListOf<TutorProfile>()
    val db = this.readableDatabase
    val cursor = db.query(TABLE_TUTOR_PROFILE, ...)
    
    cursor?.use {
        while (it.moveToNext()) {
            val tutor = TutorProfile(
                id = it.getLong(it.getColumnIndexOrThrow(COLUMN_TUTOR_ID)),
                name = it.getString(it.getColumnIndexOrThrow(COLUMN_TUTOR_NAME)),
                email = it.getString(it.getColumnIndexOrThrow(COLUMN_TUTOR_EMAIL)),
                subject = it.getString(it.getColumnIndexOrThrow(COLUMN_TUTOR_SUBJECT)),
                profileImageUrl = it.getString(it.getColumnIndexOrThrow("profile_image_url"))
            )
            tutors.add(tutor)
        }
    }
    db.close()
    return tutors
}
```

---

## 7. Architecture Summary

### Complete Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                           APP ARCHITECTURE                                          │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌──────────────────────────────────────────────────────────────────────────────┐  │
│  ��                         UI LAYER (Compose)                             │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐         │  │
│  │  │  CoursesScreen │  │ ProfileScreen │  │   HomeScreen   │         │  │
│  │  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘         │  │
│  └──────────┼────────────────────┼────────────────────┼─────────────────────┘  │
│             │                   │                   │                         │
│             ▼                   ▼                   ▼                         │
│  ┌──────────────────────────────────────────────────────────────────────────────┐  │
│  │                      VIEWMODEL LAYER                     │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐            │  │
│  │  │CoursesViewModel │  │ProfileViewModel │  │HomeScreenViewModel│           │  │
│  │  │                │  │                │  │                 │            │  │
│  │  │• loadData()     │  │• loadProfile() │  │• loadAllData()   │            │  │
│  │  │• applyForCourse│  │• loadFromCache │  │• fetchSessions()│            │  │
│  │  │• syncPending() │  │                │  │• fetchSubject() │            │  │
│  │  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘            │  │
│  └──────────┼────────────────────┼────────────────────┼────────────────────────────┘  │
│             │                   │                   │                               │
│             ▼                   ▼                   ▼                               │
│  ┌──────────────────────────────────────────────────────────────────────────────┐  │
│  │                      USE CASES / REPOSITORIES             │  │
│  │  ┌──────────────────────────────────────────────────────────────┐          │  │
│  │  │  SubjectsApiService  │  AvailabilityApiService  │ AuthApiService │       │  │
│  │  └───────────────────────────┬───────────────────────────────────┘          │  │
│  └─────────────────────────────┼────────────────────────────────────────────┘  │
│                                │                                                │
│                                ▼                                                │
│  ┌──��─��─────────────────────────────────────────────────────────────────────────┐  │
│  │                    DATA SOURCE LAYER                   │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │  │
│  │  │ RemoteDataSource│  │ LocalDataSource │  │  TokenManager  │              │  │
│  │  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘              │  │
│  └─────────┼─────────────────┼─────────────────┼────────────────────────────┘  │
│           │                 │                 │                                   │
│           ▼                 ▼                 ▼                                   │
│  ┌──────────────────────────────────────────────────────────────────────────────┐  │
│  │                    LOCAL STORAGE LAYER                 │  │
│  │                                                                       │  │
│  │  ┌─────────────────────┐                                               │  │
│  │  │  SQLiteOpenHelper   │  ◄── app_database.db                     │  │
│  │  │  (DatabaseHelper)   │      • courses table              │  │
│  │  │                   │      • approved_courses        │  │
│  │  │  Lines: 10-397    │      • applications           │  │
│  │  │                   │      • pending_applications    │  │
│  │  │                   │      • tutor_profile         │  │
│  │  └─────────┬──────────┘                                               │  │
│  │          │                                                          │  │
│  │  ┌──────▼──────────────┐                                              │  │
│  │  │  SQLiteOpenHelper │  ◄── calico_cache.db                  │  │
│  │  │ (CacheDatabase)  │      • cache_home               │  │
│  │  │                 │      • pending_availabilities  │  │
│  │  └─────────────────┘  Lines: 19-146               │  │
│  │                                                                │  │
│  │  ┌─────────────────────┐  ┌──────────────────────────┐                        │  │
│  │  │EncryptedShared │  │   DataStore        │                        │  │
│  │  │Preferences   │  │ (UserPreferences) │                        │  │
│  │  │              │  │                   │                        │  │
│  │  │• TokenManager│  │• cache_expiry_ms  │                        │  │
│  │  │• AES256-GCM  │  │• last_sync_time   │                        │  │
│  │  │  Lines:8-130  │  │• notifications   │                        │  │
│  │  └───────────────┘  │  Lines:32-69     │                        │  │
│  │                    └────────────────���─���───┘                         │  │
│  │                                                                   │  │
│  │  ┌─────────────────────┐  ┌──────────────────────────┐               │  │
│  │  │  FileManager      │  │  InMemoryCache         │               │  │
│  │  │  (App Files)     │  │  (LRU Cache)         │               │  │
│  │  │                 │  │                     │               │  │
│  │  │• backups/       │  │  LinkedHashMap      │               │  │
│  │  │• logs.txt      │  │  maxSize: 20        │               │  │
│  │  │  Lines:22-68  │  │  @Synchronized      │               │  │
│  │  └─────────────────┘  │  Lines:29-71   │               │  │
│  │                       └──────────────────────┘               │  │
│  │                                                                   │  │
│  │  ┌─────────────────────┐  ┌──────────────────────────┐               │  │
│  │  │ ApiResponseCache  │  │  Network Monitoring  │               │  │
│  │  │ (LRU for API)    │  │  (ConnectivityMgr ) │               │  │
│  │  │                 │  │                     │               │  │
│  │  │• approvedCache  │  │ • NetworkCallback  │               │  │
│  │  │• applications   │  │ • auto-refresh   │               │  │
│  │  │  Lines:14-94   │  │  Lines:194-215  │               │  │
│  │  └─────────────────┘  └──────────────────────┘               │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                                                      │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Storage Strategy Summary

| Feature | Technology | When Used |
|---------|-----------|----------|
| **Tokens** | EncryptedSharedPreferences (AES256) | Auth login/logout, API calls |
| **Profile** | SQLite + EncryptedSharedPreferences | Profile screen load |
| **Courses** | SQLite + ApiResponseCache | Courses screen |
| **Applications** | SQLite + Pending Queue | Offline submission |
| **Home Data** | SQLite + InMemoryCache (2-level) | Home screen |
| **User Preferences** | DataStore | Settings |
| **Backups/Logs** | App-specific files | Crash logging |

### Connectivity Strategies

| State | Behavior |
|-------|----------|
| **Online + API success** | Use API data, update all caches |
| **Online + API fail** | Use caches (LRU → SQLite), show stale data |
| **Offline + Cache available** | Use SQLite cache, show offline banner |
| **Offline + No cache** | Show error "No data available" |

### Multi-Threading Patterns

| Pattern | Use Case | Dispatcher |
|--------|----------|------------|
| **async/await** | Parallel API calls (Home 4-data) | Dispatchers.IO |
| **withContext(IO)** | Single DB/API operation | Dispatchers.IO |
| **viewModelScope.launch** | UI-triggered async work | Dispatchers.Main (default) |
| **@Synchronized** | In-memory cache access | Any (thread-safe) |

### Key Files Summary

| File | Lines | Purpose |
|------|-------|---------|
| `DatabaseHelper.kt` | 397 | Main SQLite (courses, apps, profile) |
| `CacheDatabase.kt` | 146 | Cache SQLite (home data) |
| `TokenManager.kt` | 130 | Encrypted token storage |
| `FileManager.kt` | 68 | App files (backups, logs) |
| `UserPreferencesDataStore.kt` | 69 | DataStore preferences |
| `InMemoryCache.kt` | 71 | L1 cache |
| `ApiResponseCache.kt` | 94 | API response cache |
| `CoursesViewModel.kt` | 530 | Courses logic |
| `ProfileViewModel.kt` | 197 | Profile logic |
| `HomeScreenViewModel.kt` | 487 | Home + multi-threading |

---

*Document generated from source code analysis*
*Calico Mobile Kotlin Application*