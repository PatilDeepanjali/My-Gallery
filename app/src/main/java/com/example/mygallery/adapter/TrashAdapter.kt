package com.example.mygallery.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mygallery.R
import com.example.mygallery.databinding.ItemTrashPhotoBinding
import com.example.mygallery.model.TrashItem
import java.io.File

class TrashAdapter(
    private var photos: List<TrashItem>,
    private val onPhotoClick: (TrashItem) -> Unit
) : RecyclerView.Adapter<TrashAdapter.TrashViewHolder>() {

    private val selectedIds =
        mutableSetOf<Long>()

    var isSelectionMode = false
        private set

    inner class TrashViewHolder(
        private val binding: ItemTrashPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: TrashItem) {

            Glide.with(binding.imgPhoto.context)
                .load(File(photo.trashFilePath))
                .centerCrop()
                .into(binding.imgPhoto)

            binding.tvDaysRemaining.text =
                calculateDaysRemaining(photo.trashedAt)

            updateSelectionUI(photo)

            binding.root.setOnClickListener {
                onPhotoClick(photo)
            }
        }

        private fun updateSelectionUI(
            photo: TrashItem
        ) {

            if (isSelectionMode) {

                binding.imgSelection.visibility =
                    View.VISIBLE

                binding.imgSelection.setImageResource(
                    if (selectedIds.contains(photo.id)) {
                        R.drawable.ic_check_circle_filled
                    } else {
                        R.drawable.ic_check_circle_outline
                    }
                )

            } else {

                binding.imgSelection.visibility =
                    View.GONE
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

        holder.bind(
            photos[position]
        )
    }

    override fun getItemCount(): Int =
        photos.size

    fun submitList(
        newPhotos: List<TrashItem>
    ) {

        photos = newPhotos

        selectedIds.retainAll(
            photos.map { it.id }.toSet()
        )

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

    fun startSelectionMode() {

        isSelectionMode = true
        selectedIds.clear()

        notifyDataSetChanged()
    }

    fun getSelectedPhotos(): List<TrashItem> {

        return photos.filter {
            selectedIds.contains(it.id)
        }
    }

    fun getSelectedCount(): Int =
        selectedIds.size

    private fun calculateDaysRemaining(
        trashedAt: Long
    ): String {

        val thirtyDays =
            30L * 24L * 60L * 60L * 1000L

        val elapsed =
            System.currentTimeMillis() - trashedAt

        val remaining =
            30L -
                    (elapsed / (24L * 60L * 60L * 1000L))

        return "${remaining.coerceAtLeast(0)} days"
    }
}