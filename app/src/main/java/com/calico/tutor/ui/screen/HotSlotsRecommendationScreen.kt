package com.calico.tutor.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calico.tutor.domain.model.HotSlot
import com.calico.tutor.domain.model.HotSlotsAnalysis
import com.calico.tutor.ui.theme.PrimaryOrange
import com.calico.tutor.ui.theme.WhiteBase
import com.calico.tutor.ui.viewmodel.HotSlotsState
import com.calico.tutor.ui.viewmodel.HotSlotsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotSlotsRecommendationScreen(
    viewModel: HotSlotsViewModel,
    tutorId: String,
    onDismiss: () -> Unit
) {
    val state = viewModel.hotSlotsState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHotSlots(tutorId)
    }

    Scaffold(
        containerColor = WhiteBase,
        topBar = {
            TopAppBar(
                title = { Text("Hot Slots", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WhiteBase,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        when (val currentState = state.value) {
            is HotSlotsState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(WhiteBase),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            }

            is HotSlotsState.Success -> {
                HotSlotsContent(
                    analysis = currentState.analysis,
                    modifier = Modifier.padding(padding)
                )
            }

            is HotSlotsState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(WhiteBase),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Error loading recommendations",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            currentState.message,
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onDismiss) {
                            Text("Back")
                        }
                    }
                }
            }

            is HotSlotsState.Idle -> {
                // Initial state, wait for LaunchedEffect
            }
        }
    }
}

@Composable
fun HotSlotsContent(
    analysis: HotSlotsAnalysis,
    modifier: Modifier = Modifier
) {
    if (analysis.hotSlots.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No hot slots found",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No time slots with bookings in the past week.",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Summary card
        item {
            SummaryCard(analysis)
        }

        // Hot slots list
        items(analysis.hotSlots) { slot ->
            HotSlotCard(slot)
        }

        // Footer
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "These are the most booked time slots by students in the last week. Set availability for these times to get more tutoring sessions.",
                fontSize = 12.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatDateDisplay(isoDate: String): String = try {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    val date = inputFormat.parse(isoDate.take(19)) ?: return isoDate
    outputFormat.format(date)
} catch (e: Exception) {
    isoDate
}

@Composable
fun SummaryCard(analysis: HotSlotsAnalysis) {
    val startDate = formatDateDisplay(analysis.analysisStartDate)
    val endDate = formatDateDisplay(analysis.analysisEndDate)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Analysis Period",
                fontSize = 12.sp,
                color = Color(0xFFF57C00),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "$startDate to $endDate",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total Sessions Last Week:",
                    fontSize = 12.sp,
                    color = Color(0xFFF57C00)
                )
                Text(
                    "${analysis.totalSessionsLastWeek}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF57C00)
                )
            }
        }
    }
}

@Composable
fun HotSlotCard(slot: HotSlot) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WhiteBase
        ),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        slot.slotStart.takeLast(5), // HH:MM
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "to ${slot.slotEnd.takeLast(5)}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "${slot.bookingCount} bookings",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(slot.tutorAvailability)
                }
            }

            if (slot.availabilityStart != null && slot.availabilityEnd != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Your availability: ${slot.availabilityStart.takeLast(5)} - ${slot.availabilityEnd.takeLast(5)}",
                    fontSize = 12.sp,
                    color = Color(0xFF2C3E50),
                    modifier = Modifier
                        .background(
                            Color(0xFFF3F3F3),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(availability: String) {
    val isAvailable = availability.lowercase() == "available"
    val backgroundColor = if (isAvailable) {
        Color(0xFFC8E6C9)
    } else {
        Color(0xFFFFCDD2)
    }

    val textColor = if (isAvailable) {
        Color(0xFF2E7D32)
    } else {
        Color(0xFFC62828)
    }

    Text(
        if (isAvailable) "Available" else "Not Available",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Preview
@Composable
fun PreviewHotSlotsScreen() {
    val sampleAnalysis = HotSlotsAnalysis(
        tutorId = "tutor_123",
        analysisStartDate = "2024-01-08",
        analysisEndDate = "2024-01-15",
        totalSessionsLastWeek = 12,
        hotSlots = listOf(
            HotSlot(
                slotStart = "2024-01-15T14:00:00Z",
                slotEnd = "2024-01-15T15:00:00Z",
                bookingCount = 5,
                tutorAvailability = "not_available"
            ),
            HotSlot(
                slotStart = "2024-01-15T10:00:00Z",
                slotEnd = "2024-01-15T11:00:00Z",
                bookingCount = 4,
                tutorAvailability = "available",
                availabilityStart = "2024-01-15T10:00:00Z",
                availabilityEnd = "2024-01-15T11:00:00Z"
            )
        )
    )

    HotSlotsContent(sampleAnalysis)
}
