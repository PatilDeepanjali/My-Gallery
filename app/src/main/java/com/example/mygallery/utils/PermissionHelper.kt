package com.example.mygallery.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHelper {

    // ---------------------------------------------------------
    // READ IMAGE PERMISSION
    // ---------------------------------------------------------

    fun hasImagePermission(
        context: Context
    ): Boolean {

        return ContextCompat.checkSelfPermission(
            context,
            getImagePermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getImagePermission(): String {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            Manifest.permission.READ_MEDIA_IMAGES

        } else {

            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }


    // ---------------------------------------------------------
    // WRITE / MODIFY MEDIA
    // ---------------------------------------------------------

    fun hasWritePermission(
        context: Context
    ): Boolean {

        // Android 11+ does not use WRITE_EXTERNAL_STORAGE
        // for normal MediaStore operations.
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.R
        ) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getWritePermission(): String {

        return Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
}