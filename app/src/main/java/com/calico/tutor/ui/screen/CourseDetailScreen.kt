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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calico.tutor.data.dto.response.TutorOccupancyData
import com.calico.tutor.domain.model.Session
import com.calico.tutor.ui.theme.MediumGray
import com.calico.tutor.ui.theme.PrimaryOrange
import com.calico.tutor.ui.viewmodel.CourseDetailState
import com.calico.tutor.ui.viewmodel.CourseDetailViewModelFactory
import com.calico.tutor.ui.viewmodel.CourseDetailViewModel
import com.calico.tutor.ui.viewmodel.CourseNoteUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: String,
    tutorId: String,
    context: Context,
    onBack: () -> Unit
) {
    val vm: CourseDetailViewModel = viewModel(
        key = "course_detail_${tutorId}_$courseId",
        factory = CourseDetailViewModelFactory(context)
    )
    val uiState by vm.uiState.collectAsState()
    val isOnline by vm.isOnline.collectAsState()
    val isSaving by vm.isSaving.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var noteText by rememberSaveable(courseId) { mutableStateOf("") }
    var noteDirty by rememberSaveable(courseId) { mutableStateOf(false) }

    LaunchedEffect(courseId, tutorId) {
        vm.load(courseId, tutorId)
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is CourseDetailState.Success) {
            if (!noteDirty) {
                noteText = state.noteState.text
            }
        }
    }

    LaunchedEffect(vm) {
        vm.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F8F8),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                CourseDetailState.Idle, CourseDetailState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryOrange)
                    }
                }
                is CourseDetailState.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text(
                            text = state.message,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is CourseDetailState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!isOnline || state.isOffline) {
                            InfoBanner("Offline mode - showing cached data and local notes.")
                        }

                        CourseHeaderCard(state.course)
                        NotesCard(
                            noteText = noteText,
                            isSaving = isSaving,
                            isPendingSync = state.noteState.isPendingSync,
                            onNoteChange = {
                                noteText = it
                                noteDirty = true
                            },
                            onSave = {
                                vm.saveNote(noteText)
                                noteDirty = false
                            }
                        )
                        SessionsCard(state.recentSessions)
                        OccupancyCard(state.occupancy)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = Color(0xFFF57C00)
        )
    }
}

@Composable
private fun CourseHeaderCard(course: com.calico.tutor.domain.model.CourseDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(course.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${course.code} • ${course.credits} credits", color = MediumGray)
            DetailRow("Faculty", course.faculty)
            DetailRow("Semester", course.semester)
            DetailRow("Difficulty", course.difficulty)
            DetailRow("Prerequisites", course.prerequisites.joinToString(", ").ifBlank { "None" })
            Text(course.description.ifBlank { "No description available." })
        }
    }
}

@Composable
private fun NotesCard(
    noteText: String,
    isSaving: Boolean,
    isPendingSync: Boolean,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Saved notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = noteText,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("Course note") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text(if (isSaving) "Saving..." else "Save note")
                }
            }
            if (isPendingSync) {
                Text("Pending sync", color = Color(0xFFF57C00), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SessionsCard(sessions: List<Session>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recent sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (sessions.isEmpty()) {
                Text("No recent sessions for this course.")
            } else {
                sessions.forEach { session ->
                    SessionRow(session)
                }
            }
        }
    }
}

@Composable
private fun OccupancyCard(occupancy: List<TutorOccupancyData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Occupancy summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (occupancy.isEmpty()) {
                Text("No occupancy data available.")
            } else {
                occupancy.take(3).forEach { item ->
                    Text(
                        text = "${item.subject}: ${(item.occupancyRate * 100).toInt()}% occupancy",
                        color = MediumGray
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: Session) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(session.studentName.ifBlank { "Unnamed student" }, fontWeight = FontWeight.SemiBold)
        Text(session.scheduledStart, color = MediumGray)
        Text(session.status.uppercase(), color = PrimaryOrange)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MediumGray)
        Text(value, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}
