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

            // Pinned folders float to the top. sortedByDescending on a
            // Boolean puts `true` (pinned) before `false` — a common
            // small trick worth remembering.
            val sortedList = folderList.sortedByDescending { folder ->
                PinPreferences.isPinned(context, folder.folderName)
            }


            // Save original list
            allAlbums = sortedList

            // Display all albums initially
            _filteredAlbums.value = sortedList

            _uiState.value = if (folderList.isEmpty()) {
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



    // ---------- Album Sort ----------

    /**
     * Re-sorts the already-loaded album list in memory (no new
     * MediaStore query needed) according to the user's chosen
     * criteria — while still keeping pinned albums first, same as
     * loadFolders() does by default. Pin priority + user sort combine
     * via Comparator.then(), so pinned albums stay pinned-first, and
     * WITHIN each group (pinned / not pinned) the chosen sort applies.
     */
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

    /**
     * Thin wrapper around the Repository's deleteImages(). Kept as a
     * plain callback (not LiveData) since this is a one-shot action,
     * not ongoing screen state — the Fragment just needs to know once
     * what happened, not observe it continuously.
     */
    fun deleteImages(context: Context, uris: List<Uri>, onResult: (DeleteResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteImages(context, uris)
            onResult(result)
        }
    }

}