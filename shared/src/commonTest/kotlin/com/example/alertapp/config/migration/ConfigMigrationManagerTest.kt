package com.example.alertapp.config.migration

import com.example.alertapp.config.*
import kotlinx.serialization.json.*
import kotlin.test.*

class ConfigMigrationManagerTest {
    private lateinit var migrationManager: ConfigMigrationManager

    @BeforeTest
    fun setup() {
        migrationManager = ConfigMigrationManager()
    }

    @Test
    fun testMigrationPathCalculation() {
        val m1 = TestMigration(ConfigVersion(1, 0, 0), ConfigVersion(1, 1, 0))
        val m2 = TestMigration(ConfigVersion(1, 1, 0), ConfigVersion(1, 2, 0))
        val m3 = TestMigration(ConfigVersion(1, 2, 0), ConfigVersion(2, 0, 0))

        // Register migrations in random order
        migrationManager.registerMigration(m2)
        migrationManager.registerMigration(m3)
        migrationManager.registerMigration(m1)

        val config = AlertAppConfig()
        val result = migrationManager.migrateAppConfig(config, ConfigVersion(1, 0, 0))

        // Verify migrations were applied in correct order
        assertEquals(3, TestMigration.migrationOrder.size)
        assertEquals(m1, TestMigration.migrationOrder[0])
        assertEquals(m2, TestMigration.migrationOrder[1])
        assertEquals(m3, TestMigration.migrationOrder[2])
    }

    @Test
    fun testMissingMigrationPath() {
        val m1 = TestMigration(ConfigVersion(1, 0, 0), ConfigVersion(1, 1, 0))
        val m3 = TestMigration(ConfigVersion(1, 2, 0), ConfigVersion(2, 0, 0))

        migrationManager.registerMigration(m1)
        migrationManager.registerMigration(m3)

        val config = AlertAppConfig()
        
        assertFailsWith<IllegalStateException> {
            migrationManager.migrateAppConfig(config, ConfigVersion(1, 0, 0))
        }
    }

    @Test
    fun testNoMigrationNeededForCurrentVersion() {
        val config = AlertAppConfig()
        val result = migrationManager.migrateAppConfig(config, ConfigVersion.CURRENT)
        assertEquals(config, result)
    }

    @Test
    fun testNoMigrationNeededForNewerVersion() {
        val config = AlertAppConfig()
        val newerVersion = ConfigVersion(
            ConfigVersion.CURRENT.major + 1,
            ConfigVersion.CURRENT.minor,
            ConfigVersion.CURRENT.patch
        )
        val result = migrationManager.migrateAppConfig(config, newerVersion)
        assertEquals(config, result)
    }

    private class TestMigration(
        fromVersion: ConfigVersion,
        toVersion: ConfigVersion
    ) : ConfigMigration(fromVersion, toVersion) {
        override fun migrateAppConfig(config: JsonObject): JsonObject {
            migrationOrder.add(this)
            return config
        }

        override fun migratePlatformConfig(config: JsonObject): JsonObject {
            migrationOrder.add(this)
            return config
        }

        override fun migrateWorkConfig(config: JsonObject): JsonObject {
            migrationOrder.add(this)
            return config
        }

        companion object {
            val migrationOrder = mutableListOf<TestMigration>()
        }
    }
}
