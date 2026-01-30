package com.example.habittracker

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class DashboardActivity : AppCompatActivity() {

    private lateinit var navHomeButton: Button
    private lateinit var navDashboardButton: Button
    private lateinit var backButton: Button

    private lateinit var taskEditText: EditText
    private lateinit var addTaskButton: Button
    private lateinit var taskListContainer: LinearLayout

    private lateinit var habitProgressText: TextView
    private lateinit var habitProgressChart: ProgressBar
    private lateinit var taskProgressText: TextView
    private lateinit var taskProgressChart: ProgressBar

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "HabitTrackerPrefs"
        private const val TASKS_KEY = "daily_tasks_list"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        navHomeButton = findViewById(R.id.nav_home)
        navDashboardButton = findViewById(R.id.nav_dashboard)
        backButton = findViewById(R.id.backButton)

        taskEditText = findViewById(R.id.taskEditText)
        addTaskButton = findViewById(R.id.addTaskButton)
        taskListContainer = findViewById(R.id.taskListContainer)

        habitProgressText = findViewById(R.id.habitProgressText)
        habitProgressChart = findViewById(R.id.habitProgressChart)
        taskProgressText = findViewById(R.id.taskProgressText)
        taskProgressChart = findViewById(R.id.taskProgressChart)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        loadTasks()
        visuallyRepresentData()

        navHomeButton.setOnClickListener { navigateTo(MainActivity::class.java) }
        backButton.setOnClickListener { navigateTo(MainActivity::class.java) }
        addTaskButton.setOnClickListener { addNewTask() }
    }

    private fun <T : AppCompatActivity> navigateTo(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }

    // task manager

    private fun loadTasks() {
        taskListContainer.removeAllViews()
        val taskListJson = sharedPreferences.getString(TASKS_KEY, "[]")

        try {
            val jsonArray = JSONArray(taskListJson)
            for (i in 0 until jsonArray.length()) {
                val taskObject = jsonArray.getJSONObject(i)
                val name = taskObject.getString("name")
                val isCompleted = taskObject.getBoolean("isCompleted")
                addTaskToUI(name, isCompleted)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun addNewTask() {
        val taskName = taskEditText.text.toString().trim()
        if (taskName.isEmpty()) {
            Toast.makeText(this, "Please enter a task.", Toast.LENGTH_SHORT).show()
            return
        }

        val newTask = JSONObject().apply {
            put("name", taskName)
            put("isCompleted", false)
        }

        val taskListJson = sharedPreferences.getString(TASKS_KEY, "[]")
        val jsonArray = JSONArray(taskListJson)
        jsonArray.put(newTask)

        sharedPreferences.edit().apply {
            putString(TASKS_KEY, jsonArray.toString())
            apply()
        }

        addTaskToUI(taskName, false)
        taskEditText.text.clear()
        Toast.makeText(this, "Task Added!", Toast.LENGTH_SHORT).show()
        visuallyRepresentData()
    }

    private fun addTaskToUI(taskName: String, isCompleted: Boolean) {

        val taskEntryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 10) }
            elevation = 2f
        }

        val taskTextView = TextView(this).apply {
            text = taskName
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            )
            if (isCompleted) {
                toggleStrikeThrough(true)
            }
        }

        taskTextView.setOnClickListener {
            val isCurrentlyStriked = taskTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG != 0

            taskTextView.toggleStrikeThrough(!isCurrentlyStriked)

            updateTaskCompletion(taskName, !isCurrentlyStriked)

            val message = if (!isCurrentlyStriked) "Task Completed!" else "Task Unmarked."
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }


        val deleteButton = Button(this).apply {
            text = "X"
            textSize = 12f
            minWidth = 0
            minHeight = 0
            setPadding(16, 8, 16, 8)
            backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_red_dark)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 16
            }
        }

        deleteButton.setOnClickListener {
            removeTask(taskName, taskEntryLayout)
        }

        taskEntryLayout.addView(taskTextView)
        taskEntryLayout.addView(deleteButton)
        taskListContainer.addView(taskEntryLayout)
    }

    private fun updateTaskCompletion(taskName: String, isCompleted: Boolean) {
        val taskListJson = sharedPreferences.getString(TASKS_KEY, "[]")
        val jsonArray = JSONArray(taskListJson)

        for (i in 0 until jsonArray.length()) {
            val taskObject = jsonArray.getJSONObject(i)
            if (taskObject.getString("name") == taskName) {
                taskObject.put("isCompleted", isCompleted)
                break
            }
        }

        sharedPreferences.edit().apply {
            putString(TASKS_KEY, jsonArray.toString())
            apply()
        }
        visuallyRepresentData()
    }

    private fun removeTask(taskName: String, viewToRemove: View) {
        taskListContainer.removeView(viewToRemove)

        val taskListJson = sharedPreferences.getString(TASKS_KEY, "[]")
        val jsonArray = JSONArray(taskListJson)
        val newJsonArray = JSONArray()

        for (i in 0 until jsonArray.length()) {
            val taskObject = jsonArray.getJSONObject(i)
            if (taskObject.getString("name") != taskName) {
                newJsonArray.put(taskObject)
            }
        }

        sharedPreferences.edit().apply {
            putString(TASKS_KEY, newJsonArray.toString())
            apply()
        }
        Toast.makeText(this, "Task removed.", Toast.LENGTH_SHORT).show()
        visuallyRepresentData()
    }

    private fun visuallyRepresentData() {

        //  Habit Data
        try {
            val habitsCompletedToday = sharedPreferences.getInt(MainActivity.PROGRESS_KEY, 0)
            val maxProgress = MainActivity.MAX_PROGRESS

            val habitProgressPercent = if (maxProgress > 0) (habitsCompletedToday * 100) / maxProgress else 0

            habitProgressText.text = "Habit Days Tracked: ${habitsCompletedToday} / ${maxProgress}"
            habitProgressChart.progress = habitProgressPercent
        } catch (e: Exception) {
            habitProgressText.text = "Habit Data Error (Fix MainActivity)"
            habitProgressChart.progress = 0
        }

        val taskListJson = sharedPreferences.getString(TASKS_KEY, "[]")
        var completedTasks = 0
        var totalTasks = 0

        try {
            val taskArray = JSONArray(taskListJson)
            totalTasks = taskArray.length()
            for (i in 0 until totalTasks) {
                val taskObject = taskArray.getJSONObject(i)
                if (taskObject.getBoolean("isCompleted")) {
                    completedTasks++
                }
            }
        } catch (e: JSONException) {

        }

        val taskProgressPercent = if (totalTasks > 0) (completedTasks * 100) / totalTasks else 0

        taskProgressText.text = "Tasks Completed: $completedTasks / $totalTasks"
        taskProgressChart.progress = taskProgressPercent
    }

    fun TextView.toggleStrikeThrough(isStriked: Boolean) {
        if (isStriked) {
            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }
}
