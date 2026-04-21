package com.calico.tutor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calico.tutor.ui.screen.AuthScreen
import com.calico.tutor.ui.theme.CalicoTheme
import com.calico.tutor.ui.viewmodel.AuthViewModelFactory
import com.calico.tutor.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalicoTheme {
                CalicoAppRoot(activity = this@MainActivity)
            }
        }
    }
}

@Composable
fun CalicoAppRoot(activity: androidx.activity.ComponentActivity) {
    val context = LocalContext.current.applicationContext
    val factory = AuthViewModelFactory(context)
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    AuthScreen(viewModel = authViewModel, context = context, activity = activity)
}
