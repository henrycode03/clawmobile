package com.user.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.user.R

class StatusBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val label: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_status_badge, this, true)
        label = findViewById(R.id.status_badge_label)

        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.StatusBadgeView)
            val status = a.getString(R.styleable.StatusBadgeView_status)
            if (status != null) setStatus(status)
            a.recycle()
        }
    }

    fun setStatus(status: String) {
        val (colorRes, textRes) = when (status.lowercase()) {
            "running" -> Pair(R.color.status_running, R.string.status_running)
            "done", "completed" -> Pair(R.color.status_completed, R.string.status_done)
            "failed" -> Pair(R.color.status_failed, R.string.status_failed)
            "pending" -> Pair(R.color.status_pending, R.string.status_pending)
            "paused" -> Pair(R.color.status_pending, R.string.status_paused)
            "approved" -> Pair(R.color.status_approved, R.string.status_approved)
            "rejected", "denied" -> Pair(R.color.status_rejected, R.string.status_rejected)
            "timeout" -> Pair(R.color.status_timeout, R.string.status_timeout)
            else -> Pair(R.color.status_disconnected, R.string.status_unknown)
        }
        val color = ContextCompat.getColor(context, colorRes)
        label.text = context.getString(textRes)
        val bgColor = Color.argb(40, Color.red(color), Color.green(color), Color.blue(color))
        label.backgroundTintList = ColorStateList.valueOf(bgColor)
        label.setTextColor(color)
    }
}
