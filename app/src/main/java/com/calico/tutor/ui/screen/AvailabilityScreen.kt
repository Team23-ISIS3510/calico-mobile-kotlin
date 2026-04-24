package com.calico.tutor.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.abs

private val START_TIME_SLOTS = (7..19).map { h -> String.format("%02d:00", h) }
private val END_TIME_SLOTS   = (8..20).map { h -> String.format("%02d:00", h) }

private fun to12h(time24: String): String = try {
    SimpleDateFormat("h:mm a", Locale.US)
        .format(SimpleDateFormat("HH:mm", Locale.US).parse(time24)!!)
} catch (e: Exception) { time24 }

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
    val listState    by vm.listState.collectAsState()
    val actionState  by vm.actionState.collectAsState()
    val pendingCount by vm.pendingCount.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var createStep by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedStartTime by remember { mutableStateOf(START_TIME_SLOTS[0]) }
    var selectedEndTime by remember { mutableStateOf(END_TIME_SLOTS[1]) }
    var title by remember { mutableStateOf("") }
    var repeatOption by remember { mutableStateOf(RepeatOption.NONE) }
    var showRepeatSheet by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is AvailabilityActionState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                vm.resetActionState()
            }
            is AvailabilityActionState.OfflineSaved -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                vm.resetActionState()
                selectedTab = 0
                createStep = 0
                selectedDate = ""
                selectedStartTime = START_TIME_SLOTS[0]
                selectedEndTime = END_TIME_SLOTS[1]
                title = ""
                repeatOption = RepeatOption.NONE
            }
            is AvailabilityActionState.Done -> {
                vm.resetActionState()
                selectedTab = 0
                createStep = 0
                selectedDate = ""
                selectedStartTime = START_TIME_SLOTS[0]
                selectedEndTime = END_TIME_SLOTS[1]
                title = ""
                repeatOption = RepeatOption.NONE
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.calico_logo),
                    contentDescription = "Calico Logo",
                    modifier = Modifier.size(64.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                AvailabilityTabItem(
                    text = "See availabilities",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                AvailabilityTabItem(
                    text = "Availability",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1; createStep = 0 },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // Badge de sincronizaciones pendientes (Eventual Connectivity)
            if (pendingCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$pendingCount disponibilidad(es) pendiente(s) de sincronizar",
                        color = PrimaryOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            when (selectedTab) {
                0 -> SeeAvailabilitiesTab(listState, vm, onNavigateToEdit)
                1 -> when (createStep) {
                    0 -> CalendarStep(
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        onNext = {
                            if (selectedDate.isBlank()) {
                                Toast.makeText(context, "Please select a date first", Toast.LENGTH_SHORT).show()
                            } else {
                                createStep = 1
                            }
                        }
                    )
                    1 -> TimeStep(
                        title = title,
                        selectedStartTime = selectedStartTime,
                        selectedEndTime = selectedEndTime,
                        repeatOption = repeatOption,
                        isLoading = actionState is AvailabilityActionState.Loading,
                        onTitleChange = { title = it },
                        onStartTimeSelected = { selectedStartTime = it },
                        onEndTimeSelected = { selectedEndTime = it },
                        onRepeatClick = { showRepeatSheet = true },
                        onBack = { createStep = 0 },
                        onSave = {
                            val effectiveTitle = title.trim().ifBlank { "Free" }
                            val err = validateAvailability(effectiveTitle, selectedDate, selectedStartTime, selectedEndTime)
                            if (err != null) {
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                return@TimeStep
                            }
                            vm.createBatch(
                                getRepeatDates(selectedDate, repeatOption).map { date ->
                                    CreateAvailabilityRequest(
                                        tutorId = tutorId,
                                        title = effectiveTitle,
                                        date = date,
                                        startTime = selectedStartTime,
                                        endTime = selectedEndTime
                                    )
                                }
                            )
                        }
                    )
                }
            }
        }

        // Only show blocking overlay while actively waiting for a network response.
        // OfflineSaved/Done/Error are emitted immediately, so this disappears quickly.
        if (actionState is AvailabilityActionState.Loading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = PrimaryOrange) }
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

private fun validateAvailability(title: String, date: String, start: String, end: String): String? {
    if (title.length > 50) return "Title must be 50 characters or less"
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = fmt.format(Date())
    if (date <= today) return "You cannot set availability for today. Please select a future date"
    if (start >= end) return "Start time must be before end time"
    if (start > "19:00") return "Start time must be 7:00 PM or earlier"
    if (end < "08:00") return "End time must be 8:00 AM or later"
    return null
}

@Composable
private fun AvailabilityTabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (isSelected) PrimaryOrange else MediumGray,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp,
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (isSelected) PrimaryOrange else Color.Transparent, RoundedCornerShape(1.dp))
        )
    }
}

@Composable
private fun SeeAvailabilitiesTab(
    listState: AvailabilityListState,
    vm: AvailabilityViewModel,
    onNavigateToEdit: (AvailabilityItem) -> Unit
) {
    when (listState) {
        is AvailabilityListState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = PrimaryOrange)
        }
        is AvailabilityListState.Error -> Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(listState.message, color = Color.Red, fontSize = 14.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.load() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) { Text("Retry") }
        }
        is AvailabilityListState.Success -> {
            val grouped = groupByDay(listState.items)
            if (grouped.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No upcoming availabilities", color = MediumGray, fontSize = 14.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    grouped.forEach { (label, dayItems) ->
                        item {
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                color = MediumGray, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        items(dayItems) { item ->
                            AvailabilityCard(item, onEdit = { onNavigateToEdit(item) }, onDelete = { vm.deleteByTutor() })
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun CalendarStep(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Select a date", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Spacer(Modifier.height(16.dp))
        CalendarView(selectedDate, onDateSelected)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
        ) { Text("Create event", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CalendarView(selectedDate: String, onDateSelected: (String) -> Unit) {
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val now = Calendar.getInstance()
    val minCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val minDate = dateFmt.format(minCal.time)
    var displayYear by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableStateOf(now.get(Calendar.MONTH)) }
    val monthNames = listOf("January","February","March","April","May","June",
        "July","August","September","October","November","December")
    val firstDayCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, displayYear); set(Calendar.MONTH, displayMonth); set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    Column {
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
                            val isSelected = dateStr == selectedDate
                            val isToday = dateStr == dateFmt.format(Date())
                            val isPast = dateStr < minDate
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) PrimaryOrange else Color.Transparent)
                                    .clickable(enabled = !isPast) { onDateSelected(dateStr) },
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
    }
}

@Composable
private fun TimeStep(
    title: String,
    selectedStartTime: String,
    selectedEndTime: String,
    repeatOption: RepeatOption,
    isLoading: Boolean,
    onTitleChange: (String) -> Unit,
    onStartTimeSelected: (String) -> Unit,
    onEndTimeSelected: (String) -> Unit,
    onRepeatClick: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black) }
            Text("Select time", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.size(48.dp))
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 50) onTitleChange(it) },
                placeholder = { Text("Title (optional)", color = MediumGray, fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.7f).align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = Color(0xFFDDDDDD)
                )
            )
            Spacer(Modifier.height(20.dp))
            Text("Start time", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            WheelTimePicker(START_TIME_SLOTS, selectedStartTime, onStartTimeSelected, Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            Text("End time", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            WheelTimePicker(END_TIME_SLOTS, selectedEndTime, onEndTimeSelected, Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = onRepeatClick,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(repeatOption.label, color = Color.Black, fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Black, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text("Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun WheelTimePicker(
    slots: List<String>,
    selectedTime: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeightDp = 48.dp
    val visibleCount = 5
    val initialIndex = slots.indexOf(selectedTime).coerceAtLeast(0)
    val lazyState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyState)

    val centerIndex by remember {
        derivedStateOf {
            val info = lazyState.layoutInfo
            val vpCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs(it.offset + it.size / 2 - vpCenter) }
                ?.index ?: initialIndex
        }
    }

    LaunchedEffect(lazyState.isScrollInProgress) {
        if (!lazyState.isScrollInProgress && centerIndex in slots.indices) {
            onSelected(slots[centerIndex])
        }
    }

    Box(modifier = modifier.height(itemHeightDp * visibleCount)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeightDp)
                .background(Color(0xFFF2F2F2), RoundedCornerShape(10.dp))
        )
        LazyColumn(
            state = lazyState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeightDp * (visibleCount / 2)),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(slots) { index, slot ->
                val isCenter = index == centerIndex
                Box(Modifier.height(itemHeightDp).fillMaxWidth(), Alignment.Center) {
                    Text(
                        text = to12h(slot),
                        fontSize = if (isCenter) 20.sp else 15.sp,
                        color = if (isCenter) Color.Black else Color(0xFFBBBBBB),
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(itemHeightDp * 2).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.White, Color.Transparent)))
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(itemHeightDp * 2).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White)))
        )
    }
}

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
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Text("Repeat", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
            RepeatOption.entries.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(option.label, fontSize = 14.sp, color = Color.Black)
                    RadioButton(selected = option == currentOption, onClick = { onSelect(option) },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryOrange))
                }
                HorizontalDivider(color = Color(0xFFEEEEEE))
            }
        }
    }
}

@Composable
private fun AvailabilityCard(item: AvailabilityItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete tutor availabilities") },
            text = { Text("This will delete all availabilities for this tutor. Continue?") },
            confirmButton = {
                TextButton(onClick = { showDialog = false; onDelete() }) { Text("Delete all", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth().shadow(elevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(item.date, color = MediumGray, fontSize = 12.sp)
                Spacer(Modifier.height(2.dp))
                Text("${to12h(item.startTime)} – ${to12h(item.endTime)}",
                    color = PrimaryOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Column {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", tint = PrimaryOrange) }
                IconButton(onClick = { showDialog = true }) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFE53935)) }
            }
        }
    }
}

private fun groupByDay(items: List<AvailabilityItem>): LinkedHashMap<String, List<AvailabilityItem>> {
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayFmt = SimpleDateFormat("EEE, d MMM", Locale.ENGLISH)
    val today = dateFmt.format(Date())
    val tomorrow = dateFmt.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time)
    val sorted = items.sortedWith(compareBy({ it.date }, { it.startTime }))
    val map = LinkedHashMap<String, MutableList<AvailabilityItem>>()
    for (item in sorted) {
        val label = when (item.date) {
            today -> "TODAY"; tomorrow -> "TOMORROW"
            else -> try { dateFmt.parse(item.date)?.let { displayFmt.format(it).uppercase() } ?: item.date }
                    catch (e: Exception) { item.date }
        }
        map.getOrPut(label) { mutableListOf() }.add(item)
    }
    @Suppress("UNCHECKED_CAST")
    return map as LinkedHashMap<String, List<AvailabilityItem>>
}

private fun getRepeatDates(baseDate: String, option: RepeatOption): List<String> {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val base = try { fmt.parse(baseDate)!! } catch (e: Exception) { return listOf(baseDate) }
    return when (option) {
        RepeatOption.NONE -> listOf(baseDate)
        RepeatOption.WEEKLY -> (0..15).map { w ->
            fmt.format(Calendar.getInstance().apply { time = base; add(Calendar.WEEK_OF_YEAR, w) }.time)
        }
        RepeatOption.MONTHLY -> (0..3).map { m ->
            fmt.format(Calendar.getInstance().apply { time = base; add(Calendar.MONTH, m) }.time)
        }
    }
}
