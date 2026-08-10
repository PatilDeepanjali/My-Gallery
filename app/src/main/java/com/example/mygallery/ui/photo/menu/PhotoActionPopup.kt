package com.example.mygallery.ui.photo.menu

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.example.mygallery.R
import com.example.mygallery.databinding.ItemPhotoPopupActionBinding
import com.example.mygallery.databinding.PopupPhotoActionsBinding
import com.example.mygallery.model.PopupMenuItem

object PhotoActionPopup {

    private val actions = listOf(

        PopupMenuItem(
            R.drawable.ic_select,
            "Select",
            PhotoAction.SELECT,
            true
        ),

        PopupMenuItem(
            R.drawable.ic_pin,
            "Pin",
            PhotoAction.PIN
        ),

        PopupMenuItem(
            R.drawable.ic_sort2,
            "Sort By",
            PhotoAction.SORT,
            true
        ),

        PopupMenuItem(
            R.drawable.ic_filter,
            "Filter",
            PhotoAction.FILTER,
            true
        ),

        PopupMenuItem(
            R.drawable.ic_layout,
            "Layout Style",
            PhotoAction.LAYOUT_STYLE,
            true
        ),

        PopupMenuItem(
            R.drawable.ic_column,
            "Column",
            PhotoAction.COLUMN,
            true
        ),

        PopupMenuItem(
            R.drawable.ic_slideshow,
            "Slide Show",
            PhotoAction.SLIDE_SHOW
        )

    )

    fun show(
        context: Context,
        anchorView: View,
        onActionSelected: (PhotoAction) -> Unit
    ) {

        val inflater = LayoutInflater.from(context)

        val containerBinding =
            PopupPhotoActionsBinding.inflate(inflater)

        val popupWindow = PopupWindow(
            containerBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        popupWindow.elevation = 16f

        for (item in actions) {

            val rowBinding = ItemPhotoPopupActionBinding.inflate(
                inflater,
                containerBinding.popupContainer,
                false
            )

            rowBinding.imgActionIcon.setImageResource(item.icon)
            rowBinding.tvActionLabel.text = item.title

            rowBinding.imgArrow.visibility =
                if (item.hasSubMenu) View.VISIBLE else View.GONE

            rowBinding.rowRoot.setOnClickListener {

                popupWindow.dismiss()
                onActionSelected(item.action)

            }

            containerBinding.popupContainer.addView(rowBinding.root)
        }

        containerBinding.root.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )

        val popupWidth = containerBinding.root.measuredWidth
        val xOffset = anchorView.width - popupWidth

        popupWindow.showAsDropDown(anchorView, xOffset, 7)
    }
}