## 3.2 Kotlin Version

### 3.2.1 New feature: Course Details and Notes

**Owner:** Daniel Camilo Quimbay Velazquez>

| Field | Content |
| :--- | :--- |
| **Event description** | User navigates to the Course Details screen to access course context, study notes, and recent sessions. The view functions reliably under offline conditions. |
| **System response** | L2 SQLite cache is queried first via `Dispatchers.IO` to ensure immediate UI rendering. If the network is available, fresh course context and recent sessions are fetched, updating both the L2 cache and UI state on `Dispatchers.Main`. |
| **Possible anti-patterns** | Re-fetching static course notes repeatedly; executing database reads on the main thread; failing to sync offline notes edits when connectivity is restored. |
| **Caching strategy** | Single-level persistent cache (SQLite) inherited from the Sprint 3 course flow. Read policy: SQLite → API → SQLite update. |
| **Storage type** | SQLite. Selected for robust offline persistence across application restarts. |
| **Stored data type** | Relational or serialized JSON payload containing course metadata, text-based notes, and session identifiers. |

#### Local Storage Strategies


| Technology | Feature / data stored | Why chosen | Access pattern | Persistence / invalidation |
| :--- | :--- | :--- | :--- | :--- |
| **SQLite** | Course Details & Notes | Ensures offline availability and reuses the Sprint 3 architecture. Avoids repeated network calls for static context. | Read on screen load, upsert on network sync. | Persists across restarts. Invalidated on explicit user sync or predefined TTL. |

#### Multithreading Strategies


**Strategy 1 — `viewModelScope.launch` with `withContext(Dispatchers.IO)`**

| Field | Content |
| :--- | :--- |
| **How it works** | A coroutine launched on the main thread suspends to `Dispatchers.IO` for SQLite querying and API fetching. The UI state is updated upon return to `Dispatchers.Main`. |
| **Why this over alternatives** | Provides structured concurrency without blocking the main thread. Avoids callback hell and integrates seamlessly with Kotlin's architecture. |
| **Threading model** | Main thread (initiation) → IO thread pool (Database/Network operations) → Main thread (UI update). |
| **Error handling** | `try/catch` wraps the I/O block. Falls back to offline SQLite data if the API call fails. Emits an Error state if the local cache is empty. |

#### Caching Strategies

**Cache 1 — SQLite Persistent Store (Course Details)**

| Field | Content |
| :--- | :--- |
| **Data structure** | SQLite tables holding course details and notes payloads. |
| **Write policy** | Upsert on successful API retrieval. |
| **Read policy** | Queried via primary key (e.g., `course_id`). |
| **TTL** | Controlled by Sprint 3 synchronization logic. |
| **Concurrency** | Suspended execution on `Dispatchers.IO` ensures thread safety and prevents Main Thread blocking. |
| **Where used** | Course details initialization and offline viewing states. |
---

# 4.2 Kotlin Version

## 4.2.1 Views Summary

| View ID | View Name | Team Member | Related Feature |
|:---|:---|:---|:---|
| V004 | Login Screen | Daniel Camilo Quimbay Velazquez | F001 |
| V005 | Register Screen | Daniel Camilo Quimbay Velazquez | F001 |
| V006 | Course Details and Notes | Daniel Camilo Quimbay Velazquez | F003 |

## 4.2.3 Course Details and Notes

**View Description**

The Course Details and Notes screen provides a single place where students and tutors can inspect a course's full context before applying, booking, or starting a session. It combines course metadata (title, summary, syllabus), a compact notes area (personal notes and optional shared notes), and a list of recent sessions for the selected course.

The layout is uncluttered with clear typographic hierarchy: course title and primary actions at the top, key metadata in a summary row, notes editable inline, and recent sessions presented as cards below. The back button and persistent bottom navigation provide orientation and quick return to other areas of the app.

Notes are scoped to the course and can be marked Private (local-only) or Shared (synced). Recent sessions show the most relevant activity for the course (recent completed sessions and upcoming slots), assisting students in deciding whether to apply or continue learning.

The screen follows offline-first behavior: cached course details and cached recent sessions (Sprint 3 SQLite cache) are shown immediately while fresh data is fetched in the background. Long-running work (DB reads, network calls, image decoding) runs off the main thread and updates UI via `StateFlow`/`LiveData`.

**UI Components (Android/Kotlin)**

| Component | Description |
|:---|:---|
| `ImageButton` / `Toolbar` | Back button at top-left to return to previous screen |
| `TextView` | Course title, large and bold (`textAppearance="headlineSmall"`) |
| `ImageView` | Course cover image or placeholder |
| `Chip` / `TextView` | Short metadata row: difficulty, duration, language, price |
| `Button` | Primary CTA: "Apply" or "Book" (enabled/disabled by availability) |
| `Button` | Secondary CTA: "Contact Tutor" / "Message" |
| `CardView` | Notes editor container with edit/save controls (inline) |
| `EditText` | Notes input (local draft saved to SQLite/DataStore) |
| `Switch` / `Checkbox` | Private vs Shared note toggle |
| `TextView` | Syllabus and long-form description (collapsible) |
| `RecyclerView` | Recent sessions list (vertical) using `LinearLayoutManager` |
| `CardView` | Session card in recent list: tutor/student name, date, price, rating |
| `BottomNavigationView` | Persistent bar with navigation options, unchanged |
| `SwipeRefreshLayout` | Pull-to-refresh to refresh course data |

**Screen Behavior (Kotlin + ViewModel)**

| State | Behavior |
|:---|:---|
| Default | Show cached course details and cached recent sessions; UI bound to `StateFlow` from ViewModel. |
| Loading | Show skeleton/shimmer for cover and text while fetching fresh data from SQLite/API on `Dispatchers.IO`. |
| Empty (no notes) | Show a prompt: "Add a personal note to remember why this course matters" with an Add Note CTA. |
| Saving Note | Optimistic local save: note persisted to local DB immediately and background-synced if marked Shared. Show small "Saving..." indicator. |
| Note Save Failure (shared) | Keep local copy and mark `sync_status = pending`; surface a retry action in notes UI. |
| Offline | Display cached course details and cached recent sessions; disable actions that require immediate network (e.g., messaging) but allow adding Private notes and queue Shared notes for sync. |
| Apply / Book tapped | If online: call API via `Dispatchers.IO`, show progress, on success update local cache and navigate. If offline: insert pending application row into SQLite queue and show "Pending Sync" badge. |
| Recent session tapped | Open Session Detail screen (optional) with full session information and notes. |
| Scroll | Paginate recent sessions when approaching list end (Paging or manual load-more). |

**Caching & Storage**

- Course details and recent sessions reuse the Sprint 3 SQLite cache. Read path: In-memory L1 -> SQLite L2 -> API. L1 is small in-memory cache for the open course; L2 is `cached_course` / `cached_course_sessions` tables keyed by `course_id` with `json_payload` + `last_updated`.
- Notes: Private notes stored locally (SQLite or DataStore) and never synced. Shared notes are written locally with `sync_status` flag and queued for background sync when network returns.
- Pending apply/book actions follow the same FIFO SQLite queue used elsewhere (table `pending_applications`).

**Threading model**

- UI actions on `Dispatchers.Main` only. Database reads/writes and network calls run on `Dispatchers.IO` via `viewModelScope.launch` and `withContext(Dispatchers.IO)`.
- Parallel loads (cover image + course payload + recent sessions) use `async(Dispatchers.IO)` and `await()` to minimize perceived latency.
- All shared caches guarded by synchronization or mutexes as used in `core/cache/InMemoryCache.kt`.

**Accessibility & UX details**

- Large tappable areas for CTAs; high-contrast text for readability; content described with `contentDescription` for images.
- Collapsible syllabus with "Read more" link to keep the top area compact.
- Private notes have a clear label and a small lock icon; shared notes show sync status (✓ / syncing / ! ).

**Code locations (examples)**

- Repository / cache: `features/courses/data/CoursesRepository.kt` · `core/storage/AppDatabaseService.kt`
- ViewModel: `features/courses/presentation/CourseDetailViewModel.kt`
- UI: `features/courses/presentation/CourseDetailScreen.kt`

**Screen Mockup**

*(Place your screenshot here)*

![Course Details Screen](path/to/course_details_screen.png)

---


---

# 5 Business Questions (BQ)

## 5.1 BQ Summary

| ID | Business Question | Type | Owner | Sprint |
|:---|:---|:---|:---|:---|
| BQ1 | Telemetry and app stability | Type 1 | Daniel Camilo Quimbay Velazquez | 2 |
| BQ2 | Notification and user experience improvement | Type 2 | Daniel Camilo Quimbay Velazquez | 3 |
| BQ3 | Sellable data opportunity | Type 4 | Daniel Camilo Quimbay Velazquez | 4 |

