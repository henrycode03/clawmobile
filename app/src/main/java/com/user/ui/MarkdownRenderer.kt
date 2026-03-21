package com.user.ui

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.*
import androidx.core.content.ContextCompat
import com.user.R

object MarkdownRenderer {

    fun render(context: Context, raw: String): CharSequence {
        val builder = SpannableStringBuilder()
        val lines = raw.split("\n")
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            // ── Code block ────────────────────────────────────
            if (line.trimStart().startsWith("```")) {
                val lang = line.trimStart().removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                val code = codeLines.joinToString("\n")
                val start = builder.length
                builder.append(code).append("\n")
                builder.setSpan(
                    TypefaceSpan("monospace"), start, builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(
                    BackgroundColorSpan(
                        ContextCompat.getColor(context, R.color.code_background)
                    ),
                    start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                i++
                continue
            }

            // ── Heading ───────────────────────────────────────
            val headingMatch = Regex("^(#{1,3})\\s+(.+)").find(line)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val text  = headingMatch.groupValues[2]
                val start = builder.length
                builder.append(text).append("\n")
                val size = when (level) { 1 -> 1.5f; 2 -> 1.3f; else -> 1.1f }
                builder.setSpan(RelativeSizeSpan(size), start, builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                i++
                continue
            }

            // ── Bullet list ───────────────────────────────────
            if (line.startsWith("- ") || line.startsWith("* ")) {
                appendInline(context, builder, "  •  " + line.drop(2))
                builder.append("\n")
                i++
                continue
            }

            // ── Numbered list ─────────────────────────────────
            val numMatch = Regex("^(\\d+)\\.\\s+(.+)").find(line)
            if (numMatch != null) {
                val num  = numMatch.groupValues[1]
                val text = numMatch.groupValues[2]
                appendInline(context, builder, "  $num.  $text")
                builder.append("\n")
                i++
                continue
            }

            // ── Blockquote ────────────────────────────────────
            if (line.startsWith("> ")) {
                val text  = line.removePrefix("> ")
                val start = builder.length
                appendInline(context, builder, text)
                builder.append("\n")
                builder.setSpan(QuoteSpan(0xFF888888.toInt()), start, builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                i++
                continue
            }

            // ── Horizontal rule ───────────────────────────────
            if (line == "---" || line == "***" || line == "___") {
                builder.append("─────────────────\n")
                i++
                continue
            }

            // ── Normal line ───────────────────────────────────
            appendInline(context, builder, line)
            builder.append("\n")
            i++
        }

        // Trim trailing newlines
        while (builder.isNotEmpty() && builder.last() == '\n') {
            builder.delete(builder.length - 1, builder.length)
        }
        return builder
    }

    private fun appendInline(context: Context, builder: SpannableStringBuilder, line: String) {
        var remaining = line
        while (remaining.isNotEmpty()) {
            // Find first match among bold, italic, inline code
            val bold   = Regex("\\*\\*(.+?)\\*\\*").find(remaining)
            val italic = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)").find(remaining)
            val code   = Regex("`([^`]+)`").find(remaining)

            val first = listOfNotNull(bold, italic, code)
                .minByOrNull { it.range.first }

            if (first == null) {
                builder.append(remaining)
                break
            }

            // Append text before match
            if (first.range.first > 0) {
                builder.append(remaining.substring(0, first.range.first))
            }

            val start     = builder.length
            val innerText = first.groupValues[1]
            builder.append(innerText)

            when (first) {
                bold -> builder.setSpan(
                    StyleSpan(Typeface.BOLD), start, builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                italic -> builder.setSpan(
                    StyleSpan(Typeface.ITALIC), start, builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                code -> {
                    builder.setSpan(
                        TypefaceSpan("monospace"), start, builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    builder.setSpan(
                        BackgroundColorSpan(
                            ContextCompat.getColor(context, R.color.code_background)
                        ),
                        start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    builder.setSpan(
                        ForegroundColorSpan(
                            ContextCompat.getColor(context, R.color.code_text)
                        ),
                        start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            remaining = remaining.substring(first.range.last + 1)
        }
    }
}

