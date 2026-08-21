package com.example.mygallery.ui.photo

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager

import com.example.mygallery.R
import com.example.mygallery.adapter.PhotosAdapter
import com.example.mygallery.databinding.FragmentFavoritesBinding
import com.example.mygallery.model.DeleteResult
import com.example.mygallery.model.ImageModel
import com.example.mygallery.model.PhotoListItem
import com.example.mygallery.model.TrashItem
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.MainActivity
import com.example.mygallery.ui.album.AlbumPickerBottomSheet
import com.example.mygallery.ui.photo.details.PhotoDetailsBottomSheet
import com.example.mygallery.ui.photo.menu.PhotoAction
import com.example.mygallery.ui.photo.menu.PhotoActionPopup
import com.example.mygallery.ui.photo.menu.SortOrder
import com.example.mygallery.ui.photo.menu.SortType
import com.example.mygallery.ui.photo.selection.PhotoSelectionAction
import com.example.mygallery.ui.photo.selection.PhotoSelectionActionPopup
import com.example.mygallery.ui.photo.slideshow.SlideShowFragment
import com.example.mygallery.ui.state.PhotosUiState
import com.example.mygallery.utils.FavoritePreferences
import com.example.mygallery.utils.TrashManager
import com.example.mygallery.utils.TrashStorage
import com.example.mygallery.viewmodel.PhotosViewModel
import com.example.mygallery.viewmodel.PhotosViewModelFactory

import kotlinx.coroutines.launch


class FavoritesFragment : Fragment() {

    // =========================================================
    // BINDING
    // =========================================================

    private var _binding: FragmentFavoritesBinding? = null

    private val binding
        get() = _binding!!


    // =========================================================
    // VIEW MODEL / ADAPTER
    // =========================================================

    private lateinit var viewModel: PhotosViewModel

    private lateinit var photosAdapter: PhotosAdapter


    // =========================================================
    // PHOTO DATA
    // =========================================================

    private val photoList =
        ArrayList<ImageModel>()


    // =========================================================
    // SORT
    // =========================================================

    private var currentSortType =
        SortType.DATE_TAKEN

    private var currentSortOrder =
        SortOrder.DESCENDING


    // =========================================================
    // DELETE / TRASH STATE
    // =========================================================

    private var pendingDeleteRetryUris:
            List<Uri>? = null

    private var pendingDeleteSuccessMessage =
        "Moved to Trash"

    private var pendingTrashItems:
            List<TrashItem> = emptyList()


    // =========================================================
    // CREATE VIEW MODEL
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        val repository =
            GalleryRepository()


        val factory =
            PhotosViewModelFactory(
                repository
            )


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


        setupObservers()

        setupBasicViews()

        setupNormalMenu()

        setupSelectionMenu()


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


        /*
         * Favorites is a normal screen.
         * Keep bottom navigation visible.
         */
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
    // OBSERVERS
    // =========================================================

    private fun setupObservers() {

        // -----------------------------------------------------
        // UI STATE
        // -----------------------------------------------------

        viewModel.uiState.observe(
            viewLifecycleOwner
        ) { state ->

            renderState(
                state
            )
        }


        // -----------------------------------------------------
        // SELECTION MODE
        // -----------------------------------------------------

        viewModel.isSelectionMode.observe(
            viewLifecycleOwner
        ) { selecting ->

            updateSelectionMode(
                selecting
            )


            if (::photosAdapter.isInitialized) {

                photosAdapter.setSelectionState(
                    selecting,
                    viewModel.selectedPhotoIds.value
                        ?: emptySet()
                )
            }
        }


        // -----------------------------------------------------
        // SELECTED IDS
        // -----------------------------------------------------

        viewModel.selectedPhotoIds.observe(
            viewLifecycleOwner
        ) { selectedIds ->

            updateSelectionCount(
                selectedIds
            )


            if (::photosAdapter.isInitialized) {

                photosAdapter.setSelectionState(
                    viewModel.isSelectionMode.value == true,
                    selectedIds
                )
            }
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


        binding.btnExitSelection.setOnClickListener {

            viewModel.exitSelectionMode()
        }
    }


    // =========================================================
    // NORMAL THREE-DOT MENU
    //
    // SAME MENU SYSTEM AS PHOTOFRAGMENT
    // =========================================================

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

                        /*
                         * Same behavior as PhotoFragment.
                         *
                         * Selection normally starts from long press
                         * on a photo.
                         */
                    }


                    // -------------------------------------------------
                    // PIN
                    // -------------------------------------------------

                    PhotoAction.PIN -> {

                        // Not required for Favorites.
                    }


                    // -------------------------------------------------
                    // SORT
                    // -------------------------------------------------

                    PhotoAction.SORT -> {

                        showSortBottomSheet()
                    }


                    // -------------------------------------------------
                    // FILTER
                    // -------------------------------------------------

                    PhotoAction.FILTER -> {

                        // Favorites does not need a separate filter.
                    }


                    // -------------------------------------------------
                    // LAYOUT
                    // -------------------------------------------------

                    PhotoAction.LAYOUT_STYLE -> {

                        /*
                         * Favorites design is fixed to grid,
                         * so nothing is changed here.
                         */
                    }


                    // -------------------------------------------------
                    // COLUMN
                    // -------------------------------------------------

                    PhotoAction.COLUMN -> {

                        /*
                         * Favorites design uses the same 4-column
                         * grid as the current Photos screen.
                         */
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
    // SELECTION THREE-DOT MENU
    //
    // SAME POPUP AS PHOTOFRAGMENT
    // =========================================================

    private fun setupSelectionMenu() {

        binding.btnSelectionMenu.setOnClickListener {

            val selectedCount =
                viewModel.selectedPhotoIds.value
                    ?.size
                    ?: 0


            if (selectedCount == 0) {

                return@setOnClickListener
            }


            PhotoSelectionActionPopup.show(
                requireContext(),
                binding.btnSelectionMenu,
                selectedCount
            ) { action ->

                when (action) {

                    // ---------------------------------------------
                    // COPY
                    // ---------------------------------------------

                    PhotoSelectionAction.COPY -> {

                        showPhotoAlbumPicker(
                            AlbumPickerBottomSheet.Mode.COPY
                        )
                    }


                    // ---------------------------------------------
                    // MOVE
                    // ---------------------------------------------

                    PhotoSelectionAction.MOVE -> {

                        showPhotoAlbumPicker(
                            AlbumPickerBottomSheet.Mode.MOVE
                        )
                    }


                    // ---------------------------------------------
                    // RENAME
                    // ---------------------------------------------

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


                    // ---------------------------------------------
                    // FAVORITE
                    // ---------------------------------------------
                    //
                    // On Favorites screen this means:
                    // REMOVE FROM FAVORITES
                    // ---------------------------------------------

                    PhotoSelectionAction.FAVORITE -> {

                        unfavoriteSelected()
                    }


                    // ---------------------------------------------
                    // SLIDE SHOW
                    // ---------------------------------------------

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


                    // ---------------------------------------------
                    // EDIT WITH
                    // ---------------------------------------------

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
                                "Select only one photo to edit",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }


                    // ---------------------------------------------
                    // SET AS WALLPAPER
                    // ---------------------------------------------

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
                                "Select only one photo",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }


                    // ---------------------------------------------
                    // SHARE
                    // ---------------------------------------------

                    PhotoSelectionAction.SHARE -> {

                        shareSelectedPhotos()
                    }


                    // ---------------------------------------------
                    // DELETE
                    // ---------------------------------------------

                    PhotoSelectionAction.DELETE -> {

                        confirmAndDeleteSelected()
                    }


                    // ---------------------------------------------
                    // DETAILS
                    // ---------------------------------------------

                    PhotoSelectionAction.DETAILS -> {

                        val selected =
                            viewModel.getSelectedPhotos()


                        if (selected.isNotEmpty()) {

                            PhotoDetailsBottomSheet(
                                ArrayList(selected)
                            ).show(
                                childFragmentManager,
                                "favorite_photo_details"
                            )
                        }
                    }


                    // ---------------------------------------------
                    // OPEN WITH
                    // ---------------------------------------------

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
                                "Select only one photo",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
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


                photoList.clear()


                /*
                 * PhotosUiState contains date headers + photos.
                 * Favorites only needs the actual ImageModels.
                 */
                state.items.forEach { item ->

                    if (
                        item is PhotoListItem.Photo
                    ) {

                        photoList.add(
                            item.image
                        )
                    }
                }


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
                                _,
                                position ->

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
                                        position
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

                            viewModel.enterSelectionMode(
                                photo.image
                            )
                        },


                        // -------------------------------------
                        // TAP WHILE SELECTING
                        // -------------------------------------

                        onPhotoToggleSelect = {
                                photo ->

                            viewModel.toggleSelection(
                                photo.image
                            )
                        }
                    )


                // ---------------------------------------------
                // Restore selection state
                // ---------------------------------------------

                photosAdapter.setSelectionState(
                    viewModel.isSelectionMode.value == true,
                    viewModel.selectedPhotoIds.value
                        ?: emptySet()
                )


                binding.recyclerFavorites.adapter =
                    photosAdapter


                // ---------------------------------------------
                // Grid
                // ---------------------------------------------

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

                            return photosAdapter
                                .getSpanSize(
                                    position,
                                    4
                                )
                        }
                    }


                binding.recyclerFavorites.layoutManager =
                    layoutManager


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
        selecting: Boolean
    ) {

        binding.groupNormalHeader.visibility =
            if (selecting) {
                View.GONE
            } else {
                View.VISIBLE
            }


        binding.selectionHeader.visibility =
            if (selecting) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }


    private fun updateSelectionCount(
        selectedIds: Set<Long>
    ) {

        val selectedCount =
            selectedIds.size


        val totalCount =
            photoList.size


        binding.tvSelectedCount.text =
            "$selectedCount / $totalCount"


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
    // REMOVE FROM FAVORITES
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
            if (selected.size == 1)
                "Removed from Favorites"
            else
                "${selected.size} photos removed from Favorites",
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
    // COPY / MOVE ALBUM PICKER
    // =========================================================

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
                .map {
                    it.folderName
                }
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


    // =========================================================
    // COPY
    // =========================================================

    private fun performPhotoCopy(
        photos: List<ImageModel>,
        destinationAlbumName: String
    ) {

        val uris =
            photos.map {
                it.uri
            }


        if (uris.isEmpty()) {

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
                    result.getOrNull()
                        ?: 0


                Toast.makeText(
                    requireContext(),
                    "Copied $copiedCount item(s) to $destinationAlbumName",
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
                    "Copy failed: ${
                        result.exceptionOrNull()?.message
                    }",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // =========================================================
    // MOVE
    // =========================================================

    private fun performPhotoMove(
        photos: List<ImageModel>,
        destinationAlbumName: String
    ) {

        val uris =
            photos.map {
                it.uri
            }


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


            if (copyResult.isSuccess) {

                viewModel.deleteImages(
                    requireContext(),
                    uris
                ) { deleteResult ->

                    handleDeleteResult(
                        deleteResult,
                        "Moved to $destinationAlbumName"
                    )
                }

            } else {

                Toast.makeText(
                    requireContext(),
                    "Move failed: ${
                        copyResult.exceptionOrNull()?.message
                    }",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // =========================================================
    // RENAME
    // =========================================================

    private fun showRenameDialog(
        photo: ImageModel
    ) {

        val editText =
            EditText(
                requireContext()
            ).apply {

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

                addView(
                    editText
                )
            }


        val dialog =
            AlertDialog.Builder(
                requireContext()
            )
                .setTitle(
                    "Rename"
                )
                .setMessage(
                    "Enter a new name for this photo."
                )
                .setView(
                    container
                )
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
                    newName
                )


                dialog.dismiss()
            }
        }


        dialog.show()
    }


    private fun renamePhoto(
        photo: ImageModel,
        newName: String
    ) {

        try {

            val extension =
                photo.name
                    .substringAfterLast(
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
                        MediaStore.MediaColumns
                            .DISPLAY_NAME,
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

                Toast.makeText(
                    requireContext(),
                    "Renamed successfully",
                    Toast.LENGTH_SHORT
                ).show()


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


    // =========================================================
    // OPEN WITH
    // =========================================================

    private fun openPhotoWith(
        photo: ImageModel
    ) {

        try {

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


            startActivity(
                Intent.createChooser(
                    intent,
                    "Open With"
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "No app available to open this file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // EDIT WITH
    // =========================================================

    private fun openPhotoForEdit(
        photo: ImageModel
    ) {

        try {

            val intent =
                Intent(
                    Intent.ACTION_EDIT
                ).apply {

                    setDataAndType(
                        photo.uri,
                        "image/*"
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }


            startActivity(
                Intent.createChooser(
                    intent,
                    "Edit image with"
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "No image editor found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // WALLPAPER
    // =========================================================

    private fun showWallpaperDialog(
        photo: ImageModel
    ) {

        val options =
            arrayOf(
                "Home Screen",
                "Lock Screen",
                "Home + Lock Screen"
            )


        AlertDialog.Builder(
            requireContext()
        )
            .setTitle(
                "Set as Wallpaper"
            )
            .setItems(
                options
            ) { _, which ->

                applyWallpaper(
                    photo,
                    which
                )
            }
            .show()
    }


    private fun applyWallpaper(
        photo: ImageModel,
        option: Int
    ) {

        try {

            val wallpaperManager =
                WallpaperManager.getInstance(
                    requireContext()
                )


            val inputStream =
                requireContext()
                    .contentResolver
                    .openInputStream(
                        photo.uri
                    )
                    ?: return


            val bitmap =
                BitmapFactory.decodeStream(
                    inputStream
                )


            inputStream.close()


            if (bitmap == null) {

                Toast.makeText(
                    requireContext(),
                    "Unable to load image",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }


            val targetWidth =
                wallpaperManager
                    .desiredMinimumWidth


            val targetHeight =
                wallpaperManager
                    .desiredMinimumHeight


            val wallpaperBitmap =
                createWallpaperBitmap(
                    bitmap,
                    targetWidth,
                    targetHeight
                )


            when (option) {

                0 -> {

                    wallpaperManager.setBitmap(
                        wallpaperBitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_SYSTEM
                    )
                }


                1 -> {

                    wallpaperManager.setBitmap(
                        wallpaperBitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_LOCK
                    )
                }


                2 -> {

                    wallpaperManager.setBitmap(
                        wallpaperBitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_SYSTEM
                                or
                                WallpaperManager.FLAG_LOCK
                    )
                }
            }


            Toast.makeText(
                requireContext(),
                "Wallpaper applied",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "Unable to set wallpaper",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun createWallpaperBitmap(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {

        val sourceWidth =
            source.width

        val sourceHeight =
            source.height


        val sourceRatio =
            sourceWidth.toFloat() /
                    sourceHeight


        val targetRatio =
            targetWidth.toFloat() /
                    targetHeight


        val cropWidth: Int
        val cropHeight: Int
        val cropX: Int
        val cropY: Int


        if (
            sourceRatio >
            targetRatio
        ) {

            cropHeight =
                sourceHeight

            cropWidth =
                (
                        sourceHeight *
                                targetRatio
                        ).toInt()

            cropX =
                (
                        sourceWidth -
                                cropWidth
                        ) / 2

            cropY =
                0

        } else {

            cropWidth =
                sourceWidth

            cropHeight =
                (
                        sourceWidth /
                                targetRatio
                        ).toInt()

            cropX =
                0

            cropY =
                (
                        sourceHeight -
                                cropHeight
                        ) / 2
        }


        val cropped =
            Bitmap.createBitmap(
                source,
                cropX,
                cropY,
                cropWidth,
                cropHeight
            )


        return Bitmap.createScaledBitmap(
            cropped,
            targetWidth,
            targetHeight,
            true
        )
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
    // SHARE
    // =========================================================

    private fun shareSelectedPhotos() {

        val selected =
            viewModel.getSelectedPhotos()


        if (selected.isEmpty()) {
            return
        }


        if (selected.size == 1) {

            val photo =
                selected.first()


            val mime =
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
                        mime

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

        } else {

            val uris =
                ArrayList<Uri>()


            selected.forEach {

                uris.add(
                    it.uri
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
    }


    // =========================================================
    // DELETE → CUSTOM TRASH
    // =========================================================

    private fun confirmAndDeleteSelected() {

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


        val count =
            selected.size


        AlertDialog.Builder(
            requireContext()
        )
            .setTitle(
                "Delete $count item${if (count > 1) "s" else ""}?"
            )
            .setMessage(
                "The selected items will be moved to Trash."
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                moveSelectedPhotosToTrash(
                    selected
                )
            }
            .show()
    }


    private fun moveSelectedPhotosToTrash(
        photos: List<ImageModel>
    ) {

        viewModel.moveImagesToTrash(
            requireContext(),
            photos
        ) { result ->

            if (result.isFailure) {

                Toast.makeText(
                    requireContext(),
                    "Unable to move to Trash: ${
                        result.exceptionOrNull()?.message
                    }",
                    Toast.LENGTH_LONG
                ).show()

                return@moveImagesToTrash
            }


            /*
             * Trash copies have been created.
             * Now delete the original MediaStore entries.
             */
            pendingTrashItems =
                TrashStorage.getAll(
                    requireContext()
                ).filter { item ->

                    photos.any {
                        it.id == item.id
                    }
                }


            val uris =
                photos.map {
                    it.uri
                }


            pendingDeleteSuccessMessage =
                "Moved to Trash"


            viewModel.deleteImages(
                requireContext(),
                uris
            ) { deleteResult ->

                handleDeleteResult(
                    deleteResult,
                    "Moved to Trash"
                )
            }
        }
    }


    // =========================================================
    // DELETE RESULT
    // =========================================================

    private val deleteIntentSenderLauncher =
        registerForActivityResult(
            ActivityResultContracts
                .StartIntentSenderForResult()
        ) { result ->

            if (
                result.resultCode ==
                Activity.RESULT_OK
            ) {

                val retryUris =
                    pendingDeleteRetryUris


                pendingDeleteRetryUris =
                    null


                if (retryUris != null) {

                    viewModel.deleteImages(
                        requireContext(),
                        retryUris
                    ) { deleteResult ->

                        handleDeleteResult(
                            deleteResult,
                            pendingDeleteSuccessMessage
                        )
                    }

                } else {

                    onDeleteFinished(
                        pendingDeleteSuccessMessage
                    )
                }

            } else {

                pendingDeleteRetryUris =
                    null


                clearPendingTrash()


                Toast.makeText(
                    requireContext(),
                    "Delete cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    private fun handleDeleteResult(
        result: DeleteResult,
        successMessage: String
    ) {

        when (result) {

            // -------------------------------------------------
            // SUCCESS
            // -------------------------------------------------

            is DeleteResult.Success -> {

                onDeleteFinished(
                    successMessage
                )
            }


            // -------------------------------------------------
            // ANDROID CONFIRMATION
            // -------------------------------------------------

            is DeleteResult.ConfirmDelete -> {

                pendingDeleteRetryUris =
                    null

                pendingDeleteSuccessMessage =
                    successMessage


                deleteIntentSenderLauncher.launch(
                    IntentSenderRequest.Builder(
                        result.intentSender
                    ).build()
                )
            }


            // -------------------------------------------------
            // ANDROID 10 PERMISSION
            // -------------------------------------------------

            is DeleteResult.GrantPermissionThenRetry -> {

                pendingDeleteRetryUris =
                    result.remainingUris

                pendingDeleteSuccessMessage =
                    successMessage


                deleteIntentSenderLauncher.launch(
                    IntentSenderRequest.Builder(
                        result.intentSender
                    ).build()
                )
            }


            // -------------------------------------------------
            // ERROR
            // -------------------------------------------------

            is DeleteResult.Error -> {

                clearPendingTrash()


                Toast.makeText(
                    requireContext(),
                    "Delete failed: ${result.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // =========================================================
    // CLEAR TRASH COPIES IF DELETE FAILS
    // =========================================================

    private fun clearPendingTrash() {

        pendingTrashItems.forEach { item ->

            TrashManager.deleteTrashCopy(
                item
            )


            TrashStorage.remove(
                requireContext(),
                item.id
            )
        }


        pendingTrashItems =
            emptyList()
    }


    // =========================================================
    // DELETE FINISHED
    // =========================================================

    private fun onDeleteFinished(
        successMessage: String
    ) {

        Toast.makeText(
            requireContext(),
            successMessage,
            Toast.LENGTH_SHORT
        ).show()


        pendingTrashItems =
            emptyList()


        viewModel.exitSelectionMode()


        /*
         * Important:
         * Favorites must reload Favorites,
         * NOT loadPhotos().
         */
        viewModel.loadFavorites(
            requireContext(),
            currentSortType,
            currentSortOrder
        )
    }


    // =========================================================
    // DESTROY VIEW
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}