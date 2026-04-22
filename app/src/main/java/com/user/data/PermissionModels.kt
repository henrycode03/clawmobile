package com.user.data

import com.google.gson.annotations.SerializedName

data class PermissionRequest(
    val id: Int = 0,
    @SerializedName("operation_type")
    val operationType: String = "",
    val description: String = "",
    val status: String = "pending",
    @SerializedName("session_id")
    val sessionId: Int = 0,
    @SerializedName("session_name")
    val sessionName: String = "",
    @SerializedName("expires_at")
    val expiresAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("approved_by")
    val approvedBy: String? = null,
    @SerializedName("auto_approve_same")
    val autoApproveSame: Boolean = false
)

data class PermissionListResponse(
    val permissions: List<PermissionRequest> = emptyList(),
    val total: Int = 0
)

data class PermissionActionResponse(
    @SerializedName("request_id")
    val requestId: Int = 0,
    val status: String = "",
    val message: String = ""
)
