package com.user.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.user.ClawMobileApplication
import com.user.R
import com.user.data.Project
import com.user.databinding.BottomSheetStartSessionBinding
import com.user.service.OrchestratorApiClient
import com.user.ui.tasks.SessionDetailActivity
import kotlinx.coroutines.launch

class StartSessionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetStartSessionBinding? = null
    private val binding get() = _binding!!

    private var orchestratorClient: OrchestratorApiClient? = null
    private var projects: List<Project> = emptyList()
    private var selectedProject: Project? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetStartSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as ClawMobileApplication
        if (app.prefsManager.isOrchestratorConfigured()) {
            orchestratorClient = OrchestratorApiClient(app.prefsManager, app.prefsManager.gatewayToken)
        }

        binding.cancelButton.setOnClickListener { dismiss() }
        binding.confirmButton.setOnClickListener { onConfirm() }

        loadProjects()
    }

    private fun loadProjects() {
        val client = orchestratorClient ?: return
        lifecycleScope.launch {
            client.getProjects().onSuccess { list ->
                projects = list
                val names = list.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                binding.projectSpinner.setAdapter(adapter)
                binding.projectSpinner.setOnItemClickListener { _, _, position, _ ->
                    selectedProject = list[position]
                    loadTasksForProject(list[position])
                }
            }
        }
    }

    private fun loadTasksForProject(project: Project) {
        val client = orchestratorClient ?: return
        lifecycleScope.launch {
            client.getProjectTasks(project.getProjectId()).onSuccess { tasks ->
                val taskNames = listOf(getString(R.string.start_session_task_hint)) +
                        tasks.map { "${it.taskId}: ${it.title}" }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, taskNames)
                binding.taskSpinner.setAdapter(adapter)
            }
        }
    }

    private fun onConfirm() {
        val project = selectedProject
        if (project == null) {
            Snackbar.make(binding.root, R.string.start_session_project_hint, Snackbar.LENGTH_SHORT).show()
            return
        }
        val name = binding.sessionNameInput.text?.toString()?.trim()
            ?.takeIf { it.isNotBlank() } ?: "Session from mobile"
        val client = orchestratorClient ?: return

        binding.confirmButton.isEnabled = false
        lifecycleScope.launch {
            val projectId = project.getProjectId().toIntOrNull() ?: 0
            client.startSession(projectId, name).onSuccess { response ->
                dismiss()
                val intent = Intent(requireContext(), SessionDetailActivity::class.java).apply {
                    putExtra("session_id", response.sessionId.toString())
                    putExtra("session_name", name)
                }
                startActivity(intent)
            }.onFailure { error ->
                binding.confirmButton.isEnabled = true
                Snackbar.make(binding.root, error.message ?: getString(R.string.error_generic), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
