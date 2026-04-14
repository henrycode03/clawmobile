package com.user.ui.tasks

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.user.ClawMobileApplication
import com.user.R
import com.user.data.ProjectSessionSummary
import com.user.databinding.ActivityProjectDetailBinding
import com.user.service.OrchestratorApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProjectDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectDetailBinding
    private lateinit var adapter: ProjectTaskAdapter
    private var orchestratorClient: OrchestratorApiClient? = null
    private var projectId: String = ""
    private var projectName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectId = intent.getStringExtra("project_id") ?: ""
        projectName = intent.getStringExtra("project_name") ?: "Project"

        val app = application as ClawMobileApplication
        if (app.prefsManager.isOrchestratorConfigured()) {
            orchestratorClient = OrchestratorApiClient(
                prefs = app.prefsManager,
                gatewayToken = app.prefsManager.gatewayToken
            )
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = projectName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupTaskList()
        loadProjectData()
    }

    private fun setupTaskList() {
        adapter = ProjectTaskAdapter { task ->
            val intent = Intent(this, TaskDetailActivity::class.java).apply {
                putExtra("task_id", task.taskId)
                putExtra("session_id", "")
            }
            startActivity(intent)
        }

        binding.projectTasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.projectTasksRecyclerView.adapter = adapter
    }

    private fun loadProjectData() {
        val client = orchestratorClient ?: run {
            binding.projectStatusSummary.text = "Orchestrator is not configured on this device."
            binding.projectTreeSummary.text = "Orchestrator is not configured on this device."
            binding.projectTreeView.visibility = View.GONE
            binding.projectTasksEmpty.visibility = View.VISIBLE
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            client.getProjectStatus(projectId).onSuccess { status ->
                binding.projectStatusSummary.text = when {
                    status.tasks == null -> "No execution data yet"
                    status.tasks.failed > 0 ->
                        "${status.tasks.failed} failed • ${status.tasks.done} done • ${status.activeSessions} active session(s)"
                    status.tasks.running > 0 ->
                        "${status.tasks.running} running • ${status.tasks.pending} pending • ${status.activeSessions} active session(s)"
                    status.tasks.total > 0 ->
                        "${status.tasks.done} done • ${status.tasks.pending} pending • ${status.tasks.total} total"
                    else -> "No tasks yet"
                }

                val description = status.description?.takeIf { it.isNotBlank() }
                if (description != null) {
                    binding.projectDescription.text = description
                    binding.projectDescription.visibility = View.VISIBLE
                } else {
                    binding.projectDescription.visibility = View.GONE
                }

                val activeSessionSummary = if (status.sessions.isEmpty()) {
                    "No active sessions right now"
                } else {
                    "${status.activeSessions} active session(s)"
                }
                binding.activeSessionsSummary.text = activeSessionSummary
                renderActiveSessions(status.sessions)
            }.onFailure { error ->
                binding.projectStatusSummary.text = error.message ?: "Unable to load project status."
                binding.activeSessionsContainer.removeAllViews()
            }

            client.getProjectTree(projectId).onSuccess { tree ->
                when {
                    !tree.exists -> {
                        binding.projectTreeSummary.text = "Project workspace has not been created yet."
                        binding.projectTreeView.visibility = View.GONE
                    }
                    tree.treeLines.isEmpty() -> {
                        binding.projectTreeSummary.text = "Project workspace is empty."
                        binding.projectTreeView.visibility = View.GONE
                    }
                    else -> {
                        val summary = buildString {
                            append("${tree.totalEntriesShown} item")
                            if (tree.totalEntriesShown != 1) append("s")
                            append(" shown")
                            if (tree.truncated) append(" • trimmed for mobile")
                        }
                        binding.projectTreeSummary.text = summary
                        binding.projectTreeView.text = tree.treeLines.joinToString("\n")
                        binding.projectTreeView.visibility = View.VISIBLE
                    }
                }
            }.onFailure { error ->
                binding.projectTreeSummary.text = error.message ?: "Unable to load file tree."
                binding.projectTreeView.visibility = View.GONE
            }

            client.getProjectTasks(projectId).onSuccess { tasks ->
                val sorted = tasks.sortedWith(
                    compareBy<com.user.data.OrchestTask> {
                        when (it.status.lowercase()) {
                            "failed" -> 0
                            "running", "executing", "in_progress" -> 1
                            "pending" -> 2
                            "done", "completed", "success" -> 3
                            else -> 4
                        }
                    }.thenBy { it.title.lowercase() }
                )
                adapter.submitList(sorted)
                binding.projectTasksEmpty.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure { error ->
                binding.projectTasksEmpty.visibility = View.VISIBLE
                binding.projectTasksEmpty.text = error.message ?: "Unable to load project tasks."
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun renderActiveSessions(sessions: List<ProjectSessionSummary>) {
        binding.activeSessionsContainer.removeAllViews()
        if (sessions.isEmpty()) {
            return
        }

        sessions.forEach { session ->
            val sessionView = TextView(this).apply {
                text = "${session.name} • ${session.status}"
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.status_running))
                setPadding(0, 8, 0, 8)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val intent = Intent(this@ProjectDetailActivity, SessionDetailActivity::class.java).apply {
                        putExtra("session_id", session.id.toString())
                        putExtra("session_name", session.name)
                    }
                    startActivity(intent)
                }
            }
            binding.activeSessionsContainer.addView(sessionView)
        }
    }
}