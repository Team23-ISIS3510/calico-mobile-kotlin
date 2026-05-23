package com.calico.tutor.ui.viewmodel

import android.content.Context
import android.util.Log
import com.calico.tutor.data.cache.InMemoryCache
import com.calico.tutor.data.dto.response.TutoringSessionData
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
    private const val KEY_USER_PREFIX = "history_user_"
    private val completedStatuses = setOf("completed", "approved", "done", "finished", "past")

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 1 del patrón stale-while-revalidate:
    // Lee únicamente desde caché local (L1 → L2) SIN tocar la red.
    // Devuelve null si no existe ningún dato local (cache miss total).
    // Dispatchers.IO: SQLite no puede ejecutarse en Main Thread.
    // ─────────────────────────────────────────────────────────────────────────
<<<<<<< HEAD

=======
>>>>>>> 42aaf7325d55be5c8100809aa7f873f3cbab201f
    @Suppress("UNCHECKED_CAST")
    suspend fun readCachedTutorHistory(
        context: Context,
        cacheDb: CacheDatabase,
        memoryCache: InMemoryCache,
        userPrefs: UserPreferencesDataStore,
        fileManager: FileManager,
        gson: Gson,
        tutorId: String
    ): HistoryState? = withContext(Dispatchers.IO) {
        val cacheKey = buildTutorCacheKey(tutorId)
        val sessionType = object : TypeToken<List<TutoringSessionData>>() {}.type

        // L1: InMemoryCache (LRU)
        memoryCache.get(cacheKey)?.let { entry ->
            Log.d(TAG, "History: cache hit L1 (memoria) para $tutorId")
            val sessions = entry.value as List<TutoringSessionData>
            return@withContext HistoryState.Success(
                mapCompletedSessions(context, cacheDb, memoryCache, userPrefs, gson, sessions)
            )
        }

        // L2: SQLite
        val (cachedJson, _) = cacheDb.getCache(cacheKey)
        cachedJson?.let {
            Log.d(TAG, "History: cache hit L2 (SQLite) para $tutorId — puede ser stale")
            val sessions = gson.fromJson<List<TutoringSessionData>>(it, sessionType)
            memoryCache.put(cacheKey, sessions)
            return@withContext HistoryState.Success(
                mapCompletedSessions(context, cacheDb, memoryCache, userPrefs, gson, sessions)
            )
        }

        null
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun readCachedStudentHistory(
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
    ): HistoryState? = withContext(Dispatchers.IO) {
        val cacheKey = buildStudentCacheKey(studentId, startDate, endDate, course, limit)
        val sessionType = object : TypeToken<List<TutoringSessionData>>() {}.type

        memoryCache.get(cacheKey)?.let { entry ->
            Log.d(TAG, "History: cache hit L1 (memoria) para student $studentId")
            val sessions = entry.value as List<TutoringSessionData>
            return@withContext HistoryState.Success(
                mapCompletedSessions(context, cacheDb, memoryCache, userPrefs, gson, sessions)
            )
        }

        val (cachedJson, _) = cacheDb.getCache(cacheKey)
        cachedJson?.let {
            Log.d(TAG, "History: cache hit L2 (SQLite) para student $studentId")
            val sessions = gson.fromJson<List<TutoringSessionData>>(it, sessionType)
            memoryCache.put(cacheKey, sessions)
            return@withContext HistoryState.Success(
                mapCompletedSessions(context, cacheDb, memoryCache, userPrefs, gson, sessions)
            )
        }

        null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 2 del patrón stale-while-revalidate:
    // Llama a la API, guarda resultado en SQLite (L2) e InMemoryCache (L1),
    // y registra el evento en FileManager.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun fetchAndCacheTutorHistory(
        context: Context,
        cacheDb: CacheDatabase,
        memoryCache: InMemoryCache,
        fileManager: FileManager,
        gson: Gson,
        userPrefs: UserPreferencesDataStore,
        tutorId: String
    ): HistoryState = withContext(Dispatchers.IO) {
        val cacheKey = buildTutorCacheKey(tutorId)
        val sessionType = object : TypeToken<List<TutoringSessionData>>() {}.type

        val response = ServiceLocator.subjectsApiService(context)
            .getPreviousTutoringSessionsForTutor(tutorId)
        val sessions = response.sessions

        cacheDb.saveCache(cacheKey, gson.toJson(sessions, sessionType))
        memoryCache.put(cacheKey, sessions)
        fileManager.appendLog("History actualizada de red para $tutorId (stale-while-revalidate)")

        HistoryState.Success(
            mapCompletedSessions(context, cacheDb, memoryCache, userPrefs, gson, sessions)
        )
    }

    suspend fun fetchAndCacheStudentHistory(
        context: Context,
        cacheDb: CacheDatabase,
        memoryCache: InMemoryCache,
        fileManager: FileManager,
        gson: Gson,
        userPrefs: UserPreferencesDataStore,
        studentId: String,
        startDate: String? = null,
        endDate: String? = null,
        course: String? = null,
        limit: Int? = null
    ): HistoryState? = withContext(Dispatchers.IO) {
        val cacheKey = buildStudentCacheKey(studentId, startDate, endDate, course, limit)
        val sessionType = object : TypeToken<List<TutoringSessionData>>() {}.type

        val response = ServiceLocator.subjectsApiService(context).getStudentTutoringSessionsHistory(
            studentId = studentId,
            startDate = startDate,
            endDate = endDate,
            course = course,
            limit = limit
        )
        val sessions = response.sessions

        cacheDb.saveCache(cacheKey, gson.toJson(sessions, sessionType))
        memoryCache.put(cacheKey, sessions)
        fileManager.appendLog("History actualizada de red para student $studentId (stale-while-revalidate)")

        HistoryState.Success(
            mapCompletedSessions(context, cacheDb, memoryCache, userPrefs, gson, sessions)
        )
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