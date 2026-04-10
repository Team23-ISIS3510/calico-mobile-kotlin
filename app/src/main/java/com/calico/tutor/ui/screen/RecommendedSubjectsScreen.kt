package com.calico.tutor.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.domain.model.Subject
import com.calico.tutor.ui.theme.*
import android.content.Context

@Composable
fun RecommendedSubjectsScreen(
    context: Context,
    onNavigateBack: () -> Unit = {},
    onApply: (List<Subject>) -> Unit = {}
) {
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedSubjects by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        try {
            val subjectsApiService = ServiceLocator.subjectsApiService(context)
            val response = subjectsApiService.getSubjectsHistory()
            
            if (response.data != null) {
                val courseMap = mutableMapOf<String, Pair<String, Int>>()
                
                response.data.forEach { courseData ->
                    val courseId = courseData.courseId
                    val courseName = courseData.course ?: "Unnamed"
                    
                    if (courseMap.containsKey(courseId)) {
                        val (name, count) = courseMap[courseId]!!
                        courseMap[courseId] = Pair(name, count + 1)
                    } else {
                        courseMap[courseId] = Pair(courseName, 1)
                    }
                }
                
                subjects = courseMap.entries.map { (courseId, nameAndCount) ->
                    Subject(
                        id = courseId,
                        name = nameAndCount.first,
                        code = courseId.take(4).uppercase(),
                        count = nameAndCount.second
                    )
                }.sortedByDescending { it.count }.take(3)
            }
            
            isLoading = false
        } catch (e: Exception) {
            error = "Error loading subjects: ${e.message}"
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp),
                color = WhiteBase
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = OnSurface
                            )
                        }
                        Text(
                            text = "Recommended Subjects",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryOrange
                        )
                    }
                } else if (error != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ErrorCardBackground
                        )
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            modifier = Modifier.padding(16.dp),
                            color = ErrorCardText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (subjects.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceVariant
                        )
                    ) {
                        Text(
                            text = "No recommended subjects available",
                            modifier = Modifier.padding(16.dp),
                            color = MediumGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = "Top 3 Recommended Subjects",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    subjects.forEach { subject ->
                        RecommendedSubjectItem(
                            subject = subject,
                            isSelected = subject.id in selectedSubjects,
                            onSelectionChange = { isSelected ->
                                selectedSubjects = if (isSelected) {
                                    selectedSubjects + subject.id
                                } else {
                                    selectedSubjects - subject.id
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Apply Button

                        onClick = {
                            val selectedItems = subjects.filter { it.id in selectedSubjects }
                            onApply(selectedItems)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryOrange,
                            contentColor = WhiteBase
                        ),
                        enabled = selectedSubjects.isNotEmpty()
                    ) {
                        Text(
                            text = "Apply Now",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RecommendedSubjectItem(
    subject: Subject,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CalicoBulletColor else Surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Code: ${subject.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${subject.count} students interested",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryOrange,
                    uncheckedColor = MediumGray
                )
            )
        }
    }
}
