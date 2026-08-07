package com.example.mygallery.ui.photo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mygallery.R
import com.example.mygallery.databinding.BottomSheetColumnBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ColumnBottomSheet(

    private val currentColumn: Int

) : BottomSheetDialogFragment() {


    private var selectedColumn = currentColumn

    private var _binding: BottomSheetColumnBinding? = null
    private val binding get() = _binding!!


    interface OnColumnSelected {

        fun onColumnSelected(column: Int)

    }

    private var listener: OnColumnSelected? = null

    fun setListener(listener: OnColumnSelected) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BottomSheetColumnBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateSelection()


        binding.layoutcolumn2.setOnClickListener {

            selectedColumn = 2
            updateSelection()

        }

        binding.column3.setOnClickListener {

            selectedColumn = 3
            updateSelection()

        }

        binding.column4.setOnClickListener {

            selectedColumn = 4
            updateSelection()

        }

        binding.column5.setOnClickListener {

            selectedColumn = 5
            updateSelection()

        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnApply.setOnClickListener {

            listener?.onColumnSelected(selectedColumn)

            dismiss()
        }
    }

    private fun updateSelection() {

        binding.column2Check.setImageResource(
            if (selectedColumn == 2)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )

        binding.column3Check.setImageResource(
            if (selectedColumn == 3)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )

        binding.column4Check.setImageResource(
            if (selectedColumn == 4)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )

        binding.column5Check.setImageResource(
            if (selectedColumn == 5)
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
