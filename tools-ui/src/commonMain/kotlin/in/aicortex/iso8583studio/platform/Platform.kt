package `in`.aicortex.iso8583studio.platform


/** Local wall-clock time as `HH:mm:ss.SSS`, the format every calculator log line uses. */
expect fun nowLogTimestamp(): String

/** Milliseconds since the Unix epoch. */
expect fun epochMillis(): Long
