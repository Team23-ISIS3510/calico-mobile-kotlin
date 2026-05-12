package com.calico.tutor.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calico.tutor.domain.model.Subject
import com.calico.tutor.ui.component.OfflineBanner
import com.calico.tutor.ui.theme.MediumGray
import com.calico.tutor.ui.theme.OnSurface
import com.calico.tutor.ui.theme.PrimaryOrange
import com.calico.tutor.ui.theme.SurfaceVariant
import com.calico.tutor.ui.theme.WhiteBase
import com.calico.tutor.ui.viewmodel.RecommendedSubjectsViewModel
import com.calico.tutor.ui.viewmodel.RecommendedSubjectsViewModelFactory
import com.calico.tutor.ui.viewmodel.SubjectsState

private const val GOOGLE_FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLScYXCyv0jDJ-lzzhrb2q2qha8qgG4YnKB0tky-cIc0Oz-w9rQ/viewform?usp=header"

data class SubjectFrequency(
    val subject: Subject,
    val frequency: Int
)

@Composable
fun TopSubjectsScreen(
    context: Context,
    tutorId: String,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: RecommendedSubjectsViewModel = viewModel(
        factory = RecommendedSubjectsViewModelFactory(context)
    )
    val subjectsState by viewModel.subjectsState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    LaunchedEffect(tutorId) {
        viewModel.loadData(tutorId)
        viewModel.startConnectivityMonitoring(tutorId)
    }

    val topSubjects: List<SubjectFrequency> = remember(subjectsState) {
        when (val state = subjectsState) {
            is SubjectsState.Success -> {
                val courseMap = linkedMapOf<String, Pair<String, Int>>()
                state.subjects.forEach { courseData ->
                    val courseId = courseData.courseId
                    val courseName = courseData.course ?: "Unnamed"
                    val current = courseMap[courseId]
                    courseMap[courseId] = if (current == null) {
                        courseName to 1
                    } else {
                        current.first to (current.second + 1)
                    }
                }

                courseMap.entries.map { (courseId, nameAndCount) ->
                    SubjectFrequency(
                        subject = Subject(
                            id = courseId,
                            name = nameAndCount.first,
                            code = courseId.take(4).uppercase(),
                            count = nameAndCount.second
                        ),
                        frequency = nameAndCount.second
                    )
                }.sortedByDescending { it.frequency }.take(3)
            }
            else -> emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                            tint = OnSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recommended Subjects",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                }
            }

            // Show offline banner when not connected
            if (!isOnline) {
                OfflineBanner()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                when (val state = subjectsState) {
                    SubjectsState.Loading, SubjectsState.Idle -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryOrange)
                        }
                    }
                    is SubjectsState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Text(
                                text = state.message,
                                modifier = Modifier.padding(16.dp),
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    is SubjectsState.Success -> {
                        if (topSubjects.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                            ) {
                                Text(
                                    text = "No recommended subjects available.",
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

                            topSubjects.forEach { subject ->
                                RecommendedSubjectItem(subject = subject.subject)
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (isOnline) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_FORM_URL)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                enabled = isOnline,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOnline) PrimaryOrange else Color(0xFFCCCCCC),
                                    contentColor = if (isOnline) WhiteBase else Color(0xFF999999),
                                    disabledContainerColor = Color(0xFFCCCCCC),
                                    disabledContentColor = Color(0xFF999999)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!isOnline) {
                                        Icon(
                                            imageVector = Icons.Default.CloudOff,
                                            contentDescription = "Offline",
                                            tint = Color(0xFF999999),
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = if (isOnline) "Apply" else "Apply (Offline)",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun RecommendedSubjectItem(subject: Subject) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
        }
    }
}
