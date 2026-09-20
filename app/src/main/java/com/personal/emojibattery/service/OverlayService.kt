package com.personal.emojibattery.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.personal.emojibattery.R
import com.personal.emojibattery.data.HorizontalPosition
import com.personal.emojibattery.data.Prefs
import com.personal.emojibattery.overlay.BatteryWidgetView
import com.personal.emojibattery.ui.MainActivity

class OverlayService : Service() {

    private lateinit var prefs: Prefs
    private lateinit var windowManager: WindowManager
    private var widgetView: BatteryWidgetView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level >= 0 && scale > 0) {
                val percent = (level * 100) / scale
                widgetView?.setBatteryLevel(percent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerBatteryReceiver()
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                prefs.overlayEnabled = false
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH -> {
                refreshOverlay()
            }
            else -> {
                if (!prefs.overlayEnabled) {
                    prefs.overlayEnabled = true
                }
                refreshOverlay()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
        removeOverlay()
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    private fun currentBatteryPercent(): Int {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (pct in 0..100) pct else 67
    }

    private fun showOverlay() {
        if (widgetView != null) return

        val view = BatteryWidgetView(this)
        view.applyConfig(prefs.sticker, prefs.size, currentBatteryPercent())

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = gravityFor(prefs.position)
        params.x = horizontalOffset(prefs.position)
        params.y = statusBarOffset()

        layoutParams = params
        widgetView = view
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            widgetView = null
            layoutParams = null
            stopSelf()
        }
    }

    private fun refreshOverlay() {
        val view = widgetView
        val params = layoutParams
        if (view == null || params == null) {
            removeOverlay()
            showOverlay()
            return
        }
        view.applyConfig(prefs.sticker, prefs.size, currentBatteryPercent())
        params.gravity = gravityFor(prefs.position)
        params.x = horizontalOffset(prefs.position)
        params.y = statusBarOffset()
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
            removeOverlay()
            showOverlay()
        }
    }

    private fun removeOverlay() {
        widgetView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        widgetView = null
        layoutParams = null
    }

    private fun gravityFor(pos: HorizontalPosition): Int = when (pos) {
        HorizontalPosition.LEFT -> Gravity.TOP or Gravity.START
        HorizontalPosition.CENTER -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
        HorizontalPosition.RIGHT -> Gravity.TOP or Gravity.END
    }

    private fun horizontalOffset(pos: HorizontalPosition): Int {
        val density = resources.displayMetrics.density
        return when (pos) {
            HorizontalPosition.LEFT -> (8 * density).toInt()
            HorizontalPosition.CENTER -> 0
            HorizontalPosition.RIGHT -> (8 * density).toInt()
        }
    }

    private fun statusBarOffset(): Int {
        // Sit near the status bar area (top edge). OEM quirks may vary.
        val density = resources.displayMetrics.density
        val statusBar = statusBarHeight()
        // Slightly below status bar icons so the sticker peeks into that zone
        return (statusBar * 0.15f).toInt().coerceAtLeast((2 * density).toInt())
    }

    private fun statusBarHeight(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else (24 * resources.displayMetrics.density).toInt()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.app_subtitle)
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pending)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "emoji_battery_overlay"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.personal.emojibattery.STOP"
        const val ACTION_REFRESH = "com.personal.emojibattery.REFRESH"

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
            context.stopService(Intent(context, OverlayService::class.java))
        }

        fun refresh(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_REFRESH
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
