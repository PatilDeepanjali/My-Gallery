package com.example.mygallery.model

data class TrashItem(

    val id: Long,

    val name: String,

    val originalUri: String,

    val folderName: String,

    val dateAdded: Long,

    val size: Long,

    val mimeType: String,

    val trashFilePath: String,

    val trashedAt: Long
)