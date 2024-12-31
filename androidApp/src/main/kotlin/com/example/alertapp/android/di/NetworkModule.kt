package com.example.alertapp.android.di

import android.content.Context
import com.example.alertapp.android.api.*
import com.example.alertapp.android.cache.CacheManager
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("weatherRetrofit")
    fun provideWeatherRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("priceRetrofit")
    fun providePriceRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://www.alphavantage.co/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherApi(
        @Named("weatherRetrofit") retrofit: Retrofit,
        @Named("openWeatherApiKey") apiKey: String
    ): WeatherApi {
        return WeatherApiImpl(retrofit.create(WeatherApi::class.java), apiKey)
    }

    @Provides
    @Singleton
    fun providePriceApi(
        @Named("priceRetrofit") retrofit: Retrofit,
        @Named("alphaVantageApiKey") apiKey: String
    ): PriceApi {
        return PriceApiImpl(retrofit.create(PriceApi::class.java), apiKey)
    }

    @Provides
    @Singleton
    fun provideContentApi(okHttpClient: OkHttpClient): ContentApi {
        return ContentApiImpl(
            Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("https://placeholder.com/") // Base URL not used for content API
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ContentApi::class.java)
        )
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideCacheManager(
        @ApplicationContext context: Context,
        gson: Gson
    ): CacheManager {
        return CacheManager(context, gson)
    }

    @Provides
    @Named("openWeatherApiKey")
    fun provideOpenWeatherApiKey(): String {
        return BuildConfig.OPEN_WEATHER_API_KEY
    }

    @Provides
    @Named("alphaVantageApiKey")
    fun provideAlphaVantageApiKey(): String {
        return BuildConfig.ALPHA_VANTAGE_API_KEY
    }
}
