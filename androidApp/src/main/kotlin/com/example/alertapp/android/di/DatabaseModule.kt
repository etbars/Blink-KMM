package com.example.alertapp.android.di

import android.content.Context
import com.example.alertapp.android.data.AlertDao
import com.example.alertapp.android.data.AlertDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAlertDatabase(
        @ApplicationContext context: Context
    ): AlertDatabase {
        return AlertDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideAlertDao(database: AlertDatabase): AlertDao {
        return database.alertDao()
    }
}
