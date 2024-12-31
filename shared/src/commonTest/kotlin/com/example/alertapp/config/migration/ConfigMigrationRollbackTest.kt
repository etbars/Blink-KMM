package com.example.alertapp.config.migration

import com.example.alertapp.config.*
import kotlinx.serialization.json.*
import kotlin.test.*

class ConfigMigrationRollbackTest {
    private lateinit var migrationManager: ConfigMigrationManager

    @BeforeTest
    fun setup() {
        migrationManager = ConfigMigrationManager()
    }

    @Test
    fun testSuccessfulRollback() {
        val m1 = FailingMigration(
            fromVersion = ConfigVersion(1, 0, 0),
            toVersion = ConfigVersion(2, 0, 0),
            failOnMigrate = true,
            failOnRollback = false
        )

        migrationManager.registerMigration(m1)

        val config = AlertAppConfig()
        
        val exception = assertFailsWith<MigrationException> {
            migrationManager.migrateAppConfig(config, ConfigVersion(1, 0, 0))
        }

        assertEquals(ConfigVersion(1, 0, 0), exception.fromVersion)
        assertEquals(ConfigVersion(2, 0, 0), exception.toVersion)
        assertTrue(exception.message?.contains("Rolled back to version 1.0.0") == true)
    }

    @Test
    fun testFailedRollback() {
        val m1 = FailingMigration(
            fromVersion = ConfigVersion(1, 0, 0),
            toVersion = ConfigVersion(2, 0, 0),
            failOnMigrate = true,
            failOnRollback = true
        )

        migrationManager.registerMigration(m1)

        val config = AlertAppConfig()
        
        val exception = assertFailsWith<MigrationException> {
            migrationManager.migrateAppConfig(config, ConfigVersion(1, 0, 0))
        }

        assertEquals(ConfigVersion(1, 0, 0), exception.fromVersion)
        assertEquals(ConfigVersion(2, 0, 0), exception.toVersion)
        assertTrue(exception.message?.contains("Migration failed") == true)
    }

    @Test
    fun testPartialMigrationRollback() {
        val m1 = FailingMigration(
            fromVersion = ConfigVersion(1, 0, 0),
            toVersion = ConfigVersion(1, 1, 0),
            failOnMigrate = false,
            failOnRollback = false
        )
        val m2 = FailingMigration(
            fromVersion = ConfigVersion(1, 1, 0),
            toVersion = ConfigVersion(2, 0, 0),
            failOnMigrate = true,
            failOnRollback = false
        )

        migrationManager.registerMigration(m1)
        migrationManager.registerMigration(m2)

        val config = AlertAppConfig()
        
        val exception = assertFailsWith<MigrationException> {
            migrationManager.migrateAppConfig(config, ConfigVersion(1, 0, 0))
        }

        assertEquals(ConfigVersion(1, 0, 0), exception.fromVersion)
        assertEquals(ConfigVersion(2, 0, 0), exception.toVersion)
        assertTrue(exception.message?.contains("Rolled back to version 1.0.0") == true)
    }

    private class FailingMigration(
        fromVersion: ConfigVersion,
        toVersion: ConfigVersion,
        private val failOnMigrate: Boolean,
        private val failOnRollback: Boolean
    ) : ConfigMigration(fromVersion, toVersion) {
        override fun migrateAppConfig(config: JsonObject): JsonObject {
            if (failOnMigrate) {
                throw RuntimeException("Migration failed")
            }
            return config
        }

        override fun rollbackAppConfig(config: JsonObject, error: Throwable): JsonObject {
            if (failOnRollback) {
                throw RuntimeException("Rollback failed")
            }
            return config
        }

        override fun migratePlatformConfig(config: JsonObject): JsonObject {
            if (failOnMigrate) {
                throw RuntimeException("Migration failed")
            }
            return config
        }

        override fun rollbackPlatformConfig(config: JsonObject, error: Throwable): JsonObject {
            if (failOnRollback) {
                throw RuntimeException("Rollback failed")
            }
            return config
        }

        override fun migrateWorkConfig(config: JsonObject): JsonObject {
            if (failOnMigrate) {
                throw RuntimeException("Migration failed")
            }
            return config
        }

        override fun rollbackWorkConfig(config: JsonObject, error: Throwable): JsonObject {
            if (failOnRollback) {
                throw RuntimeException("Rollback failed")
            }
            return config
        }
    }
}
