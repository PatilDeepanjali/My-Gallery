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

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemAlbumGridBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return ViewHolder(binding)
    }


    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val folder =
            folders[position]

        val binding =
            holder.binding

        // -----------------------------------------------------
        // Album name
        // -----------------------------------------------------

        binding.tvAlbumName.text =
            folder.folderName


        // -----------------------------------------------------
        // Album item count
        // -----------------------------------------------------

        binding.tvAlbumMeta.text =
            "${folder.imageCount} Items"


        // -----------------------------------------------------
        // Hide selection/pin controls
        // -----------------------------------------------------

        binding.imgPin.visibility =
            View.GONE

        binding.ivCheckbox.visibility =
            View.GONE


        // -----------------------------------------------------
        // Reset recycled image state
        // -----------------------------------------------------

        binding.imgAlbum.setImageDrawable(
            null
        )

        binding.imgAlbum.background =
            null


        // -----------------------------------------------------
        // Album cover
        // -----------------------------------------------------

        if (
            folder.imageList.isNotEmpty()
        ) {

            val coverUri =
                folder.imageList
                    .first()
                    .uri

            Glide.with(
                binding.imgAlbum.context
            )
                .load(coverUri)
                .centerCrop()
                .placeholder(
                    R.color.surfaceVariant
                )
                .error(
                    R.color.surfaceVariant
                )
                .into(
                    binding.imgAlbum
                )

        } else {

            // Empty custom album
            binding.imgAlbum.setBackgroundColor(
                ContextCompat.getColor(
                    binding.imgAlbum.context,
                    R.color.surfaceVariant
                )
            )
        }


        // -----------------------------------------------------
        // Click
        // -----------------------------------------------------

        binding.root.setOnClickListener {

            onAlbumClick(
                folder
            )
        }
    }


    override fun getItemCount(): Int =
        folders.size


    class ViewHolder(
        val binding: ItemAlbumGridBinding
    ) : RecyclerView.ViewHolder(
        binding.root
    )
}