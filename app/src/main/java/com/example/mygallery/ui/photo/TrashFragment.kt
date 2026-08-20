package com.example.mygallery.ui.photo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mygallery.adapter.TrashAdapter
import com.example.mygallery.databinding.FragmentTrashBinding
import com.example.mygallery.repository.GalleryRepository
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


        binding.normalHeader.visibility =
            View.GONE

        binding.selectionHeader.visibility =
            View.VISIBLE

        binding.trashActionBar.visibility =
            View.VISIBLE


        // Start with ZERO selected.
        trashAdapter.startSelectionMode()


        updateSelectionInfo()
    }


    // ---------------------------------------------------------
    // Exit Selection Mode
    // ---------------------------------------------------------

    private fun exitTrashSelection() {

        trashAdapter.exitSelectionMode()


        binding.selectionHeader.visibility =
            View.GONE

        binding.trashActionBar.visibility =
            View.GONE

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

        super.onDestroyView()

        _binding = null
    }
}