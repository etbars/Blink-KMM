package com.example.alertapp.android.data

import androidx.room.*
import com.example.alertapp.models.AlertType
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE type = :type")
    fun getAlertsByType(type: AlertType): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isActive = 1")
    fun getActiveAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE id = :id")
    suspend fun getAlertById(id: String): AlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Delete
    suspend fun deleteAlert(alert: AlertEntity)

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteAlertById(id: String)

    @Query("UPDATE alerts SET lastTriggered = :timestamp WHERE id = :id")
    suspend fun updateLastTriggered(id: String, timestamp: Long)

    @Query("UPDATE alerts SET isActive = :isActive WHERE id = :id")
    suspend fun updateAlertActive(id: String, isActive: Boolean)

    @Query("SELECT * FROM alerts WHERE userId = :userId")
    fun getAlertsByUser(userId: String): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE checkInterval <= :maxInterval AND isActive = 1")
    suspend fun getAlertsForInterval(maxInterval: Long): List<AlertEntity>
}
