package com.example.schedulewidget.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class ScheduleRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)

    // Day of week: Calendar.MONDAY = 2 ... Calendar.SUNDAY = 1
    // We store as 1=Monday ... 7=Sunday for convenience
    fun getLessonsForDay(dayOfWeek: Int): List<Lesson> {
        val key = "day_$dayOfWeek"
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { Lesson.fromJson(arr.getJSONObject(it)) }
                .sortedBy { it.startMinutes() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLessonsForDay(dayOfWeek: Int, lessons: List<Lesson>) {
        val arr = JSONArray()
        lessons.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("day_$dayOfWeek", arr.toString()).apply()
    }

    fun getTodayLessons(): List<Lesson> {
        val cal = Calendar.getInstance()
        val day = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        return getLessonsForDay(day)
    }

    // Widget settings
    fun getWidgetColor(): Int = prefs.getInt("widget_color", 0xFF1E88E5.toInt())
    fun setWidgetColor(color: Int) = prefs.edit().putInt("widget_color", color).apply()

    fun getTextColor(): Int = prefs.getInt("text_color", 0xFFFFFFFF.toInt())
    fun setTextColor(color: Int) = prefs.edit().putInt("text_color", color).apply()

    fun getAccentColor(): Int = prefs.getInt("accent_color", 0xFFFFEB3B.toInt())
    fun setAccentColor(color: Int) = prefs.edit().putInt("accent_color", color).apply()
}
