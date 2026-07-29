package com.example.mygallery.model

import android.net.Uri

data class GalleryFolder(

    val folderName: String,

    var coverImage: Uri,

    val imageList: ArrayList<ImageModel>

) {

    val imageCount: Int
        get() = imageList.size

}