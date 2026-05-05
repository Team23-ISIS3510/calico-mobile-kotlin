package com.calico.tutor.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.calico.tutor.domain.model.AvailabilityItem
import com.calico.tutor.ui.component.BottomNavBar
import com.calico.tutor.ui.component.NavBarItem
import com.calico.tutor.ui.viewmodel.CoursesViewModel
import com.calico.tutor.ui.viewmodel.ProfileViewModel

@Composable
fun MainScreen(
    userName: String,
    tutorId: String,
    userEmail: String,
    context: Context,
    onLogout: () -> Unit
) {
    var currentRoute by remember { mutableStateOf("home") }
    var editingItem by remember { mutableStateOf<AvailabilityItem?>(null) }
    var showTopSubjects by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavBarItem("Home", Icons.Default.Home, "home"),
        NavBarItem("Courses", Icons.Default.Search, "courses"),
        NavBarItem("Availability", Icons.Default.DateRange, "availability"),
        NavBarItem("Profile", Icons.Default.Person, "profile"),
    )

    when {
        showTopSubjects -> {
            TopSubjectsScreen(
                context = context,
                onNavigateBack = { showTopSubjects = false }
            )
        }
        currentRoute == "availability_edit" -> {
            CreateAvailabilityScreen(
                context = context,
                tutorId = tutorId,
                editingItem = editingItem,
                onNavigateBack = {
                    editingItem = null
                    currentRoute = "availability"
                }
            )
        }
        else -> {
            Scaffold(
                bottomBar = {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        items = navItems,
                        onNavigate = { route -> currentRoute = route }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentRoute) {
                        "home" -> HomeScreen(
                            userName = userName,
                            tutorId = tutorId,
                            context = context,
                            onLogout = onLogout,
                            onNavigateToTopSubjects = { showTopSubjects = true }
                        )
                        "courses" -> CoursesScreen(
                            tutorId = tutorId,
                            context = context,
                            viewModel = remember { CoursesViewModel(context, DatabaseHelper(context)) }
                        )
                        "availability" -> AvailabilityScreen(
                            context = context,
                            tutorId = tutorId,
                            onNavigateToEdit = { item ->
                                editingItem = item
                                currentRoute = "availability_edit"
                            }
                        )
                        "profile" -> ProfileScreen(
                            viewModel = remember { ProfileViewModel(context) },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}
