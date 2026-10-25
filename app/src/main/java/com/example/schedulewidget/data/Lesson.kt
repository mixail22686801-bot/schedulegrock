package com.example.schedulewidget.data

import org.json.JSONObject

data class Lesson(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    fun startMinutes(): Int = startHour * 60 + startMinute
    fun endMinutes(): Int = endHour * 60 + endMinute

    fun startTimeString(): String = "%02d:%02d".format(startHour, startMinute)
    fun endTimeString(): String = "%02d:%02d".format(endHour, endMinute)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("startHour", startHour)
        put("startMinute", startMinute)
        put("endHour", endHour)
        put("endMinute", endMinute)
    }

    companion object {
        fun fromJson(json: JSONObject): Lesson = Lesson(
            id = json.optString("id", java.util.UUID.randomUUID().toString()),
            name = json.getString("name"),
            startHour = json.getInt("startHour"),
            startMinute = json.getInt("startMinute"),
            endHour = json.getInt("endHour"),
            endMinute = json.getInt("endMinute")
        )
    }
}
