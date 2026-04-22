package com.user.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.user.data.DashboardSummary
import com.user.data.MobileTaskDetailResponse
import com.user.data.OrchestTask
import com.user.data.RecentActivity
import com.user.data.Task
import com.user.data.TaskStatus
import com.user.repository.ChatRepository
import com.user.service.OrchestratorApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for managing tasks in the mobile application
 */
class TaskViewModel(
    private val repository: ChatRepository,
    private val sessionId: String,
    private val orchestratorClient: OrchestratorApiClient? = null
) : ViewModel() {

    private val _allTasks = MutableLiveData<List<Task>>()
    val allTasks: LiveData<List<Task>> = _allTasks

    private val _pendingTasks = MutableLiveData<List<Task>>()
    val pendingTasks: LiveData<List<Task>> = _pendingTasks

    private val _runningTasks = MutableLiveData<List<Task>>()
    val runningTasks: LiveData<List<Task>> = _runningTasks

    // Orchestrator stats
    private val _orchestratorStats = MutableLiveData<DashboardSummary?>(null)
    val orchestratorStats: LiveData<DashboardSummary?> = _orchestratorStats

    private val _recentActivity = MutableLiveData<List<RecentActivity>>(emptyList())
    val recentActivity: LiveData<List<RecentActivity>> = _recentActivity

    private val _orchestratorWarning = MutableLiveData<String?>(null)
    val orchestratorWarning: LiveData<String?> = _orchestratorWarning

    // Project-specific task stats
    private val _projectTaskStats = mutableMapOf<String, TaskProgress>()

    data class TaskProgress(
        val running: Int = 0,
        val pending: Int = 0,
        val completed: Int = 0
    )

    private val _currentTask = MutableLiveData<Task?>()
    val currentTask: LiveData<Task?> = _currentTask

    private var loadTasksJob: Job? = null
    private var loadOrchestratorStatsJob: Job? = null

    init {
        // If no session is active and Orchestrator is configured, try to load tasks from API
        if (sessionId.isBlank() && orchestratorClient != null) {
            loadOrchestratorTasks()
        } else {
            loadTasks()
        }

        // Always try to load Orchestrator stats for dashboard display
        if (orchestratorClient != null) {
            loadOrchestratorStats()
        }
    }

    /**
     * Load all tasks for the current session
     */
    fun loadTasks() {
        loadTasksJob?.cancel()
        loadTasksJob = CoroutineScope(Dispatchers.Main).launch {
            Log.d("TaskViewModel", "Starting to load tasks for session: $sessionId")
            try {
                val allTasks = repository.getTasksBySession(sessionId).first()
                Log.d("TaskViewModel", "Successfully loaded ${allTasks.size} tasks from database for session: $sessionId")
                _allTasks.value = allTasks
                _pendingTasks.value = allTasks.filter { it.status == TaskStatus.PENDING }
                _runningTasks.value = allTasks.filter { it.status == TaskStatus.IN_PROGRESS }

                // Log task status breakdown for debugging
                val pendingCount = allTasks.count { it.status == TaskStatus.PENDING }
                val inProgressCount = allTasks.count { it.status == TaskStatus.IN_PROGRESS || it.status == TaskStatus.APPROVED }
                val completedCount = allTasks.count { it.status == TaskStatus.COMPLETED }
                Log.d("TaskViewModel", "Task breakdown - Pending: $pendingCount, In Progress: $inProgressCount, Completed: $completedCount")

            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error loading tasks from database for session: $sessionId. This may indicate a database access issue.", e)
                _allTasks.value = emptyList()
                _pendingTasks.value = emptyList()
                _runningTasks.value = emptyList()
            }
        }
    }

    /**
     * Load statistics from Orchestrator API
     *
     * This method handles connection failures gracefully - if the Orchestrator is unavailable,
     * the task list will continue to work using local data. The Orchestrator stats are purely
     * optional enhancements for dashboard display.
     */
    fun loadOrchestratorStats() {
        // Cancel any existing job first
        loadOrchestratorStatsJob?.cancel()

        // Don't attempt to load if no client is configured
        if (orchestratorClient == null) {
            Log.d("TaskViewModel", "No Orchestrator client configured - skipping stats load")
            return
        }

        loadOrchestratorStatsJob = CoroutineScope(Dispatchers.Main).launch {
            // Log the attempt but don't show errors to user
            Log.d("TaskViewModel", "Attempting to load Orchestrator stats...")

            orchestratorClient.getDashboard().onSuccess { dashboard ->
                _orchestratorStats.value = dashboard.summary
                _recentActivity.value = dashboard.recentActivity
                _orchestratorWarning.value = null
                if (dashboard.summary != null) {
                    Log.d("TaskViewModel", "Successfully loaded Orchestrator stats: ${dashboard.summary}")
                } else {
                    Log.w("TaskViewModel", "Orchestrator returned empty stats")
                }
            }.onFailure { error ->
                Log.d("TaskViewModel", "Orchestrator stats unavailable: ${error.message}")
                _orchestratorWarning.value = error.message ?: "Orchestrator stats are currently unavailable."
                _recentActivity.value = emptyList()
            }
        }
    }

    /**
     * Load tasks from Orchestrator API when no session is active.
     * This fetches all tasks across all projects for display in the task list.
     */
    fun loadOrchestratorTasks() {
        // Only load if orchestrator client is configured and no session is active
        if (orchestratorClient == null || sessionId.isNotBlank()) {
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            Log.d("TaskViewModel", "Loading tasks from Orchestrator API (no session active)...")

            val allOrchestTasks = mutableListOf<OrchestTask>()

            // First, get all projects
            orchestratorClient.getProjects().onSuccess { projects ->
                _orchestratorWarning.value = null
                Log.d("TaskViewModel", "Fetched ${projects.size} projects from Orchestrator")

                // Fetch tasks for each project
                projects.forEach { project ->
                    orchestratorClient.getProjectTasks(project.getProjectId()).onSuccess { tasks ->
                        Log.d("TaskViewModel", "Fetched ${tasks.size} tasks for project ${project.name}")
                        allOrchestTasks.addAll(tasks)
                    }.onFailure { error ->
                        Log.w("TaskViewModel", "Failed to fetch tasks for project ${project.name}: ${error.message}")
                    }
                }

                // Convert OrchestTask to local Task model and update UI
                val convertedTasks = allOrchestTasks.map { orchestTask ->
                    orchestTask.toLocalTask(sessionId = sessionId)
                }

                Log.d("TaskViewModel", "Successfully loaded ${convertedTasks.size} tasks from Orchestrator API")
                _allTasks.value = convertedTasks
                _pendingTasks.value = convertedTasks.filter { it.status == TaskStatus.PENDING }
                _runningTasks.value = convertedTasks.filter {
                    it.status == TaskStatus.IN_PROGRESS || it.status == TaskStatus.APPROVED
                }

                // Log task status breakdown for debugging
                val pendingCount = convertedTasks.count { it.status == TaskStatus.PENDING }
                val inProgressCount = convertedTasks.count {
                    it.status == TaskStatus.IN_PROGRESS || it.status == TaskStatus.APPROVED
                }
                val completedCount = convertedTasks.count { it.status == TaskStatus.COMPLETED }
                Log.d("TaskViewModel", "Orchestrator task breakdown - Pending: $pendingCount, In Progress: $inProgressCount, Completed: $completedCount")
            }.onFailure { error ->
                Log.w("TaskViewModel", "Failed to load Orchestrator tasks: ${error.message}")
                _orchestratorWarning.value = error.message ?: "Unable to sync tasks from Orchestrator."
            }
        }
    }

    /**
     * Convert an OrchestTask from the API to a local Task model
     */
    private fun com.user.data.OrchestTask.toLocalTask(sessionId: String): Task {
        return Task(
            taskId = this.taskId,
            sessionId = sessionId,  // Use empty string or project ID as session
            title = this.title,
            description = this.description ?: "",
            status = parseStatus(this.status),
            priority = this.priority,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            metadata = null
        )
    }

    private fun MobileTaskDetailResponse.toLocalTask(): Task {
        return Task(
            taskId = id.toString(),
            sessionId = sessionId?.toString().orEmpty(),
            title = title,
            description = description.orEmpty(),
            status = parseStatus(status),
            priority = priority,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            error = errorMessage,
            metadata = null
        )
    }

    /**
     * Parse Orchestrator task status string to local TaskStatus enum
     */
    private fun parseStatus(status: String): TaskStatus {
        return when (status.lowercase()) {
            "pending" -> TaskStatus.PENDING
            "approved", "waiting_for_approval" -> TaskStatus.APPROVED
            "in_progress", "running", "executing" -> TaskStatus.IN_PROGRESS
            "completed", "done", "success" -> TaskStatus.COMPLETED
            "failed", "failure" -> TaskStatus.FAILED
            "rejected", "rejected_by_user" -> TaskStatus.REJECTED
            "timeout", "timed_out" -> TaskStatus.TIMEOUT
            else -> TaskStatus.PENDING  // Default to pending for unknown statuses
        }
    }

    /**
     * Refresh both local tasks, Orchestrator tasks (if no session), and Orchestrator stats
     */
    fun refreshAll() {
        if (sessionId.isBlank()) {
            // No session active - try to load from Orchestrator first
            loadOrchestratorTasks()
        } else {
            // Session active - load local tasks
            loadTasks()
        }
        loadOrchestratorStats()  // This method now handles null client internally
    }

    /**
     * Approve a pending task
     */
    fun approveTask(taskId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                repository.approveTask(taskId)
                loadTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Reject a pending task
     */
    fun rejectTask(taskId: String, reason: String? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                repository.rejectTask(taskId, reason)
                loadTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Start an approved task
     */
    fun startTask(taskId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                repository.startTask(taskId)
                loadTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Complete a task
     */
    fun completeTask(taskId: String, result: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                repository.completeTask(taskId, result)
                loadTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Mark a task as failed
     */
    fun failTask(taskId: String, error: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                repository.failTask(taskId, error)
                loadTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Get a specific task by ID
     */
    fun getTask(taskId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (orchestratorClient != null) {
                    orchestratorClient.getTaskDetail(taskId).onSuccess { taskDetail ->
                        _currentTask.value = taskDetail.toLocalTask()
                        return@launch
                    }
                }

                val task = runCatching { repository.getTask(taskId) }.getOrNull()
                if (task != null) {
                    _currentTask.value = task
                    return@launch
                }

                val cachedTask = _allTasks.value?.firstOrNull { it.taskId == taskId }
                if (cachedTask != null) {
                    _currentTask.value = cachedTask
                    return@launch
                }

                if (sessionId.isBlank() && orchestratorClient != null) {
                    orchestratorClient.getProjects().onSuccess { projects ->
                        var resolvedTask: Task? = null
                        for (project in projects) {
                            val result = orchestratorClient.getProjectTasks(project.getProjectId())
                            result.onSuccess { tasks ->
                                val match = tasks.firstOrNull { it.taskId == taskId }
                                if (match != null) {
                                    resolvedTask = match.toLocalTask(sessionId = sessionId)
                                }
                            }
                            if (resolvedTask != null) {
                                break
                            }
                        }
                        _currentTask.value = resolvedTask
                    }.onFailure { error ->
                        Log.w("TaskViewModel", "Failed to resolve Orchestrator task $taskId: ${error.message}")
                        _currentTask.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Failed to load task $taskId", e)
            }
        }
    }

    // ── US3: Task Reorder & Workspace Review ─────────────────────

    private val _reorderResult = MutableLiveData<Result<Unit>>()
    val reorderResult: LiveData<Result<Unit>> = _reorderResult

    private val _reviewResult = MutableLiveData<Result<Unit>>()
    val reviewResult: LiveData<Result<Unit>> = _reviewResult

    fun reorderTask(taskId: String, newPosition: Int) {
        val previousList = _allTasks.value.orEmpty()
        val previousIndex = previousList.indexOfFirst { it.taskId == taskId }
        val mutable = previousList.toMutableList()
        if (previousIndex >= 0 && newPosition in mutable.indices) {
            val item = mutable.removeAt(previousIndex)
            mutable.add(newPosition, item)
            _allTasks.value = mutable
        }

        if (orchestratorClient == null) {
            _reorderResult.value = Result.failure(Exception("Orchestrator not configured"))
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            orchestratorClient.updateTaskPosition(taskId, newPosition).onSuccess {
                _reorderResult.value = Result.success(Unit)
            }.onFailure { error ->
                _allTasks.value = previousList
                _reorderResult.value = Result.failure(error)
            }
        }
    }

    fun submitReview(taskId: String, action: String, note: String? = null) {
        if (orchestratorClient == null) {
            _reviewResult.value = Result.failure(Exception("Orchestrator not configured"))
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            orchestratorClient.submitWorkspaceReview(taskId, action, note).onSuccess {
                _reviewResult.value = Result.success(Unit)
                getTask(taskId)
            }.onFailure { error ->
                _reviewResult.value = Result.failure(error)
            }
        }
    }

    /**
     * Get task progress for a specific project
     */
    fun getProjectTasks(projectId: String): LiveData<TaskProgress> {
        val liveData = MutableLiveData<TaskProgress>()

        // If we already have cached stats, return them immediately
        _projectTaskStats[projectId]?.let { stats ->
            liveData.value = stats
        }
        return liveData
    }
}

