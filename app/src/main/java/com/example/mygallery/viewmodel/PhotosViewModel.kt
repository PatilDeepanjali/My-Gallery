package com.example.mygallery.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygallery.model.DeleteResult
import com.example.mygallery.model.ImageModel
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.photo.menu.SortOrder
import com.example.mygallery.ui.photo.menu.SortType
import com.example.mygallery.ui.state.PhotosUiState
import com.example.mygallery.utils.DateGroupingUtil
import com.example.mygallery.utils.FavoritePreferences
import kotlinx.coroutines.launch

class PhotosViewModel(
     val repository: GalleryRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<PhotosUiState>()
    val uiState: LiveData<PhotosUiState> = _uiState

    // ---------------------------------------------------------
    // Current photos
    // ---------------------------------------------------------

    private var allPhotos = listOf<ImageModel>()


    // ---------------------------------------------------------
    // Selection Mode
    // ---------------------------------------------------------

    private val _isSelectionMode = MutableLiveData(false)
    val isSelectionMode: LiveData<Boolean> = _isSelectionMode

    private val _selectedPhotoIds =
        MutableLiveData<Set<Long>>(emptySet())

    val selectedPhotoIds: LiveData<Set<Long>> =
        _selectedPhotoIds


    // ---------------------------------------------------------
    // Load Photos
    // ---------------------------------------------------------

    /**
     * folderName == null → all photos
     * folderName != null → photos from that album
     */
    fun loadPhotos(
        context: Context,
        folderName: String?,
        sortType: SortType,
        sortOrder: SortOrder
    ) {

        viewModelScope.launch {

            _uiState.value = PhotosUiState.Loading

            val images = repository.getImages(
                context,
                folderName
            )

            if (images.isEmpty()) {

                allPhotos = emptyList()

                _uiState.value = PhotosUiState.Empty

                return@launch
            }


            // -------------------------------------------------
            // Sort
            // -------------------------------------------------

            val sortedImages = when (sortType) {

                SortType.DATE_TAKEN,
                SortType.LAST_MODIFIED -> {

                    if (sortOrder == SortOrder.ASCENDING) {
                        images.sortedBy { it.dateAdded }
                    } else {
                        images.sortedByDescending { it.dateAdded }
                    }
                }

                SortType.ALBUM_NAME -> {

                    if (sortOrder == SortOrder.ASCENDING) {
                        images.sortedBy {
                            it.folderName.lowercase()
                        }
                    } else {
                        images.sortedByDescending {
                            it.folderName.lowercase()
                        }
                    }
                }

                SortType.SIZE -> {

                    if (sortOrder == SortOrder.ASCENDING) {
                        images.sortedBy { it.size }
                    } else {
                        images.sortedByDescending { it.size }
                    }
                }
            }


            // Keep the sorted photos.
            // Selection will use photo IDs from this list.
            allPhotos = sortedImages


            // -------------------------------------------------
            // Group by date
            // -------------------------------------------------

            val groupedItems =
                DateGroupingUtil.groupByDate(sortedImages)

            _uiState.value =
                PhotosUiState.Success(groupedItems)
        }
    }


    /**
     * Loads only the photos the user has marked favorite, across ALL
     * albums (not just one folder). MediaStore has no concept of
     * "favorite" — this reuses the same "load everything" query as
     * loadPhotos(folderName = null), then filters client-side against
     * FavoritePreferences, then runs the SAME sort + date-grouping
     * pipeline as loadPhotos(). No new query type or state shape needed.
     */
    fun loadFavorites(
        context: Context,
        sortType: SortType,
        sortOrder: SortOrder
    ) {

        viewModelScope.launch {

            _uiState.value = PhotosUiState.Loading

            val allImages = repository.getImages(context, folderName = null)

            val favoriteIds = FavoritePreferences.getFavoriteIds(context)

            val favoriteImages = allImages.filter { it.id in favoriteIds }

            if (favoriteImages.isEmpty()) {
                allPhotos = emptyList()
                _uiState.value = PhotosUiState.Empty
                return@launch
            }

            val sortedImages = sortImages(favoriteImages, sortType, sortOrder)

            allPhotos = sortedImages

            val groupedItems = DateGroupingUtil.groupByDate(sortedImages)

            _uiState.value = PhotosUiState.Success(groupedItems)
        }
    }

    /**
     * Shared sort logic used by BOTH loadPhotos() and loadFavorites()
     * — pulled out so the same sort rules apply identically regardless
     * of which screen you're sorting.
     */
    private fun sortImages(
        images: List<ImageModel>,
        sortType: SortType,
        sortOrder: SortOrder
    ): List<ImageModel> {

        return when (sortType) {

            SortType.DATE_TAKEN,
            SortType.LAST_MODIFIED -> {
                if (sortOrder == SortOrder.ASCENDING) {
                    images.sortedBy { it.dateAdded }
                } else {
                    images.sortedByDescending { it.dateAdded }
                }
            }

            SortType.ALBUM_NAME -> {
                if (sortOrder == SortOrder.ASCENDING) {
                    images.sortedBy { it.folderName.lowercase() }
                } else {
                    images.sortedByDescending { it.folderName.lowercase() }
                }
            }

            SortType.SIZE -> {
                if (sortOrder == SortOrder.ASCENDING) {
                    images.sortedBy { it.size }
                } else {
                    images.sortedByDescending { it.size }
                }
            }
        }
    }



    // ---------------------------------------------------------
    // Selection Mode Functions
    // ---------------------------------------------------------

    /**
     * Long pressing a photo while not in selection mode
     * enters selection mode and selects that photo.
     */
    fun enterSelectionMode(photo: ImageModel) {

        _isSelectionMode.value = true

        _selectedPhotoIds.value = setOf(photo.id)
    }


    /**
     * Tapping a photo while in selection mode toggles
     * its selected/unselected state.
     */
    fun toggleSelection(photo: ImageModel) {

        val current =
            _selectedPhotoIds.value ?: emptySet()

        val updated = if (current.contains(photo.id)) {

            current - photo.id

        } else {

            current + photo.id
        }

        _selectedPhotoIds.value = updated

        // If nothing remains selected,
        // leave selection mode.
        if (updated.isEmpty()) {

            _isSelectionMode.value = false
        }
    }


    /**
     * Exit selection mode and clear all selections.
     */
    fun exitSelectionMode() {

        _isSelectionMode.value = false

        _selectedPhotoIds.value = emptySet()
    }


    /**
     * Returns the actual ImageModel objects
     * corresponding to the selected IDs.
     *
     * This will be useful later for:
     * Copy
     * Move
     * Share
     * Delete
     * Favorite
     * Details
     */
    fun getSelectedPhotos(): List<ImageModel> {

        val selectedIds =
            _selectedPhotoIds.value ?: emptySet()

        return allPhotos.filter {
            it.id in selectedIds
        }
    }


    // ---------------------------------------------------------
    // Delete
    // ---------------------------------------------------------

    fun deleteImages(
        context: Context,
        uris: List<android.net.Uri>,
        onResult: (DeleteResult) -> Unit
    ) {

        viewModelScope.launch {

            val result =
                repository.deleteImages(
                    context,
                    uris
                )

            onResult(result)
        }
    }
}