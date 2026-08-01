package com.example.mygallery.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mygallery.R
import com.example.mygallery.databinding.ItemAlbumGridBinding
import com.example.mygallery.databinding.ItemAlbumListBinding
import com.example.mygallery.model.GalleryFolder

class GalleryAdapter(
    private var isGridView: Boolean,
    private val folderList: MutableList<GalleryFolder>,
    private val onFolderClick: (GalleryFolder) -> Unit,
    // Called on long-press to ENTER selection mode (only fires when not
    // already in selection mode — see onBindViewHolder below).
    private val onFolderLongClick: (GalleryFolder) -> Unit,
    // Called on tap WHILE already in selection mode, to toggle that
    // folder's checkbox on/off.
    private val onFolderToggleSelect: (GalleryFolder) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val GRID = 0
        private const val LIST = 1
    }

    // Selection mode state, pushed in from the Fragment (which gets it
    // from the ViewModel). The adapter doesn't own this state — it just
    // renders whatever it's told.
    private var isSelectionMode = false
    private var selectedFolderNames: Set<String> = emptySet()

    /**
     * Called by the Fragment whenever selection state changes, so the
     * adapter can re-render checkboxes accordingly.
     */
    fun setSelectionState(active: Boolean, selected: Set<String>) {
        isSelectionMode = active
        selectedFolderNames = selected
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (isGridView) GRID else LIST
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        return if (viewType == GRID) {

            val binding = ItemAlbumGridBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            GridViewHolder(binding)

        } else {

            val binding = ItemAlbumListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            ListViewHolder(binding)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        val folder = folderList[position]

        if (folder.imageList.isEmpty()) return

        val coverImage = folder.imageList[0]
        val isSelected = selectedFolderNames.contains(folder.folderName)

        // Tap behavior depends on the current mode.
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                onFolderToggleSelect(folder)
            } else {
                onFolderClick(folder)
            }
        }

        // Long-press only "enters" selection mode when we're not already
        // in it. If we ARE already in selection mode, a long-press just
        // behaves like a normal tap (toggle), since there's no mode left
        // to enter.
        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                onFolderLongClick(folder)
            } else {
                onFolderToggleSelect(folder)
            }
            true
        }

        when (holder) {

            is GridViewHolder -> {

                holder.binding.tvAlbumName.text = folder.folderName
                holder.binding.tvAlbumMeta.text = "${folder.imageCount} Items"

                Glide.with(holder.itemView.context)
                    .load(coverImage.uri)
                    .centerCrop()
                    .into(holder.binding.imgAlbum)

                bindCheckbox(holder.binding.ivCheckbox, isSelected)
            }

            is ListViewHolder -> {

                holder.binding.tvAlbumName.text = folder.folderName
                holder.binding.tvAlbumMeta.text = "${folder.imageCount} Items"

                Glide.with(holder.itemView.context)
                    .load(coverImage.uri)
                    .centerCrop()
                    .into(holder.binding.imgAlbum)

                bindCheckbox(holder.binding.ivCheckbox, isSelected)
            }
        }
    }

    /**
     * Shows the checkbox only in selection mode, and swaps between the
     * filled (selected) and outline (unselected) icon.
     *
     * NOTE: ic_check_circle_outline is a NEW drawable you'll need to add
     * (Vector Asset Studio) — it's the empty/outline circle shown on
     * unselected items in your design. ic_check_circle_filled already
     * exists and is used for selected items.
     */
    private fun bindCheckbox(checkbox: android.widget.ImageView, isSelected: Boolean) {
        checkbox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        checkbox.setImageResource(
            if (isSelected) R.drawable.ic_check_circle_filled
            else R.drawable.ic_check_circle_outline
        )
    }

    fun updateList(newList: List<GalleryFolder>) {
        folderList.clear()
        folderList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return folderList.size
    }

    fun setViewMode(isGrid: Boolean) {
        isGridView = isGrid
        notifyDataSetChanged()
    }

    class GridViewHolder(
        val binding: ItemAlbumGridBinding
    ) : RecyclerView.ViewHolder(binding.root)

    class ListViewHolder(
        val binding: ItemAlbumListBinding
    ) : RecyclerView.ViewHolder(binding.root)
}