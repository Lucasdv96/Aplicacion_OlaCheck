package com.tpoAppInteractivas.olacheck.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Beach::class, BeachConditions::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun beachDao(): BeachDao
    abstract fun beachConditionsDao(): BeachConditionsDao
}