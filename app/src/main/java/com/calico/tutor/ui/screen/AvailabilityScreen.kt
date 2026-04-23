package com.calico.tutor.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calico.tutor.R
import com.calico.tutor.data.dto.request.CreateAvailabilityRequest
import com.calico.tutor.domain.model.AvailabilityItem
import com.calico.tutor.ui.theme.MediumGray
import com.calico.tutor.ui.theme.PrimaryOrange
import com.calico.tutor.ui.viewmodel.AvailabilityActionState
import com.calico.tutor.ui.viewmodel.AvailabilityListState
import com.calico.tutor.ui.viewmodel.AvailabilityViewModel
import com.calico.tutor.ui.viewmodel.AvailabilityViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

enum class RepeatOption(val label: String) {
    NONE("Does not repeat"),
    WEEKLY("Every week"),
    MONTHLY("Every month")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityScreen(
    context: Context,
    tutorId: String,
    onNavigateToEdit: (AvailabilityItem) -> Unit
) {
    val vm: AvailabilityViewModel = viewModel(
        key = "availability_$tutorId",
        factory = AvailabilityViewModelFactory(context, tutorId)
    )
    val listState by vm.listState.collectAsState()
    val actionState by vm.actionState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var createStep by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedStartTime by remember { mutableStateOf("") }
    var selectedEndTime by remember { mutableStateOf("") }
    var repeatOption by remember { mutableStateOf(RepeatOption.NONE) }
    var showRepeatSheet by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is AvailabilityActionState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_SHORT).show()
                vm.resetActionState()
            }
            is AvailabilityActionState.Done -> {
                vm.resetActionState()
                selectedTab = 0
                createStep = 0
                selectedDate = ""
                selectedStartTime = ""
                selectedEndTime = ""
                repeatOption = RepeatOption.NONE
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Logo header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.calico_logo),
                    contentDescription = "Calico Logo",
                    modifier = Modifier.size(64.dp)
                )
            }

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvailabilityTabItem(
                    text = "See availabilities",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                Spacer(modifier = Modifier.width(28.dp))
                AvailabilityTabItem(
                    text = "Availability",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1; createStep = 0 }
                )
            }

            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // Content
            when (selectedTab) {
                0 -> SeeAvailabilitiesTab(listState, vm, onNavigateToEdit)
                1 -> when (createStep) {
                    0 -> CalendarStep(
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        onCreateEvent = {
                            if (selectedDate.isNotBlank()) createStep = 1
                            else Toast.makeText(context, "Selecciona una fecha primero", Toast.LENGTH_SHORT).show()
                        }
                    )
                    1 -> TimeRepeatStep(
                        selectedStartTime = selectedStartTime,
                        selectedEndTime = selectedEndTime,
                        repeatOption = repeatOption,
                        isLoading = actionState is AvailabilityActionState.Loading,
                        onStartTimeSelected = { selectedStartTime = it },
                        onEndTimeSelected = { selectedEndTime = it },
                        onRepeatClick = { showRepeatSheet = true },
                        onBack = { createStep = 0 },
                        onSave = {
                            if (selectedStartTime.isBlank() || selectedEndTime.isBlank()) {
                                Toast.makeText(context, "Selecciona hora de inicio y fin", Toast.LENGTH_SHORT).show()
                                return@TimeRepeatStep
                            }
                            if (selectedStartTime >= selectedEndTime) {
                                Toast.makeText(context, "La hora de inicio debe ser antes que la de fin", Toast.LENGTH_SHORT).show()
                                return@TimeRepeatStep
                            }
                            vm.createBatch(
                                getRepeatDates(selectedDate, repeatOption).map { date ->
                                    CreateAvailabilityRequest(
                                        tutorId = tutorId,
                                        title = "Disponibilidad",
                                        date = date,
                                        startTime = selectedStartTime,
                                        endTime = selectedEndTime,
                                        location = "Online",
                                        description = "",
                                        course = ""
                                    )
                                }
                            )
                        }
                    )
                }
            }
        }

        if (actionState is AvailabilityActionState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryOrange)
            }
        }
    }

    if (showRepeatSheet) {
        RepeatBottomSheet(
            currentOption = repeatOption,
            onSelect = { repeatOption = it; showRepeatSheet = false },
            onDismiss = { showRepeatSheet = false }
        )
    }
}

// ── Tab label with orange underline ─────────────────────────────────────────

@Composable
private fun AvailabilityTabItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (isSelected) PrimaryOrange else MediumGray,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(PrimaryOrange, RoundedCornerShape(1.dp))
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

// ── "See availabilities" tab content ────────────────────────────────────────

@Composable
private fun SeeAvailabilitiesTab(
    listState: AvailabilityListState,
    vm: AvailabilityViewModel,
    onNavigateToEdit: (AvailabilityItem) -> Unit
) {
    when (listState) {
        is AvailabilityListState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = PrimaryOrange) }

        is AvailabilityListState.Error -> Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(listState.message, color = Color.Red, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { vm.load() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) { Text("Reintentar") }
        }

        is AvailabilityListState.Success -> {
            val grouped = groupByDay(listState.items)
            if (grouped.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No tienes disponibilidades registradas",
                        color = MediumGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    grouped.forEach { (label, dayItems) ->
                        item {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MediumGray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(dayItems) { item ->
                            AvailabilityCard(
                                item = item,
                                onEdit = { onNavigateToEdit(item) },
                                onDelete = { vm.delete(item.id) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        else -> {}
    }
}

// ── Step 1: Calendar ─────────────────────────────────────────────────────────

@Composable
private fun CalendarStep(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onCreateEvent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Select a date", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))

        CalendarView(selectedDate = selectedDate, onDateSelected = onDateSelected)

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onCreateEvent,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
        ) {
            Text("Create event", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CalendarView(selectedDate: String, onDateSelected: (String) -> Unit) {
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = dateFmt.format(Date())
    val now = Calendar.getInstance()

    var displayYear by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableStateOf(now.get(Calendar.MONTH)) }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val firstDayCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, displayYear)
        set(Calendar.MONTH, displayMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column {
        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (displayMonth == 0) { displayMonth = 11; displayYear-- } else displayMonth--
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Anterior", tint = Color.Black)
            }
            Text(
                text = "${monthNames[displayMonth]} $displayYear",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            IconButton(onClick = {
                if (displayMonth == 11) { displayMonth = 0; displayYear++ } else displayMonth++
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Siguiente", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                Text(
                    text = d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MediumGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day cells
        val rows = ((firstDayOfWeek + daysInMonth) + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val day = row * 7 + col - firstDayOfWeek + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..daysInMonth) {
                            val dateStr = String.format("%d-%02d-%02d", displayYear, displayMonth + 1, day)
                            val isSelected = dateStr == selectedDate
                            val isToday = dateStr == today

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) PrimaryOrange else Color.Transparent)
                                    .clickable { onDateSelected(dateStr) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = when {
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
    }
}

// ── Step 2: Time + Repeat + Save ─────────────────────────────────────────────

private val AVAILABILITY_TIME_SLOTS = listOf(
    "07:00", "08:00", "09:00", "10:00", "11:00", "12:00",
    "13:00", "14:00", "15:00", "16:00", "17:00", "18:00",
    "19:00", "20:00", "21:00"
)

@Composable
private fun TimeRepeatStep(
    selectedStartTime: String,
    selectedEndTime: String,
    repeatOption: RepeatOption,
    isLoading: Boolean,
    onStartTimeSelected: (String) -> Unit,
    onEndTimeSelected: (String) -> Unit,
    onRepeatClick: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.Black)
            }
            Text(
                text = "Select days",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select a time", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(12.dp))

            // Two-column time grid
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    AVAILABILITY_TIME_SLOTS.forEach { slot ->
                        TimeSlotCell(
                            time = slot,
                            isSelected = slot == selectedStartTime,
                            endPadding = true,
                            onClick = { onStartTimeSelected(slot) }
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    AVAILABILITY_TIME_SLOTS.forEach { slot ->
                        TimeSlotCell(
                            time = slot,
                            isSelected = slot == selectedEndTime,
                            endPadding = false,
                            onClick = { onEndTimeSelected(slot) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Repeat button
            OutlinedButton(
                onClick = onRepeatClick,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(text = repeatOption.label, color = Color.Black, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TimeSlotCell(time: String, isSelected: Boolean, endPadding: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                end = if (endPadding) 6.dp else 0.dp,
                start = if (endPadding) 0.dp else 6.dp,
                bottom = 8.dp
            )
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) PrimaryOrange else Color(0xFFF5F5F5))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatTime24to12(time),
            fontSize = 13.sp,
            color = if (isSelected) Color.White else Color.Black,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ── Repeat bottom sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatBottomSheet(
    currentOption: RepeatOption,
    onSelect: (RepeatOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text = "Repeat",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            RepeatOption.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = option.label, fontSize = 14.sp, color = Color.Black)
                    RadioButton(
                        selected = option == currentOption,
                        onClick = { onSelect(option) },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryOrange)
                    )
                }
                HorizontalDivider(color = Color(0xFFEEEEEE))
            }
        }
    }
}

// ── Availability card (list view) ─────────────────────────────────────────────

@Composable
private fun AvailabilityCard(item: AvailabilityItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar disponibilidad") },
            text = { Text("¿Deseas eliminar \"${item.title}\"?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${formatTime24to12(item.startTime)} – ${formatTime24to12(item.endTime)}",
                    color = PrimaryOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!item.course.isNullOrBlank()) Text(item.course, color = MediumGray, fontSize = 12.sp)
                if (!item.description.isNullOrBlank()) Text(item.description, color = MediumGray, fontSize = 12.sp)
                Text(item.location, color = MediumGray, fontSize = 12.sp)
            }
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Editar", tint = PrimaryOrange)
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, "Eliminar", tint = Color(0xFFE53935))
                }
            }
        }
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

private fun groupByDay(items: List<AvailabilityItem>): LinkedHashMap<String, List<AvailabilityItem>> {
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = dateFmt.format(Date())
    val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val tomorrow = dateFmt.format(tomorrowCal.time)
    val displayFmt = SimpleDateFormat("EEE, d MMM", Locale("es"))
    val sorted = items.sortedWith(compareBy({ it.date }, { it.startTime }))
    val map = LinkedHashMap<String, MutableList<AvailabilityItem>>()
    for (item in sorted) {
        val label = when (item.date) {
            today -> "HOY"
            tomorrow -> "MAÑANA"
            else -> try {
                dateFmt.parse(item.date)?.let { displayFmt.format(it).uppercase(Locale.getDefault()) } ?: item.date
            } catch (e: Exception) { item.date }
        }
        map.getOrPut(label) { mutableListOf() }.add(item)
    }
    @Suppress("UNCHECKED_CAST")
    return map as LinkedHashMap<String, List<AvailabilityItem>>
}

private fun formatTime24to12(time: String): String {
    return try {
        val sdf12 = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf12.format(SimpleDateFormat("HH:mm", Locale.getDefault()).parse(time)!!)
    } catch (e: Exception) { time }
}

private fun getRepeatDates(baseDate: String, option: RepeatOption): List<String> {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val base = try { fmt.parse(baseDate)!! } catch (e: Exception) { return listOf(baseDate) }
    return when (option) {
        RepeatOption.NONE -> listOf(baseDate)
        RepeatOption.WEEKLY -> (0..3).map { w ->
            fmt.format(Calendar.getInstance().apply { time = base; add(Calendar.WEEK_OF_YEAR, w) }.time)
        }
        RepeatOption.MONTHLY -> (0..2).map { m ->
            fmt.format(Calendar.getInstance().apply { time = base; add(Calendar.MONTH, m) }.time)
        }
    }
}
