package com.ost.application.data.room

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ost.application.data.StargazersRepo
import com.ost.application.data.model.FetchState
import com.ost.application.data.model.Stargazer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
                    .addCallback(
                        object : Callback() {
                            override fun onOpen(db: SupportSQLiteDatabase) =
                                fetchInitialDataIfNotYet(context, scope)
                        }
                    )
                    .build().also { INSTANCE = it }
            }


        private fun fetchInitialDataIfNotYet(context: Context, scope: CoroutineScope) {
            StargazersRepo.getInstance(context).apply {
                scope .launch {
                    when (fetchStatusFlow.first()) {
                        FetchState.INITED,
                        FetchState.REFRESHED -> Unit
                        else -> refreshStargazers()
                    }
                }
            }
        }
    }
}