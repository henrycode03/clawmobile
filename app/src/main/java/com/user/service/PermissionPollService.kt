package com.user.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.user.R
import com.user.data.PrefsManager

class PermissionPollService(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val prefs = PrefsManager(appContext)
    private val client = OrchestratorApiClient(prefs, prefs.gatewayToken)
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        ensureNotificationChannel()

        val result = client.listPermissions(status = "pending")
        val permissions = result.getOrNull()?.permissions ?: return Result.success()

        val lastKnownIds = prefs.lastKnownPermissionIds
        val newPermissions = permissions.filter { it.id !in lastKnownIds }

        for (perm in newPermissions) {
            postPermissionNotification(perm.id, perm.operationType, perm.description)
        }

        prefs.lastKnownPermissionIds = permissions.map { it.id }.toSet()
        return Result.success()
    }

    private fun postPermissionNotification(requestId: Int, operationType: String, description: String) {
        val approveIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestId * 10 + 1,
            Intent(applicationContext, PermissionActionReceiver::class.java).apply {
                action = PermissionActionReceiver.ACTION_APPROVE
                putExtra(PermissionActionReceiver.EXTRA_REQUEST_ID, requestId)
                putExtra(PermissionActionReceiver.EXTRA_NOTIFICATION_ID, requestId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val rejectIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestId * 10 + 2,
            Intent(applicationContext, PermissionActionReceiver::class.java).apply {
                action = PermissionActionReceiver.ACTION_REJECT
                putExtra(PermissionActionReceiver.EXTRA_REQUEST_ID, requestId)
                putExtra(PermissionActionReceiver.EXTRA_NOTIFICATION_ID, requestId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.permissions_notification_title))
            .setContentText("$operationType: $description")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_input_add, applicationContext.getString(R.string.permissions_approve), approveIntent)
            .addAction(android.R.drawable.ic_delete, applicationContext.getString(R.string.permissions_reject), rejectIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(requestId, notification)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.permissions_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "permission_requests"
        const val WORK_NAME = "permission_poll"
    }
}
