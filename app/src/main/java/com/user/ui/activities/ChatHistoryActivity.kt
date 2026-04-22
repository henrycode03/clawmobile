package com.user.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.user.R
import com.user.data.ChatSession
import com.user.databinding.ActivityChatHistoryBinding
import com.user.viewmodel.SessionViewModel

class ChatHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatHistoryBinding
    private val viewModel: SessionViewModel by viewModels()
    private lateinit var adapter: ChatHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.chat_history_title)

        adapter = ChatHistoryAdapter(
            onOpen = ::openSession,
            onDelete = ::confirmDelete,
        )

        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.adapter = adapter

        viewModel.sessions.observe(this) { sessions ->
            adapter.submitList(sessions)
            binding.emptyView.visibility =
                if (sessions.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun openSession(session: ChatSession) {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                putExtra("session_id", session.sessionId)
                putExtra("session_title", session.title)
            }
        )
    }

    private fun confirmDelete(session: ChatSession) {
        val title = session.title
            .takeUnless { it.isBlank() || it == "New Chat" }
            ?: "Chat ${session.sessionId.take(8)}"
        AlertDialog.Builder(this)
            .setTitle(R.string.chat_history_delete_title)
            .setMessage(getString(R.string.chat_history_delete_message, title))
            .setPositiveButton(R.string.chat_history_delete_confirm) { _, _ ->
                viewModel.deleteSession(session)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
