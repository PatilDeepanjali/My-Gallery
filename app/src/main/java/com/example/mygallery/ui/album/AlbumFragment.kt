package com.example.mygallery.ui.album

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.example.mygallery.adapter.GalleryAdapter
import com.example.mygallery.databinding.FragmentAlbumBinding
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.utils.PermissionHelper
import com.example.mygallery.viewmodel.GalleryViewModel
import com.example.mygallery.viewmodel.GalleryViewModelFactory

class AlbumFragment : Fragment() {

    private lateinit var binding: FragmentAlbumBinding

    private lateinit var viewModel: GalleryViewModel

    private var isGridView = true

    private val requestPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            Log.d("PERMISSION", "isGranted = $isGranted")

            if (isGranted) {
                viewModel.loadFolders(requireContext())
            } else {
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


//        checkPermission()

        // Default Layout
        binding.recyclerAlbums.layoutManager =
            GridLayoutManager(requireContext(), 2)

        // Load folders from MediaStore
        checkPermission()

        // Observe only ONCE
        viewModel.folders.observe(viewLifecycleOwner) { folders ->
            binding.recyclerAlbums.adapter = GalleryAdapter(folders)
        }

        // Grid View
        binding.btnGridView.setOnClickListener {
            binding.recyclerAlbums.layoutManager =
                GridLayoutManager(requireContext(), 2)
        }

        // List View
        binding.btnListView.setOnClickListener {
            binding.recyclerAlbums.layoutManager =
                LinearLayoutManager(requireContext())
        }
    }



    private fun checkPermission() {

        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        if (ContextCompat.checkSelfPermission(
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