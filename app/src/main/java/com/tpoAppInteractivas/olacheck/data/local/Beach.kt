package com.tpoAppInteractivas.olacheck.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "beach")
data class Beach(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
    val lastUpdated: Long = 0L
)