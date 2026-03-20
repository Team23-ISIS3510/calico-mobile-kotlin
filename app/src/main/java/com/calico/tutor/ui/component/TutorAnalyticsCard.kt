package com.calico.tutor.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calico.tutor.domain.model.OccupancyLevel
import com.calico.tutor.domain.model.TutorSubjectAnalytics
import com.calico.tutor.ui.theme.PrimaryOrange
import kotlin.math.roundToInt

/**
 * Componente que muestra una tarjeta con la analítica de un tutor y materia
 */
@Composable
fun TutorAnalyticsCard(
    analytics: TutorSubjectAnalytics,
    modifier: Modifier = Modifier
) {
    val occupancyColor = when (analytics.occupancyLevel) {
        OccupancyLevel.OVERLOADED -> Color(0xFFD32F2F)  // 🔴 Rojo
        OccupancyLevel.MODERATE -> Color(0xFFFFA726)    // 🟡 Naranja
        OccupancyLevel.AVAILABLE -> Color(0xFF388E3C)   // 🟢 Verde
    }

    val occupancyEmoji = when (analytics.occupancyLevel) {
        OccupancyLevel.OVERLOADED -> "🔴"
        OccupancyLevel.MODERATE -> "🟡"
        OccupancyLevel.AVAILABLE -> "🟢"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Materia y Tutor
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = analytics.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Código: ${analytics.subjectCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tutor: ${analytics.tutorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryOrange,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
                
                // Indicador visual
                Box(
                    modifier = Modifier
                        .background(
                            color = occupancyColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = occupancyEmoji,
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Métricas principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Sesiones por Hora",
                    value = String.format("%.2f", analytics.overallSessionsPerHour),
                    unit = "sesiones/h",
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                MetricItem(
                    label = "Ocupación",
                    value = "${analytics.overallOccupancyRate.roundToInt()}%",
                    unit = "",
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                MetricItem(
                    label = "Total Sesiones",
                    value = "${analytics.totalSessions}",
                    unit = "sesiones",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Desglose por demanda
            Text(
                text = "Desglose por Período de Demanda",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DemandPeriodItem(
                    title = "Alta Demanda",
                    sessionsPerHour = analytics.highDemandMetrics.sessionsPerHour,
                    occupancyRate = analytics.highDemandMetrics.occupancyRate,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                DemandPeriodItem(
                    title = "Demanda Normal",
                    sessionsPerHour = analytics.normalDemandMetrics.sessionsPerHour,
                    occupancyRate = analytics.normalDemandMetrics.occupancyRate,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Estado y ocupación
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = occupancyColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = when (analytics.occupancyLevel) {
                        OccupancyLevel.OVERLOADED -> "⚠️ Tutor Sobrecargado: El volumen de sesiones exceede la disponibilidad"
                        OccupancyLevel.MODERATE -> "⚡ Ocupación Moderada: El tutor tiene capacidad limitada"
                        OccupancyLevel.AVAILABLE -> "✅ Disponible: El tutor puede recibir más sesiones"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Componente para mostrar una métrica individual
 */
@Composable
private fun MetricItem(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryOrange,
            fontSize = 14.sp
        )
        if (unit.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 9.sp
            )
        }
    }
}

/**
 * Componente para mostrar métricas de período de demanda
 */
@Composable
private fun DemandPeriodItem(
    title: String,
    sessionsPerHour: Double,
    occupancyRate: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = "Sesiones/h: ${String.format("%.2f", sessionsPerHour)}",
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryOrange,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = "Ocupación: ${occupancyRate.roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}
