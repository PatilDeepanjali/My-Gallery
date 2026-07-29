package com.example.mygallery.model

import android.net.Uri

data class ImageModel(

    val id: Long,

    val name: String,

    val uri: Uri,

    val folderName: String,

    val dateAdded: Long,

    val size: Long,

    val mimeType: String

)