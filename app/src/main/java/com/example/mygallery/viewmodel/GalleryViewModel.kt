package com.example.mygallery.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygallery.model.GalleryFolder
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.state.GalleryUiState
import kotlinx.coroutines.launch

class GalleryViewModel(val repository: GalleryRepository) : ViewModel() {


    private var allAlbums = listOf<GalleryFolder>()

    private val _filteredAlbums = MutableLiveData<List<GalleryFolder>>()
    val filteredAlbums: LiveData<List<GalleryFolder>> = _filteredAlbums


    private val _uiState = MutableLiveData<GalleryUiState>()
    val uiState: LiveData<GalleryUiState> = _uiState
//     val folders = MutableLiveData<ArrayList<GalleryFolder>>()

    fun onPermissionDenied() {
        _uiState.value = GalleryUiState.PermissionDenied
    }

    fun loadFolders(context: Context) {

        viewModelScope.launch {

            _uiState.value = GalleryUiState.Loading // Loading state

            val folderList = repository.getAllFolders(context)

            // Save original list
            allAlbums = folderList

            // Display all albums initially
            _filteredAlbums.value = folderList

            _uiState.value = if (folderList.isEmpty()) {
                GalleryUiState.Empty
            } else {
                GalleryUiState.Success(folderList)
            }

        }

    }


    fun createAlbum(context: Context, albumName: String): Result<String> {

        return repository.createAlbum(context, albumName)

    }

    fun searchAlbum(query: String) {

        _filteredAlbums.value =
            repository.searchAlbums(allAlbums, query)

    }


}


