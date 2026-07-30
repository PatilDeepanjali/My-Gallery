package com.example.mygallery.model

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class ImageModel(

    val id: Long,

    val name: String,

    val uri: Uri,

    val folderName: String,

    val dateAdded: Long,

    val size: Long,

    val mimeType: String

): Parcelable
