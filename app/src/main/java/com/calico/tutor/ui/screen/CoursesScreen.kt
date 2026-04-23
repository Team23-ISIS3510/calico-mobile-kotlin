package com.calico.tutor.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calico.tutor.data.dto.response.TutorCourseData
import com.calico.tutor.data.dto.response.CourseApplicationResponse
import com.calico.tutor.data.dto.response.AvailableCourseResponse
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.ui.theme.*
import android.content.Context
import android.util.Log

sealed class CoursesState {
    object Idle : CoursesState()
    object Loading : CoursesState()
    data class Success(
        val approvedCourses: List<TutorCourseData>,
        val availableCourses: List<AvailableCourseResponse>,
        val applications: List<CourseApplicationResponse>
    ) : CoursesState()
    data class Error(val message: String) : CoursesState()
}

@Composable
fun CoursesScreen(
    tutorId: String = "tutor@example.com",
    context: Context? = null
) {
    var coursesState by remember { mutableStateOf<CoursesState>(CoursesState.Idle) }
    var showApplicationDialog by remember { mutableStateOf(false) }
    var selectedCourseForApplication by remember { mutableStateOf<AvailableCourseResponse?>(null) }
    var isApplying by remember { mutableStateOf(false) }
    var applicationNotes by remember { mutableStateOf("") }
    var expandedApproved by remember { mutableStateOf(true) }
    var expandedAvailable by remember { mutableStateOf(true) }
    var expandedPending by remember { mutableStateOf(true) }

    // Load data
    LaunchedEffect(tutorId) {
        if (context != null) {
            coursesState = CoursesState.Loading
            try {
                Log.d("CoursesScreen", "Loading courses and applications for tutor: $tutorId")
                val subjectsApiService = ServiceLocator.subjectsApiService(context)
                
                val approvedCourses = subjectsApiService.getTutorCourses(tutorId)
                val applications = subjectsApiService.getTutorApplications(tutorId)
                val allCoursesResponse = subjectsApiService.getAllAvailableCourses()
                
                // Extract courses from response
                val allAvailableCourses = allCoursesResponse.courses
                
                // Mark courses as applied if there's a pending/approved application
                val markedCourses = allAvailableCourses.map { course ->
                    val hasApplication = applications.any { it.courseId == course.id }
                    course.copy(hasApplied = hasApplication)
                }
                
                // Filter out already approved courses from available
                val availableCourses = markedCourses.filter { available ->
                    approvedCourses.none { approved -> approved.id == available.id }
                }
                
                Log.d("CoursesScreen", "Loaded: ${approvedCourses.size} approved, ${availableCourses.size} available, ${applications.size} applications")
                coursesState = CoursesState.Success(approvedCourses, availableCourses, applications)
            } catch (e: Exception) {
                Log.e("CoursesScreen", "Error loading data: ${e.message}", e)
                coursesState = CoursesState.Error("Error: ${e.message}")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBase)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "COURSES",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = coursesState) {
                CoursesState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryOrange)
                    }
                }
                is CoursesState.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text(
                            text = state.message,
                            color = Color.Red,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is CoursesState.Success -> {
                    // Approved Courses Section
                    if (state.approvedCourses.isNotEmpty()) {
                        CollapsibleSection(
                            title = "MY COURSES (${state.approvedCourses.size})",
                            isExpanded = expandedApproved,
                            onToggle = { expandedApproved = !expandedApproved }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                state.approvedCourses.forEach { course ->
                                    ApprovedCourseCardCompact(course)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Available Courses Section
                    if (state.availableCourses.isNotEmpty()) {
                        CollapsibleSection(
                            title = "AVAILABLE COURSES TO APPLY (${state.availableCourses.size})",
                            isExpanded = expandedAvailable,
                            onToggle = { expandedAvailable = !expandedAvailable }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                state.availableCourses.forEach { course ->
                                    AvailableCourseCardCompact(
                                        course = course,
                                        onApplyClick = {
                                            selectedCourseForApplication = course
                                            showApplicationDialog = true
                                            applicationNotes = ""
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Applications Section
                    val pendingApplications = state.applications.filter { it.status == "pending" }
                    if (pendingApplications.isNotEmpty()) {
                        CollapsibleSection(
                            title = "PENDING APPLICATIONS (${pendingApplications.size})",
                            isExpanded = expandedPending,
                            onToggle = { expandedPending = !expandedPending }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                pendingApplications.forEach { app ->
                                    ApplicationCardCompact(app)
                                }
                            }
                        }
                    }

                    if (state.approvedCourses.isEmpty() && state.availableCourses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No courses available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MediumGray
                            )
                        }
                    }
                }
                CoursesState.Idle -> {}
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Apply Dialog
    if (showApplicationDialog && selectedCourseForApplication != null) {
        ApplyForCourseDialog(
            course = selectedCourseForApplication!!,
            notes = applicationNotes,
            onNotesChange = { applicationNotes = it },
            isApplying = isApplying,
            onApply = {
                if (context != null && selectedCourseForApplication != null) {
                    isApplying = true
                }
            },
            onDismiss = {
                showApplicationDialog = false
                selectedCourseForApplication = null
                applicationNotes = ""
                isApplying = false
            }
        )
    }

    // Handle apply submission
    LaunchedEffect(isApplying) {
        if (isApplying && selectedCourseForApplication != null && context != null) {
            try {
                val subjectsApiService = ServiceLocator.subjectsApiService(context)
                val request = com.calico.tutor.data.datasource.remote.ApplyCourseRequest(
                    tutorId = tutorId,
                    courseId = selectedCourseForApplication!!.id,
                    notes = if (applicationNotes.isBlank()) null else applicationNotes
                )
                subjectsApiService.applyForCourse(request)
                Log.d("CoursesScreen", "Applied for course: ${selectedCourseForApplication!!.name}")
                
                // Reload data
                coursesState = CoursesState.Loading
                val approvedCourses = subjectsApiService.getTutorCourses(tutorId)
                val applications = subjectsApiService.getTutorApplications(tutorId)
                val allCoursesResponse = subjectsApiService.getAllAvailableCourses()
                
                val allAvailableCourses = allCoursesResponse.courses
                
                val markedCourses = allAvailableCourses.map { course ->
                    val hasApplication = applications.any { it.courseId == course.id }
                    course.copy(hasApplied = hasApplication)
                }
                
                val availableCourses = markedCourses.filter { available ->
                    approvedCourses.none { approved -> approved.id == available.id }
                }
                
                coursesState = CoursesState.Success(approvedCourses, availableCourses, applications)
                
                // Close dialog
                showApplicationDialog = false
                selectedCourseForApplication = null
                applicationNotes = ""
            } catch (e: Exception) {
                Log.e("CoursesScreen", "Error applying for course: ${e.message}", e)
                showApplicationDialog = false
            } finally {
                isApplying = false
            }
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 14.sp
            )
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = PrimaryOrange,
                modifier = Modifier.size(24.dp)
            )
        }
        
        if (isExpanded) {
            content()
        }
    }
}

@Composable
private fun ApprovedCourseCardCompact(course: TutorCourseData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${course.code} • ${course.credits} credits",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGray,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓ Approved",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun AvailableCourseCardCompact(
    course: AvailableCourseResponse,
    onApplyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${course.code} • ${course.credits} credits",
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGray,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (course.faculty != null) {
                    Text(
                        text = course.faculty,
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGray,
                        fontSize = 11.sp
                    )
                }
                
                Button(
                    onClick = onApplyClick,
                    modifier = Modifier.height(32.dp),
                    enabled = !course.hasApplied,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (course.hasApplied) MediumGray else PrimaryOrange
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(
                        text = if (course.hasApplied) "Applied" else "Apply",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationCardCompact(app: CourseApplicationResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.courseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                    Text(
                        text = app.courseCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGray,
                        fontSize = 12.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(
                            color = when (app.status) {
                                "pending" -> Color(0xFFFFF3E0)
                                "approved" -> Color(0xFFE8F5E9)
                                "rejected" -> Color(0xFFFFEBEE)
                                else -> Color(0xFFF5F5F5)
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (app.status) {
                            "pending" -> Color(0xFFF57C00)
                            "approved" -> Color(0xFF2E7D32)
                            "rejected" -> Color(0xFFC62828)
                            else -> Color.Gray
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
            
            if (app.rejectionReason != null && app.status == "rejected") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reason: ${app.rejectionReason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFC62828),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ApplyForCourseDialog(
    course: AvailableCourseResponse,
    notes: String,
    onNotesChange: (String) -> Unit,
    isApplying: Boolean,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Apply for Course")
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Code: ${course.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                enabled = !isApplying,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Apply")
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MediumGray)
            ) {
                Text("Cancel")
            }
        }
    )
}
