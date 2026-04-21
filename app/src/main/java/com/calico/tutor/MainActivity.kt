package com.calico.tutor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calico.tutor.ui.screen.AuthScreen
import com.calico.tutor.ui.theme.CalicoTheme
import com.calico.tutor.ui.viewmodel.AuthViewModelFactory
import com.calico.tutor.ui.viewmodel.AuthViewModel
import com.calico.tutor.ui.viewmodel.ShakeDetectorViewModel
import com.calico.tutor.ui.viewmodel.ShakeDetectorViewModelFactory
import com.calico.tutor.ui.viewmodel.AuthState
import com.calico.tutor.domain.model.ShakeDetectorState
import com.calico.tutor.ui.component.BugReportDialog
import android.os.Build
import android.Manifest
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var shakeDetectorViewModel: ShakeDetectorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    Log.d("MainActivity", "POST_NOTIFICATIONS permission granted")
                } else {
                    Log.w("MainActivity", "POST_NOTIFICATIONS permission denied")
                }
            }
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Initialize ShakeDetectorViewModel early for lifecycle management
        val factory = ShakeDetectorViewModelFactory(applicationContext)
        shakeDetectorViewModel = factory.create(ShakeDetectorViewModel::class.java)
        
        setContent {
            CalicoTheme {
                CalicoAppRoot(shakeDetectorViewModel = shakeDetectorViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shakeDetectorViewModel.startListening()
    }

    override fun onPause() {
        super.onPause()
        shakeDetectorViewModel.stopListening()
    }
}

@Composable
fun CalicoAppRoot(shakeDetectorViewModel: ShakeDetectorViewModel) {
    val context = LocalContext.current.applicationContext
    val factory = AuthViewModelFactory(context)
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    
    // Observe shake detector state
    val shakeState = shakeDetectorViewModel.state.collectAsState()
    
    // Track auth state to get user email
    val authState = authViewModel.authState.collectAsState()
    
    // Update user email when auth state changes
    when (val state = authState.value) {
        is AuthState.Success -> {
            val tokenManager = com.calico.tutor.di.ServiceLocator.provideTokenManager(context)
            val email = tokenManager.getEmail()
            shakeDetectorViewModel.setUserEmail(email)
            shakeDetectorViewModel.setCurrentScreen("Home")
        }
        else -> {
            shakeDetectorViewModel.setUserEmail(null)
            shakeDetectorViewModel.setCurrentScreen("Auth")
        }
    }
    
    // Show bug report dialog when shake is detected
    when (val state = shakeState.value) {
        is ShakeDetectorState.ShakeDetected -> {
            BugReportDialog(
                bugReportData = state.bugReportData,
                onDismiss = { shakeDetectorViewModel.dismissDialog() },
                onConfirm = { bugReportData ->
                    shakeDetectorViewModel.confirmReport(bugReportData)
                }
            )
        }
        else -> { /* No dialog */ }
    }
    
    AuthScreen(viewModel = authViewModel, context = context)
}
