package com.user.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.user.ClawMobileApplication
import com.user.R
import com.user.databinding.ActivitySessionsBinding
import com.user.ui.CommandAssist
import com.user.ui.SessionAdapter
import com.user.viewmodel.SessionViewModel

/**
 * Chat history activity showing all chat sessions
 */
class SessionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionsBinding
    private lateinit var sessionAdapter: SessionAdapter
    private val viewModel: SessionViewModel by viewModels()
    private lateinit var prefsManager: com.user.data.PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefsManager = (application as ClawMobileApplication).prefsManager
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Chat History"

        sessionAdapter = SessionAdapter(
            onClick = { session ->
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("session_id",    session.sessionId)
                    putExtra("session_title", session.title)
                }
                startActivity(intent)
            },
            onDelete = { session -> viewModel.deleteSession(session) },
            onPin = { session ->
                val pinned = prefsManager.togglePinnedSession(session.sessionId)
                Toast.makeText(
                    this,
                    getString(if (pinned) R.string.session_pinned else R.string.session_unpinned),
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.refresh()
            },
            isPinned = { session -> prefsManager.isSessionPinned(session.sessionId) },
            onLongPress = { session ->
                CommandAssist.showSessionActions(this, session.sessionId, session.title)
            }
        )

        binding.sessionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SessionsActivity)
            adapter = sessionAdapter
        }

        binding.searchInput.addTextChangedListener { editable ->
            viewModel.setQuery(editable?.toString().orEmpty())
        }

        viewModel.sessions.observe(this) { sessions ->
            val (pinned, regular) = sessions.partition { prefsManager.isSessionPinned(it.sessionId) }
            sessionAdapter.submitSessions(pinned + regular)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Newest First").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(0, 2, 0, "Oldest First").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(0, 3, 0, "Delete All").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            1 -> { viewModel.sortNewest(); true }
            2 -> { viewModel.sortOldest(); true }
            3 -> { viewModel.deleteAll(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}


