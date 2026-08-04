package com.example.mygallery.ui.photo

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.mygallery.R
import com.example.mygallery.adapter.PhotoPreviewAdapter
import com.example.mygallery.databinding.FragmentPhotoPreviewBinding
import com.example.mygallery.model.ImageModel
import com.example.mygallery.ui.MainActivity
import com.example.mygallery.utils.DateUtils



class PhotoPreviewFragment : Fragment() {
    private var isFavorite = false

    private lateinit var binding: FragmentPhotoPreviewBinding

    private lateinit var imageList: ArrayList<ImageModel>

    private var clickedPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageList = requireArguments().getParcelableArrayList(ARG_IMAGE_LIST)!!

        clickedPosition = requireArguments().getInt(ARG_POSITION)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


         binding = FragmentPhotoPreviewBinding.inflate(inflater,container,false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val currentImage = imageList[clickedPosition]

        binding.tvDate.text =
            DateUtils.formatDate(currentImage.dateAdded)

        binding.tvTime.text =
            DateUtils.formatTime(currentImage.dateAdded)



        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }


        binding.btnFavorite.setOnClickListener {

            isFavorite = !isFavorite

            if (isFavorite) {

                binding.btnFavorite.setImageResource(R.drawable.ic_red_heart)

            } else {

                binding.btnFavorite.setImageResource(R.drawable.ic_heart)

            }
        }

        binding.layoutEdit.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Edit coming soon",
                Toast.LENGTH_SHORT
            ).show()

        }

        binding.layoutShare.setOnClickListener {

            val intent = Intent(Intent.ACTION_SEND)

            intent.type = "image/*"

            intent.putExtra(
                Intent.EXTRA_STREAM,
                currentImage.uri
            )

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            startActivity(
                Intent.createChooser(intent, "Share Image")
            )

        }



        binding.layoutDelete.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Delete",
                Toast.LENGTH_SHORT
            ).show()
        }




        binding.layoutMore.setOnClickListener {

            PreviewActionPopup.show(
                requireContext(),
                binding.layoutMore
            ){ action ->

                when(action){

                    PreviewAction.COPY->{}

                    PreviewAction.MOVE->{}

                    PreviewAction.RENAME->{}

                    PreviewAction.OPEN_WITH->{}

                    PreviewAction.SLIDE_SHOW->{}

                    PreviewAction.WALLPAPER->{}

                    PreviewAction.DETAILS->{}

                }

            }

        }

        binding.viewPagerPhotos.adapter =
            PhotoPreviewAdapter(imageList)
        binding.viewPagerPhotos.setCurrentItem(clickedPosition, false)

        updatePhotoInfo(clickedPosition)

        binding.viewPagerPhotos.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {

                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    clickedPosition = position

                    updatePhotoInfo(position)
                }
            }
        )

    }


    override fun onResume() {
        super.onResume()

        (activity as MainActivity).hideBottomNavigation()
    }


    override fun onDestroyView() {
        super.onDestroyView()

        (activity as MainActivity).showBottomNavigation()
    }


    private fun updatePhotoInfo(position: Int) {

        val currentImage = imageList[position]

        binding.tvDate.text = DateUtils.formatDate(currentImage.dateAdded)
        binding.tvTime.text = DateUtils.formatTime(currentImage.dateAdded)
    }
    companion object {

        const val ARG_IMAGE_LIST = "image_list"

        const val ARG_POSITION = "position"
    }
}