package com.calico.tutor.util

import android.util.Log
import java.util.LinkedHashMap

/**
 * LRU Cache for API responses (courses, applications, etc.)
 * 
 * WHY LruCache over HashMap?
 * - HashMap grows unbounded → OutOfMemoryError
 * - LruCache automatically evicts least-recently-used items when full
 * - Thread-safe with synchronized methods
 */
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

    @Synchronized
    fun contains(key: String): Boolean = cache.containsKey(key)

    @Synchronized
    fun remove(key: String) = cache.remove(key)

    @Synchronized
    fun clear() {
        cache.clear()
        Log.d("ApiCache", "🧹 Cache cleared")
    }

    @Synchronized
    fun size(): Int = cache.size

    companion object {
        @Volatile
        private var approvedCoursesCache: ApiResponseCache<*>? = null
        
        @Volatile
        private var applicationsCache: ApiResponseCache<*>? = null
        
        @Volatile
        private var allCoursesCache: ApiResponseCache<*>? = null

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

        fun clearAll() {
            approvedCoursesCache = null
            applicationsCache = null
            allCoursesCache = null
        }
    }
}