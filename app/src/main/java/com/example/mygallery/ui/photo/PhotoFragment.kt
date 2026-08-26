package com.example.mygallery.ui.photo

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mygallery.R
import com.example.mygallery.adapter.PhotosAdapter
import com.example.mygallery.databinding.FragmentPhotoBinding
import com.example.mygallery.model.DeleteResult
import com.example.mygallery.model.ImageModel
import com.example.mygallery.model.PhotoListItem
import com.example.mygallery.model.TrashItem
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.MainActivity
import com.example.mygallery.ui.album.AlbumPickerBottomSheet
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
import com.example.mygallery.utils.FavoritePreferences
import com.example.mygallery.utils.TrashManager
import com.example.mygallery.utils.TrashStorage
import com.example.mygallery.viewmodel.PhotosViewModel
import com.example.mygallery.viewmodel.PhotosViewModelFactory
import kotlinx.coroutines.launch



class PhotoFragment : Fragment() {

    companion object {
        const val ARG_FOLDER_NAME = "arg_folder_name"
    }

    // ---------------------------------------------------------
    // Binding / Adapter / ViewModel
    // ---------------------------------------------------------

    private var _binding: FragmentPhotoBinding? = null
    private val binding get() = _binding!!

    private lateinit var photoList: ArrayList<ImageModel>
    private lateinit var viewModel: PhotosViewModel
    private lateinit var photosAdapter: PhotosAdapter

    private var currentFilter =
        FilterType.ALL_ITEMS


    private var allPhotoItems:
            List<PhotoListItem> = emptyList()
    // ---------------------------------------------------------
    // Folder
    // ---------------------------------------------------------

    private var folderName: String? = null


    // ---------------------------------------------------------
    // Layout
    // ---------------------------------------------------------

    private var isGridView = true
    private var gridSpanCount = 4


    // ---------------------------------------------------------
    // Sorting
    // ---------------------------------------------------------

    private var currentSortType =
        SortType.DATE_TAKEN

    private var currentSortOrder =
        SortOrder.DESCENDING


    // ---------------------------------------------------------
    // Delete state
    // ---------------------------------------------------------

    private var pendingDeleteRetryUris:
            List<android.net.Uri>? = null

    private var pendingDeleteSuccessMessage =
        "Deleted"


    // ---------------------------------------------------------
    // Trash state
    // ---------------------------------------------------------

    private var pendingTrashItems:
            List<TrashItem> = emptyList()


    // ---------------------------------------------------------
    // Rename state
    // ---------------------------------------------------------

    private var pendingRenamePhoto:
            ImageModel? = null

    private var pendingRenameName:
            String? = null


    // ---------------------------------------------------------
    // Rename permission launcher
    // Android 10+
    // ---------------------------------------------------------

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


    // LIFECYCLE


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        folderName =
            arguments?.getString(
                ARG_FOLDER_NAME
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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentPhotoBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )


        photoList =
            ArrayList()


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

                            viewModel
                                .exitSelectionMode()

                        } else {

                            isEnabled = false

                            requireActivity()
                                .onBackPressedDispatcher
                                .onBackPressed()
                        }
                    }
                }
            )


        setupObservers()

        setupBasicViews()

        setupNormalPhotoMenu()

        setupSelectionMenu()


        // -----------------------------------------------------
        // Load Photos
        // -----------------------------------------------------

        viewModel.loadPhotos(
            requireContext(),
            folderName,
            currentSortType,
            currentSortOrder
        )
    }


    override fun onResume() {

        super.onResume()


        /*
         * PhotoFragment is a normal screen.
         * Show BottomNavigation when returning
         * from Preview / Slideshow.
         */

        (activity as? MainActivity)
            ?.showBottomNavigation()


        /*
         * Restore selection state after returning
         * from another Fragment.
         */

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


    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }


    // OBSERVERS


    private fun setupObservers() {

        viewModel.uiState.observe(
            viewLifecycleOwner
        ) { state ->

            renderState(
                state
            )
        }


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


    // BASIC UI


    private fun setupBasicViews() {

        binding.tvTitle.text =
            folderName ?: "Photos"


        binding.recyclerPhotos.layoutManager =
            buildGridLayoutManager()


        binding.btnGridView.setOnClickListener {

            applyLayoutStyle(
                true
            )
        }


        binding.btnListView.setOnClickListener {

            applyLayoutStyle(
                false
            )
        }


        binding.btnExitSelection.setOnClickListener {

            viewModel.exitSelectionMode()
        }


        binding.btnBack.setOnClickListener {

            parentFragmentManager
                .popBackStack()
        }
    }


    // NORMAL PHOTO MENU


    private fun setupNormalPhotoMenu() {

        binding.btnMenu.setOnClickListener {

            PhotoActionPopup.show(
                requireContext(),
                binding.btnMenu
            ) { action ->

                when (action) {

                    PhotoAction.SELECT -> {

                        /*
                         * Selection normally starts
                         * with long press.
                         */
                    }


                    PhotoAction.PIN -> {

                        // TODO: Implement PIN
                    }


                    PhotoAction.SORT -> {

                        val sheet =
                            SortBottomSheet(
                                currentSortType,
                                currentSortOrder
                            )

                        sheet.setListener(
                            object :
                                SortBottomSheet
                                .OnSortSelected {

                                override fun onSortSelected(
                                    sortType: SortType,
                                    sortOrder: SortOrder
                                ) {

                                    currentSortType =
                                        sortType

                                    currentSortOrder =
                                        sortOrder

                                    viewModel.loadPhotos(
                                        requireContext(),
                                        folderName,
                                        currentSortType,
                                        currentSortOrder
                                    )
                                }
                            }
                        )

                        sheet.show(
                            parentFragmentManager,
                            "sort"
                        )
                    }


                    PhotoAction.FILTER -> {

                        showFilterBottomSheet()
                    }


                    PhotoAction.LAYOUT_STYLE -> {

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
                            "layout_style"
                        )
                    }


                    PhotoAction.COLUMN -> {

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
                            "column"
                        )
                    }


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


    // filter
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
            "photo_filter"
        )
    }


    // =========================================================
// APPLY FILTER
// =========================================================

    private fun applyCurrentFilter() {

        if (allPhotoItems.isEmpty()) {

            photoList.clear()

            binding.recyclerPhotos.visibility =
                View.GONE

            binding.layoutEmptyState.visibility =
                View.VISIBLE

            binding.tvSubtitle.text =
                "0 Photos"

            return
        }


        val filteredItems =
            when (currentFilter) {

                // -------------------------------------------------
                // ALL ITEMS
                // -------------------------------------------------

                FilterType.ALL_ITEMS -> {

                    allPhotoItems
                }


                // -------------------------------------------------
                // PHOTOS
                // -------------------------------------------------

                FilterType.PHOTOS -> {

                    filterPhotoItems { photo ->

                        photo.mimeType.startsWith(
                            "image/",
                            ignoreCase = true
                        )
                    }
                }


                // -------------------------------------------------
                // VIDEOS
                // -------------------------------------------------

                FilterType.VIDEOS -> {

                    filterPhotoItems { photo ->

                        photo.mimeType.startsWith(
                            "video/",
                            ignoreCase = true
                        )
                    }
                }


                // -------------------------------------------------
                // FAVOURITE
                // -------------------------------------------------

                FilterType.FAVOURITE -> {

                    filterPhotoItems { photo ->

                        FavoritePreferences.isFavorite(
                            requireContext(),
                            photo.id
                        )
                    }
                }


                // -------------------------------------------------
                // SCREENSHOTS
                // -------------------------------------------------

                FilterType.SCREENSHOTS -> {

                    filterPhotoItems { photo ->

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


        renderFilteredPhotoItems(
            filteredItems
        )
    }



    // =========================================================
// RENDER FILTERED PHOTO ITEMS
// =========================================================

    private fun renderFilteredPhotoItems(
        items: List<PhotoListItem>
    ) {

        photoList.clear()


        // -----------------------------------------------------
        // Extract actual photos
        // -----------------------------------------------------

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
        // No result
        // -----------------------------------------------------

        if (
            photoList.isEmpty()
        ) {

            binding.recyclerPhotos.visibility =
                View.GONE

            binding.layoutEmptyState.visibility =
                View.VISIBLE

            binding.tvSubtitle.text =
                "0 Photos"

            return
        }


        binding.layoutEmptyState.visibility =
            View.GONE

        binding.recyclerPhotos.visibility =
            View.VISIBLE


        // -----------------------------------------------------
        // Subtitle
        // -----------------------------------------------------

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
                // Photo click
                // -------------------------------------------------

                onPhotoClick = {
                        photoItem,
                        _ ->

                    // PhotoListItem.Photo → ImageModel
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


                    val previewPhoto =
                        PhotoPreviewFragment()


                    previewPhoto.arguments =
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
                            previewPhoto
                        )
                        .addToBackStack(
                            "photo_preview"
                        )
                        .commit()
                },


                // -------------------------------------------------
                // Long press
                // -------------------------------------------------

                onPhotoLongClick = {
                        photo ->

                    viewModel.enterSelectionMode(
                        photo.image
                    )
                },


                // -------------------------------------------------
                // Selection
                // -------------------------------------------------

                onPhotoToggleSelect = {
                        photo ->

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

        binding.recyclerPhotos.adapter =
            photosAdapter


        binding.recyclerPhotos.layoutManager =

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
// FILTER PHOTO ITEMS
// =========================================================

    private fun filterPhotoItems(
        predicate: (ImageModel) -> Boolean
    ): List<PhotoListItem> {

        val result =
            mutableListOf<PhotoListItem>()


        var pendingHeader:
                PhotoListItem? = null


        for (
        item in allPhotoItems
        ) {

            when (item) {

                is PhotoListItem.Photo -> {

                    if (
                        predicate(
                            item.image
                        )
                    ) {

                        /*
                         * Add the date header only when
                         * this group has a matching photo.
                         */

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

                    /*
                     * Keep the header temporarily.
                     *
                     * If no photo below it matches,
                     * the header is never added.
                     */

                    pendingHeader =
                        item
                }
            }
        }


        return result
    }

    // SELECTION MENU


    private fun setupSelectionMenu() {

        binding.btnSelectionMenu.setOnClickListener {

            val selectedCount =
                viewModel
                    .selectedPhotoIds
                    .value
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
                        }
                    }


                    // -------------------------------------------------
                    // FAVORITE
                    // -------------------------------------------------

                    PhotoSelectionAction.FAVORITE -> {

                        toggleFavoriteSelected()
                    }


                    // -------------------------------------------------
                    // SLIDESHOW
                    // -------------------------------------------------

                    PhotoSelectionAction.SLIDE_SHOW -> {

                        val selectedPhotos =
                            viewModel
                                .getSelectedPhotos()

                        if (
                            selectedPhotos.isEmpty()
                        ) {
                            return@show
                        }

                        startSlideShow(
                            selectedPhotos,
                            0
                        )
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

                        confirmAndDeleteSelectedPhotos()
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
                        }
                    }
                }
            }
        }
    }


    // DETAILS


    private fun showSelectedDetails() {

        val selectedPhotos =
            viewModel.getSelectedPhotos()

        PhotoMenuActions.showDetails(
            this,
            selectedPhotos
        )
    }


    // FAVORITE


    /**
     * Adds all selected photos to Favorites.
     * Already-favorited photos are left unchanged.
     */

    private fun toggleFavoriteSelected() {

        val selectedPhotos =
            viewModel.getSelectedPhotos()

        if (selectedPhotos.isEmpty()) {
            return
        }

        PhotoSelectionActions.addToFavorites(
            requireContext(),
            selectedPhotos
        )

        viewModel.exitSelectionMode()
    }


    // RENAME


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

                        viewModel.loadPhotos(
                            requireContext(),
                            folderName,
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


                dialog.dismiss()
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

                viewModel.loadPhotos(
                    requireContext(),
                    folderName,
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


    // OPEN WITH


    private fun openPhotoWith(
        photo: ImageModel
    ) {

        PhotoMenuActions.openWith(
            requireContext(),
            photo
        )
    }


    // EDIT WITH


    private fun openPhotoForEdit(
        photo: ImageModel
    ) {

        PhotoMenuActions.editWith(
            requireContext(),
            photo
        )
    }


    // WALLPAPER


    private fun showWallpaperDialog(
        photo: ImageModel
    ) {

        PhotoMenuActions.showWallpaperDialog(
            this,
            photo
        )
    }


    // SLIDESHOW


    private fun startSlideShow(
        images: List<ImageModel>,
        position: Int
    ) {

        if (images.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "No photos available",
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
                "slide_show"
            )
            .commit()
    }


    // SHARE


    private fun shareSelectedPhotos() {

        val selectedPhotos =
            viewModel.getSelectedPhotos()

        if (selectedPhotos.isEmpty()) {
            return
        }

        PhotoSelectionActions.share(
            requireContext(),
            selectedPhotos
        )
    }

    // COPY / MOVE ALBUM PICKER
    private fun showPhotoAlbumPicker(
        mode: AlbumPickerBottomSheet.Mode
    ) {

        val selectedPhotos =
            viewModel.getSelectedPhotos()

        if (selectedPhotos.isEmpty()) {

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
            "PhotoAlbumPicker"
        )
    }


    // COPY


    private fun performPhotoCopy(
        photos: List<ImageModel>,
        destinationAlbumName: String
    ) {

        if (photos.isEmpty()) {
            return
        }


        viewLifecycleOwner.lifecycleScope.launch {

            val result =
                PhotoSelectionActions.copy(
                    requireContext(),
                    viewModel.repository,
                    photos,
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


                viewModel.loadPhotos(
                    requireContext(),
                    folderName,
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


    // MOVE


    private fun performPhotoMove(
        photos: List<ImageModel>,
        destinationAlbumName: String
    ) {

        if (photos.isEmpty()) {
            return
        }


        val uris =
            photos.map {
                it.uri
            }


        viewLifecycleOwner.lifecycleScope.launch {

            val copyResult =
                PhotoSelectionActions.move(
                    requireContext(),
                    viewModel.repository,
                    photos,
                    destinationAlbumName
                )


            if (copyResult.isSuccess) {

                /*
                 * Copy succeeded.
                 * Now delete the originals.
                 */

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
                        copyResult
                            .exceptionOrNull()
                            ?.message
                    }",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // DELETE / TRASH


    private fun confirmAndDeleteSelectedPhotos() {

        val selectedPhotos =
            viewModel.getSelectedPhotos()


        if (selectedPhotos.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "No photos selected",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        val count =
            selectedPhotos.size


        AlertDialog.Builder(
            requireContext()
        )
            .setTitle(
                "Delete $count item${
                    if (count > 1) "s" else ""
                }?"
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

            if (result.isFailure) {

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


            /*
             * Trash copies are now created.
             *
             * Delete the original MediaStore items
             * using the existing DeleteResult flow.
             */

            val trashItems =
                TrashStorage.getAll(
                    requireContext()
                ).filter { item ->

                    photos.any {
                        it.id == item.id
                    }
                }


            pendingTrashItems =
                trashItems


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


    // DELETE PERMISSION


    private val deleteIntentSenderLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { activityResult ->

            if (
                activityResult.resultCode ==
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
                    ) { result ->

                        handleDeleteResult(
                            result,
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
            // Success
            // -------------------------------------------------

            is DeleteResult.Success -> {

                onDeleteFinished(
                    successMessage
                )
            }


            // -------------------------------------------------
            // Android 11+
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
            // Android 10
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
            // Error
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


    private fun onDeleteFinished(
        successMessage: String
    ) {

        Toast.makeText(
            requireContext(),
            successMessage,
            Toast.LENGTH_SHORT
        ).show()


        viewModel.exitSelectionMode()


        viewModel.loadPhotos(
            requireContext(),
            folderName,
            currentSortType,
            currentSortOrder
        )
    }


    // LAYOUT


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


            binding.recyclerPhotos.layoutManager =
                if (isGrid) {

                    buildGridLayoutManager()

                } else {

                    LinearLayoutManager(
                        requireContext()
                    )
                }
        }
    }


    private fun applyColumn(
        column: Int
    ) {

        gridSpanCount =
            column


        if (isGridView) {

            binding.recyclerPhotos.layoutManager =
                buildGridLayoutManager()
        }
    }


    // SELECTION UI


    private fun updateSelectionMode(
        isSelecting: Boolean
    ) {

        if (isSelecting) {

            // -------------------------------------------------
            // Normal header
            // -------------------------------------------------

            binding.btnBack.visibility =
                View.GONE

            binding.tvTitle.visibility =
                View.GONE

            binding.tvSubtitle.visibility =
                View.GONE

            binding.btnMenu.visibility =
                View.GONE


            // -------------------------------------------------
            // Grid/List toggle
            // -------------------------------------------------

            binding.viewToggleGroup.visibility =
                View.GONE


            // -------------------------------------------------
            // Selection header
            // -------------------------------------------------

            binding.selectionHeader.visibility =
                View.VISIBLE


            // -------------------------------------------------
            // Selection filters
            // Only All Photos has filters
            // -------------------------------------------------

            binding.selectionFilters.visibility =
                if (folderName == null) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

        } else {

            // -------------------------------------------------
            // Normal header
            // -------------------------------------------------

            binding.btnBack.visibility =
                View.VISIBLE

            binding.tvTitle.visibility =
                View.VISIBLE

            binding.tvSubtitle.visibility =
                View.VISIBLE

            binding.btnMenu.visibility =
                View.VISIBLE


            // -------------------------------------------------
            // Grid/List
            // -------------------------------------------------

            binding.viewToggleGroup.visibility =
                View.VISIBLE


            // -------------------------------------------------
            // Selection UI
            // -------------------------------------------------

            binding.selectionHeader.visibility =
                View.GONE

            binding.selectionFilters.visibility =
                View.GONE
        }
    }


    private fun updateSelectionCount(
        selectedIds: Set<Long>
    ) {

        val selectedCount =
            selectedIds.size


        val totalPhotos =
            photoList.size


        binding.tvSelectedCount.text =
            "$selectedCount / $totalPhotos"


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


    // RENDER STATE


    private fun renderState(
        state: PhotosUiState
    ) {

        binding.progressBar.visibility =
            View.GONE

        binding.layoutEmptyState.visibility =
            View.GONE

        binding.recyclerPhotos.visibility =
            View.GONE


        when (state) {

            // -----------------------------------------------------
            // Loading
            // -----------------------------------------------------

            is PhotosUiState.Loading -> {

                binding.progressBar.visibility =
                    View.VISIBLE
            }


            // -----------------------------------------------------
            // Empty
            // -----------------------------------------------------

            is PhotosUiState.Empty -> {

                allPhotoItems =
                    emptyList()

                photoList.clear()


                binding.layoutEmptyState.visibility =
                    View.VISIBLE


                updateSelectionCount(
                    viewModel
                        .selectedPhotoIds
                        .value
                        ?: emptySet()
                )
            }


            // -----------------------------------------------------
            // Success
            // -----------------------------------------------------

            is PhotosUiState.Success -> {

                allPhotoItems =
                    state.items


                applyCurrentFilter()
            }
        }
    }
}