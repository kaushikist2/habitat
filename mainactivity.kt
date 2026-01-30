package com.example.habittracker

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var habitEditText: EditText
    private lateinit var addHabitButton: Button
    private lateinit var markAsDoneButton: Button
    private lateinit var resetProgressButton: Button
    private lateinit var progressTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var habitListContainer: LinearLayout
    private lateinit var deleteAllButton: Button
    private lateinit var streakTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var navHomeButton: Button
    private lateinit var navDashboardButton: Button
    private lateinit var exportButton: Button

    private lateinit var sharedPreferences: SharedPreferences
    private val habitTextViews = mutableListOf<TextView>()

    companion object {
        private const val PREFS_NAME = "HabitTrackerPrefs"
        private const val HABITS_KEY = "habits_list"
        const val PROGRESS_KEY = "habit_progress"
        private const val STREAK_KEY = "habit_streak"
        const val MAX_PROGRESS = 30
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        habitEditText = findViewById(R.id.habitEditText)
        addHabitButton = findViewById(R.id.addHabitButton)
        markAsDoneButton = findViewById(R.id.markAsDoneButton)
        resetProgressButton = findViewById(R.id.resetProgressButton)
        progressTextView = findViewById(R.id.progressTextView)
        progressBar = findViewById(R.id.progressBar)
        habitListContainer = findViewById(R.id.habitListContainer)
        deleteAllButton = findViewById(R.id.deleteAllButton)
        streakTextView = findViewById(R.id.streakTextView)
        dateTextView = findViewById(R.id.dateTextView)
        navHomeButton = findViewById(R.id.nav_home)
        navDashboardButton = findViewById(R.id.nav_dashboard)
        exportButton = findViewById(R.id.exportButton)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        loadHabits()
        loadProgress()
        loadStreak()
        startDateTimeUpdater()

        addHabitButton.setOnClickListener { addNewHabit() }
        markAsDoneButton.setOnClickListener { markHabitAsDone() }
        resetProgressButton.setOnClickListener { resetProgress() }
        deleteAllButton.setOnClickListener { deleteAllHabits() }
        navDashboardButton.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        exportButton.setOnClickListener { exportHabitReport() }
    }

    private fun startDateTimeUpdater() {
        val handler = Handler(Looper.getMainLooper())
        val formatter = SimpleDateFormat("EEEE, MMMM dd yyyy | hh:mm:ss a", Locale.getDefault())

        handler.post(object : Runnable {
            override fun run() {
                val currentDate = Calendar.getInstance().time
                dateTextView.text = formatter.format(currentDate)
                handler.postDelayed(this, 1000)
            }
        })
    }

    /** HABIT FUNCTIONS **/
    private fun loadHabits() {
        habitListContainer.removeAllViews()
        habitTextViews.clear()
        val habitListJson = sharedPreferences.getString(HABITS_KEY, "[]")
        val jsonArray = JSONArray(habitListJson ?: "[]")
        for (i in 0 until jsonArray.length()) {
            addHabitToUI(jsonArray.getString(i))
        }
    }

    private fun loadProgress() {
        val progress = sharedPreferences.getInt(PROGRESS_KEY, 0)
        progressTextView.text = "Days of Consistency: $progress / $MAX_PROGRESS"
        progressBar.progress = progress
    }

    private fun loadStreak() {
        val streak = sharedPreferences.getInt(STREAK_KEY, 0)
        streakTextView.text = "Current Momentum: $streak Days"
    }

    private fun addNewHabit() {
        val habitName = habitEditText.text.toString().trim()
        if (habitName.isEmpty()) {
            Toast.makeText(this, "Please enter a habit.", Toast.LENGTH_SHORT).show()
            return
        }
        val jsonArray = JSONArray(sharedPreferences.getString(HABITS_KEY, "[]"))
        jsonArray.put(habitName)
        sharedPreferences.edit().putString(HABITS_KEY, jsonArray.toString()).apply()
        addHabitToUI(habitName)
        habitEditText.text.clear()
        Toast.makeText(this, "Habit Committed!", Toast.LENGTH_SHORT).show()
    }

    private fun addHabitToUI(habitName: String) {
        val habitTextView = TextView(this).apply {
            text = "• $habitName"
            textSize = 16f
            setPadding(20, 10, 20, 10)
            setTextColor(resources.getColor(android.R.color.black, theme))
        }
        habitTextViews.add(0, habitTextView)
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.rounded_input_light)
            setPadding(16, 12, 16, 12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 10, 0, 10) }
            addView(habitTextView)
        }
        habitListContainer.addView(wrapper, 0)
    }

    private fun markHabitAsDone() {
        if (habitTextViews.isEmpty()) {
            Toast.makeText(this, "No habits to mark as done!", Toast.LENGTH_SHORT).show()
            return
        }
        val targetTextView = habitTextViews[0]
        val isStriked = targetTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG != 0

        if (!isStriked) {
            targetTextView.paintFlags = targetTextView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            var currentProgress = sharedPreferences.getInt(PROGRESS_KEY, 0)
            var currentStreak = sharedPreferences.getInt(STREAK_KEY, 0)
            if (currentProgress < MAX_PROGRESS) {
                currentProgress++; currentStreak++
                sharedPreferences.edit().putInt(PROGRESS_KEY, currentProgress)
                    .putInt(STREAK_KEY, currentStreak).apply()
                loadProgress(); loadStreak(); checkMilestones(currentStreak)
                Toast.makeText(this, "Habit Achieved! Keep the momentum!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Monthly goal complete! Consider a Fresh Start.", Toast.LENGTH_SHORT).show()
            }
        } else {
            targetTextView.paintFlags = targetTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            Toast.makeText(this, "Habit Unmarked.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkMilestones(streak: Int) {
        val message = when (streak) {
            5 -> "🔥 Great job! You’ve reached a 5-day streak!"
            10 -> "💪 Incredible! 10 days strong!"
            20 -> "🌟 You’re unstoppable! 20-day streak achieved!"
            else -> return
        }
        AlertDialog.Builder(this).setTitle("Milestone Unlocked!")
            .setMessage(message)
            .setPositiveButton("Awesome!") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun resetProgress() {
        sharedPreferences.edit().putInt(PROGRESS_KEY, 0).putInt(STREAK_KEY, 0).apply()
        loadProgress(); loadStreak()
        Toast.makeText(this, "New Month, Fresh Start! Progress Reset.", Toast.LENGTH_SHORT).show()
    }

    private fun deleteAllHabits() {
        sharedPreferences.edit().remove(HABITS_KEY).putInt(PROGRESS_KEY, 0).putInt(STREAK_KEY, 0).apply()
        habitListContainer.removeAllViews()
        habitTextViews.clear()
        loadProgress(); loadStreak()
        Toast.makeText(this, "All Habits Cleared!", Toast.LENGTH_SHORT).show()
    }

    private fun exportHabitReport() {
        val habitListJson = sharedPreferences.getString(HABITS_KEY, "[]")
        val jsonArray = JSONArray(habitListJson ?: "[]")
        if (jsonArray.length() == 0) {
            Toast.makeText(this, "No habits to export!", Toast.LENGTH_SHORT).show()
            return
        }

        val sb = StringBuilder()
        sb.append("Habit Tracker Report\n====================\n\n")
        for (i in 0 until jsonArray.length()) {
            sb.append("• ${jsonArray.getString(i)}\n")
        }
        sb.append("\nProgress: ${sharedPreferences.getInt(PROGRESS_KEY, 0)} / $MAX_PROGRESS")
        sb.append("\nCurrent Streak: ${sharedPreferences.getInt(STREAK_KEY, 0)} Days")

        val fileName = "Habit_Report_${System.currentTimeMillis()}.txt"
        val data = sb.toString()
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse("data:text/plain," + Uri.encode(data))

        val request = DownloadManager.Request(uri).apply {
            setTitle(fileName)
            setDescription("Your Habit Tracker Report")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        downloadManager.enqueue(request)
        Toast.makeText(this, "Downloading $fileName...", Toast.LENGTH_SHORT).show()
    }
}
