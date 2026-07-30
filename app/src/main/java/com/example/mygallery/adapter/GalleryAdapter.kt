package com.example.mygallery.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mygallery.databinding.ItemAlbumGridBinding
import com.example.mygallery.databinding.ItemAlbumListBinding
import com.example.mygallery.model.GalleryFolder

class GalleryAdapter(
    private var isGridView: Boolean,
    private val folderList: MutableList<GalleryFolder>,
    private val onFolderClick: (GalleryFolder) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val GRID = 0
        private const val LIST = 1
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

        holder.itemView.setOnClickListener {
            onFolderClick(folder)
        }


        when (holder) {

            is GridViewHolder -> {

                holder.binding.tvAlbumName.text = folder.folderName
                holder.binding.tvAlbumMeta.text = "${folder.imageCount} Items"

                Glide.with(holder.itemView.context)
                    .load(coverImage.uri)
                    .centerCrop()
                    .into(holder.binding.imgAlbum)
            }

            is ListViewHolder -> {

                holder.binding.tvAlbumName.text = folder.folderName
                holder.binding.tvAlbumMeta.text = "${folder.imageCount} Items"

                Glide.with(holder.itemView.context)
                    .load(coverImage.uri)
                    .centerCrop()
                    .into(holder.binding.imgAlbum)
            }
        }
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
