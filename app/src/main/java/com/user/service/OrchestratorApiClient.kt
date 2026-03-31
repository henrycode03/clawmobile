package com.user.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.user.data.DashboardResponse
import com.user.data.DashboardSummary
import com.user.data.OrchestTask
import com.user.data.OrchestratorApiResponse
import com.user.data.PrefsManager
import com.user.data.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Orchestrator API client for fetching project and task statistics
 */
class OrchestratorApiClient(
    private val prefs: PrefsManager,
    private val gatewayToken: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private fun getBaseUrl(): String {
        return prefs.orchestratorServerUrl.removeSuffix("/")
    }

    private fun getHeaders(): Map<String, String> {
        return mapOf(
            "Content-Type" to "application/json",
            "X-OpenClaw-API-Key" to prefs.orchestratorApiKey,
            "Authorization" to "Bearer $gatewayToken"
        )
    }

    /**
     * Fetch dashboard summary (project count, task stats, session stats)
     */
    suspend fun getDashboardSummary(): Result<DashboardSummary> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/api/v1/mobile/dashboard"
            val request = Request.Builder()
                .url(url)
                .headers(okhttp3.Headers.headersOf(*buildHeadersArray()))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Dashboard API failed: ${response.code} ${response.message}")
                    )
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response")

                // Parse the response
                val apiResponse = gson.fromJson(responseBody, DashboardResponse::class.java)

                if (apiResponse.summary != null) {
                    Result.success(apiResponse.summary)
                } else {
                    Result.failure(Exception("No summary data in response"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch all projects
     */
    suspend fun getProjects(): Result<List<Project>> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/api/v1/mobile/projects"
            val request = Request.Builder()
                .url(url)
                .headers(okhttp3.Headers.headersOf(*buildHeadersArray()))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Projects API failed: ${response.code} ${response.message}")
                    )
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response")
                val type = object : TypeToken<OrchestratorApiResponse<List<Project>>>() {}.type
                val apiResponse = gson.fromJson<OrchestratorApiResponse<List<Project>>>(responseBody, type)

                if (apiResponse.success && apiResponse.data != null) {
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.error ?: "Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch tasks for a specific project
     */
    suspend fun getProjectTasks(projectId: String): Result<List<OrchestTask>> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/api/v1/mobile/projects/${projectId}/tasks"
            val request = Request.Builder()
                .url(url)
                .headers(okhttp3.Headers.headersOf(*buildHeadersArray()))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Tasks API failed: ${response.code} ${response.message}")
                    )
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response")
                val type = object : TypeToken<OrchestratorApiResponse<List<OrchestTask>>>() {}.type
                val apiResponse = gson.fromJson<OrchestratorApiResponse<List<OrchestTask>>>(responseBody, type)

                if (apiResponse.success && apiResponse.data != null) {
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.error ?: "Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch tasks filtered by status
     */
    suspend fun getTasksByStatus(projectId: String, status: String): Result<List<OrchestTask>> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/api/v1/mobile/projects/${projectId}/tasks?status=$status"
            val request = Request.Builder()
                .url(url)
                .headers(okhttp3.Headers.headersOf(*buildHeadersArray()))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Tasks API failed: ${response.code} ${response.message}")
                    )
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response")
                val type = object : TypeToken<OrchestratorApiResponse<List<OrchestTask>>>() {}.type
                val apiResponse = gson.fromJson<OrchestratorApiResponse<List<OrchestTask>>>(responseBody, type)

                if (apiResponse.success && apiResponse.data != null) {
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.error ?: "Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildHeadersArray(): Array<String> {
        val headers = getHeaders()
        return headers.flatMap { (key, value) -> listOf(key, value) }.toTypedArray()
    }

    /**
     * Test connection to Orchestrator API
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/api/v1/mobile/dashboard"
            val request = Request.Builder()
                .url(url)
                .headers(okhttp3.Headers.headersOf(*buildHeadersArray()))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
