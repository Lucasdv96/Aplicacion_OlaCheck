package com.tpoAppInteractivas.olacheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BeachConditionsDao {

    @Query("SELECT * FROM beach_conditions WHERE beachId = :beachId")
    fun getConditionsByBeachId(beachId: String): Flow<BeachConditions?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConditions(conditions: BeachConditions)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllConditions(conditions: List<BeachConditions>)

    @Query("SELECT * FROM beach_conditions")
    fun getAllConditions(): Flow<List<BeachConditions>>
}

