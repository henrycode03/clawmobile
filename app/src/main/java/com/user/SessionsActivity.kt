package com.user

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.user.data.ChatDatabase
import com.user.databinding.ActivitySessionsBinding
import com.user.ui.SessionAdapter
import kotlinx.coroutines.launch

class SessionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionsBinding
    private lateinit var sessionAdapter: SessionAdapter
    private lateinit var database: ChatDatabase
    private var sortNewestFirst = true
    private var allSessions = listOf<com.user.data.ChatSession>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Chat History"

        database = ChatDatabase.getDatabase(application)

        sessionAdapter = SessionAdapter(
            onClick = { session ->
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("session_id", session.sessionId)
                    putExtra("session_title", session.title)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            },
            onDelete = { session ->
                lifecycleScope.launch {
                    database.chatDao().deleteMessagesBySession(session.sessionId)
                    database.chatDao().deleteSession(session.sessionId)
                }
            }
        )

        binding.sessionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SessionsActivity)
            adapter = sessionAdapter
        }

        lifecycleScope.launch {
            database.chatDao().getAllSessions().collect { sessions ->
                allSessions = sessions
                applySort()
            }
        }
    }

    private fun applySort() {
        val sorted = if (sortNewestFirst) {
            allSessions.sortedByDescending { it.lastMessageAt }
        } else {
            allSessions.sortedBy { it.lastMessageAt }
        }
        sessionAdapter.submitList(sorted)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Newest First").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        menu.add(0, 2, 0, "Oldest First").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        menu.add(0, 3, 0, "Delete All").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }

        // Force text color to dark so it's visible
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val title = android.text.SpannableString(item.title)
            title.setSpan(
                android.text.style.ForegroundColorSpan(android.graphics.Color.BLACK),
                0, title.length,
                android.text.Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            item.title = title
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            1 -> {
                sortNewestFirst = true
                applySort()
                true
            }
            2 -> {
                sortNewestFirst = false
                applySort()
                true
            }
            3 -> {
                // Delete all sessions
                lifecycleScope.launch {
                    allSessions.forEach { session ->
                        database.chatDao().deleteMessagesBySession(session.sessionId)
                        database.chatDao().deleteSession(session.sessionId)
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}


