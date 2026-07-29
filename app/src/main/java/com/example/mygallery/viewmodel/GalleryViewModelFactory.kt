package com.example.mygallery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mygallery.repository.GalleryRepository

class GalleryViewModelFactory(private val repository: GalleryRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(GalleryViewModel::class.java))
        {
            return GalleryViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknow model Class")
    }
}