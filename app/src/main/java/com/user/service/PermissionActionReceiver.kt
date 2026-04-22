package com.user.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import com.user.data.PrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PermissionActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val requestId = intent.getIntExtra(EXTRA_REQUEST_ID, -1).takeIf { it != -1 } ?: return
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, requestId)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        val prefs = PrefsManager(context)
        val client = OrchestratorApiClient(prefs, prefs.gatewayToken)

        CoroutineScope(Dispatchers.IO).launch {
            when (action) {
                ACTION_APPROVE -> client.approvePermission(requestId)
                ACTION_REJECT -> client.rejectPermission(requestId)
            }
        }
    }

    companion object {
        const val ACTION_APPROVE = "com.user.APPROVE_PERMISSION"
        const val ACTION_REJECT = "com.user.REJECT_PERMISSION"
        const val EXTRA_REQUEST_ID = "request_id"
        const val EXTRA_ACTION = "action"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
