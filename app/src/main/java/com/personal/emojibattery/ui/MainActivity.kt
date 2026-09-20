package com.personal.emojibattery.ui

import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.materialswitch.MaterialSwitch
import com.personal.emojibattery.R
import com.personal.emojibattery.data.HorizontalPosition
import com.personal.emojibattery.data.Prefs
import com.personal.emojibattery.data.StickerId
import com.personal.emojibattery.data.WidgetSize
import com.personal.emojibattery.overlay.BatteryWidgetView
import com.personal.emojibattery.service.OverlayService

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var switchOverlay: MaterialSwitch
    private lateinit var tvPermissionStatus: TextView
    private lateinit var tvOverlayStatus: TextView
    private lateinit var previewContainer: FrameLayout
    private lateinit var previewView: BatteryWidgetView
    private var updatingUi = false

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updatePermissionUi()
            if (canOverlay() && prefs.overlayEnabled) {
                OverlayService.start(this)
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* optional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        switchOverlay = findViewById(R.id.switchOverlay)
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus)
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus)
        previewContainer = findViewById(R.id.previewContainer)

        previewView = BatteryWidgetView(this)
        previewContainer.addView(
            previewView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGrantPermission)
            .setOnClickListener { requestOverlayPermission() }

        switchOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (updatingUi) return@setOnCheckedChangeListener
            if (isChecked) {
                if (!canOverlay()) {
                    updatingUi = true
                    switchOverlay.isChecked = false
                    updatingUi = false
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                    return@setOnCheckedChangeListener
                }
                prefs.overlayEnabled = true
                maybeRequestNotificationPermission()
                OverlayService.start(this)
            } else {
                prefs.overlayEnabled = false
                OverlayService.stop(this)
            }
            updateOverlayStatusText()
        }

        setupStickerChips()
        setupSizeChips()
        setupPositionChips()
        refreshPreview()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUi()
        updatingUi = true
        switchOverlay.isChecked = prefs.overlayEnabled && canOverlay()
        updatingUi = false
        updateOverlayStatusText()
        refreshPreview()
        if (prefs.overlayEnabled && canOverlay()) {
            OverlayService.refresh(this)
        }
    }

    private fun setupStickerChips() {
        val map = mapOf(
            R.id.chipTreecko to StickerId.TREECKO,
            R.id.chipPikachu to StickerId.PIKACHU,
            R.id.chipEevee to StickerId.EEVEE,
            R.id.chipMudkip to StickerId.MUDKIP,
            R.id.chipTorchic to StickerId.TORCHIC,
            R.id.chipSquirtle to StickerId.SQUIRTLE
        )
        map.forEach { (id, sticker) ->
            val chip = findViewById<Chip>(id)
            chip.isChecked = prefs.sticker == sticker
            chip.setOnClickListener {
                prefs.sticker = sticker
                refreshPreview()
                notifyService()
            }
        }
    }

    private fun setupSizeChips() {
        val map = mapOf(
            R.id.chipSizeSmall to WidgetSize.SMALL,
            R.id.chipSizeMedium to WidgetSize.MEDIUM,
            R.id.chipSizeLarge to WidgetSize.LARGE
        )
        map.forEach { (id, size) ->
            val chip = findViewById<Chip>(id)
            chip.isChecked = prefs.size == size
            chip.setOnClickListener {
                prefs.size = size
                refreshPreview()
                notifyService()
            }
        }
    }

    private fun setupPositionChips() {
        val map = mapOf(
            R.id.chipPosLeft to HorizontalPosition.LEFT,
            R.id.chipPosCenter to HorizontalPosition.CENTER,
            R.id.chipPosRight to HorizontalPosition.RIGHT
        )
        map.forEach { (id, pos) ->
            val chip = findViewById<Chip>(id)
            chip.isChecked = prefs.position == pos
            chip.setOnClickListener {
                prefs.position = pos
                notifyService()
            }
        }
    }

    private fun refreshPreview() {
        val bm = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        var pct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (pct !in 0..100) pct = 67
        previewView.applyConfig(prefs.sticker, prefs.size, pct)
    }

    private fun notifyService() {
        if (prefs.overlayEnabled && canOverlay()) {
            OverlayService.refresh(this)
        }
    }

    private fun canOverlay(): Boolean = Settings.canDrawOverlays(this)

    private fun updatePermissionUi() {
        if (canOverlay()) {
            tvPermissionStatus.text = getString(R.string.permission_granted)
            tvPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.md_theme_primary))
        } else {
            tvPermissionStatus.text = getString(R.string.permission_denied)
            tvPermissionStatus.setTextColor(0xFFC62828.toInt())
        }
    }

    private fun updateOverlayStatusText() {
        tvOverlayStatus.text = getString(
            if (prefs.overlayEnabled && canOverlay()) R.string.overlay_on else R.string.overlay_off
        )
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
