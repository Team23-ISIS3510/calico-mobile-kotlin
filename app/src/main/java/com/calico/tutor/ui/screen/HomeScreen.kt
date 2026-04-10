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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calico.tutor.R
import com.calico.tutor.ui.theme.*
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.domain.model.Session
import com.calico.tutor.data.dto.response.TutorOccupancyData
import com.calico.tutor.ui.viewmodel.HomeScreenViewModel
import com.calico.tutor.ui.viewmodel.HomeScreenViewModelFactory
import com.calico.tutor.ui.viewmodel.SessionsState
import com.calico.tutor.ui.viewmodel.OccupancyState
import android.content.Context
import android.content.pm.ApplicationInfo
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

@Composable
fun HomeScreen(
    userName: String = "User",
    tutorId: String = "tutor@example.com",
    context: Context? = null,
    onLogout: () -> Unit = {},
    onNavigateToTopSubjects: () -> Unit = {}
) {
    // Obtener ViewModel
    val viewModel: HomeScreenViewModel = viewModel(
        factory = context?.let { HomeScreenViewModelFactory(it) }
    )

    // Observar estados del ViewModel
    val sessionsStateValue = viewModel.sessionsState.collectAsState().value
    val occupancyStateValue = viewModel.occupancyState.collectAsState().value
    val tutorNameValue = viewModel.tutorName.collectAsState().value

    // Cargar datos al inicializar
    LaunchedEffect(Unit) {
        viewModel.loadSessions(tutorId)
        viewModel.loadOccupancy(tutorId)
    }

    // Extraer datos del estado de sesiones
    val (previousSessions, upcomingSessions, sessionsError) = when (sessionsStateValue) {
        is SessionsState.Success -> Triple(
            sessionsStateValue.previousSessions,
            sessionsStateValue.upcomingSessions,
            null
        )
        is SessionsState.Error -> Triple(emptyList(), emptyList(), sessionsStateValue.message)
        else -> Triple(emptyList(), emptyList(), null)
    }

    val isLoadingSessions = sessionsStateValue is SessionsState.Loading

    // Extraer datos del estado de ocupancy
    val (occupancyData, occupancyError) = when (occupancyStateValue) {
        is OccupancyState.Success -> Pair(occupancyStateValue.data, null)
        is OccupancyState.Error -> Pair(emptyList(), occupancyStateValue.message)
        else -> Pair(emptyList(), null)
    }

    val isLoadingOccupancy = occupancyStateValue is OccupancyState.Loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBase)
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
                text = "WELCOME TO CALICO,\n$tutorNameValue",
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

            if (isLoadingSessions) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            } else if (sessionsError != null && upcomingSessions.isEmpty() && previousSessions.isEmpty()) {
                Text(
                    text = sessionsError,
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
                    SessionCard(session = session, context = context)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Previous Sessions Section
            Text(
                text = "Previous Sessions",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (previousSessions.isEmpty() && !isLoadingSessions) {
                Text(
                    text = "You have not had any tutoring sessions yet.",
                    color = MediumGray,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                previousSessions.forEach { session ->
                    SessionCard(session = session, context = context)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Subject Occupancy Analytics Section
            Text(
                text = "Subject Occupancy Analytics",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sessions per hour and occupancy rate by subject",
                style = MaterialTheme.typography.labelSmall,
                color = MediumGray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Occupancy Analytics Content
            if (isLoadingOccupancy) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            } else if (occupancyError != null) {
                Text(
                    text = occupancyError ?: "Error loading occupancy data",
                    color = Color.Red,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (occupancyData.isEmpty()) {
                Text(
                    text = "No occupancy data available",
                    color = MediumGray,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                occupancyData.forEach { occupancy ->
                    OccupancyCard(occupancy = occupancy)
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

            // Logout Button
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceVariant,
                    contentColor = TextColorBlack
                )
            ) {
                Text(
                    text = "Logout",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OccupancyCard(occupancy: TutorOccupancyData) {
    // Determine occupancy level indicator emoji
    val occupancyIndicator = when {
        occupancy.occupancyRate >= 75 -> "🔴"  // Overloaded (red)
        occupancy.occupancyRate >= 50 -> "🟡"  // Medium (yellow)
        else -> "🟢"                            // Available (green)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WhiteBase
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = occupancy.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Occupancy: ${String.format("%.2f", occupancy.occupancyRate)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGray
                    )
                }
                Text(
                    text = occupancyIndicator,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Occupancy progress bar
            LinearProgressIndicator(
                progress = { (occupancy.occupancyRate / 100.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = when {
                    occupancy.occupancyRate >= 75 -> StatusHighRed  // Red
                    occupancy.occupancyRate >= 50 -> StatusMediumYellow  // Yellow
                    else -> StatusLowGreen                             // Green
                },
                trackColor = LightGray,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sessions/Hour: ${occupancy.sessionsPerHour}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGray
                )
                Text(
                    text = "Total: ${occupancy.totalSessions}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGray
                )
            }
        }
    }
}

@Composable
private fun SessionCard(session: Session, context: Context? = null) {
    var courseName by remember { mutableStateOf<String?>(null) }
    var isLoadingCourseName by remember { mutableStateOf(true) }

    LaunchedEffect(session.courseId) {
        if (context != null && !session.courseId.isNullOrEmpty()) {
            try {
                val subjectsApiService = ServiceLocator.subjectsApiService(context)
                val courseResponse = subjectsApiService.getCourseById(session.courseId ?: "")
                courseName = courseResponse.course?.name ?: "Unknown Course"
                isLoadingCourseName = false
            } catch (e: Exception) {
                courseName = session.course ?: "Unknown Course"
                isLoadingCourseName = false
            }
        } else {
            courseName = session.course ?: "Unknown Course"
            isLoadingCourseName = false
        }
    }

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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WhiteBase
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
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isLoadingCourseName) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = PrimaryOrange,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = courseName ?: "Unknown Course",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
