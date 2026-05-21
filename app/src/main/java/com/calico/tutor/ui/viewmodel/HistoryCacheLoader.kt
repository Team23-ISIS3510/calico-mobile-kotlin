package com.calico.tutor.ui.viewmodel

import android.content.Context
import android.util.Log
import com.calico.tutor.data.cache.InMemoryCache
import com.calico.tutor.data.dto.response.TutoringSessionData
import com.calico.tutor.data.dto.response.TutoringSessionsResponse
import com.calico.tutor.data.dto.response.UserProfileResponse
import com.calico.tutor.data.local.CacheDatabase
import com.calico.tutor.data.local.FileManager
import com.calico.tutor.data.local.UserPreferencesDataStore
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.domain.model.Session
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object HistoryCacheLoader {
    private const val TAG = "HistoryCacheLoader"
    private const val KEY_TUTOR_SUBJECTS = "history_subjects"
    private const val KEY_USER_PREFIX = "history_user_"
    private val completedStatuses = setOf("completed", "approved", "done", "finished", "past")

    // Fetches sessions for a tutor through the 2-level cache pipeline (L1→L2→Network).
    // Subject-cache warmup is intentionally delegated to the ViewModel so that caller
    // controls concurrency. FileManager logging mirrors the pattern used in HomeScreenViewModel.
    suspend fun loadTutorHistory(
        context: Context,
        cacheDb: CacheDatabase,
        memoryCache: InMemoryCache,
        userPrefs: UserPreferencesDataStore,
        fileManager: FileManager,
        gson: Gson,
        tutorId: String
    ): HistoryState = withContext(Dispatchers.IO) {
        val cacheKey = buildTutorCacheKey(tutorId)
        loadHistory(
            context = context,
            cacheDb = cacheDb,
            memoryCache = memoryCache,
            userPrefs = userPrefs,
            fileManager = fileManager,
            gson = gson,
            cacheKey = cacheKey,
            fetcher = {
                ServiceLocator.subjectsApiService(context)
                    .getPreviousTutoringSessionsForTutor(tutorId)
            }
        )
    }

    suspend fun loadStudentHistory(
        context: Context,
        cacheDb: CacheDatabase,
        memoryCache: InMemoryCache,
        userPrefs: UserPreferencesDataStore,
        fileManager: FileManager,
        gson: Gson,
        studentId: String,
        startDate: String? = null,
        endDate: String? = null,
        course: String? = null,
        limit: Int? = null
    ): HistoryState = withContext(Dispatchers.IO) {
        val cacheKey = buildStudentCacheKey(studentId, startDate, endDate, course, limit)
        loadHistory(
            context = context,
            cacheDb = cacheDb,
            memoryCache = memoryCache,
            userPrefs = userPrefs,
            fileManager = fileManager,
            gson = gson,
            cacheKey = cacheKey,
            fetcher = {
                ServiceLocator.subjectsApiService(context).getStudentTutoringSessionsHistory(
                    studentId = studentId,
                    startDate = startDate,
                    endDate = endDate,
                    course = course,
                    limit = limit
                )
            }
        )
    }

    /**
     * Núcleo del pipeline de caché de 2 niveles para History.
     *
     * Flujo idéntico al de HomeScreenViewModel.fetchSessionsWithCache:
     *   L1 (InMemoryCache) → L2 SQLite fresco → Red → L2 SQLite expirado (offline fallback) → Error
     *
     * Dispatchers.IO: todas las operaciones de SQLite y red se ejecutan aquí ya que el
     * llamador (loadTutorHistory / loadStudentHistory) usa withContext(Dispatchers.IO).
     * FileManager registra los mismos eventos que Home para auditoría y debugging offline.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun loadHistory(
        context: Context,
        cacheDb: CacheDatabase,
        memoryCache: InMemoryCache,
        userPrefs: UserPreferencesDataStore,
        fileManager: FileManager,
        gson: Gson,
        cacheKey: String,
        fetcher: suspend () -> TutoringSessionsResponse
    ): HistoryState {
        val expiryMs = userPrefs.cacheExpiryMs.first()
        val sessionType = object : TypeToken<List<TutoringSessionData>>() {}.type

        // Nivel 1: caché en memoria (LRU) — acceso O(1), sin I/O de disco
        memoryCache.get(cacheKey)?.let { entry ->
            Log.d(TAG, "History: L1 cache hit for $cacheKey")
            val sessions = entry.value as List<TutoringSessionData>
            return HistoryState.Success(mapCompletedSessions(context, cacheDb, memoryCache, userPrefs, gson, sessions))
        }

        // Nivel 2: SQLite — persistencia entre sesiones de la app; verificar frescura
        val (cachedJson, cachedTs) = cacheDb.getCache(cacheKey)
        val isFresh = (System.currentTimeMillis() - cachedTs) < expiryMs

        if (cachedJson != null && isFresh) {
            Log.d(TAG, "History: L2 cache hit (fresh) for $cacheKey")
            val sessions = gson.fromJson<List<TutoringSessionData>>(cachedJson, sessionType)
            memoryCache.put(cacheKey, sessions)
            return HistoryState.Success(mapCompletedSessions(context, cacheDb, memoryCache, userPrefs, gson, sessions))
        }

        // Nivel 3: Red — fetch remoto + sincronización API → SQLite (L2) → InMemoryCache (L1)
        return try {
            val response = fetcher()
            val sessions = response.sessions
            cacheDb.saveCache(cacheKey, gson.toJson(sessions, sessionType))   // API → SQLite
            memoryCache.put(cacheKey, sessions)                               // API → InMemoryCache
            fileManager.appendLog("History cargada de red para $cacheKey")
            HistoryState.Success(mapCompletedSessions(context, cacheDb, memoryCache, userPrefs, gson, sessions))
        } catch (e: Exception) {
            Log.e(TAG, "History network error for $cacheKey: ${e.message}")
            fileManager.appendLog("History network error para $cacheKey: ${e.message}")
            // Fallback offline: usar L2 aunque esté expirado (tolerancia a fallos, mismo patrón que Home)
            cachedJson?.let {
                val sessions = gson.fromJson<List<TutoringSessionData>>(it, sessionType)
                memoryCache.put(cacheKey, sessions)
                fileManager.appendLog("History: usando caché local (offline) para $cacheKey")
                HistoryState.Success(mapCompletedSessions(context, cacheDb, memoryCache, userPrefs, gson, sessions))
            } ?: HistoryState.Error("Failed to load tutoring history.")
        }
    }

    private suspend fun mapCompletedSessions(
        context: Context,
        cacheDb: CacheDatabase,
        memoryCache: InMemoryCache,
        userPrefs: UserPreferencesDataStore,
        gson: Gson,
        rawSessions: List<TutoringSessionData>
    ): List<Session> {
        val completedSessions = rawSessions.filter { data ->
            val startMillis = parseTimestamp(data.scheduledStart)
            val status = data.status.lowercase(Locale.getDefault())
            val isPast = startMillis != 0L && startMillis <= System.currentTimeMillis()
            val isCompleted = status in completedStatuses
            isPast || isCompleted
        }

        val uniqueStudentIds = completedSessions
            .mapNotNull { it.studentId.takeIf(String::isNotBlank) }
            .distinct()

        // Resolución concurrente de perfiles: una corrutina async por cada estudiante único,
        // todas corriendo en paralelo en Dispatchers.IO. awaitAll() garantiza que ninguna
        // sesión se mapee antes de tener todos los perfiles resueltos.
        val resolvedUsers = coroutineScope {
            uniqueStudentIds
                .map { studentId ->
                    async {
                        studentId to resolveUserProfile(context, cacheDb, memoryCache, userPrefs, gson, studentId)
                    }
                }
                .awaitAll()
                .toMap()
        }

        return completedSessions
            .map { data ->
                val resolvedUser = resolvedUsers[data.studentId]
                val displayName = resolvedUser?.name?.takeIf { it.isNotBlank() }
                    ?: data.studentName.takeIf { it.isNotBlank() }
                    ?: "Student Not Available"
                val avatarUrl = resolvedUser?.profileImage
                    ?: resolvedUser?.profilePictureUrl
                    ?: resolvedUser?.avatarUrl
                    ?: data.studentAvatarUrl

                Session(
                    id = data.id,
                    studentId = data.studentId,
                    scheduledStart = data.scheduledStart,
                    scheduledEnd = data.scheduledEnd,
                    status = data.status,
                    course = data.course,
                    courseId = data.courseId,
                    date = formatDate(data.scheduledStart),
                    time = formatTimeRange(data.scheduledStart, data.scheduledEnd),
                    tutorName = "",
                    subjectName = data.course ?: data.courseId ?: "Unknown",
                    subjectCode = data.courseId.orEmpty(),
                    studentName = displayName,
                    studentAvatarUrl = avatarUrl.orEmpty(),
                    price = data.price
                )
            }
            .sortedByDescending { parseTimestamp(it.scheduledStart) }
    }

    private suspend fun resolveUserProfile(
        context: Context,
        cacheDb: CacheDatabase,
        memoryCache: InMemoryCache,
        userPrefs: UserPreferencesDataStore,
        gson: Gson,
        userId: String
    ): UserProfileResponse? {
        val cacheKey = "$KEY_USER_PREFIX$userId"
        memoryCache.get(cacheKey)?.let { entry ->
            return entry.value as? UserProfileResponse
        }

        val (cachedJson, cachedTs) = cacheDb.getCache(cacheKey)
        val expiryMs = userPrefs.cacheExpiryMs.first()
        val isFresh = (System.currentTimeMillis() - cachedTs) < expiryMs
        if (cachedJson != null && isFresh) {
            val user = gson.fromJson(cachedJson, UserProfileResponse::class.java)
            memoryCache.put(cacheKey, user)
            return user
        }

        return try {
            val user = ServiceLocator.usersApiService(context).getUserById(userId)
            cacheDb.saveCache(cacheKey, gson.toJson(user))
            memoryCache.put(cacheKey, user)
            user
        } catch (e: Exception) {
            Log.e(TAG, "User lookup failed for $userId: ${e.message}")
            cachedJson?.let {
                val user = gson.fromJson(it, UserProfileResponse::class.java)
                memoryCache.put(cacheKey, user)
                user
            }
        }
    }

    private fun formatDate(timestamp: String): String {
        val parsed = parseTimestamp(timestamp)
        if (parsed == 0L) return "Not available"
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(parsed))
    }

    private fun formatTimeRange(startTimestamp: String, endTimestamp: String): String {
        val start = parseTimestamp(startTimestamp)
        if (start == 0L) return "Not available"

        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val startText = formatter.format(Date(start))
        val end = parseTimestamp(endTimestamp)
        return if (end > 0L) {
            "$startText - ${formatter.format(Date(end))}"
        } else {
            startText
        }
    }

    private fun parseTimestamp(value: String): Long {
        if (value.isBlank()) return 0L

        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )

        for (format in formats) {
            try {
                return format.parse(value)?.time ?: 0L
            } catch (_: Exception) {
            }
        }

        return 0L
    }

    private fun buildTutorCacheKey(tutorId: String): String = "${CacheDatabase.KEY_HISTORY}_tutor_$tutorId"

    private fun buildStudentCacheKey(
        studentId: String,
        startDate: String?,
        endDate: String?,
        course: String?,
        limit: Int?
    ): String {
        return buildString {
            append(CacheDatabase.KEY_HISTORY)
            append("_student_")
            append(studentId)
            append("_start_")
            append(startDate.orEmpty())
            append("_end_")
            append(endDate.orEmpty())
            append("_course_")
            append(course.orEmpty())
            append("_limit_")
            append(limit?.toString().orEmpty())
        }
    }
}