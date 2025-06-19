package io.cryptocalc.core

fun hexToBytes(hex: String): ByteArray {
    val cleanHex = hex.replace(" ", "").uppercase()
    return cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

fun bytesToHex(bytes: ByteArray): String =
    bytes.joinToString("") { "%02X".format(it) }

fun String.padEnd(length: Int, padChar: Char): String =
    if (this.length >= length) this else this + padChar.toString().repeat(length - this.length)
