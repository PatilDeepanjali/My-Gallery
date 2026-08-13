package com.example.mygallery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.mygallery.databinding.BottomSheetMenuBinding
import com.example.mygallery.repository.GalleryRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * The "Menu" tab's content — NOT a full screen. It's a bottom sheet
 * that pops up over whichever screen (Album/Photos) is currently
 * showing, then dismisses back to it. See MainActivity's handling of
 * R.id.menu for why this matters (we deliberately don't let the Menu
 * tab icon become "selected").
 */
class MenuBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMenuBinding? = null
    private val binding get() = _binding!!

    private val repository = GalleryRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadStats()

        binding.btnFavorites.setOnClickListener {
            Toast.makeText(requireContext(), "Favorites — coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnTrash.setOnClickListener {
            Toast.makeText(requireContext(), "Trash — coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnTheme.setOnClickListener {
            Toast.makeText(requireContext(), "Theme — coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnPrivacy.setOnClickListener {
            Toast.makeText(requireContext(), "Privacy — coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Computes the 3 stats from the same folder data the Album screen
     * already uses — no new Repository query type needed for Photos/
     * Albums counts. Videos is honestly hardcoded to 0 for now: this
     * app only queries MediaStore.Images, so there's no real video
     * data to count yet (that's the separate "Video Support" feature
     * from the roadmap).
     */
    private fun loadStats() {
        viewLifecycleOwner.lifecycleScope.launch {

            val folders = repository.getAllFolders(requireContext())

            val photoCount = folders.sumOf { it.imageCount }
            val albumCount = folders.size

            binding.tvPhotosCount.text = "%,d".format(photoCount)
            binding.tvVideosCount.text = "0"
            binding.tvAlbumsCount.text = "%,d".format(albumCount)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}