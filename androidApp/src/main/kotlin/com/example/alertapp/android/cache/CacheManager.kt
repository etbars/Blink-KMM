package com.example.alertapp.android.cache

import android.content.Context
import androidx.room.*
import com.example.alertapp.android.processors.PriceData
import com.example.alertapp.android.processors.WeatherData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Entity(tableName = "cache_entries")
data class CacheEntry(
    @PrimaryKey val key: String,
    val data: String,
    val timestamp: Long,
    val type: CacheType
)

enum class CacheType {
    PRICE,
    WEATHER
}

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache_entries WHERE `key` = :key AND type = :type")
    suspend fun get(key: String, type: CacheType): CacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CacheEntry)

    @Query("DELETE FROM cache_entries WHERE timestamp < :timestamp")
    suspend fun deleteOld(timestamp: Long)
}

@Database(entities = [CacheEntry::class], version = 1)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}

@Singleton
class CacheManager @Inject constructor(
    context: Context,
    private val gson: Gson
) {
    private val db = Room.databaseBuilder(
        context,
        CacheDatabase::class.java,
        "alert_cache.db"
    ).build()

    private val dao = db.cacheDao()

    companion object {
        private val PRICE_CACHE_DURATION = TimeUnit.MINUTES.toMillis(5) // Cache price data for 5 minutes
        private val WEATHER_CACHE_DURATION = TimeUnit.MINUTES.toMillis(30) // Cache weather data for 30 minutes
    }

    suspend fun getPriceData(symbol: String): PriceData? {
        return getCacheEntry(symbol, CacheType.PRICE, PRICE_CACHE_DURATION)?.let { entry ->
            gson.fromJson(entry.data, PriceData::class.java)
        }
    }

    suspend fun getWeatherData(location: String): WeatherData? {
        return getCacheEntry(location, CacheType.WEATHER, WEATHER_CACHE_DURATION)?.let { entry ->
            gson.fromJson(entry.data, WeatherData::class.java)
        }
    }

    suspend fun cachePriceData(symbol: String, data: PriceData) {
        val entry = CacheEntry(
            key = symbol,
            data = gson.toJson(data),
            timestamp = System.currentTimeMillis(),
            type = CacheType.PRICE
        )
        dao.insert(entry)
    }

    suspend fun cacheWeatherData(location: String, data: WeatherData) {
        val entry = CacheEntry(
            key = location,
            data = gson.toJson(data),
            timestamp = System.currentTimeMillis(),
            type = CacheType.WEATHER
        )
        dao.insert(entry)
    }

    private suspend fun getCacheEntry(key: String, type: CacheType, maxAge: Long): CacheEntry? {
        val entry = dao.get(key, type) ?: return null
        val age = System.currentTimeMillis() - entry.timestamp
        return if (age <= maxAge) entry else null
    }

    suspend fun cleanOldEntries() {
        val oldestValidTimestamp = System.currentTimeMillis() - maxOf(PRICE_CACHE_DURATION, WEATHER_CACHE_DURATION)
        dao.deleteOld(oldestValidTimestamp)
    }
}
