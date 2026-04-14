package com.user.ui

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormatUtils {

    private val displayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun formatApiTimestamp(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null

        return runCatching {
            OffsetDateTime.parse(value)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(displayFormatter)
        }.recoverCatching {
            Instant.parse(value)
                .atZone(ZoneId.systemDefault())
                .format(displayFormatter)
        }.recoverCatching {
            LocalDateTime.parse(value)
                .atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(displayFormatter)
        }.getOrNull() ?: value
    }
}