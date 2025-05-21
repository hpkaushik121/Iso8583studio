package `in`.aicortex.iso8583studio.domain.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    fun formatDate(milliseconds: Long, pattern: String = "dd MMM yyyy HH:mm a"): String {
        val date = Date(milliseconds)
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(date)
    }

}