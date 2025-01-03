package com.example.alertapp.config.backup.compression

import com.example.alertapp.config.backup.ConfigBackup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Handles compression and decompression of backups.
 */
class BackupCompression(
    private val json: Json = Json { prettyPrint = true }
) {
    /**
     * Compress a backup.
     * @param backup The backup to compress
     * @return The compressed backup data
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun compress(backup: ConfigBackup): CompressedBackup {
        val backupJson = json.encodeToString(backup)
        val compressed = compressGzip(backupJson.encodeToByteArray())
        return CompressedBackup(
            data = Base64.encode(compressed)
        )
    }

    /**
     * Decompress a backup.
     * @param compressed The compressed backup data
     * @return The decompressed backup
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decompress(compressed: CompressedBackup): ConfigBackup {
        val decodedData = Base64.decode(compressed.data)
        val decompressed = decompressGzip(decodedData)
        return json.decodeFromString(decompressed.decodeToString())
    }
}

/**
 * Platform-specific GZIP compression.
 */
expect fun compressGzip(data: ByteArray): ByteArray

/**
 * Platform-specific GZIP decompression.
 */
expect fun decompressGzip(data: ByteArray): ByteArray
