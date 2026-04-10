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
fun TopSubjectsScreen(
    context: Context,
    onNavigateBack: () -> Unit = {}
) {
    var topSubjects by remember { mutableStateOf<List<Pair<Subject, Int>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

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
                
                topSubjects = courseMap.entries.map { (courseId, nameAndCount) ->
                    val subject = Subject(
                        id = courseId,
                        name = nameAndCount.first,
                        code = "",
                        count = nameAndCount.second
                    )
                    Pair(subject, nameAndCount.second)
                }.sortedByDescending { it.second }
                    .take(3)
            }
            
            isLoading = false
        } catch (e: Exception) {
            error = "Error loading subjects"
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBase)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextColorBlack
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Top 3 Subjects",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
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
                } else if (topSubjects.isEmpty()) {
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
                            text = "No subjects available",
                            modifier = Modifier.padding(16.dp),
                            color = MediumGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    topSubjects.forEachIndexed { index, (subject, frequency) ->
                        TopSubjectCard(
                            ranking = index + 1,
                            subject = subject.name,
                            frequency = frequency
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Apply Button
                    Button(
                        onClick = { onNavigateBack() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun TopSubjectCard(
    ranking: Int,
    subject: String,
    frequency: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ranking number
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = PrimaryOrange,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$ranking",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Subject information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$frequency sessions",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
