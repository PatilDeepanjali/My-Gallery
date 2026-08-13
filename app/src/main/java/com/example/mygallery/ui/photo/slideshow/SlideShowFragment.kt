package com.example.mygallery.ui.photo.slideshow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.mygallery.R
import com.example.mygallery.adapter.PhotoPreviewAdapter
import com.example.mygallery.databinding.FragmentSlideShowBinding
import com.example.mygallery.model.ImageModel
import com.example.mygallery.ui.MainActivity
import com.example.mygallery.utils.DateUtils

class SlideShowFragment : Fragment() {

    private var _binding: FragmentSlideShowBinding? = null
    private val binding get() = _binding!!

    private lateinit var imageList: ArrayList<ImageModel>

    private var currentPosition = 0

    private var isPlaying = true

    // Time for each image
    private val slideDuration = 3000L

    private val handler = Handler(Looper.getMainLooper())

    private val slideRunnable = object : Runnable {

        override fun run() {

            if (!isPlaying || imageList.isEmpty()) {
                return
            }

            val nextPosition =
                if (currentPosition < imageList.lastIndex) {
                    currentPosition + 1
                } else {
                    0
                }

            binding.viewPagerSlideshow.setCurrentItem(
                nextPosition,
                true
            )

            handler.postDelayed(
                this,
                slideDuration
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageList =
            requireArguments()
                .getParcelableArrayList(ARG_IMAGE_LIST)
                ?: arrayListOf()

        currentPosition =
            requireArguments()
                .getInt(ARG_POSITION, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentSlideShowBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        if (imageList.isEmpty()) {
            parentFragmentManager.popBackStack()
            return
        }

        setupViewPager()
        setupControls()
        setupThumbnails()

        updatePosition()

        startSlideShow()
    }

    // ---------------------------------------------------------
    // ViewPager
    // ---------------------------------------------------------

    private fun setupViewPager() {

        binding.viewPagerSlideshow.adapter =
            PhotoPreviewAdapter(imageList)

        binding.viewPagerSlideshow.setCurrentItem(
            currentPosition,
            false
        )

        binding.viewPagerSlideshow.registerOnPageChangeCallback(
            pageChangeCallback
        )
    }

    private val pageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                currentPosition = position

                updatePosition()
                updateThumbnails()

                // Restart timer whenever the current image changes
                if (isPlaying) {
                    restartSlideShow()
                }
            }
        }


    // ---------------------------------------------------------
    // Controls
    // ---------------------------------------------------------

    private fun setupControls() {

        // Back
        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }


        // Play / Pause
        binding.btnPlayPause.setOnClickListener {

            if (isPlaying) {
                pauseSlideShow()
            } else {
                startSlideShow()
            }
        }


        // Volume
        binding.btnVolume.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Volume control coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // ---------------------------------------------------------
    // Thumbnails
    // ---------------------------------------------------------

    private fun setupThumbnails() {

        binding.thumbnailContainer.removeAllViews()

        imageList.forEachIndexed { index, image ->

            val imageView = ImageView(requireContext())

            val size =
                resources.getDimensionPixelSize(
                    R.dimen.slideshow_thumbnail_size
                )

            val margin =
                resources.getDimensionPixelSize(
                    R.dimen.slideshow_thumbnail_margin
                )

            val params =
                LinearLayout.LayoutParams(
                    size,
                    size
                )

            params.setMargins(
                margin,
                0,
                margin,
                0
            )

            imageView.layoutParams = params

            imageView.scaleType =
                ImageView.ScaleType.CENTER_CROP

            Glide.with(this)
                .load(image.uri)
                .centerCrop()
                .into(imageView)

            imageView.setOnClickListener {

                binding.viewPagerSlideshow.setCurrentItem(
                    index,
                    true
                )
            }

            binding.thumbnailContainer.addView(
                imageView
            )
        }

        updateThumbnails()
    }


    private fun updateThumbnails() {

        for (i in 0 until binding.thumbnailContainer.childCount) {

            val imageView =
                binding.thumbnailContainer.getChildAt(i)
                        as ImageView

            imageView.alpha =
                if (i == currentPosition) {
                    1f
                } else {
                    0.55f
                }
        }
    }


    // ---------------------------------------------------------
    // Position / Date / Timer
    // ---------------------------------------------------------

    private fun updatePosition() {

        binding.tvPosition.text =
            "${currentPosition + 1} / ${imageList.size}"

        val currentImage =
            imageList[currentPosition]

        binding.tvDate.text =
            DateUtils.formatDate(
                currentImage.dateAdded
            )

        // Static slideshow timer text for now.
        // Later this can become a real media/video timer.
        binding.tvTimer.text =
            "00:00 / 00:03"
    }


    // ---------------------------------------------------------
    // Slideshow control
    // ---------------------------------------------------------

    private fun startSlideShow() {

        isPlaying = true

        binding.btnPlayPause.setImageResource(
            R.drawable.ic_pause
        )

        handler.removeCallbacks(
            slideRunnable
        )

        handler.postDelayed(
            slideRunnable,
            slideDuration
        )
    }


    private fun pauseSlideShow() {

        isPlaying = false

        binding.btnPlayPause.setImageResource(
            R.drawable.ic_play_circle
        )

        handler.removeCallbacks(
            slideRunnable
        )
    }


    private fun restartSlideShow() {

        handler.removeCallbacks(
            slideRunnable
        )

        if (isPlaying) {

            handler.postDelayed(
                slideRunnable,
                slideDuration
            )
        }
    }


    // ---------------------------------------------------------
    // Activity / Lifecycle
    // ---------------------------------------------------------

    override fun onResume() {
        super.onResume()

        (activity as? MainActivity)
            ?.hideBottomNavigation()
    }


    override fun onPause() {
        super.onPause()

        handler.removeCallbacks(
            slideRunnable
        )
    }


    override fun onDestroyView() {

        handler.removeCallbacks(
            slideRunnable
        )

        binding.viewPagerSlideshow.unregisterOnPageChangeCallback(
            pageChangeCallback
        )

        super.onDestroyView()

        _binding = null
    }


    // ---------------------------------------------------------
    // Arguments
    // ---------------------------------------------------------

    companion object {

        private const val ARG_IMAGE_LIST =
            "slideshow_image_list"

        private const val ARG_POSITION =
            "slideshow_position"

        fun newInstance(
            images: ArrayList<ImageModel>,
            position: Int = 0
        ): SlideShowFragment {

            return SlideShowFragment().apply {

                arguments = Bundle().apply {

                    putParcelableArrayList(
                        ARG_IMAGE_LIST,
                        images
                    )

                    putInt(
                        ARG_POSITION,
                        position
                    )
                }
            }
        }
    }
}