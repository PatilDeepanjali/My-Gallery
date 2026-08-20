package com.example.mygallery.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mygallery.R
import com.example.mygallery.databinding.ItemTrashPhotoBinding
import com.example.mygallery.model.ImageModel

class TrashAdapter(
    private var photos: List<ImageModel>,
    private val onPhotoClick: (ImageModel) -> Unit
) : RecyclerView.Adapter<TrashAdapter.TrashViewHolder>() {

    private val selectedIds =
        mutableSetOf<Long>()

    var isSelectionMode = false
        private set

    inner class TrashViewHolder(
        private val binding: ItemTrashPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: ImageModel) {

            Glide.with(binding.imgPhoto.context)
                .load(photo.uri)
                .centerCrop()
                .into(binding.imgPhoto)

            // For now we show 29 days as in the Figma.
            binding.tvDaysRemaining.text = "29 days"

            updateSelectionUI(photo)

            binding.root.setOnClickListener {
                onPhotoClick(photo)
            }
        }

        private fun updateSelectionUI(
            photo: ImageModel
        ) {

            if (isSelectionMode) {

                binding.imgSelection.visibility =
                    android.view.View.VISIBLE

                binding.imgSelection.setImageResource(
                    if (selectedIds.contains(photo.id)) {
                        R.drawable.ic_check_circle_filled
                    } else {
                        R.drawable.ic_check_circle_outline
                    }
                )

            } else {

                binding.imgSelection.visibility =
                    android.view.View.GONE
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TrashViewHolder {

        val binding =
            ItemTrashPhotoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return TrashViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: TrashViewHolder,
        position: Int
    ) {

        holder.bind(photos[position])
    }

    override fun getItemCount(): Int =
        photos.size

    fun submitList(
        newPhotos: List<ImageModel>
    ) {

        photos = newPhotos

        selectedIds.retainAll(
            photos.map { it.id }.toSet()
        )

        notifyDataSetChanged()
    }

    fun enterSelectionMode(
        photoId: Long
    ) {

        isSelectionMode = true

        selectedIds.clear()
        selectedIds.add(photoId)

        notifyDataSetChanged()
    }

    fun toggleSelection(
        photoId: Long
    ) {

        if (selectedIds.contains(photoId)) {

            selectedIds.remove(photoId)

        } else {

            selectedIds.add(photoId)
        }

        if (selectedIds.isEmpty()) {

            isSelectionMode = false
        }

        notifyDataSetChanged()
    }

    fun exitSelectionMode() {

        isSelectionMode = false
        selectedIds.clear()

        notifyDataSetChanged()
    }

    fun getSelectedPhotos(): List<ImageModel> {

        return photos.filter {
            selectedIds.contains(it.id)
        }
    }
    fun startSelectionMode() {

        isSelectionMode = true

        selectedIds.clear()

        notifyDataSetChanged()
    }

    fun getSelectedCount(): Int =
        selectedIds.size
}