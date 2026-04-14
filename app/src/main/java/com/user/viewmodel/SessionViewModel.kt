package com.user.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.user.ClawMobileApplication
import com.user.data.ChatSession
import com.user.repository.ChatRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ClawMobileApplication
    private val repository = app.repository

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


