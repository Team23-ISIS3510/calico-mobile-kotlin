package com.calico.tutor.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calico.tutor.ui.component.AuthTextField
import com.calico.tutor.ui.theme.PrimaryOrange

@Composable
fun RegisterScreen(
    onRegisterClick: (String, String, String, String, Boolean) -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    isRetryable: Boolean = false,
    onRetry: (() -> Unit)? = null
) {
    val (email, setEmail) = remember { mutableStateOf("") }
    val (password, setPassword) = remember { mutableStateOf("") }
    val (name, setName) = remember { mutableStateOf("") }
    val (phone, setPhone) = remember { mutableStateOf("") }
    val (isTutor, setIsTutor) = remember { mutableStateOf(false) }
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = PrimaryOrange,
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
                "Create Account",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Join Calico and start learning",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            AuthTextField(
                value = email,
                onValueChange = { setEmail(it) },
                label = "Email",
                isEmail = true,
                modifier = Modifier.fillMaxWidth(),
                isError = email.isNotEmpty() && !email.contains("@")
            )

            Spacer(modifier = Modifier.height(12.dp))

            AuthTextField(
                value = password,
                onValueChange = { setPassword(it) },
                label = "Password",
                isPassword = true,
                modifier = Modifier.fillMaxWidth(),
                isError = password.isNotEmpty() && password.length < 6,
                supportingText = if (password.isNotEmpty() && password.length < 6) "Min 6 characters" else null
            )

            Spacer(modifier = Modifier.height(12.dp))

            AuthTextField(
                value = name,
                onValueChange = { setName(it) },
                label = "Full Name",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            AuthTextField(
                value = phone,
                onValueChange = { setPhone(it) },
                label = "Phone Number",
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.fillMaxWidth(),
                isError = phone.isNotEmpty() && phone.length < 10
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isTutor,
                    onCheckedChange = { setIsTutor(it) }
                )
                Text(
                    "I'm a tutor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

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
                onClick = { onRegisterClick(email, password, name, phone, isTutor) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOrange,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty() && 
                         name.isNotEmpty() && phone.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onBackClick) {
                    Text(
                        "Sign In",
                        color = PrimaryOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
