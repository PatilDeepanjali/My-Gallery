package com.example.mygallery.ui.photo.menu

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.mygallery.model.ImageModel
import com.example.mygallery.repository.GalleryRepository
import com.example.mygallery.utils.FavoritePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PhotoSelectionActions {

    // ---------------------------------------------------------
    // FAVORITE
    // ---------------------------------------------------------

    fun toggleFavorite(
        context: Context,
        photos: List<ImageModel>
    ): Boolean {

        if (photos.isEmpty()) {
            return false
        }

        val allAlreadyFavorite =
            photos.all {
                FavoritePreferences.isFavorite(
                    context,
                    it.id
                )
            }

        if (allAlreadyFavorite) {

            photos.forEach {

                FavoritePreferences.toggleFavorite(
                    context,
                    it.id
                )
            }

            Toast.makeText(
                context,
                "Removed from Favorites",
                Toast.LENGTH_SHORT
            ).show()

            return false

        } else {

            photos.forEach { photo ->

                if (
                    !FavoritePreferences.isFavorite(
                        context,
                        photo.id
                    )
                ) {

                    FavoritePreferences.toggleFavorite(
                        context,
                        photo.id
                    )
                }
            }

            Toast.makeText(
                context,
                "Added to Favorites",
                Toast.LENGTH_SHORT
            ).show()

            return true
        }
    }


    // ---------------------------------------------------------
    // SHARE
    // ---------------------------------------------------------

    fun share(
        context: Context,
        photos: List<ImageModel>
    ) {

        if (photos.isEmpty()) {
            return
        }

        try {

            if (photos.size == 1) {

                val photo = photos.first()

                val mimeType =
                    context.contentResolver.getType(
                        photo.uri
                    ) ?: photo.mimeType.ifBlank {
                        "image/*"
                    }

                val intent =
                    Intent(Intent.ACTION_SEND).apply {

                        type = mimeType

                        putExtra(
                            Intent.EXTRA_STREAM,
                            photo.uri
                        )

                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

                context.startActivity(
                    Intent.createChooser(
                        intent,
                        "Share Photo"
                    )
                )

            } else {

                val uris =
                    ArrayList<Uri>()

                photos.forEach {
                    uris.add(it.uri)
                }

                val intent =
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {

                        type = "image/*"

                        putParcelableArrayListExtra(
                            Intent.EXTRA_STREAM,
                            uris
                        )

                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

                context.startActivity(
                    Intent.createChooser(
                        intent,
                        "Share Photos"
                    )
                )
            }

        } catch (e: Exception) {

            Toast.makeText(
                context,
                "Unable to share selected photos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // ---------------------------------------------------------
    // COPY
    // ---------------------------------------------------------

    suspend fun copy(
        context: Context,
        repository: GalleryRepository,
        photos: List<ImageModel>,
        destinationAlbumName: String
    ): Result<Int> {

        if (photos.isEmpty()) {

            return Result.failure(
                Exception("No photos to copy")
            )
        }

        return repository.copyImages(
            context,
            photos.map { it.uri },
            destinationAlbumName
        )
    }


    // ---------------------------------------------------------
    // MOVE
    //
    // This only performs the COPY part.
    // The original MediaStore items must still be deleted
    // through PhotosViewModel.deleteImages() because that
    // handles Android 10/11+ permission correctly.
    // ---------------------------------------------------------

    suspend fun move(
        context: Context,
        repository: GalleryRepository,
        photos: List<ImageModel>,
        destinationAlbumName: String
    ): Result<Int> {

        if (photos.isEmpty()) {

            return Result.failure(
                Exception("No photos to move")
            )
        }

        return repository.copyImages(
            context,
            photos.map { it.uri },
            destinationAlbumName
        )
    }


    // ---------------------------------------------------------
    // SELECTED PHOTO COUNT
    // ---------------------------------------------------------

    fun selectedCount(
        photos: List<ImageModel>
    ): Int {
        return photos.size
    }


    // ---------------------------------------------------------
    // SELECTED SIZE
    // ---------------------------------------------------------

    fun selectedSize(
        photos: List<ImageModel>
    ): Long {

        return photos.sumOf {
            it.size
        }
    }



    fun addToFavorites(
        context: Context,
        photos: List<ImageModel>
    ) {
        if (photos.isEmpty()) return

        photos.forEach { photo ->

            if (
                !FavoritePreferences.isFavorite(
                    context,
                    photo.id
                )
            ) {
                FavoritePreferences.toggleFavorite(
                    context,
                    photo.id
                )
            }
        }

        Toast.makeText(
            context,
            if (photos.size == 1)
                "Added to Favorites"
            else
                "${photos.size} photos added to Favorites",
            Toast.LENGTH_SHORT
        ).show()
    }



    fun removeFromFavorites(
        context: Context,
        photos: List<ImageModel>
    ) {
        if (photos.isEmpty()) return

        photos.forEach { photo ->

            if (
                FavoritePreferences.isFavorite(
                    context,
                    photo.id
                )
            ) {
                FavoritePreferences.toggleFavorite(
                    context,
                    photo.id
                )
            }
        }

        Toast.makeText(
            context,
            if (photos.size == 1)
                "Removed from Favorites"
            else
                "${photos.size} photos removed from Favorites",
            Toast.LENGTH_SHORT
        ).show()
    }

}






