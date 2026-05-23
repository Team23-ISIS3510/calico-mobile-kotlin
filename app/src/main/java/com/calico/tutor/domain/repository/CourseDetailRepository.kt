package com.calico.tutor.domain.repository

import com.calico.tutor.data.dto.response.TutorCourseData
import com.calico.tutor.data.dto.response.TutorCourseNoteResponseDto
import com.calico.tutor.domain.model.CourseDetail
import com.calico.tutor.domain.utils.Result

interface CourseDetailRepository {
    suspend fun getCourseDetail(courseId: String): Result<CourseDetail>
    suspend fun getTutorCourses(tutorId: String): Result<List<TutorCourseData>>
    suspend fun updateCourseNote(
        tutorId: String,
        courseId: String,
        note: String
    ): Result<TutorCourseNoteResponseDto>
}
