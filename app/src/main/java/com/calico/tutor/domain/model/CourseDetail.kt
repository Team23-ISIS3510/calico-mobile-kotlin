package com.calico.tutor.domain.model

data class CourseDetail(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val credits: Int = 0,
    val faculty: String = "",
    val description: String = "",
    val prerequisites: List<String> = emptyList(),
    val difficulty: String = "",
    val semester: String = ""
)
