package com.example.schedulewidget

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.schedulewidget.data.Lesson
import com.example.schedulewidget.data.ScheduleRepository
import com.example.schedulewidget.widget.ScheduleWidgetProvider
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var repo: ScheduleRepository
    private lateinit var tabLayout: TabLayout
    private lateinit var recycler: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: LessonsAdapter

    private var currentDay = 1 // 1=Mon ... 7=Sun
    private var lessons = mutableListOf<Lesson>()

    private val dayNames = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repo = ScheduleRepository(this)

        tabLayout = findViewById(R.id.tab_days)
        recycler = findViewById(R.id.recycler_lessons)
        emptyText = findViewById(R.id.tv_empty)

        // Tabs
        dayNames.forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentDay = tab.position + 1
                loadDay()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        adapter = LessonsAdapter(
            onEdit = { lesson -> showLessonDialog(lesson) },
            onDelete = { lesson ->
                lessons.removeAll { it.id == lesson.id }
                saveAndRefresh()
            }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<Button>(R.id.btn_add).setOnClickListener {
            showLessonDialog(null)
        }

        findViewById<Button>(R.id.btn_colors).setOnClickListener {
            showColorDialog()
        }

        findViewById<Button>(R.id.btn_refresh_widget).setOnClickListener {
            ScheduleWidgetProvider.updateAll(this)
            Toast.makeText(this, "Виджет обновлён", Toast.LENGTH_SHORT).show()
        }

        // Select today
        val cal = java.util.Calendar.getInstance()
        val todayIndex = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> 0
            java.util.Calendar.TUESDAY -> 1
            java.util.Calendar.WEDNESDAY -> 2
            java.util.Calendar.THURSDAY -> 3
            java.util.Calendar.FRIDAY -> 4
            java.util.Calendar.SATURDAY -> 5
            java.util.Calendar.SUNDAY -> 6
            else -> 0
        }
        tabLayout.getTabAt(todayIndex)?.select()
        currentDay = todayIndex + 1
        loadDay()
    }

    private fun loadDay() {
        lessons = repo.getLessonsForDay(currentDay).toMutableList()
        adapter.submit(lessons)
        emptyText.visibility = if (lessons.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun saveAndRefresh() {
        repo.saveLessonsForDay(currentDay, lessons)
        adapter.submit(lessons)
        emptyText.visibility = if (lessons.isEmpty()) View.VISIBLE else View.GONE
        ScheduleWidgetProvider.updateAll(this)
    }

    private fun showLessonDialog(existing: Lesson?) {
        val view = layoutInflater.inflate(R.layout.dialog_lesson, null)
        val etName = view.findViewById<EditText>(R.id.et_name)
        val tpStart = view.findViewById<TimePicker>(R.id.tp_start)
        val tpEnd = view.findViewById<TimePicker>(R.id.tp_end)

        tpStart.setIs24HourView(true)
        tpEnd.setIs24HourView(true)

        if (existing != null) {
            etName.setText(existing.name)
            tpStart.hour = existing.startHour
            tpStart.minute = existing.startMinute
            tpEnd.hour = existing.endHour
            tpEnd.minute = existing.endMinute
        } else {
            // Default: next hour
            val cal = java.util.Calendar.getInstance()
            tpStart.hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            tpStart.minute = 0
            tpEnd.hour = (cal.get(java.util.Calendar.HOUR_OF_DAY) + 1) % 24
            tpEnd.minute = 0
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Новый урок" else "Редактировать урок")
            .setView(view)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val startH = tpStart.hour
                val startM = tpStart.minute
                val endH = tpEnd.hour
                val endM = tpEnd.minute

                if (endH * 60 + endM <= startH * 60 + startM) {
                    Toast.makeText(this, "Время окончания должно быть позже начала", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (existing != null) {
                    val idx = lessons.indexOfFirst { it.id == existing.id }
                    if (idx >= 0) {
                        lessons[idx] = existing.copy(
                            name = name,
                            startHour = startH,
                            startMinute = startM,
                            endHour = endH,
                            endMinute = endM
                        )
                    }
                } else {
                    lessons.add(
                        Lesson(
                            name = name,
                            startHour = startH,
                            startMinute = startM,
                            endHour = endH,
                            endMinute = endM
                        )
                    )
                }
                lessons.sortBy { it.startMinutes() }
                saveAndRefresh()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showColorDialog() {
        val colors = listOf(
            "Синий" to 0xFF1E88E5.toInt(),
            "Тёмно-синий" to 0xFF0D47A1.toInt(),
            "Зелёный" to 0xFF43A047.toInt(),
            "Фиолетовый" to 0xFF8E24AA.toInt(),
            "Оранжевый" to 0xFFFB8C00.toInt(),
            "Красный" to 0xFFE53935.toInt(),
            "Тёмный" to 0xFF212121.toInt(),
            "Серый" to 0xFF546E7A.toInt()
        )
        val names = colors.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Цвет виджета")
            .setItems(names) { _, which ->
                repo.setWidgetColor(colors[which].second)
                // Text color: light on dark colors, dark on light
                val isDark = which in listOf(1, 6, 7)
                repo.setTextColor(if (isDark) 0xFFFFFFFF.toInt() else 0xFFFFFFFF.toInt())
                repo.setAccentColor(0xFFFFEB3B.toInt())
                ScheduleWidgetProvider.updateAll(this)
                Toast.makeText(this, "Цвет изменён", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // --- Adapter ---
    class LessonsAdapter(
        private val onEdit: (Lesson) -> Unit,
        private val onDelete: (Lesson) -> Unit
    ) : RecyclerView.Adapter<LessonsAdapter.VH>() {

        private var items = listOf<Lesson>()

        fun submit(list: List<Lesson>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_lesson, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val lesson = items[position]
            holder.tvNumber.text = "${position + 1}."
            holder.tvName.text = lesson.name
            holder.tvTime.text = "${lesson.startTimeString()} – ${lesson.endTimeString()}"
            holder.btnEdit.setOnClickListener { onEdit(lesson) }
            holder.btnDelete.setOnClickListener { onDelete(lesson) }
        }

        override fun getItemCount() = items.size

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNumber: TextView = v.findViewById(R.id.tv_number)
            val tvName: TextView = v.findViewById(R.id.tv_name)
            val tvTime: TextView = v.findViewById(R.id.tv_time)
            val btnEdit: ImageButton = v.findViewById(R.id.btn_edit)
            val btnDelete: ImageButton = v.findViewById(R.id.btn_delete)
        }
    }
}
