package com.example.carelink.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.carelink.R

// channel + send helpers from the Notifications lecture.
// kept tiny so it's easy to call from anywhere in the app.
object NotificationsHelper {

    const val CHANNEL_ID = "carelink_general"
    private const val CHANNEL_NAME = "CareLink updates"
    private const val CHANNEL_DESC = "Confirmations and reminders for your welfare requests"

    // call once at app startup
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = CHANNEL_DESC
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    // confirmation when a resident submits a welfare request
    fun sendSubmissionConfirmation(context: Context, requestTitle: String) {
        send(
            context = context,
            id = ID_SUBMISSION,
            title = "Welfare request submitted",
            body = "We have received your request \"${requestTitle.ifBlank { "(untitled)" }}\". A coordinator will review it shortly."
        )
    }

    // gentle reminder if a resident closes the app with an unfinished draft
    fun sendDraftReminder(context: Context) {
        send(
            context = context,
            id = ID_DRAFT,
            title = "Unfinished welfare request",
            body = "You have a draft request that has not been submitted yet. Open CareLink to finish it."
        )
    }

    private fun send(context: Context, id: Int, title: String, body: String) {
        // skip silently when the user has not granted POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private const val ID_SUBMISSION = 1001
    private const val ID_DRAFT = 1002
}