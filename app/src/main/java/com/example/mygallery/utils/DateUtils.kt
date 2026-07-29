package com.example.mygallery.utils

import android.icu.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    fun formatDate(time: Long): String {
        val sdf = SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        )
        return sdf.format(Date(time))
    }
}