package com.example.mygallery.repository

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.mygallery.model.DeleteResult
import com.example.mygallery.model.GalleryFolder
import com.example.mygallery.model.ImageModel
import com.example.mygallery.model.TrashItem
import com.example.mygallery.utils.CustomAlbumPreferences
import com.example.mygallery.utils.TrashManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GalleryRepository {

    // =========================================================
    // MEDIA COLLECTIONS
    // =========================================================

    private val imageCollection =
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    private val videoCollection =
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI


    // =========================================================
    // COMMON MEDIA COLUMNS
    // =========================================================

    private val idColumn =
        MediaStore.MediaColumns._ID

    private val displayNameColumn =
        MediaStore.MediaColumns.DISPLAY_NAME

    private val dateAddedColumn =
        MediaStore.MediaColumns.DATE_ADDED

    private val sizeColumn =
        MediaStore.MediaColumns.SIZE

    private val mimeTypeColumn =
        MediaStore.MediaColumns.MIME_TYPE


    // =========================================================
    // GET ALL MEDIA
    // =========================================================

    private fun queryAllMedia(
        context: Context,
        folderName: String? = null
    ): ArrayList<ImageModel> {

        val result =
            ArrayList<ImageModel>()

        queryImageMedia(
            context = context,
            folderName = folderName,
            result = result
        )

        queryVideoMedia(
            context = context,
            folderName = folderName,
            result = result
        )

        result.sortByDescending {
            it.dateAdded
        }

        return result
    }


    // =========================================================
    // QUERY IMAGES
    // =========================================================

    private fun queryImageMedia(
        context: Context,
        folderName: String?,
        result: MutableList<ImageModel>
    ) {

        val projection =
            arrayOf(
                idColumn,
                displayNameColumn,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                dateAddedColumn,
                sizeColumn,
                mimeTypeColumn
            )

        val selection =
            if (folderName == null) {
                null
            } else {
                "${MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME} = ?"
            }

        val selectionArgs =
            if (folderName == null) {
                null
            } else {
                arrayOf(folderName)
            }

        context.contentResolver.query(
            imageCollection,
            projection,
            selection,
            selectionArgs,
            "$dateAddedColumn DESC"
        )?.use { cursor ->

            val idIndex =
                cursor.getColumnIndexOrThrow(idColumn)

            val nameIndex =
                cursor.getColumnIndexOrThrow(displayNameColumn)

            val folderIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME
                )

            val dateIndex =
                cursor.getColumnIndexOrThrow(dateAddedColumn)

            val sizeIndex =
                cursor.getColumnIndexOrThrow(sizeColumn)

            val mimeIndex =
                cursor.getColumnIndexOrThrow(mimeTypeColumn)

            while (cursor.moveToNext()) {

                val id =
                    cursor.getLong(idIndex)

                val name =
                    cursor.getString(nameIndex)

                val folder =
                    cursor.getString(folderIndex)
                        ?: "Unknown"

                val dateAdded =
                    cursor.getLong(dateIndex)

                val size =
                    cursor.getLong(sizeIndex)

                val mimeType =
                    cursor.getString(mimeIndex)
                        ?: "image/*"

                val uri =
                    Uri.withAppendedPath(
                        imageCollection,
                        id.toString()
                    )

                result.add(
                    ImageModel(
                        id = id,
                        name = name,
                        uri = uri,
                        folderName = folder,
                        dateAdded = dateAdded,
                        size = size,
                        mimeType = mimeType
                    )
                )
            }
        }
    }


    // =========================================================
    // QUERY VIDEOS
    // =========================================================

    private fun queryVideoMedia(
        context: Context,
        folderName: String?,
        result: MutableList<ImageModel>
    ) {

        val projection =
            arrayOf(
                idColumn,
                displayNameColumn,
                MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME,
                dateAddedColumn,
                sizeColumn,
                mimeTypeColumn
            )

        val selection =
            if (folderName == null) {
                null
            } else {
                "${MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME} = ?"
            }

        val selectionArgs =
            if (folderName == null) {
                null
            } else {
                arrayOf(folderName)
            }

        context.contentResolver.query(
            videoCollection,
            projection,
            selection,
            selectionArgs,
            "$dateAddedColumn DESC"
        )?.use { cursor ->

            val idIndex =
                cursor.getColumnIndexOrThrow(idColumn)

            val nameIndex =
                cursor.getColumnIndexOrThrow(displayNameColumn)

            val folderIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME
                )

            val dateIndex =
                cursor.getColumnIndexOrThrow(dateAddedColumn)

            val sizeIndex =
                cursor.getColumnIndexOrThrow(sizeColumn)

            val mimeIndex =
                cursor.getColumnIndexOrThrow(mimeTypeColumn)

            while (cursor.moveToNext()) {

                val id =
                    cursor.getLong(idIndex)

                val name =
                    cursor.getString(nameIndex)

                val folder =
                    cursor.getString(folderIndex)
                        ?: "Unknown"

                val dateAdded =
                    cursor.getLong(dateIndex)

                val size =
                    cursor.getLong(sizeIndex)

                val mimeType =
                    cursor.getString(mimeIndex)
                        ?: "video/*"

                val uri =
                    Uri.withAppendedPath(
                        videoCollection,
                        id.toString()
                    )

                result.add(
                    ImageModel(
                        id = id,
                        name = name,
                        uri = uri,
                        folderName = folder,
                        dateAdded = dateAdded,
                        size = size,
                        mimeType = mimeType
                    )
                )
            }
        }
    }


    // =========================================================
    // GET ALL FOLDERS
    // =========================================================

    suspend fun getAllFolders(
        context: Context
    ): ArrayList<GalleryFolder> {

        return withContext(Dispatchers.IO) {

            val allMedia =
                queryAllMedia(context)

            val folders =
                ArrayList<GalleryFolder>()

            for (media in allMedia) {

                var folder =
                    folders.find {
                        it.folderName == media.folderName
                    }

                if (folder == null) {

                    folder =
                        GalleryFolder(
                            folderName = media.folderName,
                            coverImage = media.uri,
                            imageList = arrayListOf()
                        )

                    folders.add(folder)
                }

                folder.imageList.add(media)
            }

            folders
        }
    }


    // =========================================================
    // GET IMAGES + VIDEOS
    // =========================================================

    suspend fun getImages(
        context: Context,
        folderName: String?
    ): ArrayList<ImageModel> {

        return withContext(Dispatchers.IO) {

            queryAllMedia(
                context,
                folderName
            )
        }
    }


    // =========================================================
    // CREATE ALBUM
    // =========================================================

    fun createAlbum(
        context: Context,
        albumName: String
    ): Result<String> {

        if (albumName.isBlank()) {

            return Result.failure(
                Exception(
                    "Album name cannot be empty"
                )
            )
        }

        if (
            CustomAlbumPreferences.hasCustomAlbum(
                context,
                albumName
            )
        ) {

            return Result.failure(
                Exception(
                    "Album already exists"
                )
            )
        }

        CustomAlbumPreferences.addCustomAlbum(
            context,
            albumName
        )

        return Result.success(
            "Album created successfully"
        )
    }


    // =========================================================
    // SEARCH ALBUMS
    // =========================================================

    fun searchAlbums(
        albums: List<GalleryFolder>,
        query: String
    ): List<GalleryFolder> {

        if (query.isBlank()) {
            return albums
        }

        return albums.filter {

            it.folderName.contains(
                query,
                ignoreCase = true
            )
        }
    }


    // =========================================================
    // MERGE CUSTOM ALBUMS
    // =========================================================

    fun mergeCustomAlbums(
        context: Context,
        realFolders: List<GalleryFolder>
    ): List<GalleryFolder> {

        val realNames =
            realFolders
                .map {
                    it.folderName
                }
                .toSet()

        val customOnlyNames =
            CustomAlbumPreferences
                .getCustomAlbumNames(context)
                .filter {
                    it !in realNames
                }

        val placeholders =
            customOnlyNames.map { name ->

                GalleryFolder(
                    folderName = name,
                    coverImage = Uri.EMPTY,
                    imageList = arrayListOf()
                )
            }

        return realFolders + placeholders
    }


    // =========================================================
    // DELETE MEDIA
    // =========================================================

    suspend fun deleteImages(
        context: Context,
        uris: List<Uri>
    ): DeleteResult {

        return withContext(Dispatchers.IO) {

            if (uris.isEmpty()) {
                return@withContext DeleteResult.Success
            }

            when {

                // -------------------------------------------------
                // Android 11+
                // -------------------------------------------------

                Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.R -> {

                    try {

                        val pendingIntent =
                            MediaStore.createDeleteRequest(
                                context.contentResolver,
                                uris
                            )

                        DeleteResult.ConfirmDelete(
                            pendingIntent.intentSender
                        )

                    } catch (e: Exception) {

                        DeleteResult.Error(
                            e.message
                                ?: "Could not request delete"
                        )
                    }
                }


                // -------------------------------------------------
                // Android 10
                // -------------------------------------------------

                Build.VERSION.SDK_INT ==
                        Build.VERSION_CODES.Q -> {

                    val remaining =
                        uris.toMutableList()

                    try {

                        for (uri in uris) {

                            context.contentResolver.delete(
                                uri,
                                null,
                                null
                            )

                            remaining.remove(uri)
                        }

                        DeleteResult.Success

                    } catch (
                        e: RecoverableSecurityException
                    ) {

                        DeleteResult.GrantPermissionThenRetry(
                            e.userAction
                                .actionIntent
                                .intentSender,
                            remaining
                        )

                    } catch (e: Exception) {

                        DeleteResult.Error(
                            e.message
                                ?: "Delete failed"
                        )
                    }
                }


                // -------------------------------------------------
                // Android 9 and below
                // -------------------------------------------------

                else -> {

                    try {

                        uris.forEach { uri ->

                            context.contentResolver.delete(
                                uri,
                                null,
                                null
                            )
                        }

                        DeleteResult.Success

                    } catch (e: Exception) {

                        DeleteResult.Error(
                            e.message
                                ?: "Delete failed"
                        )
                    }
                }
            }
        }
    }


    // =========================================================
    // MOVE IMAGE / VIDEO TO CUSTOM TRASH
    // =========================================================

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

        return withContext(Dispatchers.IO) {

            try {

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

                        return@withContext Result.failure(
                            result.exceptionOrNull()
                                ?: Exception(
                                    "Unable to move media to Trash"
                                )
                        )
                    }
                }

                Result.success(movedCount)

            } catch (e: Exception) {

                Result.failure(e)
            }
        }
    }


    // =========================================================
    // COPY IMAGE / VIDEO
    // =========================================================

    suspend fun copyImages(
        context: Context,
        uris: List<Uri>,
        destinationAlbumName: String
    ): Result<Int> {

        val safeAlbumName =
            sanitizeAlbumName(
                destinationAlbumName
            )

        return withContext(Dispatchers.IO) {

            try {

                if (
                    destinationAlbumName.isBlank()
                ) {

                    return@withContext Result.failure(
                        Exception(
                            "Destination album cannot be empty"
                        )
                    )
                }

                if (uris.isEmpty()) {

                    return@withContext Result.success(
                        0
                    )
                }

                val resolver =
                    context.contentResolver

                var copiedCount = 0

                for (sourceUri in uris) {

                    // -------------------------------------------------
                    // Get source information
                    // -------------------------------------------------

                    val displayName =
                        queryDisplayName(
                            context,
                            sourceUri
                        ) ?: "MEDIA_${System.currentTimeMillis()}"

                    val mimeType =
                        resolver.getType(
                            sourceUri
                        ) ?: "application/octet-stream"

                    val isVideo =
                        mimeType.startsWith(
                            "video/",
                            ignoreCase = true
                        )


                    // -------------------------------------------------
                    // Choose MediaStore collection
                    // -------------------------------------------------

                    val destinationCollection =
                        if (isVideo) {
                            videoCollection
                        } else {
                            imageCollection
                        }


                    // -------------------------------------------------
                    // Build destination path
                    // -------------------------------------------------

                    val relativePath =
                        if (isVideo) {

                            "${Environment.DIRECTORY_MOVIES}/" +
                                    "MyGallery/" +
                                    safeAlbumName

                        } else {

                            "${Environment.DIRECTORY_PICTURES}/" +
                                    "MyGallery/" +
                                    safeAlbumName
                        }


                    // -------------------------------------------------
                    // ContentValues
                    // -------------------------------------------------

                    val values =
                        ContentValues().apply {

                            put(
                                MediaStore.MediaColumns.DISPLAY_NAME,
                                displayName
                            )

                            put(
                                MediaStore.MediaColumns.MIME_TYPE,
                                mimeType
                            )


                            if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.Q
                            ) {

                                put(
                                    MediaStore.MediaColumns.RELATIVE_PATH,
                                    relativePath
                                )

                                put(
                                    MediaStore.MediaColumns.IS_PENDING,
                                    1
                                )

                            } else {

                                /*
                                 * Android 9 and below.
                                 *
                                 * DATA is used because
                                 * RELATIVE_PATH does not exist.
                                 */

                                val baseDirectory =
                                    if (isVideo) {

                                        Environment
                                            .getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_MOVIES
                                            )

                                    } else {

                                        Environment
                                            .getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_PICTURES
                                            )
                                    }


                                val galleryDirectory =
                                    File(
                                        baseDirectory,
                                        "MyGallery/$safeAlbumName"
                                    )


                                if (
                                    !galleryDirectory.exists()
                                ) {

                                    galleryDirectory.mkdirs()
                                }


                                val destinationFile =
                                    File(
                                        galleryDirectory,
                                        displayName
                                    )


                                put(
                                    MediaStore.MediaColumns.DATA,
                                    destinationFile.absolutePath
                                )
                            }
                        }


                    // -------------------------------------------------
                    // Insert destination media
                    // -------------------------------------------------

                    val newUri =
                        resolver.insert(
                            destinationCollection,
                            values
                        )


                    if (newUri == null) {
                        continue
                    }


                    try {

                        // -------------------------------------------------
                        // Copy bytes
                        // -------------------------------------------------

                        val copied =
                            resolver
                                .openInputStream(
                                    sourceUri
                                )
                                ?.use { input ->

                                    resolver
                                        .openOutputStream(
                                            newUri
                                        )
                                        ?.use { output ->

                                            input.copyTo(
                                                output
                                            )

                                            true
                                        }
                                } ?: false


                        if (!copied) {

                            resolver.delete(
                                newUri,
                                null,
                                null
                            )

                            continue
                        }


                        // -------------------------------------------------
                        // Android 10+
                        // Make media visible
                        // -------------------------------------------------

                        if (
                            Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.Q
                        ) {

                            val completeValues =
                                ContentValues().apply {

                                    put(
                                        MediaStore.MediaColumns.IS_PENDING,
                                        0
                                    )
                                }

                            resolver.update(
                                newUri,
                                completeValues,
                                null,
                                null
                            )
                        }


                        copiedCount++

                    } catch (e: Exception) {

                        try {

                            resolver.delete(
                                newUri,
                                null,
                                null
                            )

                        } catch (_: Exception) {
                        }
                    }
                }


                Result.success(
                    copiedCount
                )

            } catch (e: Exception) {

                Result.failure(e)
            }
        }
    }


    // =========================================================
    // GET DISPLAY NAME
    // =========================================================

    private fun queryDisplayName(
        context: Context,
        uri: Uri
    ): String? {

        val projection =
            arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME
            )

        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {

                val index =
                    cursor.getColumnIndex(
                        MediaStore.MediaColumns.DISPLAY_NAME
                    )

                if (index >= 0) {

                    return cursor.getString(index)
                }
            }
        }

        return null
    }


    // =========================================================
    // LEGACY TRASH API
    // =========================================================

    suspend fun trashImages(
        context: Context,
        uris: List<Uri>
    ): Result<Int> {

        return withContext(Dispatchers.IO) {

            if (
                Build.VERSION.SDK_INT <
                Build.VERSION_CODES.R
            ) {

                return@withContext Result.failure(
                    Exception(
                        "Trash is supported on Android 11 and above"
                    )
                )
            }

            try {

                var count = 0

                for (uri in uris) {

                    val values =
                        ContentValues().apply {

                            put(
                                MediaStore.MediaColumns.IS_TRASHED,
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
                        count++
                    }
                }

                Result.success(count)

            } catch (e: Exception) {

                Result.failure(e)
            }
        }
    }


    private fun sanitizeAlbumName(
        albumName: String
    ): String {

        return albumName
            .trim()
            .replace(
                Regex("[\\\\/:*?\"<>|]"),
                "_"
            )
            .replace(
                Regex("/+"),
                ""
            )
            .trim('/')
            .ifBlank {
                "MyGallery"
            }
    }
}