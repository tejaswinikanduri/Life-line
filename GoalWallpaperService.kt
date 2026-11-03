package com.everydaycounts.livewallpaper

import android.service.wallpaper.WallpaperService
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

class GoalWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = GoalEngine()

    inner class GoalEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private val prefs by lazy { getSharedPreferences("goals", MODE_PRIVATE) }
        private val tick = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, 60_000L)
            }
        }

        override fun onVisibilityChanged(visibleNow: Boolean) {
            visible = visibleNow
            handler.removeCallbacks(tick)
            if (visible) tick.run()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(tick)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(tick)
            super.onDestroy()
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) render(canvas)
            } catch (_: Exception) {
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun render(c: Canvas) {
            val w = c.width.toFloat()
            val h = c.height.toFloat()
            c.drawColor(Color.rgb(15, 15, 15))
            val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }
            val grey = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(125, 125, 125); textAlign = Paint.Align.CENTER }
            val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0, 184, 238); textAlign = Paint.Align.CENTER }
            val mutedDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(55, 55, 55) }
            val doneDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            val todayDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0, 184, 238) }

            val now = Calendar.getInstance()
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val date = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date())
            white.textSize = w * 0.19f
            c.drawText(time, w / 2f, h * 0.205f, white)
            white.textSize = w * 0.043f
            c.drawText(date, w / 2f, h * 0.255f, white)

            val goals = (0 until 3).mapNotNull { i ->
                val name = prefs.getString("name_$i", "").orEmpty()
                val targetText = prefs.getString("target_$i", "").orEmpty()
                val target = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(targetText)?.time }.getOrNull()
                if (name.isBlank() || target == null) null else Triple(name, target, prefs.getLong("start_$i", dayStart(System.currentTimeMillis())))
            }.take(3)

            if (goals.isEmpty()) {
                grey.textSize = w * 0.04f
                c.drawText("ADD YOUR GOALS IN THE APP", w / 2f, h * 0.48f, grey)
                return
            }

            val sectionTop = h * (if (goals.size == 1) 0.40f else 0.34f)
            val sectionHeight = h * (if (goals.size == 1) 0.23f else 0.43f)
            val eachHeight = sectionHeight / goals.size
            goals.forEachIndexed { index, goal ->
                val centerY = sectionTop + eachHeight * index
                val title = goal.first.uppercase(Locale.getDefault()).take(34)
                white.textSize = w * 0.032f
                c.drawText(title, w / 2f, centerY, white)

                val today = dayStart(System.currentTimeMillis())
                val targetDay = dayStart(goal.second)
                val startDay = dayStart(goal.third)
                val total = ((targetDay - startDay) / 86_400_000L).coerceAtLeast(1L)
                val elapsed = ((today - startDay) / 86_400_000L).coerceIn(0L, total)
                val left = ((targetDay - today) / 86_400_000L).coerceAtLeast(0L)
                val progress = (elapsed.toFloat() / total.toFloat() * 100f).coerceIn(0f, 100f)

                val cols = 14
                val rows = 5
                val dotAreaWidth = w * 0.82f
                val spacingX = dotAreaWidth / cols
                val radius = min(spacingX * 0.28f, w * 0.018f)
                val gridTop = centerY + h * 0.035f
                val dotCount = min(total, (cols * rows).toLong()).toInt()
                for (d in 0 until dotCount) {
                    val row = d / cols
                    val col = d % cols
                    val x = (w - dotAreaWidth) / 2f + spacingX * (col + 0.5f)
                    val y = gridTop + row * (radius * 3.0f)
                    val paint = when {
                        d.toLong() < elapsed -> doneDot
                        d.toLong() == elapsed && elapsed < total -> todayDot
                        else -> mutedDot
                    }
                    c.drawCircle(x, y, radius, paint)
                }
                grey.textSize = w * 0.029f
                val summaryY = gridTop + rows * radius * 3.0f + h * 0.018f
                c.drawText("${left}d left  •  ${progress.toInt()}%", w / 2f, summaryY, white)
            }
        }

        private fun dayStart(ms: Long): Long {
            val c = Calendar.getInstance().apply { timeInMillis = ms; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            return c.timeInMillis
        }
    }
}
