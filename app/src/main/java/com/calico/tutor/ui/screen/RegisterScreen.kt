package com.calico.tutor.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.KeyboardType
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
            .background(MainBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextColorBlack
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calico Logo
            Image(
                painter = painterResource(id = R.drawable.calico_logo),
                contentDescription = "Calico Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = { setEmail(it) },
                placeholder = {
                    Text(
                        "Email",
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
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { setName(it) },
                placeholder = {
                    Text(
                        "Full Name",
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
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone Input
            OutlinedTextField(
                value = phone,
                onValueChange = { setPhone(it) },
                placeholder = {
                    Text(
                        "Phone Number",
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
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tutor Checkbox
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
                    color = Color.Black
                )
            }

            val (validationError, setValidationError) = remember { mutableStateOf<String?>(null) }
            val combinedErrorMessage = validationError ?: errorMessage

            if (combinedErrorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    combinedErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Register Button
            Button(
                onClick = {
                    // Basic client-side validation before attempting registration
                    val trimmedEmail = email.trim()
                    val trimmedName = name.trim()
                    val trimmedPhone = phone.trim()

                    val localError = when {
                        trimmedEmail.isEmpty() ||
                            !trimmedEmail.contains("@") ||
                            !trimmedEmail.contains(".") ->
                            "Please enter a valid email address."
                        password.length < 6 ->
                            "Password must be at least 6 characters long."
                        trimmedName.length < 2 ->
                            "Please enter your full name."
                        trimmedPhone.length < 10 ->
                            "Please enter a valid phone number."
                        else -> null
                    }

                    if (localError != null) {
                        setValidationError(localError)
                    } else {
                        setValidationError(null)
                        onRegisterClick(trimmedEmail, password, trimmedName, trimmedPhone, isTutor)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOrange,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty() && 
                         name.isNotEmpty() && phone.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Register",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to Login Button
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BeigeButton,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Back to Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
