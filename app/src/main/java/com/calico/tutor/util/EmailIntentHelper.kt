package com.calico.tutor.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.calico.tutor.domain.model.BugReportData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EmailIntentHelper {

    private const val BUG_REPORT_EMAIL = "calico.tutorias@gmail.com"

    fun sendBugReport(context: Context, bugReportData: BugReportData) {
        val subject = buildEmailSubject(bugReportData)
        val body = buildEmailBody(bugReportData)

        // Build mailto URI with subject and body as query parameters
        // This ensures Gmail and all email clients properly pre-fill the content
        val uriText = buildString {
            append("mailto:$BUG_REPORT_EMAIL")
            append("?subject=${Uri.encode(subject)}")
            append("&body=${Uri.encode(body)}")
        }

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(uriText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(emailIntent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to open email app: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun buildEmailSubject(data: BugReportData): String {
        return "Bug Report - ${data.appVersion} - ${data.timestamp}"
    }

    private fun buildEmailBody(data: BugReportData): String {
        return buildString {
            appendLine("Bug Report for Calico Tutor")
            appendLine("=" .repeat(50))
            appendLine()
            appendLine("DEVICE INFORMATION:")
            appendLine("Manufacturer: ${data.deviceManufacturer}")
            appendLine("Model: ${data.deviceModel}")
            appendLine("Android Version: ${data.androidVersion}")
            appendLine()
            appendLine("APP INFORMATION:")
            appendLine("Version: ${data.appVersion}")
            appendLine("Version Code: ${data.appVersionCode}")
            appendLine()
            appendLine("REPORT DETAILS:")
            appendLine("Timestamp: ${data.timestamp}")
            appendLine("Current Screen: ${data.currentScreen}")
            if (data.userEmail != null) {
                appendLine("User Email: ${data.userEmail}")
            } else {
                appendLine("User: Not logged in")
            }
            appendLine()
            appendLine("=" .repeat(50))
            appendLine()
            appendLine("DESCRIPTION OF THE BUG:")
            appendLine("(Please describe what happened, what you expected to happen, and steps to reproduce)")
            appendLine()
            appendLine()
            appendLine()
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
