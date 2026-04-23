package com.calico.tutor.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calico.tutor.data.dto.request.CreateAvailabilityRequest
import com.calico.tutor.data.dto.request.UpdateAvailabilityRequest
import com.calico.tutor.domain.model.AvailabilityItem
import com.calico.tutor.ui.theme.PrimaryOrange
import com.calico.tutor.ui.viewmodel.AvailabilityActionState
import com.calico.tutor.ui.viewmodel.AvailabilityViewModel
import com.calico.tutor.ui.viewmodel.AvailabilityViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

private val TIME_SLOTS = listOf(
    "07:00", "07:30", "08:00", "08:30", "09:00", "09:30",
    "10:00", "10:30", "11:00", "11:30", "12:00", "12:30",
    "13:00", "13:30", "14:00", "14:30", "15:00", "15:30",
    "16:00", "16:30", "17:00", "17:30", "18:00", "18:30",
    "19:00", "19:30", "20:00", "20:30", "21:00", "21:30"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAvailabilityScreen(
    context: Context,
    tutorId: String,
    editingItem: AvailabilityItem? = null,
    onNavigateBack: () -> Unit
) {
    val vm: AvailabilityViewModel = viewModel(
        key = "availability_$tutorId",
        factory = AvailabilityViewModelFactory(context, tutorId)
    )
    val actionState by vm.actionState.collectAsState()
    val isEditing = editingItem != null

    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = dateFmt.format(Date())

    var title by remember { mutableStateOf(editingItem?.title ?: "") }
    var date by remember { mutableStateOf(editingItem?.date ?: today) }
    var startTime by remember { mutableStateOf(editingItem?.startTime ?: "") }
    var endTime by remember { mutableStateOf(editingItem?.endTime ?: "") }
    var location by remember { mutableStateOf(editingItem?.location ?: "Online") }
    var description by remember { mutableStateOf(editingItem?.description ?: "") }
    var course by remember { mutableStateOf(editingItem?.course ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is AvailabilityActionState.Done -> {
                vm.resetActionState()
                onNavigateBack()
            }
            is AvailabilityActionState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_SHORT).show()
                vm.resetActionState()
            }
            else -> {}
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                dateFmt.parse(date)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = millis
                        date = dateFmt.format(cal.time)
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker) {
        TimeSlotPickerDialog(
            title = "Hora de inicio",
            selectedTime = startTime,
            onSelect = { startTime = it; showStartTimePicker = false },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        TimeSlotPickerDialog(
            title = "Hora de fin",
            selectedTime = endTime,
            onSelect = { endTime = it; showEndTimePicker = false },
            onDismiss = { showEndTimePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditing) "Editar disponibilidad" else "Nueva disponibilidad")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    label = { Text("Fecha") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = PrimaryOrange)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Black,
                        disabledBorderColor = Color.Gray,
                        disabledLabelColor = Color.Gray,
                        disabledTrailingIconColor = PrimaryOrange
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStartTimePicker = true }
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = {},
                        label = { Text("Inicio") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        placeholder = { Text("HH:MM") },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.Gray
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEndTimePicker = true }
                ) {
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = {},
                        label = { Text("Fin") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        placeholder = { Text("HH:MM") },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.Gray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Lugar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = course,
                onValueChange = { course = it },
                label = { Text("Curso (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(24.dp))

            val isLoading = actionState is AvailabilityActionState.Loading

            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "El título es obligatorio", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (startTime.isBlank() || endTime.isBlank()) {
                        Toast.makeText(context, "Selecciona la hora de inicio y fin", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isEditing && editingItem != null) {
                        vm.update(
                            editingItem.id,
                            UpdateAvailabilityRequest(
                                title = title,
                                date = date,
                                startTime = startTime,
                                endTime = endTime,
                                location = location,
                                description = description.ifBlank { null },
                                course = course.ifBlank { null }
                            )
                        )
                    } else {
                        vm.create(
                            CreateAvailabilityRequest(
                                tutorId = tutorId,
                                title = title,
                                date = date,
                                startTime = startTime,
                                endTime = endTime,
                                location = location,
                                description = description,
                                course = course
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = if (isEditing) "Guardar cambios" else "Crear disponibilidad",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TimeSlotPickerDialog(
    title: String,
    selectedTime: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(TIME_SLOTS) { slot ->
                    val isSelected = slot == selectedTime
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryOrange else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) PrimaryOrange else Color.Gray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelect(slot) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = slot,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else Color.Black
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
