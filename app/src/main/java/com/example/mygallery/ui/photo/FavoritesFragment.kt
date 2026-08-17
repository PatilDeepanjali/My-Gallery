package com.example.mygallery.ui.photo

import android.app.Activity
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mygallery.R
import com.example.mygallery.adapter.PhotosAdapter
import com.example.mygallery.databinding.FragmentFavoritesBinding
import com.example.mygallery.model.DeleteResult
import com.example.mygallery.model.ImageModel
import com.example.mygallery.model.PhotoListItem
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.MainActivity
import com.example.mygallery.ui.photo.menu.SortOrder
import com.example.mygallery.ui.photo.menu.SortType
import com.example.mygallery.ui.state.PhotosUiState
import com.example.mygallery.utils.FavoritePreferences
import com.example.mygallery.viewmodel.PhotosViewModel
import com.example.mygallery.viewmodel.PhotosViewModelFactory

/**
 * Dedicated "Favorites" screen (reached from the Menu sheet) — shows
 * every favorited photo across ALL albums, date-grouped, reusing the
 * same PhotosAdapter as the regular Photos screen (so the heart icon,
 * grid layout, and selection mechanics all behave identically).
 */
class FavoritesFragment : Fragment() {

    private lateinit var binding: FragmentFavoritesBinding
    private lateinit var viewModel: PhotosViewModel
    private lateinit var photosAdapter: PhotosAdapter

    private var photoList = ArrayList<ImageModel>()

    // Favorites screen doesn't expose its own sort UI right now —
    // fixed to newest-first. Easy to add a Sort button later reusing
    // the same SortBottomSheet Photos already has.
    private val currentSortType = SortType.DATE_TAKEN
    private val currentSortOrder = SortOrder.DESCENDING

    private var pendingDeleteRetryUris: List<Uri>? = null
    private var pendingDeleteSuccessMessage = "Deleted"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = GalleryRepository()
        val factory = PhotosViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[PhotosViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        binding.recyclerFavorites.layoutManager = buildGridLayoutManager()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnExitSelection.setOnClickListener {
            viewModel.exitSelectionMode()
        }

        binding.btnUnfavoriteSelected.setOnClickListener {
            unfavoriteSelected()
        }

        binding.btnDeleteSelected.setOnClickListener {
            confirmAndDeleteSelected()
        }

        viewModel.loadFavorites(requireContext(), currentSortType, currentSortOrder)
    }

    override fun onResume() {
        super.onResume()

        (activity as? MainActivity)?.showBottomNavigation()

        // Restore selection state after returning from Preview, same
        // pattern PhotoFragment uses.
        if (::photosAdapter.isInitialized) {
            photosAdapter.setSelectionState(
                viewModel.isSelectionMode.value == true,
                viewModel.selectedPhotoIds.value ?: emptySet()
            )
            updateSelectionMode(viewModel.isSelectionMode.value == true)
            updateSelectionCount(viewModel.selectedPhotoIds.value ?: emptySet())
        }
    }

    private fun setupObservers() {

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        viewModel.isSelectionMode.observe(viewLifecycleOwner) { isSelecting ->
            updateSelectionMode(isSelecting)
            if (::photosAdapter.isInitialized) {
                photosAdapter.setSelectionState(
                    isSelecting,
                    viewModel.selectedPhotoIds.value ?: emptySet()
                )
            }
        }

        viewModel.selectedPhotoIds.observe(viewLifecycleOwner) { selectedIds ->
            updateSelectionCount(selectedIds)
            if (::photosAdapter.isInitialized) {
                photosAdapter.setSelectionState(
                    viewModel.isSelectionMode.value == true,
                    selectedIds
                )
            }
        }
    }

    private fun buildGridLayoutManager(): GridLayoutManager {
        val layoutManager = GridLayoutManager(requireContext(), 4)

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (::photosAdapter.isInitialized) {
                    photosAdapter.getSpanSize(position, 4)
                } else {
                    1
                }
            }
        }

        return layoutManager
    }

    private fun renderState(state: PhotosUiState) {

        binding.progressBar.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.GONE
        binding.recyclerFavorites.visibility = View.GONE

        when (state) {

            is PhotosUiState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
            }

            is PhotosUiState.Empty -> {
                photoList.clear()
                binding.layoutEmptyState.visibility = View.VISIBLE
                updateSelectionCount(viewModel.selectedPhotoIds.value ?: emptySet())
            }

            is PhotosUiState.Success -> {
                binding.recyclerFavorites.visibility = View.VISIBLE

                photoList.clear()
                state.items.forEach { item ->
                    if (item is PhotoListItem.Photo) {
                        photoList.add(item.image)
                    }
                }

                binding.tvSubtitle.text = "${photoList.size} Photos"

                photosAdapter = PhotosAdapter(
                    isGridView = true,
                    items = state.items,
                    onPhotoClick = { _, position ->

                        val preview = PhotoPreviewFragment()
                        preview.arguments = Bundle().apply {
                            putParcelableArrayList(
                                PhotoPreviewFragment.ARG_IMAGE_LIST,
                                photoList
                            )
                            putInt(PhotoPreviewFragment.ARG_POSITION, position)
                        }

                        parentFragmentManager.beginTransaction()
                            .replace(R.id.frameContainer, preview)
                            .addToBackStack(null)
                            .commit()
                    },
                    onPhotoLongClick = { photo ->
                        viewModel.enterSelectionMode(photo.image)
                    },
                    onPhotoToggleSelect = { photo ->
                        viewModel.toggleSelection(photo.image)
                    }
                )

                photosAdapter.setSelectionState(
                    viewModel.isSelectionMode.value == true,
                    viewModel.selectedPhotoIds.value ?: emptySet()
                )

                binding.recyclerFavorites.adapter = photosAdapter
                binding.recyclerFavorites.layoutManager = buildGridLayoutManager()

                updateSelectionCount(viewModel.selectedPhotoIds.value ?: emptySet())
                updateSelectionMode(viewModel.isSelectionMode.value == true)
            }
        }
    }

    private fun updateSelectionMode(isSelecting: Boolean) {
        binding.groupNormalHeader.visibility = if (isSelecting) View.GONE else View.VISIBLE
        binding.selectionHeader.visibility = if (isSelecting) View.VISIBLE else View.GONE
    }

    private fun updateSelectionCount(selectedIds: Set<Long>) {
        binding.tvSelectedCount.text = "${selectedIds.size} Selected"
    }

    /**
     * Removes all selected photos from Favorites (they're all
     * currently favorite by definition, since this screen only shows
     * favorited photos — so toggling each one off just unfavorites it).
     */
    private fun unfavoriteSelected() {

        val selected = viewModel.getSelectedPhotos()

        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "No photos selected", Toast.LENGTH_SHORT).show()
            return
        }

        selected.forEach { photo ->
            FavoritePreferences.toggleFavorite(requireContext(), photo.id)
        }

        Toast.makeText(requireContext(), "Removed from Favorites", Toast.LENGTH_SHORT).show()

        viewModel.exitSelectionMode()
        viewModel.loadFavorites(requireContext(), currentSortType, currentSortOrder)
    }

    // =========================================================
    // DELETE — same version-aware flow used everywhere else
    // =========================================================

    private fun confirmAndDeleteSelected() {

        val selected = viewModel.getSelectedPhotos()

        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "No photos selected", Toast.LENGTH_SHORT).show()
            return
        }

        val uris = selected.map { it.uri }

        AlertDialog.Builder(requireContext())
            .setTitle("Delete ${uris.size} item${if (uris.size > 1) "s" else ""}?")
            .setMessage("This will permanently delete the selected items from your device. This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteImages(requireContext(), uris) { result ->
                    handleDeleteResult(result, "Deleted")
                }
            }
            .show()
    }

    private val deleteIntentSenderLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->

        if (activityResult.resultCode == Activity.RESULT_OK) {

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

    private fun handleDeleteResult(result: DeleteResult, successMessage: String) {
        when (result) {

            is DeleteResult.Success -> {
                onDeleteFinished(successMessage)
            }

            is DeleteResult.ConfirmDelete -> {
                pendingDeleteRetryUris = null
                pendingDeleteSuccessMessage = successMessage
                deleteIntentSenderLauncher.launch(
                    IntentSenderRequest.Builder(result.intentSender).build()
                )
            }

            is DeleteResult.GrantPermissionThenRetry -> {
                pendingDeleteRetryUris = result.remainingUris
                pendingDeleteSuccessMessage = successMessage
                deleteIntentSenderLauncher.launch(
                    IntentSenderRequest.Builder(result.intentSender).build()
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

    private fun onDeleteFinished(successMessage: String) {
        Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
        viewModel.exitSelectionMode()
        viewModel.loadFavorites(requireContext(), currentSortType, currentSortOrder)
    }
}