package com.example.mygallery.ui.photo.preview

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.example.mygallery.R
import com.example.mygallery.databinding.ItemPhotoPopupActionBinding
import com.example.mygallery.databinding.PopupPhotoActionsBinding
import com.example.mygallery.model.PopupMenuItem

object PreviewActionPopup {

    private val actions = listOf(

        PopupMenuItem(
            R.drawable.ic_copy,
            "Copy",
            PreviewAction.COPY
        ),

        PopupMenuItem(
            R.drawable.ic_move,
            "Move",
            PreviewAction.MOVE
        ),

        PopupMenuItem(
            R.drawable.ic_rename,
            "Rename",
            PreviewAction.RENAME
        ),

        PopupMenuItem(
            R.drawable.ic_open_with,
            "Open With",
            PreviewAction.OPEN_WITH
        ),

        PopupMenuItem(
            R.drawable.ic_slideshow,
            "Slide Show",
            PreviewAction.SLIDE_SHOW
        ),

        PopupMenuItem(
            R.drawable.ic_wallpaper,
            "Set as Wallpaper",
            PreviewAction.WALLPAPER
        ),

        PopupMenuItem(
            R.drawable.ic_details,
            "Details",
            PreviewAction.DETAILS
        )

    )

    fun show(
        context: Context,
        anchorView: View,
        onActionSelected: (PreviewAction) -> Unit
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

        // Add menu items
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

        // Measure popup size
        containerBinding.root.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )

        val popupWidth = containerBinding.root.measuredWidth
        val popupHeight = containerBinding.root.measuredHeight

        // Get anchor position on screen
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)

        val anchorX = location[0]
        val anchorY = location[1]

        // Align popup right edge with More button
        val x = anchorX + anchorView.width - popupWidth

        // Show popup ABOVE the More button
        val y = anchorY - popupHeight - 12

        popupWindow.showAtLocation(
            anchorView.rootView,
            Gravity.TOP or Gravity.START,
            x,
            y
        )
    }
}