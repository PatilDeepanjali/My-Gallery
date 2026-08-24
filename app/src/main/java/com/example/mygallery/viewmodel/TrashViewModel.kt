package com.example.mygallery.viewmodel

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygallery.model.TrashItem
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.utils.TrashStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TrashViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _trashedPhotos =
        MutableLiveData<List<TrashItem>>()

    val trashedPhotos: LiveData<List<TrashItem>> =
        _trashedPhotos


    // =========================================================
    // Constants
    // =========================================================

    companion object {

        private const val TRASH_DAYS = 30L

        private const val MILLIS_PER_DAY =
            24L * 60L * 60L * 1000L
    }


    // =========================================================
    // Load Trash
    // =========================================================

    fun loadTrash(
        context: Context
    ) {

        viewModelScope.launch {

            val photos =
                withContext(Dispatchers.IO) {

                    val allItems =
                        TrashStorage.getAll(
                            context
                        )


                    val currentTime =
                        System.currentTimeMillis()


                    val expiredItems =
                        allItems.filter { item ->

                            currentTime -
                                    item.trashedAt >=
                                    TRASH_DAYS *
                                    MILLIS_PER_DAY
                        }


                    // -------------------------------------------------
                    // Permanently delete expired items
                    // -------------------------------------------------

                    expiredItems.forEach { item ->

                        permanentlyDeleteSingle(
                            context,
                            item
                        )
                    }


                    // -------------------------------------------------
                    // Return only active Trash items
                    // -------------------------------------------------

                    allItems.filterNot { item ->

                        item in expiredItems
                    }
                }


            _trashedPhotos.postValue(
                photos
            )
        }
    }


    // =========================================================
    // Restore
    // =========================================================

    fun restoreImages(
        context: Context,
        photos: List<TrashItem>,
        onResult: (Int) -> Unit
    ) {

        viewModelScope.launch {

            val restoredCount =
                withContext(Dispatchers.IO) {

                    var count = 0


                    photos.forEach { photo ->

                        if (
                            restoreSingleImage(
                                context,
                                photo
                            )
                        ) {

                            count++
                        }
                    }


                    count
                }


            onResult(
                restoredCount
            )


            loadTrash(
                context
            )
        }
    }


    // =========================================================
    // Restore Single Image
    // =========================================================

    private fun restoreSingleImage(
        context: Context,
        item: TrashItem
    ): Boolean {

        val trashFile =
            File(
                item.trashFilePath
            )


        if (
            !trashFile.exists()
        ) {

            // The Trash file is missing.
            // Remove stale metadata.

            TrashStorage.remove(
                context,
                item.id
            )

            return false
        }


        val resolver =
            context.contentResolver


        val collection =
            MediaStore.Images.Media
                .getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )


        // ---------------------------------------------------------
        // Original album
        // ---------------------------------------------------------

        val relativePath =

            if (
                item.folderName.isBlank()
            ) {

                "${Environment.DIRECTORY_PICTURES}/"

            } else {

                "${Environment.DIRECTORY_PICTURES}/" +
                        "${item.folderName}/"
            }


        // ---------------------------------------------------------
        // Create MediaStore entry
        // ---------------------------------------------------------

        val values =
            ContentValues().apply {

                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    item.name
                )

                put(
                    MediaStore.Images.Media.MIME_TYPE,
                    item.mimeType
                )

                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    relativePath
                )

                put(
                    MediaStore.Images.Media.IS_PENDING,
                    1
                )
            }


        val newUri =
            resolver.insert(
                collection,
                values
            )


        if (
            newUri == null
        ) {

            return false
        }


        return try {

            // -----------------------------------------------------
            // Copy Trash file → MediaStore
            // -----------------------------------------------------

            resolver.openOutputStream(
                newUri
            )?.use { output ->

                trashFile.inputStream().use { input ->

                    input.copyTo(
                        output
                    )
                }

            } ?: throw Exception(
                "Unable to open MediaStore output stream"
            )


            // -----------------------------------------------------
            // Make image visible
            // -----------------------------------------------------

            val completeValues =
                ContentValues().apply {

                    put(
                        MediaStore.Images.Media.IS_PENDING,
                        0
                    )
                }


            resolver.update(
                newUri,
                completeValues,
                null,
                null
            )


            // -----------------------------------------------------
            // Remove Trash copy
            // -----------------------------------------------------

            val trashDeleted =
                trashFile.delete()


            if (
                !trashDeleted &&
                trashFile.exists()
            ) {

                // We successfully restored the image,
                // so don't fail the restore merely because
                // cleanup failed.
            }


            // -----------------------------------------------------
            // Remove metadata
            // -----------------------------------------------------

            TrashStorage.remove(
                context,
                item.id
            )


            true

        } catch (
            e: Exception
        ) {

            // -----------------------------------------------------
            // Restore failed.
            // Remove partially-created MediaStore item.
            // -----------------------------------------------------

            try {

                resolver.delete(
                    newUri,
                    null,
                    null
                )

            } catch (
                _: Exception
            ) {
            }


            false
        }
    }


    // =========================================================
    // Permanent Delete
    // =========================================================

    fun permanentlyDeleteImages(
        context: Context,
        photos: List<TrashItem>,
        onResult: (Int) -> Unit
    ) {

        viewModelScope.launch {

            val deletedCount =
                withContext(Dispatchers.IO) {

                    var count = 0


                    photos.forEach { photo ->

                        if (
                            permanentlyDeleteSingle(
                                context,
                                photo
                            )
                        ) {

                            count++
                        }
                    }


                    count
                }


            onResult(
                deletedCount
            )


            loadTrash(
                context
            )
        }
    }


    // =========================================================
    // Permanently Delete Single
    // =========================================================

    private fun permanentlyDeleteSingle(
        context: Context,
        item: TrashItem
    ): Boolean {

        return try {

            val file =
                File(
                    item.trashFilePath
                )


            // -----------------------------------------------------
            // Delete physical Trash file
            // -----------------------------------------------------

            val deleted =

                if (
                    file.exists()
                ) {

                    file.delete()

                } else {

                    true
                }


            // -----------------------------------------------------
            // Remove metadata only when physical cleanup
            // succeeded.
            // -----------------------------------------------------

            if (
                deleted
            ) {

                TrashStorage.remove(
                    context,
                    item.id
                )
            }


            deleted

        } catch (
            _: Exception
        ) {

            false
        }
    }
}