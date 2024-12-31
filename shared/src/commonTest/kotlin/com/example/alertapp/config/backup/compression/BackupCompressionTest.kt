package com.example.alertapp.config.backup.compression

import com.example.alertapp.config.AlertAppConfig
import com.example.alertapp.config.backup.ConfigBackup
import com.example.alertapp.config.migration.ConfigVersion
import kotlinx.datetime.Clock
import kotlin.test.*

class BackupCompressionTest {
    private lateinit var compression: BackupCompression
    private lateinit var testBackup: ConfigBackup

    @BeforeTest
    fun setup() {
        compression = BackupCompression()
        testBackup = ConfigBackup(
            timestamp = Clock.System.now(),
            version = ConfigVersion.CURRENT,
            appConfig = AlertAppConfig(),
            platformConfig = null,
            workConfig = null
        )
    }

    @Test
    fun testCompressionDecompression() {
        // Compress backup
        val compressed = compression.compress(testBackup)
        
        // Verify compressed data is not null
        assertNotNull(compressed.data)
        
        // Decompress backup
        val decompressed = compression.decompress(compressed)
        
        // Verify decompressed data matches original
        assertEquals(testBackup.timestamp, decompressed.timestamp)
        assertEquals(testBackup.version, decompressed.version)
        assertEquals(testBackup.appConfig, decompressed.appConfig)
        assertEquals(testBackup.platformConfig, decompressed.platformConfig)
        assertEquals(testBackup.workConfig, decompressed.workConfig)
    }

    @Test
    fun testDecompressionWithCorruptedData() {
        val compressed = compression.compress(testBackup)
        val corrupted = compressed.copy(data = compressed.data.dropLast(1))
        
        assertFailsWith<BackupCompressionException> {
            compression.decompress(corrupted)
        }
    }

    @Test
    fun testCompressionRatio() {
        val compressed = compression.compress(testBackup)
        val ratio = compression.getCompressionRatio(testBackup, compressed)
        
        // Verify compression actually reduces size
        assertTrue(ratio < 1.0)
    }

    @Test
    fun testMultipleCompressions() {
        val compressed1 = compression.compress(testBackup)
        val compressed2 = compression.compress(testBackup)
        
        // Verify compression is deterministic
        assertEquals(compressed1.data, compressed2.data)
        
        // Verify both can be decompressed
        val decompressed1 = compression.decompress(compressed1)
        val decompressed2 = compression.decompress(compressed2)
        
        assertEquals(decompressed1, decompressed2)
    }
}
