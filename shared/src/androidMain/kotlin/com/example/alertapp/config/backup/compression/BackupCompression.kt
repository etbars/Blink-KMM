package com.example.alertapp.config.backup.compression

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

actual fun compressGzip(data: ByteArray): ByteArray {
    return ByteArrayOutputStream().use { byteStream ->
        GZIPOutputStream(byteStream).use { gzipStream ->
            gzipStream.write(data)
        }
        byteStream.toByteArray()
    }
}

actual fun decompressGzip(data: ByteArray): ByteArray {
    return ByteArrayInputStream(data).use { byteStream ->
        GZIPInputStream(byteStream).use { gzipStream ->
            gzipStream.readBytes()
        }
    }
}
