package com.example.mythomash.ui.location

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mythomash.ui.location.LocationLog
import com.example.mythomash.ui.location.LocationLogDao

@Database(entities = [LocationLog::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationLogDao(): LocationLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sensor_logger_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}