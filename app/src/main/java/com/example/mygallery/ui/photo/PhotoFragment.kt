package com.example.mygallery.ui.photo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mygallery.R
import com.example.mygallery.adapter.PhotosAdapter
import com.example.mygallery.databinding.FragmentPhotoBinding
import com.example.mygallery.model.ImageModel
import com.example.mygallery.ui.photo.menu.PhotoAction
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.photo.menu.ColumnBottomSheet
import com.example.mygallery.ui.photo.menu.LayoutStyleBottomSheet
import com.example.mygallery.ui.photo.menu.PhotoActionPopup
import com.example.mygallery.ui.photo.menu.SortBottomSheet
import com.example.mygallery.ui.photo.menu.SortOrder
import com.example.mygallery.ui.photo.menu.SortType
import com.example.mygallery.ui.state.PhotosUiState
import com.example.mygallery.viewmodel.PhotosViewModel
import com.example.mygallery.viewmodel.PhotosViewModelFactory
import com.example.mygallery.ui.photo.selection.PhotoSelectionAction
import com.example.mygallery.ui.photo.selection.PhotoSelectionActionPopup


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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        folderName = arguments?.getString(ARG_FOLDER_NAME)

        val repository = GalleryRepository()
        val factory = PhotosViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[PhotosViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.btnExitSelection.setOnClickListener {
            viewModel.exitSelectionMode()
        }

        // Title: the album name we were opened with.
        binding.tvTitle.text = folderName ?: "Photos"

        binding.recyclerPhotos.layoutManager = buildGridLayoutManager()


        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }


        binding.btnMenu.setOnClickListener {

            PhotoActionPopup.show(
                requireContext(),
                binding.btnMenu
            ) { action ->

                when (action) {

                    PhotoAction.SELECT -> {

                    }

                    PhotoAction.PIN -> {

                    }

                    PhotoAction.SORT -> {

                        val sheet = SortBottomSheet(
                            currentSortType,
                            currentSortOrder
                        )

                        sheet.setListener(object : SortBottomSheet.OnSortSelected {

                            override fun onSortSelected(
                                sortType: SortType,
                                sortOrder: SortOrder
                            ) {

                                currentSortType = sortType
                                currentSortOrder = sortOrder

                                viewModel.loadPhotos(
                                    requireContext(),
                                    folderName,
                                    currentSortType,
                                    currentSortOrder
                                )

                            }

                        })

                        sheet.show(parentFragmentManager, "sort")
                    }

                    PhotoAction.FILTER -> {

                    }

                    PhotoAction.LAYOUT_STYLE -> {

                        val sheet = LayoutStyleBottomSheet(isGridView)

                        sheet.setListener(object : LayoutStyleBottomSheet.OnLayoutStyleSelected {

                            override fun onLayoutSelected(isGrid: Boolean) {

                                applyLayoutStyle(isGrid)

                            }
                        })

                        sheet.show(parentFragmentManager, "layout_style")
                    }

                    PhotoAction.COLUMN -> {

                        val sheet = ColumnBottomSheet(gridSpanCount)

                        sheet.setListener(object : ColumnBottomSheet.OnColumnSelected {

                            override fun onColumnSelected(column: Int) {

                                applyColumn(column)

                            }

                        })

                        sheet.show(parentFragmentManager, "column")

                    }

                    PhotoAction.SLIDE_SHOW -> {

                    }

                }

            }

        }


    // For Selection Menu

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
                        // Implement later
                    }

                    PhotoSelectionAction.MOVE -> {
                        // Implement later
                    }

                    PhotoSelectionAction.RENAME -> {
                        // Implement later
                    }

                    PhotoSelectionAction.FAVORITE -> {
                        // Implement later
                    }

                    PhotoSelectionAction.OPEN_WITH -> {
                        // Implement later
                    }

                    PhotoSelectionAction.SLIDE_SHOW -> {
                        // Implement later
                    }

                    PhotoSelectionAction.EDIT_WITH -> {
                        // Implement later
                    }

                    PhotoSelectionAction.SET_AS_WALLPAPER -> {
                        // Implement later
                    }

                    PhotoSelectionAction.SHARE -> {
                        // Implement later
                    }

                    PhotoSelectionAction.DELETE -> {
                        // Implement later
                    }

                    PhotoSelectionAction.DETAILS -> {
                        // Implement later
                    }
                }
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        viewModel.isSelectionMode.observe(viewLifecycleOwner) { isSelecting ->

            if (::photosAdapter.isInitialized) {
                photosAdapter.setSelectionState(
                    isSelecting,
                    viewModel.selectedPhotoIds.value ?: emptySet()
                )
            }

            updateSelectionMode(isSelecting)
        }


        viewModel.selectedPhotoIds.observe(viewLifecycleOwner) { selectedIds ->

            if (::photosAdapter.isInitialized) {
                photosAdapter.setSelectionState(
                    viewModel.isSelectionMode.value == true,
                    selectedIds
                )
            }

            updateSelectionCount(selectedIds)
        }

        viewModel.loadPhotos(
            requireContext(),
            folderName,
            currentSortType,
            currentSortOrder
        )

        binding.btnGridView.setOnClickListener {
            applyLayoutStyle(true)
        }

        binding.btnListView.setOnClickListener {
            applyLayoutStyle(false)
        }
    }

    private fun buildGridLayoutManager(): GridLayoutManager {
        val layoutManager = GridLayoutManager(requireContext(), gridSpanCount)

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (::photosAdapter.isInitialized) {
                    photosAdapter.getSpanSize(position, gridSpanCount)
                } else {
                    1
                }
            }
        }

        return layoutManager
    }


    private fun applyLayoutStyle(isGrid: Boolean) {

        isGridView = isGrid

        if (::photosAdapter.isInitialized) {

            photosAdapter.setViewMode(isGrid)

            binding.recyclerPhotos.layoutManager =
                if (isGrid) {
                    buildGridLayoutManager()
                } else {
                    LinearLayoutManager(requireContext())
                }
        }
    }


    private fun applyColumn(column: Int) {

        gridSpanCount = column

        if (isGridView) {

            binding.recyclerPhotos.layoutManager =
                buildGridLayoutManager()

        }

    }


    private fun updateSelectionMode(isSelecting: Boolean) {

        if (isSelecting) {

            // Hide normal header
            binding.btnBack.visibility = View.GONE
            binding.tvTitle.visibility = View.GONE
            binding.tvSubtitle.visibility = View.GONE
            binding.btnMenu.visibility = View.GONE
            binding.viewToggleGroup.visibility = View.GONE

            // Show selection header
            binding.selectionHeader.visibility = View.VISIBLE
            binding.selectionFilters.visibility = View.VISIBLE

        } else {

            // Show normal header
            binding.btnBack.visibility = View.VISIBLE
            binding.tvTitle.visibility = View.VISIBLE
            binding.tvSubtitle.visibility = View.VISIBLE
            binding.btnMenu.visibility = View.VISIBLE
            binding.viewToggleGroup.visibility = View.VISIBLE

            // Hide selection header
            binding.selectionHeader.visibility = View.GONE
            binding.selectionFilters.visibility = View.GONE
        }
    }

    private fun updateSelectionCount(selectedIds: Set<Long>) {

        val selectedCount = selectedIds.size
        val totalPhotos = photoList.size

        binding.tvSelectedCount.text =
            "$selectedCount / $totalPhotos"

        val selectedSize = photoList
            .filter { it.id in selectedIds }
            .sumOf { it.size }

        binding.tvSelectedSize.text =
            android.text.format.Formatter.formatShortFileSize(
                requireContext(),
                selectedSize
            )
    }

    private fun renderState(state: PhotosUiState) {

        binding.progressBar.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.GONE
        binding.recyclerPhotos.visibility = View.GONE

        when (state) {

            is PhotosUiState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
            }

            is PhotosUiState.Empty -> {
                binding.layoutEmptyState.visibility = View.VISIBLE
            }

            is PhotosUiState.Success -> {
                binding.recyclerPhotos.visibility = View.VISIBLE


                // Photo count for the subtitle: only count actual photos,
                // not the date header rows mixed into the same list.
                val photoCount =
                    state.items.count { it is com.example.mygallery.model.PhotoListItem.Photo }
                binding.tvSubtitle.text = "$photoCount Photos"


                photoList.clear()

                state.items.forEach { item ->

                    if (item is com.example.mygallery.model.PhotoListItem.Photo) {

                        photoList.add(item.image)

                    }

                }



                photosAdapter = PhotosAdapter(
                    isGridView = isGridView,
                    items = state.items,

                    onPhotoClick = { photo, position ->

                        val previewPhoto = PhotoPreviewFragment()

                        previewPhoto.arguments = Bundle().apply {

                            putParcelableArrayList(
                                PhotoPreviewFragment.ARG_IMAGE_LIST,
                                photoList
                            )

                            putInt(
                                PhotoPreviewFragment.ARG_POSITION,
                                position
                            )
                        }

                        parentFragmentManager.beginTransaction()
                            .replace(
                                R.id.frameContainer,
                                previewPhoto
                            )
                            .addToBackStack(null)
                            .commit()
                    },

                    onPhotoLongClick = { photo ->

                        viewModel.enterSelectionMode(
                            photo.image
                        )
                    },

                    onPhotoToggleSelect = { photo ->

                        viewModel.toggleSelection(
                            photo.image
                        )
                    }
                )






                binding.recyclerPhotos.adapter = photosAdapter

                // Re-attach the layout manager now that the adapter (and
                // therefore getSpanSize) actually exists.
                binding.recyclerPhotos.layoutManager = buildGridLayoutManager()
                if (!isGridView) {
                    binding.recyclerPhotos.layoutManager = LinearLayoutManager(requireContext())
                }
            }
        }
    }
}