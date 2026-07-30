package com.example.mygallery.utils

import com.example.mygallery.model.ImageModel
import com.example.mygallery.model.PhotoListItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateGroupingUtil {

    fun groupByDate(images: List<ImageModel>): List<PhotoListItem> {

        val result = mutableListOf<PhotoListItem>()
        var lastLabel: String? = null

        for (image in images) {

            val label = buildDateLabel(image.dateAdded)

            // Only insert a new header when the label actually changes
            // from the previous photo's label.
            if (label != lastLabel) {
                result.add(PhotoListItem.DateHeader(label))
                lastLabel = label
            }

            result.add(PhotoListItem.Photo(image))
        }

        return result
    }


    private fun buildDateLabel(dateAddedSeconds: Long): String {

        val photoDate = Date(dateAddedSeconds * 1000)

        val photoCalendar = Calendar.getInstance().apply { time = photoDate }
        val todayCalendar = Calendar.getInstance()

        return if (isSameDay(photoCalendar, todayCalendar)) {
            val formatted = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
                .format(photoDate)
            "Today $formatted"
        } else {
            SimpleDateFormat("EEEE d MMMM, yyyy", Locale.getDefault())
                .format(photoDate)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}