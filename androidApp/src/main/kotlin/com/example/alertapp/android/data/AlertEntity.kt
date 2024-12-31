package com.example.alertapp.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.alertapp.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.datetime.Instant
import java.lang.reflect.Type

@Entity(tableName = "alerts")
@TypeConverters(AlertConverters::class)
data class AlertEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val type: AlertType,
    val trigger: AlertTrigger,
    val actions: List<AlertAction>,
    val isActive: Boolean,
    val createdAt: Instant,
    val lastTriggered: Instant?,
    val userId: String,
    val checkInterval: Long,
    val metadata: Map<String, String>
) {
    fun toAlert(): Alert = Alert(
        id = id,
        name = name,
        description = description,
        type = type,
        trigger = trigger,
        actions = actions,
        isActive = isActive,
        createdAt = createdAt,
        lastTriggered = lastTriggered,
        userId = userId,
        checkInterval = checkInterval,
        metadata = metadata
    )

    companion object {
        fun fromAlert(alert: Alert): AlertEntity = AlertEntity(
            id = alert.id,
            name = alert.name,
            description = alert.description,
            type = alert.type,
            trigger = alert.trigger,
            actions = alert.actions,
            isActive = alert.isActive,
            createdAt = alert.createdAt,
            lastTriggered = alert.lastTriggered,
            userId = alert.userId,
            checkInterval = alert.checkInterval,
            metadata = alert.metadata
        )
    }
}

class AlertConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilliseconds()
    }

    @TypeConverter
    fun alertTypeToString(type: AlertType): String = type.name

    @TypeConverter
    fun stringToAlertType(value: String): AlertType = AlertType.valueOf(value)

    @TypeConverter
    fun alertTriggerToString(trigger: AlertTrigger): String = gson.toJson(trigger)

    @TypeConverter
    fun stringToAlertTrigger(value: String): AlertTrigger = gson.fromJson(value, AlertTrigger::class.java)

    @TypeConverter
    fun alertActionsToString(actions: List<AlertAction>): String = gson.toJson(actions)

    @TypeConverter
    fun stringToAlertActions(value: String): List<AlertAction> {
        val listType: Type = object : TypeToken<List<AlertAction>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun mapToString(map: Map<String, String>): String = gson.toJson(map)

    @TypeConverter
    fun stringToMap(value: String): Map<String, String> {
        val mapType: Type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }
}
