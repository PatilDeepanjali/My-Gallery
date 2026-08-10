package com.example.mygallery.utils

import android.content.Context

/**
 * Tracks album names the user has explicitly created via "+", WITHOUT
 * touching the filesystem directly (java.io.File.mkdirs() on shared
 * storage breaks under Scoped Storage on Android 10+).
 *
 * An album only becomes "real" in MediaStore once it contains at least
 * one photo — MediaStore has no concept of a truly empty folder. So
 * this just remembers the user's intent, and the picker UI shows these
 * names as selectable destinations even before any photo exists there.
 * Once a photo is copied/moved in (via GalleryRepository.copyImages,
 * which uses RELATIVE_PATH), the album becomes a genuine MediaStore
 * folder and shows up normally everywhere.
 */
object CustomAlbumPreferences {

    private const val PREFS_NAME = "custom_album_prefs"
    private const val KEY_NAMES = "key_custom_album_names"

    fun getCustomAlbumNames(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return (prefs.getStringSet(KEY_NAMES, emptySet()) ?: emptySet()).toSet()
    }

    fun hasCustomAlbum(context: Context, name: String): Boolean {
        return getCustomAlbumNames(context).contains(name)
    }

    fun addCustomAlbum(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getCustomAlbumNames(context).toMutableSet()
        current.add(name)
        prefs.edit().putStringSet(KEY_NAMES, current).apply()
    }
}