package com.example.mygallery.adapter

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mygallery.databinding.ItemDateHeaderBinding
import com.example.mygallery.databinding.ItemPhotoGridBinding
import com.example.mygallery.databinding.ItemPhotoListBinding
import com.example.mygallery.model.PhotoListItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotosAdapter(
    private var isGridView: Boolean,
    private val items: List<PhotoListItem>,
    private val onPhotoClick: (PhotoListItem.Photo) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PHOTO_GRID = 1
        private const val TYPE_PHOTO_LIST = 2
    }

    fun getSpanSize(position: Int, totalSpanCount: Int): Int {
        return if (items[position] is PhotoListItem.DateHeader) totalSpanCount else 1
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            items[position] is PhotoListItem.DateHeader -> TYPE_HEADER
            isGridView -> TYPE_PHOTO_GRID
            else -> TYPE_PHOTO_LIST
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {

            TYPE_HEADER -> {
                val binding = ItemDateHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }

            TYPE_PHOTO_GRID -> {
                val binding = ItemPhotoGridBinding.inflate(inflater, parent, false)
                GridViewHolder(binding)
            }

            else -> {
                val binding = ItemPhotoListBinding.inflate(inflater, parent, false)
                ListViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder) {

            is HeaderViewHolder -> {
                val header = items[position] as PhotoListItem.DateHeader
                holder.binding.root.text = header.label
            }

            is GridViewHolder -> {
                val photo = items[position] as PhotoListItem.Photo

                holder.itemView.setOnClickListener { onPhotoClick(photo) }

                Glide.with(holder.itemView.context)
                    .load(photo.image.uri)
                    .centerCrop()
                    .into(holder.binding.imgPhoto)
            }

            is ListViewHolder -> {
                val photo = items[position] as PhotoListItem.Photo

                holder.itemView.setOnClickListener { onPhotoClick(photo) }

                holder.binding.tvPhotoName.text = photo.image.name

                val formattedDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                    .format(Date(photo.image.dateAdded * 1000))
                val formattedSize = Formatter.formatShortFileSize(
                    holder.itemView.context,
                    photo.image.size
                )
                holder.binding.tvPhotoMeta.text = "$formattedDate • $formattedSize"

                Glide.with(holder.itemView.context)
                    .load(photo.image.uri)
                    .centerCrop()
                    .into(holder.binding.imgPhoto)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun setViewMode(isGrid: Boolean) {
        isGridView = isGrid
        notifyDataSetChanged()
    }

    class HeaderViewHolder(
        val binding: ItemDateHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root)

    class GridViewHolder(
        val binding: ItemPhotoGridBinding
    ) : RecyclerView.ViewHolder(binding.root)

    class ListViewHolder(
        val binding: ItemPhotoListBinding
    ) : RecyclerView.ViewHolder(binding.root)
}