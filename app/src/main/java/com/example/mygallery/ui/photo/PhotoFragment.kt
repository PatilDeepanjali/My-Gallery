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

                        Toast.makeText(
                            requireContext(),
                            "Favorite coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
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

                        val selectedPhotos =
                            viewModel.getSelectedPhotos()

                        if (selectedPhotos.isNotEmpty()) {

                            PhotoDetailsBottomSheet(
                                ArrayList(selectedPhotos)
                            ).show(
                                childFragmentManager,
                                "photo_details"
                            )
                        }
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

        try {

            val mimeType =
                requireContext()
                    .contentResolver
                    .getType(photo.uri)
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

    // Edit With

    private fun openPhotoForEdit(
        photo: ImageModel
    ) {

        try {

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

                    addFlags(
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }


            startActivity(
                Intent.createChooser(
                    intent,
                    "Edit With"
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "No editing app available",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Set AS Wallpaper

    private fun showWallpaperDialog(
        photo: ImageModel
    ) {

        val options = arrayOf(
            "Set On Home Screen",
            "Set On Lock Screen",
            "Set On Both Screen"
        )


        var selectedOption = 0


        AlertDialog.Builder(requireContext())
            .setTitle("Set Wallpaper")
            .setSingleChoiceItems(
                options,
                selectedOption
            ) { _, which ->

                selectedOption = which
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Apply"
            ) { _, _ ->

                applyWallpaper(
                    photo,
                    selectedOption
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

        val selectedIds =
            viewModel.selectedPhotoIds.value
                ?: emptySet()

        if (selectedIds.isEmpty()) {
            return
        }


        val selectedPhotos =
            photoList.filter {
                it.id in selectedIds
            }

        if (selectedPhotos.isEmpty()) {
            return
        }


        if (selectedPhotos.size == 1) {

            val photo =
                selectedPhotos.first()

            val mimeType =
                requireContext()
                    .contentResolver
                    .getType(photo.uri)
                    ?: "image/*"


            val intent =
                Intent(Intent.ACTION_SEND).apply {

                    type = mimeType

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
                ArrayList<android.net.Uri>()

            selectedPhotos.forEach { photo ->
                uris.add(photo.uri)
            }


            val intent =
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {

                    type = "image/*"

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


        val selectedUris =
            selectedPhotos.map { it.uri }

        val count =
            selectedUris.size


        AlertDialog.Builder(requireContext())
            .setTitle(
                "Delete $count item${if (count > 1) "s" else ""}?"
            )
            .setMessage(
                "This will permanently delete the selected items from your device. This cannot be undone."
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                viewModel.deleteImages(
                    requireContext(),
                    selectedUris
                ) { result ->

                    handleDeleteResult(
                        result,
                        "Deleted"
                    )
                }
            }
            .show()
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