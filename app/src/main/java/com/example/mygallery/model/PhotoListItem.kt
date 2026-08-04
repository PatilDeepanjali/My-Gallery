package com.example.mygallery.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class PhotoListItem {
    data class DateHeader(val label: String) : PhotoListItem()
    @Parcelize
    data class Photo(val image: ImageModel) : PhotoListItem(), Parcelable
}