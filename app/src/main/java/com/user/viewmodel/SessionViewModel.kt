package com.user.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.user.ClawMobileApplication
import com.user.data.ChatSession
import com.user.data.MobileSessionActionResponse
import com.user.repository.ChatRepository
import com.user.service.LogEntry
import com.user.service.OrchestratorApiClient
import com.user.service.WebSocketManager
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ClawMobileApplication
    private val repository = app.repository

    private val orchestratorClient by lazy {
        OrchestratorApiClient(app.prefsManager, app.prefsManager.gatewayToken)
    }
    val webSocketManager by lazy { WebSocketManager(app.prefsManager) }
    val logStream: SharedFlow<LogEntry> get() = webSocketManager.logStream

    private val _sessionAction = MutableLiveData<Result<MobileSessionActionResponse>>()
    val sessionAction: LiveData<Result<MobileSessionActionResponse>> = _sessionAction

    fun startSession(projectId: Int, name: String, taskId: Int? = null) {
        viewModelScope.launch {
            val result = orchestratorClient.startSession(projectId, name, taskId)
            _sessionAction.postValue(result)
        }
    }

    fun pauseSession(sessionId: String) {
        viewModelScope.launch {
            val result = orchestratorClient.pauseSession(sessionId)
            _sessionAction.postValue(result)
        }
    }

    fun resumeSession(sessionId: String) {
        viewModelScope.launch {
            val result = orchestratorClient.resumeSession(sessionId)
            _sessionAction.postValue(result)
        }
    }

    private val _sessions = MutableLiveData<List<ChatSession>>(emptyList())
    val sessions: LiveData<List<ChatSession>> = _sessions

    private var sortNewestFirst = true
    private var allSessions = listOf<ChatSession>()
    private var query: String = ""

    init {
        observeSessions()
    }

    private fun observeSessions() {
        viewModelScope.launch {
            repository.getSessions().collectLatest { list ->
                allSessions = list
                applySort()
            }
        }
    }

    fun sortNewest() {
        sortNewestFirst = true
        applySort()
    }

    fun sortOldest() {
        sortNewestFirst = false
        applySort()
    }

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            repository.deleteSession(session.sessionId)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            allSessions.forEach { repository.deleteSession(it.sessionId) }
        }
    }

    fun setQuery(value: String) {
        query = value.trim()
        applySort()
    }

    fun refresh() {
        applySort()
    }

    private fun applySort() {
        val filtered = if (query.isBlank()) {
            allSessions
        } else {
            val normalizedQuery = query.lowercase()
            allSessions.filter { session ->
                session.title.lowercase().contains(normalizedQuery) ||
                        session.sessionId.lowercase().contains(normalizedQuery)
            }
        }

        val sorted = if (sortNewestFirst) {
            filtered.sortedByDescending { it.lastMessageAt }
        } else {
            filtered.sortedBy { it.lastMessageAt }
        }
        _sessions.postValue(sorted)
    }
}


