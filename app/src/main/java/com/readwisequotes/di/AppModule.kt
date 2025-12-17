// app/src/main/java/com/readwisequotes/di/AppModule.kt
package com.readwisequotes.di

import android.content.Context
import androidx.room.Room
import com.readwisequotes.data.local.AppDatabase
import com.readwisequotes.data.local.QuoteDao
import com.readwisequotes.data.remote.AuthInterceptor
import com.readwisequotes.data.remote.ReadwiseApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "readwise_quotes.db"
        ).build()
    }

    @Provides
    fun provideQuoteDao(database: AppDatabase): QuoteDao {
        return database.quoteDao()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideReadwiseApi(okHttpClient: OkHttpClient): ReadwiseApi {
        return Retrofit.Builder()
            .baseUrl(ReadwiseApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReadwiseApi::class.java)
    }
}
