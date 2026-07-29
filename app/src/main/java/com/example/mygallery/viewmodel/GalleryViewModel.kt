package com.example.mygallery.viewmodel

import android.content.Context
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mygallery.model.GalleryFolder
import com.example.mygallery.repository.GalleryRepository

class GalleryViewModel (val repository: GalleryRepository): ViewModel() {


     val folders = MutableLiveData<ArrayList<GalleryFolder>>()

    fun loadFolders(context: Context)
    {
    val folderList = repository.getAllFolders(context)

        folders.value=folderList
    }
}


