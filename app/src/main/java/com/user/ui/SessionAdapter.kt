package com.user.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.user.data.MobileSessionListItem
import com.user.databinding.ItemSessionBinding
import com.user.databinding.ItemSessionHeaderBinding
import com.user.ui.TimeFormatUtils

class SessionAdapter(
    private val onClick: (MobileSessionListItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class SessionListItem {
        data class Header(val title: String) : SessionListItem()
        data class Row(val session: MobileSessionListItem) : SessionListItem()
    }

    private val items = mutableListOf<SessionListItem>()

    fun submitSessions(sessions: List<MobileSessionListItem>) {
        items.clear()
        sessions
            .groupBy { it.status.lowercase() }
            .forEach { (status, group) ->
                items += SessionListItem.Header(status.replaceFirstChar { it.uppercase() })
                group.forEach { session -> items += SessionListItem.Row(session) }
            }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is SessionListItem.Header -> VIEW_TYPE_HEADER
        is SessionListItem.Row -> VIEW_TYPE_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                ItemSessionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> SessionViewHolder(
                ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SessionListItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is SessionListItem.Row -> (holder as SessionViewHolder).bind(item.session)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderViewHolder(
        private val binding: ItemSessionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.sessionHeaderTitle.text = title
        }
    }

    inner class SessionViewHolder(
        private val binding: ItemSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(session: MobileSessionListItem) {
            binding.sessionTitle.text = session.name.ifBlank { "Session #${session.id}" }
            binding.sessionTime.text = session.startedAt
                ?.let { TimeFormatUtils.formatApiTimestamp(it) ?: it }
                ?: "—"
            binding.sessionStatusBadge.setStatus(session.status)
            binding.pinButton.visibility = android.view.View.GONE
            binding.deleteButton.visibility = android.view.View.GONE
            binding.root.setOnClickListener { onClick(session) }
        }
    }

    private companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ROW = 1
    }
}
