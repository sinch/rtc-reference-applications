package com.sinch.rtc.vvc.reference.app.utils.date

import java.text.SimpleDateFormat
import java.util.*

object DateFormats {

    private const val SIMPLE_FORMAT_DATE_ONLY = "d MMMM yyyy"

    fun dateOnlyDefault(date: Date): String =
        SimpleDateFormat(SIMPLE_FORMAT_DATE_ONLY, Locale.US).format(date)

}