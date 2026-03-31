package com.user.data

import com.google.gson.annotations.SerializedName

/**
 * Recent activity item from Orchestrator API
 */
data class RecentActivity(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val timestamp: String
)

/**
 * Dashboard summary from Orchestrator API
 */
data class DashboardSummary(
    val projects: Int,
    val sessions: OrchestratorSessionStats,
    val tasks: OrchestratorTaskStats
)

data class DashboardResponse(
    val summary: DashboardSummary? = null,
    @SerializedName("recentActivity")
    val recentActivity: List<RecentActivity> = emptyList()
)

/**
 * Session statistics from Orchestrator
 */
data class OrchestratorSessionStats(
    val total: Int,
    val running: Int,
    val completed: Int
)

/**
 * Task statistics from Orchestrator
 */
data class OrchestratorTaskStats(
    val total: Int,
    val pending: Int,
    val inProgress: Int,
    val approved: Int,
    val done: Int,
    val failed: Int,
    val completionRate: String
)

/**
 * Project from Orchestrator API
 */
data class Project(
    val projectId: String,
    val name: String,
    val description: String? = null,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Task from Orchestrator API
 */
data class OrchestTask(
    val taskId: String,
    val title: String,
    val description: String? = null,
    val status: String,
    val projectId: String,
    val sessionId: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val priority: Int = 0
)

/**
 * API response wrapper for Orchestrator API
 */
data class OrchestratorApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)
