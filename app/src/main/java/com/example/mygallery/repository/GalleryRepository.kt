package com.example.mygallery.repository

import android.content.ContentUris
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.example.mygallery.model.GalleryFolder
import com.example.mygallery.model.ImageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GalleryRepository {


    private fun queryImages(
        context: Context,
        selection: String?,
        selectionArgs: Array<String>?
    ): ArrayList<ImageModel> {

        val imageList = ArrayList<ImageModel>()

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
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use { cur ->

            val idColumn =
                cur.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn =
                cur.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val folderColumn =
                cur.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateColumn =
                cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeColumn =
                cur.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeColumn =
                cur.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (cur.moveToNext()) {

                val id = cur.getLong(idColumn)
                val name = cur.getString(nameColumn)
                val folderName = cur.getString(folderColumn) ?: "Unknown"
                val dateAdded = cur.getLong(dateColumn)
                val size = cur.getLong(sizeColumn)
                val mimeType = cur.getString(mimeColumn) ?: ""

                val imageUri = ContentUris.withAppendedId(collection, id)

                imageList.add(
                    ImageModel(
                        id = id,
                        name = name,
                        uri = imageUri,
                        folderName = folderName,
                        dateAdded = dateAdded,
                        size = size,
                        mimeType = mimeType
                    )
                )
            }
        }

        return imageList
    }

    suspend fun getAllFolders(context: Context): ArrayList<GalleryFolder> {

        return withContext(Dispatchers.IO) {

            val allImages = queryImages(context, selection = null, selectionArgs = null)
            val folderList = ArrayList<GalleryFolder>()

            for (image in allImages) {

                var folder = folderList.find { f -> f.folderName == image.folderName }

                if (folder == null) {
                    folder = GalleryFolder(
                        folderName = image.folderName,
                        coverImage = image.uri,
                        imageList = arrayListOf()
                    )
                    folderList.add(folder)
                }

                folder.imageList.add(image)
            }

            folderList
        }
    }


    suspend fun getImages(context: Context, folderName: String?): ArrayList<ImageModel> {

        return withContext(Dispatchers.IO) {

            if (folderName == null) {
                queryImages(context, selection = null, selectionArgs = null)
            } else {
                // "?" is a safe placeholder — Android substitutes folderName
                // into it. Never build this by directly concatenating the
                // folder name into the string (SQL-injection risk + bad habit).
                val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(folderName)
                queryImages(context, selection, selectionArgs)
            }
        }
    }

    // Creating new Album
    fun createAlbum(context: Context, albumName: String): Result<String> {

        if (albumName.isBlank()) {
            return Result.failure(Exception("Album name cannot be empty"))
        }

        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        val myGalleryDir = File(picturesDir, "MyGallery")

        if (!myGalleryDir.exists()) {
            myGalleryDir.mkdirs()
        }

        val albumFolder = File(myGalleryDir, albumName)

        return if (albumFolder.exists()) {

            Result.failure(Exception("Album already exists"))

        } else {

            if (albumFolder.mkdirs()) {

                Result.success("Album created successfully")

            } else {

                Result.failure(Exception("Failed to create album"))

            }

        }
    }

    fun searchAlbums(
        albums: List<GalleryFolder>,
        query: String
    ): List<GalleryFolder> {

        if (query.isBlank()) return albums

        return albums.filter {
            it.folderName.contains(query, ignoreCase = true)
        }
    }
}