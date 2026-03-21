package com.calico.tutor.util

import com.auth0.jwt.JWT
import android.util.Log

object JwtUtils {
    /**
     * Decodes a JWT token and extracts the Firebase UID from the 'sub' claim
     * @param token The JWT token (idToken from Firebase)
     * @return The Firebase UID (subject claim) or null if extraction fails
     */
    fun extractFirebaseUid(token: String): String? {
        return try {
            val decodedJWT = JWT.decode(token)
            val uid = decodedJWT.getClaim("sub").asString()
            Log.d("JwtUtils", "✅ Extracted Firebase UID: $uid")
            uid
        } catch (e: Exception) {
            Log.e("JwtUtils", "❌ Error decoding JWT token: ${e.message}")
            null
        }
    }

    /**
     * Extracts email from JWT token
     * @param token The JWT token
     * @return The email from 'email' claim or null if not found
     */
    fun extractEmail(token: String): String? {
        return try {
            val decodedJWT = JWT.decode(token)
            decodedJWT.getClaim("email").asString()
        } catch (e: Exception) {
            Log.e("JwtUtils", "Error extracting email from JWT: ${e.message}")
            null
        }
    }
}
