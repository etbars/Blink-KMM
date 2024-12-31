package com.example.alertapp.config.migration.v1_0_0

import kotlinx.serialization.json.*
import kotlin.test.*

class InitialMigrationTest {
    private lateinit var migration: InitialMigration

    @BeforeTest
    fun setup() {
        migration = InitialMigration()
    }

    @Test
    fun testAppConfigMigration() {
        val oldConfig = buildJsonObject {
            put("alertCheck", buildJsonObject {
                put("defaultInterval", JsonPrimitive("900000")) // 15 minutes in ms
                put("minInterval", JsonPrimitive("300000")) // 5 minutes in ms
                put("maxInterval", JsonPrimitive("86400000")) // 24 hours in ms
            })
            put("api", buildJsonObject {
                put("newsApi", buildJsonObject {
                    put("baseUrl", JsonPrimitive("https://old-news-api.org"))
                    put("apiKey", JsonPrimitive("old-key"))
                })
            })
        }

        val newConfig = migration.migrateAppConfig(oldConfig)

        // Verify alert check intervals were converted to duration format
        assertEquals(
            "15m",
            newConfig.jsonObject["alertCheck"]?.jsonObject?.getString("defaultInterval")
        )
        assertEquals(
            "5m",
            newConfig.jsonObject["alertCheck"]?.jsonObject?.getString("minInterval")
        )
        assertEquals(
            "24h",
            newConfig.jsonObject["alertCheck"]?.jsonObject?.getString("maxInterval")
        )

        // Verify API config was preserved and timeout was added
        val newsApi = newConfig.jsonObject["api"]?.jsonObject?.get("newsApi")?.jsonObject
        assertNotNull(newsApi)
        assertEquals("https://old-news-api.org", newsApi.getString("baseUrl"))
        assertEquals("old-key", newsApi.getString("apiKey"))
        assertEquals("30s", newsApi.getString("timeout"))

        // Verify default values were added
        assertNotNull(newConfig.jsonObject["cache"])
        assertNotNull(newConfig.jsonObject["database"])
        assertNotNull(newConfig.jsonObject["pagination"])
        assertNotNull(newConfig.jsonObject["timing"])
    }

    @Test
    fun testPlatformConfigMigration() {
        val oldConfig = buildJsonObject {
            put("notification", buildJsonObject {
                put("channelId", JsonPrimitive("old_channel"))
                put("channelName", JsonPrimitive("Old Channel"))
            })
        }

        val newConfig = migration.migratePlatformConfig(oldConfig)

        // Verify notification config was preserved
        val notification = newConfig.jsonObject["notification"]?.jsonObject
        assertNotNull(notification)
        assertEquals("old_channel", notification.getString("channelId"))
        assertEquals("Old Channel", notification.getString("channelName"))

        // Verify default values were added
        assertNotNull(newConfig.jsonObject["permissions"])
        assertNotNull(newConfig.jsonObject["storage"])
    }

    @Test
    fun testWorkConfigMigration() {
        val oldConfig = buildJsonObject {
            put("workNames", buildJsonObject {
                put("alertCheckWork", JsonPrimitive("old_alert_check"))
            })
        }

        val newConfig = migration.migrateWorkConfig(oldConfig)

        // Verify work names were preserved
        val workNames = newConfig.jsonObject["workNames"]?.jsonObject
        assertNotNull(workNames)
        assertEquals("old_alert_check", workNames.getString("alertCheckWork"))

        // Verify default values were added
        assertNotNull(newConfig.jsonObject["intentActions"])
        assertNotNull(newConfig.jsonObject["intentExtras"])
    }

    private fun JsonObject.getString(key: String): String? =
        get(key)?.jsonPrimitive?.content
}
