package com.calico.tutor.data.utils

import com.calico.tutor.domain.utils.Result
import kotlinx.coroutines.delay

object RetryHelper {
    suspend fun <T> withRetry(
        maxRetries: Int = 3,
        initialDelayMillis: Long = 100,
        maxDelayMillis: Long = 3000,
        backoffMultiplier: Double = 2.0,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelayMillis
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return Result.Success(block())
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * backoffMultiplier).toLong().coerceAtMost(maxDelayMillis)
                }
            }
        }

        return Result.Error(
            lastException ?: Exception("Unknown error"),
            "Failed after $maxRetries retries"
        )
    }
}

class RetryQueue {
    private val failedRequests = mutableListOf<SuspendBlock<*>>()

    data class SuspendBlock<T>(
        val id: String,
        val block: suspend () -> Result<T>,
        val retries: Int = 0
    )

    fun enqueue(id: String, block: suspend () -> Result<*>) {
        failedRequests.add(SuspendBlock(id, block))
    }

    suspend fun retryAll(): List<Pair<String, Result<*>>> {
        val results = mutableListOf<Pair<String, Result<*>>>()

        for (request in failedRequests) {
            val result = request.block()
            results.add(request.id to result)
        }

        if (results.all { it.second is Result.Success }) {
            failedRequests.clear()
        }

        return results
    }

    fun getPendingRequests(): Int = failedRequests.size

    fun clearQueue() {
        failedRequests.clear()
    }
}
