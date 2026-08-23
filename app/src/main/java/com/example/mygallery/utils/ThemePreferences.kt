package com.example.mygallery.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.mygallery.R

/**
 * Persists and applies the two theme-related settings on the Settings
 * screen: UI Mode (System/Light/Dark) and Theme Color (accent swatches).
 *
 * IMPORTANT ordering rule for both: whatever is saved here must be
 * APPLIED before any screen's setContentView() runs — themes can't be
 * swapped on a screen that's already drawn.
 *   - UI Mode is applied in MyGalleryApp.onCreate() (runs before ANY
 *     Activity, guaranteed).
 *   - Theme Color is applied via setTheme() in MainActivity, called
 *     BEFORE super.onCreate().
 */
object ThemePreferences {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_UI_MODE = "key_ui_mode"
    private const val KEY_THEME_COLOR = "key_theme_color"

    enum class UiMode {
        SYSTEM, LIGHT, DARK
    }

    /**
     * One entry per swatch in the design. `styleResId` is the actual
     * theme variant to apply; `swatchColorRes` is just the solid color
     * used to draw that swatch button in the Settings UI.
     */
    enum class ThemeColorOption(val styleResId: Int, val swatchColorRes: Int) {
        BLUE(R.style.Theme_MyGallery_Blue, R.color.accent_blue),
        GREEN(R.style.Theme_MyGallery_Green, R.color.accent_green),
        PINK(R.style.Theme_MyGallery_Pink, R.color.accent_pink),
        PURPLE(R.style.Theme_MyGallery_Purple, R.color.accent_purple),
        TEAL(R.style.Theme_MyGallery_Teal, R.color.accent_teal),
        YELLOW(R.style.Theme_MyGallery_Yellow, R.color.accent_yellow),
        RED(R.style.Theme_MyGallery_Red, R.color.accent_red),
        VIOLET(R.style.Theme_MyGallery_Violet, R.color.accent_violet)
    }

    fun getUiMode(context: Context): UiMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_UI_MODE, UiMode.SYSTEM.name)
        return UiMode.valueOf(saved ?: UiMode.SYSTEM.name)
    }

    fun setUiMode(context: Context, mode: UiMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_UI_MODE, mode.name)
            .apply()

        applyUiMode(mode)
    }

    /**
     * Actually tells AppCompat which mode to use. Called once at app
     * startup (MyGalleryApp) to restore the saved choice, and again
     * immediately whenever the user picks a new one on the Settings
     * screen.
     */
    fun applyUiMode(mode: UiMode) {
        val nightMode = when (mode) {
            UiMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            UiMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            UiMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun getThemeColor(context: Context): ThemeColorOption {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME_COLOR, ThemeColorOption.BLUE.name)
        return ThemeColorOption.valueOf(saved ?: ThemeColorOption.BLUE.name)
    }

    fun setThemeColor(context: Context, option: ThemeColorOption) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_COLOR, option.name)
            .apply()
    }
}