package com.user.ui.tasks

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.user.ClawMobileApplication
import com.user.R
import com.user.data.ProjectSessionSummary
import com.user.databinding.ActivityProjectDetailBinding
import com.user.service.OrchestratorApiClient
import com.user.ui.CommandAssist
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
        adapter = ProjectTaskAdapter(
            onTaskClick = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java).apply {
                    putExtra("task_id", task.taskId)
                    putExtra("session_id", "")
                }
                startActivity(intent)
            },
            onTaskLongPress = { task ->
                CommandAssist.showTaskActions(this, task.taskId, task.title)
            }
        )

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
                        "${status.tasks.failed} failed • ${status.tasks.done} done"
                    status.tasks.running > 0 ->
                        "${status.tasks.running} running • ${status.tasks.pending} waiting"
                    status.tasks.total > 0 ->
                        "${status.tasks.done} done • ${status.tasks.pending} waiting • ${status.tasks.total} total"
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
                    "No recent sessions right now"
                } else {
                    buildString {
                        append("${status.activeSessions} active")
                        if (status.recentSessions > 0) {
                            append(" • ${status.recentSessions} recent")
                        }
                        append(" session")
                        if (status.recentSessions != 1 || status.activeSessions != 1) {
                            append("s")
                        }
                    }
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
                        it.sequenceIndex ?: Int.MAX_VALUE
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

        val paddingHorizontal = (16 * resources.displayMetrics.density).toInt()
        val paddingVertical = (12 * resources.displayMetrics.density).toInt()
        val sessionBottomMargin = (8 * resources.displayMetrics.density).toInt()

        sessions.forEach { session ->
            val sessionView = TextView(this).apply {
                val activeMarker = if (session.isActive) " • live" else ""
                val shortName = session.name.takeIf { it.length <= 42 } ?: "${session.name.take(39)}..."
                val statusLabel = session.status.replace('_', ' ').replaceFirstChar { it.uppercase() }
                text = "$shortName\n#${session.id} • $statusLabel$activeMarker"
                textSize = 13f
                setBackgroundResource(R.drawable.bg_overview_panel)
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (session.isActive) R.color.status_running else R.color.text_secondary
                    )
                )
                setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val intent = Intent(this@ProjectDetailActivity, SessionDetailActivity::class.java).apply {
                        putExtra("session_id", session.id.toString())
                        putExtra("session_name", session.name)
                    }
                    startActivity(intent)
                }
                setOnLongClickListener {
                    CommandAssist.showSessionActions(
                        this@ProjectDetailActivity,
                        session.id.toString(),
                        session.name
                    )
                    true
                }
            }
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = sessionBottomMargin
            }
            sessionView.layoutParams = layoutParams
            binding.activeSessionsContainer.addView(sessionView)
        }
    }
}
