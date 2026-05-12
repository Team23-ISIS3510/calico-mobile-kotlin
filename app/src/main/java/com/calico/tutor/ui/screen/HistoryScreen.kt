package com.calico.tutor.ui.screen

import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.calico.tutor.domain.model.Session
import com.calico.tutor.ui.component.OfflineBanner
import com.calico.tutor.ui.theme.MediumGray
import com.calico.tutor.ui.theme.OnSurface
import com.calico.tutor.ui.theme.PrimaryOrange
import com.calico.tutor.ui.theme.SurfaceVariant
import com.calico.tutor.ui.theme.WhiteBase
import com.calico.tutor.ui.viewmodel.HistoryState
import com.calico.tutor.ui.viewmodel.HistoryViewModel
import com.calico.tutor.ui.viewmodel.HistoryViewModelFactory

@Composable
fun HistoryScreen(
    context: Context,
    tutorId: String,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(context)
    )
    val historyState by viewModel.historyState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    LaunchedEffect(tutorId) {
        viewModel.loadTutorHistory(tutorId)
        viewModel.startTutorConnectivityMonitoring(tutorId)
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                }
            }

            if (!isOnline) {
                OfflineBanner()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp)
                ) {
                    when (val state = historyState) {
                        HistoryState.Idle, HistoryState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = PrimaryOrange)
                            }
                        }

                        is HistoryState.Error -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F2))
                            ) {
                                Text(
                                    text = state.message,
                                    modifier = Modifier.padding(16.dp),
                                    color = Color(0xFFB42318),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        is HistoryState.Success -> {
                            val sessions = state.sessions
                            if (sessions.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                                ) {
                                    Text(
                                        text = "No completed tutoring sessions found.",
                                        modifier = Modifier.padding(16.dp),
                                        color = MediumGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                sessions.forEach { session ->
                                    HistorySessionCard(session = session)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySessionCard(session: Session) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFF0E6DD)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(color = Color(0xFFFFF1E8)),
                    contentAlignment = Alignment.Center
                ) {
                    if (session.studentAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = session.studentAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.studentName.ifBlank { "Student Not Available" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = session.course?.ifBlank { session.subjectName } ?: session.subjectName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediumGray
                    )
                }

                PriceChip(price = session.price)
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF0E6DD))
            Spacer(modifier = Modifier.height(14.dp))

            HistoryDetailRow(
                icon = Icons.Default.Person,
                label = "Student",
                value = session.studentName.ifBlank { "Student Not Available" }
            )
            Spacer(modifier = Modifier.height(10.dp))
            HistoryDetailRow(
                icon = Icons.Default.DateRange,
                label = "Session date",
                value = session.date
            )
            Spacer(modifier = Modifier.height(10.dp))
            HistoryDetailRow(
                icon = Icons.Default.AccessTime,
                label = "Session time",
                value = session.time
            )
            Spacer(modifier = Modifier.height(10.dp))
            HistoryDetailRow(
                icon = Icons.Default.AttachMoney,
                label = "Session price",
                value = session.price.ifBlank { "Not available" }
            )
        }
    }
}

@Composable
private fun HistoryDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryOrange,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MediumGray
            )
            Text(
                text = value.ifBlank { "Not available" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = OnSurface
            )
        }
    }
}

@Composable
private fun PriceChip(price: String) {
    Box(
        modifier = Modifier
            .background(
                color = Color(0xFFFFF1E8),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = price.ifBlank { "N/A" },
            color = Color(0xFF9A3412),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}