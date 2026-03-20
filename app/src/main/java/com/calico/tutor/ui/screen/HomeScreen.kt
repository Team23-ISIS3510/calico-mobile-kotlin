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
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    userName: String = "Usuario",
    tutorId: String = "tutor@example.com",
    context: Context? = null,
    onLogout: () -> Unit = {},
    onNavigateToTopSubjects: () -> Unit = {}
) {
    var previousSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var upcomingSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var loadingPrevious by remember { mutableStateOf(true) }
    var loadingUpcoming by remember { mutableStateOf(true) }
    var errorPrevious by remember { mutableStateOf<String?>(null) }
    var errorUpcoming by remember { mutableStateOf<String?>(null) }

    // Cargar datos cuando se monta el composable
    LaunchedEffect(Unit) {
        if (context != null) {
            try {
                val subjectsApiService = ServiceLocator.subjectsApiService(context)
                
                // Cargar sesiones previas del tutor
                try {
                    val prevResponse = subjectsApiService.getTutorSessionHistory(tutorId)
                    previousSessions = prevResponse.sessions.take(2)
                    loadingPrevious = false
                } catch (e: Exception) {
                    errorPrevious = "Error al cargar sesiones previas"
                    loadingPrevious = false
                }

                // Cargar sesiones próximas (rango de fechas: hoy a 30 días)
                try {
                    val today = Calendar.getInstance()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val startDate = dateFormat.format(today.time)
                    
                    val endDate = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_MONTH, 30)
                    }
                    val endDateStr = dateFormat.format(endDate.time)
                    
                    val upcomingResponse = subjectsApiService.getSessionsByDateRange(startDate, endDateStr)
                    upcomingSessions = upcomingResponse.sessions.take(2)
                    loadingUpcoming = false
                } catch (e: Exception) {
                    errorUpcoming = "Error al cargar sesiones próximas"
                    loadingUpcoming = false
                }
            } catch (e: Exception) {
                errorPrevious = "Error de conexión"
                errorUpcoming = "Error de conexión"
                loadingPrevious = false
                loadingUpcoming = false
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

            // Logo Calico - Centrado
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
                text = "BIENVENIDO A CALICO,\n$userName",
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

            if (loadingUpcoming) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            } else if (errorUpcoming != null) {
                Text(
                    text = errorUpcoming ?: "Error",
                    color = Color.Red,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (upcomingSessions.isEmpty()) {
                Text(
                    text = "No hay sesiones próximas",
                    color = MediumGray,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                upcomingSessions.forEach { session ->
                    SessionCard(
                        date = "${session.date} - ${session.time}",
                        tutorName = session.tutorName,
                        subject = "${session.subjectName} - ${session.subjectCode}"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Previous Sessions Section
            Text(
                text = "Previous sessions",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (loadingPrevious) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            } else if (errorPrevious != null) {
                Text(
                    text = errorPrevious ?: "Error",
                    color = Color.Red,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (previousSessions.isEmpty()) {
                Text(
                    text = "No hay sesiones previas",
                    color = MediumGray,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                previousSessions.forEach { session ->
                    SessionCard(
                        date = "${session.date} - ${session.time}",
                        tutorName = session.tutorName,
                        subject = "${session.subjectName} - ${session.subjectCode}"
                    )
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
                    text = "Materias recomendadas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Button
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE0E0E0),
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Cerrar sesión",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SessionCard(
    date: String,
    tutorName: String,
    subject: String
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tutorName,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryOrange,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subject,
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
