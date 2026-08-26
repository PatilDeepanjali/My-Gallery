package com.example.mygallery.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mygallery.adapter.TrashAdapter
import com.example.mygallery.databinding.FragmentTrashBinding
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.MainActivity
import com.example.mygallery.viewmodel.TrashViewModel
import com.example.mygallery.viewmodel.TrashViewModelFactory

class TrashFragment : Fragment() {

    private var _binding: FragmentTrashBinding? = null
    private val binding get() = _binding!!

    private lateinit var trashAdapter: TrashAdapter

    private val repository = GalleryRepository()

    private val viewModel: TrashViewModel by lazy {

        ViewModelProvider(
            this,
            TrashViewModelFactory(repository)
        )[TrashViewModel::class.java]
    }


    // ---------------------------------------------------------
    // Back button callback
    // ---------------------------------------------------------

    private val backPressedCallback =
        object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {

                if (trashAdapter.isSelectionMode) {

                    // First Back → exit selection mode
                    exitTrashSelection()

                } else {

                    // Normal Trash screen → go back
                    parentFragmentManager.popBackStack()
                }
            }
        }


    // ---------------------------------------------------------
    // Create View
    // ---------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentTrashBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }


    // ---------------------------------------------------------
    // View Created
    // ---------------------------------------------------------

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )


        // -----------------------------------------------------
        // System Back Button
        // -----------------------------------------------------

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(
                viewLifecycleOwner,
                backPressedCallback
            )


        // -----------------------------------------------------
        // Adapter
        // -----------------------------------------------------

        trashAdapter =
            TrashAdapter(
                emptyList()
            ) { photo ->

                if (trashAdapter.isSelectionMode) {

                    trashAdapter.toggleSelection(
                        photo.id
                    )

                    updateSelectionInfo()
                }
            }


        // -----------------------------------------------------
        // RecyclerView
        // -----------------------------------------------------

        binding.recyclerTrash.apply {

            layoutManager =
                GridLayoutManager(
                    requireContext(),
                    4
                )

            adapter = trashAdapter
        }


        // -----------------------------------------------------
        // Back Arrow
        // -----------------------------------------------------

        binding.btnBack.setOnClickListener {

            if (trashAdapter.isSelectionMode) {

                exitTrashSelection()

            } else {

                parentFragmentManager.popBackStack()
            }
        }


        // -----------------------------------------------------
        // Normal Menu
        // -----------------------------------------------------

        binding.btnMenu.setOnClickListener {

            showTrashMenu()
        }


        // -----------------------------------------------------
        // Exit Selection
        // -----------------------------------------------------

        binding.btnExitSelection.setOnClickListener {

            exitTrashSelection()
        }


        binding.btnRestore.setOnClickListener {

            restoreSelectedPhotos()
        }


        binding.btnDelete.setOnClickListener {

            permanentlyDeleteSelectedPhotos()
        }


        // -----------------------------------------------------
        // Observe Trash
        // -----------------------------------------------------

        viewModel.trashedPhotos.observe(
            viewLifecycleOwner
        ) { photos ->

            binding.progressBar.visibility =
                View.GONE


            if (photos.isEmpty()) {

                binding.recyclerTrash.visibility =
                    View.GONE

                binding.layoutEmptyState.visibility =
                    View.VISIBLE

                binding.tvSubtitle.text =
                    "0 Photos • 0 B"

            } else {

                binding.layoutEmptyState.visibility =
                    View.GONE

                binding.recyclerTrash.visibility =
                    View.VISIBLE

                trashAdapter.submitList(
                    photos
                )

                val totalSize =
                    photos.sumOf {
                        it.size
                    }

                binding.tvSubtitle.text =
                    "${photos.size} Photos • ${
                        formatSize(totalSize)
                    }"
            }
        }


        // -----------------------------------------------------
        // Load Trash
        // -----------------------------------------------------

        viewModel.loadTrash(
            requireContext()
        )
    }


    // ---------------------------------------------------------
    // Trash Menu
    // ---------------------------------------------------------

    private fun showTrashMenu() {

        val popup =
            PopupMenu(
                requireContext(),
                binding.btnMenu
            )

        popup.menu.add(
            "Select Images"
        )

        popup.setOnMenuItemClickListener { item ->

            when (item.title.toString()) {

                "Select Images" -> {

                    enterTrashSelection()

                    true
                }

                else -> false
            }
        }

        popup.show()
    }


    // ---------------------------------------------------------
    // Enter Selection Mode
    // ---------------------------------------------------------

    private fun enterTrashSelection() {

        if (trashAdapter.itemCount == 0) {
            return
        }


        // Hide MainActivity bottom navigation
        // to match the Figma selection screen.
        (activity as? MainActivity)
            ?.hideBottomNavigation()


        // Hide normal header
        binding.normalHeader.visibility =
            View.GONE


        // Show selection header
        binding.selectionHeader.visibility =
            View.VISIBLE


        // Show Restore/Delete bar
        binding.trashActionBar.visibility =
            View.VISIBLE


        // Start with ZERO selected
        trashAdapter.startSelectionMode()


        updateSelectionInfo()
    }






    // ---------------------------------------------------------
// Restore Selected Photos
// ---------------------------------------------------------

    private fun restoreSelectedPhotos() {

        val selectedPhotos =
            trashAdapter.getSelectedPhotos()

        if (selectedPhotos.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "Select at least one photo",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        viewModel.restoreImages(
            requireContext(),
            selectedPhotos
        ) { restoredCount ->

            if (restoredCount == selectedPhotos.size) {

                Toast.makeText(
                    requireContext(),
                    "Restored $restoredCount item(s)",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    requireContext(),
                    "Restored $restoredCount of ${selectedPhotos.size} item(s)",
                    Toast.LENGTH_LONG
                ).show()
            }

            exitTrashSelection()
        }
    }


    // ---------------------------------------------------------
// Permanently Delete Selected Photos
// ---------------------------------------------------------

    private fun permanentlyDeleteSelectedPhotos() {

        val selectedPhotos =
            trashAdapter.getSelectedPhotos()

        if (selectedPhotos.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "Select at least one photo",
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
                "Delete permanently?"
            )
            .setMessage(
                "These $count item${if (count > 1) "s" else ""} will be permanently deleted and cannot be restored."
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                viewModel.permanentlyDeleteImages(
                    requireContext(),
                    selectedPhotos
                ) { deletedCount ->

                    if (deletedCount == count) {

                        Toast.makeText(
                            requireContext(),
                            "Deleted $deletedCount item(s) permanently",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            requireContext(),
                            "Deleted $deletedCount of $count item(s)",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    exitTrashSelection()
                }
            }
            .show()
    }

    // ---------------------------------------------------------
    // Exit Selection Mode
    // ---------------------------------------------------------

    private fun exitTrashSelection() {

        trashAdapter.exitSelectionMode()


        // Show MainActivity bottom navigation again
        (activity as? MainActivity)
            ?.showBottomNavigation()


        // Hide selection header
        binding.selectionHeader.visibility =
            View.GONE


        // Hide action bar
        binding.trashActionBar.visibility =
            View.GONE


        // Show normal header
        binding.normalHeader.visibility =
            View.VISIBLE
    }


    // ---------------------------------------------------------
    // Selection Information
    // ---------------------------------------------------------

    private fun updateSelectionInfo() {

        val selectedCount =
            trashAdapter.getSelectedCount()

        val totalCount =
            trashAdapter.itemCount


        binding.tvSelectedCount.text =
            "$selectedCount / $totalCount"


        val selectedSize =
            trashAdapter
                .getSelectedPhotos()
                .sumOf {
                    it.size
                }


        binding.tvSelectedSize.text =
            formatSize(
                selectedSize
            )
    }


    // ---------------------------------------------------------
    // Format Size
    // ---------------------------------------------------------

    private fun formatSize(
        bytes: Long
    ): String {

        return when {

            bytes >=
                    1024L * 1024L * 1024L ->

                String.format(
                    "%.1f GB",
                    bytes /
                            (1024.0 *
                                    1024.0 *
                                    1024.0)
                )


            bytes >=
                    1024L * 1024L ->

                String.format(
                    "%.1f MB",
                    bytes /
                            (1024.0 *
                                    1024.0)
                )


            bytes >=
                    1024L ->

                String.format(
                    "%.1f KB",
                    bytes / 1024.0
                )


            else ->
                "$bytes B"
        }
    }


    // ---------------------------------------------------------
    // Destroy View
    // ---------------------------------------------------------

    override fun onDestroyView() {

        // Safety:
        // If Fragment leaves while selection mode is active,
        // make sure MainActivity bottom navigation comes back.

        (activity as? MainActivity)
            ?.showBottomNavigation()

        super.onDestroyView()

        _binding = null
    }
}