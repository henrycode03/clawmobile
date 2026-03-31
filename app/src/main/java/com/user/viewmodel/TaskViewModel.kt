package com.user.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.user.data.DashboardSummary
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

    private val _currentTask = MutableLiveData<Task?>()
    val currentTask: LiveData<Task?> = _currentTask

    private var loadTasksJob: Job? = null
    private var loadOrchestratorStatsJob: Job? = null

    init {
        loadTasks()
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
            try {
                val allTasks = repository.getTasksBySession(sessionId).first()
                _allTasks.value = allTasks
                _pendingTasks.value = allTasks.filter { it.status == TaskStatus.PENDING }
                _runningTasks.value = allTasks.filter { it.status == TaskStatus.IN_PROGRESS }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error loading tasks", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * Load statistics from Orchestrator API
     */
    fun loadOrchestratorStats() {
        loadOrchestratorStatsJob?.cancel()
        loadOrchestratorStatsJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                orchestratorClient?.getDashboardSummary()?.onSuccess { stats ->
                    _orchestratorStats.value = stats
                    Log.d("TaskViewModel", "Loaded Orchestrator stats: $stats")
                }?.onFailure { error ->
                    Log.w("TaskViewModel", "Failed to load Orchestrator stats: ${error.message}")
                    // Don't set error state, just log it - local data still works
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error loading Orchestrator stats", e)
            }
        }
    }

    /**
     * Refresh both local tasks and Orchestrator stats
     */
    fun refreshAll() {
        loadTasks()
        if (orchestratorClient != null) {
            loadOrchestratorStats()
        }
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
                val task = repository.getTask(taskId)
                _currentTask.value = task
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Refresh task statistics
     */
    fun refreshStats() {
        // Implement if needed
    }

    override fun onCleared() {
        super.onCleared()
        loadTasksJob?.cancel()
    }
}

/**
 * Factory class for creating TaskViewModel instances
 */
class TaskViewModelFactory(
    private val repository: ChatRepository,
    private val sessionId: String,
    private val orchestratorClient: com.user.service.OrchestratorApiClient? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(repository, sessionId, orchestratorClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
