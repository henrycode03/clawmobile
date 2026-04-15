package com.user.ui.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.user.R
import com.user.data.Project

/**
 * Adapter for displaying project progress cards with real task counts from Orchestrator API
 */
class ProjectProgressAdapter(
    private val onProjectClickListener: ((Project) -> Unit)? = null,
    private val onProjectPinClickListener: ((Project) -> Unit)? = null,
    private val isProjectPinned: ((Project) -> Boolean)? = null,
    private val onProjectLongClickListener: ((Project) -> Unit)? = null,
) : ListAdapter<Project, ProjectProgressAdapter.ProjectViewHolder>(ProjectDiffCallback()) {

    data class ProjectStats(
        val running: Int = 0,
        val pending: Int = 0,
        val completed: Int = 0,
        val total: Int = 0,
        val failed: Int = 0,
        val activeSessions: Int = 0
    )

    private val statsByProjectId = mutableMapOf<String, ProjectStats>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project_progress, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = getItem(position)
        holder.bind(project, statsByProjectId[project.getProjectId()])
    }

    fun updateProjectStats(projectId: String, stats: ProjectStats) {
        statsByProjectId[projectId] = stats
        val index = currentList.indexOfFirst { it.getProjectId() == projectId }
        if (index >= 0) {
            notifyItemChanged(index)
        }
    }

    inner class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val projectName: TextView = itemView.findViewById(R.id.projectName)
        private val projectStatusSummary: TextView = itemView.findViewById(R.id.projectStatusSummary)
        private val projectRunningCount: TextView = itemView.findViewById(R.id.projectRunningCount)
        private val projectPendingCount: TextView = itemView.findViewById(R.id.projectPendingCount)
        private val projectCompletedCount: TextView = itemView.findViewById(R.id.projectCompletedCount)
        private val projectFailedCount: TextView = itemView.findViewById(R.id.projectFailedCount)
        private val projectProgressBar: ProgressBar = itemView.findViewById(R.id.projectProgressBar)
        private val pinProjectButton: ImageButton = itemView.findViewById(R.id.pinProjectButton)

        fun bind(project: Project, stats: ProjectStats?) {
            projectName.text = project.name

            if (stats != null) {
                updateWithStats(
                    running = stats.running,
                    pending = stats.pending,
                    completed = stats.completed,
                    total = stats.total,
                    failed = stats.failed,
                    activeSessions = stats.activeSessions
                )
            } else {
                showEmptyState()
            }

            // Click handler
            itemView.setOnClickListener {
                onProjectClickListener?.invoke(project)
            }
            itemView.setOnLongClickListener {
                onProjectLongClickListener?.invoke(project)
                true
            }
            pinProjectButton.setOnClickListener {
                onProjectPinClickListener?.invoke(project)
            }
            val pinned = isProjectPinned?.invoke(project) == true
            pinProjectButton.contentDescription = itemView.context.getString(
                if (pinned) R.string.unpin_project else R.string.pin_project
            )
            pinProjectButton.setImageResource(
                if (pinned) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )
            pinProjectButton.setColorFilter(
                ContextCompat.getColor(
                    itemView.context,
                    if (pinned) R.color.status_pending else R.color.timestamp_text
                )
            )
        }

        fun updateWithStats(
            running: Int,
            pending: Int,
            completed: Int,
            total: Int,
            failed: Int,
            activeSessions: Int
        ) {
            // Using a temporary variable to avoid re-appending if called multiple times
            projectName.text = projectName.text.toString().substringBefore(" (")
            projectRunningCount.text = running.toString()
            projectPendingCount.text = pending.toString()
            projectCompletedCount.text = completed.toString()
            projectFailedCount.text = failed.toString()

            projectStatusSummary.text = when {
                failed > 0 -> buildString {
                    append("Needs attention")
                    if (activeSessions > 0) {
                        append(" • $activeSessions live")
                    }
                }
                running > 0 -> buildString {
                    append("Active now")
                    if (activeSessions > 0) {
                        append(" • $activeSessions live")
                    }
                }
                pending > 0 -> "Waiting to run"
                total == 0 -> "No tasks yet"
                else -> "Completed"
            }

            // Calculate completion percentage if we have total tasks
            if (total > 0 && (completed > 0 || failed > 0)) {
                val percent = ((completed.toFloat()) / total * 100).toInt()
                projectProgressBar.progress = percent
                projectProgressBar.visibility = View.VISIBLE
            } else {
                projectProgressBar.visibility = View.GONE
            }
        }

        fun showEmptyState() {
            projectName.text = projectName.text.toString().substringBefore(" (")
            projectRunningCount.text = "0"
            projectPendingCount.text = "0"
            projectCompletedCount.text = "0"
            projectFailedCount.text = "0"
            projectStatusSummary.text = "No execution data yet"
            projectProgressBar.visibility = View.GONE
        }
    }

    class ProjectDiffCallback : DiffUtil.ItemCallback<Project>() {
        override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean {
            return oldItem.getProjectId() == newItem.getProjectId()
        }

        override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean {
            return oldItem == newItem
        }
    }
}