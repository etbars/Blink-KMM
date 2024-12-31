package com.example.alertapp.config.migration.v1_0_0

import com.example.alertapp.config.migration.ConfigMigration
import com.example.alertapp.config.migration.ConfigVersion
import kotlinx.serialization.json.*

/**
 * Initial migration that sets up the base configuration structure.
 * This migration converts from unversioned config to version 1.0.0.
 */
class InitialMigration : ConfigMigration(
    fromVersion = ConfigVersion(0, 0, 0),
    toVersion = ConfigVersion(1, 0, 0)
) {
    override fun migrateAppConfig(config: JsonObject): JsonObject {
        // Convert old alert check intervals from milliseconds to Duration format
        val alertCheck = config.getOrNull("alertCheck")?.jsonObject?.let { check ->
            buildJsonObject {
                // Convert milliseconds to minutes/hours
                check.getString("defaultInterval")?.toLongOrNull()?.let { ms ->
                    put("defaultInterval", "${ms / (60 * 1000)}m")
                }
                check.getString("minInterval")?.toLongOrNull()?.let { ms ->
                    put("minInterval", "${ms / (60 * 1000)}m")
                }
                check.getString("maxInterval")?.toLongOrNull()?.let { ms ->
                    put("maxInterval", "${ms / (60 * 60 * 1000)}h")
                }
            }
        } ?: buildJsonObject {
            put("defaultInterval", "15m")
            put("minInterval", "5m")
            put("maxInterval", "24h")
        }

        // Convert old API configurations
        val api = config.getOrNull("api")?.jsonObject?.let { apiConfig ->
            buildJsonObject {
                // Migrate each API endpoint config
                listOf("newsApi", "weatherApi", "priceApi").forEach { endpoint ->
                    apiConfig.getOrNull(endpoint)?.jsonObject?.let { old ->
                        put(endpoint, buildJsonObject {
                            old.getString("baseUrl")?.let { put("baseUrl", JsonPrimitive(it)) }
                            old.getString("apiKey")?.let { put("apiKey", JsonPrimitive(it)) }
                            put("timeout", JsonPrimitive("30s"))
                        })
                    }
                }
            }
        } ?: buildJsonObject {
            // Default API configurations
            put("newsApi", buildJsonObject {
                put("baseUrl", "https://newsapi.org/v2/")
                put("apiKey", "YOUR_NEWS_API_KEY")
                put("timeout", "30s")
            })
            // Add other default API configs...
        }

        // Convert cache configuration
        val cache = config.getOrNull("cache")?.jsonObject?.let { cacheConfig ->
            buildJsonObject {
                cacheConfig.getNumber("maxSizeBytes")?.let { 
                    put("maxSizeBytes", JsonPrimitive(it.toLong())) 
                }
                cacheConfig.getNumber("maxAge")?.let { hours ->
                    put("maxAge", JsonPrimitive("${hours}h"))
                }
                cacheConfig.getNumber("maxStale")?.let { days ->
                    put("maxStale", JsonPrimitive("${days}d"))
                }
                put("timeout", JsonPrimitive("5s"))
            }
        } ?: buildJsonObject {
            put("maxSizeBytes", JsonPrimitive(10L * 1024L * 1024L))
            put("maxAge", JsonPrimitive("1h"))
            put("maxStale", JsonPrimitive("7d"))
            put("timeout", JsonPrimitive("5s"))
        }

        return buildJsonObject {
            put("alertCheck", alertCheck)
            put("api", api)
            put("cache", cache)
            put("database", config.getOrNull("database") ?: buildJsonObject {
                put("name", JsonPrimitive("alert_app.db"))
                put("version", JsonPrimitive(1))
            })
            put("pagination", config.getOrNull("pagination") ?: buildJsonObject {
                put("defaultPageSize", JsonPrimitive(20))
                put("maxPageSize", JsonPrimitive(100))
                put("initialPage", JsonPrimitive(1))
            })
            put("timing", config.getOrNull("timing") ?: buildJsonObject {
                put("animationDurationShort", JsonPrimitive("150ms"))
                put("animationDurationMedium", JsonPrimitive("300ms"))
                put("animationDurationLong", JsonPrimitive("500ms"))
                put("networkTimeout", JsonPrimitive("30s"))
                put("cacheTimeout", JsonPrimitive("5s"))
            })
        }
    }

    override fun migratePlatformConfig(config: JsonObject): JsonObject {
        // Convert notification configuration
        val notification = config.getOrNull("notification")?.jsonObject?.let { notifConfig ->
            buildJsonObject {
                notifConfig.getString("channelId")?.let { 
                    put("channelId", JsonPrimitive(it)) 
                }
                notifConfig.getString("channelName")?.let { 
                    put("channelName", JsonPrimitive(it)) 
                }
                notifConfig.getString("channelDescription")?.let { 
                    put("channelDescription", JsonPrimitive(it)) 
                }
            }
        } ?: buildJsonObject {
            put("channelId", JsonPrimitive("alert_notifications"))
            put("channelName", JsonPrimitive("Alert Notifications"))
            put("channelDescription", JsonPrimitive("Notifications for triggered alerts"))
        }

        return buildJsonObject {
            put("notification", notification)
            put("permissions", config.getOrNull("permissions") ?: buildJsonObject {
                put("locationPermissionCode", JsonPrimitive(1001))
                put("notificationPermissionCode", JsonPrimitive(1002))
                put("signInPermissionCode", JsonPrimitive(1003))
            })
            put("storage", config.getOrNull("storage") ?: buildJsonObject {
                put("preferencesName", JsonPrimitive("alert_app_preferences"))
                put("themeModePrefKey", JsonPrimitive("theme_mode"))
                put("notificationEnabledPrefKey", JsonPrimitive("notifications_enabled"))
                put("lastSyncPrefKey", JsonPrimitive("last_sync"))
            })
        }
    }

    override fun migrateWorkConfig(config: JsonObject): JsonObject {
        return buildJsonObject {
            put("workNames", config.getOrNull("workNames") ?: buildJsonObject {
                put("alertCheckWork", JsonPrimitive("alert_check_work"))
                put("syncWork", JsonPrimitive("sync_work"))
            })
            put("intentActions", config.getOrNull("intentActions") ?: buildJsonObject {
                put("alertTriggered", JsonPrimitive("com.example.alertapp.ALERT_TRIGGERED"))
                put("alertDismissed", JsonPrimitive("com.example.alertapp.ALERT_DISMISSED"))
            })
            put("intentExtras", config.getOrNull("intentExtras") ?: buildJsonObject {
                put("alertId", JsonPrimitive("alert_id"))
                put("alertType", JsonPrimitive("alert_type"))
                put("notificationId", JsonPrimitive("notification_id"))
            })
        }
    }
}
