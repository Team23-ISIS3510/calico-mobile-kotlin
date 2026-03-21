package com.calico.tutor.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.calico.tutor.domain.model.BugReportData

@Composable
fun BugReportDialog(
    bugReportData: BugReportData,
    onDismiss: () -> Unit,
    onConfirm: (BugReportData) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "Bug Report",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Report a Bug?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Would you like to send a bug report to the Calico team? " +
                        "Your device information and app details will be included to help us fix the issue.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(bugReportData) }
            ) {
                Text("Send Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
