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

data class SubjectFrequency(
    val subject: Subject,
    val frequency: Int
)

@Composable
fun TopSubjectsScreen(
    context: Context,
    onNavigateBack: () -> Unit = {}
) {
    var topSubjects by remember { mutableStateOf<List<SubjectFrequency>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val subjectsApiService = ServiceLocator.subjectsApiService(context)
            val response = subjectsApiService.getSubjectsHistory()
            
            // Agrupar por nombre de materia y contar frecuencias
            val frequencyMap = mutableMapOf<String, Pair<Subject, Int>>()
            
            response.subjects.forEach { subject ->
                val key = subject.name
                if (frequencyMap.containsKey(key)) {
                    val (existingSubject, count) = frequencyMap[key]!!
                    frequencyMap[key] = Pair(existingSubject, count + 1)
                } else {
                    frequencyMap[key] = Pair(subject, 1)
                }
            }
            
            // Convertir a lista de SubjectFrequency y ordenar descendentemente
            topSubjects = frequencyMap.values
                .map { (subject, frequency) -> SubjectFrequency(subject, frequency) }
                .sortedByDescending { it.frequency }
                .take(3)
            
            isLoading = false
        } catch (e: Exception) {
            error = "Error al cargar las materias: ${e.message}"
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header con flecha atrás
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp),
                color = Color.White
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
                            contentDescription = "Volver",
                            tint = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Top 3 Materias",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            // Content
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
                            containerColor = Color(0xFFFFEBEE)
                        )
                    ) {
                        Text(
                            text = error ?: "Error desconocido",
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFFC62828),
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
                            containerColor = Color(0xFFF5F5F5)
                        )
                    ) {
                        Text(
                            text = "No hay materias disponibles",
                            modifier = Modifier.padding(16.dp),
                            color = MediumGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    topSubjects.forEachIndexed { index, subjectFreq ->
                        TopSubjectCard(
                            ranking = index + 1,
                            subject = subjectFreq.subject.name,
                            frequency = subjectFreq.frequency
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Apply Button
                    Button(
                        onClick = {},
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
                            text = "Aplica ya",
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
            // Número de ranking
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

            // Información de la materia
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$frequency diccionarios",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
