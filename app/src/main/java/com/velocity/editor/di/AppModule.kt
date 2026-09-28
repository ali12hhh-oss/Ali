package com.velocity.editor.di

import android.content.Context
import androidx.room.Room
import com.velocity.editor.data.AppDatabase
import com.velocity.editor.data.ProjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "velocity.db").build()

    @Provides
    fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()
}
