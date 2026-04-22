package com.user.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.user.R

class OfflineBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val messageView: TextView
    var retryAction: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_offline_banner, this, true)
        messageView = findViewById(R.id.offline_banner_message)
        findViewById<android.view.View>(R.id.offline_banner_retry).setOnClickListener {
            retryAction?.invoke()
        }
    }

    fun showWithTimestamp(cachedAtMillis: Long?) {
        val timeText = if (cachedAtMillis != null) {
            val elapsed = System.currentTimeMillis() - cachedAtMillis
            val minutes = elapsed / 60_000
            context.getString(R.string.offline_banner_message_with_time, minutes)
        } else {
            context.getString(R.string.offline_banner_message)
        }
        messageView.text = timeText
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
