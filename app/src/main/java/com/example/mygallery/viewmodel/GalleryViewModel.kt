package com.example.mygallery.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygallery.model.DeleteResult
import com.example.mygallery.model.GalleryFolder
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.state.GalleryUiState
import com.example.mygallery.utils.PinPreferences
import kotlinx.coroutines.launch

class GalleryViewModel(val repository: GalleryRepository) : ViewModel() {


    private var allAlbums = listOf<GalleryFolder>()

    private val _filteredAlbums = MutableLiveData<List<GalleryFolder>>()
    val filteredAlbums: LiveData<List<GalleryFolder>> = _filteredAlbums


    private val _uiState = MutableLiveData<GalleryUiState>()
    val uiState: LiveData<GalleryUiState> = _uiState

    // ---------- Selection Mode state ----------

    private val _isSelectionMode = MutableLiveData(false)
    val isSelectionMode: LiveData<Boolean> = _isSelectionMode

    private val _selectedFolderNames = MutableLiveData<Set<String>>(emptySet())
    val selectedFolderNames: LiveData<Set<String>> = _selectedFolderNames

    fun onPermissionDenied() {
        _uiState.value = GalleryUiState.PermissionDenied
    }

    fun loadFolders(context: Context) {

        viewModelScope.launch {

            _uiState.value = GalleryUiState.Loading

            val realFolders = repository.getAllFolders(context)

            // Bring in placeholder entries for custom albums that don't
            // have any real photos yet — without this, an album you
            // just created with "+" would never appear on this screen,
            // only in the Move/Copy picker (which did this merge
            // separately).
            val mergedList = repository.mergeCustomAlbums(context, realFolders)

            // Pinned folders float to the top.
            val sortedList = mergedList.sortedByDescending { folder ->
                PinPreferences.isPinned(context, folder.folderName)
            }

            allAlbums = sortedList
            _filteredAlbums.value = sortedList

            _uiState.value = if (sortedList.isEmpty()) {
                GalleryUiState.Empty
            } else {
                GalleryUiState.Success(sortedList)
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

    // ---------- Selection Mode functions ----------

    fun enterSelectionMode(folder: GalleryFolder) {
        _isSelectionMode.value = true
        _selectedFolderNames.value = setOf(folder.folderName)
    }

    fun toggleSelection(folder: GalleryFolder) {

        val current = _selectedFolderNames.value ?: emptySet()

        val updated = if (current.contains(folder.folderName)) {
            current - folder.folderName
        } else {
            current + folder.folderName
        }

        _selectedFolderNames.value = updated

        if (updated.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedFolderNames.value = emptySet()
    }

    fun getSelectedFolders(): List<GalleryFolder> {
        val selectedNames = _selectedFolderNames.value ?: emptySet()
        return allAlbums.filter { it.folderName in selectedNames }
    }

    // ---------- Album Sort ----------

    fun applySort(
        context: Context,
        sortType: com.example.mygallery.ui.album.AlbumSortType,
        sortOrder: com.example.mygallery.ui.photo.menu.SortOrder
    ) {
        val baseComparator: Comparator<GalleryFolder> = when (sortType) {
            com.example.mygallery.ui.album.AlbumSortType.NAME ->
                compareBy { folder -> folder.folderName.lowercase() }

            com.example.mygallery.ui.album.AlbumSortType.ITEM_COUNT ->
                compareBy { folder -> folder.imageCount }

            com.example.mygallery.ui.album.AlbumSortType.SIZE ->
                compareBy { folder -> folder.imageList.sumOf { it.size } }

            com.example.mygallery.ui.album.AlbumSortType.DATE_ADDED ->
                compareBy { folder -> folder.imageList.maxOfOrNull { it.dateAdded } ?: 0L }
        }

        val orderedComparator =
            if (sortOrder == com.example.mygallery.ui.photo.menu.SortOrder.DESCENDING)
                baseComparator.reversed()
            else
                baseComparator

        val pinnedFirstComparator =
            compareByDescending<GalleryFolder> { folder -> PinPreferences.isPinned(context, folder.folderName) }
                .then(orderedComparator)

        val sorted = allAlbums.sortedWith(pinnedFirstComparator)

        allAlbums = sorted
        _filteredAlbums.value = sorted
        _uiState.value = GalleryUiState.Success(sorted)
    }

    // ---------- Delete ----------

    fun deleteImages(context: Context, uris: List<Uri>, onResult: (DeleteResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteImages(context, uris)
            onResult(result)
        }
    }

}