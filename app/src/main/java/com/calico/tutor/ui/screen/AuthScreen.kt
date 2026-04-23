package com.calico.tutor.ui.screen

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.ui.component.BottomNavBar
import com.calico.tutor.ui.component.NavBarItem
import com.calico.tutor.ui.theme.WhiteBase
import com.calico.tutor.ui.viewmodel.AuthState
import com.calico.tutor.ui.viewmodel.AuthViewModel
import com.calico.tutor.ui.viewmodel.CoursesViewModel
import com.calico.tutor.util.JwtUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person

@Composable
fun AuthScreen(viewModel: AuthViewModel, context: Context) {
    val authState = viewModel.authState.collectAsState()
    val (showLogin, setShowLogin) = remember { mutableStateOf(true) }
    val (currentScreen, setCurrentScreen) = remember { mutableStateOf("home") }

    // Si el usuario está autenticado, mostrar HomeScreen
    when (val state = authState.value) {
        is AuthState.Success -> {
            // Usuario autenticado - mostrar Home Page
            val tokenManager = ServiceLocator.provideTokenManager(context)
            val email = tokenManager.getEmail() ?: state.token.idToken
            
            // Extraer Firebase UID del JWT token
            val firebaseUid = try {
                val idToken = state.token.idToken ?: tokenManager.getIdToken() ?: ""
                JwtUtils.extractFirebaseUid(idToken).also { uid ->
                    if (uid != null) {
                        // Guardar el Firebase UID en TokenManager para uso futuro
                        tokenManager.saveFirebaseUid(uid)
                        Log.d("AuthScreen", "✅ Firebase UID guardado: $uid")
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthScreen", "❌ Error al extraer Firebase UID: ${e.message}")
                null
            } ?: tokenManager.getFirebaseUid() // Fallback: intentar obtener del almacenamiento
            
            val userName = email
                .substringBefore("@")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            
            // Bottom navigation items
            val navItems = listOf(
                NavBarItem(label = "Home", icon = Icons.Filled.Home, route = "home"),
                NavBarItem(label = "Courses", icon = Icons.Filled.Home, route = "courses"),
                NavBarItem(label = "Statistics", icon = Icons.Filled.Home, route = "statistics"),
                NavBarItem(label = "Profile", icon = Icons.Filled.Person, route = "profile")
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WhiteBase)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Screen content (takes up available space)
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        when (currentScreen) {
                            "home" -> {
                                HomeScreen(
                                    userName = userName,
                                    tutorId = firebaseUid ?: email,
                                    context = context,
                                    onLogout = {
                                        viewModel.resetState()
                                    },
                                    onNavigateToTopSubjects = {
                                        setCurrentScreen("topSubjects")
                                    }
                                )
                            }
                            "topSubjects" -> {
                                TopSubjectsScreen(
                                    context = context,
                                    onNavigateBack = { setCurrentScreen("home") }
                                )
                            }
                            "courses" -> {
                                val dbHelper = remember { DatabaseHelper(context) }
                                val coursesViewModel = remember { CoursesViewModel(context, dbHelper) }
                                CoursesScreen(
                                    tutorId = firebaseUid ?: email,
                                    context = context,
                                    viewModel = coursesViewModel
                                )
                            }
                            "statistics" -> {
                                StatisticsScreen()
                            }
                            "profile" -> {
                                ProfileScreen(
                                    userName = userName,
                                    userEmail = email ?: "",
                                    onLogout = {
                                        viewModel.resetState()
                                    }
                                )
                            }
                            else -> {
                                HomeScreen(
                                    userName = userName,
                                    tutorId = firebaseUid ?: email,
                                    context = context,
                                    onLogout = {
                                        viewModel.resetState()
                                    },
                                    onNavigateToTopSubjects = {
                                        setCurrentScreen("topSubjects")
                                    }
                                )
                            }
                        }
                    }
                    
                    // Bottom Navigation Bar
                    BottomNavBar(
                        currentRoute = currentScreen,
                        items = navItems,
                        onNavigate = { route ->
                            if (route != "topSubjects") { // Don't navigate to topSubjects from navbar
                                setCurrentScreen(route)
                            }
                        }
                    )
                }
            }
        }
        else -> {
            // Mostrar Login o Register según el estado
            if (showLogin) {
                val errorState = authState.value as? AuthState.Error
                LoginScreen(
                    onLoginClick = { email, password ->
                        viewModel.login(email, password)
                    },
                    onRegisterClick = { setShowLogin(false) },
                    isLoading = authState.value is AuthState.Loading,
                    errorMessage = errorState?.message,
                    isRetryable = errorState?.retryable == true,
                    onRetry = { viewModel.retryFailedOperation() }
                )
            } else {
                val errorState = authState.value as? AuthState.Error
                RegisterScreen(
                    onRegisterClick = { email, password, name, phone, isTutor ->
                        viewModel.register(email, password, name, phone, isTutor)
                    },
                    onBackClick = {
                        setShowLogin(true)
                        viewModel.resetState()
                    },
                    isLoading = authState.value is AuthState.Loading,
                    errorMessage = errorState?.message,
                    isRetryable = errorState?.retryable == true,
                    onRetry = { viewModel.retryFailedOperation() }
                )
            }
        }
    }
}
