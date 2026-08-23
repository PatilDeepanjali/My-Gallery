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
import androidx.activity.OnBackPressedCallback
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
import com.example.mygallery.utils.TrashManager
import com.example.mygallery.utils.TrashStorage
import com.example.mygallery.viewmodel.PhotosViewModel
import com.example.mygallery.viewmodel.PhotosViewModelFactory
import kotlinx.coroutines.launch


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


    // ---------------------------------------------------------
    // Trash state
    // ---------------------------------------------------------

    private var pendingTrashItems:
            List<TrashItem> = emptyList()

    private var pendingDeleteRetryUris:
            List<android.net.Uri>? = null

    private var pendingDeleteSuccessMessage =
        "Moved to Trash"


    // ---------------------------------------------------------
    // Rename state
    // ---------------------------------------------------------

    private var pendingRenamePhoto:
            ImageModel? = null

    private var pendingRenameName:
            String? = null


  
    // RENAME PERMISSION
  

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


  
    // DELETE / TRASH PERMISSION
  

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


  
    // CREATE
  

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


  
    // CREATE VIEW
  

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


  
    // VIEW CREATED
  

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


  
    // RESUME
  

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


  
    // BASIC VIEWS
  

    private fun setupBasicViews() {

        binding.btnBack.setOnClickListener {

            parentFragmentManager
                .popBackStack()
        }
    }


  
    // NORMAL MENU
  

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


                    // -------------------------------------------------
                    // Not applicable to Favorites
                    // -------------------------------------------------

                    PhotoAction.PIN -> {
                        // Not applicable
                    }

                    PhotoAction.FILTER -> {
                        // Not applicable
                    }

                    PhotoAction.LAYOUT_STYLE -> {
                        // Favorites keeps the designed grid
                    }

                    PhotoAction.COLUMN -> {
                        // Favorites keeps the designed grid
                    }
                }
            }
        }
    }


  
    // SORT
  

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


  
    // SELECTION HEADER
  

    private fun setupSelectionHeader() {

        binding.btnExitSelection.setOnClickListener {

            viewModel.exitSelectionMode()
        }


        binding.btnSelectionMenu.setOnClickListener {

            showSelectionMenu()
        }
    }


  
    // ENTER SELECTION
  

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
         * Keep the same selection behavior as PhotoFragment:
         * entering selection mode selects the first photo.
         *
         * Long press on a photo still selects the exact
         * photo that was long pressed.
         */

        viewModel.enterSelectionMode(
            photoList.first()
        )
    }


  
    // SELECTION MENU
  

    private fun showSelectionMenu() {

        val selectedCount =
            viewModel
                .selectedPhotoIds
                .value
                ?.size
                ?: 0


        if (selectedCount == 0) {
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


  
    // DETAILS
  

    private fun showSelectedDetails() {

        val selectedPhotos =
            viewModel.getSelectedPhotos()

        if (selectedPhotos.isEmpty()) {
            return
        }

        PhotoMenuActions.showDetails(
            this,
            selectedPhotos
        )
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


                /*
                 * Don't dismiss if permission is required.
                 * The permission dialog is launched from here.
                 */

                if (
                    PhotoMenuActions.rename(
                        requireContext(),
                        photo,
                        newName
                    ) !is PhotoMenuActions
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


  
    // COPY / MOVE ALBUM PICKER
  

    private fun showPhotoAlbumPicker(
        mode: AlbumPickerBottomSheet.Mode
    ) {

        /*
         * Get the selection once.
         * The selected list is then passed through the
         * rest of the operation.
         */

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
            "FavoriteAlbumPicker"
        )
    }


  
    // COPY
  

    private fun performPhotoCopy(
        photos: List<ImageModel>,
        destinationAlbumName: String
    ) {

        /*
         * Do NOT call getSelectedPhotos() again.
         *
         * The selected list was already captured when
         * the album picker was opened.
         */

        if (
            photos.isEmpty()
        ) {
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


                /*
                 * Favorites must reload Favorites,
                 * not Photos.
                 */

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


  
    // MOVE
  

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


        viewLifecycleOwner.lifecycleScope.launch {

            /*
             * First copy the photos to the destination.
             */

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


            /*
             * Only delete the originals after the copy
             * succeeds.
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
        }
    }


  
    // REMOVE FROM FAVORITES
  

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


  
    // SHARE
  

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


  
    // DELETE → CUSTOM TRASH
  

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


            /*
             * Trash copy has been created.
             *
             * Now delete the original MediaStore items.
             */

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


  
    // DELETE RESULT
  

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
            // ANDROID 11+
            // -------------------------------------------------

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


            // -------------------------------------------------
            // ANDROID 10
            // -------------------------------------------------

            is DeleteResult
            .GrantPermissionThenRetry -> {

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


            // -------------------------------------------------
            // ERROR
            // -------------------------------------------------

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

        /*
         * If this was a Trash operation, the custom Trash
         * metadata/copy should remain.
         *
         * Only the original MediaStore item is deleted.
         */


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


  
    // SLIDESHOW
  

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


  
    // OBSERVERS
  

    private fun setupObservers() {

        // -----------------------------------------------------
        // UI state
        // -----------------------------------------------------

        viewModel.uiState.observe(
            viewLifecycleOwner
        ) { state ->

            renderState(
                state
            )
        }


        // -----------------------------------------------------
        // Selection mode
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


  
    // RENDER STATE
  

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

                binding.recyclerFavorites.visibility =
                    View.VISIBLE


                /*
                 * Extract only actual photos.
                 * Date headers are ignored.
                 */

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


                binding.tvSubtitle.text =
                    "${photoList.size} Photos"


                // -------------------------------------------------
                // Adapter
                // -------------------------------------------------

                photosAdapter =
                    PhotosAdapter(
                        isGridView = true,
                        items = state.items,

                        // -----------------------------------------
                        // Normal photo click
                        // -----------------------------------------

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


                        // -----------------------------------------
                        // Long press
                        // -----------------------------------------

                        onPhotoLongClick = {
                                photo ->

                            viewModel.enterSelectionMode(
                                photo.image
                            )
                        },


                        // -----------------------------------------
                        // Tap while selecting
                        // -----------------------------------------

                        onPhotoToggleSelect = {
                                photo ->

                            viewModel.toggleSelection(
                                photo.image
                            )
                        }
                    )


                // -------------------------------------------------
                // Restore selection
                // -------------------------------------------------

                photosAdapter.setSelectionState(
                    viewModel
                        .isSelectionMode
                        .value == true,

                    viewModel
                        .selectedPhotoIds
                        .value
                        ?: emptySet()
                )


                // -------------------------------------------------
                // RecyclerView
                // -------------------------------------------------

                binding.recyclerFavorites.adapter =
                    photosAdapter


                binding.recyclerFavorites.layoutManager =
                    buildGridLayoutManager()


                // -------------------------------------------------
                // Header
                // -------------------------------------------------

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
        }
    }


  
    // SELECTION UI
  

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


  
    // GRID
  

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


  
    // DESTROY


    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}