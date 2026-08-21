package com.example.mygallery.viewmodel

import android.content.ContentValues
import android.content.Context
import android.os.Build
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


    // ---------------------------------------------------------
    // Load Trash
    // ---------------------------------------------------------

    fun loadTrash(
        context: Context
    ) {

        viewModelScope.launch {

            val photos =
                withContext(Dispatchers.IO) {

                    val allItems =
                        TrashStorage.getAll(context)

                    val currentTime =
                        System.currentTimeMillis()

                    val thirtyDays =
                        30L *
                                24L *
                                60L *
                                60L *
                                1000L

                    val expiredItems =
                        allItems.filter { item ->

                            currentTime - item.trashedAt >= thirtyDays
                        }

                    // Permanently remove expired files
                    expiredItems.forEach { item ->

                        permanentlyDeleteSingle(
                            context,
                            item
                        )
                    }

                    // Only keep non-expired items
                    allItems.filter { item ->

                        item !in expiredItems
                    }
                }

            _trashedPhotos.value =
                photos
        }
    }


    // ---------------------------------------------------------
    // Restore
    // ---------------------------------------------------------

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

            loadTrash(context)
        }
    }


    private fun restoreSingleImage(
        context: Context,
        item: TrashItem
    ): Boolean {

        val trashFile =
            File(
                item.trashFilePath
            )

        if (!trashFile.exists()) {
            return false
        }


        val resolver =
            context.contentResolver

        val collection =
            MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )


        val relativePath =
            if (item.folderName.isBlank()) {

                Environment.DIRECTORY_PICTURES

            } else {

                "${Environment.DIRECTORY_PICTURES}/" +
                        item.folderName
            }


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
            ) ?: return false


        return try {

            resolver.openOutputStream(
                newUri
            )?.use { output ->

                trashFile.inputStream().use { input ->

                    input.copyTo(output)
                }
            }


            // Make the restored image visible.
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


            // Remove our Trash copy.
            trashFile.delete()

            TrashStorage.remove(
                context,
                item.id
            )

            true

        } catch (e: Exception) {

            // If restore failed, remove the partially
            // created MediaStore item.
            try {
                resolver.delete(
                    newUri,
                    null,
                    null
                )
            } catch (_: Exception) {
            }

            false
        }
    }


    // ---------------------------------------------------------
    // Permanent Delete
    // ---------------------------------------------------------

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

            loadTrash(context)
        }
    }


    private fun permanentlyDeleteSingle(
        context: Context,
        item: TrashItem
    ): Boolean {

        return try {

            val file =
                File(
                    item.trashFilePath
                )

            val deleted =
                !file.exists() || file.delete()

            if (deleted) {

                TrashStorage.remove(
                    context,
                    item.id
                )
            }

            deleted

        } catch (e: Exception) {

            false
        }
    }
}