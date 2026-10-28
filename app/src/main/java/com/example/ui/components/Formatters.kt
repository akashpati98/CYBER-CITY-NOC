package com.example.ui.components

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

object Formatters {

    fun formatBps(bps: Long): String {
        val absVal = abs(bps)
        return when {
            absVal >= 1_000_000_000L -> String.format(Locale.US, "%.2f Gbps", bps / 1_000_000_000.0)
            absVal >= 1_000_000L -> String.format(Locale.US, "%.1f Mbps", bps / 1_000_000.0)
            absVal >= 1_000L -> String.format(Locale.US, "%.0f kbps", bps / 1_000.0)
            else -> "$bps bps"
        }
    }

    fun formatPps(pps: Long): String {
        return when {
            pps >= 1_000_000L -> String.format(Locale.US, "%.2f Mpps", pps / 1_000_000.0)
            pps >= 1_000L -> String.format(Locale.US, "%.1f kpps", pps / 1_000.0)
            else -> "$pps pps"
        }
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            seconds < 60 -> "${seconds}s ago"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${hours / 24}d ago"
        }
    }
}
