package com.example.mygallery.ui.photo.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mygallery.databinding.BottomSheetPhotoDetailsBinding
import com.example.mygallery.model.ImageModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PhotoDetailsBottomSheet(
    private val photos: ArrayList<ImageModel>
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPhotoDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BottomSheetPhotoDetailsBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        if (photos.size == 1) {
            showSinglePhotoDetails(photos.first())
        } else {
            showMultiplePhotoDetails(photos)
        }

        binding.btnOkay.setOnClickListener {
            dismiss()
        }
    }

    private fun showSinglePhotoDetails(photo: ImageModel) {

        binding.singleDetailsGroup.visibility = View.VISIBLE
        binding.multipleDetailsGroup.visibility = View.GONE

        binding.tvNameValue.text = photo.name

        binding.tvPathValue.text =
            photo.uri.toString()

        binding.tvSizeValue.text =
            android.text.format.Formatter.formatShortFileSize(
                requireContext(),
                photo.size
            )

        binding.tvResolutionValue.text =
            "Not available"

        binding.tvModifiedValue.text =
            formatDate(photo.dateAdded)

        binding.tvTakenValue.text =
            formatDate(photo.dateAdded)
    }

    private fun showMultiplePhotoDetails(
        photos: List<ImageModel>
    ) {

        binding.singleDetailsGroup.visibility = View.GONE
        binding.multipleDetailsGroup.visibility = View.VISIBLE

        binding.tvSelectedCountValue.text =
            photos.size.toString()

        val totalSize =
            photos.sumOf { it.size }

        binding.tvTotalSizeValue.text =
            android.text.format.Formatter.formatShortFileSize(
                requireContext(),
                totalSize
            )
    }

    private fun formatDate(seconds: Long): String {

        return java.text.SimpleDateFormat(
            "MMMM dd, yyyy",
            java.util.Locale.getDefault()
        ).format(
            java.util.Date(seconds * 1000)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}