package com.calico.tutor.domain.model

data class AuthToken(
    val idToken: String = "user",
    val refreshToken: String = "",
    val expiresIn: Long = 3600
)

data class User(
    val email: String,
    val name: String,
    val phone: String,
    val isTutor: Boolean,
    val courses: List<String>? = null
)

data class Subject(
    val id: String,
    val name: String,
    val code: String,
    val count: Int
)

data class SubjectsHistory(
    val subjects: List<Subject>
)

data class Session(
    val id: String,
    val date: String,
    val time: String,
    val tutorName: String,
    val subjectName: String,
    val subjectCode: String,
    val tutorId: String = "",
    val status: String = "completed"
)

data class SessionHistory(
    val sessions: List<Session>
)
