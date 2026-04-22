package com.user.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.user.ClawMobileApplication
import com.user.data.PermissionRequest
import com.user.service.OrchestratorApiClient
import kotlinx.coroutines.launch

class PermissionViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ClawMobileApplication
    private val client by lazy {
        OrchestratorApiClient(app.prefsManager, app.prefsManager.gatewayToken)
    }

    private val _pendingPermissions = MutableLiveData<List<PermissionRequest>>(emptyList())
    val pendingPermissions: LiveData<List<PermissionRequest>> = _pendingPermissions

    private val _resolvedPermissions = MutableLiveData<List<PermissionRequest>>(emptyList())
    val resolvedPermissions: LiveData<List<PermissionRequest>> = _resolvedPermissions

    private val _pendingCount = MutableLiveData(0)
    val pendingCount: LiveData<Int> = _pendingCount

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadPermissions() {
        viewModelScope.launch {
            client.listPermissions().onSuccess { response ->
                val normalized = response.permissions.map { permission ->
                    permission.copy(status = normalizeStatus(permission.status))
                }
                val pending = normalized.filter { it.status == "pending" }
                val resolved = normalized.filter { it.status != "pending" }
                _pendingPermissions.postValue(pending)
                _resolvedPermissions.postValue(resolved)
                _pendingCount.postValue(pending.size)
                _error.postValue(null)
            }.onFailure {
                _error.postValue(it.message)
            }
        }
    }

    fun approve(requestId: Int, autoApproveSame: Boolean = false) {
        val current = _pendingPermissions.value.orEmpty().toMutableList()
        val item = current.firstOrNull { it.id == requestId } ?: return
        current.remove(item)
        _pendingPermissions.postValue(current)
        _pendingCount.postValue(current.size)
        _resolvedPermissions.postValue(_resolvedPermissions.value.orEmpty() + item.copy(status = "approved"))

        viewModelScope.launch {
            client.approvePermission(requestId, autoApproveSame).onFailure {
                // Rollback optimistic update
                val restoredPending = _pendingPermissions.value.orEmpty() + item
                _pendingPermissions.postValue(restoredPending)
                _pendingCount.postValue(restoredPending.size)
                _error.postValue(it.message)
            }.onSuccess {
                _error.postValue(null)
            }
        }
    }

    fun reject(requestId: Int) {
        val current = _pendingPermissions.value.orEmpty().toMutableList()
        val item = current.firstOrNull { it.id == requestId } ?: return
        current.remove(item)
        _pendingPermissions.postValue(current)
        _pendingCount.postValue(current.size)
        _resolvedPermissions.postValue(_resolvedPermissions.value.orEmpty() + item.copy(status = "rejected"))

        viewModelScope.launch {
            client.rejectPermission(requestId).onFailure {
                val restoredPending = _pendingPermissions.value.orEmpty() + item
                _pendingPermissions.postValue(restoredPending)
                _pendingCount.postValue(restoredPending.size)
                _error.postValue(it.message)
            }.onSuccess {
                _error.postValue(null)
            }
        }
    }

    private fun normalizeStatus(status: String): String {
        return when (status.lowercase()) {
            "denied" -> "rejected"
            else -> status.lowercase()
        }
    }
}
