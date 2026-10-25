package com.example.schedulewidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.schedulewidget.MainActivity
import com.example.schedulewidget.R
import com.example.schedulewidget.data.ScheduleRepository
import com.example.schedulewidget.utils.ScheduleHelper
import java.util.Calendar

class ScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ScheduleWidgetProvider::class.java)
            )
            onUpdate(context, manager, ids)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.schedulewidget.ACTION_REFRESH"

        fun updateAll(context: Context) {
            val intent = Intent(context, ScheduleWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent)
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val repo = ScheduleRepository(context)
            val lessons = repo.getTodayLessons()
            val status = ScheduleHelper.getCurrentStatus(lessons)

            val bgColor = repo.getWidgetColor()
            val textColor = repo.getTextColor()
            val accentColor = repo.getAccentColor()

            val views = RemoteViews(context.packageName, R.layout.widget_schedule)

            // Background
            views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)

            // Header - day name
            val dayNames = arrayOf("", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
            val cal = Calendar.getInstance()
            val dayIndex = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
            views.setTextViewText(R.id.tv_day, dayNames[dayIndex])
            views.setTextColor(R.id.tv_day, textColor)

            // Status
            views.setTextViewText(R.id.tv_status, status.statusText)
            views.setTextColor(R.id.tv_status, accentColor)

            // Current lesson / break
            if (status.currentLesson != null) {
                views.setTextViewText(R.id.tv_current_name, status.currentLesson.name)
                views.setTextViewText(
                    R.id.tv_current_time,
                    "${status.currentLesson.startTimeString()} – ${status.currentLesson.endTimeString()}"
                )
                views.setTextViewText(
                    R.id.tv_remaining,
                    "осталось ${ScheduleHelper.formatRemaining(status.minutesRemaining)}"
                )
                views.setViewVisibility(R.id.progress_bar, View.VISIBLE)
                views.setProgressBar(R.id.progress_bar, 100, (status.progressPercent * 100).toInt(), false)
            } else if (status.isBreak && status.nextLesson != null) {
                views.setTextViewText(R.id.tv_current_name, "Перемена")
                views.setTextViewText(R.id.tv_current_time, "")
                views.setTextViewText(
                    R.id.tv_remaining,
                    "до ${status.nextLesson.name} — ${ScheduleHelper.formatRemaining(status.minutesRemaining)}"
                )
                views.setViewVisibility(R.id.progress_bar, View.GONE)
            } else {
                views.setTextViewText(R.id.tv_current_name, status.statusText)
                views.setTextViewText(R.id.tv_current_time, "")
                views.setTextViewText(R.id.tv_remaining, "")
                views.setViewVisibility(R.id.progress_bar, View.GONE)
            }

            views.setTextColor(R.id.tv_current_name, textColor)
            views.setTextColor(R.id.tv_current_time, textColor)
            views.setTextColor(R.id.tv_remaining, accentColor)

            // Next lesson
            if (status.nextLesson != null && status.currentLesson != null) {
                views.setViewVisibility(R.id.next_container, View.VISIBLE)
                views.setTextViewText(R.id.tv_next_label, "Следующий:")
                views.setTextViewText(
                    R.id.tv_next_name,
                    "${status.nextLesson.name} (${status.nextLesson.startTimeString()})"
                )
            } else {
                views.setViewVisibility(R.id.next_container, View.GONE)
            }
            views.setTextColor(R.id.tv_next_label, textColor)
            views.setTextColor(R.id.tv_next_name, textColor)

            // Side list of lessons
            val listBuilder = StringBuilder()
            lessons.forEachIndexed { index, lesson ->
                val isCurrent = status.currentLesson?.id == lesson.id
                val marker = when {
                    isCurrent -> "▶ "
                    status.isBreak && status.nextLesson?.id == lesson.id -> "│ "
                    else -> "  "
                }
                listBuilder.append("${marker}${index + 1}. ${lesson.name}\n")
                listBuilder.append("   ${lesson.startTimeString()}–${lesson.endTimeString()}\n")
            }
            views.setTextViewText(R.id.tv_side_list, listBuilder.toString().trim())
            views.setTextColor(R.id.tv_side_list, textColor)

            // Click to open app
            val openIntent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)

            // Refresh button
            val refreshIntent = Intent(context, ScheduleWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPending = PendingIntent.getBroadcast(
                context, 1, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
