package com.example.carelink.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// shared formatters so dates look the same everywhere in the app
object DateTimeFormat {

    private val dateOnly = SimpleDateFormat("d MMM yyyy", Locale.UK)
    private val dateAndTime = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.UK)

    fun date(millis: Long): String = dateOnly.format(Date(millis))

    fun dateTime(millis: Long): String = dateAndTime.format(Date(millis))
}