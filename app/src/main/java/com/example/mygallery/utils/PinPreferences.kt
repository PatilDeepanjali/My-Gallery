package com.example.mygallery.utils

import android.content.Context

/**
 * Tracks which album folder names the user has pinned. Kept completely
 * separate from GalleryFolder/MediaStore — "pinned" is purely an
 * app-level preference, not something the device's media database
 * knows or cares about.
 */
object PinPreferences {

    private const val PREFS_NAME = "pin_prefs"
    private const val KEY_PINNED = "key_pinned_folders"
    private const val KEY_BANNER_DISMISSED = "key_pin_banner_dismissed"

    fun getPinnedFolderNames(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // IMPORTANT: getStringSet() returns a set you must NOT mutate
        // directly (Android docs warn this can corrupt the stored
        // preference) — toMutableSet() makes a safe copy to edit.
        return (prefs.getStringSet(KEY_PINNED, emptySet()) ?: emptySet()).toSet()
    }

    fun isPinned(context: Context, folderName: String): Boolean {
        return getPinnedFolderNames(context).contains(folderName)
    }

    fun togglePin(context: Context, folderName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getPinnedFolderNames(context).toMutableSet()

        if (current.contains(folderName)) {
            current.remove(folderName)
        } else {
            current.add(folderName)
        }

        prefs.edit().putStringSet(KEY_PINNED, current).apply()
    }

    /**
     * The "Pinned albums will appear at the top" hint banner. Once the
     * user dismisses it, we remember that permanently — no reason to
     * keep explaining the same thing to them every time they open the
     * app.
     */
    fun isBannerDismissed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BANNER_DISMISSED, false)
    }

    fun dismissBanner(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BANNER_DISMISSED, true).apply()
    }
}