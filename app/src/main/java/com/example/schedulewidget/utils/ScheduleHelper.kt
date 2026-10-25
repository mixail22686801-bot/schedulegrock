package com.example.schedulewidget.utils

import com.example.schedulewidget.data.Lesson
import java.util.Calendar

data class CurrentStatus(
    val currentLesson: Lesson?,
    val nextLesson: Lesson?,
    val isBreak: Boolean,
    val minutesRemaining: Int,       // until end of current lesson or until next lesson starts
    val statusText: String,          // "Урок" / "Перемена" / "Нет уроков"
    val progressPercent: Float       // 0..1 for current lesson progress
)

object ScheduleHelper {

    fun getCurrentStatus(lessons: List<Lesson>): CurrentStatus {
        if (lessons.isEmpty()) {
            return CurrentStatus(null, null, false, 0, "Нет уроков", 0f)
        }

        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        // Find current lesson
        val current = lessons.firstOrNull {
            nowMinutes >= it.startMinutes() && nowMinutes < it.endMinutes()
        }

        if (current != null) {
            val remaining = current.endMinutes() - nowMinutes
            val duration = current.endMinutes() - current.startMinutes()
            val progress = if (duration > 0) {
                1f - remaining.toFloat() / duration
            } else 0f
            val next = lessons.firstOrNull { it.startMinutes() > current.endMinutes() }
            return CurrentStatus(
                currentLesson = current,
                nextLesson = next,
                isBreak = false,
                minutesRemaining = remaining,
                statusText = "Урок",
                progressPercent = progress.coerceIn(0f, 1f)
            )
        }

        // Check if before first lesson
        val first = lessons.first()
        if (nowMinutes < first.startMinutes()) {
            val remaining = first.startMinutes() - nowMinutes
            return CurrentStatus(
                currentLesson = null,
                nextLesson = first,
                isBreak = true,
                minutesRemaining = remaining,
                statusText = "До начала",
                progressPercent = 0f
            )
        }

        // Check if after last lesson
        val last = lessons.last()
        if (nowMinutes >= last.endMinutes()) {
            return CurrentStatus(
                currentLesson = null,
                nextLesson = null,
                isBreak = false,
                minutesRemaining = 0,
                statusText = "Уроки закончились",
                progressPercent = 1f
            )
        }

        // Break between lessons
        val next = lessons.firstOrNull { it.startMinutes() > nowMinutes }
        val prev = lessons.lastOrNull { it.endMinutes() <= nowMinutes }

        val remaining = if (next != null) next.startMinutes() - nowMinutes else 0

        return CurrentStatus(
            currentLesson = null,
            nextLesson = next,
            isBreak = true,
            minutesRemaining = remaining,
            statusText = "Перемена",
            progressPercent = 0f
        )
    }

    fun formatRemaining(minutes: Int): String {
        if (minutes <= 0) return "0 мин"
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 -> "${h}ч ${m}м"
            else -> "${m} мин"
        }
    }
}
