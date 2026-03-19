package com.calico.tutor.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isEmail: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null,
    isError: Boolean = false
) {
    val (showPassword, setShowPassword) = remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                isEmail -> KeyboardType.Email
                isPassword -> KeyboardType.Password
                else -> keyboardType
            }
        ),
        visualTransformation = if (isPassword && !showPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = { setShowPassword(!showPassword) }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility"
                    )
                }
            }
        },
        leadingIcon = {
            Icon(
                imageVector = when {
                    isEmail -> Icons.Default.Email
                    isPassword -> Icons.Default.Lock
                    else -> Icons.Default.Lock
                },
                contentDescription = label
            )
        },
        supportingText = if (supportingText != null) {
            { Text(supportingText) }
        } else null,
        isError = isError
    )
}

@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text)
        }
    }
}
