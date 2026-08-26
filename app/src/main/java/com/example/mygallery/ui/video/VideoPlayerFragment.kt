package com.example.mygallery.ui.video

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.mygallery.R
import com.example.mygallery.databinding.FragmentVideoPlayerBinding

class VideoPlayerFragment :
    Fragment(R.layout.fragment_video_player) {

    companion object {

        const val ARG_VIDEO_URI =
            "video_uri"
    }


    private var _binding:
            FragmentVideoPlayerBinding? =
        null

    private val binding
        get() = _binding!!


    private var player:
            ExoPlayer? = null


    // =========================================================
    // View Created
    // =========================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )


        _binding =
            FragmentVideoPlayerBinding
                .bind(view)


        binding.btnBack.setOnClickListener {

            parentFragmentManager
                .popBackStack()
        }


        initializePlayer()
    }


    // =========================================================
    // Initialize Player
    // =========================================================

    private fun initializePlayer() {

        val uriString =
            arguments?.getString(
                ARG_VIDEO_URI
            )


        if (
            uriString.isNullOrBlank()
        ) {

            parentFragmentManager
                .popBackStack()

            return
        }


        val videoUri =
            Uri.parse(
                uriString
            )


        player =
            ExoPlayer.Builder(
                requireContext()
            ).build()


        binding.playerView.player =
            player


        val mediaItem =
            MediaItem.fromUri(
                videoUri
            )


        player?.apply {

            setMediaItem(
                mediaItem
            )

            prepare()

            playWhenReady =
                true
        }
    }


    // =========================================================
    // Pause when Fragment is not visible
    // =========================================================

    override fun onPause() {

        super.onPause()

        player?.pause()
    }


    // =========================================================
    // Resume playback
    // =========================================================

    override fun onResume() {

        super.onResume()

        if (
            player != null
        ) {

            player?.play()
        }
    }


    // =========================================================
    // Release player
    // =========================================================

    private fun releasePlayer() {

        binding.playerView.player =
            null


        player?.release()

        player =
            null
    }


    override fun onDestroyView() {

        releasePlayer()

        _binding =
            null

        super.onDestroyView()
    }
}