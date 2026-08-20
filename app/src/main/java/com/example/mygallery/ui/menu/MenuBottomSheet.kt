package com.example.mygallery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.mygallery.R
import com.example.mygallery.databinding.BottomSheetMenuBinding
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.ui.photo.FavoritesFragment
import com.example.mygallery.ui.photo.TrashFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

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

            // Dismiss the sheet, then navigate the underlying Activity
            // to FavoritesFragment — same frameContainer the bottom
            // nav tabs use, with a back-stack entry so the system
            // Back button returns to whichever tab was open before.
            dismiss()

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, FavoritesFragment())
                .addToBackStack("favorites")
                .commit()
        }

        binding.btnTrash.setOnClickListener {

            dismiss()

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, TrashFragment())
                .commit()
        }

        binding.btnTheme.setOnClickListener {
            Toast.makeText(requireContext(), "Theme — coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnPrivacy.setOnClickListener {
            Toast.makeText(requireContext(), "Privacy — coming soon", Toast.LENGTH_SHORT).show()
        }
    }

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