package com.example.mygallery.viewmodel

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygallery.model.ImageModel
import com.example.mygallery.repository.GalleryRepository
import kotlinx.coroutines.launch

class TrashViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _trashedPhotos =
        MutableLiveData<List<ImageModel>>()

    val trashedPhotos: LiveData<List<ImageModel>> =
        _trashedPhotos


    fun loadTrash(context: Context) {

        viewModelScope.launch {

            val photos =
                repository.getTrashedImages(
                    context
                )

            _trashedPhotos.value = photos
        }
    }


    fun restoreImages(
        context: Context,
        photos: List<ImageModel>,
        onResult: (Boolean) -> Unit
    ) {

        viewModelScope.launch {

            try {

                var restoredCount = 0

                photos.forEach { photo ->

                    val values = ContentValues().apply {
                        put(
                            MediaStore.Images.Media.IS_TRASHED,
                            0
                        )
                    }

                    val updated =
                        context.contentResolver.update(
                            photo.uri,
                            values,
                            null,
                            null
                        )

                    if (updated > 0) {
                        restoredCount++
                    }
                }

                onResult(
                    restoredCount == photos.size
                )

            } catch (e: Exception) {

                onResult(false)
            }
        }
    }
}