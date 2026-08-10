package com.example.mygallery.ui.photo.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mygallery.R
import com.example.mygallery.databinding.BottomSheetSortBinding
import com.example.mygallery.ui.photo.menu.SortOrder
import com.example.mygallery.ui.photo.menu.SortType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SortBottomSheet(

    private val currentSortType: SortType,
    private val currentSortOrder: SortOrder

) : BottomSheetDialogFragment()
{

    private var selectedSortType = currentSortType

    private var selectedSortOrder = currentSortOrder

    private var _binding: BottomSheetSortBinding? = null
    private val binding get() = _binding!!


    interface OnSortSelected {

        fun onSortSelected(
            sortType: SortType,
            sortOrder: SortOrder
        )

    }
    private var listener: OnSortSelected? = null

    fun setListener(listener: OnSortSelected) {
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

        updateSelection()


        binding.layoutDateTaken.setOnClickListener {

            selectedSortType = SortType.DATE_TAKEN
            updateSelection()

        }

        binding.layoutLastModified.setOnClickListener {

            selectedSortType = SortType.LAST_MODIFIED
            updateSelection()

        }

        binding.layoutAlbumName.setOnClickListener {

            selectedSortType = SortType.ALBUM_NAME
            updateSelection()

        }

        binding.layoutSize.setOnClickListener {

            selectedSortType = SortType.SIZE
            updateSelection()

        }



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

            listener?.onSortSelected(

                selectedSortType,

                selectedSortOrder

            )
            dismiss()
        }
    }

    private fun updateSelection() {

        // ---------- Sort Type ----------

        binding.imgDateTakenCheck.setImageResource(
            if (selectedSortType == SortType.DATE_TAKEN)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )

        binding.imgLastModifiedCheck.setImageResource(
            if (selectedSortType == SortType.LAST_MODIFIED)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )

        binding.imgAlbumNameCheck.setImageResource(
            if (selectedSortType == SortType.ALBUM_NAME)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )

        binding.imgSizeCheck.setImageResource(
            if (selectedSortType == SortType.SIZE)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )


        // ---------- Sort Order ----------

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