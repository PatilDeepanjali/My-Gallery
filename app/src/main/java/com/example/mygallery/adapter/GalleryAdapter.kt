package com.example.mygallery.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mygallery.R
import com.example.mygallery.databinding.ItemAlbumGridBinding
import com.example.mygallery.databinding.ItemAlbumListBinding
import com.example.mygallery.model.GalleryFolder
import com.example.mygallery.utils.PinPreferences

class GalleryAdapter(
    private var isGridView: Boolean,
    private val folderList: MutableList<GalleryFolder>,
    private val onFolderClick: (GalleryFolder) -> Unit,
    private val onFolderLongClick: (GalleryFolder) -> Unit,
    private val onFolderToggleSelect: (GalleryFolder) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val GRID = 0
        private const val LIST = 1
    }

    private var isSelectionMode = false
    private var selectedFolderNames: Set<String> = emptySet()

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

        // NOTE: previously this had "if (folder.imageList.isEmpty())
        // return" here — that silently skipped binding ANY empty album
        // entirely, leaving a blank/broken cell. Empty custom albums
        // (created via "+", with no photos yet) are a legitimate case
        // now, so we handle them below instead of bailing out.

        val hasPhotos = folder.imageList.isNotEmpty()
        val isSelected = selectedFolderNames.contains(folder.folderName)
        val isPinned = PinPreferences.isPinned(holder.itemView.context, folder.folderName)

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                onFolderToggleSelect(folder)
            } else {
                onFolderClick(folder)
            }
        }

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

                if (hasPhotos) {
                    Glide.with(holder.itemView.context)
                        .load(folder.imageList[0].uri)
                        .centerCrop()
                        .into(holder.binding.imgAlbum)
                } else {
                    // No cover photo available yet — clear any recycled
                    // image and show a plain placeholder background.
                    holder.binding.imgAlbum.setImageDrawable(null)
                    holder.binding.imgAlbum.setBackgroundColor(
                        ContextCompat.getColor(holder.itemView.context, R.color.surfaceVariant)
                    )
                }

                bindCheckbox(holder.binding.ivCheckbox, isSelected)

                holder.binding.imgPin.visibility =
                    if (isPinned) View.VISIBLE else View.GONE
            }

            is ListViewHolder -> {

                holder.binding.tvAlbumName.text = folder.folderName
                holder.binding.tvAlbumMeta.text = "${folder.imageCount} Items"

                if (hasPhotos) {
                    Glide.with(holder.itemView.context)
                        .load(folder.imageList[0].uri)
                        .centerCrop()
                        .into(holder.binding.imgAlbum)
                } else {
                    holder.binding.imgAlbum.setImageDrawable(null)
                    holder.binding.imgAlbum.setBackgroundColor(
                        ContextCompat.getColor(holder.itemView.context, R.color.surfaceVariant)
                    )
                }

                bindCheckbox(holder.binding.ivCheckbox, isSelected)

                holder.binding.imgPin.visibility =
                    if (isPinned) View.VISIBLE else View.GONE
            }
        }
    }

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