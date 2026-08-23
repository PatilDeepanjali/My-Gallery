package com.example.mygallery.ui.photo.menu

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mygallery.model.ImageModel
import android.app.RecoverableSecurityException
import android.os.Build
import androidx.annotation.RequiresApi

object PhotoMenuActions {

    // ---------------------------------------------------------
    // OPEN WITH
    // ---------------------------------------------------------

    fun openWith(
        context: Context,
        photo: ImageModel
    ) {

        try {

            val mimeType =
                context.contentResolver.getType(photo.uri)
                    ?: photo.mimeType
                        .ifBlank { "image/*" }

            val intent =
                Intent(Intent.ACTION_VIEW).apply {

                    setDataAndType(
                        photo.uri,
                        mimeType
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            context.startActivity(
                Intent.createChooser(
                    intent,
                    "Open With"
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                context,
                "No app available to open this file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // ---------------------------------------------------------
    // EDIT WITH
    // ---------------------------------------------------------

    fun editWith(
        context: Context,
        photo: ImageModel
    ) {

        try {

            val mimeType =
                context.contentResolver.getType(photo.uri)
                    ?: photo.mimeType
                        .ifBlank { "image/*" }

            val intent =
                Intent(Intent.ACTION_EDIT).apply {

                    setDataAndType(
                        photo.uri,
                        mimeType
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    addFlags(
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }

            context.startActivity(
                Intent.createChooser(
                    intent,
                    "Edit With"
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                context,
                "No editing app available",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // ---------------------------------------------------------
    // RENAME
    // ---------------------------------------------------------

    sealed class RenameResult {

        data object Success : RenameResult()

        data class NeedsPermission(
            val intentSender: android.content.IntentSender
        ) : RenameResult()

        data class Error(
            val message: String
        ) : RenameResult()
    }


    fun rename(
        context: Context,
        photo: ImageModel,
        newName: String
    ): RenameResult {

        val extension =
            photo.name.substringAfterLast(
                ".",
                ""
            )

        val finalName =
            if (
                extension.isNotEmpty() &&
                !newName.contains(".")
            ) {
                "$newName.$extension"
            } else {
                newName
            }

        val values =
            ContentValues().apply {
                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    finalName
                )
            }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            renameApi29Plus(
                context,
                photo,
                values
            )

        } else {

            renameBelowApi29(
                context,
                photo,
                values
            )
        }
    }

    @androidx.annotation.RequiresApi(
        android.os.Build.VERSION_CODES.Q
    )
    private fun renameApi29Plus(
        context: Context,
        photo: ImageModel,
        values: ContentValues
    ): RenameResult {

        return try {

            val updated =
                context.contentResolver.update(
                    photo.uri,
                    values,
                    null,
                    null
                )

            if (updated > 0) {

                RenameResult.Success

            } else {

                RenameResult.Error(
                    "Rename failed"
                )
            }

        } catch (
            e: android.app.RecoverableSecurityException
        ) {

            RenameResult.NeedsPermission(
                e.userAction
                    .actionIntent
                    .intentSender
            )

        } catch (e: SecurityException) {

            RenameResult.Error(
                "No permission to rename this photo"
            )

        } catch (e: Exception) {

            RenameResult.Error(
                e.message ?: "Rename failed"
            )
        }
    }


    private fun renameBelowApi29(
        context: Context,
        photo: ImageModel,
        values: ContentValues
    ): RenameResult {

        return try {

            val updated =
                context.contentResolver.update(
                    photo.uri,
                    values,
                    null,
                    null
                )

            if (updated > 0) {

                RenameResult.Success

            } else {

                RenameResult.Error(
                    "Rename failed"
                )
            }

        } catch (e: Exception) {

            RenameResult.Error(
                e.message ?: "Rename failed"
            )
        }
    }

    fun showRenameDialog(
        fragment: Fragment,
        photo: ImageModel,
        onRename: (String) -> Unit
    ) {

        val editText =
            EditText(fragment.requireContext()).apply {

                setSingleLine(true)

                setText(
                    photo.name.substringBeforeLast(
                        ".",
                        photo.name
                    )
                )

                selectAll()
            }

        val container =
            LinearLayout(
                fragment.requireContext()
            ).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    24,
                    0,
                    24,
                    0
                )

                addView(editText)
            }

        val dialog =
            AlertDialog.Builder(
                fragment.requireContext()
            )
                .setTitle("Rename")
                .setMessage("Enter a new name for this photo.")
                .setView(container)
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "Done",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val newName =
                    editText.text
                        .toString()
                        .trim()

                if (newName.isEmpty()) {

                    editText.error =
                        "Name cannot be empty"

                    return@setOnClickListener
                }

                onRename(newName)

                dialog.dismiss()
            }
        }

        dialog.show()
    }


    // ---------------------------------------------------------
    // WALLPAPER
    // ---------------------------------------------------------

    fun showWallpaperDialog(
        fragment: Fragment,
        photo: ImageModel
    ) {

        val options =
            arrayOf(
                "Set On Home Screen",
                "Set On Lock Screen",
                "Set On Both Screen"
            )

        var selectedOption = 0

        AlertDialog.Builder(
            fragment.requireContext()
        )
            .setTitle("Set Wallpaper")
            .setSingleChoiceItems(
                options,
                selectedOption
            ) { _, which ->

                selectedOption = which
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Apply"
            ) { _, _ ->

                applyWallpaper(
                    fragment.requireContext(),
                    photo,
                    selectedOption
                )
            }
            .show()
    }


    private fun applyWallpaper(
        context: Context,
        photo: ImageModel,
        option: Int
    ) {

        try {

            val wallpaperManager =
                WallpaperManager.getInstance(
                    context
                )

            val inputStream =
                context.contentResolver
                    .openInputStream(photo.uri)

            if (inputStream == null) {

                Toast.makeText(
                    context,
                    "Unable to load image",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            val originalBitmap =
                inputStream.use {
                    android.graphics.BitmapFactory
                        .decodeStream(it)
                }

            if (originalBitmap == null) {

                Toast.makeText(
                    context,
                    "Unable to load image",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            val targetWidth =
                wallpaperManager
                    .desiredMinimumWidth

            val targetHeight =
                wallpaperManager
                    .desiredMinimumHeight

            val wallpaperBitmap =
                createWallpaperBitmap(
                    originalBitmap,
                    targetWidth,
                    targetHeight
                )

            when (option) {

                0 -> {

                    wallpaperManager.setBitmap(
                        wallpaperBitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_SYSTEM
                    )
                }

                1 -> {

                    wallpaperManager.setBitmap(
                        wallpaperBitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_LOCK
                    )
                }

                2 -> {

                    wallpaperManager.setBitmap(
                        wallpaperBitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_SYSTEM or
                                WallpaperManager.FLAG_LOCK
                    )
                }
            }

            Toast.makeText(
                context,
                "Wallpaper applied",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            Toast.makeText(
                context,
                "Unable to set wallpaper",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun createWallpaperBitmap(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {

        if (
            targetWidth <= 0 ||
            targetHeight <= 0
        ) {
            return source
        }

        val sourceRatio =
            source.width.toFloat() /
                    source.height.toFloat()

        val targetRatio =
            targetWidth.toFloat() /
                    targetHeight.toFloat()

        val cropWidth: Int
        val cropHeight: Int
        val cropX: Int
        val cropY: Int

        if (sourceRatio > targetRatio) {

            cropHeight =
                source.height

            cropWidth =
                (source.height * targetRatio)
                    .toInt()

            cropX =
                (source.width - cropWidth) / 2

            cropY = 0

        } else {

            cropWidth =
                source.width

            cropHeight =
                (source.width / targetRatio)
                    .toInt()

            cropX = 0

            cropY =
                (source.height - cropHeight) / 2
        }

        val croppedBitmap =
            Bitmap.createBitmap(
                source,
                cropX,
                cropY,
                cropWidth,
                cropHeight
            )

        return Bitmap.createScaledBitmap(
            croppedBitmap,
            targetWidth,
            targetHeight,
            true
        )
    }


    // ---------------------------------------------------------
    // SHARE ONE PHOTO
    // ---------------------------------------------------------

    fun sharePhoto(
        context: Context,
        photo: ImageModel
    ) {

        try {

            val mimeType =
                context.contentResolver.getType(
                    photo.uri
                ) ?: photo.mimeType
                    .ifBlank { "image/*" }

            val intent =
                Intent(Intent.ACTION_SEND).apply {

                    type = mimeType

                    putExtra(
                        Intent.EXTRA_STREAM,
                        photo.uri
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            context.startActivity(
                Intent.createChooser(
                    intent,
                    "Share Photo"
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                context,
                "Unable to share photo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // ---------------------------------------------------------
    // DETAILS
    // ---------------------------------------------------------

    fun showDetails(
        fragment: Fragment,
        photos: List<ImageModel>
    ) {

        if (photos.isEmpty()) {
            return
        }

        com.example.mygallery.ui.photo.details
            .PhotoDetailsBottomSheet(
                ArrayList(photos)
            )
            .show(
                fragment.childFragmentManager,
                "photo_details"
            )
    }
}