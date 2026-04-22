package com.user.ui.permissions

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.user.databinding.ActivityPermissionsBinding
import com.user.viewmodel.PermissionViewModel

class PermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding
    private val viewModel: PermissionViewModel by viewModels()
    private lateinit var pendingAdapter: PermissionAdapter
    private lateinit var resolvedAdapter: PermissionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerViews()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPermissions()
    }

    private fun setupRecyclerViews() {
        pendingAdapter = PermissionAdapter(
            onApprove = { viewModel.approve(it) },
            onReject = { viewModel.reject(it) },
            readOnly = false
        )
        resolvedAdapter = PermissionAdapter(
            onApprove = {},
            onReject = {},
            readOnly = true
        )
        binding.pendingRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PermissionsActivity)
            adapter = pendingAdapter
        }
        binding.resolvedRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PermissionsActivity)
            adapter = resolvedAdapter
        }
        binding.offlineBanner.retryAction = { viewModel.loadPermissions() }
    }

    private fun setupObservers() {
        viewModel.pendingPermissions.observe(this) { list ->
            pendingAdapter.submitList(list)
            binding.emptyPendingText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.resolvedPermissions.observe(this) { list ->
            resolvedAdapter.submitList(list)
            binding.emptyResolvedText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(this) { error ->
            if (error != null) {
                binding.offlineBanner.showWithTimestamp(null)
                Snackbar.make(binding.root, error, Snackbar.LENGTH_SHORT).show()
            } else {
                binding.offlineBanner.hide()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
