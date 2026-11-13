package com.tourderun.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RunPointEntity::class], version = 1, exportSchema = true)
abstract class RunDatabase : RoomDatabase() {
    abstract fun runPointDao(): RunPointDao
}
