package com.calico.tutor.ui.viewmodel

import android.content.Context
import android.util.Log
import com.calico.tutor.data.cache.InMemoryCache
import com.calico.tutor.data.dto.response.CourseData
import com.calico.tutor.data.local.CacheDatabase
import com.calico.tutor.data.local.UserPreferencesDataStore
import com.calico.tutor.di.ServiceLocator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

internal object SubjectsCacheLoader {
    private const val TAG = "SubjectsCacheLoader"

    @Suppress("UNCHECKED_CAST")
    suspend fun loadSubjects(
        context: Context,
        cacheDb: CacheDatabase,
        memoryCache: InMemoryCache,
        userPrefs: UserPreferencesDataStore,
        gson: Gson
    ): SubjectsState {
        val cacheKey = CacheDatabase.KEY_SUBJECTS
        val expiryMs = userPrefs.cacheExpiryMs.first()
        val subjectsType = object : TypeToken<List<CourseData>>() {}.type

        memoryCache.get(cacheKey)?.let { entry ->
            Log.d(TAG, "Subjects: L1 cache hit")
            return SubjectsState.Success(entry.value as List<CourseData>)
        }

        val (cachedJson, cachedTs) = cacheDb.getCache(cacheKey)
        val isFresh = cachedJson != null && (System.currentTimeMillis() - cachedTs) < expiryMs

        if (isFresh && cachedJson != null) {
            Log.d(TAG, "Subjects: L2 cache hit (fresh)")
            val subjects = gson.fromJson<List<CourseData>>(cachedJson, subjectsType)
            memoryCache.put(cacheKey, subjects)
            return SubjectsState.Success(subjects)
        }

        return try {
            val response = ServiceLocator.subjectsApiService(context).getSubjectsHistory()
            val subjects = response.data ?: emptyList()
            cacheDb.saveCache(cacheKey, gson.toJson(subjects, subjectsType))
            memoryCache.put(cacheKey, subjects)
            SubjectsState.Success(subjects)
        } catch (e: Exception) {
            Log.e(TAG, "Subjects network error: ${e.message}")
            if (cachedJson != null) {
                val subjects = gson.fromJson<List<CourseData>>(cachedJson, subjectsType)
                memoryCache.put(cacheKey, subjects)
                SubjectsState.Success(subjects)
            } else {
                SubjectsState.Error("No data available")
            }
        }
    }
}