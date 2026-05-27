package com.tpoAppInteractivas.olacheck.repository

import com.tpoAppInteractivas.olacheck.data.local.Beach
import com.tpoAppInteractivas.olacheck.data.local.BeachConditions

interface BeachDetailRepository {
    suspend fun getBeachById(beachId: String): Beach?
    suspend fun getConditionsForBeach(beachId: String): BeachConditions?
    suspend fun refreshConditions(beachId: String)
}