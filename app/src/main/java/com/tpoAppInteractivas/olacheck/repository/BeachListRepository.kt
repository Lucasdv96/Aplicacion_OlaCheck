package com.tpoAppInteractivas.olacheck.repository

import com.tpoAppInteractivas.olacheck.data.local.Beach
import com.tpoAppInteractivas.olacheck.data.local.BeachConditions
import kotlinx.coroutines.flow.Flow

interface BeachListRepository {
    fun getBeaches(): Flow<List<Beach>>

    fun getAllConditions(): Flow<List<BeachConditions>>
    fun getConditionsForBeach(beachId: String): Flow<BeachConditions?>
    suspend fun refreshBeachData()
    fun isOnline(): Boolean
}

