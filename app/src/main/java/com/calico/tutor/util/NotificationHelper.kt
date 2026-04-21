package com.calico.tutor.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import com.calico.tutor.R

/**
 * Manages local notifications for the app
 * Handles channel creation and notification sending
 */
object NotificationHelper {
    
    private const val CHANNEL_ID = "calico_tutor_alerts"
    private const val CHANNEL_NAME = "Session Alerts"
    private const val NOTIFICATION_ID_BASE = 1000
    private const val TAG = "NotificationHelper"
    
    /**
     * Creates notification channel (required for Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for upcoming tutoring sessions"
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }
    
    /**
     * Shows a session alert notification
     * @param context Application context
     * @param studentName Name of the student with upcoming session
     * @param minutesToStart Minutes until session starts
     * @param sessionId Unique session identifier (used to prevent duplicates)
     */
    fun showSessionAlertNotification(
        context: Context,
        studentName: String,
        minutesToStart: Int,
        sessionId: String
    ) {
        // Check if app has notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show notification.")
                return
            }
            Log.d(TAG, "POST_NOTIFICATIONS permission granted")
        }
        
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Generate unique notification ID based on session ID
        // This prevents duplicate notifications for the same session
        val notificationId = (NOTIFICATION_ID_BASE + sessionId.hashCode()).toInt()
        
        Log.d(TAG, "Creating notification: id=$notificationId, studentName=$studentName, minutesToStart=$minutesToStart")
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Upcoming Session Soon!")
            .setContentText("You have a session with $studentName starting in $minutesToStart minutes.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        try {
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "Notification displayed successfully: id=$notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying notification: ${e.message}", e)
        }
    }
}
