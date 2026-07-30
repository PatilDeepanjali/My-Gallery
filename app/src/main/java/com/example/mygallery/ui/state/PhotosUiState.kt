package com.example.mygallery.ui.state

import com.example.mygallery.model.ImageModel
import com.example.mygallery.model.PhotoListItem

sealed class PhotosUiState {
    object Loading : PhotosUiState()
    object Empty : PhotosUiState()
    data class Success(val items: List<PhotoListItem>) : PhotosUiState()
}