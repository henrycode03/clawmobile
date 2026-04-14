package com.user.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.user.R
import com.user.ui.activities.MainActivity

object CommandAssist {

    private data class CommandEntry(
        val label: String,
        val command: String,
    )

    fun showProjectActions(
        activity: AppCompatActivity,
        projectId: String,
        projectName: String,
    ) {
        showActions(
            activity = activity,
            entityLabel = projectName.ifBlank { "Project" },
            entityId = projectId,
            smartCommand = CommandEntry(
                label = activity.getString(R.string.command_action_project_next),
                command = "what should I do next for project $projectId"
            )
        )
    }

    fun showTaskActions(
        activity: AppCompatActivity,
        taskId: String,
        taskTitle: String,
    ) {
        showActions(
            activity = activity,
            entityLabel = taskTitle.ifBlank { "Task" },
            entityId = taskId,
            smartCommand = CommandEntry(
                label = activity.getString(R.string.command_action_task_diagnose),
                command = "diagnose task $taskId"
            )
        )
    }

    fun showSessionActions(
        activity: AppCompatActivity,
        sessionId: String,
        sessionName: String,
    ) {
        showActions(
            activity = activity,
            entityLabel = sessionName.ifBlank { "Session" },
            entityId = sessionId,
            smartCommand = CommandEntry(
                label = activity.getString(R.string.command_action_session_resume),
                command = "resume session $sessionId"
            )
        )
    }

    private fun showActions(
        activity: AppCompatActivity,
        entityLabel: String,
        entityId: String,
        smartCommand: CommandEntry,
    ) {
        val options = listOf(
            activity.getString(R.string.command_action_copy_id),
            activity.getString(R.string.command_action_copy_command, smartCommand.label),
            activity.getString(R.string.command_action_send_to_chat, smartCommand.label),
        )

        AlertDialog.Builder(activity)
            .setTitle(entityLabel)
            .setMessage(activity.getString(R.string.command_action_id_label, entityId))
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        copyToClipboard(activity, entityId)
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.command_action_id_copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    1 -> {
                        copyToClipboard(activity, smartCommand.command)
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.command_action_command_copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    2 -> {
                        activity.startActivity(
                            Intent(activity, MainActivity::class.java).apply {
                                putExtra(MainActivity.EXTRA_PREFILL_MESSAGE, smartCommand.command)
                            }
                        )
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ClawMobile Command", text))
    }
}