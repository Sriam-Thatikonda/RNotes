package com.baverika.notoir.util.time

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diffMillis = now - timestamp

        if (diffMillis < 60_000) {
            return "Just now"
        }

        val minutes = diffMillis / (60 * 1000)
        if (minutes < 60) {
            return "${minutes}m ago"
        }

        val hours = diffMillis / (60 * 60 * 1000)
        if (hours < 24) {
            return "${hours}h ago"
        }

        val days = diffMillis / (24 * 60 * 60 * 1000)
        if (days == 1L) {
            return "Yesterday, ${timeFormat.format(Date(timestamp))}"
        }

        if (days < 7) {
            return "${days}d ago"
        }

        return dateFormat.format(Date(timestamp))
    }

    fun formatFullDateTime(timestamp: Long): String {
        return "${fullDateFormat.format(Date(timestamp))} at ${timeFormat.format(Date(timestamp))}"
    }
}
