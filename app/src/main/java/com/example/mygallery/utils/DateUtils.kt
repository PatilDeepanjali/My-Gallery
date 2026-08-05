package com.example.mygallery.utils

import android.icu.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
     fun formatDate(time: Long): String {

        val formatter =
            SimpleDateFormat("MMMM dd", Locale.getDefault())

        return formatter.format(Date(time * 1000))
    }

     fun formatTime(time: Long): String {

        val formatter =
            SimpleDateFormat("HH:mm", Locale.getDefault())

        return formatter.format(Date(time * 1000))
    }
}