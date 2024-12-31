package com.example.alertapp.android.di

import android.content.Context
import com.example.alertapp.services.AlertProcessor
import com.example.alertapp.services.processors.AndroidContentAlertProcessor
import com.example.alertapp.services.processors.AndroidLocationProvider
import com.example.alertapp.services.processors.AndroidPriceAlertProcessor
import com.example.alertapp.services.processors.AndroidWeatherAlertProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    @Provides
    @Singleton
    fun provideAlertProcessor(
        @ApplicationContext context: Context,
        locationProvider: AndroidLocationProvider,
        weatherProcessor: AndroidWeatherAlertProcessor,
        priceProcessor: AndroidPriceAlertProcessor,
        contentProcessor: AndroidContentAlertProcessor
    ): AlertProcessor {
        return AlertProcessor(
            context = context,
            locationProvider = locationProvider,
            weatherProcessor = weatherProcessor,
            priceProcessor = priceProcessor,
            contentProcessor = contentProcessor
        )
    }

    @Provides
    @Singleton
    fun provideLocationProvider(
        @ApplicationContext context: Context
    ): AndroidLocationProvider {
        return AndroidLocationProvider(context)
    }

    @Provides
    @Singleton
    fun provideWeatherProcessor(
        @ApplicationContext context: Context
    ): AndroidWeatherAlertProcessor {
        return AndroidWeatherAlertProcessor(context)
    }

    @Provides
    @Singleton
    fun providePriceProcessor(
        @ApplicationContext context: Context
    ): AndroidPriceAlertProcessor {
        return AndroidPriceAlertProcessor(context)
    }

    @Provides
    @Singleton
    fun provideContentProcessor(
        @ApplicationContext context: Context
    ): AndroidContentAlertProcessor {
        return AndroidContentAlertProcessor(context)
    }
}
