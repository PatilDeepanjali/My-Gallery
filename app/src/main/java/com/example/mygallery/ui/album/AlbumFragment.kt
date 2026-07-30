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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mygallery.adapter.GalleryAdapter
import com.example.mygallery.databinding.DialogCreateAlbumBinding
import com.example.mygallery.databinding.FragmentAlbumBinding
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.photos.PhotoFragment
import com.example.mygallery.ui.state.GalleryUiState
import com.example.mygallery.viewmodel.GalleryViewModel
import com.example.mygallery.viewmodel.GalleryViewModelFactory

class AlbumFragment : Fragment() {

    private lateinit var binding: FragmentAlbumBinding
    private lateinit var viewModel: GalleryViewModel
    private lateinit var galleryAdapter: GalleryAdapter

    private var isGridView = true

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
        binding.recyclerAlbums.layoutManager =
            GridLayoutManager(requireContext(), 3)


        // for Searching
        viewModel.filteredAlbums.observe(viewLifecycleOwner) {
            if (::galleryAdapter.isInitialized) {
                galleryAdapter.updateList(it)
            }
        }


        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        // Request permission and load albums
        checkPermission()

        binding.btnGrantAccess.setOnClickListener {
            checkPermission()
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



        // List Button
        binding.btnListView.setOnClickListener {

            isGridView = false

            binding.recyclerAlbums.layoutManager =
                LinearLayoutManager(requireContext())

            if (::galleryAdapter.isInitialized) {
                galleryAdapter.setViewMode(false)
            }
        }


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

    private fun renderState(state: GalleryUiState) {


        binding.progressBar.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.GONE
        binding.layoutPermissionDenied.visibility = View.GONE
        binding.recyclerAlbums.visibility = View.GONE

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

                galleryAdapter = GalleryAdapter(
                    isGridView,
                    state.folder.toMutableList()
                ) { folder ->

                    val photoFragment = PhotoFragment()

                    photoFragment.arguments = Bundle().apply {
                        putParcelableArrayList("images", folder.imageList)
                        putString("folderName", folder.folderName)
                    }

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.frameContainer, photoFragment)
                        .addToBackStack(null)
                        .commit()
                }
                binding.recyclerAlbums.adapter = galleryAdapter
            }
        }
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