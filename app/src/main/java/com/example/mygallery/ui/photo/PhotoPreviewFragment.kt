package com.example.mygallery.ui.photo

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
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
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2

import com.example.mygallery.R
import com.example.mygallery.adapter.PhotoPreviewAdapter
import com.example.mygallery.databinding.FragmentPhotoPreviewBinding
import com.example.mygallery.model.DeleteResult
import com.example.mygallery.model.ImageModel
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.MainActivity
import com.example.mygallery.ui.album.AlbumPickerBottomSheet
import com.example.mygallery.ui.photo.preview.PreviewAction
import com.example.mygallery.ui.photo.preview.PreviewActionPopup
import com.example.mygallery.ui.photo.slideshow.SlideShowFragment
import com.example.mygallery.utils.DateUtils

import android.text.format.Formatter
import androidx.lifecycle.ViewModelProvider
import com.example.mygallery.utils.FavoritePreferences
import com.example.mygallery.viewmodel.PhotosViewModel
import com.example.mygallery.viewmodel.PhotosViewModelFactory
import kotlinx.coroutines.launch

import com.example.mygallery.ui.photo.menu.PhotoMenuActions


class PhotoPreviewFragment : Fragment() {

    private var isFavorite = false

    private lateinit var binding: FragmentPhotoPreviewBinding

    private lateinit var imageList: ArrayList<ImageModel>

    private lateinit var viewModel: PhotosViewModel

    private lateinit var repository: GalleryRepository

    private var clickedPosition = 0


    // =========================================================
    // DELETE STATE
    // =========================================================

    private var pendingDeleteRetryUris: List<android.net.Uri>? = null

    private var pendingDeleteSuccessMessage = "Deleted"

    private var pendingRenamePhoto: ImageModel? = null
    private var pendingRenameName: String? = null

    private val renamePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val photo =
                    pendingRenamePhoto

                val newName =
                    pendingRenameName

                if (
                    photo != null &&
                    newName != null
                ) {

                    performRename(
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
    // DELETE PERMISSION RESULT
    // =========================================================
    private val deleteIntentSenderLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { activityResult ->

            if (activityResult.resultCode == Activity.RESULT_OK) {

                val retryUris = pendingDeleteRetryUris

                pendingDeleteRetryUris = null

                if (retryUris != null) {

                    viewLifecycleOwner.lifecycleScope.launch {

                        val result =
                            repository.deleteImages(
                                requireContext(),
                                retryUris
                            )

                        handlePreviewDeleteResult(
                            result,
                            pendingDeleteSuccessMessage
                        )
                    }

                } else {

                    onPreviewDeleteFinished(
                        pendingDeleteSuccessMessage
                    )
                }

            } else {

                pendingDeleteRetryUris = null

                Toast.makeText(
                    requireContext(),
                    "Delete cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        repository =
            GalleryRepository()
        val factory =
            PhotosViewModelFactory(repository)

        viewModel =
            ViewModelProvider(this, factory)[PhotosViewModel::class.java]

        imageList =
            requireArguments()
                .getParcelableArrayList(
                    ARG_IMAGE_LIST
                )
                ?: arrayListOf()


        clickedPosition =
            requireArguments()
                .getInt(
                    ARG_POSITION,
                    0
                )
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            FragmentPhotoPreviewBinding.inflate(
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


        if (imageList.isEmpty()) {

            parentFragmentManager
                .popBackStack()

            return
        }


        setupButtons()

        setupViewPager()
    }


    // =========================================================
    // BUTTONS
    // =========================================================

    private fun setupButtons() {

        updatePhotoInfo(
            clickedPosition
        )


        // -----------------------------------------------------
        // BACK
        // -----------------------------------------------------

        binding.btnBack.setOnClickListener {

            parentFragmentManager
                .popBackStack()
        }


        // -----------------------------------------------------
        // FAVORITE
        // -----------------------------------------------------

        binding.btnFavorite.setOnClickListener {

            val photo = imageList[clickedPosition]

            FavoritePreferences.toggleFavorite(
                requireContext(),
                photo.id
            )

            updateFavoriteIcon(photo)
        }


        // -----------------------------------------------------
        // EDIT
        // -----------------------------------------------------

        binding.layoutEdit.setOnClickListener {

            val photo =
                imageList[clickedPosition]

            editPhoto(photo)
        }


        // -----------------------------------------------------
        // SHARE
        // -----------------------------------------------------

        binding.layoutShare.setOnClickListener {

            val photo =
                imageList[clickedPosition]

            sharePhoto(photo)
        }


        // -----------------------------------------------------
        // DELETE
        // -----------------------------------------------------

        binding.layoutDelete.setOnClickListener {

            val photo =
                imageList[clickedPosition]

            showDeleteConfirmation(photo)
        }


        // -----------------------------------------------------
        // MORE
        // -----------------------------------------------------

        binding.layoutMore.setOnClickListener {

            PreviewActionPopup.show(
                requireContext(),
                binding.layoutMore
            ) { action ->

                when (action) {

                    // -----------------------------------------
                    // COPY
                    // -----------------------------------------

                    PreviewAction.COPY -> {

                        showPreviewAlbumPicker(
                            AlbumPickerBottomSheet.Mode.COPY
                        )
                    }


                    // -----------------------------------------
                    // MOVE
                    // -----------------------------------------

                    PreviewAction.MOVE -> {

                        showPreviewAlbumPicker(
                            AlbumPickerBottomSheet.Mode.MOVE
                        )
                    }


                    // -----------------------------------------
                    // RENAME
                    // -----------------------------------------

                    PreviewAction.RENAME -> {

                        val photo =
                            imageList[clickedPosition]

                        showRenameDialog(photo)
                    }


                    // -----------------------------------------
                    // OPEN WITH
                    // -----------------------------------------

                    PreviewAction.OPEN_WITH -> {

                        val photo =
                            imageList[clickedPosition]

                        openPhotoWith(photo)
                    }


                    // -----------------------------------------
                    // SLIDE SHOW
                    // -----------------------------------------

                    PreviewAction.SLIDE_SHOW -> {

                        parentFragmentManager
                            .beginTransaction()
                            .replace(
                                R.id.frameContainer,
                                SlideShowFragment
                                    .newInstance(
                                        imageList,
                                        clickedPosition
                                    )
                            )
                            .addToBackStack(
                                "slide_show"
                            )
                            .commit()
                    }


                    // -----------------------------------------
                    // WALLPAPER
                    // -----------------------------------------

                    PreviewAction.WALLPAPER -> {

                        val photo =
                            imageList[clickedPosition]

                        showWallpaperDialog(photo)
                    }


                    // -----------------------------------------
                    // DETAILS
                    // -----------------------------------------

                    PreviewAction.DETAILS -> {

                        val photo =
                            imageList[clickedPosition]

                        showPhotoDetails(photo)
                    }
                }
            }
        }
    }


    // =========================================================
    // VIEW PAGER
    // =========================================================

    private fun setupViewPager() {

        binding.viewPagerPhotos.adapter =
            PhotoPreviewAdapter(imageList)


        binding.viewPagerPhotos.setCurrentItem(
            clickedPosition,
            false
        )


        binding.viewPagerPhotos
            .registerOnPageChangeCallback(
                object :
                    ViewPager2.OnPageChangeCallback() {

                    override fun onPageSelected(
                        position: Int
                    ) {

                        super.onPageSelected(
                            position
                        )

                        clickedPosition =
                            position

                        updatePhotoInfo(
                            position
                        )
                    }
                }
            )
    }


    // =========================================================
    // SHARE
    // =========================================================

    private fun sharePhoto(
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
                    Intent.ACTION_SEND
                ).apply {

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
                    "Share Image"
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "Unable to share image",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // COPY / MOVE ALBUM PICKER
    // =========================================================

    private fun showPreviewAlbumPicker(
        mode: AlbumPickerBottomSheet.Mode
    ) {

        val currentPhoto =
            imageList[clickedPosition]


        val excludedNames =
            listOf(
                currentPhoto.folderName
            )


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

                    performPreviewCopy(
                        currentPhoto,
                        destinationAlbumName
                    )

                } else {

                    performPreviewMove(
                        currentPhoto,
                        destinationAlbumName
                    )
                }
            }


        sheet.show(
            childFragmentManager,
            "PreviewAlbumPicker"
        )
    }


    // =========================================================
    // COPY
    // =========================================================

    private fun performPreviewCopy(
        photo: ImageModel,
        destinationAlbumName: String
    ) {

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                val result =
                    repository.copyImages(
                        requireContext(),
                        listOf(photo.uri),
                        destinationAlbumName
                    )


                if (result.isSuccess) {

                    val count =
                        result.getOrNull()
                            ?: 0


                    Toast.makeText(
                        requireContext(),
                        "Copied $count item to $destinationAlbumName",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Copy failed: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }


    // =========================================================
    // MOVE
    // =========================================================

    private fun performPreviewMove(
        photo: ImageModel,
        destinationAlbumName: String
    ) {

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                val copyResult =
                    repository.copyImages(
                        requireContext(),
                        listOf(photo.uri),
                        destinationAlbumName
                    )


                if (!copyResult.isSuccess) {

                    Toast.makeText(
                        requireContext(),
                        "Move failed: ${copyResult.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }


                val deleteResult =
                    repository.deleteImages(
                        requireContext(),
                        listOf(photo.uri)
                    )

                handlePreviewDeleteResult(
                    deleteResult,
                    "Moved to $destinationAlbumName"
                )
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

                addView(editText)
            }


        val dialog =
            AlertDialog.Builder(
                requireContext()
            )
                .setTitle("Rename")
                .setMessage(
                    "Enter a name for this photo."
                )
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

                refreshCurrentPhotoName(
                    photo,
                    newName
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

                pendingRenamePhoto =
                    photo

                pendingRenameName =
                    newName

                renamePermissionLauncher.launch(
                    IntentSenderRequest.Builder(
                        result.intentSender
                    ).build()
                )
            }
        }
    }

    private fun performRename(
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

                refreshCurrentPhotoName(
                    photo,
                    newName
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

                renamePermissionLauncher.launch(
                    IntentSenderRequest.Builder(
                        result.intentSender
                    ).build()
                )
            }
        }
    }



    private fun refreshCurrentPhotoName(
        photo: ImageModel,
        newName: String
    ) {

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

        val index =
            imageList.indexOfFirst {
                it.id == photo.id
            }

        if (index >= 0) {

            imageList[index] =
                imageList[index].copy(
                    name = finalName
                )
        }

        updatePhotoInfo(
            clickedPosition
        )
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
                "No app available to open this image",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // EDIT
    // =========================================================

    private fun editPhoto(
        photo: ImageModel
    ) {

        val intent =
            Intent(
                Intent.ACTION_EDIT
            ).apply {

                setDataAndType(
                    photo.uri,
                    "image/*"
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }


        try {

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
                "Set On Home Screen",
                "Set On Lock Screen",
                "Set On Both Screen"
            )


        var selectedOption =
            0


        AlertDialog.Builder(
            requireContext()
        )
            .setTitle(
                "Set Wallpaper"
            )
            .setSingleChoiceItems(
                options,
                selectedOption
            ) { _, which ->

                selectedOption =
                    which
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



    // =========================================================
    // DELETE CONFIRMATION
    // =========================================================

    private fun showDeleteConfirmation(
        photo: ImageModel
    ) {

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle(
                "Delete photo?"
            )
            .setMessage(
                "Are you sure you want to delete \"${photo.name}\"?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deletePreviewPhoto(
                    photo
                )
            }
            .show()
    }


    // =========================================================
    // DELETE
    // =========================================================

    private fun deletePreviewPhoto(
        photo: ImageModel
    ) {

        viewLifecycleOwner.lifecycleScope.launch {

            val result =
                repository.deleteImages(
                    requireContext(),
                    listOf(photo.uri)
                )

            handlePreviewDeleteResult(
                result,
                "Deleted"
            )
        }
    }


    private fun handlePreviewDeleteResult(
        result: DeleteResult,
        successMessage: String
    ) {

        when (result) {

            // -------------------------------------------------
            // DELETE SUCCESS
            // -------------------------------------------------

            is DeleteResult.Success -> {

                onPreviewDeleteFinished(
                    successMessage
                )
            }


            // -------------------------------------------------
            // USER CONFIRMATION REQUIRED
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
            // PERMISSION REQUIRED FOR REMAINING FILES
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

                Toast.makeText(
                    requireContext(),
                    "Delete failed: ${result.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // =========================================================
    // DELETE FINISHED
    // =========================================================

    private fun onPreviewDeleteFinished(
        successMessage: String
    ) {

        Toast.makeText(
            requireContext(),
            successMessage,
            Toast.LENGTH_SHORT
        ).show()


        // Remove deleted photo from preview list.
        imageList.removeAt(
            clickedPosition
        )


        // No photos remain.
        if (imageList.isEmpty()) {

            parentFragmentManager
                .popBackStack()

            return
        }


        // If the last photo was deleted,
        // move position to the new last item.
        if (
            clickedPosition >=
            imageList.size
        ) {

            clickedPosition =
                imageList.lastIndex
        }


        // Re-create adapter with updated list.
        binding.viewPagerPhotos.adapter =
            PhotoPreviewAdapter(
                imageList
            )


        binding.viewPagerPhotos.setCurrentItem(
            clickedPosition,
            false
        )


        updatePhotoInfo(
            clickedPosition
        )
    }


    // =========================================================
    // DETAILS
    // =========================================================

    private fun showPhotoDetails(
        photo: ImageModel
    ) {

        val size =
            Formatter.formatShortFileSize(
                requireContext(),
                photo.size
            )


        val details =
            """
            Name: ${photo.name}
            
            Size: $size
            
            Date: ${DateUtils.formatDate(photo.dateAdded)}
            
            Time: ${DateUtils.formatTime(photo.dateAdded)}
            
            Folder: ${photo.folderName}
            
            URI: ${photo.uri}
            """.trimIndent()


        AlertDialog.Builder(
            requireContext()
        )
            .setTitle(
                "Details"
            )
            .setMessage(
                details
            )
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

// Favourite


    private fun updateFavoriteIcon(
        photo: ImageModel
    ) {

        val isFavorite =
            FavoritePreferences.isFavorite(
                requireContext(),
                photo.id
            )

        binding.btnFavorite.setImageResource(
            if (isFavorite) {
                R.drawable.ic_red_heart
            } else {
                R.drawable.ic_heart
            }
        )
    }

    // PHOTO INFO
    // =========================================================

    private fun updatePhotoInfo(
        position: Int
    ) {

        if (
            position < 0 ||
            position >= imageList.size
        ) {
            return
        }

        val currentImage =
            imageList[position]

        binding.tvDate.text =
            DateUtils.formatDate(
                currentImage.dateAdded
            )

        binding.tvTime.text =
            DateUtils.formatTime(
                currentImage.dateAdded
            )

        updateFavoriteIcon(currentImage)
    }


    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    override fun onResume() {

        super.onResume()

        (activity as? MainActivity)
            ?.hideBottomNavigation()
    }


    override fun onDestroyView() {

        (activity as? MainActivity)
            ?.showBottomNavigation()

        super.onDestroyView()
    }


    // =========================================================
    // ARGUMENTS
    // =========================================================

    companion object {

        const val ARG_IMAGE_LIST =
            "image_list"

        const val ARG_POSITION =
            "position"
    }
}