package com.ost.application.data.room

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ost.application.data.model.Stargazer
import com.ost.application.data.model.StargazersDao
import kotlinx.coroutines.CoroutineScope

@Database(entities = [Stargazer::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ])
abstract class StargazersDB : RoomDatabase() {
    abstract fun stargazerDao(): StargazersDao

    companion object {
        @Volatile
        private var INSTANCE: StargazersDB? = null

        fun getDatabase(context: Context, scope: CoroutineScope): StargazersDB =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    StargazersDB::class.java,
                    "stargazer_database"
                )
                    .build().also {
                        INSTANCE = it
                    }
            }
    }
}