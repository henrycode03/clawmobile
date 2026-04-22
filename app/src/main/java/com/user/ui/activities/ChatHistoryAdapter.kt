package com.user.ui.activities

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.user.data.ChatSession
import com.user.databinding.ItemChatHistoryBinding

class ChatHistoryAdapter(
    private val onOpen: (ChatSession) -> Unit,
    private val onDelete: (ChatSession) -> Unit,
) : ListAdapter<ChatSession, ChatHistoryAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemChatHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(session: ChatSession) {
            binding.historyTitle.text = session.title
                .takeUnless { it.isBlank() || it == "New Chat" }
                ?: "Chat ${session.sessionId.take(8)}"
            binding.historySubtitle.text = DateUtils.getRelativeTimeSpanString(
                session.lastMessageAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            binding.historySessionId.text = "ID ${session.sessionId.take(8)}"
            binding.root.setOnClickListener { onOpen(session) }
            binding.deleteButton.setOnClickListener { onDelete(session) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<ChatSession>() {
            override fun areItemsTheSame(oldItem: ChatSession, newItem: ChatSession): Boolean =
                oldItem.sessionId == newItem.sessionId

            override fun areContentsTheSame(oldItem: ChatSession, newItem: ChatSession): Boolean =
                oldItem == newItem
        }
    }
}
