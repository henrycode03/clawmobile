package com.user.ui.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.user.data.OrchestTask
import com.user.databinding.ItemProjectTaskBinding

class ProjectTaskAdapter(
    private val onTaskClick: (OrchestTask) -> Unit
) : ListAdapter<OrchestTask, ProjectTaskAdapter.ProjectTaskViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectTaskViewHolder {
        val binding = ItemProjectTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProjectTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectTaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProjectTaskViewHolder(
        private val binding: ItemProjectTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: OrchestTask) {
            binding.taskTitle.text = task.title
            binding.taskDescription.text = task.description?.takeIf { it.isNotBlank() } ?: "No description"
            binding.taskStatus.text = task.status.replace('_', ' ').uppercase()

            binding.taskMeta.text = when (task.status.lowercase()) {
                "failed" -> "Needs attention"
                "running", "executing", "in_progress" -> "Currently executing"
                "pending" -> "Waiting to run"
                "done", "completed", "success" -> "Completed"
                else -> "Status available"
            }

            binding.root.setOnClickListener { onTaskClick(task) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<OrchestTask>() {
        override fun areItemsTheSame(oldItem: OrchestTask, newItem: OrchestTask): Boolean {
            return oldItem.taskId == newItem.taskId
        }

        override fun areContentsTheSame(oldItem: OrchestTask, newItem: OrchestTask): Boolean {
            return oldItem == newItem
        }
    }
}
