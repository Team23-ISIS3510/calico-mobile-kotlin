package com.calico.tutor.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calico.tutor.domain.model.AvailabilityItem
import com.calico.tutor.ui.theme.MediumGray
import com.calico.tutor.ui.theme.PrimaryOrange
import com.calico.tutor.ui.viewmodel.AvailabilityActionState
import com.calico.tutor.ui.viewmodel.AvailabilityListState
import com.calico.tutor.ui.viewmodel.AvailabilityViewModel
import com.calico.tutor.ui.viewmodel.AvailabilityViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AvailabilityScreen(
    context: Context,
    tutorId: String,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (AvailabilityItem) -> Unit
) {
    val vm: AvailabilityViewModel = viewModel(
        key = "availability_$tutorId",
        factory = AvailabilityViewModelFactory(context, tutorId)
    )
    val listState by vm.listState.collectAsState()
    val actionState by vm.actionState.collectAsState()

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is AvailabilityActionState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_SHORT).show()
                vm.resetActionState()
            }
            is AvailabilityActionState.Done -> vm.resetActionState()
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        when (val state = listState) {
            is AvailabilityListState.Loading -> {
                CircularProgressIndicator(
                    color = PrimaryOrange,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is AvailabilityListState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message, color = Color.Red, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { vm.load() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) { Text("Reintentar") }
                }
            }

            is AvailabilityListState.Success -> {
                val grouped = groupByDay(state.items)
                if (grouped.isEmpty()) {
                    Text(
                        text = "No tienes disponibilidades registradas",
                        color = MediumGray,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
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

        FloatingActionButton(
            onClick = onNavigateToCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = PrimaryOrange,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nueva disponibilidad")
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
}

@Composable
private fun AvailabilityCard(
    item: AvailabilityItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar disponibilidad") },
            text = { Text("¿Deseas eliminar \"${item.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Eliminar", color = Color.Red) }
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
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.startTime} – ${item.endTime}",
                    color = PrimaryOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!item.course.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = item.course, color = MediumGray, fontSize = 12.sp)
                }
                if (!item.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = item.description, color = MediumGray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = item.location, color = MediumGray, fontSize = 12.sp)
            }
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PrimaryOrange)
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFE53935))
                }
            }
        }
    }
}

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
                val d = dateFmt.parse(item.date)
                if (d != null) displayFmt.format(d).uppercase(Locale.getDefault()) else item.date
            } catch (e: Exception) {
                item.date
            }
        }
        map.getOrPut(label) { mutableListOf() }.add(item)
    }

    @Suppress("UNCHECKED_CAST")
    return map as LinkedHashMap<String, List<AvailabilityItem>>
}
