package com.example.mygallery.ui.album

import android.Manifest
import com.example.mygallery.R
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mygallery.adapter.GalleryAdapter
import com.example.mygallery.databinding.DialogCreateAlbumBinding
import com.example.mygallery.databinding.FragmentAlbumBinding
import com.example.mygallery.model.GalleryFolder
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.photo.PhotoFragment
import com.example.mygallery.ui.state.GalleryUiState
import com.example.mygallery.utils.PinPreferences
import com.example.mygallery.viewmodel.GalleryViewModel
import com.example.mygallery.viewmodel.GalleryViewModelFactory
import kotlinx.coroutines.launch

class AlbumFragment : Fragment() {

    private lateinit var binding: FragmentAlbumBinding
    private lateinit var viewModel: GalleryViewModel
    private lateinit var galleryAdapter: GalleryAdapter


    // Current Album sort preference — defaults match "no explicit sort
    // applied yet" (alphabetical, ascending).
    private var currentAlbumSortType = AlbumSortType.NAME
    private var currentAlbumSortOrder = com.example.mygallery.ui.photo.menu.SortOrder.ASCENDING


    private var isGridView = true

    // Holds the remaining URIs still needing deletion when we're on the
    // Android 10 (Q) "grant permission, then retry" path. Null means
    // we're either not mid-delete, or we're on the Android 11+ path
    // where Android deletes everything itself after confirmation.
    private var pendingDeleteRetryUris: List<android.net.Uri>? = null

    // The toast shown once deletion finishes. Delete uses "Deleted";
    // Move reuses this same delete machinery internally (Move = Copy +
    // Delete) but should say "Moved to X" instead — this field lets
    // both paths share one flow while showing the right message.
    private var pendingDeleteSuccessMessage: String = "Deleted"

    // Launches the system's delete/permission confirmation dialog and
    // reports back whether the user allowed or denied it.
    private val deleteIntentSenderLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->

        if (activityResult.resultCode == android.app.Activity.RESULT_OK) {

            val retryUris = pendingDeleteRetryUris
            pendingDeleteRetryUris = null

            if (retryUris != null) {
                viewModel.deleteImages(requireContext(), retryUris) { result ->
                    handleDeleteResult(result, pendingDeleteSuccessMessage)
                }
            } else {
                onDeleteFinished(pendingDeleteSuccessMessage)
            }

        } else {
            pendingDeleteRetryUris = null
            Toast.makeText(requireContext(), "Delete cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            Log.d("PERMISSION", "isGranted = $isGranted")

            if (isGranted) {
                viewModel.loadFolders(requireContext())
            } else {
                viewModel.onPermissionDenied()
                Toast.makeText(
                    requireContext(),
                    "Permission Denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = GalleryRepository()
        val factory = GalleryViewModelFactory(repository)

        viewModel = ViewModelProvider(this, factory)[GalleryViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentAlbumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabScrollTop.hide()

        // Default RecyclerView Layout
        updateLayoutManager()

        // for Searching
        viewModel.filteredAlbums.observe(viewLifecycleOwner) {
            if (::galleryAdapter.isInitialized) {
                galleryAdapter.updateList(it)
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        // Selection mode: both LiveData feed the same render function,
        // since the toolbar text and adapter checkboxes both depend on
        // BOTH "are we in selection mode" AND "which folders are selected".
        viewModel.isSelectionMode.observe(viewLifecycleOwner) {
            renderSelectionState()
        }
        viewModel.selectedFolderNames.observe(viewLifecycleOwner) {
            renderSelectionState()
        }

        // Selection toolbar: close button exits selection mode entirely.
        binding.toolbarSelection.btnClose.setOnClickListener {
            viewModel.exitSelectionMode()
        }

        // Selection toolbar: overflow shows the same 6-action popup as
        // before, but now it acts on ALL currently selected folders.
        binding.toolbarSelection.btnOverflow.setOnClickListener {
            val selectedFolders = viewModel.getSelectedFolders()

            if (selectedFolders.isEmpty()) return@setOnClickListener

            AlbumActionPopup.show(
                requireContext(),
                binding.toolbarSelection.btnOverflow
            ) { action ->
                handleAlbumAction(action, selectedFolders)
            }
        }

        // Request permission and load albums
        checkPermission()

        binding.btnGrantAccess.setOnClickListener {
            checkPermission()
        }

        // Pin banner: dismiss permanently, hide immediately.
        // This listener was previously missing entirely — that's why
        // tapping the X did nothing.
        binding.btnDismissBanner.setOnClickListener {
            PinPreferences.dismissBanner(requireContext())
            binding.pinnedBanner.visibility = View.GONE
        }



        binding.btnSort.setOnClickListener {

            val sheet = AlbumSortBottomSheet(currentAlbumSortType, currentAlbumSortOrder)

            sheet.setListener(object : AlbumSortBottomSheet.OnAlbumSortSelected {
                override fun onAlbumSortSelected(
                    sortType: AlbumSortType,
                    sortOrder: com.example.mygallery.ui.photo.menu.SortOrder
                ) {
                    currentAlbumSortType = sortType
                    currentAlbumSortOrder = sortOrder
                    viewModel.applySort(requireContext(), sortType, sortOrder)
                }
            })

            sheet.show(childFragmentManager, "AlbumSort")
        }


            // Grid Button
        binding.btnGridView.setOnClickListener {

            isGridView = true

            binding.recyclerAlbums.layoutManager =
                GridLayoutManager(requireContext(), 3)

            if (::galleryAdapter.isInitialized) {
                galleryAdapter.setViewMode(true)
            }
        }

        // List Button
        binding.btnListView.setOnClickListener {

            isGridView = false

            binding.recyclerAlbums.layoutManager =
                LinearLayoutManager(requireContext())

            if (::galleryAdapter.isInitialized) {
                galleryAdapter.setViewMode(false)
            }
        }

        // Search Faeture
        binding.searchAlbums.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                viewModel.searchAlbum(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })





        binding.btnAdd.setOnClickListener {

            val dialogBinding = DialogCreateAlbumBinding.inflate(layoutInflater)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogBinding.root)
                .create()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            dialog.show()

            dialogBinding.btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialogBinding.btnCreate.setOnClickListener {

                val albumName = dialogBinding.etAlbumName.text.toString().trim()

                val result = viewModel.createAlbum(requireContext(), albumName)

                if (result.isSuccess) {

                    Toast.makeText(
                        requireContext(),
                        result.getOrNull(),
                        Toast.LENGTH_SHORT
                    ).show()

                    viewModel.loadFolders(requireContext())

                    dialog.dismiss()

                } else {

                    dialogBinding.tilAlbumName.error =
                        result.exceptionOrNull()?.message
                }
            }
        }

        // For Top arrow Button
        binding.recyclerAlbums.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(
                recyclerView: RecyclerView,
                dx: Int,
                dy: Int
            ) {
                super.onScrolled(recyclerView, dx, dy)

                if (!::galleryAdapter.isInitialized) return

                if (recyclerView.canScrollVertically(-1)) {
                    binding.fabScrollTop.show()
                } else {
                    binding.fabScrollTop.hide()
                }
            }
        })

        binding.fabScrollTop.setOnClickListener {

            binding.recyclerAlbums.smoothScrollToPosition(0)

        }
    }

    private fun updateLayoutManager() {

        binding.recyclerAlbums.layoutManager =
            if (isGridView) {
                GridLayoutManager(requireContext(), 3)
            } else {
                LinearLayoutManager(requireContext())
            }
    }

    private fun renderState(state: GalleryUiState) {

        binding.progressBar.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.GONE
        binding.layoutPermissionDenied.visibility = View.GONE
        binding.recyclerAlbums.visibility = View.GONE
        binding.pinnedBanner.visibility = View.GONE

        when (state) {

            is GalleryUiState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
            }

            is GalleryUiState.Empty -> {
                binding.layoutEmptyState.visibility = View.VISIBLE
            }

            is GalleryUiState.PermissionDenied -> {
                binding.layoutPermissionDenied.visibility = View.VISIBLE
            }

            is GalleryUiState.Success -> {
                binding.recyclerAlbums.visibility = View.VISIBLE

                // Show the hint banner only when it's actually relevant
                // (at least one pinned album) and the user hasn't
                // already dismissed it permanently.
                val hasPinnedAlbum = state.folder.any { folder ->
                    PinPreferences.isPinned(requireContext(), folder.folderName)
                }
                binding.pinnedBanner.visibility =
                    if (hasPinnedAlbum && !PinPreferences.isBannerDismissed(requireContext())) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                galleryAdapter = GalleryAdapter(
                    isGridView,
                    state.folder.toMutableList(),
                    onFolderClick = { folder ->

                        val photoFragment = PhotoFragment()

                        photoFragment.arguments = Bundle().apply {
                            putString(PhotoFragment.ARG_FOLDER_NAME, folder.folderName)
                        }

                        parentFragmentManager.beginTransaction()
                            .replace(R.id.frameContainer, photoFragment)
                            .addToBackStack(null)
                            .commit()
                    },
                    onFolderLongClick = { folder ->
                        viewModel.enterSelectionMode(folder)
                    },
                    onFolderToggleSelect = { folder ->
                        viewModel.toggleSelection(folder)
                    }
                )
                binding.recyclerAlbums.adapter = galleryAdapter

                renderSelectionState()
            }
        }
    }

    private fun renderSelectionState() {

        val isSelectionMode = viewModel.isSelectionMode.value ?: false
        val selectedNames = viewModel.selectedFolderNames.value ?: emptySet()

        binding.groupNormalHeader.visibility =
            if (isSelectionMode) View.GONE else View.VISIBLE
        binding.toolbarSelection.root.visibility =
            if (isSelectionMode) View.VISIBLE else View.GONE

        if (::galleryAdapter.isInitialized) {
            galleryAdapter.setSelectionState(isSelectionMode, selectedNames)
        }

        if (isSelectionMode) {
            val selectedFolders = viewModel.getSelectedFolders()
            val totalCount = selectedFolders.size
            val totalSize = selectedFolders.sumOf { folder ->
                folder.imageList.sumOf { it.size }
            }
            val formattedSize = android.text.format.Formatter.formatShortFileSize(
                requireContext(),
                totalSize
            )
            binding.toolbarSelection.tvSelectionCount.text =
                "$totalCount Selected ($formattedSize)"
        }
    }

    private fun handleAlbumAction(action: AlbumAction, folders: List<GalleryFolder>) {
        when (action) {

            AlbumAction.PIN -> {
                folders.forEach { folder ->
                    PinPreferences.togglePin(requireContext(), folder.folderName)
                }
                viewModel.exitSelectionMode()
                viewModel.loadFolders(requireContext())
            }

            AlbumAction.SHARE -> {
                shareFolders(folders)
            }

            AlbumAction.DELETE -> {
                confirmAndDeleteFolders(folders)
            }

            AlbumAction.COPY -> {
                showAlbumPicker(AlbumPickerBottomSheet.Mode.COPY, folders) { destinationName ->
                    performCopy(folders, destinationName)
                }
            }

            AlbumAction.MOVE -> {
                showAlbumPicker(AlbumPickerBottomSheet.Mode.MOVE, folders) { destinationName ->
                    performMove(folders, destinationName)
                }
            }

            AlbumAction.DETAILS -> {
                showAlbumDetailsDialog(folders)
            }
        }
    }

    private fun showAlbumPicker(
        mode: AlbumPickerBottomSheet.Mode,
        sourceFolders: List<GalleryFolder>,
        onChosen: (destinationAlbumName: String) -> Unit
    ) {
        val excludedNames = sourceFolders.map { it.folderName }

        val sheet = AlbumPickerBottomSheet.newInstance(mode, excludedNames)
        sheet.onAlbumSelected = { destinationName ->
            onChosen(destinationName)
        }
        sheet.show(childFragmentManager, "AlbumPicker")
    }

    private fun performCopy(folders: List<GalleryFolder>, destinationAlbumName: String) {

        val uris = folders.flatMap { folder -> folder.imageList.map { it.uri } }

        if (uris.isEmpty()) {
            Toast.makeText(requireContext(), "No images to copy", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val result = viewModel.repository.copyImages(requireContext(), uris, destinationAlbumName)

            if (result.isSuccess) {
                Toast.makeText(
                    requireContext(),
                    "Copied ${result.getOrNull()} item(s) to $destinationAlbumName",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.exitSelectionMode()
                viewModel.loadFolders(requireContext())
            } else {
                Toast.makeText(
                    requireContext(),
                    "Copy failed: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Move = Copy to the destination, THEN delete the originals. This
     * reuses copyImages() (no special permission handling needed, since
     * we're only ever creating new files) AND our existing delete flow
     * (which already handles all 3 Android version cases correctly) —
     * so Move needs zero new low-level file-permission logic of its own.
     */
    private fun performMove(folders: List<GalleryFolder>, destinationAlbumName: String) {

        val uris = folders.flatMap { folder -> folder.imageList.map { it.uri } }

        if (uris.isEmpty()) {
            Toast.makeText(requireContext(), "No images to move", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val copyResult = viewModel.repository.copyImages(requireContext(), uris, destinationAlbumName)

            if (copyResult.isSuccess) {
                viewModel.deleteImages(requireContext(), uris) { result ->
                    handleDeleteResult(result, "Moved to $destinationAlbumName")
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

    private fun confirmAndDeleteFolders(folders: List<GalleryFolder>) {

        val allUris = folders.flatMap { folder -> folder.imageList.map { it.uri } }

        if (allUris.isEmpty()) {
            Toast.makeText(requireContext(), "No images to delete", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Delete ${allUris.size} item${if (allUris.size > 1) "s" else ""}?")
            .setMessage("This will permanently delete the selected items from your device. This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteImages(requireContext(), allUris) { result ->
                    handleDeleteResult(result, "Deleted")
                }
            }
            .show()
    }

    private fun handleDeleteResult(result: com.example.mygallery.model.DeleteResult, successMessage: String) {
        when (result) {

            is com.example.mygallery.model.DeleteResult.Success -> {
                onDeleteFinished(successMessage)
            }

            is com.example.mygallery.model.DeleteResult.ConfirmDelete -> {
                pendingDeleteRetryUris = null
                pendingDeleteSuccessMessage = successMessage
                deleteIntentSenderLauncher.launch(
                    androidx.activity.result.IntentSenderRequest.Builder(result.intentSender).build()
                )
            }

            is com.example.mygallery.model.DeleteResult.GrantPermissionThenRetry -> {
                pendingDeleteRetryUris = result.remainingUris
                pendingDeleteSuccessMessage = successMessage
                deleteIntentSenderLauncher.launch(
                    androidx.activity.result.IntentSenderRequest.Builder(result.intentSender).build()
                )
            }

            is com.example.mygallery.model.DeleteResult.Error -> {
                Toast.makeText(
                    requireContext(),
                    "Delete failed: ${result.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun onDeleteFinished(successMessage: String) {
        Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
        viewModel.exitSelectionMode()
        viewModel.loadFolders(requireContext())
    }

    private fun shareFolders(folders: List<GalleryFolder>) {

        val uris = ArrayList<android.net.Uri>()
        folders.forEach { folder ->
            folder.imageList.forEach { image ->
                uris.add(image.uri)
            }
        }

        if (uris.isEmpty()) {
            Toast.makeText(requireContext(), "No images to share", Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent =
            android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        startActivity(
            android.content.Intent.createChooser(shareIntent, "Share ${uris.size} images")
        )
    }

    private fun showAlbumDetailsDialog(folders: List<GalleryFolder>) {

        val totalItems = folders.sumOf { it.imageCount }
        val totalSizeBytes = folders.sumOf { folder -> folder.imageList.sumOf { it.size } }
        val formattedSize = android.text.format.Formatter.formatShortFileSize(
            requireContext(),
            totalSizeBytes
        )

        val namesList = folders.joinToString(", ") { it.folderName }

        val message = """
            Albums: $namesList
            Total items: $totalItems
            Total size: $formattedSize
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Album Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun checkPermission() {

        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        if (
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            viewModel.loadFolders(requireContext())

        } else {

            requestPermission.launch(permission)

        }
    }
}