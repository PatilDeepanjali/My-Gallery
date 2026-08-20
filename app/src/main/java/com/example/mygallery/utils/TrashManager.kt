package com.example.mygallery.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.mygallery.model.ImageModel
import com.example.mygallery.model.TrashItem
import java.io.File

object TrashManager {

    fun moveToTrash(
        context: Context,
        image: ImageModel
    ): Result<TrashItem> {

        return try {

            val resolver =
                context.contentResolver

            val trashDirectory =
                TrashStorage.getTrashDirectoryPath(
                    context
                )

            val safeFileName =
                "${System.currentTimeMillis()}_${image.name}"

            val trashFile =
                File(
                    trashDirectory,
                    safeFileName
                )

            resolver.openInputStream(
                image.uri
            )?.use { input ->

                trashFile.outputStream().use { output ->

                    input.copyTo(output)
                }

            } ?: return Result.failure(
                Exception(
                    "Unable to read image"
                )
            )


            val trashItem =
                TrashItem(

                    id = image.id,

                    name = image.name,

                    originalUri =
                        image.uri.toString(),

                    folderName =
                        image.folderName,

                    dateAdded =
                        image.dateAdded,

                    size =
                        image.size,

                    mimeType =
                        image.mimeType,

                    trashFilePath =
                        trashFile.absolutePath,

                    trashedAt =
                        System.currentTimeMillis()
                )


            // Save metadata first.
            TrashStorage.save(
                context,
                trashItem
            )


            // Remove the original MediaStore item.
            val deleted =
                resolver.delete(
                    image.uri,
                    null,
                    null
                )


            if (deleted <= 0) {

                // Original could not be deleted.
                // Remove the Trash copy and metadata
                // so we don't create an inconsistent item.

                trashFile.delete()

                TrashStorage.remove(
                    context,
                    image.id
                )

                return Result.failure(
                    Exception(
                        "Unable to remove original image"
                    )
                )
            }


            Result.success(
                trashItem
            )

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}