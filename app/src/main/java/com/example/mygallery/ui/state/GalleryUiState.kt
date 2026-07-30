package com.example.mygallery.ui.state

import com.example.mygallery.model.GalleryFolder

sealed class GalleryUiState {
    object PermissionDenied: GalleryUiState()
    object Loading: GalleryUiState()
    object Empty: GalleryUiState()
    data class Success(val folder: List<GalleryFolder>): GalleryUiState()
}