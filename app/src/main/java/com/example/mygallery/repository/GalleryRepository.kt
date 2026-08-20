package com.example.mygallery.repository

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.example.mygallery.model.DeleteResult
import com.example.mygallery.model.GalleryFolder
import com.example.mygallery.model.ImageModel
import com.example.mygallery.model.TrashItem
import com.example.mygallery.utils.TrashManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.IS_TRASHED,
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
                val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(folderName)
                queryImages(context, selection, selectionArgs)
            }
        }
    }


    suspend fun getTrashedImages(
        context: Context
    ): ArrayList<ImageModel> {

        return withContext(Dispatchers.IO) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return@withContext ArrayList()
            }

            val imageList = ArrayList<ImageModel>()

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.IS_TRASHED
            )

            val collection =
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val queryArgs = Bundle().apply {

                putInt(
                    MediaStore.QUERY_ARG_MATCH_TRASHED,
                    MediaStore.MATCH_ONLY
                )

                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    "${MediaStore.Images.Media.IS_TRASHED} = 1"
                )
            }

            context.contentResolver.query(
                collection,
                projection,
                queryArgs,
                null
            )?.use { cursor ->

                val idColumn =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Images.Media._ID
                    )

                val nameColumn =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Images.Media.DISPLAY_NAME
                    )

                val folderColumn =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                    )

                val dateColumn =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Images.Media.DATE_ADDED
                    )

                val sizeColumn =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Images.Media.SIZE
                    )

                val mimeColumn =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Images.Media.MIME_TYPE
                    )

                while (cursor.moveToNext()) {

                    val id =
                        cursor.getLong(idColumn)

                    val name =
                        cursor.getString(nameColumn)

                    val folderName =
                        cursor.getString(folderColumn)
                            ?: "Unknown"

                    val dateAdded =
                        cursor.getLong(dateColumn)

                    val size =
                        cursor.getLong(sizeColumn)

                    val mimeType =
                        cursor.getString(mimeColumn)
                            ?: ""

                    val imageUri =
                        ContentUris.withAppendedId(
                            collection,
                            id
                        )

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

            imageList
        }
    }


    suspend fun trashImages(
        context: Context,
        uris: List<Uri>
    ): Result<Int> {

        return withContext(Dispatchers.IO) {

            try {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    return@withContext Result.failure(
                        Exception(
                            "Trash is supported on Android 11 and above"
                        )
                    )
                }

                var trashedCount = 0

                for (uri in uris) {

                    val values = ContentValues().apply {
                        put(
                            MediaStore.Images.Media.IS_TRASHED,
                            1
                        )
                    }

                    val updated =
                        context.contentResolver.update(
                            uri,
                            values,
                            null,
                            null
                        )

                    if (updated > 0) {
                        trashedCount++
                    }
                }

                Result.success(trashedCount)

            } catch (e: Exception) {

                Result.failure(e)
            }
        }
    }


    // Creating new Album — deliberately does NOT touch the filesystem.
    fun createAlbum(context: Context, albumName: String): Result<String> {

        if (albumName.isBlank()) {
            return Result.failure(Exception("Album name cannot be empty"))
        }

        if (com.example.mygallery.utils.CustomAlbumPreferences.hasCustomAlbum(context, albumName)) {
            return Result.failure(Exception("Album already exists"))
        }

        com.example.mygallery.utils.CustomAlbumPreferences.addCustomAlbum(context, albumName)
        return Result.success("Album created successfully")
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

    /**
     * Merges in placeholder entries for custom albums (created via "+")
     * that don't have any real photos yet — these have no MediaStore
     * representation at all, so getAllFolders() never returns them on
     * its own. Shared by GalleryViewModel (main Album grid) and
     * AlbumPickerBottomSheet (Move/Copy picker) so both show the same
     * set of albums, not just whichever one remembers to merge.
     */
    fun mergeCustomAlbums(context: Context, realFolders: List<GalleryFolder>): List<GalleryFolder> {

        val realNames = realFolders.map { it.folderName }.toSet()

        val customOnlyNames = com.example.mygallery.utils.CustomAlbumPreferences
            .getCustomAlbumNames(context)
            .filter { it !in realNames }

        val placeholders = customOnlyNames.map { name ->
            GalleryFolder(
                folderName = name,
                coverImage = Uri.EMPTY,
                imageList = arrayListOf()
            )
        }

        return realFolders + placeholders
    }

    suspend fun deleteImages(context: Context, uris: List<Uri>): DeleteResult {

        return withContext(Dispatchers.IO) {

            when {

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    try {
                        val pendingIntent =
                            MediaStore.createDeleteRequest(context.contentResolver, uris)
                        DeleteResult.ConfirmDelete(pendingIntent.intentSender)
                    } catch (e: Exception) {
                        DeleteResult.Error(e.message ?: "Could not request delete")
                    }
                }

                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                    val remaining = uris.toMutableList()
                    try {
                        for (uri in uris) {
                            context.contentResolver.delete(uri, null, null)
                            remaining.remove(uri)
                        }
                        DeleteResult.Success
                    } catch (e: RecoverableSecurityException) {
                        DeleteResult.GrantPermissionThenRetry(
                            e.userAction.actionIntent.intentSender,
                            remaining
                        )
                    } catch (e: Exception) {
                        DeleteResult.Error(e.message ?: "Delete failed")
                    }
                }

                else -> {
                    try {
                        uris.forEach { uri -> context.contentResolver.delete(uri, null, null) }
                        DeleteResult.Success
                    } catch (e: Exception) {
                        DeleteResult.Error(e.message ?: "Delete failed")
                    }
                }
            }
        }
    }

    suspend fun moveImageToTrash(
        context: Context,
        image: ImageModel
    ): Result<TrashItem> {

        return TrashManager.moveToTrash(
            context,
            image
        )
    }

    suspend fun moveImagesToTrash(
        context: Context,
        images: List<ImageModel>
    ): Result<Int> {

        return try {

            var movedCount = 0

            for (image in images) {

                val result =
                    moveImageToTrash(
                        context,
                        image
                    )

                if (result.isSuccess) {
                    movedCount++
                } else {
                    return Result.failure(
                        result.exceptionOrNull()
                            ?: Exception("Unable to move image to Trash")
                    )
                }
            }

            Result.success(movedCount)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }


    suspend fun copyImages(
        context: Context,
        uris: List<Uri>,
        destinationAlbumName: String
    ): Result<Int> {

        return withContext(Dispatchers.IO) {

            try {
                var copiedCount = 0

                for (sourceUri in uris) {

                    val displayName = queryDisplayName(context, sourceUri)
                        ?: "IMG_${System.currentTimeMillis()}.jpg"
                    val mimeType = context.contentResolver.getType(sourceUri) ?: "image/jpeg"

                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "Pictures/MyGallery/$destinationAlbumName"
                        )
                    }

                    val newUri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    ) ?: continue

                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        context.contentResolver.openOutputStream(newUri)?.use { output ->
                            input.copyTo(output)
                        }
                    }

                    copiedCount++
                }

                Result.success(copiedCount)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                return cursor.getString(nameColumn)
            }
        }
        return null
    }
}