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

            // Copy original image to Trash folder
            resolver.openInputStream(
                image.uri
            )?.use { input ->

                trashFile.outputStream().use { output ->

                    input.copyTo(output)
                }

            } ?: return Result.failure(
                Exception("Unable to read image")
            )


            // Create Trash metadata
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


            // Save Trash metadata
            TrashStorage.save(
                context,
                trashItem
            )


            // IMPORTANT:
            // Do NOT delete the original here.
            // Android 10 requires the proper MediaStore
            // user-confirmation flow.

            Result.success(
                trashItem
            )

        } catch (e: Exception) {

            Result.failure(e)
        }
    }


    fun deleteTrashCopy(
        trashItem: TrashItem
    ) {

        try {
            File(
                trashItem.trashFilePath
            ).delete()

        } catch (_: Exception) {
            // Ignore cleanup failure
        }
    }
}