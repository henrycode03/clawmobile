package com.user.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.user.R
import com.user.data.PrefsManager
import com.user.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GUIDE_MODE = "guide_mode"
    }

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefsManager: PrefsManager
    private var guideMode: Boolean = false
    private val pages = listOf(
        OnboardingPage(
            eyebrow = "Welcome",
            title = "ClawMobile is your mobile control plane",
            body = "Use your phone to monitor OpenClaw runs, inspect project progress, and intervene when work is blocked.",
            bullets = listOf(
                "Track projects, tasks, and sessions from Orchestrator",
                "Inspect logs, checkpoints, and file trees without going back to the host",
                "Use ClawMobile for fast supervision, not full desktop replacement",
            ),
        ),
        OnboardingPage(
            eyebrow = "Architecture",
            title = "Gateway and Orchestrator do different jobs",
            body = "ClawMobile talks to two pieces of your stack. Understanding that split makes setup and debugging much easier.",
            bullets = listOf(
                "Gateway: live chat, device pairing, and direct OpenClaw communication",
                "Orchestrator: projects, tasks, sessions, logs, checkpoints, and progress tracking",
                "For phone access, Tailscale or a reachable host IP is usually the cleanest path",
            ),
        ),
        OnboardingPage(
            eyebrow = "Remote control",
            title = "Start with a few command patterns",
            body = "When you are away from the computer, keep commands short and operational so the agent can act quickly.",
            bullets = listOf(
                "show blockers all",
                "open project <project_id>",
                "status session <session_id>",
                "resume session <session_id>",
                "diagnose task <task_id>",
            ),
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()

        prefsManager = PrefsManager(this)
        guideMode = intent.getBooleanExtra(EXTRA_GUIDE_MODE, false)

        val adapter = OnboardingPagerAdapter(pages)
        binding.onboardingPager.adapter = adapter
        TabLayoutMediator(binding.pageIndicator, binding.onboardingPager) { _, _ -> }.attach()
        configurePageIndicators()
        binding.skipButton.setOnClickListener { completeOnboarding() }
        binding.primaryButton.setOnClickListener {
            val nextIndex = binding.onboardingPager.currentItem + 1
            if (nextIndex < pages.size) {
                binding.onboardingPager.currentItem = nextIndex
            } else {
                completeOnboarding()
            }
        }
        binding.secondaryButton.setOnClickListener {
            val previousIndex = binding.onboardingPager.currentItem - 1
            if (previousIndex >= 0) {
                binding.onboardingPager.currentItem = previousIndex
            }
        }

        binding.onboardingPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateActions(position)
                updatePageIndicators(position)
            }
        })

        updateActions(0)
    }

    private fun applySystemInsets() {
        val rootStart = binding.root.paddingStart
        val rootTop = binding.root.paddingTop
        val rootEnd = binding.root.paddingEnd
        val rootBottom = binding.root.paddingBottom
        val buttonRowBottom = binding.buttonRow.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(
                rootStart + systemBars.left,
                rootTop + systemBars.top,
                rootEnd + systemBars.right,
                rootBottom
            )
            binding.buttonRow.updatePadding(bottom = buttonRowBottom + systemBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun updateActions(position: Int) {
        val isFirst = position == 0
        val isLast = position == pages.lastIndex

        binding.secondaryButton.isEnabled = !isFirst
        binding.secondaryButton.alpha = if (isFirst) 0.45f else 1f
        binding.primaryButton.text = if (isLast) {
            getString(R.string.onboarding_finish)
        } else {
            getString(R.string.onboarding_next)
        }
    }

    private fun configurePageIndicators() {
        val inactiveSize = 6.dpToPx()
        val spacing = 4.dpToPx()

        repeat(pages.size) { index ->
            val tab = binding.pageIndicator.getTabAt(index) ?: return@repeat
            tab.customView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(inactiveSize, inactiveSize).apply {
                    marginStart = spacing
                    marginEnd = spacing
                }
                background = AppCompatResources.getDrawable(
                    context,
                    R.drawable.bg_onboarding_indicator_inactive
                )
            }
        }

        updatePageIndicators(binding.onboardingPager.currentItem)
    }

    private fun updatePageIndicators(selectedPosition: Int) {
        val inactiveSize = 6.dpToPx()
        val activeWidth = 20.dpToPx()
        val spacing = 4.dpToPx()

        repeat(pages.size) { index ->
            val indicatorView = binding.pageIndicator.getTabAt(index)?.customView ?: return@repeat
            val params = (indicatorView.layoutParams as? LinearLayout.LayoutParams)
                ?: LinearLayout.LayoutParams(inactiveSize, inactiveSize)
            params.width = if (index == selectedPosition) activeWidth else inactiveSize
            params.height = inactiveSize
            params.marginStart = spacing
            params.marginEnd = spacing
            indicatorView.layoutParams = params
            indicatorView.background = AppCompatResources.getDrawable(
                this,
                if (index == selectedPosition) {
                    R.drawable.bg_onboarding_indicator_active
                } else {
                    R.drawable.bg_onboarding_indicator_inactive
                }
            )
        }
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

    private fun completeOnboarding() {
        prefsManager.onboardingCompleted = true
        if (!guideMode) {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
