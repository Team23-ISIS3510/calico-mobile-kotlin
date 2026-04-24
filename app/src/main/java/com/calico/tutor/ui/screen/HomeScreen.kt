package com.calico.tutor.ui.screen

import android.content.Context
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
import com.calico.tutor.data.dto.response.CourseData
import com.calico.tutor.data.dto.response.TutorOccupancyData
import com.calico.tutor.domain.model.Session
import com.calico.tutor.ui.theme.*
import com.calico.tutor.ui.viewmodel.HomeScreenViewModelFactory
import com.calico.tutor.ui.viewmodel.HomeScreenViewModel
import com.calico.tutor.ui.viewmodel.OccupancyState
import com.calico.tutor.ui.viewmodel.SessionsState
import com.calico.tutor.ui.viewmodel.SubjectsState
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
    if (context == null) return

    val vm: HomeScreenViewModel = viewModel(
        factory = HomeScreenViewModelFactory(context)
    )

    val sessionsState  by vm.sessionsState.collectAsState()
    val occupancyState by vm.occupancyState.collectAsState()
    val subjectsState  by vm.subjectsState.collectAsState()
    val tutorName      by vm.tutorName.collectAsState()

    LaunchedEffect(tutorId) {
        vm.loadAllData(tutorId)
        vm.startSessionAlertPolling()
    }

    val displayName = tutorName.ifBlank { userName }

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

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.calico_logo),
                    contentDescription = "Calico Logo",
                    modifier = Modifier.size(120.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "WELCOME TO CALICO,\n$displayName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Vista 1: Upcoming Sessions ────────────────────────────────────
            Text(
                text = "Upcoming Sessions",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = sessionsState) {
                is SessionsState.Loading -> LoadingBox()
                is SessionsState.Error   -> ErrorText(state.message)
                is SessionsState.Success -> {
                    if (state.upcomingSessions.isEmpty()) {
                        Text(
                            text = "You do not have any upcoming tutoring sessions.",
                            color = MediumGray,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        state.upcomingSessions.take(2).forEach { session ->
                            SessionCard(session = session)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Vista 2: Previous Sessions ────────────────────────────────────
            Text(
                text = "Previous Sessions",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = sessionsState) {
                is SessionsState.Loading -> LoadingBox()
                is SessionsState.Error   -> ErrorText(state.message)
                is SessionsState.Success -> {
                    if (state.previousSessions.isEmpty()) {
                        Text(
                            text = "You have not had any tutoring sessions yet.",
                            color = MediumGray,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        state.previousSessions.take(2).forEach { session ->
                            SessionCard(session = session)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Vista 4: Recommended Subjects ─────────────────────────────────
            Text(
                text = "Recommended Subjects",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = subjectsState) {
                is SubjectsState.Loading -> LoadingBox(height = 60.dp)
                is SubjectsState.Error   -> ErrorText(state.message)
                is SubjectsState.Success -> {
                    if (state.subjects.isEmpty()) {
                        Text(
                            text = "No recommended subjects available.",
                            color = MediumGray,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        state.subjects.take(5).forEach { subj ->
                            SubjectChip(name = subj.course ?: subj.courseId)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onNavigateToTopSubjects,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOrange,
                    contentColor   = Color.White
                )
            ) {
                Text("See all recommended subjects", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE0E0E0),
                    contentColor   = Color.Black
                )
            ) {
                Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Vista 3: Subject Occupancy Analytics ──────────────────────────
            Text(
                text = "Subject Occupancy Analytics",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sessions per hour and occupancy rate by subject",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = occupancyState) {
                is OccupancyState.Loading -> LoadingBox()
                is OccupancyState.Error   -> ErrorText(state.message)
                is OccupancyState.Success -> {
                    if (state.data.isEmpty()) {
                        Text(
                            text = "No occupancy data available",
                            color = MediumGray,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        state.data.forEach { occupancy ->
                            OccupancyCard(occupancy = occupancy)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Componentes privados ──────────────────────────────────────────────────────

@Composable
private fun LoadingBox(height: androidx.compose.ui.unit.Dp = 100.dp) {
    Box(
        modifier = Modifier.fillMaxWidth().height(height),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PrimaryOrange)
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        color = Color.Red,
        fontSize = 13.sp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun SubjectChip(name: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFF3E0),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "•  $name",
                fontSize = 13.sp,
                color = Color(0xFFBF360C),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OccupancyCard(occupancy: TutorOccupancyData) {
    val indicator = when {
        occupancy.occupancyRate >= 75 -> "🔴"
        occupancy.occupancyRate >= 50 -> "🟡"
        else -> "🟢"
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(elevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = occupancy.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(text = indicator, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Sessions/Hour", style = MaterialTheme.typography.labelSmall, color = MediumGray, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("%.2f".format(occupancy.sessionsPerHour), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.Black)
                }
                Column {
                    Text("Occupancy Rate", style = MaterialTheme.typography.labelSmall, color = MediumGray, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("%.1f%%".format(occupancy.occupancyRate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PrimaryOrange)
                }
                Column {
                    Text("Total Sessions", style = MaterialTheme.typography.labelSmall, color = MediumGray, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(occupancy.totalSessions.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (occupancy.occupancyRate / 100.0).toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = when {
                    occupancy.occupancyRate >= 75 -> Color(0xFFE53935)
                    occupancy.occupancyRate >= 50 -> Color(0xFFFDD835)
                    else -> Color(0xFF43A047)
                },
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
private fun SessionCard(session: Session) {
    val fmt1 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    val fmt2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    val disp = SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault())

    val formattedDateTime = try {
        (try { fmt1.parse(session.scheduledStart) } catch (e: Exception) { fmt2.parse(session.scheduledStart) })
            ?.let { disp.format(it) } ?: "Date TBD"
    } catch (e: Exception) { "Date TBD" }

    val courseName = session.course ?: session.courseId ?: "Unknown Course"

    Card(
        modifier = Modifier.fillMaxWidth().shadow(elevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formattedDateTime, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.height(8.dp))
                Text(courseName, style = MaterialTheme.typography.labelSmall, color = PrimaryOrange, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Status: ${session.status}", style = MaterialTheme.typography.labelSmall, color = MediumGray)
            }
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "View", tint = PrimaryOrange, modifier = Modifier.size(24.dp))
        }
    }
}
