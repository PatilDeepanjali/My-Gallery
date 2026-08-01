package com.example.mygallery.ui.photo

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.example.mygallery.R
import com.example.mygallery.databinding.ItemPhotoPopupActionBinding
import com.example.mygallery.databinding.ItemPopupActionBinding
import com.example.mygallery.databinding.PopupPhotoActionsBinding
import com.example.mygallery.model.PhotoAction

object PhotoActionPopup {

    // (icon, title, action)
    private val actions = listOf(

        Triple(R.drawable.ic_select, "Select", PhotoAction.SELECT),

        Triple(R.drawable.ic_pin, "Pin", PhotoAction.PIN),

        Triple(R.drawable.ic_sort, "Sort By", PhotoAction.SORT),

        Triple(R.drawable.ic_filter, "Filter", PhotoAction.FILTER),

        Triple(R.drawable.ic_layout, "Layout Style", PhotoAction.LAYOUT_STYLE),

        Triple(R.drawable.ic_column, "Column", PhotoAction.COLUMN),

        Triple(R.drawable.ic_slideshow, "Slide Show", PhotoAction.SLIDE_SHOW)

    )

    fun show(
        context: Context,
        anchorView: View,
        onActionSelected: (PhotoAction) -> Unit
    ) {

        val inflater = LayoutInflater.from(context)

        val containerBinding = PopupPhotoActionsBinding.inflate(inflater)

        val popupWindow = PopupWindow(
            containerBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Transparent background so only the custom rounded popup is visible
        popupWindow.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        popupWindow.elevation = 16f

        for ((iconRes, label, action) in actions) {

            val rowBinding = ItemPhotoPopupActionBinding.inflate(
                inflater,
                containerBinding.popupContainer,
                false
            )

            rowBinding.imgActionIcon.setImageResource(iconRes)
            rowBinding.tvActionLabel.text = label

            rowBinding.rowRoot.setOnClickListener {

                popupWindow.dismiss()
                onActionSelected(action)

            }

            containerBinding.popupContainer.addView(rowBinding.root)
        }

        // Show popup below the 3-dot button
        popupWindow.showAsDropDown(anchorView, 0, 7)
    }
}