package com.example.mygallery

import android.app.Application
import com.example.mygallery.utils.ThemePreferences

class MyGalleryApp : Application() {

    override fun onCreate() {
        super.onCreate()

        ThemePreferences.applyUiMode(
            ThemePreferences.getUiMode(this)
        )
    }
}