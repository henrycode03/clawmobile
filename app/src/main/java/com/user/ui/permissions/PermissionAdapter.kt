package com.user.ui.permissions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.user.data.PermissionRequest
import com.user.databinding.ItemPermissionBinding

class PermissionAdapter(
    private val onApprove: (Int) -> Unit,
    private val onReject: (Int) -> Unit,
    private val readOnly: Boolean
) : ListAdapter<PermissionRequest, PermissionAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPermissionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPermissionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PermissionRequest) {
            binding.permOperationType.text = item.operationType.replace("_", " ").replaceFirstChar { it.uppercase() }
            binding.permDescription.text = item.description
            val sessionLabel = item.sessionName.takeIf { it.isNotBlank() }?.let { "Session: $it" }
            binding.permSessionName.text = sessionLabel.orEmpty()
            binding.permSessionName.visibility = if (sessionLabel != null) View.VISIBLE else View.GONE
            val expiryLabel = item.expiresAt?.take(16)
            binding.permExpiry.text = expiryLabel.orEmpty()
            binding.permExpiry.visibility = if (expiryLabel != null) View.VISIBLE else View.GONE
            binding.permStatusBadge.setStatus(item.status)

            if (readOnly) {
                binding.permActionRow.visibility = View.GONE
            } else {
                binding.permActionRow.visibility = View.VISIBLE
                binding.btnApprove.setOnClickListener { onApprove(item.id) }
                binding.btnReject.setOnClickListener { onReject(item.id) }
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PermissionRequest>() {
            override fun areItemsTheSame(a: PermissionRequest, b: PermissionRequest) = a.id == b.id
            override fun areContentsTheSame(a: PermissionRequest, b: PermissionRequest) = a == b
        }
    }
}
