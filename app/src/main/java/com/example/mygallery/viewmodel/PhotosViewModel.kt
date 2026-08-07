package com.example.mygallery.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.photo.SortOrder
import com.example.mygallery.ui.photo.SortType
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
    fun loadPhotos(
        context: Context,
        folderName: String?,
        sortType: SortType,
        sortOrder: SortOrder
    ) {

        viewModelScope.launch {

            _uiState.value = PhotosUiState.Loading

            val images = repository.getImages(context, folderName)

            val sortedImages = when (sortType) {

                SortType.DATE_TAKEN,
                SortType.LAST_MODIFIED -> {

                    if (sortOrder == SortOrder.ASCENDING)
                        images.sortedBy { it.dateAdded }
                    else
                        images.sortedByDescending { it.dateAdded }

                }

                SortType.ALBUM_NAME -> {

                    if (sortOrder == SortOrder.ASCENDING)
                        images.sortedBy { it.folderName.lowercase() }
                    else
                        images.sortedByDescending { it.folderName.lowercase() }

                }

                SortType.SIZE -> {

                    if (sortOrder == SortOrder.ASCENDING)
                        images.sortedBy { it.size }
                    else
                        images.sortedByDescending { it.size }

                }

            }


            _uiState.value = if (images.isEmpty()) {
                PhotosUiState.Empty
            } else {
                // Turn the flat, date-sorted image list into a list with
                // date headers inserted, ready for the adapter.
                val groupedItems = DateGroupingUtil.groupByDate(sortedImages)
                PhotosUiState.Success(groupedItems)
            }
        }
    }
}