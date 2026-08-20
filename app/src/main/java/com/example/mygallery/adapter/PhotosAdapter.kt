package com.example.mygallery.adapter

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mygallery.R
import com.example.mygallery.databinding.ItemDateHeaderBinding
import com.example.mygallery.databinding.ItemPhotoGridBinding
import com.example.mygallery.databinding.ItemPhotoListBinding
import com.example.mygallery.model.PhotoListItem
import com.example.mygallery.utils.FavoritePreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotosAdapter(
    private var isGridView: Boolean,
    private val items: List<PhotoListItem>,
    private val onPhotoClick: (PhotoListItem.Photo, Int) -> Unit,
    private val onPhotoLongClick: (PhotoListItem.Photo) -> Unit,
    private val onPhotoToggleSelect: (PhotoListItem.Photo) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PHOTO_GRID = 1
        private const val TYPE_PHOTO_LIST = 2
    }

    private var isSelectionMode = false
    private var selectedPhotoIds: Set<Long> = emptySet()

    fun setSelectionState(
        active: Boolean,
        selected: Set<Long>
    ) {
        isSelectionMode = active
        selectedPhotoIds = selected
        notifyDataSetChanged()
    }

    fun getSpanSize(position: Int, totalSpanCount: Int): Int {
        return if (items[position] is PhotoListItem.DateHeader) {
            totalSpanCount
        } else {
            1
        }
    }

    private fun getPhotoIndex(adapterPosition: Int): Int {

        var photoIndex = -1

        for (i in 0..adapterPosition) {
            if (items[i] is PhotoListItem.Photo) {
                photoIndex++
            }
        }

        return photoIndex
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            items[position] is PhotoListItem.DateHeader ->
                TYPE_HEADER

            isGridView ->
                TYPE_PHOTO_GRID

            else ->
                TYPE_PHOTO_LIST
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {

            TYPE_HEADER -> {
                val binding =
                    ItemDateHeaderBinding.inflate(
                        inflater,
                        parent,
                        false
                    )

                HeaderViewHolder(binding)
            }

            TYPE_PHOTO_GRID -> {
                val binding =
                    ItemPhotoGridBinding.inflate(
                        inflater,
                        parent,
                        false
                    )

                GridViewHolder(binding)
            }

            else -> {
                val binding =
                    ItemPhotoListBinding.inflate(
                        inflater,
                        parent,
                        false
                    )

                ListViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        when (holder) {

            // -------------------------------------------------
            // DATE HEADER
            // -------------------------------------------------

            is HeaderViewHolder -> {

                val header =
                    items[position] as PhotoListItem.DateHeader

                holder.binding.root.text = header.label
            }

            // -------------------------------------------------
            // GRID PHOTO
            // -------------------------------------------------

            is GridViewHolder -> {

                val photo =
                    items[position] as PhotoListItem.Photo

                val isSelected =
                    selectedPhotoIds.contains(photo.image.id)

                // Load photo
                Glide.with(holder.itemView.context)
                    .load(photo.image.uri)
                    .centerCrop()
                    .into(holder.binding.imgPhoto)

                // Show selected/unselected checkbox
                bindCheckbox(
                    holder.binding.ivCheckbox,
                    isSelected
                )

                // Favorite heart — independent of selection mode
                bindFavorite(holder.binding.imgFavorite, photo)

                // Normal click / selection click
                holder.itemView.setOnClickListener {

                    if (isSelectionMode) {

                        onPhotoToggleSelect(photo)

                    } else {

                        val currentPosition =
                            holder.bindingAdapterPosition

                        if (currentPosition != RecyclerView.NO_POSITION) {

                            onPhotoClick(
                                photo,
                                getPhotoIndex(currentPosition)
                            )
                        }
                    }
                }

                // Long press
                holder.itemView.setOnLongClickListener {

                    if (!isSelectionMode) {

                        onPhotoLongClick(photo)

                    } else {

                        onPhotoToggleSelect(photo)
                    }

                    // IMPORTANT: consume long click
                    true
                }
            }

            // -------------------------------------------------
            // LIST PHOTO
            // -------------------------------------------------

            is ListViewHolder -> {

                val photo =
                    items[position] as PhotoListItem.Photo

                val isSelected =
                    selectedPhotoIds.contains(photo.image.id)

                // Photo name
                holder.binding.tvPhotoName.text =
                    photo.image.name

                // Date
                val formattedDate =
                    SimpleDateFormat(
                        "MMMM dd, yyyy",
                        Locale.getDefault()
                    ).format(
                        Date(photo.image.dateAdded * 1000)
                    )

                // File size
                val formattedSize =
                    Formatter.formatShortFileSize(
                        holder.itemView.context,
                        photo.image.size
                    )

                holder.binding.tvPhotoMeta.text =
                    "$formattedDate • $formattedSize"

                // Load photo
                Glide.with(holder.itemView.context)
                    .load(photo.image.uri)
                    .centerCrop()
                    .into(holder.binding.imgPhoto)

                // Show selected/unselected checkbox
                bindCheckbox(
                    holder.binding.ivCheckbox,
                    isSelected
                )

                // Favorite heart — independent of selection mode
                bindFavorite(holder.binding.imgFavorite, photo)

                // Normal click / selection click
                holder.itemView.setOnClickListener {

                    if (isSelectionMode) {

                        onPhotoToggleSelect(photo)

                    } else {

                        val currentPosition =
                            holder.bindingAdapterPosition

                        if (currentPosition != RecyclerView.NO_POSITION) {

                            onPhotoClick(
                                photo,
                                getPhotoIndex(currentPosition)
                            )
                        }
                    }
                }

                // Long press
                holder.itemView.setOnLongClickListener {

                    if (!isSelectionMode) {

                        onPhotoLongClick(photo)

                    } else {

                        onPhotoToggleSelect(photo)
                    }

                    // IMPORTANT: consume long click
                    true
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    // ---------------------------------------------------------
    // CHECKBOX
    // ---------------------------------------------------------

    private fun bindCheckbox(
        checkbox: android.widget.ImageView,
        isSelected: Boolean
    ) {

        checkbox.visibility =
            if (isSelectionMode) {
                View.VISIBLE
            } else {
                View.GONE
            }

        checkbox.setImageResource(
            if (isSelected) {
                R.drawable.ic_check_circle_filled
            } else {
                R.drawable.ic_check_circle_outline
            }
        )
    }

    // ---------------------------------------------------------
    // FAVORITE
    // ---------------------------------------------------------

    /**
     * Binds the heart icon AND its own independent click listener.
     * Tapping the heart toggles the persisted favorite flag directly
     * and updates the icon immediately — this does NOT go through
     * onPhotoClick/selection at all, so favoriting a photo works the
     * same whether or not selection mode is active.
     */
    private fun bindFavorite(
        favoriteIcon: android.widget.ImageView,
        photo: PhotoListItem.Photo
    ) {
        val context = favoriteIcon.context
        val photoId = photo.image.id

        fun renderIcon() {
            val isFavorite =
                FavoritePreferences.isFavorite(context, photoId)

            favoriteIcon.setImageResource(
                if (isFavorite)
                    R.drawable.ic_red_heart
                else
                    R.drawable.ic_heart
            )
        }

        renderIcon()

        favoriteIcon.setOnClickListener {
            FavoritePreferences.toggleFavorite(context, photoId)
            renderIcon()
        }
    }

    // ---------------------------------------------------------
    // GRID / LIST MODE
    // ---------------------------------------------------------

    fun setViewMode(isGrid: Boolean) {

        isGridView = isGrid

        notifyDataSetChanged()
    }

    // ---------------------------------------------------------
    // VIEW HOLDERS
    // ---------------------------------------------------------

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