package com.calico.tutor.util

object EmailValidator {
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
