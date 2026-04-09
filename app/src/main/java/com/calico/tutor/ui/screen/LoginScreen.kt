package com.calico.tutor.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calico.tutor.R
import com.calico.tutor.ui.theme.BeigeButton
import com.calico.tutor.ui.theme.BrownText
import com.calico.tutor.ui.theme.CreamBackground
import com.calico.tutor.ui.theme.CreamInput
import com.calico.tutor.ui.theme.MainBackground
import com.calico.tutor.ui.theme.PrimaryOrange
import com.calico.tutor.ui.theme.TextColorBlack
import com.calico.tutor.util.EmailValidator

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
    
    // Validation state - show error only if email is non-empty but invalid
    val emailError = remember(email) {
        if (email.isNotBlank() && !EmailValidator.isValidEmail(email)) {
            "Invalid email format"
        } else {
            null
        }
    }
    
    // Determine error type: validation vs authentication
    val validationError = emailError
    val authenticationError = if (validationError == null && errorMessage != null) errorMessage else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Calico Logo (text-based placeholder)
            Image(
                painter = painterResource(id = R.drawable.calico_logo),
                contentDescription = "Calico Logo",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1.5f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Email/Username Input
            OutlinedTextField(
                value = email,
                onValueChange = { setEmail(it) },
                placeholder = {
                    Text(
                        "Username or Email",
                        color = BrownText,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CreamInput,
                    unfocusedContainerColor = CreamInput,
                    disabledContainerColor = CreamInput,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextColorBlack,
                    unfocusedTextColor = TextColorBlack
                ),
                singleLine = true,
                isError = validationError != null
            )
            
            // Email validation error in red
            if (validationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    validationError,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { setPassword(it) },
                placeholder = {
                    Text(
                        "Password",
                        color = BrownText,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CreamInput,
                    unfocusedContainerColor = CreamInput,
                    disabledContainerColor = CreamInput,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextColorBlack,
                    unfocusedTextColor = TextColorBlack
                ),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            // Authentication error message
            if (authenticationError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    authenticationError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Log In Button
            Button(
                onClick = { onLoginClick(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOrange,
                    contentColor = TextColorBlack
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty() && validationError == null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TextColorBlack,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Log In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Register Button
            Button(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BeigeButton,
                    contentColor = TextColorBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Register",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Forgot Password Link
            TextButton(onClick = { /* TODO: Implement forgot password */ }) {
                Text(
                    "Forgot Password?",
                    color = BrownText,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
