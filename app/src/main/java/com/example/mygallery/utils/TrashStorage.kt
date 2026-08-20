package com.example.mygallery.utils

import android.content.Context
import com.example.mygallery.model.TrashItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TrashStorage {

    private const val FILE_NAME = "trash_items.json"

    private fun getTrashDirectory(
        context: Context
    ): File {

        val directory =
            File(
                context.filesDir,
                "Trash"
            )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return directory
    }

    private fun getMetadataFile(
        context: Context
    ): File {

        return File(
            getTrashDirectory(context),
            FILE_NAME
        )
    }

    fun getAll(
        context: Context
    ): List<TrashItem> {

        val file =
            getMetadataFile(context)

        if (!file.exists()) {
            return emptyList()
        }

        return try {

            val json =
                JSONArray(
                    file.readText()
                )

            val items =
                mutableListOf<TrashItem>()

            for (i in 0 until json.length()) {

                val item =
                    json.getJSONObject(i)

                items.add(
                    TrashItem(
                        id = item.getLong("id"),
                        name = item.getString("name"),
                        originalUri =
                            item.getString("originalUri"),
                        folderName =
                            item.getString("folderName"),
                        dateAdded =
                            item.getLong("dateAdded"),
                        size =
                            item.getLong("size"),
                        mimeType =
                            item.getString("mimeType"),
                        trashFilePath =
                            item.getString("trashFilePath"),
                        trashedAt =
                            item.getLong("trashedAt")
                    )
                )
            }

            items

        } catch (e: Exception) {

            emptyList()
        }
    }

    fun save(
        context: Context,
        trashItem: TrashItem
    ) {

        val items =
            getAll(context)
                .toMutableList()

        items.removeAll {
            it.id == trashItem.id
        }

        items.add(trashItem)

        writeItems(
            context,
            items
        )
    }

    fun remove(
        context: Context,
        trashId: Long
    ) {

        val updated =
            getAll(context)
                .filterNot {
                    it.id == trashId
                }

        writeItems(
            context,
            updated
        )
    }

    private fun writeItems(
        context: Context,
        items: List<TrashItem>
    ) {

        val json =
            JSONArray()

        items.forEach { item ->

            val objectItem =
                JSONObject().apply {

                    put(
                        "id",
                        item.id
                    )

                    put(
                        "name",
                        item.name
                    )

                    put(
                        "originalUri",
                        item.originalUri
                    )

                    put(
                        "folderName",
                        item.folderName
                    )

                    put(
                        "dateAdded",
                        item.dateAdded
                    )

                    put(
                        "size",
                        item.size
                    )

                    put(
                        "mimeType",
                        item.mimeType
                    )

                    put(
                        "trashFilePath",
                        item.trashFilePath
                    )

                    put(
                        "trashedAt",
                        item.trashedAt
                    )
                }

            json.put(
                objectItem
            )
        }

        getMetadataFile(context)
            .writeText(
                json.toString()
            )
    }

    fun getTrashDirectoryPath(
        context: Context
    ): File {

        return getTrashDirectory(context)
    }
}