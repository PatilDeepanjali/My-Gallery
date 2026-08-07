package com.example.mygallery.ui.photos

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
import com.example.mygallery.ui.photo.PhotoAction
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.photo.ColumnBottomSheet
import com.example.mygallery.ui.photo.LayoutStyleBottomSheet
import com.example.mygallery.ui.photo.PhotoActionPopup
import com.example.mygallery.ui.photo.PhotoPreviewFragment
import com.example.mygallery.ui.photo.SortBottomSheet
import com.example.mygallery.ui.photo.SortOrder
import com.example.mygallery.ui.photo.SortType
import com.example.mygallery.ui.state.PhotosUiState
import com.example.mygallery.viewmodel.PhotosViewModel
import com.example.mygallery.viewmodel.PhotosViewModelFactory




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

    private var gridSpanCount = 3

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




        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            renderState(state)
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
                val photoCount = state.items.count { it is com.example.mygallery.model.PhotoListItem.Photo }
                binding.tvSubtitle.text = "$photoCount Photos"


                photoList.clear()

                state.items.forEach { item ->

                    if (item is com.example.mygallery.model.PhotoListItem.Photo) {

                        photoList.add(item.image)

                    }

                }

                photosAdapter = PhotosAdapter(
                    isGridView,
                    state.items
                ) { photo, position ->


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
                        .replace(R.id.frameContainer,
                        previewPhoto)
                        .addToBackStack(null)
                        .commit()
                    // Image Preview screen - built in a later step.
                }
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