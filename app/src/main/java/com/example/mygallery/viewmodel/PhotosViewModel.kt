package com.example.mygallery.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.state.PhotosUiState
import com.example.mygallery.utils.DateGroupingUtil
import kotlinx.coroutines.launch

class PhotosViewModel(private val repository: GalleryRepository) : ViewModel() {

    private val _uiState = MutableLiveData<PhotosUiState>()
    val uiState: LiveData<PhotosUiState> = _uiState

    /**
     * @param folderName  null = all photos on the device.
     *                    Non-null = only this album's photos.
     */
    fun loadPhotos(context: Context, folderName: String?) {

        viewModelScope.launch {

            _uiState.value = PhotosUiState.Loading

            val images = repository.getImages(context, folderName)

            _uiState.value = if (images.isEmpty()) {
                PhotosUiState.Empty
            } else {
                // Turn the flat, date-sorted image list into a list with
                // date headers inserted, ready for the adapter.
                val groupedItems = DateGroupingUtil.groupByDate(images)
                PhotosUiState.Success(groupedItems)
            }
        }
    }
}