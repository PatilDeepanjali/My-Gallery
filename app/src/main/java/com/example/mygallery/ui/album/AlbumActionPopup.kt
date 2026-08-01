package com.example.mygallery.ui.album

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.example.mygallery.R
import com.example.mygallery.databinding.ItemPopupActionBinding
import com.example.mygallery.databinding.PopupAlbumActionsBinding

/**
 * Builds and shows the long-press popup for an album (Pin / Share /
 * Delete / Copy / Move / Details).
 *
 * Instead of repeating near-identical XML for each of the 6 rows, we
 * define them once as data here and inflate item_popup_action.xml in
 * a loop, setting the icon/label/click-action for each row in code.
 * This is the same "don't repeat structurally identical XML" idea we
 * used for PhotosAdapter's view types.
 */
object AlbumActionPopup {

    // (icon resource, display label, action) for each row, in display order.
    private val actions = listOf(
        Triple(R.drawable.ic_pin, "Pin", AlbumAction.PIN),
        Triple(R.drawable.ic_share, "Share", AlbumAction.SHARE),
        Triple(R.drawable.ic_delete, "Delete", AlbumAction.DELETE),
        Triple(R.drawable.ic_copy, "Copy", AlbumAction.COPY),
        Triple(R.drawable.ic_move, "Move", AlbumAction.MOVE),
        Triple(R.drawable.ic_details, "Details", AlbumAction.DETAILS)
    )

    fun show(
        context: Context,
        anchorView: View,
        onActionSelected: (AlbumAction) -> Unit
    ) {
        val inflater = LayoutInflater.from(context)

        val containerBinding = PopupAlbumActionsBinding.inflate(inflater)

        // PopupWindow needs its size + whether it's "focusable" up front.
        // focusable = true is what makes tapping outside the popup
        // dismiss it automatically.
        val popupWindow = PopupWindow(
            containerBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // PopupWindow draws its own background box by default, which
        // can visually clash with (and flatten the shadow of) our own
        // rounded bg_popup_menu drawable. Making it transparent lets
        // ONLY our card's shape/shadow show through.
        popupWindow.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )


        popupWindow.elevation = 16f

        popupWindow.showAsDropDown(anchorView, 0, 7)

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

        // Shows the popup just below-and-left of the long-pressed item.
        popupWindow.showAsDropDown(anchorView, 0, 0)
    }
}