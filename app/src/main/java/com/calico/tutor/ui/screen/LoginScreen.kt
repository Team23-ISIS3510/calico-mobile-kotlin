package com.calico.tutor.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calico.tutor.R
import com.calico.tutor.ui.component.AuthButton
import com.calico.tutor.ui.component.AuthTextField
import com.calico.tutor.ui.theme.PrimaryYellow

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    isRetryable: Boolean = false,
    onRetry: (() -> Unit)? = null
) {
    val (email, setEmail) = remember { mutableStateOf("") }
    val (password, setPassword) = remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = PrimaryYellow,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "CT",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Welcome Back",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Sign in to continue learning",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            AuthTextField(
                value = email,
                onValueChange = { setEmail(it) },
                label = "Email",
                isEmail = true,
                modifier = Modifier.fillMaxWidth(),
                isError = email.isNotEmpty() && !email.contains("@")
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = password,
                onValueChange = { setPassword(it) },
                label = "Password",
                isPassword = true,
                modifier = Modifier.fillMaxWidth(),
                isError = password.isNotEmpty() && password.length < 6
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (isRetryable && onRetry != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = onRetry,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Retry", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onLoginClick(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryYellow,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onRegisterClick) {
                    Text(
                        "Sign Up",
                        color = PrimaryYellow,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
