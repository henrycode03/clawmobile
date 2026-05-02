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
import com.user.ui.tasks.SessionDetailActivity

class InterventionPollService(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val prefs = PrefsManager(appContext)
    private val client = OrchestratorApiClient(prefs, prefs.gatewayToken)
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        ensureNotificationChannel()

        val sessions = client.listSessions(status = "waiting_for_human").getOrNull() ?: return Result.success()
        val lastKnownIds = prefs.lastKnownInterventionSessionIds
        val newSessions = sessions.filter { it.id !in lastKnownIds }

        for (session in newSessions) {
            postInterventionNotification(session.id, session.name)
        }

        prefs.lastKnownInterventionSessionIds = sessions.map { it.id }.toSet()
        return Result.success()
    }

    private fun postInterventionNotification(sessionId: Int, sessionName: String) {
        val openIntent = PendingIntent.getActivity(
            applicationContext,
            sessionId,
            Intent(applicationContext, SessionDetailActivity::class.java).apply {
                putExtra("session_id", sessionId.toString())
                putExtra("session_name", sessionName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(applicationContext.getString(R.string.intervention_notification_title))
            .setContentText("$sessionName: agent waiting for your input")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(sessionId + NOTIFICATION_ID_OFFSET, notification)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.intervention_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "intervention_requests"
        const val WORK_NAME = "intervention_poll"
        private const val NOTIFICATION_ID_OFFSET = 10000
    }
}
