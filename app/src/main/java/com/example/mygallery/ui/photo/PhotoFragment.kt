package com.example.mygallery.ui.photo

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
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
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.MainActivity
import com.example.mygallery.ui.album.AlbumPickerBottomSheet
import com.example.mygallery.ui.photo.details.PhotoDetailsBottomSheet
import com.example.mygallery.ui.photo.menu.ColumnBottomSheet
import com.example.mygallery.ui.photo.menu.LayoutStyleBottomSheet
import com.example.mygallery.ui.photo.menu.PhotoAction
import com.example.mygallery.ui.photo.menu.PhotoActionPopup
import com.example.mygallery.ui.photo.menu.SortOrder
import com.example.mygallery.ui.photo.menu.SortType
import com.example.mygallery.ui.photo.selection.PhotoSelectionAction
import com.example.mygallery.ui.photo.selection.PhotoSelectionActionPopup
import com.example.mygallery.ui.photo.slideshow.SlideShowFragment
import com.example.mygallery.ui.state.PhotosUiState
import com.example.mygallery.viewmodel.PhotosViewModel
import com.example.mygallery.viewmodel.PhotosViewModelFactory
import kotlinx.coroutines.launch
import android.app.WallpaperManager
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import com.example.mygallery.model.TrashItem
import com.example.mygallery.ui.photo.menu.PhotoMenuActions
import com.example.mygallery.ui.photo.menu.PhotoSelectionActions
import com.example.mygallery.utils.FavoritePreferences
import com.example.mygallery.utils.TrashManager
import com.example.mygallery.utils.TrashStorage

class PhotoFragment : Fragment() {

    companion object {
        const val ARG_FOLDER_NAME = "arg_folder_name"
    }

    private var photoList = ArrayList<ImageModel>()

    private lateinit var binding: FragmentPhotoBinding
    private lateinit var viewModel: PhotosViewModel
    private lateinit var photosAdapter: PhotosAdapter

    private var folderName: String? = null

    private var isGridView = true
    private var gridSpanCount = 4

    private var currentSortType = SortType.DATE_TAKEN
    private var currentSortOrder = SortOrder.DESCENDING

    // ---------------------------------------------------------
    // Delete state
    // ---------------------------------------------------------

    private var pendingDeleteRetryUris: List<android.net.Uri>? = null

    private var pendingDeleteSuccessMessage = "Deleted"


    private var pendingTrashItems: List<TrashItem> = emptyList()
    private var pendingRenamePhoto: ImageModel? = null
    private var pendingRenameName: String? = null

    private val renamePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val photo = pendingRenamePhoto
                val newName = pendingRenameName

                if (photo != null && newName != null) {

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

    // ---------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        folderName = arguments?.getString(ARG_FOLDER_NAME)

        val repository = GalleryRepository()

        val factory =
            PhotosViewModelFactory(repository)

        viewModel =
            ViewModelProvider(this, factory)[PhotosViewModel::class.java]
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
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
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    if (viewModel.isSelectionMode.value == true) {

                        // Exit selection mode
                        viewModel.exitSelectionMode()

                    } else {

                        // Normal Android Back behavior
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
         * PhotoFragment is a normal screen, so the bottom navigation
         * must be visible when we return from Preview/Slideshow.
         */
        (activity as? MainActivity)
            ?.showBottomNavigation()

        /*
         * Restore the current selection state after returning from
         * another Fragment such as SlideShowFragment.
         */
        if (::photosAdapter.isInitialized) {

            photosAdapter.setSelectionState(
                viewModel.isSelectionMode.value == true,
                viewModel.selectedPhotoIds.value ?: emptySet()
            )

            updateSelectionMode(
                viewModel.isSelectionMode.value == true
            )

            updateSelectionCount(
                viewModel.selectedPhotoIds.value ?: emptySet()
            )
        }
    }


    // =========================================================
    // OBSERVERS
    // =========================================================

    private fun setupObservers() {

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }


        viewModel.isSelectionMode.observe(
            viewLifecycleOwner
        ) { isSelecting ->

            updateSelectionMode(isSelecting)

            if (::photosAdapter.isInitialized) {

                photosAdapter.setSelectionState(
                    isSelecting,
                    viewModel.selectedPhotoIds.value
                        ?: emptySet()
                )
            }
        }


        viewModel.selectedPhotoIds.observe(
            viewLifecycleOwner
        ) { selectedIds ->

            updateSelectionCount(selectedIds)

            if (::photosAdapter.isInitialized) {

                photosAdapter.setSelectionState(
                    viewModel.isSelectionMode.value == true,
                    selectedIds
                )
            }
        }
    }


    // =========================================================
    // BASIC UI
    // =========================================================

    private fun setupBasicViews() {

        binding.tvTitle.text =
            folderName ?: "Photos"

        binding.recyclerPhotos.layoutManager =
            buildGridLayoutManager()


        binding.btnGridView.setOnClickListener {
            applyLayoutStyle(true)
        }


        binding.btnListView.setOnClickListener {
            applyLayoutStyle(false)
        }


        binding.btnExitSelection.setOnClickListener {
            viewModel.exitSelectionMode()
        }


        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
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
                         * Selection mode is normally entered by
                         * long-pressing a photo.
                         *
                         * Keep this empty for now unless your Figma
                         * requires tapping SELECT to enter it.
                         */
                    }


                    PhotoAction.PIN -> {
                        // Implement later
                    }


                    PhotoAction.SORT -> {

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
                        // Implement later
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

                                    applyLayoutStyle(isGrid)
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

                                    applyColumn(column)
                                }
                            }
                        )

                        sheet.show(
                            parentFragmentManager,
                            "column"
                        )
                    }


                    PhotoAction.SLIDE_SHOW -> {

                        /*
                         * Normal three-dot menu:
                         * slideshow uses ALL photos currently
                         * displayed on this screen.
                         */
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
    // SELECTION MENU
    // =========================================================

    private fun setupSelectionMenu() {

        binding.btnSelectionMenu.setOnClickListener {

            val selectedCount =
                viewModel.selectedPhotoIds.value?.size ?: 0

            if (selectedCount == 0) {
                return@setOnClickListener
            }


            PhotoSelectionActionPopup.show(
                requireContext(),
                binding.btnSelectionMenu,
                selectedCount
            ) { action ->

                when (action) {
                    PhotoSelectionAction.COPY -> {

                        showPhotoAlbumPicker(
                            AlbumPickerBottomSheet.Mode.COPY
                        )
                    }

                    PhotoSelectionAction.MOVE -> {

                        showPhotoAlbumPicker(
                            AlbumPickerBottomSheet.Mode.MOVE
                        )
                    }

                    PhotoSelectionAction.RENAME -> {

                        val selectedPhotos =
                            viewModel.getSelectedPhotos()

                        if (selectedPhotos.size == 1) {

                            showRenameDialog(
                                selectedPhotos.first()
                            )
                        }
                    }

                    PhotoSelectionAction.FAVORITE -> {

                        toggleFavoriteSelected()
                    }

                    PhotoSelectionAction.SLIDE_SHOW -> {

                        val selectedPhotos =
                            viewModel.getSelectedPhotos()

                        if (selectedPhotos.isEmpty()) {
                            return@show
                        }

                        startSlideShow(
                            selectedPhotos,
                            0
                        )
                    }


                    PhotoSelectionAction.EDIT_WITH -> {

                        val selectedPhotos =
                            viewModel.getSelectedPhotos()

                        if (selectedPhotos.size == 1) {

                            openPhotoForEdit(
                                selectedPhotos.first()
                            )
                        }
                    }

                    PhotoSelectionAction.SET_AS_WALLPAPER -> {

                        val selectedPhotos =
                            viewModel.getSelectedPhotos()

                        if (selectedPhotos.size == 1) {

                            showWallpaperDialog(
                                selectedPhotos.first()
                            )
                        }
                    }

                    PhotoSelectionAction.SHARE -> {

                        shareSelectedPhotos()
                    }

                    PhotoSelectionAction.DELETE -> {

                        confirmAndDeleteSelectedPhotos()
                    }

                    PhotoSelectionAction.DETAILS -> {
                        showSelectedDetails()

                    }

                    PhotoSelectionAction.OPEN_WITH -> {

                        val selectedPhotos =
                            viewModel.getSelectedPhotos()

                        if (selectedPhotos.size == 1) {

                            openPhotoWith(
                                selectedPhotos.first()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showSelectedDetails() {

        val selectedPhotos =
            viewModel.getSelectedPhotos()

        PhotoMenuActions.showDetails(
            this,
            selectedPhotos
        )
    }
    /**
     * Behavior for a mixed selection (some favorited, some not):
     * if EVERY selected photo is already favorited, this removes
     * them all from Favorites. Otherwise, it favorites whichever
     * selected photos aren't already favorited (existing favorites
     * in the selection are left untouched). This matches how most
     * gallery apps handle a bulk "Favorite" tap on a mixed selection —
     * a single tap shouldn't accidentally unfavorite something the
     * user didn't mean to touch.
     */
    private fun toggleFavoriteSelected() {

        val selectedPhotos =
            viewModel.getSelectedPhotos()

        PhotoSelectionActions.addToFavorites(
            requireContext(),
            selectedPhotos
        )

        viewModel.exitSelectionMode()
    }


    // Rename
    private fun showRenameDialog(
        photo: ImageModel
    ) {

        val editText = EditText(requireContext()).apply {

            setSingleLine(true)

            setText(
                photo.name.substringBeforeLast(
                    ".",
                    photo.name
                )
            )

            selectAll()
        }


        val container = LinearLayout(
            requireContext()
        ).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                24,
                0,
                24,
                0
            )

            addView(editText)
        }


        val dialog =
            AlertDialog.Builder(requireContext())
                .setTitle("Rename")
                .setMessage("Enter a name for this album.")
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


                when (
                    val result =
                        PhotoMenuActions.rename(
                            requireContext(),
                            photo,
                            newName
                        )
                ) {

                    is PhotoMenuActions.RenameResult.Success -> {

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

                    is PhotoMenuActions.RenameResult.Error -> {

                        Toast.makeText(
                            requireContext(),
                            result.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    is PhotoMenuActions.RenameResult.NeedsPermission -> {

                        pendingRenamePhoto = photo
                        pendingRenameName = newName

                        val request =
                            IntentSenderRequest.Builder(
                                result.intentSender
                            ).build()

                        renamePermissionLauncher.launch(
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

            is PhotoMenuActions.RenameResult.Success -> {

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

            is PhotoMenuActions.RenameResult.Error -> {

                Toast.makeText(
                    requireContext(),
                    result.message,
                    Toast.LENGTH_LONG
                ).show()
            }

            is PhotoMenuActions.RenameResult.NeedsPermission -> {

                // Permission was already requested.
                // If Android asks again, launch it again.
                val request =
                    IntentSenderRequest.Builder(
                        result.intentSender
                    ).build()

                renamePermissionLauncher.launch(
                    request
                )
            }
        }
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

    // Open With

    private fun openPhotoWith(
        photo: ImageModel
    ) {
        PhotoMenuActions.openWith(
            requireContext(),
            photo
        )
    }

    // Edit With

    private fun openPhotoForEdit(
        photo: ImageModel
    ) {
        PhotoMenuActions.editWith(
            requireContext(),
            photo
        )
    }

    // Set AS Wallpaper

    private fun showWallpaperDialog(
        photo: ImageModel
    ) {

        PhotoMenuActions.showWallpaperDialog(
            this,
            photo
        )
    }


    private fun applyWallpaper(
        photo: ImageModel,
        option: Int
    ) {

        try {

            val wallpaperManager =
                WallpaperManager.getInstance(requireContext())

            val inputStream =
                requireContext()
                    .contentResolver
                    .openInputStream(photo.uri)
                    ?: return

            val originalBitmap =
                BitmapFactory.decodeStream(inputStream)

            inputStream.close()

            if (originalBitmap == null) {
                Toast.makeText(
                    requireContext(),
                    "Unable to load image",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val targetWidth =
                wallpaperManager.desiredMinimumWidth

            val targetHeight =
                wallpaperManager.desiredMinimumHeight

            val wallpaperBitmap =
                createWallpaperBitmap(
                    originalBitmap,
                    targetWidth,
                    targetHeight
                )

            when (option) {

                // Home Screen
                0 -> {

                    wallpaperManager.setBitmap(
                        wallpaperBitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_SYSTEM
                    )
                }

                // Lock Screen
                1 -> {

                    wallpaperManager.setBitmap(
                        wallpaperBitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_LOCK
                    )
                }

                // Both
                2 -> {

                    wallpaperManager.setBitmap(
                        wallpaperBitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_SYSTEM
                                or WallpaperManager.FLAG_LOCK
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

        val sourceWidth = source.width
        val sourceHeight = source.height

        val sourceRatio =
            sourceWidth.toFloat() / sourceHeight

        val targetRatio =
            targetWidth.toFloat() / targetHeight

        val cropWidth: Int
        val cropHeight: Int
        val cropX: Int
        val cropY: Int

        if (sourceRatio > targetRatio) {

            // Image is wider than target.
            // Crop left and right.
            cropHeight = sourceHeight
            cropWidth =
                (sourceHeight * targetRatio).toInt()

            cropX =
                (sourceWidth - cropWidth) / 2

            cropY = 0

        } else {

            // Image is taller than target.
            // Crop top and bottom.
            cropWidth = sourceWidth
            cropHeight =
                (sourceWidth / targetRatio).toInt()

            cropX = 0

            cropY =
                (sourceHeight - cropHeight) / 2
        }

        val croppedBitmap =
            Bitmap.createBitmap(
                source,
                cropX,
                cropY,
                cropWidth,
                cropHeight
            )

        return Bitmap.createScaledBitmap(
            croppedBitmap,
            targetWidth,
            targetHeight,
            true
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


        parentFragmentManager.beginTransaction()
            .replace(
                R.id.frameContainer,
                slideshow
            )
            .addToBackStack("slide_show")
            .commit()
    }


    // =========================================================
    // SHARE
    // =========================================================

    private fun shareSelectedPhotos() {

        PhotoSelectionActions.share(
            requireContext(),
            viewModel.getSelectedPhotos()
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


    // =========================================================
    // COPY
    // =========================================================

    private fun performPhotoCopy(
        photos: List<ImageModel>,
        destinationAlbumName: String
    ) {

        val uris =
            photos.map { it.uri }

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


                viewModel.loadPhotos(
                    requireContext(),
                    folderName,
                    currentSortType,
                    currentSortOrder
                )

            } else {

                Toast.makeText(
                    requireContext(),
                    "Copy failed: ${result.exceptionOrNull()?.message}",
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

        val uris =
            photos.map { it.uri }

        if (uris.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "No photos to move",
                Toast.LENGTH_SHORT
            ).show()

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
                    "Move failed: ${copyResult.exceptionOrNull()?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // =========================================================
    // DELETE
    // =========================================================

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

        AlertDialog.Builder(requireContext())
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
                        result.exceptionOrNull()?.message
                    }",
                    Toast.LENGTH_LONG
                ).show()

                return@moveImagesToTrash
            }

            /*
             * The Trash copies are now created.
             *
             * We still need to delete the original MediaStore
             * items using the existing DeleteResult flow.
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


                deleteIntentSenderLauncher.launch(
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


                deleteIntentSenderLauncher.launch(
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


    // =========================================================
    // LAYOUT
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


    private fun applyLayoutStyle(
        isGrid: Boolean
    ) {

        isGridView = isGrid


        if (::photosAdapter.isInitialized) {

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

        gridSpanCount = column


        if (isGridView) {

            binding.recyclerPhotos.layoutManager =
                buildGridLayoutManager()
        }
    }


    // =========================================================
    // SELECTION UI
    // =========================================================

    private fun updateSelectionMode(
        isSelecting: Boolean
    ) {

        if (isSelecting) {

            // Normal header
            binding.btnBack.visibility =
                View.GONE

            binding.tvTitle.visibility =
                View.GONE

            binding.tvSubtitle.visibility =
                View.GONE

            binding.btnMenu.visibility =
                View.GONE


            // Grid/List toggle
            binding.viewToggleGroup.visibility =
                View.GONE


            // Selection header
            binding.selectionHeader.visibility =
                View.VISIBLE


            /*
             * Horizontal filter chips exist ONLY
             * on the All Photos screen.
             *
             * Album screen:
             * folderName != null
             * → filters hidden.
             */
            binding.selectionFilters.visibility =
                if (folderName == null) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

        } else {

            // Normal header
            binding.btnBack.visibility =
                View.VISIBLE

            binding.tvTitle.visibility =
                View.VISIBLE

            binding.tvSubtitle.visibility =
                View.VISIBLE

            binding.btnMenu.visibility =
                View.VISIBLE


            // Grid/List
            binding.viewToggleGroup.visibility =
                View.VISIBLE


            // Selection UI
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

        binding.recyclerPhotos.visibility =
            View.GONE


        when (state) {

            is PhotosUiState.Loading -> {

                binding.progressBar.visibility =
                    View.VISIBLE
            }


            is PhotosUiState.Empty -> {

                photoList.clear()

                binding.layoutEmptyState.visibility =
                    View.VISIBLE

                updateSelectionCount(
                    viewModel.selectedPhotoIds.value
                        ?: emptySet()
                )
            }


            is PhotosUiState.Success -> {

                binding.recyclerPhotos.visibility =
                    View.VISIBLE


                // ---------------------------------------------
                // Extract only actual photos.
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


                val photoCount =
                    photoList.size


                binding.tvSubtitle.text =
                    "$photoCount Photos"


                // ---------------------------------------------
                // Create adapter
                // ---------------------------------------------

                photosAdapter =
                    PhotosAdapter(
                        isGridView = isGridView,
                        items = state.items,

                        // Normal tap
                        onPhotoClick = {
                                photo,
                                position ->

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
                                        position
                                    )
                                }


                            parentFragmentManager
                                .beginTransaction()
                                .replace(
                                    R.id.frameContainer,
                                    previewPhoto
                                )
                                .addToBackStack(null)
                                .commit()
                        },


                        // Long press
                        onPhotoLongClick = {
                                photo ->

                            viewModel
                                .enterSelectionMode(
                                    photo.image
                                )
                        },


                        // Tap while selecting
                        onPhotoToggleSelect = {
                                photo ->

                            viewModel
                                .toggleSelection(
                                    photo.image
                                )
                        }
                    )


                // ---------------------------------------------
                // IMPORTANT:
                // Restore selection state immediately.
                //
                // This fixes:
                // "count is visible but filled circles
                // are missing after returning from slideshow."
                // ---------------------------------------------

                photosAdapter.setSelectionState(
                    viewModel.isSelectionMode.value == true,
                    viewModel.selectedPhotoIds.value
                        ?: emptySet()
                )


                binding.recyclerPhotos.adapter =
                    photosAdapter


                // ---------------------------------------------
                // Layout manager
                // ---------------------------------------------

                binding.recyclerPhotos.layoutManager =
                    if (isGridView) {

                        buildGridLayoutManager()

                    } else {

                        LinearLayoutManager(
                            requireContext()
                        )
                    }


                // ---------------------------------------------
                // Update selection count AFTER photoList
                // has been rebuilt.
                // ---------------------------------------------

                updateSelectionCount(
                    viewModel.selectedPhotoIds.value
                        ?: emptySet()
                )


                // Restore selection UI after rendering.
                updateSelectionMode(
                    viewModel.isSelectionMode.value == true
                )
            }
        }
    }



}