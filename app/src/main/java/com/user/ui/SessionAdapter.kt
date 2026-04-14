package com.user.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.user.data.ChatSession
import com.user.databinding.ItemSessionBinding
import com.user.databinding.ItemSessionHeaderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionAdapter(
    private val onClick: (ChatSession) -> Unit,
    private val onDelete: (ChatSession) -> Unit,
    private val onPin: (ChatSession) -> Unit,
    private val isPinned: (ChatSession) -> Boolean,
    private val onLongPress: ((ChatSession) -> Unit)? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class SessionListItem {
        data class Header(val title: String) : SessionListItem()
        data class Row(val session: ChatSession) : SessionListItem()
    }

    private val items = mutableListOf<SessionListItem>()

    fun submitSessions(sessions: List<ChatSession>) {
        items.clear()
        sessions
            .groupBy { inferProjectBucket(it.title) }
            .toSortedMap(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
            .forEach { (group, groupSessions) ->
                items += SessionListItem.Header(group)
                groupSessions.forEach { session ->
                    items += SessionListItem.Row(session)
                }
            }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is SessionListItem.Header -> VIEW_TYPE_HEADER
        is SessionListItem.Row -> VIEW_TYPE_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemSessionHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemSessionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SessionViewHolder(binding)
            }
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

        fun bind(session: ChatSession) {
            binding.sessionTitle.text = session.title
            binding.sessionTime.text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(Date(session.lastMessageAt))
            binding.root.setOnClickListener { onClick(session) }
            binding.root.setOnLongClickListener {
                onLongPress?.invoke(session)
                true
            }
            binding.deleteButton.setOnClickListener { onDelete(session) }
            binding.pinButton.setOnClickListener { onPin(session) }
            val pinned = isPinned(session)
            binding.pinButton.contentDescription = binding.root.context.getString(
                if (pinned) com.user.R.string.unpin_session else com.user.R.string.pin_session
            )
            binding.pinButton.setImageResource(
                if (pinned) com.user.R.drawable.ic_star_filled else com.user.R.drawable.ic_star_outline
            )
            binding.pinButton.setColorFilter(
                ContextCompat.getColor(
                    binding.root.context,
                    if (pinned) com.user.R.color.status_pending else com.user.R.color.timestamp_text
                )
            )
        }
    }

    private fun inferProjectBucket(rawTitle: String): String {
        val title = rawTitle.trim()
        if (title.isBlank()) return "General"

        val normalized = title
            .removeSuffix("_session")
            .removeSuffix(" session")
            .removeSuffix("_Session")
            .trim()

        if (normalized.equals("New Chat", ignoreCase = true)) return "General"
        if (normalized.startsWith("Task ", ignoreCase = true)) return "Task Runs"

        val bucket = normalized
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

        return bucket.ifBlank { "General" }
    }

    private companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ROW = 1
    }
}
