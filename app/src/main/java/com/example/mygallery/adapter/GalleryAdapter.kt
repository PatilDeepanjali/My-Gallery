package com.example.mygallery.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mygallery.databinding.ItemAlbumBinding
import com.example.mygallery.model.GalleryFolder
import java.io.File

class GalleryAdapter(val folderList: ArrayList<GalleryFolder>) :
    RecyclerView.Adapter<GalleryAdapter.GalleryViewHoder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GalleryViewHoder {
        var binding: ItemAlbumBinding =
            ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return GalleryViewHoder(binding)
    }

    override fun onBindViewHolder(
        holder: GalleryViewHoder,
        position: Int
    ) {

        val folder = folderList[position]

        var coverImage = folder.imageList[0]

        holder.binding.tvAlbumName.text = folder.folderName
        holder.binding.tvAlbumMeta.text = folder.imageCount.toString()
        holder.binding.imgAlbum

        if(folder.imageList.isNotEmpty())
        {
            Glide.with(holder.itemView.context)
                .load(coverImage.uri)
                .centerCrop()
                .into(holder.binding.imgAlbum)

        }


    }

    override fun getItemCount(): Int {

        return folderList.size

    }

    class GalleryViewHoder(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root) {


    }
}