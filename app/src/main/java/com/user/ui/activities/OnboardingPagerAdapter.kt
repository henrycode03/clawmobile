package com.user.ui.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.user.databinding.ItemOnboardingPageBinding

data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val body: String,
    val bullets: List<String>,
)

class OnboardingPagerAdapter(
    private val pages: List<OnboardingPage>,
) : RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    class OnboardingViewHolder(
        private val binding: ItemOnboardingPageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(page: OnboardingPage) {
            binding.eyebrowView.text = page.eyebrow
            binding.titleView.text = page.title
            binding.bodyView.text = page.body
            binding.bulletsView.text = page.bullets.joinToString("\n") { "• $it" }
        }
    }
}