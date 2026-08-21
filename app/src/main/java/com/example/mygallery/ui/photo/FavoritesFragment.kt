package com.example.mygallery.ui.photo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager

import com.example.mygallery.R
import com.example.mygallery.adapter.PhotosAdapter
import com.example.mygallery.databinding.FragmentFavoritesBinding
import com.example.mygallery.model.ImageModel
import com.example.mygallery.model.PhotoListItem
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.MainActivity
import com.example.mygallery.ui.photo.menu.PhotoAction
import com.example.mygallery.ui.photo.menu.PhotoActionPopup
import com.example.mygallery.ui.photo.menu.SortOrder
import com.example.mygallery.ui.photo.menu.SortType
import com.example.mygallery.ui.photo.selection.PhotoSelectionAction
import com.example.mygallery.ui.photo.selection.PhotoSelectionActionPopup
import com.example.mygallery.ui.photo.slideshow.SlideShowFragment
import com.example.mygallery.ui.state.PhotosUiState
import com.example.mygallery.utils.FavoritePreferences
import com.example.mygallery.viewmodel.PhotosViewModel
import com.example.mygallery.viewmodel.PhotosViewModelFactory

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.widget.EditText
import android.widget.LinearLayout

import androidx.lifecycle.lifecycleScope
import com.example.mygallery.ui.album.AlbumPickerBottomSheet

import com.example.mygallery.ui.photo.details.PhotoDetailsBottomSheet
import kotlinx.coroutines.launch
import android.content.ContentValues
import android.provider.MediaStore

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PhotosViewModel
    private lateinit var photosAdapter: PhotosAdapter

    // ---------------------------------------------------------
    // Current favorite photos
    // ---------------------------------------------------------

    private val photoList =
        ArrayList<ImageModel>()


    // ---------------------------------------------------------
    // Sort
    // ---------------------------------------------------------

    private var currentSortType =
        SortType.DATE_TAKEN

    private var currentSortOrder =
        SortOrder.DESCENDING


    // =========================================================
    // CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val repository =
            GalleryRepository()

        val factory =
            PhotosViewModelFactory(repository)

        viewModel =
            ViewModelProvider(
                this,
                factory
            )[PhotosViewModel::class.java]
    }


    // =========================================================
    // CREATE VIEW
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentFavoritesBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }


    // =========================================================
    // VIEW CREATED
    // =========================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )


        // -----------------------------------------------------
        // Android back
        // -----------------------------------------------------

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {

                    override fun handleOnBackPressed() {

                        if (
                            viewModel.isSelectionMode.value == true
                        ) {

                            viewModel.exitSelectionMode()

                        } else {

                            isEnabled = false

                            requireActivity()
                                .onBackPressedDispatcher
                                .onBackPressed()
                        }
                    }
                }
            )


        // -----------------------------------------------------
        // Setup
        // -----------------------------------------------------

        setupObservers()

        setupBasicViews()

        setupNormalMenu()

        setupSelectionHeader()


        // -----------------------------------------------------
        // Load favorites
        // -----------------------------------------------------

        viewModel.loadFavorites(
            requireContext(),
            currentSortType,
            currentSortOrder
        )
    }


    // =========================================================
    // RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()

        (activity as? MainActivity)
            ?.showBottomNavigation()


        if (::photosAdapter.isInitialized) {

            photosAdapter.setSelectionState(
                viewModel.isSelectionMode.value == true,
                viewModel.selectedPhotoIds.value
                    ?: emptySet()
            )

            updateSelectionMode(
                viewModel.isSelectionMode.value == true
            )

            updateSelectionCount(
                viewModel.selectedPhotoIds.value
                    ?: emptySet()
            )
        }
    }


    // =========================================================
    // BASIC VIEWS
    // =========================================================

    private fun setupBasicViews() {

        binding.btnBack.setOnClickListener {

            parentFragmentManager
                .popBackStack()
        }
    }


    // =========================================================
    // NORMAL MENU
    // =========================================================

    /**
     * Uses the SAME custom menu as PhotoFragment.
     *
     * Design:
     *
     *   ◉ Selected       >
     *   ↕ Sort By        >
     *   ▣ Slide Show     >
     */
    private fun setupNormalMenu() {

        binding.btnMenu.setOnClickListener {

            PhotoActionPopup.show(
                requireContext(),
                binding.btnMenu
            ) { action ->

                when (action) {

                    // -------------------------------------------------
                    // SELECT
                    // -------------------------------------------------

                    PhotoAction.SELECT -> {

                        enterSelectionMode()
                    }


                    // -------------------------------------------------
                    // SORT
                    // -------------------------------------------------

                    PhotoAction.SORT -> {

                        showSortBottomSheet()
                    }


                    // -------------------------------------------------
                    // SLIDE SHOW
                    // -------------------------------------------------

                    PhotoAction.SLIDE_SHOW -> {

                        startSlideShow(
                            photoList,
                            0
                        )
                    }


                    /*
                     * These actions belong to PhotoFragment's
                     * general menu but are not needed on Favorites.
                     */
                    PhotoAction.PIN -> {
                        // Not applicable to Favorites
                    }

                    PhotoAction.FILTER -> {
                        // Not applicable to Favorites
                    }

                    PhotoAction.LAYOUT_STYLE -> {
                        // Keep Favorites in the Figma grid layout
                    }

                    PhotoAction.COLUMN -> {
                        // Keep Favorites in the Figma grid layout
                    }
                }
            }
        }
    }


    // =========================================================
    // SORT
    // =========================================================

    private fun showSortBottomSheet() {

        val sheet =
            SortBottomSheet(
                currentSortType,
                currentSortOrder
            )


        sheet.setListener(
            object :
                SortBottomSheet.OnSortSelected {

                override fun onSortSelected(
                    sortType: SortType,
                    sortOrder: SortOrder
                ) {

                    currentSortType =
                        sortType

                    currentSortOrder =
                        sortOrder


                    viewModel.exitSelectionMode()


                    viewModel.loadFavorites(
                        requireContext(),
                        currentSortType,
                        currentSortOrder
                    )
                }
            }
        )


        sheet.show(
            parentFragmentManager,
            "favorites_sort"
        )
    }


    // =========================================================
    // SELECTION HEADER
    // =========================================================

    private fun setupSelectionHeader() {

        // -----------------------------------------------------
        // Close selection
        // -----------------------------------------------------

        binding.btnExitSelection.setOnClickListener {

            viewModel.exitSelectionMode()
        }


        // -----------------------------------------------------
        // Selection menu
        // -----------------------------------------------------

        binding.btnSelectionMenu.setOnClickListener {

            showSelectionMenu()
        }
    }


    // =========================================================
    // ENTER SELECTION
    // =========================================================

    private fun enterSelectionMode() {

        if (photoList.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "No favorite photos",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        /*
         * The ViewModel enters selection mode with
         * the first photo selected.
         *
         * The user can then tap it to deselect it.
         */
        viewModel.enterSelectionMode(
            photoList.first()
        )
    }


    // =========================================================
    // SELECTION MENU
    // =========================================================

    /**
     * Uses the SAME selection popup as PhotoFragment.
     *
     * The popup design is therefore identical to the
     * normal PhotoFragment selection menu.
     */
    private fun showSelectionMenu() {

        val selectedCount =
            viewModel.selectedPhotoIds.value?.size ?: 0

        if (selectedCount == 0) {
            return
        }

        PhotoSelectionActionPopup.show(
            requireContext(),
            binding.btnSelectionMenu,
            selectedCount
        ) { action ->

            when (action) {

                // =================================================
                // COPY
                // =================================================

                PhotoSelectionAction.COPY -> {

                    showPhotoAlbumPicker(
                        AlbumPickerBottomSheet.Mode.COPY
                    )
                }


                // =================================================
                // MOVE
                // =================================================

                PhotoSelectionAction.MOVE -> {

                    showPhotoAlbumPicker(
                        AlbumPickerBottomSheet.Mode.MOVE
                    )
                }


                // =================================================
                // RENAME
                // =================================================

                PhotoSelectionAction.RENAME -> {

                    val selected =
                        viewModel.getSelectedPhotos()

                    if (selected.size == 1) {

                        showRenameDialog(
                            selected.first()
                        )

                    } else {

                        Toast.makeText(
                            requireContext(),
                            "Select only one photo to rename",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                // =================================================
                // FAVORITE
                // =================================================
                // On Favorites screen this means:
                // REMOVE FROM FAVORITES
                // =================================================

                PhotoSelectionAction.FAVORITE -> {

                    unfavoriteSelected()
                }


                // =================================================
                // SLIDE SHOW
                // =================================================

                PhotoSelectionAction.SLIDE_SHOW -> {

                    val selected =
                        viewModel.getSelectedPhotos()

                    if (selected.isNotEmpty()) {

                        startSlideShow(
                            selected,
                            0
                        )
                    }
                }


                // =================================================
                // EDIT WITH
                // =================================================

                PhotoSelectionAction.EDIT_WITH -> {

                    val selected =
                        viewModel.getSelectedPhotos()

                    if (selected.size == 1) {

                        openPhotoForEdit(
                            selected.first()
                        )

                    } else {

                        Toast.makeText(
                            requireContext(),
                            "Select one photo to edit",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                // =================================================
                // SET AS WALLPAPER
                // =================================================

                PhotoSelectionAction.SET_AS_WALLPAPER -> {

                    val selected =
                        viewModel.getSelectedPhotos()

                    if (selected.size == 1) {

                        showWallpaperDialog(
                            selected.first()
                        )

                    } else {

                        Toast.makeText(
                            requireContext(),
                            "Select one photo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                // =================================================
                // SHARE
                // =================================================

                PhotoSelectionAction.SHARE -> {

                    shareSelectedPhotos()
                }


                // =================================================
                // DELETE
                // =================================================
                // Favorites → Custom Trash
                // =================================================

                PhotoSelectionAction.DELETE -> {

                    confirmAndMoveToTrash()
                }


                // =================================================
                // DETAILS
                // =================================================

                PhotoSelectionAction.DETAILS -> {

                    showSelectedDetails()
                }


                // =================================================
                // OPEN WITH
                // =================================================

                PhotoSelectionAction.OPEN_WITH -> {

                    val selected =
                        viewModel.getSelectedPhotos()

                    if (selected.size == 1) {

                        openPhotoWith(
                            selected.first()
                        )

                    } else {

                        Toast.makeText(
                            requireContext(),
                            "Select one photo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }


    private fun openPhotoWith(
        photo: ImageModel
    ) {

        val mimeType =
            requireContext()
                .contentResolver
                .getType(photo.uri)
                ?: photo.mimeType
                    .ifBlank { "image/*" }

        val intent =
            Intent(
                Intent.ACTION_VIEW
            ).apply {

                setDataAndType(
                    photo.uri,
                    mimeType
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        try {

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "No app can open this photo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }



    private fun renamePhoto(
        photo: ImageModel,
        newName: String
    ) {

        try {

            val extension =
                photo.name.substringAfterLast(
                    ".",
                    ""
                )

            val finalName =
                if (
                    extension.isNotEmpty() &&
                    !newName.contains(".")
                ) {

                    "$newName.$extension"

                } else {

                    newName
                }

            val values =
                ContentValues().apply {

                    put(
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        finalName
                    )
                }

            val updated =
                requireContext()
                    .contentResolver
                    .update(
                        photo.uri,
                        values,
                        null,
                        null
                    )

            if (updated > 0) {

                // Update local list
                val index =
                    photoList.indexOfFirst {
                        it.id == photo.id
                    }

                if (index != -1) {

                    photoList[index] =
                        photoList[index].copy(
                            name = finalName
                        )
                }

                Toast.makeText(
                    requireContext(),
                    "Renamed successfully",
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.exitSelectionMode()

                viewModel.loadFavorites(
                    requireContext(),
                    currentSortType,
                    currentSortOrder
                )

            } else {

                Toast.makeText(
                    requireContext(),
                    "Rename failed",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "Rename failed: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun openPhotoForEdit(
        photo: ImageModel
    ) {

        val mimeType =
            requireContext()
                .contentResolver
                .getType(photo.uri)
                ?: "image/*"

        val intent =
            Intent(
                Intent.ACTION_EDIT
            ).apply {

                setDataAndType(
                    photo.uri,
                    mimeType
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        try {

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "No editing app found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }



    private fun showWallpaperDialog(
        photo: ImageModel
    ) {

        val options =
            arrayOf(
                "Home screen",
                "Lock screen",
                "Home & Lock screen"
            )

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle(
                "Set as wallpaper"
            )
            .setItems(options) { _, which ->

                when (which) {

                    0 -> setWallpaper(
                        photo,
                        WallpaperManager.FLAG_SYSTEM
                    )

                    1 -> setWallpaper(
                        photo,
                        WallpaperManager.FLAG_LOCK
                    )

                    2 -> setWallpaper(
                        photo,
                        WallpaperManager.FLAG_SYSTEM or
                                WallpaperManager.FLAG_LOCK
                    )
                }
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }


    private fun setWallpaper(
        photo: ImageModel,
        flags: Int
    ) {

        try {

            val wallpaperManager =
                WallpaperManager.getInstance(
                    requireContext()
                )

            requireContext()
                .contentResolver
                .openInputStream(
                    photo.uri
                )
                ?.use { inputStream ->

                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.N
                    ) {

                        wallpaperManager.setStream(
                            inputStream,
                            null,
                            true,
                            flags
                        )

                    } else {

                        wallpaperManager.setStream(
                            inputStream
                        )
                    }
                }

            Toast.makeText(
                requireContext(),
                "Wallpaper set successfully",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "Failed to set wallpaper: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun showSelectedDetails() {

        val selected =
            viewModel.getSelectedPhotos()

        if (selected.isEmpty()) {
            return
        }

        PhotoDetailsBottomSheet(
            ArrayList(selected)
        ).show(
            childFragmentManager,
            "favorite_photo_details"
        )
    }


    private fun showRenameDialog(
        photo: ImageModel
    ) {

        val editText =
            EditText(requireContext()).apply {

                setSingleLine(true)

                setText(
                    photo.name.substringBeforeLast(
                        ".",
                        photo.name
                    )
                )

                selectAll()
            }

        val container =
            LinearLayout(
                requireContext()
            ).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    24,
                    0,
                    24,
                    0
                )

                addView(editText)
            }

        val dialog =
            AlertDialog.Builder(
                requireContext()
            )
                .setTitle("Rename")
                .setView(container)
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "Done",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val newName =
                    editText.text
                        .toString()
                        .trim()

                if (newName.isEmpty()) {

                    editText.error =
                        "Name cannot be empty"

                    return@setOnClickListener
                }

                renamePhoto(
                    photo,
                    newName,
                )
            }
        }

        dialog.show()
    }





    private fun performPhotoCopy(
        photos: List<ImageModel>,
        destinationAlbumName: String
    ) {

        val uris =
            photos.map {
                it.uri
            }

        if (uris.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "No photos to copy",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            val result =
                viewModel.repository.copyImages(
                    requireContext(),
                    uris,
                    destinationAlbumName
                )

            if (result.isSuccess) {

                val copiedCount =
                    result.getOrNull() ?: 0

                Toast.makeText(
                    requireContext(),
                    "Copied $copiedCount item(s) to $destinationAlbumName",
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.exitSelectionMode()

                // IMPORTANT:
                // Favorites must reload favorites,
                // not normal photos.
                viewModel.loadFavorites(
                    requireContext(),
                    currentSortType,
                    currentSortOrder
                )

            } else {

                Toast.makeText(
                    requireContext(),
                    "Copy failed: ${
                        result.exceptionOrNull()?.message
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun showPhotoAlbumPicker(
        mode: AlbumPickerBottomSheet.Mode
    ) {

        val selected =
            viewModel.getSelectedPhotos()

        if (selected.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "No photos selected",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val excludedNames =
            selected
                .map { it.folderName }
                .distinct()

        val sheet =
            AlbumPickerBottomSheet.newInstance(
                mode,
                excludedNames
            )

        sheet.onAlbumSelected =
            { destinationAlbumName ->

                if (
                    mode ==
                    AlbumPickerBottomSheet.Mode.COPY
                ) {

                    performPhotoCopy(
                        selected,
                        destinationAlbumName
                    )

                } else {

                    performPhotoMove(
                        selected,
                        destinationAlbumName
                    )
                }
            }

        sheet.show(
            childFragmentManager,
            "FavoriteAlbumPicker"
        )
    }

    private fun performPhotoMove(
        photos: List<ImageModel>,
        destinationAlbumName: String
    ) {

        val uris =
            photos.map { it.uri }

        if (uris.isEmpty()) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            val copyResult =
                viewModel.repository.copyImages(
                    requireContext(),
                    uris,
                    destinationAlbumName
                )

            if (!copyResult.isSuccess) {

                Toast.makeText(
                    requireContext(),
                    "Move failed: ${
                        copyResult.exceptionOrNull()?.message
                    }",
                    Toast.LENGTH_LONG
                ).show()

                return@launch
            }

            viewModel.deleteImages(
                requireContext(),
                uris
            ) { deleteResult ->

                if (deleteResult is com.example.mygallery.model.DeleteResult.Success) {

                    Toast.makeText(
                        requireContext(),
                        "Moved to $destinationAlbumName",
                        Toast.LENGTH_SHORT
                    ).show()

                    viewModel.exitSelectionMode()

                    viewModel.loadFavorites(
                        requireContext(),
                        currentSortType,
                        currentSortOrder
                    )

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Move completed partially. Check your photos.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // =========================================================
    // UNFAVORITE
    // =========================================================

    private fun unfavoriteSelected() {

        val selected =
            viewModel.getSelectedPhotos()


        if (selected.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "No photos selected",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        selected.forEach { photo ->

            if (
                FavoritePreferences.isFavorite(
                    requireContext(),
                    photo.id
                )
            ) {

                FavoritePreferences.toggleFavorite(
                    requireContext(),
                    photo.id
                )
            }
        }


        Toast.makeText(
            requireContext(),
            "Removed from Favorites",
            Toast.LENGTH_SHORT
        ).show()


        viewModel.exitSelectionMode()


        viewModel.loadFavorites(
            requireContext(),
            currentSortType,
            currentSortOrder
        )
    }


    // =========================================================
    // SHARE
    // =========================================================

    private fun shareSelectedPhotos() {

        val selected =
            viewModel.getSelectedPhotos()


        if (selected.isEmpty()) {
            return
        }


        // -----------------------------------------------------
        // Single photo
        // -----------------------------------------------------

        if (selected.size == 1) {

            val photo =
                selected.first()


            val mimeType =
                requireContext()
                    .contentResolver
                    .getType(
                        photo.uri
                    )
                    ?: "image/*"


            val intent =
                Intent(
                    Intent.ACTION_SEND
                ).apply {

                    type =
                        mimeType

                    putExtra(
                        Intent.EXTRA_STREAM,
                        photo.uri
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }


            startActivity(
                Intent.createChooser(
                    intent,
                    "Share Photo"
                )
            )

            return
        }


        // -----------------------------------------------------
        // Multiple photos
        // -----------------------------------------------------

        val uris =
            ArrayList<Uri>()


        selected.forEach { photo ->

            uris.add(
                photo.uri
            )
        }


        val intent =
            Intent(
                Intent.ACTION_SEND_MULTIPLE
            ).apply {

                type =
                    "image/*"

                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    uris
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }


        startActivity(
            Intent.createChooser(
                intent,
                "Share Photos"
            )
        )
    }


    // =========================================================
    // OPEN WITH
    // =========================================================

    private fun openSelectedPhoto() {

        val selected =
            viewModel.getSelectedPhotos()


        if (selected.isEmpty()) {
            return
        }


        val photo =
            selected.first()


        val mimeType =
            requireContext()
                .contentResolver
                .getType(
                    photo.uri
                )
                ?: "image/*"


        val intent =
            Intent(
                Intent.ACTION_VIEW
            ).apply {

                setDataAndType(
                    photo.uri,
                    mimeType
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }


        try {

            startActivity(intent)

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                requireContext(),
                "No app can open this file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // DELETE → CUSTOM TRASH
    // =========================================================

    /**
     * Favorites uses the same custom Trash system as Photos.
     *
     * This is important for your Android 10 phone because
     * MediaStore.IS_TRASHED is not available there.
     */
    private fun confirmAndMoveToTrash() {

        val selected =
            viewModel.getSelectedPhotos()

        if (selected.isEmpty()) {
            return
        }

        val count =
            selected.size

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle(
                "Move to Trash?"
            )
            .setMessage(
                "$count photo${if (count > 1) "s" else ""} will be moved to Trash."
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Move to Trash"
            ) { _, _ ->

                viewModel.moveImagesToTrash(
                    requireContext(),
                    selected
                ) { result ->

                    if (result.isSuccess) {

                        val moved =
                            result.getOrNull() ?: 0

                        Toast.makeText(
                            requireContext(),
                            "$moved photo${if (moved != 1) "s" else ""} moved to Trash",
                            Toast.LENGTH_SHORT
                        ).show()

                        viewModel.exitSelectionMode()

                        viewModel.loadFavorites(
                            requireContext(),
                            currentSortType,
                            currentSortOrder
                        )

                    } else {

                        Toast.makeText(
                            requireContext(),
                            "Unable to move to Trash: ${
                                result.exceptionOrNull()?.message
                            }",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }


    // =========================================================
    // SLIDESHOW
    // =========================================================

    private fun startSlideShow(
        images: List<ImageModel>,
        position: Int
    ) {

        if (images.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "No favorite photos available",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        val slideshow =
            SlideShowFragment.newInstance(
                ArrayList(images),
                position
            )


        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.frameContainer,
                slideshow
            )
            .addToBackStack(
                "favorite_slideshow"
            )
            .commit()
    }


    // =========================================================
    // OBSERVERS
    // =========================================================

    private fun setupObservers() {

        // -----------------------------------------------------
        // UI State
        // -----------------------------------------------------

        viewModel.uiState.observe(
            viewLifecycleOwner
        ) { state ->

            renderState(state)
        }


        // -----------------------------------------------------
        // Selection Mode
        // -----------------------------------------------------

        viewModel.isSelectionMode.observe(
            viewLifecycleOwner
        ) { isSelecting ->

            updateSelectionMode(
                isSelecting
            )


            if (
                ::photosAdapter.isInitialized
            ) {

                photosAdapter.setSelectionState(
                    isSelecting,
                    viewModel.selectedPhotoIds.value
                        ?: emptySet()
                )
            }
        }


        // -----------------------------------------------------
        // Selected IDs
        // -----------------------------------------------------

        viewModel.selectedPhotoIds.observe(
            viewLifecycleOwner
        ) { selectedIds ->

            updateSelectionCount(
                selectedIds
            )


            if (
                ::photosAdapter.isInitialized
            ) {

                photosAdapter.setSelectionState(
                    viewModel.isSelectionMode.value == true,
                    selectedIds
                )
            }
        }
    }


    // =========================================================
    // RENDER STATE
    // =========================================================

    private fun renderState(
        state: PhotosUiState
    ) {

        binding.progressBar.visibility =
            View.GONE

        binding.layoutEmptyState.visibility =
            View.GONE

        binding.recyclerFavorites.visibility =
            View.GONE


        when (state) {

            // -------------------------------------------------
            // LOADING
            // -------------------------------------------------

            is PhotosUiState.Loading -> {

                binding.progressBar.visibility =
                    View.VISIBLE
            }


            // -------------------------------------------------
            // EMPTY
            // -------------------------------------------------

            is PhotosUiState.Empty -> {

                photoList.clear()


                binding.tvSubtitle.text =
                    "0 Photos"


                binding.layoutEmptyState.visibility =
                    View.VISIBLE


                updateSelectionCount(
                    viewModel.selectedPhotoIds.value
                        ?: emptySet()
                )
            }


            // -------------------------------------------------
            // SUCCESS
            // -------------------------------------------------

            is PhotosUiState.Success -> {

                binding.recyclerFavorites.visibility =
                    View.VISIBLE


                // ---------------------------------------------
                // Extract actual photos.
                //
                // DateHeader items are ignored.
                // ---------------------------------------------

                photoList.clear()


                state.items.forEach { item ->

                    if (
                        item is PhotoListItem.Photo
                    ) {

                        photoList.add(
                            item.image
                        )
                    }
                }


                // ---------------------------------------------
                // Subtitle
                // ---------------------------------------------

                binding.tvSubtitle.text =
                    "${photoList.size} Photos"


                // ---------------------------------------------
                // Adapter
                // ---------------------------------------------

                photosAdapter =
                    PhotosAdapter(
                        isGridView = true,
                        items = state.items,


                        // -------------------------------------
                        // PHOTO CLICK
                        // -------------------------------------

                        onPhotoClick = {
                                photo,
                                _ ->

                            val photoPosition =
                                photoList.indexOf(
                                    photo.image
                                )

                            if (
                                photoPosition < 0
                            ) {
                                return@PhotosAdapter
                            }


                            val preview =
                                PhotoPreviewFragment()


                            preview.arguments =
                                Bundle().apply {

                                    putParcelableArrayList(
                                        PhotoPreviewFragment
                                            .ARG_IMAGE_LIST,
                                        photoList
                                    )

                                    putInt(
                                        PhotoPreviewFragment
                                            .ARG_POSITION,
                                        photoPosition
                                    )
                                }


                            parentFragmentManager
                                .beginTransaction()
                                .replace(
                                    R.id.frameContainer,
                                    preview
                                )
                                .addToBackStack(
                                    "favorite_preview"
                                )
                                .commit()
                        },


                        // -------------------------------------
                        // LONG PRESS
                        // -------------------------------------

                        onPhotoLongClick = {
                                photo ->

                            viewModel
                                .enterSelectionMode(
                                    photo.image
                                )
                        },


                        // -------------------------------------
                        // TAP WHILE SELECTING
                        // -------------------------------------

                        onPhotoToggleSelect = {
                                photo ->

                            viewModel
                                .toggleSelection(
                                    photo.image
                                )
                        }
                    )


                // ---------------------------------------------
                // Restore selection
                // ---------------------------------------------

                photosAdapter.setSelectionState(
                    viewModel.isSelectionMode.value == true,
                    viewModel.selectedPhotoIds.value
                        ?: emptySet()
                )


                // ---------------------------------------------
                // RecyclerView
                // ---------------------------------------------

                binding.recyclerFavorites.adapter =
                    photosAdapter


                binding.recyclerFavorites.layoutManager =
                    buildGridLayoutManager()


                // ---------------------------------------------
                // Header
                // ---------------------------------------------

                updateSelectionCount(
                    viewModel.selectedPhotoIds.value
                        ?: emptySet()
                )


                updateSelectionMode(
                    viewModel.isSelectionMode.value == true
                )
            }
        }
    }


    // =========================================================
    // SELECTION UI
    // =========================================================

    private fun updateSelectionMode(
        isSelecting: Boolean
    ) {

        binding.normalHeader.visibility =
            if (isSelecting) {
                View.GONE
            } else {
                View.VISIBLE
            }


        binding.selectionHeader.visibility =
            if (isSelecting) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }


    private fun updateSelectionCount(
        selectedIds: Set<Long>
    ) {

        binding.tvSelectedCount.text =
            "${selectedIds.size} / ${photoList.size}"


        val selectedSize =
            photoList
                .filter {
                    it.id in selectedIds
                }
                .sumOf {
                    it.size
                }


        binding.tvSelectedSize.text =
            android.text.format.Formatter
                .formatShortFileSize(
                    requireContext(),
                    selectedSize
                )
    }


    // =========================================================
    // GRID
    // =========================================================

    private fun buildGridLayoutManager():
            GridLayoutManager {

        val layoutManager =
            GridLayoutManager(
                requireContext(),
                4
            )


        layoutManager.spanSizeLookup =
            object :
                GridLayoutManager.SpanSizeLookup() {

                override fun getSpanSize(
                    position: Int
                ): Int {

                    return if (
                        ::photosAdapter.isInitialized
                    ) {

                        photosAdapter.getSpanSize(
                            position,
                            4
                        )

                    } else {

                        1
                    }
                }
            }


        return layoutManager
    }


    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}