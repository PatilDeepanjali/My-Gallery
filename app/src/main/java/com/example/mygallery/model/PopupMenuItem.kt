package com.example.mygallery.model


data class PopupMenuItem<T>(
    val icon: Int,
    val title: String,
    val action: T,
    val hasSubMenu: Boolean = false
)