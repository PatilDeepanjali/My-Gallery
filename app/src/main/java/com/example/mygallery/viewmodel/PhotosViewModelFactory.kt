package com.example.mygallery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mygallery.repository.GalleryRepository


class PhotosViewModelFactory(
    private val repository: GalleryRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhotosViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}