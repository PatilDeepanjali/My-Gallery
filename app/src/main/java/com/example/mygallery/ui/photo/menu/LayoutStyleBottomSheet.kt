package com.example.mygallery.ui.photo.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mygallery.R
import com.example.mygallery.databinding.BottomSheetLayoutStyleBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LayoutStyleBottomSheet(
    private val isGridView: Boolean
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLayoutStyleBinding? = null
    private val binding get() = _binding!!

    private var selectedGrid = isGridView

    interface OnLayoutStyleSelected {
        fun onLayoutSelected(isGrid: Boolean)
    }

    private var listener: OnLayoutStyleSelected? = null

    fun setListener(listener: OnLayoutStyleSelected) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BottomSheetLayoutStyleBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateSelection()




        binding.layoutList.setOnClickListener {

            selectedGrid = false
            updateSelection()

        }
        binding.layoutGrid.setOnClickListener {

            selectedGrid = true
            updateSelection()

        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnApply.setOnClickListener {

            listener?.onLayoutSelected(selectedGrid)

            dismiss()
        }
    }

    private fun updateSelection() {

        if (selectedGrid) {

            binding.imgGridCheck.setImageResource(R.drawable.ic_check_circle_filled)
            binding.imgListCheck.setImageResource(R.drawable.ic_plane_circle)

        } else {

            binding.imgGridCheck.setImageResource(R.drawable.ic_plane_circle)
            binding.imgListCheck.setImageResource(R.drawable.ic_check_circle_filled)

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}