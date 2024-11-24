package com.ost.application.data.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.ost.application.data.model.Stargazer

@Database(entities = [Stargazer::class], version = 1)
abstract class StargazersDB : RoomDatabase() {
    abstract fun stargazerDao(): StargazersDao

    companion object {
        @Volatile
        private var INSTANCE: StargazersDB? = null

        fun getDatabase(context: Context): StargazersDB =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    StargazersDB::class.java,
                    "stargazer_database").build().also { INSTANCE = it }
            }
    }
}