package com.user.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.user.data.ChatMessage
import com.user.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.MessageViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(
        private val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            val time = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(message.timestamp))

            if (message.isUser) {
                // User message — right aligned
                binding.userRow.visibility = View.VISIBLE
                binding.aiRow.visibility   = View.GONE
                binding.userMessageText.text = message.message
                binding.userTimestampText.text = time
            } else {
                // AI message — left aligned with Markdown
                binding.aiRow.visibility   = View.VISIBLE
                binding.userRow.visibility = View.GONE
                binding.messageText.text = MarkdownRenderer.render(
                    binding.root.context, message.message
                )
                binding.timestampText.text = time
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem.message == newItem.message && oldItem.status == newItem.status
    }
}

