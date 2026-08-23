package com.example.mygallery

import android.app.Application
import com.example.mygallery.utils.ThemePreferences

/**
 * Restores the user's saved UI Mode (System/Light/Dark) as the very
 * first thing that happens when the app process starts — before ANY
 * Activity's onCreate() runs. This is important: applying it only in
 * MainActivity would mean any OTHER entry point (widgets, deep links,
 * a future second Activity) could briefly show the wrong mode.
 */
class MyGalleryApp : Application() {

    override fun onCreate() {
        super.onCreate()

        ThemePreferences.applyUiMode(
            ThemePreferences.getUiMode(this)
        )
    }
}