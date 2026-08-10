package com.example.mygallery.ui.photo.selection

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.example.mygallery.R
import com.example.mygallery.databinding.ItemPopupActionBinding
import com.example.mygallery.databinding.PopupAlbumActionsBinding
import com.example.mygallery.model.PopupMenuItem

object PhotoSelectionActionPopup {

    fun show(
        context: Context,
        anchorView: View,
        selectedCount: Int,
        onActionSelected: (PhotoSelectionAction) -> Unit
    ) {

        /*
         * SINGLE PHOTO
         *
         * Copy
         * Move
         * Rename
         * Favorite
         * Open With
         * Slide Show
         * Edit With
         * Set As Wallpaper
         * Share
         * Delete
         * Details
         */
        val singlePhotoActions = listOf(

            PopupMenuItem(
                R.drawable.ic_copy,
                "Copy",
                PhotoSelectionAction.COPY
            ),

            PopupMenuItem(
                R.drawable.ic_move,
                "Move",
                PhotoSelectionAction.MOVE
            ),

            PopupMenuItem(
                R.drawable.ic_rename,
                "Rename",
                PhotoSelectionAction.RENAME
            ),

            PopupMenuItem(
                R.drawable.ic_heart,
                "Favorite",
                PhotoSelectionAction.FAVORITE
            ),

            PopupMenuItem(
                R.drawable.ic_open_with,
                "Open With",
                PhotoSelectionAction.OPEN_WITH
            ),

            PopupMenuItem(
                R.drawable.ic_slideshow,
                "Slide Show",
                PhotoSelectionAction.SLIDE_SHOW
            ),

            PopupMenuItem(
                R.drawable.ic_edit,
                "Edit With",
                PhotoSelectionAction.EDIT_WITH
            ),

            PopupMenuItem(
                R.drawable.ic_wallpaper,
                "Set As Wallpaper",
                PhotoSelectionAction.SET_AS_WALLPAPER
            ),

            PopupMenuItem(
                R.drawable.ic_share,
                "Share",
                PhotoSelectionAction.SHARE
            ),

            PopupMenuItem(
                R.drawable.ic_delete,
                "Delete",
                PhotoSelectionAction.DELETE
            ),

            PopupMenuItem(
                R.drawable.ic_details,
                "Details",
                PhotoSelectionAction.DETAILS
            )
        )


        /*
         * MULTIPLE PHOTOS
         *
         * Copy
         * Move
         * Favorite
         * Slide Show
         * Share
         * Delete
         * Details
         */
        val multiplePhotoActions = listOf(

            PopupMenuItem(
                R.drawable.ic_copy,
                "Copy",
                PhotoSelectionAction.COPY
            ),

            PopupMenuItem(
                R.drawable.ic_move,
                "Move",
                PhotoSelectionAction.MOVE
            ),

            PopupMenuItem(
                R.drawable.ic_heart,
                "Favorite",
                PhotoSelectionAction.FAVORITE
            ),

            PopupMenuItem(
                R.drawable.ic_slideshow,
                "Slide Show",
                PhotoSelectionAction.SLIDE_SHOW
            ),

            PopupMenuItem(
                R.drawable.ic_share,
                "Share",
                PhotoSelectionAction.SHARE
            ),

            PopupMenuItem(
                R.drawable.ic_delete,
                "Delete",
                PhotoSelectionAction.DELETE
            ),

            PopupMenuItem(
                R.drawable.ic_details,
                "Details",
                PhotoSelectionAction.DETAILS
            )
        )


        val actions =
            if (selectedCount == 1) {
                singlePhotoActions
            } else {
                multiplePhotoActions
            }


        val inflater = LayoutInflater.from(context)

        val containerBinding =
            PopupAlbumActionsBinding.inflate(inflater)

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


        for ((iconRes, label, action) in actions) {

            val rowBinding = ItemPopupActionBinding.inflate(
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


        containerBinding.root.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )

        val popupWidth =
            containerBinding.root.measuredWidth

        val xOffset =
            anchorView.width - popupWidth


        popupWindow.showAsDropDown(
            anchorView,
            xOffset,
            0
        )
    }
}