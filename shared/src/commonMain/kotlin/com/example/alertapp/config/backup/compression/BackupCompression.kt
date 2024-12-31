package com.example.alertapp.config.backup.compression

import com.example.alertapp.config.backup.ConfigBackup
import io.ktor.utils.io.core.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Buffer
import okio.GzipSink
import okio.GzipSource
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
        
        val buffer = Buffer()
        GzipSink(buffer).use { gzip ->
            gzip.write(backupJson.encodeToByteArray())
        }

        val compressed = buffer.readByteArray()
        return CompressedBackup(
            data = Base64.encode(compressed)
        )
    }

    /**
     * Decompress a backup.
     * @param compressed The compressed backup data
     * @return The decompressed backup
     * @throws BackupCompressionException if decompression fails
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decompress(compressed: CompressedBackup): ConfigBackup {
        try {
            val compressedData = Base64.decode(compressed.data)
            
            val buffer = Buffer()
            buffer.write(compressedData)
            
            val decompressed = Buffer()
            GzipSource(buffer).use { gzip ->
                decompressed.writeAll(gzip)
            }

            val backupJson = decompressed.readUtf8()
            return json.decodeFromString(backupJson)
        } catch (e: Exception) {
            throw BackupCompressionException("Failed to decompress backup", e)
        }
    }

    /**
     * Calculate the compression ratio for a backup.
     * @param original The original backup
     * @param compressed The compressed backup
     * @return The compression ratio (compressed size / original size)
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun getCompressionRatio(original: ConfigBackup, compressed: CompressedBackup): Double {
        val originalSize = json.encodeToString(original).encodeToByteArray().size
        val compressedSize = Base64.decode(compressed.data).size
        return compressedSize.toDouble() / originalSize.toDouble()
    }
}
