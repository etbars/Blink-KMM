package com.example.alertapp.config.backup.compression

import platform.Foundation.*

actual fun compressGzip(data: ByteArray): ByteArray {
    val nsData = data.toNSData()
    return (nsData.gzippedData() ?: throw BackupCompressionException("Failed to compress data")).toByteArray()
}

actual fun decompressGzip(data: ByteArray): ByteArray {
    val nsData = data.toNSData()
    return (nsData.gunzippedData() ?: throw BackupCompressionException("Failed to decompress data")).toByteArray()
}

private fun ByteArray.toNSData(): NSData {
    return NSData.dataWithBytes(this, this.size.toULong())
}

private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(this.length.toInt())
    this.getBytes(bytes)
    return bytes
}
