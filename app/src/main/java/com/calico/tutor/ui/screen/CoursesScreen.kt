package com.calico.tutor.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calico.tutor.ui.theme.*
import com.calico.tutor.ui.viewmodel.HomeScreenViewModel
import com.calico.tutor.ui.viewmodel.HomeScreenViewModelFactory
import com.calico.tutor.ui.viewmodel.OccupancyState
import android.content.Context

@Composable
fun CoursesScreen(
    tutorId: String = "tutor@example.com",
    context: Context? = null
) {
    val viewModel: HomeScreenViewModel = viewModel(
        factory = context?.let { HomeScreenViewModelFactory(it) }
    )

    // Observar estado de ocupancy (datos de cursos)
    val occupancyStateValue = viewModel.occupancyState.collectAsState().value

    // Cargar datos al inicializar
    LaunchedEffect(Unit) {
        viewModel.loadOccupancy(tutorId)
    }

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

            // Título
            Text(
                text = "MY COURSES",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Loading state
            if (isLoadingOccupancy) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            } else if (occupancyError != null) {
                // Error state
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = occupancyError,
                        color = Color.Red,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (occupancyData.isEmpty()) {
                // Empty state
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
            } else {
                // Courses list
                occupancyData.forEach { course ->
                    CourseCard(
                        courseName = course.subject,
                        sessionsPerHour = course.sessionsPerHour,
                        occupancyRate = course.occupancyRate
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun CourseCard(
    courseName: String,
    sessionsPerHour: Double,
    occupancyRate: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Course name
            Text(
                text = courseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sessions Per Hour metric
            MetricRow(
                label = "Sessions/Hour",
                value = String.format("%.2f", sessionsPerHour),
                color = PrimaryOrange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Occupancy Rate metric
            MetricRow(
                label = "Occupancy Rate",
                value = String.format("%.1f%%", occupancyRate),
                color = AccentMagenta
            )
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MediumGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
