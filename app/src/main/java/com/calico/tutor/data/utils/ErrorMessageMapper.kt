package com.calico.tutor.data.utils

import android.util.Log
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps technical exceptions to user-friendly error messages.
 * Handles network errors, HTTP errors, and unexpected failures gracefully.
 */
object ErrorMessageMapper {
    private const val TAG = "ErrorMessageMapper"

    /**
     * Converts any exception to a user-friendly message.
     * 
     * @param exception The exception to convert
     * @return A user-friendly error message
     */
    fun getErrorMessage(exception: Throwable): String {
        Log.e(TAG, "Error occurred: ${exception.javaClass.simpleName} - ${exception.message}", exception)

        return when (exception) {
            // Network connectivity issues
            is ConnectException, is UnknownHostException -> {
                "Unable to connect to the server. Please check your internet connection and try again."
            }
            is SocketTimeoutException -> {
                "The server took too long to respond. Please check your connection and try again."
            }
            is IOException -> {
                "Network error occurred. Please try again later."
            }
            // HTTP errors with response bodies
            is HttpException -> {
                when {
                    exception.code() in 400..499 -> {
                        parseHttpErrorMessage(exception) ?: when (exception.code()) {
                            400 -> "Invalid request. Please check your input and try again."
                            401 -> "Invalid email or password"
                            403 -> "You don't have permission to perform this action."
                            404 -> "The requested resource was not found."
                            else -> "Request failed. Please try again."
                        }
                    }
                    exception.code() in 500..599 -> {
                        "Server error. Please try again later."
                    }
                    else -> "Something went wrong. Please try again."
                }
            }
            // Fallback for unknown errors
            else -> {
                "Something went wrong. Please try again later."
            }
        }
    }

    /**
     * Attempts to parse a user-friendly message from the HTTP error response body.
     * 
     * @param exception The HttpException
     * @return The parsed message field from the response, or null if unavailable
     */
    private fun parseHttpErrorMessage(exception: HttpException): String? {
        return try {
            val errorBody = exception.response()?.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val errorJson = com.google.gson.Gson().fromJson(errorBody, Map::class.java)
                (errorJson["message"] as? String)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse error response: ${e.message}")
            null
        }
    }
}
