package com.example.mygallery.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.example.mygallery.databinding.ItemPreviewPhotoBinding
import com.example.mygallery.model.ImageModel

class PhotoPreviewAdapter(
    private val imageList: List<ImageModel>
) : RecyclerView.Adapter<PhotoPreviewAdapter.PhotoPreviewViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PhotoPreviewViewHolder {

        val binding =
            ItemPreviewPhotoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return PhotoPreviewViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PhotoPreviewViewHolder,
        position: Int
    ) {

        val media = imageList[position]

        holder.releasePlayer()

        val isVideo =
            media.mimeType.startsWith(
                "video/",
                ignoreCase = true
            )

        if (isVideo) {

            holder.binding.imgPreview.visibility =
                View.GONE

            holder.playerView.visibility =
                View.VISIBLE

            holder.playVideo(media)

        } else {

            holder.playerView.visibility =
                View.GONE

            holder.binding.imgPreview.visibility =
                View.VISIBLE

            Glide.with(holder.itemView.context)
                .load(media.uri)
                .into(holder.binding.imgPreview)
        }
    }

    override fun onViewRecycled(
        holder: PhotoPreviewViewHolder
    ) {

        holder.releasePlayer()

        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int =
        imageList.size

    class PhotoPreviewViewHolder(
        val binding: ItemPreviewPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        val playerView: PlayerView

        private var player: ExoPlayer? = null

        init {

            /*
             * We create PlayerView programmatically so this adapter
             * does not require a new XML id.
             */

            playerView =
                PlayerView(itemView.context).apply {

                    layoutParams =
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )

                    useController = true

                    visibility = View.GONE
                }

            val root =
                binding.root

            if (root is ViewGroup) {

                root.addView(playerView)
            }
        }

        fun playVideo(
            media: ImageModel
        ) {

            val context =
                itemView.context

            player =
                ExoPlayer.Builder(
                    context
                ).build().also { exoPlayer ->

                    playerView.player =
                        exoPlayer

                    exoPlayer.setMediaItem(
                        MediaItem.fromUri(
                            media.uri
                        )
                    )

                    exoPlayer.prepare()

                    /*
                     * Do not autoplay.
                     * User sees the normal Media3 play button.
                     */

                    exoPlayer.playWhenReady =
                        false
                }
        }

        fun releasePlayer() {

            player?.release()

            player = null

            playerView.player = null
        }
    }
}