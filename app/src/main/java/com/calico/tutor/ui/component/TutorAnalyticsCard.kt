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
 * Component that shows a tutor and subject analytics card.
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
            // Header: subject and tutor
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
                        text = "Code: ${analytics.subjectCode}",
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
                
                // Visual indicator
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

            // Main metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Sessions per Hour",
                    value = String.format("%.2f", analytics.overallSessionsPerHour),
                    unit = "sessions/h",
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                MetricItem(
                    label = "Occupancy",
                    value = "${analytics.overallOccupancyRate.roundToInt()}%",
                    unit = "",
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                MetricItem(
                    label = "Total Sessions",
                    value = "${analytics.totalSessions}",
                    unit = "sessions",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Demand breakdown
            Text(
                text = "Demand Period Breakdown",
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
                    title = "High Demand",
                    sessionsPerHour = analytics.highDemandMetrics.sessionsPerHour,
                    occupancyRate = analytics.highDemandMetrics.occupancyRate,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                DemandPeriodItem(
                    title = "Normal Demand",
                    sessionsPerHour = analytics.normalDemandMetrics.sessionsPerHour,
                    occupancyRate = analytics.normalDemandMetrics.occupancyRate,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: status and occupancy
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
                        OccupancyLevel.OVERLOADED -> "⚠️ Overloaded tutor: session volume exceeds availability"
                        OccupancyLevel.MODERATE -> "⚡ Moderate occupancy: the tutor has limited capacity"
                        OccupancyLevel.AVAILABLE -> "✅ Available: the tutor can take more sessions"
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
 * Component to display an individual metric.
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
 * Component to display demand period metrics.
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
            text = "Sessions/h: ${String.format("%.2f", sessionsPerHour)}",
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryOrange,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = "Occupancy: ${occupancyRate.roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}
