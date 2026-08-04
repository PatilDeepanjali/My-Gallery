package com.example.mygallery.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mygallery.databinding.ItemPhotoGridBinding
import com.example.mygallery.databinding.ItemPreviewPhotoBinding
import com.example.mygallery.model.ImageModel

class PhotoPreviewAdapter(val imageList: List<ImageModel>): RecyclerView.Adapter<PhotoPreviewAdapter.PhotoPreviewViewHoder>(){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PhotoPreviewViewHoder {



        val binding= ItemPreviewPhotoBinding.inflate(LayoutInflater.from(parent.context),parent,false)


        return PhotoPreviewViewHoder(binding)

    }

    override fun onBindViewHolder(
        holder: PhotoPreviewViewHoder,
        position: Int
    ) {

        val image = imageList[position]

        Glide.with(holder.itemView.context)
            .load(image.uri)
            .into(holder.binding.imgPreview)



           }

    override fun getItemCount(): Int {
        return imageList.size
    }

    class PhotoPreviewViewHoder(val binding: ItemPreviewPhotoBinding):
        RecyclerView.ViewHolder(binding.root) {


    }
}