package com.example.alertapp.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [AlertEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(AlertConverters::class)
abstract class AlertDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao

    companion object {
        private const val DATABASE_NAME = "alert_database"

        @Volatile
        private var INSTANCE: AlertDatabase? = null

        fun getInstance(context: Context): AlertDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlertDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
