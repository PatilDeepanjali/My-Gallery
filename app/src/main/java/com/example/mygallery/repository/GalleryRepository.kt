package com.example.mygallery.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.mygallery.model.GalleryFolder
import com.example.mygallery.model.ImageModel

class GalleryRepository {

    fun getAllFolders(context: Context): ArrayList<GalleryFolder> {

        val folderList = ArrayList<GalleryFolder>()

        val projection = arrayOf(

            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE

        )

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val cursor = context.contentResolver.query(

            collection,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"

        )

        cursor?.use {

            val idColumn =
                it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            val nameColumn =
                it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

            val folderColumn =
                it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            val dateColumn =
                it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            val sizeColumn =
                it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            val mimeColumn =
                it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (it.moveToNext()) {

                val id = it.getLong(idColumn)

                val name = it.getString(nameColumn)

                val folderName = it.getString(folderColumn) ?: "Unknown"

                val dateAdded = it.getLong(dateColumn)

                val size = it.getLong(sizeColumn)

                val mimeType = it.getString(mimeColumn) ?: ""

                val imageUri = ContentUris.withAppendedId(
                    collection,
                    id
                )

                val image = ImageModel(

                    id = id,

                    name = name,

                    uri = imageUri,

                    folderName = folderName,

                    dateAdded = dateAdded,

                    size = size,

                    mimeType = mimeType

                )

                var folder = folderList.find {

                    it.folderName == folderName

                }

                if (folder == null) {

                    folder = GalleryFolder(

                        folderName = folderName,

                        coverImage = imageUri,

                        imageList = arrayListOf()

                    )

                    folderList.add(folder)

                }

                folder.imageList.add(image)

            }

        }

        return folderList

    }

}