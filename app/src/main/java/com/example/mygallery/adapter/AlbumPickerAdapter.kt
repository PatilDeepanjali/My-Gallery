package com.example.mygallery.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mygallery.R
import com.example.mygallery.databinding.ItemAlbumGridBinding
import com.example.mygallery.model.GalleryFolder

class AlbumPickerAdapter(
    private val folders: List<GalleryFolder>,
    private val onAlbumClick: (GalleryFolder) -> Unit
) : RecyclerView.Adapter<AlbumPickerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlbumGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = folders[position]

        holder.binding.tvAlbumName.text = folder.folderName
        holder.binding.tvAlbumMeta.text = "${folder.imageCount} Items"

        holder.binding.imgPin.visibility = View.GONE
        holder.binding.ivCheckbox.visibility = View.GONE

        if (folder.imageList.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(folder.imageList[0].uri)
                .centerCrop()
                .into(holder.binding.imgAlbum)
        } else {
            // A freshly created custom album with no photos yet has no
            // real image to use as a cover — clear any recycled image
            // and show a plain placeholder background instead.
            holder.binding.imgAlbum.setImageDrawable(null)
            holder.binding.imgAlbum.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.surfaceVariant)
            )
        }

        holder.itemView.setOnClickListener {
            onAlbumClick(folder)
        }
    }

    override fun getItemCount(): Int = folders.size

    class ViewHolder(val binding: ItemAlbumGridBinding) : RecyclerView.ViewHolder(binding.root)
}