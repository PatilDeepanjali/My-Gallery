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

    // ---------- Selection Mode state ----------

    private val _isSelectionMode = MutableLiveData(false)
    val isSelectionMode: LiveData<Boolean> = _isSelectionMode

    // We track SELECTED folders by folderName (a String), not the whole
    // GalleryFolder object. Folder names are unique and lightweight —
    // storing full objects in a Set would also work, but comparing/
    // hashing plain Strings is simpler and avoids relying on GalleryFolder
    // having correct equals()/hashCode() implementations.
    private val _selectedFolderNames = MutableLiveData<Set<String>>(emptySet())
    val selectedFolderNames: LiveData<Set<String>> = _selectedFolderNames

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

    // ---------- Selection Mode functions ----------

    /**
     * Long-pressing a folder while NOT already in selection mode enters
     * it, and selects that first folder.
     */
    fun enterSelectionMode(folder: GalleryFolder) {
        _isSelectionMode.value = true
        _selectedFolderNames.value = setOf(folder.folderName)
    }

    /**
     * Tapping a folder while IN selection mode toggles its checkbox.
     * If this toggle empties the selection entirely, we automatically
     * exit selection mode — there's nothing left "selected" to act on.
     */
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

    /**
     * Resolves the currently selected folder NAMES back into full
     * GalleryFolder objects (with their image lists), so actions like
     * Delete/Move/Details have the real data to work with.
     */
    fun getSelectedFolders(): List<GalleryFolder> {
        val selectedNames = _selectedFolderNames.value ?: emptySet()
        return allAlbums.filter { it.folderName in selectedNames }
    }

}