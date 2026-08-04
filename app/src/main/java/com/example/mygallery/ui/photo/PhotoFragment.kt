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
import com.example.mygallery.ui.photo.PhotoActionPopup
import com.example.mygallery.ui.photo.PhotoPreviewFragment
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

    private val gridSpanCount = 3

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

                    }

                    PhotoAction.FILTER -> {

                    }

                    PhotoAction.LAYOUT_STYLE -> {

                    }

                    PhotoAction.COLUMN -> {

                    }

                    PhotoAction.SLIDE_SHOW -> {

                    }

                }

            }

        }




        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        viewModel.loadPhotos(requireContext(), folderName)

        binding.btnGridView.setOnClickListener {
            isGridView = true
            binding.recyclerPhotos.layoutManager = buildGridLayoutManager()
            if (::photosAdapter.isInitialized) {
                photosAdapter.setViewMode(true)
            }
        }

        binding.btnListView.setOnClickListener {
            isGridView = false
            binding.recyclerPhotos.layoutManager = LinearLayoutManager(requireContext())
            if (::photosAdapter.isInitialized) {
                photosAdapter.setViewMode(false)
            }
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