package com.everydaycounts.livewallpaper

import android.app.Activity
import android.app.DatePickerDialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val fields = mutableListOf<EditText>()
    private val dates = mutableListOf<EditText>()
    private val prefs by lazy { getSharedPreferences("goals", MODE_PRIVATE) }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(17, 17, 17))
        }
        val title = TextView(this).apply {
            text = "EVERY DAY COUNTS"
            textSize = 25f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(title, matchWrap())
        val intro = TextView(this).apply {
            text = "Add up to 3 goals. Each countdown updates automatically every day."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 18)
        }
        root.addView(intro, matchWrap())

        for (i in 0 until 3) {
            val label = TextView(this).apply {
                text = "GOAL ${i + 1}"
                textSize = 12f
                setTextColor(Color.rgb(22, 185, 234))
                setPadding(0, 12, 0, 4)
            }
            root.addView(label, matchWrap())
            val name = EditText(this).apply {
                hint = "e.g. Stenography → JOB"
                setSingleLine(true)
                setTextColor(Color.WHITE)
                setHintTextColor(Color.GRAY)
                setBackgroundColor(Color.rgb(40, 40, 40))
                setPadding(12, 8, 12, 8)
                setText(prefs.getString("name_$i", "") ?: "")
            }
            fields.add(name)
            root.addView(name, matchWidth(52))
            val date = EditText(this).apply {
                hint = "Target date (tap to choose)"
                isFocusable = false
                isClickable = true
                setTextColor(Color.WHITE)
                setHintTextColor(Color.GRAY)
                setBackgroundColor(Color.rgb(40, 40, 40))
                setPadding(12, 8, 12, 8)
                setText(prefs.getString("target_$i", "") ?: "")
                setOnClickListener { showDatePicker(this) }
            }
            dates.add(date)
            val dateParams = matchWidth(52).apply { topMargin = 7 }
            root.addView(date, dateParams)
        }

        val save = Button(this).apply {
            text = "SAVE GOALS"
            setOnClickListener { saveGoals() }
        }
        root.addView(save, matchWidth(54).apply { topMargin = 20 })
        val setWallpaper = Button(this).apply {
            text = "PREVIEW / SET LIVE WALLPAPER"
            setOnClickListener {
                val component = ComponentName(this@MainActivity, GoalWallpaperService::class.java)
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                try { startActivity(intent) } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "Open Android Wallpaper settings and choose Every Day Counts.", Toast.LENGTH_LONG).show()
                }
            }
        }
        root.addView(setWallpaper, matchWidth(54).apply { topMargin = 8 })
        val note = TextView(this).apply {
            text = "Tip: Save your goals first, then preview and apply the wallpaper. Choose Home screen, Lock screen, or both if your phone offers that option."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 18, 0, 0)
        }
        root.addView(note, matchWrap())
        setContentView(root)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun matchWidth(height: Int) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)

    private fun showDatePicker(target: EditText) {
        val c = Calendar.getInstance()
        val current = target.text.toString()
        if (current.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            runCatching { dateFormat.parse(current) }.getOrNull()?.let { c.time = it }
        }
        DatePickerDialog(this, { _, y, m, d ->
            target.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveGoals() {
        val editor = prefs.edit()
        for (i in 0 until 3) {
            val name = fields[i].text.toString().trim()
            val target = dates[i].text.toString().trim()
            editor.putString("name_$i", name)
            editor.putString("target_$i", target)
            if (name.isNotBlank() && target.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                val oldTarget = prefs.getString("target_$i", "")
                if (oldTarget != target || prefs.getLong("start_$i", 0L) == 0L) {
                    editor.putLong("start_$i", dayStart(System.currentTimeMillis()))
                }
            }
        }
        editor.apply()
        Toast.makeText(this, "Goals saved. Preview your live wallpaper!", Toast.LENGTH_SHORT).show()
    }

    private fun dayStart(ms: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ms; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        return c.timeInMillis
    }
}
