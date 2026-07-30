package com.example.mygallery.model

sealed class PhotoListItem {
    data class DateHeader(val label: String) : PhotoListItem()
    data class Photo(val image: ImageModel) : PhotoListItem()
}