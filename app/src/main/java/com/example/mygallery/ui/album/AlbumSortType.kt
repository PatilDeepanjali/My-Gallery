package com.example.mygallery.ui.album

/**
 * Sort criteria for the ALBUM list (folders), as opposed to
 * com.example.mygallery.ui.photo.SortType which sorts individual
 * photos. "Date Taken" doesn't make sense for a folder, so Album gets
 * its own set of options here.
 */
enum class AlbumSortType {
    NAME,
    ITEM_COUNT,
    SIZE,
    DATE_ADDED   // uses the most recently added photo inside the album
}