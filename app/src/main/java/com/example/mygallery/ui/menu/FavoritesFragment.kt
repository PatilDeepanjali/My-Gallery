package com.example.mygallery.ui.menu

import android.app.Activity
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.example.mygallery.ui.photo.PhotoPreviewFragment
import com.example.mygallery.ui.photo.SortBottomSheet
import com.example.mygallery.ui.photo.menu.ColumnBottomSheet
import com.example.mygallery.ui.photo.menu.FilterBottomSheet
import com.example.mygallery.ui.photo.menu.FilterType
import com.example.mygallery.ui.photo.menu.LayoutStyleBottomSheet
import com.example.mygallery.ui.photo.menu.PhotoAction
import com.example.mygallery.ui.photo.menu.PhotoActionPopup
import com.example.mygallery.ui.photo.menu.PhotoMenuActions
import com.example.mygallery.ui.photo.menu.PhotoSelectionActions
import com.example.mygallery.ui.photo.menu.SortOrder
import com.example.mygallery.ui.photo.menu.SortType
import com.example.mygallery.ui.photo.selection.PhotoSelectionAction
import com.example.mygallery.ui.photo.selection.PhotoSelectionActionPopup
import com.example.mygallery.ui.photo.slideshow.SlideShowFragment
import com.example.mygallery.ui.state.PhotosUiState
import com.example.mygallery.ui.video.VideoPlayerFragment
import com.example.mygallery.utils.TrashManager
import com.example.mygallery.utils.TrashStorage
import com.example.mygallery.viewmodel.PhotosViewModel
import com.example.mygallery.viewmodel.PhotosViewModelFactory
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var viewModel: PhotosViewModel
    private lateinit var photosAdapter: PhotosAdapter


    // =========================================================
    // FILTER
    // =========================================================

    private var currentFilter =
        FilterType.ALL_ITEMS

    private var allFavoriteItems:
            List<PhotoListItem> = emptyList()


    // =========================================================
    // CURRENT FAVORITE PHOTOS
    // =========================================================

    private val photoList =
        ArrayList<ImageModel>()


    // =========================================================
    // LAYOUT
    // Same behavior as PhotoFragment
    // =========================================================

    private var isGridView =
        true

    private var gridSpanCount =
        4


    // =========================================================
    // SORT
    // =========================================================

    private var currentSortType =
        SortType.DATE_TAKEN

    private var currentSortOrder =
        SortOrder.DESCENDING


    // =========================================================
    // TRASH STATE
    // =========================================================

    private var pendingTrashItems:
            List<TrashItem> = emptyList()

    private var pendingDeleteRetryUris:
            List<Uri>? = null

    private var pendingDeleteSuccessMessage =
        "Moved to Trash"


    // =========================================================
    // RENAME STATE
    // =========================================================

    private var pendingRenamePhoto:
            ImageModel? = null

    private var pendingRenameName:
            String? = null


    // =========================================================
    // RENAME PERMISSION
    // =========================================================

    private val renamePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            if (
                result.resultCode ==
                Activity.RESULT_OK
            ) {

                val photo =
                    pendingRenamePhoto

                val newName =
                    pendingRenameName

                if (
                    photo != null &&
                    newName != null
                ) {

                    retryRename(
                        photo,
                        newName
                    )
                }

            } else {

                Toast.makeText(
                    requireContext(),
                    "Rename permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }

            pendingRenamePhoto = null
            pendingRenameName = null
        }


    // =========================================================
    // DELETE / TRASH PERMISSION
    // =========================================================

    private val deleteIntentSenderLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            if (
                result.resultCode ==
                Activity.RESULT_OK
            ) {

                val retryUris =
                    pendingDeleteRetryUris

                pendingDeleteRetryUris =
                    null

                if (
                    retryUris != null
                ) {

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


    // =========================================================
    // CREATE
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


        // -----------------------------------------------------
        // Android Back
        // -----------------------------------------------------

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {

                    override fun handleOnBackPressed() {

                        if (
                            viewModel
                                .isSelectionMode
                                .value == true
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
        // Load Favorites
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


        if (
            ::photosAdapter.isInitialized
        ) {

            photosAdapter.setSelectionState(
                viewModel
                    .isSelectionMode
                    .value == true,

                viewModel
                    .selectedPhotoIds
                    .value
                    ?: emptySet()
            )

            updateSelectionMode(
                viewModel
                    .isSelectionMode
                    .value == true
            )

            updateSelectionCount(
                viewModel
                    .selectedPhotoIds
                    .value
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


        binding.recyclerFavorites.layoutManager =
            buildGridLayoutManager()
    }


    // =========================================================
    // NORMAL MENU
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

                        enterSelectionMode()
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

                        showFilterBottomSheet()
                    }


                    // -------------------------------------------------
                    // LAYOUT STYLE
                    // -------------------------------------------------

                    PhotoAction.LAYOUT_STYLE -> {

                        showLayoutStyleBottomSheet()
                    }


                    // -------------------------------------------------
                    // COLUMN
                    // -------------------------------------------------

                    PhotoAction.COLUMN -> {

                        showColumnBottomSheet()
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


                    // -------------------------------------------------
                    // PIN
                    // -------------------------------------------------

                    PhotoAction.PIN -> {

                        // Not applicable to Favorites
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
    // FILTER
    // =========================================================

    private fun showFilterBottomSheet() {

        val sheet =
            FilterBottomSheet(
                currentFilter
            )


        sheet.setListener(
            object :
                FilterBottomSheet.OnFilterSelected {

                override fun onFilterSelected(
                    filterType: FilterType
                ) {

                    currentFilter =
                        filterType

                    applyCurrentFilter()
                }
            }
        )


        sheet.show(
            parentFragmentManager,
            "favorites_filter"
        )
    }


    // =========================================================
    // APPLY FILTER
    // =========================================================

    private fun applyCurrentFilter() {

        if (
            allFavoriteItems.isEmpty()
        ) {

            photoList.clear()

            binding.recyclerFavorites.visibility =
                View.GONE

            binding.layoutEmptyState.visibility =
                View.VISIBLE

            binding.tvSubtitle.text =
                "0 Photos"

            return
        }


        val filteredItems =
            when (currentFilter) {

                // ---------------------------------------------
                // All items
                // ---------------------------------------------

                FilterType.ALL_ITEMS,
                FilterType.FAVOURITE -> {

                    allFavoriteItems
                }


                // ---------------------------------------------
                // Photos
                // ---------------------------------------------

                FilterType.PHOTOS -> {

                    filterFavoriteItems { photo ->

                        photo.mimeType.startsWith(
                            "image/",
                            ignoreCase = true
                        )
                    }
                }


                // ---------------------------------------------
                // Videos
                // ---------------------------------------------

                FilterType.VIDEOS -> {

                    filterFavoriteItems { photo ->

                        photo.mimeType.startsWith(
                            "video/",
                            ignoreCase = true
                        )
                    }
                }


                // ---------------------------------------------
                // Screenshots
                // ---------------------------------------------

                FilterType.SCREENSHOTS -> {

                    filterFavoriteItems { photo ->

                        photo.name.contains(
                            "screenshot",
                            ignoreCase = true
                        ) ||

                                photo.folderName.contains(
                                    "screenshot",
                                    ignoreCase = true
                                )
                    }
                }
            }


        renderFavoriteItems(
            filteredItems
        )
    }


    // =========================================================
    // FILTER DATE HEADERS
    // =========================================================

    private fun filterFavoriteItems(
        predicate: (ImageModel) -> Boolean
    ): List<PhotoListItem> {

        val result =
            mutableListOf<PhotoListItem>()


        var pendingHeader:
                PhotoListItem? = null


        for (
        item in allFavoriteItems
        ) {

            when (item) {

                is PhotoListItem.Photo -> {

                    if (
                        predicate(
                            item.image
                        )
                    ) {

                        if (
                            pendingHeader != null
                        ) {

                            result.add(
                                pendingHeader
                            )

                            pendingHeader =
                                null
                        }


                        result.add(
                            item
                        )
                    }
                }


                else -> {

                    pendingHeader =
                        item
                }
            }
        }


        return result
    }


    // =========================================================
    // LAYOUT STYLE
    // =========================================================

    private fun showLayoutStyleBottomSheet() {

        val sheet =
            LayoutStyleBottomSheet(
                isGridView
            )


        sheet.setListener(
            object :
                LayoutStyleBottomSheet
                .OnLayoutStyleSelected {

                override fun onLayoutSelected(
                    isGrid: Boolean
                ) {

                    applyLayoutStyle(
                        isGrid
                    )
                }
            }
        )


        sheet.show(
            parentFragmentManager,
            "favorites_layout_style"
        )
    }


    // =========================================================
    // APPLY LAYOUT STYLE
    // Same behavior as PhotoFragment
    // =========================================================

    private fun applyLayoutStyle(
        isGrid: Boolean
    ) {

        isGridView =
            isGrid


        if (
            ::photosAdapter.isInitialized
        ) {

            photosAdapter.setViewMode(
                isGrid
            )


            binding.recyclerFavorites.layoutManager =

                if (isGrid) {

                    buildGridLayoutManager()

                } else {

                    LinearLayoutManager(
                        requireContext()
                    )
                }
        }
    }


    // =========================================================
    // COLUMN
    // =========================================================

    private fun showColumnBottomSheet() {

        val sheet =
            ColumnBottomSheet(
                gridSpanCount
            )


        sheet.setListener(
            object :
                ColumnBottomSheet
                .OnColumnSelected {

                override fun onColumnSelected(
                    column: Int
                ) {

                    applyColumn(
                        column
                    )
                }
            }
        )


        sheet.show(
            parentFragmentManager,
            "favorites_column"
        )
    }


    // =========================================================
    // APPLY COLUMN
    // Same behavior as PhotoFragment
    // =========================================================

    private fun applyColumn(
        column: Int
    ) {

        gridSpanCount =
            column


        if (
            isGridView &&
            ::photosAdapter.isInitialized
        ) {

            binding.recyclerFavorites.layoutManager =
                buildGridLayoutManager()
        }
    }


    // =========================================================
    // SELECTION HEADER
    // =========================================================

    private fun setupSelectionHeader() {

        binding.btnExitSelection.setOnClickListener {

            viewModel.exitSelectionMode()
        }


        binding.btnSelectionMenu.setOnClickListener {

            showSelectionMenu()
        }
    }


    // =========================================================
    // ENTER SELECTION
    // =========================================================

    private fun enterSelectionMode() {

        if (
            photoList.isEmpty()
        ) {

            Toast.makeText(
                requireContext(),
                "No favorite photos",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        viewModel.enterSelectionMode(
            photoList.first()
        )
    }


    // =========================================================
    // SELECTION MENU
    // =========================================================

    private fun showSelectionMenu() {

        val selectedCount =
            viewModel
                .selectedPhotoIds
                .value
                ?.size
                ?: 0


        if (
            selectedCount == 0
        ) {
            return
        }


        PhotoSelectionActionPopup.show(
            requireContext(),
            binding.btnSelectionMenu,
            selectedCount
        ) { action ->

            when (action) {

                // -------------------------------------------------
                // COPY
                // -------------------------------------------------

                PhotoSelectionAction.COPY -> {

                    showPhotoAlbumPicker(
                        AlbumPickerBottomSheet.Mode.COPY
                    )
                }


                // -------------------------------------------------
                // MOVE
                // -------------------------------------------------

                PhotoSelectionAction.MOVE -> {

                    showPhotoAlbumPicker(
                        AlbumPickerBottomSheet.Mode.MOVE
                    )
                }


                // -------------------------------------------------
                // RENAME
                // -------------------------------------------------

                PhotoSelectionAction.RENAME -> {

                    val selectedPhotos =
                        viewModel
                            .getSelectedPhotos()


                    if (
                        selectedPhotos.size == 1
                    ) {

                        showRenameDialog(
                            selectedPhotos.first()
                        )

                    } else {

                        Toast.makeText(
                            requireContext(),
                            "Select only one photo to rename",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                // -------------------------------------------------
                // FAVORITE
                // -------------------------------------------------

                PhotoSelectionAction.FAVORITE -> {

                    removeSelectedFromFavorites()
                }


                // -------------------------------------------------
                // SLIDESHOW
                // -------------------------------------------------

                PhotoSelectionAction.SLIDE_SHOW -> {

                    val selectedPhotos =
                        viewModel
                            .getSelectedPhotos()


                    if (
                        selectedPhotos.isNotEmpty()
                    ) {

                        startSlideShow(
                            selectedPhotos,
                            0
                        )
                    }
                }


                // -------------------------------------------------
                // EDIT WITH
                // -------------------------------------------------

                PhotoSelectionAction.EDIT_WITH -> {

                    val selectedPhotos =
                        viewModel
                            .getSelectedPhotos()


                    if (
                        selectedPhotos.size == 1
                    ) {

                        openPhotoForEdit(
                            selectedPhotos.first()
                        )

                    } else {

                        Toast.makeText(
                            requireContext(),
                            "Select one photo to edit",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                // -------------------------------------------------
                // WALLPAPER
                // -------------------------------------------------

                PhotoSelectionAction.SET_AS_WALLPAPER -> {

                    val selectedPhotos =
                        viewModel
                            .getSelectedPhotos()


                    if (
                        selectedPhotos.size == 1
                    ) {

                        showWallpaperDialog(
                            selectedPhotos.first()
                        )

                    } else {

                        Toast.makeText(
                            requireContext(),
                            "Select one photo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                // -------------------------------------------------
                // SHARE
                // -------------------------------------------------

                PhotoSelectionAction.SHARE -> {

                    shareSelectedPhotos()
                }


                // -------------------------------------------------
                // DELETE
                // -------------------------------------------------

                PhotoSelectionAction.DELETE -> {

                    confirmAndMoveToTrash()
                }


                // -------------------------------------------------
                // DETAILS
                // -------------------------------------------------

                PhotoSelectionAction.DETAILS -> {

                    showSelectedDetails()
                }


                // -------------------------------------------------
                // OPEN WITH
                // -------------------------------------------------

                PhotoSelectionAction.OPEN_WITH -> {

                    val selectedPhotos =
                        viewModel
                            .getSelectedPhotos()


                    if (
                        selectedPhotos.size == 1
                    ) {

                        openPhotoWith(
                            selectedPhotos.first()
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


    // =========================================================
    // DETAILS
    // =========================================================

    private fun showSelectedDetails() {

        val selectedPhotos =
            viewModel.getSelectedPhotos()


        if (
            selectedPhotos.isEmpty()
        ) {
            return
        }


        PhotoMenuActions.showDetails(
            this,
            selectedPhotos
        )
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
                    "Enter a name for this photo."
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


                if (
                    newName.isEmpty()
                ) {

                    editText.error =
                        "Name cannot be empty"

                    return@setOnClickListener
                }


                val result =
                    PhotoMenuActions.rename(
                        requireContext(),
                        photo,
                        newName
                    )


                when (result) {

                    is PhotoMenuActions
                    .RenameResult.Success -> {

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
                    }


                    is PhotoMenuActions
                    .RenameResult.Error -> {

                        Toast.makeText(
                            requireContext(),
                            result.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }


                    is PhotoMenuActions
                    .RenameResult.NeedsPermission -> {

                        pendingRenamePhoto =
                            photo

                        pendingRenameName =
                            newName


                        val request =
                            IntentSenderRequest.Builder(
                                result.intentSender
                            ).build()


                        renamePermissionLauncher
                            .launch(
                                request
                            )
                    }
                }


                if (
                    result !is PhotoMenuActions
                    .RenameResult.NeedsPermission
                ) {

                    dialog.dismiss()
                }
            }
        }


        dialog.show()
    }


    private fun retryRename(
        photo: ImageModel,
        newName: String
    ) {

        when (
            val result =
                PhotoMenuActions.rename(
                    requireContext(),
                    photo,
                    newName
                )
        ) {

            is PhotoMenuActions
            .RenameResult.Success -> {

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
            }


            is PhotoMenuActions
            .RenameResult.Error -> {

                Toast.makeText(
                    requireContext(),
                    result.message,
                    Toast.LENGTH_LONG
                ).show()
            }


            is PhotoMenuActions
            .RenameResult.NeedsPermission -> {

                val request =
                    IntentSenderRequest.Builder(
                        result.intentSender
                    ).build()


                renamePermissionLauncher
                    .launch(
                        request
                    )
            }
        }
    }


    // =========================================================
    // OPEN WITH
    // =========================================================

    private fun openPhotoWith(
        photo: ImageModel
    ) {

        PhotoMenuActions.openWith(
            requireContext(),
            photo
        )
    }


    // =========================================================
    // EDIT WITH
    // =========================================================

    private fun openPhotoForEdit(
        photo: ImageModel
    ) {

        PhotoMenuActions.editWith(
            requireContext(),
            photo
        )
    }


    // =========================================================
    // WALLPAPER
    // =========================================================

    private fun showWallpaperDialog(
        photo: ImageModel
    ) {

        PhotoMenuActions.showWallpaperDialog(
            this,
            photo
        )
    }


    // =========================================================
    // COPY / MOVE ALBUM PICKER
    // =========================================================

    private fun showPhotoAlbumPicker(
        mode: AlbumPickerBottomSheet.Mode
    ) {

        val selectedPhotos =
            viewModel.getSelectedPhotos()


        if (
            selectedPhotos.isEmpty()
        ) {

            Toast.makeText(
                requireContext(),
                "No photos selected",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        val excludedNames =
            selectedPhotos
                .map {
                    it.folderName
                }
                .distinct()


        val sheet =
            AlbumPickerBottomSheet.Companion.newInstance(
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
                        selectedPhotos,
                        destinationAlbumName
                    )

                } else {

                    performPhotoMove(
                        selectedPhotos,
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

        if (
            photos.isEmpty()
        ) {
            return
        }


        viewLifecycleOwner
            .lifecycleScope
            .launch {

                val result =
                    PhotoSelectionActions.copy(
                        requireContext(),
                        viewModel.repository,
                        photos,
                        destinationAlbumName
                    )


                if (
                    result.isSuccess
                ) {

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
                            result
                                .exceptionOrNull()
                                ?.message
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

        if (
            photos.isEmpty()
        ) {
            return
        }


        val uris =
            photos.map {
                it.uri
            }


        viewLifecycleOwner
            .lifecycleScope
            .launch {

                val copyResult =
                    PhotoSelectionActions.move(
                        requireContext(),
                        viewModel.repository,
                        photos,
                        destinationAlbumName
                    )


                if (
                    !copyResult.isSuccess
                ) {

                    Toast.makeText(
                        requireContext(),
                        "Move failed: ${
                            copyResult
                                .exceptionOrNull()
                                ?.message
                        }",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }


                viewModel.deleteImages(
                    requireContext(),
                    uris
                ) { deleteResult ->

                    handleDeleteResult(
                        deleteResult,
                        "Moved to $destinationAlbumName"
                    )
                }
            }
    }


    // =========================================================
    // REMOVE FROM FAVORITES
    // =========================================================

    private fun removeSelectedFromFavorites() {

        val selectedPhotos =
            viewModel.getSelectedPhotos()


        if (
            selectedPhotos.isEmpty()
        ) {
            return
        }


        PhotoSelectionActions.removeFromFavorites(
            requireContext(),
            selectedPhotos
        )


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

        val selectedPhotos =
            viewModel.getSelectedPhotos()


        if (
            selectedPhotos.isEmpty()
        ) {
            return
        }


        PhotoSelectionActions.share(
            requireContext(),
            selectedPhotos
        )
    }


    // =========================================================
    // DELETE → TRASH
    // =========================================================

    private fun confirmAndMoveToTrash() {

        val selectedPhotos =
            viewModel.getSelectedPhotos()


        if (
            selectedPhotos.isEmpty()
        ) {
            return
        }


        val count =
            selectedPhotos.size


        AlertDialog.Builder(
            requireContext()
        )
            .setTitle(
                "Move to Trash?"
            )
            .setMessage(
                "$count photo${
                    if (count > 1) "s" else ""
                } will be moved to Trash."
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Move to Trash"
            ) { _, _ ->

                moveSelectedPhotosToTrash(
                    selectedPhotos
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

            if (
                result.isFailure
            ) {

                Toast.makeText(
                    requireContext(),
                    "Unable to move to Trash: ${
                        result
                            .exceptionOrNull()
                            ?.message
                    }",
                    Toast.LENGTH_LONG
                ).show()

                return@moveImagesToTrash
            }


            pendingTrashItems =
                TrashStorage
                    .getAll(
                        requireContext()
                    )
                    .filter { item ->

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

    private fun handleDeleteResult(
        result: DeleteResult,
        successMessage: String
    ) {

        when (result) {

            is DeleteResult.Success -> {

                onDeleteFinished(
                    successMessage
                )
            }


            is DeleteResult.ConfirmDelete -> {

                pendingDeleteRetryUris =
                    null

                pendingDeleteSuccessMessage =
                    successMessage


                deleteIntentSenderLauncher
                    .launch(
                        IntentSenderRequest.Builder(
                            result.intentSender
                        ).build()
                    )
            }


            is DeleteResult.GrantPermissionThenRetry -> {

                pendingDeleteRetryUris =
                    result.remainingUris

                pendingDeleteSuccessMessage =
                    successMessage


                deleteIntentSenderLauncher
                    .launch(
                        IntentSenderRequest.Builder(
                            result.intentSender
                        ).build()
                    )
            }


            is DeleteResult.Error -> {

                clearPendingTrash()


                Toast.makeText(
                    requireContext(),
                    "Delete failed: ${result.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun onDeleteFinished(
        successMessage: String
    ) {

        Toast.makeText(
            requireContext(),
            successMessage,
            Toast.LENGTH_SHORT
        ).show()


        viewModel.exitSelectionMode()


        viewModel.loadFavorites(
            requireContext(),
            currentSortType,
            currentSortOrder
        )
    }


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
    // SLIDESHOW
    // =========================================================

    private fun startSlideShow(
        images: List<ImageModel>,
        position: Int
    ) {

        if (
            images.isEmpty()
        ) {

            Toast.makeText(
                requireContext(),
                "No favorite photos available",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        val slideshow =
            SlideShowFragment.Companion.newInstance(
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

            renderState(
                state
            )
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

                    viewModel
                        .selectedPhotoIds
                        .value
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
                    viewModel
                        .isSelectionMode
                        .value == true,

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

                allFavoriteItems =
                    emptyList()

                photoList.clear()


                binding.tvSubtitle.text =
                    "0 Photos"


                binding.layoutEmptyState.visibility =
                    View.VISIBLE


                updateSelectionCount(
                    viewModel
                        .selectedPhotoIds
                        .value
                        ?: emptySet()
                )
            }


            // -------------------------------------------------
            // SUCCESS
            // -------------------------------------------------

            is PhotosUiState.Success -> {

                allFavoriteItems =
                    state.items


                applyCurrentFilter()
            }
        }
    }


    // =========================================================
    // RENDER FILTERED FAVORITES
    // =========================================================

    private fun renderFavoriteItems(
        items: List<PhotoListItem>
    ) {

        photoList.clear()


        items.forEach { item ->

            if (
                item is PhotoListItem.Photo
            ) {

                photoList.add(
                    item.image
                )
            }
        }


        // -----------------------------------------------------
        // Empty filtered result
        // -----------------------------------------------------

        if (
            photoList.isEmpty()
        ) {

            binding.recyclerFavorites.visibility =
                View.GONE

            binding.layoutEmptyState.visibility =
                View.VISIBLE

            binding.tvSubtitle.text =
                "0 Photos"

            updateSelectionCount(
                viewModel
                    .selectedPhotoIds
                    .value
                    ?: emptySet()
            )

            return
        }


        binding.layoutEmptyState.visibility =
            View.GONE

        binding.recyclerFavorites.visibility =
            View.VISIBLE


        binding.tvSubtitle.text =
            "${photoList.size} Photos"


        // -----------------------------------------------------
        // Adapter
        // -----------------------------------------------------

        photosAdapter =
            PhotosAdapter(

                isGridView =
                    isGridView,

                items =
                    items,


                // -------------------------------------------------
                // Normal click
                // -------------------------------------------------

                onPhotoClick = {
                        photoItem,
                        _ ->

                    // PhotoListItem.Photo -> ImageModel
                    val image =
                        photoItem.image


                    // -------------------------------------------------
                    // VIDEO
                    // -------------------------------------------------

                    if (
                        image.mimeType.startsWith(
                            "video/",
                            ignoreCase = true
                        )
                    ) {

                        openVideoPlayer(
                            image
                        )

                        return@PhotosAdapter
                    }


                    // -------------------------------------------------
                    // IMAGE
                    // -------------------------------------------------

                    val photoPosition =
                        photoList.indexOf(
                            image
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


                // -------------------------------------------------
                // Long press
                // -------------------------------------------------

                onPhotoLongClick = { photo ->

                    viewModel.enterSelectionMode(
                        photo.image
                    )
                },


                // -------------------------------------------------
                // Selection toggle
                // -------------------------------------------------

                onPhotoToggleSelect = { photo ->

                    viewModel.toggleSelection(
                        photo.image
                    )
                }
            )


        // -----------------------------------------------------
        // Restore selection
        // -----------------------------------------------------

        photosAdapter.setSelectionState(

            viewModel
                .isSelectionMode
                .value == true,

            viewModel
                .selectedPhotoIds
                .value
                ?: emptySet()
        )


        // -----------------------------------------------------
        // RecyclerView
        // -----------------------------------------------------

        binding.recyclerFavorites.adapter =
            photosAdapter


        binding.recyclerFavorites.layoutManager =

            if (isGridView) {

                buildGridLayoutManager()

            } else {

                LinearLayoutManager(
                    requireContext()
                )
            }


        // -----------------------------------------------------
        // Selection UI
        // -----------------------------------------------------

        updateSelectionCount(
            viewModel
                .selectedPhotoIds
                .value
                ?: emptySet()
        )


        updateSelectionMode(
            viewModel
                .isSelectionMode
                .value == true
        )
    }


    // =========================================================
    // GRID
    // Same logic as PhotoFragment
    // =========================================================

    private fun buildGridLayoutManager():
            GridLayoutManager {

        val layoutManager =
            GridLayoutManager(
                requireContext(),
                gridSpanCount
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
                            gridSpanCount
                        )

                    } else {

                        1
                    }
                }
            }


        return layoutManager
    }

    private fun openVideoPlayer(
        photo: ImageModel
    ) {

        val videoFragment =
            VideoPlayerFragment()


        videoFragment.arguments =
            Bundle().apply {

                putString(
                    VideoPlayerFragment
                        .ARG_VIDEO_URI,

                    photo.uri.toString()
                )
            }


        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.frameContainer,
                videoFragment
            )
            .addToBackStack(
                null
            )
            .commit()
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
            Formatter
                .formatShortFileSize(
                    requireContext(),
                    selectedSize
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