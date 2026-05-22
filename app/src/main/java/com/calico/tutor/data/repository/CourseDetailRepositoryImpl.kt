package com.calico.tutor.data.repository

import android.util.Log
import com.calico.tutor.data.cache.InMemoryCache
import com.calico.tutor.data.datasource.remote.SubjectsApiService
import com.calico.tutor.data.local.CacheDatabase
import com.calico.tutor.data.local.UserPreferencesDataStore
import com.calico.tutor.data.dto.request.UpdateTutorCourseNoteDto
import com.calico.tutor.data.dto.response.TutorCourseData
import com.calico.tutor.data.dto.response.TutorCourseNoteResponseDto
import com.calico.tutor.data.mapper.toDomain
import com.calico.tutor.domain.model.CourseDetail
import com.calico.tutor.domain.repository.CourseDetailRepository
import com.calico.tutor.domain.utils.Result
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CourseDetailRepositoryImpl(
    private val subjectsApiService: SubjectsApiService,
    private val cacheDatabase: CacheDatabase,
    private val memoryCache: InMemoryCache,
    private val userPreferences: UserPreferencesDataStore,
    private val gson: Gson = Gson()
) : CourseDetailRepository {

    private val tag = "CourseDetailRepository"

    override suspend fun getCourseDetail(courseId: String): Result<CourseDetail> {
        val cacheKey = "course_detail_$courseId"
        val expiryMs = userPreferences.cacheExpiryMs.first()

        memoryCache.get(cacheKey)?.let { entry ->
            (entry.value as? CourseDetail)?.let { cached ->
                Log.d(tag, "Course detail L1 cache hit for $courseId")
                return Result.Success(cached)
            }
        }

        val (cachedJson, cachedTs) = cacheDatabase.getCache(cacheKey)
        val isFresh = cachedJson != null && (System.currentTimeMillis() - cachedTs) < expiryMs
        if (isFresh && cachedJson != null) {
            val cached = gson.fromJson(cachedJson, CourseDetail::class.java)
            memoryCache.put(cacheKey, cached)
            Log.d(tag, "Course detail L2 cache hit for $courseId")
            return Result.Success(cached)
        }

        return try {
            val response = withContext(Dispatchers.IO) {
                subjectsApiService.getCourseById(courseId)
            }
            val detail = response.course
                ?: return Result.Error(Exception("Missing course detail"), "Course detail not found")
            val domain = detail.toDomain()
            cacheDatabase.saveCache(cacheKey, gson.toJson(domain))
            memoryCache.put(cacheKey, domain)
            Result.Success(domain)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching course detail: ${e.message}")
            if (cachedJson != null) {
                val cached = gson.fromJson(cachedJson, CourseDetail::class.java)
                memoryCache.put(cacheKey, cached)
                Result.Success(cached)
            } else {
                Result.Error(e, "Error fetching course detail: ${e.message}")
            }
        }
    }

    override suspend fun getTutorCourses(tutorId: String): Result<List<TutorCourseData>> {
        return try {
            Result.Success(subjectsApiService.getTutorCourses(tutorId))
        } catch (e: Exception) {
            Log.e(tag, "Error fetching tutor courses: ${e.message}")
            Result.Error(e, "Error fetching tutor courses: ${e.message}")
        }
    }

    override suspend fun updateCourseNote(
        tutorId: String,
        courseId: String,
        note: String
    ): Result<TutorCourseNoteResponseDto> {
        return try {
            val response = subjectsApiService.updateTutorCourseNote(
                tutorId = tutorId,
                courseId = courseId,
                request = UpdateTutorCourseNoteDto(note)
            )
            Result.Success(response)
        } catch (e: Exception) {
            Log.e(tag, "Error updating course note: ${e.message}")
            Result.Error(e, "Error updating course note: ${e.message}")
        }
    }
}
