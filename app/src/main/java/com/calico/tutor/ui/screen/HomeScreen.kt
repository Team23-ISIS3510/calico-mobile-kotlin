package com.calico.tutor.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calico.tutor.R
import com.calico.tutor.ui.theme.*
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.domain.model.Session
import com.calico.tutor.domain.model.TutorSubjectAnalytics
import com.calico.tutor.ui.component.TutorAnalyticsCard
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    userName: String = "User",
    tutorId: String = "tutor@example.com",
    context: Context? = null,
    onLogout: () -> Unit = {},
    onNavigateToTopSubjects: () -> Unit = {}
) {
    var previousSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var upcomingSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tutorName by remember { mutableStateOf(userName) }

    LaunchedEffect(Unit) {
        if (context != null) {
            try {
                val subjectsApiService = ServiceLocator.subjectsApiService(context)
                
                // Load tutor profile to get the actual name
                try {
                    val tutorResponse = subjectsApiService.getTutorProfile(tutorId)
                    tutorName = tutorResponse.name
                } catch (e: Exception) {
                    tutorName = userName
                }
                
                // Load tutoring sessions for the tutor
                try {
                    val response = subjectsApiService.getTutoringSessionsForTutor(tutorId)
                    val now = System.currentTimeMillis()
                    
                    // Separate sessions into previous and upcoming based on scheduledStart
                    val sessionList = response.sessions.map { sessionData ->
                        val startTime = try {
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(sessionData.scheduledStart)?.time ?: 0
                        } catch (e: Exception) {
                            try {
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).parse(sessionData.scheduledStart)?.time ?: 0
                            } catch (e: Exception) {
                                0
                            }
                        }
                        
                        Session(
                            id = sessionData.id,
                            scheduledStart = sessionData.scheduledStart,
                            scheduledEnd = sessionData.scheduledEnd,
                            status = sessionData.status,
                            course = sessionData.course,
                            courseId = sessionData.courseId,
                            date = sessionData.scheduledStart,
                            time = sessionData.scheduledStart,
                            tutorName = "",
                            subjectName = sessionData.course ?: sessionData.courseId ?: "Unknown",
                            subjectCode = ""
                        ) to startTime
                    }
                    
                    // Filter: Previous (scheduledStart < now), sorted descending, take last 2
                    previousSessions = sessionList
                        .filter { it.second < now }
                        .sortedByDescending { it.second }
                        .take(2)
                        .map { it.first }
                    
                    // Filter: Upcoming (scheduledStart > now), sorted ascending, take next 2
                    upcomingSessions = sessionList
                        .filter { it.second > now }
                        .sortedBy { it.second }
                        .take(2)
                        .map { it.first }
                    
                    isLoading = false
                } catch (e: Exception) {
                    error = "Error loading sessions: ${e.message}"
                    isLoading = false
                }
            } catch (e: Exception) {
                error = "Connection error: ${e.message}"
                isLoading = false
            }

            // Cargar analítica
            try {
                loadingAnalytics = true
                val analyticsRepository = ServiceLocator.analyticsRepository(context)
                val response = analyticsRepository.getTutoringDemandAnalytics()
                analyticsData = response.analytics
                loadingAnalytics = false
            } catch (e: Exception) {
                errorAnalytics = e.message ?: "Failed to load analytics"
                loadingAnalytics = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo Calico - Centered
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.calico_logo),
                    contentDescription = "Calico Logo",
                    modifier = Modifier.size(120.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Welcome Message
            Text(
                text = "WELCOME TO CALICO,\n$tutorName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Upcoming Sessions Section
            Text(
                text = "Upcoming Sessions",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            } else if (error != null && upcomingSessions.isEmpty() && previousSessions.isEmpty()) {
                Text(
                    text = error ?: "Error",
                    color = Color.Red,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (upcomingSessions.isEmpty()) {
                Text(
                    text = "You do not have any upcoming tutoring sessions.",
                    color = MediumGray,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                upcomingSessions.forEach { session ->
                    SessionCard(session = session)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Previous Sessions Section
            Text(
                text = "Previous Sessions",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (previousSessions.isEmpty() && !isLoading) {
                Text(
                    text = "You have not had any tutoring sessions yet.",
                    color = MediumGray,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                previousSessions.forEach { session ->
                    SessionCard(session = session)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recommended Subjects Button
            Button(
                onClick = onNavigateToTopSubjects,
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
                    text = "Recommended Subjects",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Analytics Section
            Text(
                text = "Volumen de Sesiones por Materia",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Comparación de demanda vs disponibilidad (últimos 2 años)",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Analytics Content
            if (loadingAnalytics) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            } else if (errorAnalytics != null) {
                Text(
                    text = "Logout",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (analyticsData.isEmpty()) {
                Text(
                    text = "No hay datos de analítica disponibles",
                    color = MediumGray,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                analyticsData.forEach { tutorAnalytics ->
                    TutorAnalyticsCard(analytics = tutorAnalytics)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

        }
    }
}

@Composable
private fun SessionCard(session: Session) {
    // Format the date and time from scheduledStart
    val dateTimeFormatter = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    } catch (e: Exception) {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    }
    
    val displayFormatter = SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault())
    
    val formattedDateTime = try {
        val date = dateTimeFormatter.parse(session.scheduledStart)
        if (date != null) {
            displayFormatter.format(date)
        } else {
            "Date TBD"
        }
    } catch (e: Exception) {
        "Date TBD"
    }
    
    val courseName = session.course ?: session.courseId ?: "Unknown Course"
    
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedDateTime,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = courseName,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryOrange,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Status: ${session.status}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGray
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "View",
                tint = PrimaryOrange,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
