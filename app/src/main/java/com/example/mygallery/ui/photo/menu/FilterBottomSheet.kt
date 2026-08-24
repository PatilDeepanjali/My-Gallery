package com.example.mygallery.ui.photo.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.mygallery.R
import com.example.mygallery.databinding.BottomSheetFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheet(
    private val currentFilter: FilterType =
        FilterType.ALL_ITEMS
) : BottomSheetDialogFragment() {

    interface OnFilterSelected {

        fun onFilterSelected(
            filterType: FilterType
        )
    }

    private var listener:
            OnFilterSelected? = null

    private var _binding:
            BottomSheetFilterBinding? = null

    private val binding
        get() = _binding!!


    private var selectedFilter =
        currentFilter


    fun setListener(
        listener: OnFilterSelected
    ) {

        this.listener = listener
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            BottomSheetFilterBinding.inflate(
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

        super.onViewCreated(
            view,
            savedInstanceState
        )


        // -----------------------------------------------------
        // Initial selection
        // -----------------------------------------------------

        updateSelectionUI()


        // -----------------------------------------------------
        // All Items
        // -----------------------------------------------------

        binding.filterAllItems.setOnClickListener {

            selectedFilter =
                FilterType.ALL_ITEMS

            updateSelectionUI()
        }


        // -----------------------------------------------------
        // Photos
        // -----------------------------------------------------

        binding.filterPhotos.setOnClickListener {

            selectedFilter =
                FilterType.PHOTOS

            updateSelectionUI()
        }


        // -----------------------------------------------------
        // Videos
        // -----------------------------------------------------

        binding.filterVideos.setOnClickListener {

            selectedFilter =
                FilterType.VIDEOS

            updateSelectionUI()
        }


        // -----------------------------------------------------
        // Favourite
        // -----------------------------------------------------

        binding.filterFavourite.setOnClickListener {

            selectedFilter =
                FilterType.FAVOURITE

            updateSelectionUI()
        }


        // -----------------------------------------------------
        // Screenshots
        // -----------------------------------------------------

        binding.filterScreenshots.setOnClickListener {

            selectedFilter =
                FilterType.SCREENSHOTS

            updateSelectionUI()
        }


        // -----------------------------------------------------
        // Cancel
        // -----------------------------------------------------

        binding.btnCancel.setOnClickListener {

            dismiss()
        }


        // -----------------------------------------------------
        // Apply
        // -----------------------------------------------------

        binding.btnApply.setOnClickListener {

            listener?.onFilterSelected(
                selectedFilter
            )

            dismiss()
        }
    }


    // =========================================================
    // Selection UI
    // =========================================================

    private fun updateSelectionUI() {

        setCheck(
            binding.filterAllItemsCheck,
            selectedFilter ==
                    FilterType.ALL_ITEMS
        )

        setCheck(
            binding.filterPhotosCheck,
            selectedFilter ==
                    FilterType.PHOTOS
        )

        setCheck(
            binding.filterVideosCheck,
            selectedFilter ==
                    FilterType.VIDEOS
        )

        setCheck(
            binding.filterFavouriteCheck,
            selectedFilter ==
                    FilterType.FAVOURITE
        )

        setCheck(
            binding.filterScreenshotsCheck,
            selectedFilter ==
                    FilterType.SCREENSHOTS
        )
    }


    private fun setCheck(
        imageView: ImageView,
        selected: Boolean
    ) {

        imageView.setImageResource(

            if (selected) {

                R.drawable.ic_check_circle_filled

            } else {

                R.drawable.ic_plane_circle
            }
        )
    }


    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}