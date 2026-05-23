package com.calico.tutor.data.mapper

import com.calico.tutor.data.dto.response.CourseDetail
import com.calico.tutor.domain.model.CourseDetail as DomainCourseDetail

fun CourseDetail.toDomain(): DomainCourseDetail = DomainCourseDetail(
    id = id,
    name = name,
    code = code,
    credits = credits,
    faculty = faculty,
    description = description,
    prerequisites = prerequisites,
    difficulty = difficulty,
    semester = semester
)
