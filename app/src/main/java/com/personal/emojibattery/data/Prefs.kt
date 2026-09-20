package com.personal.emojibattery.data

import android.content.Context
import android.content.SharedPreferences
import com.personal.emojibattery.R

enum class StickerId(val prefValue: String, val drawableRes: Int, val labelRes: Int) {
    TREECKO("treecko", R.drawable.sticker_treecko, R.string.sticker_treecko),
    PIKACHU("pikachu", R.drawable.sticker_pikachu, R.string.sticker_pikachu),
    EEVEE("eevee", R.drawable.sticker_eevee, R.string.sticker_eevee),
    MUDKIP("mudkip", R.drawable.sticker_mudkip, R.string.sticker_mudkip),
    TORCHIC("torchic", R.drawable.sticker_torchic, R.string.sticker_torchic),
    SQUIRTLE("squirtle", R.drawable.sticker_squirtle, R.string.sticker_squirtle);

    companion object {
        fun fromPref(value: String?): StickerId =
            entries.find { it.prefValue == value } ?: TREECKO
    }
}

enum class WidgetSize(val prefValue: String, val scale: Float) {
    SMALL("small", 0.75f),
    MEDIUM("medium", 1.0f),
    LARGE("large", 1.35f);

    companion object {
        fun fromPref(value: String?): WidgetSize =
            entries.find { it.prefValue == value } ?: MEDIUM
    }
}

enum class HorizontalPosition(val prefValue: String) {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right");

    companion object {
        fun fromPref(value: String?): HorizontalPosition =
            entries.find { it.prefValue == value } ?: RIGHT
    }
}

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var overlayEnabled: Boolean
        get() = sp.getBoolean(KEY_OVERLAY, false)
        set(value) = sp.edit().putBoolean(KEY_OVERLAY, value).apply()

    var sticker: StickerId
        get() = StickerId.fromPref(sp.getString(KEY_STICKER, StickerId.TREECKO.prefValue))
        set(value) = sp.edit().putString(KEY_STICKER, value.prefValue).apply()

    var size: WidgetSize
        get() = WidgetSize.fromPref(sp.getString(KEY_SIZE, WidgetSize.MEDIUM.prefValue))
        set(value) = sp.edit().putString(KEY_SIZE, value.prefValue).apply()

    var position: HorizontalPosition
        get() = HorizontalPosition.fromPref(sp.getString(KEY_POSITION, HorizontalPosition.RIGHT.prefValue))
        set(value) = sp.edit().putString(KEY_POSITION, value.prefValue).apply()

    companion object {
        private const val PREFS_NAME = "emoji_battery_prefs"
        private const val KEY_OVERLAY = "overlay_enabled"
        private const val KEY_STICKER = "sticker"
        private const val KEY_SIZE = "size"
        private const val KEY_POSITION = "position"
    }
}
