package com.example.mygallery.utils

import android.content.Context

/**
 * Tracks which photo ids the user has marked as favorite. Same pattern
 * as PinPreferences (for albums), but keyed by the photo's MediaStore
 * id (a Long) instead of a folder name — every ImageModel already
 * carries a stable `id` field pulled straight from MediaStore._ID, so
 * it's a reliable key across app restarts.
 *
 * SharedPreferences can only store String sets, so ids are stored as
 * their String form and converted back on read.
 */
object FavoritePreferences {

    private const val PREFS_NAME = "favorite_prefs"
    private const val KEY_FAVORITE_IDS = "key_favorite_photo_ids"

    fun getFavoriteIds(context: Context): Set<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getStringSet(KEY_FAVORITE_IDS, emptySet()) ?: emptySet()
        return stored.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun isFavorite(context: Context, photoId: Long): Boolean {
        return getFavoriteIds(context).contains(photoId)
    }

    fun toggleFavorite(context: Context, photoId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getFavoriteIds(context).toMutableSet()

        if (current.contains(photoId)) {
            current.remove(photoId)
        } else {
            current.add(photoId)
        }

        prefs.edit()
            .putStringSet(KEY_FAVORITE_IDS, current.map { it.toString() }.toSet())
            .apply()
    }
}