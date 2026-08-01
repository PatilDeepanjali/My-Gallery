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
import com.example.mygallery.model.GalleryFolder
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

            AlbumActionPopup.show(requireContext(), binding.toolbarSelection.btnOverflow) { action ->
                handleAlbumAction(action, selectedFolders)
            }
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

                // In case Success arrives WHILE already in selection mode
                // (e.g. list refreshed after a delete), make sure the new
                // adapter instance immediately reflects current selection.
                renderSelectionState()
            }
        }
    }

    /**
     * Single place that reacts to BOTH isSelectionMode and
     * selectedFolderNames changing — keeps the toolbar swap, the
     * toolbar's count/size text, and the adapter's checkboxes all in
     * sync with one source of truth (the ViewModel).
     */
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

    /**
     * Handles whichever action the user picked from the popup, applied
     * to ALL currently selected folders. Most of these are stubs for
     * now (Toast only) — each one is its own real feature we'll build
     * properly in a later step. Details is implemented fully since it
     * only needs data we already have.
     */
    private fun handleAlbumAction(action: AlbumAction, folders: List<GalleryFolder>) {
        when (action) {

            AlbumAction.PIN -> {
                Toast.makeText(requireContext(), "Pin — coming soon", Toast.LENGTH_SHORT).show()
            }

            AlbumAction.SHARE -> {
                Toast.makeText(requireContext(), "Share — coming soon", Toast.LENGTH_SHORT).show()
            }

            AlbumAction.DELETE -> {
                Toast.makeText(requireContext(), "Delete — coming soon", Toast.LENGTH_SHORT).show()
            }

            AlbumAction.COPY -> {
                Toast.makeText(requireContext(), "Copy — coming soon", Toast.LENGTH_SHORT).show()
            }

            AlbumAction.MOVE -> {
                Toast.makeText(requireContext(), "Move — coming soon", Toast.LENGTH_SHORT).show()
            }

            AlbumAction.DETAILS -> {
                showAlbumDetailsDialog(folders)
            }
        }
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