package com.tpoAppInteractivas.olacheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BeachDao {

    @Query("SELECT * FROM beach")
    fun getAllBeaches(): Flow<List<Beach>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeaches(beaches: List<Beach>)

    @Query("SELECT * FROM beach WHERE id = :beachId")
    suspend fun getBeachById(beachId: String): Beach?
}

