package com.example.mygallery.ui.album

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mygallery.R
import com.example.mygallery.databinding.BottomSheetSortBinding
import com.example.mygallery.ui.photo.menu.SortOrder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AlbumSortBottomSheet(

    private val currentSortType: AlbumSortType,
    private val currentSortOrder: SortOrder

) : BottomSheetDialogFragment() {

    private var selectedSortType = currentSortType
    private var selectedSortOrder = currentSortOrder

    // Same binding class as Photos' SortBottomSheet — this is the
    // actual "one shared file" win: both classes inflate the exact
    // same layout, each configuring it for their own context.
    private var _binding: BottomSheetSortBinding? = null
    private val binding get() = _binding!!

    interface OnAlbumSortSelected {
        fun onAlbumSortSelected(sortType: AlbumSortType, sortOrder: SortOrder)
    }

    private var listener: OnAlbumSortSelected? = null

    fun setListener(listener: OnAlbumSortSelected) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure each generic slot for the ALBUM context.
        binding.imgOption1Icon.setImageResource(R.drawable.ic_album)
        binding.tvOption1Label.text = "Name"
        binding.layoutOption1.setOnClickListener {
            selectedSortType = AlbumSortType.NAME
            updateSelection()
        }

        binding.imgOption2Icon.setImageResource(R.drawable.ic_list)
        binding.tvOption2Label.text = "Item Count"
        binding.layoutOption2.setOnClickListener {
            selectedSortType = AlbumSortType.ITEM_COUNT
            updateSelection()
        }

        binding.imgOption3Icon.setImageResource(R.drawable.ic_size)
        binding.tvOption3Label.text = "Size"
        binding.layoutOption3.setOnClickListener {
            selectedSortType = AlbumSortType.SIZE
            updateSelection()
        }

        binding.imgOption4Icon.setImageResource(R.drawable.ic_calendar)
        binding.tvOption4Label.text = "Date Added"
        binding.layoutOption4.setOnClickListener {
            selectedSortType = AlbumSortType.DATE_ADDED
            updateSelection()
        }

        updateSelection()

        binding.layoutAscending.setOnClickListener {
            selectedSortOrder = SortOrder.ASCENDING
            updateSelection()
        }

        binding.layoutDescending.setOnClickListener {
            selectedSortOrder = SortOrder.DESCENDING
            updateSelection()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnApply.setOnClickListener {
            listener?.onAlbumSortSelected(selectedSortType, selectedSortOrder)
            dismiss()
        }
    }

    private fun updateSelection() {

        binding.imgOption1Check.setImageResource(
            if (selectedSortType == AlbumSortType.NAME)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )

        binding.imgOption2Check.setImageResource(
            if (selectedSortType == AlbumSortType.ITEM_COUNT)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )

        binding.imgOption3Check.setImageResource(
            if (selectedSortType == AlbumSortType.SIZE)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )

        binding.imgOption4Check.setImageResource(
            if (selectedSortType == AlbumSortType.DATE_ADDED)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )

        binding.imgAscendingCheck.setImageResource(
            if (selectedSortOrder == SortOrder.ASCENDING)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )

        binding.imgDescendingCheck.setImageResource(
            if (selectedSortOrder == SortOrder.DESCENDING)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}