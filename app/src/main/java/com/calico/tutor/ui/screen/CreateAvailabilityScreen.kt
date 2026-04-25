package com.calico.tutor.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calico.tutor.data.dto.request.UpdateAvailabilityRequest
import com.calico.tutor.domain.model.AvailabilityItem
import com.calico.tutor.ui.theme.MediumGray
import com.calico.tutor.ui.theme.PrimaryOrange
import com.calico.tutor.ui.viewmodel.AvailabilityActionState
import com.calico.tutor.ui.viewmodel.AvailabilityViewModel
import com.calico.tutor.ui.viewmodel.AvailabilityViewModelFactory
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val EDIT_START_SLOTS = (7..19).map { h -> String.format("%02d:00", h) }
private val EDIT_END_SLOTS   = (8..20).map { h -> String.format("%02d:00", h) }

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
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val minDate = dateFmt.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time)

    var title by remember { mutableStateOf(editingItem?.title ?: "") }
    var date by remember {
        mutableStateOf(
            if (editingItem?.date != null && editingItem.date > dateFmt.format(Date()))
                editingItem.date else minDate
        )
    }
    var startTime by remember {
        mutableStateOf(
            editingItem?.startTime?.let { if (it in EDIT_START_SLOTS) it else EDIT_START_SLOTS[0] }
                ?: EDIT_START_SLOTS[0]
        )
    }
    var endTime by remember {
        mutableStateOf(
            editingItem?.endTime?.let { if (it in EDIT_END_SLOTS) it else EDIT_END_SLOTS[1] }
                ?: EDIT_END_SLOTS[1]
        )
    }

    val initCal = remember { Calendar.getInstance().apply { dateFmt.parse(date)?.let { time = it } } }
    var displayYear by remember { mutableStateOf(initCal.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableStateOf(initCal.get(Calendar.MONTH)) }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is AvailabilityActionState.Done -> {
                vm.consumePendingActionMessage()?.let { message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    delay(350)
                }
                vm.resetActionState()
                onNavigateBack()
            }
            is AvailabilityActionState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                vm.resetActionState()
            }
            else -> {}
        }
    }

    val isLoading = actionState is AvailabilityActionState.Loading
    val monthNames = listOf("January","February","March","April","May","June",
        "July","August","September","October","November","December")

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Edit availability", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White, titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 50) title = it },
                placeholder = { Text("Title (optional)", color = MediumGray, fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.75f).align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = Color(0xFFDDDDDD)
                )
            )

            Spacer(Modifier.height(20.dp))
            Text("Select date", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
            Spacer(Modifier.height(12.dp))

            val firstDayCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, displayYear); set(Calendar.MONTH, displayMonth); set(Calendar.DAY_OF_MONTH, 1)
            }
            val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1
            val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (displayMonth == 0) { displayMonth = 11; displayYear-- } else displayMonth--
                }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Prev", tint = Color.Black) }
                Text("${monthNames[displayMonth]} $displayYear", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                IconButton(onClick = {
                    if (displayMonth == 11) { displayMonth = 0; displayYear++ } else displayMonth++
                }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", tint = Color.Black) }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("S","M","T","W","T","F","S").forEach { d ->
                    Text(d, Modifier.weight(1f), textAlign = TextAlign.Center,
                        color = MediumGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(8.dp))

            val rows = ((firstDayOfWeek + daysInMonth) + 6) / 7
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val day = row * 7 + col - firstDayOfWeek + 1
                        Box(Modifier.weight(1f).aspectRatio(1f), Alignment.Center) {
                            if (day in 1..daysInMonth) {
                                val dateStr = String.format("%d-%02d-%02d", displayYear, displayMonth + 1, day)
                                val isSelected = dateStr == date
                                val isToday = dateStr == dateFmt.format(Date())
                                val isPast = dateStr < minDate
                                Box(
                                    modifier = Modifier
                                        .size(36.dp).clip(CircleShape)
                                        .background(if (isSelected) PrimaryOrange else Color.Transparent)
                                        .clickable(enabled = !isPast) { date = dateStr },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        day.toString(),
                                        color = when {
                                            isPast -> Color(0xFFCCCCCC)
                                            isSelected -> Color.White
                                            isToday -> PrimaryOrange
                                            else -> Color.Black
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Start time", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            WheelTimePicker(EDIT_START_SLOTS, startTime, { startTime = it }, Modifier.fillMaxWidth())

            Spacer(Modifier.height(20.dp))
            Text("End time", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            WheelTimePicker(EDIT_END_SLOTS, endTime, { endTime = it }, Modifier.fillMaxWidth())

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    val effectiveTitle = title.trim().ifBlank { "Free" }
                    if (effectiveTitle.length > 50) {
                        Toast.makeText(context, "Title must be 50 characters or less", Toast.LENGTH_LONG).show(); return@Button
                    }
                    val today = dateFmt.format(Date())
                    if (date <= today) {
                        Toast.makeText(context, "You cannot set availability for today. Please select a future date", Toast.LENGTH_LONG).show(); return@Button
                    }
                    if (startTime >= endTime) {
                        Toast.makeText(context, "Start time must be before end time", Toast.LENGTH_LONG).show(); return@Button
                    }
                    if (startTime > "19:00") {
                        Toast.makeText(context, "Start time must be 7:00 PM or earlier", Toast.LENGTH_LONG).show(); return@Button
                    }
                    if (endTime < "08:00") {
                        Toast.makeText(context, "End time must be 8:00 AM or later", Toast.LENGTH_LONG).show(); return@Button
                    }
                    if (editingItem != null) {
                        vm.update(
                            editingItem.id,
                            UpdateAvailabilityRequest(
                                title = effectiveTitle,
                                date = date,
                                startTime = startTime,
                                endTime = endTime
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text("Save changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
